package com.fsu.native.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fsu.native.data.AlarmDao
import com.fsu.native.data.ProtoLogDao
import com.fsu.native.model.*
import com.fsu.native.protocol.BInterfaceProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一数据访问层 - 管理FSU配置、告警、协议日志和设备信号
 * 使用SharedPreferences存储配置，Room存储告警和日志
 */
class FSURepository private constructor(
    private val context: Context,
    private val alarmDao: AlarmDao,
    private val protoLogDao: ProtoLogDao
) {
    companion object {
        private const val TAG = "FSURepository"
        private const val PREFS_NAME = "fsu_prefs"

        @Volatile
        private var INSTANCE: FSURepository? = null

        fun getInstance(context: Context): FSURepository {
            return INSTANCE ?: synchronized(this) {
                val db = com.fsu.native.data.FSUDatabase.getDatabase(
                    context,
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                )
                FSURepository(context, db.alarmDao(), db.protoLogDao()).also {
                    INSTANCE = it
                }
            }
        }
    }

    // SharedPreferences
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 内存缓存 - 协议日志（最近200条）
    private val _protoLogCache = mutableListOf<ProtoLog>()
    private val maxCacheSize = 200

    // 协议日志Flow
    private val _protoLogs = MutableStateFlow<List<ProtoLog>>(emptyList())
    val protoLogs: StateFlow<List<ProtoLog>> = _protoLogs.asStateFlow()

    // 活动告警数Flow
    private val _activeAlarmCount = MutableStateFlow(0)
    val activeAlarmCount: StateFlow<Int> = _activeAlarmCount.asStateFlow()

    // ============================================================================
    // FSU参数配置
    // ============================================================================

    fun saveFSUParams(params: FSUParams) {
        prefs.edit().apply {
            putString("fsu_id", params.fsuId)
            putString("fsu_code", params.fsuCode)
            putString("fsu_name", params.fsuName)
            putString("fsu_type", params.fsuType.name)
            putString("site_type", params.siteType.name)
            putString("work_state", params.workState.name)
            putString("monitor_id", params.monitorId)
            putString("battery_id1", params.batteryId1)
            putString("battery_id2", params.batteryId2)
            putString("dam_longitude", params.damLongitude)
            putString("dam_latitude", params.damLatitude)
            putString("dam_height", params.damHeight)
            putString("soft_ver", params.softVer)
            putString("b_ver", params.bVer)
            apply()
        }
    }

    fun getFSUParams(): FSUParams {
        return FSUParams(
            fsuId = prefs.getString("fsu_id", "") ?: "",
            fsuCode = prefs.getString("fsu_code", "") ?: "",
            fsuName = prefs.getString("fsu_name", "FSU-模拟器") ?: "FSU-模拟器",
            fsuType = FSUType.valueOf(prefs.getString("fsu_type", "INTSTAN") ?: "INTSTAN"),
            siteType = SiteType.valueOf(prefs.getString("site_type", "SHARED") ?: "SHARED"),
            workState = WorkState.valueOf(prefs.getString("work_state", "NORMAL") ?: "NORMAL"),
            monitorId = prefs.getString("monitor_id", "") ?: "",
            batteryId1 = prefs.getString("battery_id1", "") ?: "",
            batteryId2 = prefs.getString("battery_id2", "") ?: "",
            damLongitude = prefs.getString("dam_longitude", "0.000000") ?: "0.000000",
            damLatitude = prefs.getString("dam_latitude", "0.000000") ?: "0.000000",
            damHeight = prefs.getString("dam_height", "0") ?: "0",
            softVer = prefs.getString("soft_ver", "25.2.HQ.FSU.GX.AA63.R") ?: "25.2.HQ.FSU.GX.AA63.R",
            bVer = prefs.getString("b_ver", "2.78.00.00") ?: "2.78.00.00"
        )
    }

    // ============================================================================
    // VPN配置
    // ============================================================================

    fun saveVPNConfig(config: VPNConfig) {
        prefs.edit().apply {
            putString("vpn_net", config.vpnNet)
            putString("vpn_type", config.vpnType.name)
            putString("vpn_server1", config.serverIP1)
            putString("vpn_server2", config.serverIP2)
            putString("vpn_user", config.vpnUser)
            putString("vpn_pass", config.password)
            putString("vpn_passkey", config.passKey)
            putBoolean("vpn_effect", config.effectFlag)
            apply()
        }
    }

    fun getVPNConfig(): VPNConfig {
        return VPNConfig(
            vpnNet = prefs.getString("vpn_net", "") ?: "",
            vpnType = VPNType.valueOf(prefs.getString("vpn_type", "DISABLE") ?: "DISABLE"),
            serverIP1 = prefs.getString("vpn_server1", "") ?: "",
            serverIP2 = prefs.getString("vpn_server2", "") ?: "",
            vpnUser = prefs.getString("vpn_user", "") ?: "",
            password = prefs.getString("vpn_pass", "") ?: "",
            passKey = prefs.getString("vpn_passkey", "") ?: "",
            effectFlag = prefs.getBoolean("vpn_effect", false)
        )
    }

    // ============================================================================
    // SC配置
    // ============================================================================

    fun saveSCConfig(config: SCConfig) {
        prefs.edit().apply {
            putString("sc_name", config.name)
            putString("sc_ip", config.scIP)
            putInt("sc_port", config.scPort)
            putBoolean("sc_enabled", config.enabled)
            putString("sc_user", config.username)
            putString("sc_pass", config.password)
            apply()
        }
    }

    fun getSCConfig(): SCConfig {
        return SCConfig(
            name = prefs.getString("sc_name", "SC服务器") ?: "SC服务器",
            scIP = prefs.getString("sc_ip", "") ?: "",
            scPort = prefs.getInt("sc_port", 8080),
            enabled = prefs.getBoolean("sc_enabled", true),
            username = prefs.getString("sc_user", "cntower") ?: "cntower",
            password = prefs.getString("sc_pass", "cntower") ?: "cntower"
        )
    }

    // ============================================================================
    // 串口配置
    // ============================================================================

    fun saveSerialConfigs(configs: List<SerialPortConfig>) {
        prefs.edit().apply {
            for (config in configs) {
                val prefix = "serial_${config.portIndex}_"
                putBoolean(prefix + "enabled", config.enabled)
                putInt(prefix + "baud", config.baudRate)
                putString(prefix + "dev_name", config.deviceName)
                putString(prefix + "dev_type", config.deviceType)
                putString(prefix + "dev_code", config.deviceCode)
            }
            apply()
        }
    }

    fun getSerialConfigs(): List<SerialPortConfig> {
        val configs = mutableListOf<SerialPortConfig>()
        for (i in 1..18) {
            val prefix = "serial_${i}_"
            configs.add(
                SerialPortConfig(
                    portIndex = i,
                    enabled = prefs.getBoolean(prefix + "enabled", false),
                    baudRate = prefs.getInt(prefix + "baud", 9600),
                    deviceName = prefs.getString(prefix + "dev_name", "") ?: "",
                    deviceType = prefs.getString(prefix + "dev_type", "") ?: "",
                    deviceCode = prefs.getString(prefix + "dev_code", "") ?: ""
                )
            )
        }
        return configs
    }

    // ============================================================================
    // AI/DI通道
    // ============================================================================

    fun saveAIChannels(channels: List<AIChannel>) {
        prefs.edit().apply {
            for (ch in channels) {
                val prefix = "ai_${ch.index}_"
                putBoolean(prefix + "enabled", ch.enabled)
                putString(prefix + "name", ch.name)
                putString(prefix + "itemid", ch.itemId)
                putString(prefix + "unit", ch.unit)
                putFloat(prefix + "hi", ch.alarmHi)
                putFloat(prefix + "lo", ch.alarmLo)
            }
            apply()
        }
    }

    fun getAIChannels(): List<AIChannel> {
        val channels = mutableListOf<AIChannel>()
        for (i in 1..8) {
            val prefix = "ai_${i}_"
            channels.add(
                AIChannel(
                    index = i,
                    enabled = prefs.getBoolean(prefix + "enabled", false),
                    name = prefs.getString(prefix + "name", "AI通道$i") ?: "AI通道$i",
                    itemId = prefs.getString(prefix + "itemid", "") ?: "",
                    unit = prefs.getString(prefix + "unit", "") ?: "",
                    alarmHi = prefs.getFloat(prefix + "hi", Float.MAX_VALUE),
                    alarmLo = prefs.getFloat(prefix + "lo", Float.MIN_VALUE),
                    value = 0f
                )
            )
        }
        return channels
    }

    fun saveDIChannels(channels: List<DIChannel>) {
        prefs.edit().apply {
            for (ch in channels) {
                val prefix = "di_${ch.index}_"
                putBoolean(prefix + "enabled", ch.enabled)
                putString(prefix + "name", ch.name)
                putString(prefix + "itemid", ch.itemId)
            }
            apply()
        }
    }

    fun getDIChannels(): List<DIChannel> {
        val channels = mutableListOf<DIChannel>()
        for (i in 1..16) {
            val prefix = "di_${i}_"
            channels.add(
                DIChannel(
                    index = i,
                    enabled = prefs.getBoolean(prefix + "enabled", false),
                    name = prefs.getString(prefix + "name", "DI通道$i") ?: "DI通道$i",
                    itemId = prefs.getString(prefix + "itemid", "") ?: "",
                    value = 0
                )
            )
        }
        return channels
    }

    // ============================================================================
    // 智能设备
    // ============================================================================

    fun saveSmartDevices(devices: List<SmartDevice>) {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        prefs.edit().putString("smart_devices_json", "").apply()
        // 简化：使用Gson
        try {
            val gson = com.google.gson.Gson()
            prefs.edit().putString("smart_devices_json", gson.toJson(devices)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存智能设备失败: ${e.message}")
        }
    }

    fun getSmartDevices(): List<SmartDevice> {
        val json = prefs.getString("smart_devices_json", "") ?: ""
        if (json.isEmpty()) return emptyList()
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<SmartDevice>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "读取智能设备失败: ${e.message}")
            emptyList()
        }
    }

    // ============================================================================
    // 告警数据
    // ============================================================================

    suspend fun insertAlarm(alarm: AlarmRecord): Long {
        val id = alarmDao.insert(alarm)
        refreshActiveAlarmCount()
        return id
    }

    suspend fun getAllAlarms(): List<AlarmRecord> = alarmDao.getAll()

    suspend fun getActiveAlarms(): List<AlarmRecord> = alarmDao.getActive()

    suspend fun deleteAlarm(id: Long) {
        alarmDao.deleteById(id)
        refreshActiveAlarmCount()
    }

    suspend fun clearActiveAlarm(itemId: String) {
        alarmDao.clearActive(itemId)
        refreshActiveAlarmCount()
    }

    suspend fun clearAllActiveAlarms() {
        alarmDao.clearAllActive()
        refreshActiveAlarmCount()
    }

    suspend fun getActiveAlarmCount(): Int = alarmDao.getActiveCount()

    private suspend fun refreshActiveAlarmCount() {
        _activeAlarmCount.value = alarmDao.getActiveCount()
    }

    // ============================================================================
    // 协议日志
    // ============================================================================

    suspend fun addProtoLog(
        direction: ProtoLog.Direction,
        message: String,
        level: ProtoLog.Level = ProtoLog.Level.INFO
    ) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val log = ProtoLog(
            timestamp = now,
            direction = direction,
            message = message,
            level = level
        )

        // 写入数据库
        try {
            protoLogDao.insert(log)
            // 清理旧日志，保留500条
            protoLogDao.trimToMax(500)
        } catch (e: Exception) {
            Log.e(TAG, "写入日志失败: ${e.message}")
        }

        // 更新内存缓存
        synchronized(_protoLogCache) {
            _protoLogCache.add(0, log)
            while (_protoLogCache.size > maxCacheSize) {
                _protoLogCache.removeAt(_protoLogCache.size - 1)
            }
            _protoLogs.value = _protoLogCache.toList()
        }
    }

    suspend fun getAllProtoLogs(): List<ProtoLog> = protoLogDao.getAll()

    suspend fun clearProtoLogs() {
        protoLogDao.clearAll()
        synchronized(_protoLogCache) {
            _protoLogCache.clear()
            _protoLogs.value = emptyList()
        }
    }

    // ============================================================================
    // 设备信号数据
    // ============================================================================

    fun getAllDeviceSignals(): Map<String, List<DeviceSignal>> {
        // 从AI/DI通道和智能设备构建信号数据
        val signals = mutableMapOf<String, MutableList<DeviceSignal>>()
        val params = getFSUParams()

        // AI通道信号
        val aiChannels = getAIChannels()
        for (ai in aiChannels) {
            if (!ai.enabled || ai.itemId.isEmpty()) continue
            val devKey = params.fsuId
            signals.getOrPut(devKey) { mutableListOf() }.add(
                DeviceSignal(
                    devId = params.fsuId,
                    devCode = params.fsuCode,
                    type = SignalType.AI,
                    id = ai.itemId,
                    measuredVal = ai.value.toString(),
                    setupVal = "",
                    status = "0"
                )
            )
        }

        // DI通道信号
        val diChannels = getDIChannels()
        for (di in diChannels) {
            if (!di.enabled || di.itemId.isEmpty()) continue
            val devKey = params.fsuId
            signals.getOrPut(devKey) { mutableListOf() }.add(
                DeviceSignal(
                    devId = params.fsuId,
                    devCode = params.fsuCode,
                    type = SignalType.DI,
                    id = di.itemId,
                    measuredVal = di.value.toString(),
                    setupVal = "",
                    status = di.value.toString()
                )
            )
        }

        // 智能设备信号
        val smartDevices = getSmartDevices()
        for (device in smartDevices) {
            if (!device.enabled) continue
            val devSignals = mutableListOf<DeviceSignal>()
            for (item in device.items) {
                devSignals.add(
                    DeviceSignal(
                        devId = device.devId,
                        devCode = "",
                        type = SignalType.AI,
                        id = item.itemId,
                        measuredVal = item.value,
                        setupVal = item.min,
                        status = "0"
                    )
                )
            }
            signals[device.devId] = devSignals
        }

        return signals
    }

    fun getDeviceIds(): List<String> {
        val params = getFSUParams()
        return getSmartDevices()
            .filter { it.enabled && it.devId.isNotEmpty() }
            .map { it.devId }
            .ifEmpty { listOf(params.fsuId) }
    }

    fun getDeviceInfoList(): List<BInterfaceProtocol.DeviceInfo> {
        val devices = getSmartDevices().filter { it.enabled }
        return devices.map { dev ->
            BInterfaceProtocol.DeviceInfo(
                deviceId = dev.devId,
                deviceCode = "",
                deviceName = dev.name,
                idList = dev.items.map { it.itemId }
            )
        }
    }

    // ============================================================================
    // 登录状态持久化
    // ============================================================================

    fun setLoginState(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    // ============================================================================
    // MAC地址
    // ============================================================================

    fun getMacAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = intf.hardwareAddress ?: continue
                    return macBytes.joinToString(":") { "%02X".format(it) }
                }
            }
        } catch (_: Exception) {
        }
        return "02:00:00:00:00:00"
    }

    // ============================================================================
    // 本地IP
    // ============================================================================

    fun getLocalIP(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "0.0.0.0"
    }
}
