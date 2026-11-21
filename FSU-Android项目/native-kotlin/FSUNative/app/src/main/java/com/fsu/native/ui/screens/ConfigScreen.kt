package com.fsu.native.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fsu.native.FSUApplication
import com.fsu.native.model.*

/**
 * 配置页 - VPN/SC/FSU参数配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("FSU参数", "VPN配置", "SC配置", "串口配置")

    // FSU参数
    val fsuParams = remember { mutableStateOf(repository.getFSUParams()) }
    var fsuId by remember { mutableStateOf(fsuParams.value.fsuId) }
    var fsuCode by remember { mutableStateOf(fsuParams.value.fsuCode) }
    var fsuName by remember { mutableStateOf(fsuParams.value.fsuName) }

    // SC配置
    val scConfig = remember { mutableStateOf(repository.getSCConfig()) }
    var scIP by remember { mutableStateOf(scConfig.value.scIP) }
    var scPort by remember { mutableStateOf(scConfig.value.scPort.toString()) }
    var scUser by remember { mutableStateOf(scConfig.value.username) }
    var scPass by remember { mutableStateOf(scConfig.value.password) }

    // VPN配置
    val vpnConfig = remember { mutableStateOf(repository.getVPNConfig()) }
    var vpnServer by remember { mutableStateOf(vpnConfig.value.serverIP1) }
    var vpnUser by remember { mutableStateOf(vpnConfig.value.vpnUser) }
    var vpnPass by remember { mutableStateOf(vpnConfig.value.password) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 保存所有配置
                        val newFsuParams = fsuParams.value.copy(fsuId = fsuId, fsuCode = fsuCode, fsuName = fsuName)
                        repository.saveFSUParams(newFsuParams)

                        val newScConfig = scConfig.value.copy(
                            scIP = scIP,
                            scPort = scPort.toIntOrNull() ?: 8080,
                            username = scUser,
                            password = scPass
                        )
                        repository.saveSCConfig(newScConfig)

                        val newVpnConfig = vpnConfig.value.copy(
                            serverIP1 = vpnServer,
                            vpnUser = vpnUser,
                            password = vpnPass
                        )
                        repository.saveVPNConfig(newVpnConfig)
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FSUParamConfig(fsuId, fsuCode, fsuName,
                    onFsuIdChange = { fsuId = it },
                    onFsuCodeChange = { fsuCode = it },
                    onFsuNameChange = { fsuName = it }
                )
                1 -> VPNParamConfig(vpnServer, vpnUser, vpnPass,
                    onServerChange = { vpnServer = it },
                    onUserChange = { vpnUser = it },
                    onPassChange = { vpnPass = it }
                )
                2 -> SCParamConfig(scIP, scPort, scUser, scPass,
                    onIPChange = { scIP = it },
                    onPortChange = { scPort = it },
                    onUserChange = { scUser = it },
                    onPassChange = { scPass = it }
                )
                3 -> SerialParamConfig()
            }
        }
    }
}

@Composable
private fun FSUParamConfig(
    fsuId: String, fsuCode: String, fsuName: String,
    onFsuIdChange: (String) -> Unit,
    onFsuCodeChange: (String) -> Unit,
    onFsuNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FSU基础参数", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ConfigTextField("FSU ID (14位)", fsuId, onFsuIdChange, KeyboardType.Number)
        ConfigTextField("FSU编码", fsuCode, onFsuCodeChange, KeyboardType.Text)
        ConfigTextField("FSU名称", fsuName, onFsuNameChange, KeyboardType.Text)

        Spacer(modifier = Modifier.height(8.dp))
        Text("注: FSU ID为14位数字编码，由平台分配", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VPNParamConfig(
    server: String, user: String, pass: String,
    onServerChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("VPN配置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ConfigTextField("VPN服务器地址", server, onServerChange, KeyboardType.Text)
        ConfigTextField("VPN用户名", user, onUserChange, KeyboardType.Text)
        ConfigTextField("VPN密码", pass, onPassChange, KeyboardType.Password)

        Spacer(modifier = Modifier.height(8.dp))
        Text("VPN类型: L2TP/IPsec", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SCParamConfig(
    ip: String, port: String, user: String, pass: String,
    onIPChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUserChange: (String) -> Unit,
    onPassChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SC服务器配置", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        ConfigTextField("SC IP地址", ip, onIPChange, KeyboardType.Text)
        ConfigTextField("SC端口", port, onPortChange, KeyboardType.Number)
        ConfigTextField("SC用户名", user, onUserChange, KeyboardType.Text)
        ConfigTextField("SC密码", pass, onPassChange, KeyboardType.Password)
    }
}

@Composable
private fun SerialParamConfig() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("串口配置 (18路)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        for (i in 1..18) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("串口 $i", fontWeight = FontWeight.Medium)
                    Text("9600bps | 未配置", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfigTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
