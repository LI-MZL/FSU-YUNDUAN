package com.fsu.native.protocol

import com.fsu.native.model.AlarmRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * B接口协议 - 铁塔FSU与SC（Supervision Center）之间的SOAP XML通信协议实现
 * 遵循真实DAM-2160I-RH-T FSU设备的B接口XML格式
 */
object BInterfaceProtocol {

    // ============================================================================
    // 数据类
    // ============================================================================

    data class DataItem(
        val id: String,
        val value: String,
        val unit: String = "",
        val state: String = "0",
        val time: String = ""
    )

    data class DeviceInfo(
        val deviceId: String,
        val deviceCode: String,
        val deviceName: String,
        val idList: List<String>
    )

    // ============================================================================
    // SOAP封装 - 6种格式支持
    // ============================================================================

    /**
     * 构建SOAP信封
     * @param innerXml 内部XML内容
     * @param format SOAP格式 (1-6)
     * @return 完整SOAP XML字符串
     */
    fun buildSoapEnvelope(innerXml: String, format: Int = 1): String {
        return when (format) {
            1 -> """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:tem="http://tempuri.org/">
   <soapenv:Header/>
   <soapenv:Body>
      $innerXml
   </soapenv:Body>
</soapenv:Envelope>"""

            2 -> """<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
   <soap:Header/>
   <soap:Body>
      $innerXml
   </soap:Body>
</soap:Envelope>"""

            3 -> """<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      $innerXml
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"""

            4 -> """<?xml version="1.0" encoding="UTF-8"?>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/">
   <S:Header/>
   <S:Body>
      $innerXml
   </S:Body>
</S:Envelope>"""

            5 -> """<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://www.w3.org/2003/05/soap-envelope">
   <env:Header/>
   <env:Body>
      $innerXml
   </env:Body>
</env:Envelope>"""

            else -> """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header/>
   <soapenv:Body>
      $innerXml
   </soapenv:Body>
</soapenv:Envelope>"""
        }
    }

    // ============================================================================
    // LOGIN - 登录
    // ============================================================================

    fun buildLoginXml(
        fsuId: String,
        fsuCode: String,
        fsuIP: String,
        fsuPort: Int,
        username: String,
        password: String,
        deviceIds: List<String>
    ): String {
        val deviceIdsXml = deviceIds.joinToString("") { deviceId ->
            "<DeviceID>${deviceId}</DeviceID>"
        }
        return buildSoapEnvelope("""
<tem:LOGIN>
   <tem:FsuId>${fsuId}</tem:FsuId>
   <tem:FsuCode>${fsuCode}</tem:FsuCode>
   <tem:FsuIP>${fsuIP}</tem:FsuIP>
   <tem:FsuPort>${fsuPort}</tem:FsuPort>
   <tem:UserName>${username}</tem:UserName>
   <tem:Password>${password}</tem:Password>
   <tem:DeviceIDList>$deviceIdsXml</tem:DeviceIDList>
</tem:LOGIN>""")
    }

    // ============================================================================
    // LOGOUT - 登出
    // ============================================================================

    fun buildLogoutXml(fsuId: String): String {
        return buildSoapEnvelope("""
<tem:LOGOUT>
   <tem:FsuId>${fsuId}</tem:FsuId>
</tem:LOGOUT>""")
    }

    // ============================================================================
    // HEART_BEAT - 心跳
    // ============================================================================

    fun buildHeartBeatXml(fsuId: String, fsuCode: String): String {
        return buildSoapEnvelope("""
<tem:HEART_BEAT>
   <tem:FsuId>${fsuId}</tem:FsuId>
   <tem:FsuCode>${fsuCode}</tem:FsuCode>
</tem:HEART_BEAT>""")
    }

    // ============================================================================
    // SEND_ALARM - 发送告警
    // ============================================================================

    fun buildSendAlarmXml(alarm: AlarmRecord): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val alarmTime = if (alarm.alarmTime.isNotEmpty()) alarm.alarmTime else now
        return buildSoapEnvelope("""
<tem:SEND_ALARM>
   <tem:FsuId>${alarm.fsuId}</tem:FsuId>
   <tem:FsuCode>${alarm.fsuCode}</tem:FsuCode>
   <tem:DevId>${alarm.devId}</tem:DevId>
   <tem:DevCode>${alarm.devCode}</tem:DevCode>
   <tem:AlarmTime>${alarmTime}</tem:AlarmTime>
   <tem:ItemId>${alarm.itemId}</tem:ItemId>
   <tem:AlarmLevel>${alarm.alarmLevel.level}</tem:AlarmLevel>
   <tem:AlarmFlag>${if (alarm.alarmFlag.name == "START") 1 else 0}</tem:AlarmFlag>
   <tem:AlarmDesc>${alarm.alarmDesc}</tem:AlarmDesc>
</tem:SEND_ALARM>""")
    }

    // ============================================================================
    // REPORT_DATA - 上报数据
    // ============================================================================

    fun buildReportDataXml(
        fsuId: String,
        fsuCode: String,
        deviceId: String,
        deviceCode: String,
        dataItems: List<DataItem>
    ): String {
        val dataItemsXml = dataItems.joinToString("") { item ->
            val timeStr = if (item.time.isNotEmpty()) item.time else
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            """<tem:DataItem>
         <tem:Id>${item.id}</tem:Id>
         <tem:Value>${item.value}</tem:Value>
         <tem:Unit>${item.unit}</tem:Unit>
         <tem:State>${item.state}</tem:State>
         <tem:Time>${timeStr}</tem:Time>
      </tem:DataItem>"""
        }
        return buildSoapEnvelope("""
<tem:REPORT_DATA>
   <tem:FsuId>${fsuId}</tem:FsuId>
   <tem:FsuCode>${fsuCode}</tem:FsuCode>
   <tem:DeviceId>${deviceId}</tem:DeviceId>
   <tem:DeviceCode>${deviceCode}</tem:DeviceCode>
   <tem:DataItems>$dataItemsXml</tem:DataItems>
</tem:REPORT_DATA>""")
    }

    // ============================================================================
    // REPORT_DEV_INFO - 上报设备信息
    // ============================================================================

    fun buildReportDevInfoXml(
        fsuId: String,
        fsuCode: String,
        devices: List<DeviceInfo>
    ): String {
        val devicesXml = devices.joinToString("") { dev ->
            val idsXml = dev.idList.joinToString("") { id ->
                "<tem:Id>$id</tem:Id>"
            }
            """<tem:Device>
         <tem:DeviceId>${dev.deviceId}</tem:DeviceId>
         <tem:DeviceCode>${dev.deviceCode}</tem:DeviceCode>
         <tem:DeviceName>${dev.deviceName}</tem:DeviceName>
         <tem:IdList>$idsXml</tem:IdList>
      </tem:Device>"""
        }
        return buildSoapEnvelope("""
<tem:REPORT_DEV_INFO>
   <tem:FsuId>${fsuId}</tem:FsuId>
   <tem:FsuCode>${fsuCode}</tem:FsuCode>
   <tem:DeviceList>$devicesXml</tem:DeviceList>
</tem:REPORT_DEV_INFO>""")
    }

    // ============================================================================
    // GET_DATA - 获取数据（SC下发）
    // ============================================================================

    fun buildGetDataXml(fsuId: String, deviceIds: List<String>? = null): String {
        val deviceIdsXml = if (deviceIds.isNullOrEmpty()) {
            ""
        } else {
            deviceIds.joinToString("") { id ->
                "<tem:DeviceID>$id</tem:DeviceID>"
            }
        }
        return buildSoapEnvelope("""
<tem:GET_DATA>
   <tem:FsuId>${fsuId}</tem:FsuId>
   <tem:DeviceIDList>$deviceIdsXml</tem:DeviceIDList>
</tem:GET_DATA>""")
    }

    // ============================================================================
    // TIME_CHECK - 时间校验
    // ============================================================================

    fun buildTimeCheckXml(): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return buildSoapEnvelope("""
<tem:TIME_CHECK>
   <tem:Time>${now}</tem:Time>
</tem:TIME_CHECK>""")
    }

    // ============================================================================
    // GET_FSU_INFO - 获取FSU信息
    // ============================================================================

    fun buildGetFsuInfoXml(fsuId: String): String {
        return buildSoapEnvelope("""
<tem:GET_FSU_INFO>
   <tem:FsuId>${fsuId}</tem:FsuId>
</tem:GET_FSU_INFO>""")
    }

    // ============================================================================
    // 响应解析
    // ============================================================================

    /**
     * 解析SC响应
     * @return Pair(Name, Code)
     */
    fun parseResponse(soapResponse: String): Pair<String, String> {
        val nameRegex = Regex("""<Name>([^<]*)</Name>""", RegexOption.IGNORE_CASE)
        val codeRegex = Regex("""<Code>([^<]*)</Code>""", RegexOption.IGNORE_CASE)

        val name = nameRegex.find(soapResponse)?.groupValues?.get(1) ?: ""
        val code = codeRegex.find(soapResponse)?.groupValues?.get(1) ?: ""

        return Pair(name, code)
    }

    /**
     * 检查是否为未知协议响应
     */
    fun isUnknowProtocol(response: String): Boolean {
        return response.contains("UnknowProtocol", ignoreCase = true) ||
                response.contains("UnknownProtocol", ignoreCase = true) ||
                response.contains("UNKNOWN_PROTOCOL", ignoreCase = true)
    }

    // ============================================================================
    // 辅助方法
    // ============================================================================

    fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun escapeXml(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
