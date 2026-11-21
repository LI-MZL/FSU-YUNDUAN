package com.fsu.native.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fsu.native.FSUApplication
import com.fsu.native.model.ProtoLog
import com.fsu.native.ui.theme.TowerGreen
import com.fsu.native.ui.theme.TowerRed
import com.fsu.native.ui.theme.TowerYellow
import kotlinx.coroutines.launch

/**
 * 协议日志页面 - 显示B接口通信日志
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtoLogScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository
    val scope = rememberCoroutineScope()

    val logs = repository.protoLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("协议日志 (${logs.value.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { repository.clearProtoLogs() }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清除")
                    }
                }
            )
        }
    ) { padding ->
        if (logs.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("暂无协议日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(logs.value) { log ->
                    ProtoLogItem(log = log)
                }
            }
        }
    }
}

@Composable
private fun ProtoLogItem(log: ProtoLog) {
    val dirColor = when (log.direction) {
        ProtoLog.Direction.SEND -> TowerGreen
        ProtoLog.Direction.RECV -> MaterialTheme.colorScheme.primary
    }
    val levelBg = when (log.level) {
        ProtoLog.Level.WARN -> TowerYellow.copy(alpha = 0.1f)
        ProtoLog.Level.SUCCESS -> TowerGreen.copy(alpha = 0.1f)
        ProtoLog.Level.DEBUG -> Color.LightGray.copy(alpha = 0.3f)
        ProtoLog.Level.INFO -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = levelBg)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = dirColor,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = log.direction.name,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = log.level.name,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = log.timestamp,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.message.take(500),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 8,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============================================================================
// 以下为其余Screen占位实现
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialTestScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("串口测试", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("串口测试功能", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("选择串口进行数据读写测试", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            for (i in 1..18) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("串口 COM$i")
                        Text("9600,N,8,1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessParamScreen(navController: NavController) {
    PlaceholderScreen("业务参数", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceParamScreen(navController: NavController) {
    PlaceholderScreen("设备参数", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetParamScreen(navController: NavController) {
    PlaceholderScreen("网络参数", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectGuideScreen(navController: NavController) {
    PlaceholderScreen("工程引导", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository
    val params = remember { repository.getFSUParams() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备信息", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoCard("FSU ID", params.fsuId)
            InfoCard("FSU编码", params.fsuCode)
            InfoCard("FSU名称", params.fsuName)
            InfoCard("FSU类型", params.fsuType.value)
            InfoCard("站点类型", params.siteType.value)
            InfoCard("工作状态", params.workState.value)
            InfoCard("监控ID", params.monitorId.ifEmpty { "-" })
            InfoCard("经度", params.damLongitude)
            InfoCard("纬度", params.damLatitude)
            InfoCard("海拔高度", params.damHeight + "m")
            InfoCard("软件版本", params.softVer)
            InfoCard("B版本", params.bVer)
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val onlineStatus by container.onlineManager.onlineStatus.collectAsStateWithLifecycle()
    val fsuState by container.onlineManager.fsuState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上线进度", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 步骤指示器
            val steps = listOf(
                "VPN配置" to (fsuState.loginState.value.isNotEmpty()),
                "VPN拨号" to fsuState.loginState.value.contains("VPN"),
                "获取VPN IP" to fsuState.vpnIP.isNotEmpty(),
                "SC登录" to fsuState.loginState.value.contains("登录"),
                "心跳启动" to (fsuState.heartbeatCount > 0)
            )

            for ((stepName, completed) in steps) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (completed) TowerGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stepName, fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("当前状态:", fontWeight = FontWeight.Bold)
            Text(onlineStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunLogScreen(navController: NavController) {
    PlaceholderScreen("运行日志", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("铁塔FSU模拟器", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("版本: 25.2.HQ.FSU.GX.AA63.R", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("B版本: 2.78.00.00", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("设备信息") },
                leadingContent = { Icon(Icons.Default.Info, null) },
                modifier = Modifier.clickable { navController.navigate("device_info") }
            )
            ListItem(
                headlineContent = { Text("协议日志") },
                leadingContent = { Icon(Icons.Default.Description, null) },
                modifier = Modifier.clickable { navController.navigate("log") }
            )
            ListItem(
                headlineContent = { Text("系统配置") },
                leadingContent = { Icon(Icons.Default.Settings, null) },
                modifier = Modifier.clickable { navController.navigate("config") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JiaoweiScreen(navController: NavController) {
    PlaceholderScreen("交维", navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(title: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "$title\n功能开发中...",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
