package com.fsu.native.server

import android.util.Log
import com.fsu.native.model.DeviceSignal
import com.fsu.native.model.SignalType
import fi.iki.elonen.NanoHTTPD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * FSU HTTP服务器 - 基于NanoHTTPD
 * 模拟真实FSU设备的HTTP接口，接收SC下发的GET_DATA等请求
 * 同时提供本地管理接口
 */
class FSUHttpServer(port: Int = 8080) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "FSUHttpServer"
    }

    // 静态存储 - FSU基本信息
    var fsuId: String = ""
        set(value) {
            field = value
            Log.d(TAG, "FSU ID updated: $value")
        }

    var fsuCode: String = ""
        set(value) {
            field = value
            Log.d(TAG, "FSU Code updated: $value")
        }

    // 设备信号数据 (devId -> List<DeviceSignal>)
    private val deviceSignals = ConcurrentHashMap<String, List<DeviceSignal>>()

    // 告警列表
    private val alarmList = mutableListOf<String>()
        get() = synchronized(this) { ArrayList(get()) }

    fun setDeviceSignals(devId: String, signals: List<DeviceSignal>) {
        deviceSignals[devId] = signals
    }

    fun getDeviceSignals(devId: String): List<DeviceSignal> {
        return deviceSignals[devId] ?: emptyList()
    }

    fun getAllDeviceSignals(): Map<String, List<DeviceSignal>> {
        return deviceSignals.toMap()
    }

    fun addAlarm(alarmXml: String) {
        synchronized(alarmList) {
            alarmList.add(alarmXml)
        }
    }

    fun getAlarms(): List<String> = alarmList

    fun clearAlarms() {
        synchronized(alarmList) {
            alarmList.clear()
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        Log.d(TAG, "收到请求: $method $uri")

        return when {
            // SC下发GET_DATA请求
            (method == Method.POST && (uri == "/services/FSUService" || uri == "/FSUService")) -> {
                handleGetDataRequest(session)
            }

            // 更新FSU信息
            (method == Method.POST && uri == "/updateFsuInfo") -> {
                handleUpdateFsuInfo(session)
            }

            // 更新设备数据
            (method == Method.POST && uri == "/updateData") -> {
                handleUpdateData(session)
            }

            // 更新告警
            (method == Method.POST && uri == "/updateAlarms") -> {
                handleUpdateAlarms(session)
            }

            // WSDL查询
            (method == Method.GET && uri.contains("wsdl")) -> {
                handleWsdlRequest(uri)
            }

            // GET数据查询
            (method == Method.GET && uri == "/getData") -> {
                handleGetDataRequest(session)
            }

            // GET FSU信息
            (method == Method.GET && uri == "/getFsuInfo") -> {
                newFixedLengthResponse(Response.Status.OK, "text/xml", buildGetFsuInfoAck())
            }

            // 健康检查
            (method == Method.GET && uri == "/health") -> {
                newFixedLengthResponse(Response.Status.OK, "application/json", """{"status":"running","fsuId":"$fsuId","fsuCode":"$fsuCode","devices":${deviceSignals.size},"alarms":${alarmList.size}}""")
            }

            else -> {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found: $uri")
            }
        }
    }

    /**
     * 处理SC下发的GET_DATA请求
     */
    private fun handleGetDataRequest(session: IHTTPSession): Response {
        val body = parsePostBody(session)
        Log.d(TAG, "GET_DATA请求体: ${body.take(300)}")
        val ackXml = buildGetDataAck()
        return newFixedLengthResponse(Response.Status.OK, "text/xml; charset=utf-8", ackXml)
    }

    /**
     * 处理更新FSU信息请求
     */
    private fun handleUpdateFsuInfo(session: IHTTPSession): Response {
        val body = parsePostBody(session)
        // 格式: fsuId:fsuCode
        val parts = body.split(":")
        if (parts.size >= 2) {
            fsuId = parts[0].trim()
            fsuCode = parts[1].trim()
            Log.d(TAG, "更新FSU信息: $fsuId / $fsuCode")
        }
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
    }

    /**
     * 处理更新设备数据请求
     */
    private fun handleUpdateData(session: IHTTPSession): Response {
        val body = parsePostBody(session)
        // 格式: devId:devCode:type:id:value:status;devId:devCode:type:id:value:status;...
        val entries = body.split(";").filter { it.isNotBlank() }
        for (entry in entries) {
            val fields = entry.split(":")
            if (fields.size >= 6) {
                val devId = fields[0]
                val devCode = fields[1]
                val type = SignalType.fromValue(fields[2])
                val id = fields[3]
                val value = fields[4]
                val status = fields[5]

                val signal = DeviceSignal(
                    devId = devId,
                    devCode = devCode,
                    type = type,
                    id = id,
                    measuredVal = value,
                    setupVal = value,
                    status = status
                )

                val existing = deviceSignals[devId]?.toMutableList() ?: mutableListOf()
                // 更新或添加信号
                val idx = existing.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    existing[idx] = signal
                } else {
                    existing.add(signal)
                }
                deviceSignals[devId] = existing
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
    }

    /**
     * 处理更新告警请求
     */
    private fun handleUpdateAlarms(session: IHTTPSession): Response {
        val body = parsePostBody(session)
        addAlarm(body)
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
    }

    /**
     * 处理WSDL请求
     */
    private fun handleWsdlRequest(uri: String): Response {
        val wsdl = """<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://schemas.xmlsoap.org/wsdl/"
             xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
             xmlns:tns="http://tempuri.org/"
             xmlns:xsd="http://www.w3.org/2001/XMLSchema"
             targetNamespace="http://tempuri.org/"
             name="FSUService">
    <types>
        <xsd:schema targetNamespace="http://tempuri.org/">
            <xsd:element name="LOGIN" type="xsd:string"/>
            <xsd:element name="LOGOUT" type="xsd:string"/>
            <xsd:element name="HEART_BEAT" type="xsd:string"/>
            <xsd:element name="SEND_ALARM" type="xsd:string"/>
            <xsd:element name="REPORT_DATA" type="xsd:string"/>
            <xsd:element name="GET_DATA" type="xsd:string"/>
            <xsd:element name="GET_FSU_INFO" type="xsd:string"/>
        </xsd:schema>
    </types>
    <message name="LOGINRequest"><part name="parameters" element="tns:LOGIN"/></message>
    <message name="LOGINResponse"><part name="parameters" type="xsd:string"/></message>
    <portType name="FSUServicePortType">
        <operation name="LOGIN">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="LOGOUT">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="HEART_BEAT">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="SEND_ALARM">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="REPORT_DATA">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="GET_DATA">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
        <operation name="GET_FSU_INFO">
            <input message="tns:LOGINRequest"/>
            <output message="tns:LOGINResponse"/>
        </operation>
    </portType>
    <binding type="tns:FSUServicePortType" name="FSUServiceBinding">
        <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
        <operation name="LOGIN"><soap:operation soapAction="\""/><input><soap:body use="literal"/></input><output><soap:body use="literal"/></output></operation>
    </binding>
    <service name="FSUService">
        <port binding="tns:FSUServiceBinding" name="FSUServicePort">
            <soap:address location="http://${getLocalIpAddress()}:8080/services/FSUService"/>
        </port>
    </service>
</definitions>"""
        return newFixedLengthResponse(Response.Status.OK, "text/xml; charset=utf-8", wsdl)
    }

    // ============================================================================
    // B接口响应构建
    // ============================================================================

    /**
     * 构建GET_DATA应答
     */
    fun buildGetDataAck(): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 构建设备信号XML
        val deviceListXml = StringBuilder()
        for ((devId, signals) in deviceSignals) {
            if (signals.isEmpty()) continue
            val devCode = signals.first().devCode
            val semaphoreXml = signals.joinToString("") { signal ->
                val setupVal = if (signal.setupVal.isEmpty()) signal.measuredVal else signal.setupVal
                """<TSemaphore>
<TSemaphoreID>${signal.id}</TSemaphoreID>
<TMeasuredVal>${escapeXml(signal.measuredVal)}</TMeasuredVal>
<TSetupVal>${escapeXml(setupVal)}</TSetupVal>
<TStatus>${signal.status}</TStatus>
</TSemaphore>"""
            }
            deviceListXml.append("""
<Device>
<DeviceID>$devId</DeviceID>
<DeviceCode>$devCode</DeviceCode>
$semaphoreXml
</Device>""")
        }

        // 构建告警XML
        val alarmListXml = if (alarmList.isEmpty()) {
            "<AlarmList></AlarmList>"
        } else {
            "<AlarmList>${alarmList.joinToString("") { escapeXml(it) }}</AlarmList>"
        }

        return """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header/>
   <soapenv:Body>
      <GET_DATAResponse>
         <FsuId>${fsuId}</FsuId>
         <FsuCode>${fsuCode}</FsuCode>
         <DeviceList>$deviceListXml</DeviceList>
         <TSemaphore/>
         $alarmListXml
         <Time>${now}</Time>
      </GET_DATAResponse>
   </soapenv:Body>
</soapenv:Envelope>"""
    }

    /**
     * 构建GET_FSU_INFO应答
     */
    fun buildGetFsuInfoAck(): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header/>
   <soapenv:Body>
      <GET_FSU_INFOResponse>
         <FsuId>${fsuId}</FsuId>
         <FsuCode>${fsuCode}</FsuCode>
         <FsuName>FSU-SIM-${fsuId.takeLast(6)}</FsuName>
         <SoftVer>25.2.HQ.FSU.GX.AA63.R</SoftVer>
         <BVer>2.78.00.00</BVer>
         <Time>${now}</Time>
      </GET_FSU_INFOResponse>
   </soapenv:Body>
</soapenv:Envelope>"""
    }

    /**
     * 构建心跳应答
     */
    fun buildHeartBeatAck(): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header/>
   <soapenv:Body>
      <HEART_BEATResponse>
         <FsuId>${fsuId}</FsuId>
         <FsuCode>${fsuCode}</FsuCode>
         <Time>${now}</Time>
      </HEART_BEATResponse>
   </soapenv:Body>
</soapenv:Envelope>"""
    }

    /**
     * 构建登录应答
     */
    fun buildLoginAck(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Header/>
   <soapenv:Body>
      <LOGINResponse>
         <Result>0</Result>
         <Name>LOGIN</Name>
         <Code>0</Code>
         <Desc>登录成功</Desc>
      </LOGINResponse>
   </soapenv:Body>
</soapenv:Envelope>"""
    }

    /**
     * XML转义
     */
    fun escapeXml(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // ============================================================================
    // 辅助方法
    // ============================================================================

    private fun parsePostBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"] ?: ""
    }

    private fun getLocalIpAddress(): String {
        return "127.0.0.1"
    }
}
