package com.fsu.native.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * 网络页 - 显示VPN/SC/网络状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网络状态", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("网络配置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkInfoItem("WIFI状态", "已连接")
                        NetworkInfoItem("MAC地址", "AA:BB:CC:DD:EE:FF")
                        NetworkInfoItem("本地IP", "192.168.1.100")
                        NetworkInfoItem("网关", "192.168.1.1")
                        NetworkInfoItem("DNS", "8.8.8.8")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("VPN状态", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkInfoItem("VPN类型", "L2TP")
                        NetworkInfoItem("VPN服务器", "10.0.0.1")
                        NetworkInfoItem("VPN IP", "10.100.50.1")
                        NetworkInfoItem("连接状态", "已连接")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SC连接", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        NetworkInfoItem("SC地址", "172.16.0.1:8080")
                        NetworkInfoItem("SC路径", "/services/SCService")
                        NetworkInfoItem("SC模式", "FULL")
                        NetworkInfoItem("最后心跳", "--")
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
