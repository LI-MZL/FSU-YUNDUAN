package com.fsu.native.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fsu.native.FSUApplication
import com.fsu.native.model.*
import com.fsu.native.ui.theme.TowerGreen
import com.fsu.native.ui.theme.TowerRed
import com.fsu.native.ui.theme.TowerYellow
import com.fsu.native.util.DeviceIdGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设备页 - 设备列表、信号点列表、手动告警触发
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository

    val smartDevices = remember { mutableStateOf(repository.getSmartDevices()) }
    val selectedDevice = remember { mutableStateOf<SmartDevice?>(null) }
    val showAlarmDialog = remember { mutableStateOf(false) }
    val alarmDeviceItem = remember { mutableStateOf<SmartDeviceItem?>(null) }

    // 默认设备列表（如果为空则创建模拟数据）
    LaunchedEffect(Unit) {
        if (smartDevices.value.isEmpty()) {
            createDefaultDevices(repository)
            smartDevices.value = repository.getSmartDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    createDefaultDevices(repository)
                    smartDevices.value = repository.getSmartDevices()
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedDevice.value == null) {
                // 设备列表视图
                DeviceListView(
                    devices = smartDevices.value,
                    onDeviceClick = { device ->
                        selectedDevice.value = device
                    }
                )
            } else {
                // 信号点列表视图
                SignalPointView(
                    device = selectedDevice.value!!,
                    onBack = { selectedDevice.value = null },
                    onTriggerAlarm = { item ->
                        alarmDeviceItem.value = item
                        showAlarmDialog.value = true
                    }
                )
            }
        }
    }

    // 告警弹窗
    if (showAlarmDialog.value && alarmDeviceItem.value != null) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog.value = false },
            title = { Text("触发告警") },
            text = {
                Column {
                    Text("${alarmDeviceItem.value!!.name}: ${alarmDeviceItem.value!!.value}${alarmDeviceItem.value!!.unit}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("选择告警操作:")
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        triggerAlarm(repository, alarmDeviceItem.value!!, AlarmFlag.START)
                        showAlarmDialog.value = false
                    }) {
                        Text("告警发生", color = TowerRed)
                    }
                    TextButton(onClick = {
                        triggerAlarm(repository, alarmDeviceItem.value!!, AlarmFlag.END)
                        showAlarmDialog.value = false
                    }) {
                        Text("告警恢复", color = TowerGreen)
                    }
                    TextButton(onClick = { showAlarmDialog.value = false }) {
                        Text("取消")
                    }
                }
            }
        )
    }
}

@Composable
private fun DeviceListView(
    devices: List<SmartDevice>,
    onDeviceClick: (SmartDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            DeviceCard(
                device = device,
                onClick = { onDeviceClick(device) }
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: SmartDevice,
    onClick: () -> Unit
) {
    val icon = when (device.deviceType) {
        SmartDeviceType.POWER -> Icons.Default.Power
        SmartDeviceType.BATTERY -> Icons.Default.BatteryFull
        SmartDeviceType.AC -> Icons.Default.AcUnit
        SmartDeviceType.SMOKE -> Icons.Default.SmokeFree
        SmartDeviceType.WATERTEMP -> Icons.Default.Thermostat
        SmartDeviceType.WATER -> Icons.Default.WaterDrop
        SmartDeviceType.DOOR -> Icons.Default.DoorFront
        SmartDeviceType.THUMBTEMP -> Icons.Default.Thermostat
    }

    val statusColor = if (device.enabled) TowerGreen else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = device.name,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${device.devId} | ${device.deviceType.value}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "信号点: ${device.items.size}个",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = device.enabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TowerGreen
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignalPointView(
    device: SmartDevice,
    onBack: () -> Unit,
    onTriggerAlarm: (SmartDeviceItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 设备信息头
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        InfoItem("设备ID", device.devId)
                        InfoItem("设备类型", device.deviceType.value)
                        InfoItem("信号点数量", "${device.items.size}")
                    }
                }
            }

            // 信号点列表头
            item {
                Text(
                    text = "信号点列表",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 各信号点
            items(device.items) { item ->
                SignalItemCard(
                    item = item,
                    onTriggerAlarm = { onTriggerAlarm(item) }
                )
            }
        }
    }
}

@Composable
private fun SignalItemCard(
    item: SmartDeviceItem,
    onTriggerAlarm: () -> Unit
) {
    val valueNum = item.value.toFloatOrNull() ?: 0f
    val maxNum = item.max.toFloatOrNull() ?: Float.MAX_VALUE
    val isOverLimit = valueNum > maxNum

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOverLimit) TowerRed.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "ID: ${item.itemId} | 范围: ${item.min}~${item.max}${item.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.value}${item.unit}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverLimit) TowerRed else MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onTriggerAlarm) {
                    Text("触发告警", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun createDefaultDevices(repository: FSURepository) {
    val params = repository.getFSUParams()
    val fsuId = params.fsuId.ifEmpty { "00000000000000" }

    val devices = listOf(
        createDevice(repository, fsuId, "开关电源-1", SmartDeviceType.POWER, listOf(
            SmartDeviceItem(id = "1", name = "电压", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.POWER), 0), unit = "V", value = "53.5", min = "46", max = "57"),
            SmartDeviceItem(id = "2", name = "电流", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.POWER), 1), unit = "A", value = "12.3", min = "0", max = "50"),
            SmartDeviceItem(id = "3", name = "负载电压", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.POWER), 2), unit = "V", value = "48.2", min = "42", max = "58")
        )),
        createDevice(repository, fsuId, "蓄电池组-1", SmartDeviceType.BATTERY, listOf(
            SmartDeviceItem(id = "1", name = "电池电压", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.BATTERY), 0), unit = "V", value = "48.6", min = "43", max = "56"),
            SmartDeviceItem(id = "2", name = "电池温度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.BATTERY), 1), unit = "C", value = "25.3", min = "-10", max = "55"),
            SmartDeviceItem(id = "3", name = "内阻", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.BATTERY), 2), unit = "mOhm", value = "15.2", min = "0", max = "30")
        )),
        createDevice(repository, fsuId, "空调-1", SmartDeviceType.AC, listOf(
            SmartDeviceItem(id = "1", name = "送风温度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.AC), 0), unit = "C", value = "18.5", min = "5", max = "35"),
            SmartDeviceItem(id = "2", name = "回风温度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.AC), 1), unit = "C", value = "24.2", min = "5", max = "40")
        )),
        createDevice(repository, fsuId, "烟感-1", SmartDeviceType.SMOKE, listOf(
            SmartDeviceItem(id = "1", name = "烟雾浓度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.SMOKE), 0), unit = "", value = "0", min = "0", max = "100")
        )),
        createDevice(repository, fsuId, "水浸-1", SmartDeviceType.WATER, listOf(
            SmartDeviceItem(id = "1", name = "水浸状态", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.WATER), 0), unit = "", value = "0", min = "0", max = "1")
        )),
        createDevice(repository, fsuId, "门禁-1", SmartDeviceType.DOOR, listOf(
            SmartDeviceItem(id = "1", name = "门状态", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.DOOR), 0), unit = "", value = "0", min = "0", max = "1")
        )),
        createDevice(repository, fsuId, "温湿度-1", SmartDeviceType.WATERTEMP, listOf(
            SmartDeviceItem(id = "1", name = "温度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.WATERTEMP), 0), unit = "C", value = "24.8", min = "-20", max = "60"),
            SmartDeviceItem(id = "2", name = "湿度", itemId = DeviceIdGenerator.deriveItemId(DeviceIdGenerator.deriveDevId(fsuId, SmartDeviceType.WATERTEMP), 1), unit = "%", value = "45.6", min = "0", max = "100")
        ))
    )

    repository.saveSmartDevices(devices)
}

private fun createDevice(repository: FSURepository, fsuId: String, name: String, type: SmartDeviceType, items: List<SmartDeviceItem>): SmartDevice {
    val devId = DeviceIdGenerator.deriveDevId(fsuId, type)
    return SmartDevice(
        id = devId,
        name = name,
        deviceType = type,
        enabled = true,
        devId = devId,
        items = items
    )
}

private suspend fun triggerAlarm(repository: FSURepository, item: SmartDeviceItem, flag: AlarmFlag) {
    val params = repository.getFSUParams()
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val alarm = AlarmRecord(
        devId = params.fsuId,
        devCode = params.fsuCode,
        alarmTime = now,
        fsuId = params.fsuId,
        fsuCode = params.fsuCode,
        itemId = item.itemId,
        alarmLevel = AlarmLevel.LEVEL2,
        alarmFlag = flag,
        alarmDesc = "${item.name} ${if (flag == AlarmFlag.START) "告警发生" else "告警恢复"}: ${item.value}${item.unit}"
    )
    repository.insertAlarm(alarm)
}
