package com.fsu.native.service

import android.content.Context
import android.util.Log
import com.fsu.native.FSUApplication
import com.fsu.native.model.AlarmLevel
import com.fsu.native.model.AlarmFlag
import com.fsu.native.model.AlarmRecord
import com.fsu.native.model.AIChannel
import com.fsu.native.model.DIChannel
import com.fsu.native.model.FsuState
import com.fsu.native.model.LoginState
import com.fsu.native.model.SCMode
import com.fsu.native.model.SmartDevice
import com.fsu.native.model.SmartDeviceType
import com.fsu.native.protocol.BInterfaceProtocol
import com.fsu.native.protocol.SCClient
import com.fsu.native.repository.FSURepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 心跳管理器 - 核心跳保活管理
 * 负责定时发送心跳、数据上报、告警检测等周期性任务
 */
class HeartbeatManager(private val context: Context) {

    companion object {
        private const val TAG = "HeartbeatManager"

        // 心跳间隔 30秒
        const val HEART_BEAT_INTERVAL = 30_000L

        // LOGIN保活间隔 120秒
        const val LOGIN_KEEPALIVE_INTERVAL = 120_000L

        // 数据同步间隔 60秒
        const val DATA_SYNC_INTERVAL = 60_000L

        // 主动上报间隔 60秒
        const val REPORT_DATA_INTERVAL = 60_000L

        // SC探测最大次数
        private const val MAX_SC_DETECT_COUNT = 3
    }

    private val container = FSUApplication.getInstance().container
    private val scClient: SCClient = container.scClient
    private val repository: FSURepository = container.repository
    private val httpServer = container.httpServer

    private var scope: CoroutineScope? = null
    private var heartbeatJob: Job? = null
    private var loginKeepAliveJob: Job? = null
    private var dataSyncJob: Job? = null
    private var reportDataJob: Job? = null

    // 心跳计数
    private var _hbCount: Int = 0

    // SC模式（自动检测）
    private var _scMode: SCMode = SCMode.UNKNOWN

    // SC探测计数
    private var _scDetectCount: Int = 0

    // SC探测是否完成
    private var _scDetectDone: Boolean = false

    // 已发送过设备信息标记
    private var _devInfoReported: Boolean = false

    // UI状态
    private val _state = MutableStateFlow(
        FsuState(
            loginState = LoginState.OFFLINE,
            heartbeatCount = 0,
            scMode = SCMode.UNKNOWN
        )
    )
    val state: StateFlow<FsuState> = _state.asStateFlow()

    private val _logMessage = MutableStateFlow("")
    val logMessage: StateFlow<String> = _logMessage.asStateFlow()

    /**
     * 启动心跳管理
     */
    fun start() {
        if (scope != null) {
            Log.w(TAG, "心跳管理器已在运行")
            return
        }

        scope = CoroutineScope(Dispatchers.IO)
        Log.i(TAG, "心跳管理器启动")

        // 启动心跳定时器
        heartbeatJob = scope?.launch {
            while (true) {
                sendHeartBeat()
                delay(HEART_BEAT_INTERVAL)
            }
        }

        // 启动LOGIN保活定时器
        loginKeepAliveJob = scope?.launch {
            delay(LOGIN_KEEPALIVE_INTERVAL)
            while (true) {
                sendLoginKeepAlive()
                delay(LOGIN_KEEPALIVE_INTERVAL)
            }
        }

        // 启动数据同步定时器
        dataSyncJob = scope?.launch {
            delay(DATA_SYNC_INTERVAL)
            while (true) {
                syncDataAndAlarms()
                delay(DATA_SYNC_INTERVAL)
            }
        }

        // 启动数据上报定时器
        reportDataJob = scope?.launch {
            delay(REPORT_DATA_INTERVAL)
            while (true) {
                sendReportData()
                delay(REPORT_DATA_INTERVAL)
            }
        }
    }

    /**
     * 停止心跳管理
     */
    fun stop() {
        Log.i(TAG, "心跳管理器停止")
        heartbeatJob?.cancel()
        loginKeepAliveJob?.cancel()
        dataSyncJob?.cancel()
        reportDataJob?.cancel()
        scope = null
        _hbCount = 0
        _scDetectDone = false
        _scDetectCount = 0
        _devInfoReported = false
        _state.value = _state.value.copy(
            loginState = LoginState.OFFLINE,
            heartbeatCount = 0,
            scMode = SCMode.UNKNOWN
        )
    }

    /**
     * 重置状态（用于重连）
     */
    fun reset() {
        _hbCount = 0
        _scDetectDone = false
        _scDetectCount = 0
        _devInfoReported = false
        _scMode = SCMode.UNKNOWN
    }

    /**
     * 发送心跳 - 检测SC能力
     */
    private suspend fun sendHeartBeat() {
        val params = repository.getFSUParams()
        val sc = repository.getSCConfig()

        if (params.fsuId.isEmpty() || sc.scIP.isEmpty()) {
            Log.d(TAG, "心跳跳过: FSU ID或SC配置为空")
            return
        }

        _hbCount++
        updateState { copy(heartbeatCount = _hbCount) }

        try {
            val response = scClient.heartbeat(sc, params.fsuId, params.fsuCode)

            // 记录协议日志
            val sendXml = BInterfaceProtocol.buildHeartBeatXml(params.fsuId, params.fsuCode)
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.SEND,
                message = sendXml,
                level = com.fsu.native.model.ProtoLog.Level.INFO
            )
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.RECV,
                message = response.body,
                level = if (response.ok) com.fsu.native.model.ProtoLog.Level.SUCCESS
                        else com.fsu.native.model.ProtoLog.Level.WARN
            )

            if (response.ok) {
                val (name, code) = BInterfaceProtocol.parseResponse(response.body)
                Log.d(TAG, "心跳响应: Name=$name, Code=$code")

                // 检测SC能力
                val supported = code == "0" || response.body.contains("HEART_BEAT", ignoreCase = true)
                detectSCMode(supported)

                // 更新在线状态
                if (_state.value.loginState == LoginState.VPN_CONNECTED) {
                    updateState { copy(loginState = LoginState.LOGIN_SUCCESS) }
                }
            } else {
                Log.w(TAG, "心跳失败: ${response.status}")
                // 心跳失败不改变登录状态，允许短暂网络波动
            }
        } catch (e: Exception) {
            Log.e(TAG, "心跳异常: ${e.message}")
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.SEND,
                message = "HEART_BEAT error: ${e.message}",
                level = com.fsu.native.model.ProtoLog.Level.WARN
            )
        }
    }

    /**
     * LOGIN保活 - 发送LOGIN指令维持SC会话
     */
    private suspend fun sendLoginKeepAlive() {
        if (_state.value.loginState != LoginState.LOGIN_SUCCESS) {
            return
        }

        val params = repository.getFSUParams()
        val sc = repository.getSCConfig()
        val deviceIds = repository.getDeviceIds()

        try {
            val response = scClient.login(
                sc = sc,
                fsuId = params.fsuId,
                fsuCode = params.fsuCode,
                vpnIP = _state.value.vpnIP,
                deviceIds = deviceIds
            )

            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.SEND,
                message = "LOGIN keep-alive (hbCount=$_hbCount)",
                level = com.fsu.native.model.ProtoLog.Level.DEBUG
            )
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.RECV,
                message = response.body,
                level = if (response.ok) com.fsu.native.model.ProtoLog.Level.SUCCESS
                        else com.fsu.native.model.ProtoLog.Level.WARN
            )

            if (!response.ok) {
                Log.w(TAG, "LOGIN保活失败: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "LOGIN保活异常: ${e.message}")
        }
    }

    /**
     * 同步数据到HTTP服务器 + 检测告警
     */
    private suspend fun syncDataAndAlarms() {
        try {
            // 同步设备信号到HTTP服务器
            val signals = repository.getAllDeviceSignals()
            for ((devId, signalList) in signals) {
                httpServer.setDeviceSignals(devId, signalList)
            }

            // 更新HTTP服务器的FSU信息
            val params = repository.getFSUParams()
            httpServer.fsuId = params.fsuId
            httpServer.fsuCode = params.fsuCode

            // 检测告警
            checkAndSendAlarms()

            Log.d(TAG, "数据同步完成")
        } catch (e: Exception) {
            Log.e(TAG, "数据同步异常: ${e.message}")
        }
    }

    /**
     * 主动上报数据（REPORT_DATA）
     */
    private suspend fun sendReportData() {
        if (_state.value.loginState != LoginState.LOGIN_SUCCESS) {
            return
        }

        val params = repository.getFSUParams()
        val sc = repository.getSCConfig()

        // 首次上报时发送设备信息
        if (!_devInfoReported) {
            sendReportDevInfo()
            _devInfoReported = true
        }

        try {
            val allSignals = repository.getAllDeviceSignals()
            for ((devId, signalList) in allSignals) {
                if (signalList.isEmpty()) continue
                val devCode = signalList.first().devCode
                val dataItems = signalList.map { signal ->
                    BInterfaceProtocol.DataItem(
                        id = signal.id,
                        value = signal.measuredVal,
                        unit = "",
                        state = signal.status,
                        time = BInterfaceProtocol.getCurrentTime()
                    )
                }
                val response = scClient.reportData(
                    sc = sc,
                    fsuId = params.fsuId,
                    fsuCode = params.fsuCode,
                    deviceId = devId,
                    deviceCode = devCode,
                    dataItems = dataItems
                )

                if (!response.ok) {
                    Log.w(TAG, "REPORT_DATA失败($devId): ${response.status}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "REPORT_DATA异常: ${e.message}")
        }
    }

    /**
     * 发送设备信息（REPORT_DEV_INFO）
     */
    private suspend fun sendReportDevInfo() {
        val params = repository.getFSUParams()
        val sc = repository.getSCConfig()
        val devices = repository.getDeviceInfoList()

        if (devices.isEmpty()) return

        try {
            val response = scClient.reportDevInfo(
                sc = sc,
                fsuId = params.fsuId,
                fsuCode = params.fsuCode,
                devices = devices
            )

            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.SEND,
                message = "REPORT_DEV_INFO (${devices.size} devices)",
                level = com.fsu.native.model.ProtoLog.Level.INFO
            )
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.RECV,
                message = response.body,
                level = if (response.ok) com.fsu.native.model.ProtoLog.Level.SUCCESS
                        else com.fsu.native.model.ProtoLog.Level.WARN
            )

            Log.d(TAG, "设备信息上报: ${response.status}")
        } catch (e: Exception) {
            Log.e(TAG, "设备信息上报异常: ${e.message}")
        }
    }

    /**
     * SC能力自动检测
     */
    private fun detectSCMode(supported: Boolean) {
        if (_scDetectDone) return

        _scDetectCount++
        if (supported) {
            _scMode = SCMode.FULL
            _scDetectDone = true
            Log.i(TAG, "SC模式检测: FULL (第${_scDetectCount}次心跳)")
        } else if (_scDetectCount >= MAX_SC_DETECT_COUNT) {
            _scMode = SCMode.LOGIN_ONLY
            _scDetectDone = true
            Log.i(TAG, "SC模式检测: LOGIN_ONLY (连续${MAX_SC_DETECT_COUNT}次心跳不支持)")
        }
        updateState { copy(scMode = _scMode) }
    }

    /**
     * 遍历AI/DI/智能设备检测告警
     */
    private suspend fun checkAndSendAlarms() {
        val params = repository.getFSUParams()
        if (params.fsuId.isEmpty()) return

        val aiChannels = repository.getAIChannels()
        val diChannels = repository.getDIChannels()
        val smartDevices = repository.getSmartDevices()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 检测AI通道告警
        for (ai in aiChannels) {
            if (!ai.enabled) continue
            if (ai.value > ai.alarmHi || ai.value < ai.alarmLo) {
                val alarm = AlarmRecord(
                    devId = params.fsuId,
                    devCode = params.fsuCode,
                    alarmTime = now,
                    fsuId = params.fsuId,
                    fsuCode = params.fsuCode,
                    itemId = ai.itemId,
                    alarmLevel = if (ai.value > ai.alarmHi) AlarmLevel.LEVEL2 else AlarmLevel.LEVEL3,
                    alarmFlag = AlarmFlag.START,
                    alarmDesc = "${ai.name} 超限: ${ai.value}${ai.unit}"
                )
                sendAlarm(alarm)
            }
        }

        // 检测DI通道告警
        for (di in diChannels) {
            if (!di.enabled) continue
            if (di.value != 0) {
                val alarm = AlarmRecord(
                    devId = params.fsuId,
                    devCode = params.fsuCode,
                    alarmTime = now,
                    fsuId = params.fsuId,
                    fsuCode = params.fsuCode,
                    itemId = di.itemId,
                    alarmLevel = AlarmLevel.LEVEL1,
                    alarmFlag = AlarmFlag.START,
                    alarmDesc = "${di.name} 状态变化: ${di.value}"
                )
                sendAlarm(alarm)
            }
        }

        // 检测智能设备告警
        for (device in smartDevices) {
            if (!device.enabled) continue
            for (item in device.items) {
                val valNum = item.value.toFloatOrNull() ?: continue
                val minNum = item.min.toFloatOrNull() ?: Float.MIN_VALUE
                val maxNum = item.max.toFloatOrNull() ?: Float.MAX_VALUE

                if (valNum > maxNum || valNum < minNum) {
                    val alarm = AlarmRecord(
                        devId = device.devId,
                        devCode = "",
                        alarmTime = now,
                        fsuId = params.fsuId,
                        fsuCode = params.fsuCode,
                        itemId = item.itemId,
                        alarmLevel = AlarmLevel.LEVEL2,
                        alarmFlag = AlarmFlag.START,
                        alarmDesc = "${device.name}-${item.name} 超限: ${item.value}${item.unit}"
                    )
                    sendAlarm(alarm)
                }
            }
        }
    }

    /**
     * 发送单个告警
     */
    private suspend fun sendAlarm(alarm: AlarmRecord) {
        val sc = repository.getSCConfig()

        // 保存到数据库
        repository.insertAlarm(alarm)

        // 添加到HTTP服务器告警列表
        val alarmXml = """<Alarm><DevId>${alarm.devId}</DevId><ItemId>${alarm.itemId}</ItemId><AlarmLevel>${alarm.alarmLevel.level}</AlarmLevel><AlarmFlag>${if (alarm.alarmFlag == AlarmFlag.START) 1 else 0}</AlarmFlag><AlarmDesc>${alarm.alarmDesc}</AlarmDesc></Alarm>"""
        httpServer.addAlarm(alarmXml)

        // 发送到SC
        try {
            val response = scClient.sendAlarm(sc, alarm)

            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.SEND,
                message = "SEND_ALARM: ${alarm.itemId} ${alarm.alarmDesc}",
                level = com.fsu.native.model.ProtoLog.Level.WARN
            )
            repository.addProtoLog(
                direction = com.fsu.native.model.ProtoLog.Direction.RECV,
                message = response.body,
                level = if (response.ok) com.fsu.native.model.ProtoLog.Level.SUCCESS
                        else com.fsu.native.model.ProtoLog.Level.WARN
            )
        } catch (e: Exception) {
            Log.e(TAG, "告警发送失败: ${e.message}")
        }
    }

    /**
     * 更新VPN IP
     */
    fun updateVpnIP(vpnIP: String) {
        updateState { copy(vpnIP = vpnIP) }
    }

    /**
     * 更新登录状态
     */
    fun updateLoginState(state: LoginState) {
        updateState { copy(loginState = state) }
    }

    private fun updateState(update: FsuState.() -> FsuState) {
        _state.value = _state.value.update()
    }
}
