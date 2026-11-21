package com.fsu.native.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fsu.native.FSUApplication
import com.fsu.native.MainActivity
import com.fsu.native.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FSU前台服务 - 确保心跳不被系统杀死
 * 在通知栏显示FSU运行状态（心跳计数、告警数量）
 */
class FSUForegroundService : Service() {

    companion object {
        private const val TAG = "FSUForegroundService"
        private const val CHANNEL_ID = "fsu_service_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * 启动前台服务
         */
        fun start(context: Context) {
            val intent = Intent(context, FSUForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 停止前台服务
         */
        fun stop(context: Context) {
            val intent = Intent(context, FSUForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var notificationUpdateJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "前台服务创建")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0, 0, "启动中..."))
        startHeartbeatAndServices()
        startNotificationUpdater()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "前台服务启动命令")
        val notification = buildNotification(
            heartbeatCount = 0,
            alarmCount = 0,
            status = "FSU服务运行中"
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "前台服务销毁")
        notificationUpdateJob?.cancel()
        stopHeartbeatAndServices()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 创建通知渠道（Android 8+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FSU服务通道",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "FSU后台心跳保活服务"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * 构建通知
     */
    private fun buildNotification(heartbeatCount: Int, alarmCount: Int, status: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("铁塔FSU模拟器")
            .setContentText("心跳: $heartbeatCount | 告警: $alarmCount | $status")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 启动心跳和相关服务
     */
    private fun startHeartbeatAndServices() {
        val container = FSUApplication.getInstance().container

        // 启动HTTP服务器
        try {
            if (!container.httpServer.isAlive()) {
                container.httpServer.start()
                Log.d(TAG, "HTTP服务器启动: 端口 8080")
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP服务器启动失败: ${e.message}")
        }

        // 启动心跳管理器
        container.heartbeatManager.start()
        Log.d(TAG, "心跳管理器已启动")
    }

    /**
     * 停止心跳和相关服务
     */
    private fun stopHeartbeatAndServices() {
        val container = FSUApplication.getInstance().container
        container.heartbeatManager.stop()
        try {
            container.httpServer.stop()
        } catch (_: Exception) {
        }
    }

    /**
     * 定期更新通知内容
     */
    private fun startNotificationUpdater() {
        val container = FSUApplication.getInstance().container
        notificationUpdateJob = serviceScope.launch {
            kotlinx.coroutines.delay(5000) // 初始延迟5秒
            while (true) {
                try {
                    val state = container.heartbeatManager.state.value
                    val activeAlarmCount = container.repository.getActiveAlarmCount()

                    val statusText = when {
                        state.loginState.value == "登录成功" -> "在线"
                        state.loginState.value == "离线" -> "离线"
                        state.loginState.value.contains("连接") -> "连接中"
                        else -> state.loginState.value
                    }

                    val notification = buildNotification(
                        heartbeatCount = state.heartbeatCount,
                        alarmCount = activeAlarmCount,
                        status = statusText
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {
                }
                kotlinx.coroutines.delay(5000) // 每5秒更新一次
            }
        }
    }
}

// ============================================================================
// 开机自启接收器
// ============================================================================

class BootReceiver : android.content.BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "收到开机广播，启动FSU服务")
            FSUForegroundService.start(context)
        }
    }
}
