package com.fsu.native.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fsu.native.ui.screens.*

/**
 * 应用导航 - 17个页面路由
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        // 首页
        composable("home") {
            HomeScreen(navController = navController)
        }

        // 设备页
        composable("device") {
            DeviceScreen(navController = navController)
        }

        // 网络页
        composable("network") {
            NetworkScreen(navController = navController)
        }

        // 活动告警
        composable("active_alarm") {
            ActiveAlarmScreen(navController = navController)
        }

        // 历史告警
        composable("history_alarm") {
            HistoryAlarmScreen(navController = navController)
        }

        // 配置页
        composable("config") {
            ConfigScreen(navController = navController)
        }

        // 协议日志
        composable("log") {
            ProtoLogScreen(navController = navController)
        }

        // 串口测试
        composable("serial_test") {
            SerialTestScreen(navController = navController)
        }

        // 业务参数
        composable("business_param") {
            BusinessParamScreen(navController = navController)
        }

        // 设备参数
        composable("device_param") {
            DeviceParamScreen(navController = navController)
        }

        // 网络参数
        composable("net_param") {
            NetParamScreen(navController = navController)
        }

        // 工程引导
        composable("project_guide") {
            ProjectGuideScreen(navController = navController)
        }

        // 设备信息
        composable("device_info") {
            DeviceInfoScreen(navController = navController)
        }

        // 进度页
        composable("prog") {
            ProgressScreen(navController = navController)
        }

        // 运行日志
        composable("run_log") {
            RunLogScreen(navController = navController)
        }

        // 我的
        composable("my") {
            MyScreen(navController = navController)
        }

        // 交维
        composable("jiaowei") {
            JiaoweiScreen(navController = navController)
        }
    }
}
