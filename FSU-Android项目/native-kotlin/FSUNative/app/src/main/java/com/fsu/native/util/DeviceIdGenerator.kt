package com.fsu.native.util

import com.fsu.native.model.SmartDeviceType

/**
 * 设备ID生成器 - 根据FSU ID和设备类型派生设备ID
 * 遵循真实FSU设备的ID编码规则
 */
object DeviceIdGenerator {

    companion object {
        // 设备类型码映射
        private val DEVICE_TYPE_CODES = mapOf(
            SmartDeviceType.POWER to "40",
            SmartDeviceType.BATTERY to "41",
            SmartDeviceType.AC to "42",
            SmartDeviceType.SMOKE to "02",
            SmartDeviceType.WATERTEMP to "03",
            SmartDeviceType.WATER to "04",
            SmartDeviceType.DOOR to "01"
        )
    }

    /**
     * 根据FSU ID和设备类型派生设备ID
     * 格式: 14位数字，前12位取自FSU ID后12位，后2位为设备类型码+序号
     */
    fun deriveDevId(fsuId: String, type: SmartDeviceType, index: Int = 0): String {
        val typeCode = DEVICE_TYPE_CODES[type] ?: "99"
        // 取FSU ID的后12位
        val fsuPart = fsuId.takeLast(12).padStart(12, '0')
        // 拼接类型码（2位）
        return "$fsuPart$typeCode".take(14)
    }

    /**
     * 根据设备类型生成信号点ID
     * 格式: 8位数字
     */
    fun deriveItemId(devId: String, signalIndex: Int = 0): String {
        // 使用设备ID的最后6位 + 信号点序号(2位)
        val devPart = devId.takeLast(6).padStart(6, '0')
        val signalPart = signalIndex.toString().padStart(2, '0')
        return "$devPart$signalPart"
    }

    /**
     * 验证设备ID格式 (14位数字)
     */
    fun validateDeviceId(id: String): Boolean {
        return id.length == 14 && id.all { it.isDigit() }
    }

    /**
     * 验证信号点ID格式 (8位数字)
     */
    fun validateItemId(id: String): Boolean {
        return id.length == 8 && id.all { it.isDigit() }
    }

    /**
     * 获取设备类型码
     */
    fun getDeviceTypeCode(type: SmartDeviceType): String {
        return DEVICE_TYPE_CODES[type] ?: "99"
    }

    /**
     * 根据设备ID推断设备类型
     */
    fun inferDeviceType(devId: String): SmartDeviceType {
        if (devId.length < 14) return SmartDeviceType.POWER
        val typeCode = devId.takeLast(2)
        return DEVICE_TYPE_CODES.entries.find { it.value == typeCode }?.key ?: SmartDeviceType.POWER
    }

    /**
     * 批量生成设备ID列表
     */
    fun generateDeviceIds(fsuId: String, types: List<SmartDeviceType>): List<String> {
        return types.map { type -> deriveDevId(fsuId, type) }
    }

    /**
     * 温湿度特殊处理 - 使用 "THUMBTEMP" 类型码 "05"
     */
    fun deriveThumbTempDevId(fsuId: String, index: Int = 0): String {
        val fsuPart = fsuId.takeLast(12).padStart(12, '0')
        val typeCode = "05"
        return "$fsuPart$typeCode".take(14)
    }
}
