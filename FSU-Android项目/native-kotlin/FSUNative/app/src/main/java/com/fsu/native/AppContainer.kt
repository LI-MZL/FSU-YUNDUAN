package com.fsu.native

import android.content.Context
import com.fsu.native.data.FSUDatabase
import com.fsu.native.data.ProtoLogDao
import com.fsu.native.data.AlarmDao
import com.fsu.native.protocol.SCClient
import com.fsu.native.repository.FSURepository
import com.fsu.native.server.FSUHttpServer
import com.fsu.native.service.HeartbeatManager
import com.fsu.native.service.OnlineManager
import com.fsu.native.vpn.VPNManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * 应用级依赖注入容器
 * 提供Repository、Service、Database等单例实例
 */
class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    // Room 数据库
    val database: FSUDatabase = FSUDatabase.getDatabase(context, applicationScope)

    // DAO
    val alarmDao: AlarmDao = database.alarmDao()
    val protoLogDao: ProtoLogDao = database.protoLogDao()

    // Repository
    val repository: FSURepository = FSURepository(context, alarmDao, protoLogDao)

    // SC通信客户端
    val scClient: SCClient = SCClient(context)

    // HTTP服务器
    val httpServer: FSUHttpServer = FSUHttpServer(port = 8080)

    // 心跳管理器
    val heartbeatManager: HeartbeatManager = HeartbeatManager(context)

    // 上线管理器
    val onlineManager: OnlineManager = OnlineManager(context, scClient, heartbeatManager)

    // VPN管理器
    val vpnManager: VPNManager = VPNManager(context)
}
