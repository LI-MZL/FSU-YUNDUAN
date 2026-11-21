package com.fsu.native.service

import android.content.Context
import android.util.Log
import com.fsu.native.model.AlarmRecord
import com.fsu.native.model.FsuState
import com.fsu.native.model.LoginState
import com.fsu.native.model.ProtoLog
import com.fsu.native.model.SmartDeviceType
import com.fsu.native.protocol.BInterfaceProtocol
import com.fsu.native.protocol.SCClient
import com.fsu.native.repository.FSURepository
import com.fsu.native.util.DeviceIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 上线流程管理器 - 管理完整的FSU上线/下线流程
 *
 * 上线流程:
 *   Step1: VPN配置 -> Step2: VPN拨号 -> Step3: 获取VPN IP
 *   Step4: LOGIN -> Step5: 心跳启动
 *
 * 下线流程:
 *   LOGOUT -> 断VPN -> 停心跳
 */
class OnlineManager(
    private val context: Context,
    private val scClient: SCClient,
    private val heartbeatManager: HeartbeatManager
) {

    companion object {
        private const val TAG = "OnlineManager"
        private const val MAX_RELOGIN_RETRY = 5
        private const val BASE_RELOGIN_DELAY = 10_000L // 10秒基础重连延迟
    }

    private val repository: FSURepository = FSURepository.getInstance(context)

    private val _fsuState = MutableStateFlow(FsuState())
    val fsuState: StateFlow<FsuState> = _fsuState.asStateFlow()

    private val _onlineStatus = MutableStateFlow("")
    val onlineStatus: StateFlow<String> = _onlineStatus.asStateFlow()

    private var isOnline: Boolean = false
    private var reloginCount: Int = 0
    private var scope: CoroutineScope? = null

    /**
     * 完整上线流程
     */
    fun goOnline() {
        if (isOnline) {
            Log.w(TAG, "FSU已在线，跳过上线")
            return
        }

        scope = CoroutineScope(Dispatchers.IO)
        scope?.launch {
            try {
                // Step 1: VPN配置
                updateStatus("Step 1: 配置VPN...")
                val vpnConfig = repository.getVPNConfig()
                if (vpnConfig.vpnType != com.fsu.native.model.VPNType.DISABLE) {
                    // Step 2: VPN拨号
                    updateStatus("Step 2: VPN拨号中...")
                    updateLoginState(LoginState.VPN_CONNECTING)

                    // 写入VPN配置
                    val vpnManager = com.fsu.native.vpn.VPNManager(context)
                    vpnManager.writeVPNConfig(vpnConfig)

                    // 模拟VPN连接（真实设备会使用VpnService）
                    Thread.sleep(2000)
                    val mockVpnIP = "10.${(100..255).random()}.${(1..255).random()}.${(1..254).random()}"

                    // Step 3: 获取VPN IP
                    updateStatus("Step 3: 获取VPN IP: $mockVpnIP")
                    updateLoginState(LoginState.VPN_CONNECTED)
                    _fsuState.value = _fsuState.value.copy(vpnIP = mockVpnIP)
                    heartbeatManager.updateVpnIP(mockVpnIP)
                } else {
                    updateStatus("VPN禁用，跳过VPN步骤")
                    updateLoginState(LoginState.VPN_CONNECTED)
                    _fsuState.value = _fsuState.value.copy(vpnIP = "127.0.0.1")
                }

                // Step 4: LOGIN
                updateStatus("Step 4: 向SC登录...")
                val params = repository.getFSUParams()
                val sc = repository.getSCConfig()

                if (sc.scIP.isNotEmpty() && params.fsuId.isNotEmpty()) {
                    // 生成设备ID列表
                    val deviceIds = generateDeviceIds(params.fsuId)

                    // 先探测SC路径
                    updateStatus("Step 4: 探测SC路径...")
                    val scPath = scClient.probeSC(sc.scIP, sc.scPort)
                    if (scPath != null) {
                        updateStatus("Step 4: SC路径 $scPath, 发送LOGIN...")
                    }

                    val loginResponse = scClient.login(
                        sc = sc,
                        fsuId = params.fsuId,
                        fsuCode = params.fsuCode,
                        vpnIP = _fsuState.value.vpnIP,
                        deviceIds = deviceIds
                    )

                    // 记录协议日志
                    val loginXml = BInterfaceProtocol.buildLoginXml(
                        fsuId = params.fsuId,
                        fsuCode = params.fsuCode,
                        fsuIP = _fsuState.value.vpnIP,
                        fsuPort = 8080,
                        username = sc.username,
                        password = sc.password,
                        deviceIds = deviceIds
                    )
                    repository.addProtoLog(
                        direction = ProtoLog.Direction.SEND,
                        message = loginXml,
                        level = ProtoLog.Level.INFO
                    )
                    repository.addProtoLog(
                        direction = ProtoLog.Direction.RECV,
                        message = loginResponse.body,
                        level = if (loginResponse.ok) ProtoLog.Level.SUCCESS
                                else ProtoLog.Level.WARN
                    )

                    if (loginResponse.ok) {
                        val (name, code) = BInterfaceProtocol.parseResponse(loginResponse.body)
                        if (code == "0" || loginResponse.body.contains("0", ignoreCase = true)) {
                            updateStatus("Step 4: LOGIN成功")
                            updateLoginState(LoginState.LOGIN_SUCCESS)

                            // Step 5: 启动心跳
                            updateStatus("Step 5: 启动心跳保活...")
                            heartbeatManager.reset()
                            heartbeatManager.start()

                            isOnline = true
                            reloginCount = 0
                            updateStatus("上线完成，心跳运行中")

                            // 启动前台服务
                            FSUForegroundService.start(context)

                            // 保存登录状态
                            repository.setLoginState(true)
                        } else {
                            updateLoginState(LoginState.LOGIN_FAIL)
                            updateStatus("LOGIN失败: Code=$code, Name=$name")
                        }
                    } else {
                        updateLoginState(LoginState.LOGIN_FAIL)
                        updateStatus("LOGIN请求失败: ${loginResponse.status}")
                    }
                } else {
                    updateStatus("SC配置或FSU ID为空，跳过LOGIN")
                    updateLoginState(LoginState.LOGIN_SUCCESS)
                    isOnline = true

                    // 启动心跳（离线模拟模式）
                    heartbeatManager.reset()
                    heartbeatManager.start()
                    FSUForegroundService.start(context)
                    repository.setLoginState(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "上线异常: ${e.message}")
                updateLoginState(LoginState.LOGIN_FAIL)
                updateStatus("上线失败: ${e.message}")
            }
        }
    }

    /**
     * 完整下线流程
     */
    fun goOffline() {
        if (!isOnline) {
            Log.w(TAG, "FSU已离线，跳过下线")
            return
        }

        scope?.launch {
            try {
                // Step 1: LOGOUT
                updateStatus("下线: 发送LOGOUT...")
                val params = repository.getFSUParams()
                val sc = repository.getSCConfig()

                if (sc.scIP.isNotEmpty() && params.fsuId.isNotEmpty()) {
                    val logoutResponse = scClient.logout(sc, params.fsuId)

                    repository.addProtoLog(
                        direction = ProtoLog.Direction.SEND,
                        message = BInterfaceProtocol.buildLogoutXml(params.fsuId),
                        level = ProtoLog.Level.INFO
                    )
                    repository.addProtoLog(
                        direction = ProtoLog.Direction.RECV,
                        message = logoutResponse.body,
                        level = ProtoLog.Level.INFO
                    )
                }

                // Step 2: 断VPN
                updateStatus("下线: 断开VPN...")
                heartbeatManager.stop()
                heartbeatManager.updateLoginState(LoginState.OFFLINE)

                // Step 3: 停心跳
                updateStatus("下线: 停止心跳...")

                isOnline = false
                updateLoginState(LoginState.OFFLINE)
                updateStatus("下线完成")

                // 停止前台服务
                FSUForegroundService.stop(context)

                // 保存离线状态
                repository.setLoginState(false)
            } catch (e: Exception) {
                Log.e(TAG, "下线异常: ${e.message}")
            }
        }
    }

    /**
     * 重连逻辑（递增延迟）
     */
    fun doReLogin() {
        if (reloginCount >= MAX_RELOGIN_RETRY) {
            Log.w(TAG, "重连次数已达上限($MAX_RELOGIN_RETRY)")
            updateStatus("重连失败，已达到最大重试次数")
            return
        }

        scope?.launch {
            reloginCount++
            val delayMs = BASE_RELOGIN_DELAY * reloginCount
            updateStatus("重连中(${reloginCount}/$MAX_RELOGIN_RETRY)，${delayMs / 1000}秒后重试...")

            delay(delayMs)

            try {
                val params = repository.getFSUParams()
                val sc = repository.getSCConfig()

                val loginResponse = scClient.login(
                    sc = sc,
                    fsuId = params.fsuId,
                    fsuCode = params.fsuCode,
                    vpnIP = _fsuState.value.vpnIP,
                    deviceIds = generateDeviceIds(params.fsuId)
                )

                if (loginResponse.ok) {
                    val (name, code) = BInterfaceProtocol.parseResponse(loginResponse.body)
                    if (code == "0" || loginResponse.body.contains("0", ignoreCase = true)) {
                        updateLoginState(LoginState.LOGIN_SUCCESS)
                        reloginCount = 0
                        updateStatus("重连成功")
                    } else {
                        updateStatus("重连失败: Code=$code")
                        doReLogin()
                    }
                } else {
                    updateStatus("重连请求失败: ${loginResponse.status}")
                    doReLogin()
                }
            } catch (e: Exception) {
                Log.e(TAG, "重连异常: ${e.message}")
                updateStatus("重连异常: ${e.message}")
                doReLogin()
            }
        }
    }

    /**
     * 获取当前在线状态
     */
    fun getIsOnline(): Boolean = isOnline

    // ============================================================================
    // 内部方法
    // ============================================================================

    private fun updateLoginState(state: LoginState) {
        _fsuState.value = _fsuState.value.copy(loginState = state)
        heartbeatManager.updateLoginState(state)
    }

    private fun updateStatus(status: String) {
        _onlineStatus.value = status
        Log.d(TAG, status)
    }

    /**
     * 生成默认设备ID列表
     */
    private fun generateDeviceIds(fsuId: String): List<String> {
        return listOf(
            SmartDeviceType.POWER,
            SmartDeviceType.BATTERY,
            SmartDeviceType.AC,
            SmartDeviceType.SMOKE,
            SmartDeviceType.WATER,
            SmartDeviceType.DOOR,
            SmartDeviceType.WATERTEMP
        ).map { type -> DeviceIdGenerator.deriveDevId(fsuId, type) }
    }
}
