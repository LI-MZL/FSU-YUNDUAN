package com.fsu.native.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 枚举类型
// ============================================================================

enum class FSUType(val value: String) {
    INTSTAN("综合机房监控"),
    DISSTAN("室外机柜监控"),
    IHIEXTER("室内综合监控"),
    IIHIEXTER("室内外综合监控"),
    ILOEXTER("室内智能监控"),
    IILOEXTER("室内外智能监控");

    companion object {
        fun fromValue(value: String?): FSUType = entries.find { it.value == value } ?: INTSTAN
        fun fromCode(code: String?): FSUType = when (code) {
            "1" -> INTSTAN
            "2" -> DISSTAN
            "3" -> IHIEXTER
            "4" -> IIHIEXTER
            "5" -> ILOEXTER
            "6" -> IILOEXTER
            else -> INTSTAN
        }
    }
}

enum class SiteType(val value: String) {
    SHARED("共享"),
    MOBILE("移动"),
    UNICOM("联通"),
    TELECOM("电信");

    companion object {
        fun fromValue(value: String?): SiteType = entries.find { it.value == value } ?: SHARED
        fun fromCode(code: String?): SiteType = when (code) {
            "1" -> SHARED
            "2" -> MOBILE
            "3" -> UNICOM
            "4" -> TELECOM
            else -> SHARED
        }
    }
}

enum class WorkState(val value: String) {
    NORMAL("正常运行"),
    INSTALL("安装调试");

    companion object {
        fun fromValue(value: String?): WorkState = entries.find { it.value == value } ?: NORMAL
    }
}

enum class VPNType(val value: String) {
    DISABLE("禁用"),
    PPTP("PPTP"),
    L2TP("L2TP");

    companion object {
        fun fromValue(value: String?): VPNType = entries.find { it.value == value } ?: DISABLE
    }
}

enum class AlarmLevel(val value: String, val level: Int) {
    LEVEL1("一级告警", 1),
    LEVEL2("二级告警", 2),
    LEVEL3("三级告警", 3),
    LEVEL4("四级告警", 4);

    companion object {
        fun fromLevel(level: Int): AlarmLevel = entries.find { it.level == level } ?: LEVEL4
    }
}

enum class AlarmFlag(val value: String) {
    START("告警发生"),
    END("告警恢复");

    companion object {
        fun fromValue(value: String?): AlarmFlag = entries.find { it.value == value } ?: START
    }
}

enum class SignalType(val value: String) {
    DI("DI"),
    AI("AI");

    companion object {
        fun fromValue(value: String?): SignalType = entries.find { it.value == value } ?: DI
    }
}

enum class NetworkMode(val value: String) {
    WIFI("WIFI"),
    ETHERNET("以太网"),
    LTE_4G("4G"),
    LTE_5G("5G");

    companion object {
        fun fromValue(value: String?): NetworkMode = entries.find { it.value == value } ?: ETHERNET
    }
}

enum class LoginState(val value: String) {
    OFFLINE("离线"),
    VPN_CONNECTING("VPN连接中"),
    VPN_CONNECTED("VPN已连接"),
    LOGIN_SUCCESS("登录成功"),
    LOGIN_FAIL("登录失败");

    companion object {
        fun fromValue(value: String?): LoginState = entries.find { it.value == value } ?: OFFLINE
    }
}

enum class SCMode(val value: String) {
    UNKNOWN("未知"),
    FULL("全功能"),
    LOGIN_ONLY("仅登录");

    companion object {
        fun fromValue(value: String?): SCMode = entries.find { it.value == value } ?: UNKNOWN
    }
}

enum class FsuAuthLevel(val value: String) {
    NONE("无权限"),
    READ_ONLY("只读"),
    READ_WRITE("读写");

    companion object {
        fun fromValue(value: String?): FsuAuthLevel = entries.find { it.value == value } ?: NONE
    }
}

enum class VpnState(val value: String) {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
    FAILED("连接失败");

    companion object {
        fun fromValue(value: String?): VpnState = entries.find { it.value == value } ?: DISCONNECTED
    }
}

enum class SerialStatus(val value: String) {
    ACTIVE("正常"),
    ERROR("故障"),
    IDLE("空闲"),
    BLUE("蓝牙");

    companion object {
        fun fromValue(value: String?): SerialStatus = entries.find { it.value == value } ?: IDLE
    }
}

// ============================================================================
// Room 类型转换器
// ============================================================================

class Converters {
    @TypeConverter
    fun fromFSUType(value: FSUType): String = value.name

    @TypeConverter
    fun toFSUType(value: String): FSUType = FSUType.valueOf(value)

    @TypeConverter
    fun fromAlarmLevel(value: AlarmLevel): String = value.name

    @TypeConverter
    fun toAlarmLevel(value: String): AlarmLevel = AlarmLevel.valueOf(value)

    @TypeConverter
    fun fromAlarmFlag(value: AlarmFlag): String = value.name

    @TypeConverter
    fun toAlarmFlag(value: String): AlarmFlag = AlarmFlag.valueOf(value)

    @TypeConverter
    fun fromSignalType(value: SignalType): String = value.name

    @TypeConverter
    fun toSignalType(value: String): SignalType = SignalType.valueOf(value)

    @TypeConverter
    fun fromDirection(value: ProtoLog.Direction): String = value.name

    @TypeConverter
    fun toDirection(value: String): ProtoLog.Direction = ProtoLog.Direction.valueOf(value)

    @TypeConverter
    fun fromLogLevel(value: ProtoLog.Level): String = value.name

    @TypeConverter
    fun toLogLevel(value: String): ProtoLog.Level = ProtoLog.Level.valueOf(value)
}

// ============================================================================
// FSU参数
// ============================================================================

data class FSUParams(
    var fsuId: String = "",
    var fsuCode: String = "",
    var fsuName: String = "",
    var fsuType: FSUType = FSUType.INTSTAN,
    var siteType: SiteType = SiteType.SHARED,
    var workState: WorkState = WorkState.NORMAL,
    var monitorId: String = "",
    var batteryId1: String = "",
    var batteryId2: String = "",
    var damLongitude: String = "0.000000",
    var damLatitude: String = "0.000000",
    var damHeight: String = "0",
    var softVer: String = "25.2.HQ.FSU.GX.AA63.R",
    var bVer: String = "2.78.00.00"
)

// ============================================================================
// VPN配置
// ============================================================================

data class VPNConfig(
    var vpnNet: String = "",
    var vpnType: VPNType = VPNType.DISABLE,
    var serverIP1: String = "",
    var serverIP2: String = "",
    var vpnUser: String = "",
    var password: String = "",
    var passKey: String = "",
    var effectFlag: Boolean = false
)

// ============================================================================
// SC配置
// ============================================================================

data class SCConfig(
    var name: String = "SC服务器",
    var scIP: String = "",
    var scPort: Int = 8080,
    var enabled: Boolean = true,
    var username: String = "cntower",
    var password: String = "cntower"
)

// ============================================================================
// 串口配置
// ============================================================================

data class SerialPortConfig(
    var portIndex: Int = 1,
    var enabled: Boolean = false,
    var baudRate: Int = 9600,
    var deviceName: String = "",
    var deviceType: String = "",
    var deviceCode: String = ""
)

// ============================================================================
// AI通道
// ============================================================================

data class AIChannel(
    var index: Int = 1,
    var enabled: Boolean = false,
    var name: String = "",
    var itemId: String = "",
    var unit: String = "",
    var alarmHi: Float = Float.MAX_VALUE,
    var alarmLo: Float = Float.MIN_VALUE,
    var value: Float = 0f
)

// ============================================================================
// DI通道
// ============================================================================

data class DIChannel(
    var index: Int = 1,
    var enabled: Boolean = false,
    var name: String = "",
    var itemId: String = "",
    var value: Int = 0
)

// ============================================================================
// 智能设备及子项
// ============================================================================

enum class SmartDeviceType(val value: String) {
    POWER("开关电源"),
    BATTERY("蓄电池"),
    AC("空调"),
    SMOKE("烟感"),
    WATERTEMP("水浸温湿度"),
    WATER("水浸"),
    DOOR("门禁"),
    THUMBTEMP("温湿度");

    companion object {
        fun fromValue(value: String): SmartDeviceType = entries.find { it.value == value } ?: POWER
    }
}

data class SmartDeviceItem(
    var id: String = "",
    var name: String = "",
    var itemId: String = "",
    var unit: String = "",
    var value: String = "",
    var min: String = "",
    var max: String = ""
)

data class SmartDevice(
    var id: String = "",
    var name: String = "",
    var deviceType: SmartDeviceType = SmartDeviceType.POWER,
    var enabled: Boolean = false,
    var devId: String = "",
    var items: List<SmartDeviceItem> = emptyList()
)

// ============================================================================
// 告警记录 (Room Entity)
// ============================================================================

@Entity(tableName = "alarm_records")
@TypeConverters(Converters::class)
data class AlarmRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val devId: String = "",
    val devCode: String = "",
    val alarmTime: String = "",
    val fsuId: String = "",
    val fsuCode: String = "",
    val itemId: String = "",
    val alarmLevel: AlarmLevel = AlarmLevel.LEVEL4,
    val alarmFlag: AlarmFlag = AlarmFlag.START,
    val alarmDesc: String = ""
)

// ============================================================================
// 设备信号
// ============================================================================

data class DeviceSignal(
    var devId: String = "",
    var devCode: String = "",
    var type: SignalType = SignalType.DI,
    var id: String = "",
    var measuredVal: String = "0",
    var setupVal: String = "0",
    var status: String = "0"
)

// ============================================================================
// 协议日志 (Room Entity)
// ============================================================================

@Entity(tableName = "proto_logs")
@TypeConverters(Converters::class)
data class ProtoLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: String = "",
    val direction: Direction = Direction.SEND,
    val message: String = "",
    val level: Level = Level.INFO
) {
    enum class Direction {
        SEND, RECV
    }

    enum class Level {
        INFO, WARN, SUCCESS, DEBUG
    }
}

// ============================================================================
// FSU运行状态
// ============================================================================

data class FsuState(
    var loginState: LoginState = LoginState.OFFLINE,
    var vpnIP: String = "",
    var scAuthLevel: FsuAuthLevel = FsuAuthLevel.NONE,
    var heartbeatCount: Int = 0,
    var scMode: SCMode = SCMode.UNKNOWN
) {
    val isOnline: Boolean
        get() = loginState == LoginState.LOGIN_SUCCESS

    val displayState: String
        get() = loginState.value
}
