package com.fsu.native.ui.screens

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
import androidx.navigation.NavController
import com.fsu.native.FSUApplication
import com.fsu.native.model.AlarmRecord
import com.fsu.native.ui.theme.TowerRed
import com.fsu.native.ui.theme.TowerYellow
import com.fsu.native.ui.theme.TowerGreen
import kotlinx.coroutines.launch

/**
 * 活动告警页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveAlarmScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository
    val scope = rememberCoroutineScope()

    val alarms = remember { mutableStateOf<List<AlarmRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            alarms.value = repository.getActiveAlarms()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("活动告警 (${alarms.value.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            repository.clearAllActiveAlarms()
                            alarms.value = repository.getActiveAlarms()
                        }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清除全部")
                    }
                }
            )
        }
    ) { padding ->
        if (alarms.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TowerGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无活动告警", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(alarms.value) { alarm ->
                    AlarmCard(alarm = alarm) {
                        scope.launch {
                            repository.clearActiveAlarm(alarm.itemId)
                            alarms.value = repository.getActiveAlarms()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 历史告警页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAlarmScreen(navController: NavController) {
    val container = FSUApplication.getInstance().container
    val repository = container.repository
    val scope = rememberCoroutineScope()

    val alarms = remember { mutableStateOf<List<AlarmRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            alarms.value = repository.getAllAlarms()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史告警", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (alarms.value.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无告警记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(alarms.value) { alarm ->
                    HistoryAlarmCard(alarm = alarm)
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: AlarmRecord, onClear: () -> Unit) {
    val levelColor = when (alarm.alarmLevel) {
        com.fsu.native.model.AlarmLevel.LEVEL1 -> TowerRed
        com.fsu.native.model.AlarmLevel.LEVEL2 -> TowerYellow
        com.fsu.native.model.AlarmLevel.LEVEL3 -> MaterialTheme.colorScheme.primary
        com.fsu.native.model.AlarmLevel.LEVEL4 -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = levelColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = levelColor,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = alarm.alarmLevel.value,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(alarm.alarmDesc, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设备: ${alarm.devId} | 信号点: ${alarm.itemId} | ${alarm.alarmTime}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "清除", tint = levelColor)
            }
        }
    }
}

@Composable
private fun HistoryAlarmCard(alarm: AlarmRecord) {
    val levelColor = when (alarm.alarmLevel) {
        com.fsu.native.model.AlarmLevel.LEVEL1 -> TowerRed
        com.fsu.native.model.AlarmLevel.LEVEL2 -> TowerYellow
        com.fsu.native.model.AlarmLevel.LEVEL3 -> MaterialTheme.colorScheme.primary
        com.fsu.native.model.AlarmLevel.LEVEL4 -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = levelColor,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = alarm.alarmLevel.value,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (alarm.alarmFlag == com.fsu.native.model.AlarmFlag.START) "[告警]" else "[恢复]",
                        fontSize = 11.sp,
                        color = if (alarm.alarmFlag == com.fsu.native.model.AlarmFlag.START) TowerRed else TowerGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(alarm.alarmDesc, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "设备: ${alarm.devId} | ${alarm.alarmTime}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
