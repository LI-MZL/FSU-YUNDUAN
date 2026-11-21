package com.fsu.native.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fsu.native.FSUApplication
import com.fsu.native.R
import com.fsu.native.model.LoginState
import com.fsu.native.model.SerialStatus
import com.fsu.native.repository.FSURepository
import com.fsu.native.service.FSUForegroundService
import com.fsu.native.service.OnlineManager
import com.fsu.native.ui.theme.TowerGreen
import com.fsu.native.ui.theme.TowerGray
import com.fsu.native.ui.theme.TowerRed
import com.fsu.native.ui.theme.TowerYellow

/**
 * 首页 - 仿真实FSU home.html布局
 * 站点概览、网络信息、资源使用率、串口状态、上线控制
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val heartbeatState by container.heartbeatManager.state.collectAsStateWithLifecycle()
    val onlineStatus by container.onlineManager.onlineStatus.collectAsStateWithLifecycle()
    val fsuState by container.onlineManager.fsuState.collectAsStateWithLifecycle()
    val repository = container.repository

    val fsuParams = remember { mutableStateOf(repository.getFSUParams()) }
    val scConfig = remember { mutableStateOf(repository.getSCConfig()) }
    val vpnConfig = remember { mutableStateOf(repository.getVPNConfig()) }
    val macAddress = remember { repository.getMacAddress() }
    val localIP = remember { repository.getLocalIP() }

    // 资源使用率模拟
    val cpuUsage = remember { mutableFloatStateOf((15..45).random().toFloat()) }
    val ramUsage = remember { mutableFloatStateOf((30..60).random().toFloat()) }
    val diskUsage = remember { mutableFloatStateOf((20..50).random().toFloat()) }

    // 18路串口状态
    val serialStatuses = remember {
        mutableStateOf(
            List(18) { index ->
                when (index % 4) {
                    0 -> SerialStatus.ACTIVE
                    1 -> SerialStatus.IDLE
                    2 -> if ((0..3).random() == 0) SerialStatus.ERROR else SerialStatus.ACTIVE
                    else -> SerialStatus.IDLE
                }
            }
        )
    }

    val isOnline = heartbeatState.loginState == LoginState.LOGIN_SUCCESS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("铁塔FSU模拟器", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isOnline) TowerGreen else TowerGray
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("config") }) {
                        Icon(Icons.Default.Settings, contentDescription = "配置")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ========================
            // 站点概览卡片
            // ========================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "站点概览",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("FSU名称", fsuParams.value.fsuName)
                    InfoRow("FSU ID", fsuParams.value.fsuId)
                    InfoRow("FSU编码", fsuParams.value.fsuCode)
                    InfoRow("FSU类型", fsuParams.value.fsuType.value)
                    InfoRow("站点类型", fsuParams.value.siteType.value)
                    InfoRow("工作状态", fsuParams.value.workState.value)
                    InfoRow("软件版本", fsuParams.value.softVer)
                    InfoRow("B版本", fsuParams.value.bVer)
                }
            }

            // ========================
            // 网络信息卡片
            // ========================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "网络信息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("MAC地址", macAddress)
                    InfoRow("本地IP", localIP)
                    InfoRow("VPN服务器", vpnConfig.value.serverIP1.ifEmpty { "未配置" })
                    InfoRow("VPN IP", fsuState.vpnIP.ifEmpty { "未连接" })
                    InfoRow("SC地址", "${scConfig.value.scIP}:${scConfig.value.scPort}")
                    InfoRow("SC模式", heartbeatState.scMode.value)
                    InfoRow("登录状态", heartbeatState.loginState.value)
                }
            }

            // ========================
            // 资源使用率
            // ========================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "资源使用率",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ProgressBarRow("CPU", cpuUsage.floatValue)
                    ProgressBarRow("内存", ramUsage.floatValue)
                    ProgressBarRow("磁盘", diskUsage.floatValue)
                }
            }

            // ========================
            // 18路串口状态
            // ========================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "串口状态 (18路)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 串口指示灯网格
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 0..17) {
                            val status = serialStatuses.value[i]
                            val color = when (status) {
                                SerialStatus.ACTIVE -> TowerGreen
                                SerialStatus.ERROR -> TowerRed
                                SerialStatus.BLUE -> Color.Blue
                                SerialStatus.IDLE -> TowerGray
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    drawCircle(color = color)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${i + 1}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    // 图例
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LegendItem("正常", TowerGreen)
                        LegendItem("故障", TowerRed)
                        LegendItem("空闲", TowerGray)
                        LegendItem("蓝牙", Color.Blue)
                    }
                }
            }

            // ========================
            // 登录状态 & 操作按钮
            // ========================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) TowerGreen.copy(alpha = 0.1f)
                    else TowerRed.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 登录状态指示
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            drawCircle(
                                color = if (isOnline) TowerGreen else TowerRed
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "在线 - 心跳: ${heartbeatState.heartbeatCount}" else "离线",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) TowerGreen else TowerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = onlineStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 一键上线/下线按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { container.onlineManager.goOnline() },
                            modifier = Modifier.weight(1f),
                            enabled = !isOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = TowerGreen)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键上线")
                        }

                        Button(
                            onClick = { container.onlineManager.goOffline() },
                            modifier = Modifier.weight(1f),
                            enabled = isOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = TowerRed)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键下线")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProgressBarRow(label: String, usage: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${"%.0f".format(usage)}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { usage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = when {
                usage > 80f -> TowerRed
                usage > 60f -> TowerYellow
                else -> TowerGreen
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Canvas(modifier: Modifier = Modifier, onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCircle(color: Color, radius: Float = this.size.minDimension / 2) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}
