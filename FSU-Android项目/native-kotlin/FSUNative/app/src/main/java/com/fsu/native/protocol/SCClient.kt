package com.fsu.native.protocol

import android.content.Context
import android.util.Log
import com.fsu.native.model.AlarmRecord
import com.fsu.native.model.ProtoLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SC通信客户端 - 使用OkHttp实现B接口通信
 * 负责FSU与SC（Supervision Center）之间的所有SOAP XML请求
 */
class SCClient(private val context: Context) {

    companion object {
        private const val TAG = "SCClient"
        private const val CONTENT_TYPE = "text/xml; charset=utf-8"
        private val XML_MEDIA_TYPE = CONTENT_TYPE.toMediaType()

        // 9个SC路径优先级列表（真实FSU按此顺序探测）
        val SC_PATHS = listOf(
            "/services/SCService",
            "/FSUService",
            "/services/FSUService",
            "/SCService",
            "/services/WebServiceSC",
            "/WebServiceSC",
            "/services/SC",
            "/SC",
            "/ws/SCService"
        )
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * SC响应数据
     */
    data class SCResponse(
        val ok: Boolean,
        val status: Int,
        val body: String,
        val bytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SCResponse) return false
            return ok == other.ok && status == other.status && body == other.body
        }

        override fun hashCode(): Int {
            var result = ok.hashCode()
            result = 31 * result + status
            result = 31 * result + body.hashCode()
            return result
        }
    }

    /**
     * 发送SOAP请求到SC
     */
    suspend fun sendRequest(
        scIP: String,
        scPort: Int,
        xmlBody: String,
        path: String,
        timeoutMs: Int = 15000
    ): SCResponse = withContext(Dispatchers.IO) {
        try {
            val url = "http://$scIP:$scPort$path"
            Log.d(TAG, "SC请求: $url")
            Log.d(TAG, "XML: ${xmlBody.take(500)}...")

            val timeoutClient = client.newBuilder()
                .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(xmlBody.toRequestBody(XML_MEDIA_TYPE))
                .header("Content-Type", CONTENT_TYPE)
                .header("SOAPAction", "\"\"")
                .build()

            val response = timeoutClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val bytes = response.body?.bytes() ?: ByteArray(0)

            Log.d(TAG, "SC响应: ${response.code} ${body.take(300)}")

            SCResponse(
                ok = response.isSuccessful,
                status = response.code,
                body = body,
                bytes = bytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "SC请求失败: ${e.message}")
            SCResponse(
                ok = false,
                status = -1,
                body = e.message ?: "Unknown error",
                bytes = ByteArray(0)
            )
        }
    }

    /**
     * 探测SC可用路径
     * @return 可用的SC路径，或null表示探测失败
     */
    suspend fun probeSC(scIP: String, scPort: Int): String? {
        // 使用TIME_CHECK作为探测请求
        val probeXml = BInterfaceProtocol.buildTimeCheckXml()

        for (path in SC_PATHS) {
            try {
                val response = sendRequest(scIP, scPort, probeXml, path, timeoutMs = 5000)
                if (response.ok) {
                    // 检查是否为有效SOAP响应
                    if (response.body.contains("<soapenv:Envelope", ignoreCase = true) ||
                        response.body.contains("<soap:Envelope", ignoreCase = true) ||
                        response.body.contains("<SOAP-ENV:Envelope", ignoreCase = true) ||
                        response.body.contains("<S:Envelope", ignoreCase = true)
                    ) {
                        Log.d(TAG, "SC路径探测成功: $path")
                        return path
                    }
                }
            } catch (_: Exception) {
                continue
            }
        }
        Log.w(TAG, "SC路径探测失败")
        return null
    }

    /**
     * FSU登录SC
     */
    suspend fun login(
        sc: SCConfig,
        fsuId: String,
        fsuCode: String,
        vpnIP: String,
        deviceIds: List<String>
    ): SCResponse {
        val xml = BInterfaceProtocol.buildLoginXml(
            fsuId = fsuId,
            fsuCode = fsuCode,
            fsuIP = vpnIP,
            fsuPort = 8080,
            username = sc.username,
            password = sc.password,
            deviceIds = deviceIds
        )
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }

    /**
     * FSU登出SC
     */
    suspend fun logout(sc: SCConfig, fsuId: String): SCResponse {
        val xml = BInterfaceProtocol.buildLogoutXml(fsuId)
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }

    /**
     * 发送心跳
     */
    suspend fun heartbeat(sc: SCConfig, fsuId: String, fsuCode: String): SCResponse {
        val xml = BInterfaceProtocol.buildHeartBeatXml(fsuId, fsuCode)
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }

    /**
     * 发送告警
     */
    suspend fun sendAlarm(sc: SCConfig, alarm: AlarmRecord): SCResponse {
        val xml = BInterfaceProtocol.buildSendAlarmXml(alarm)
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }

    /**
     * 上报数据
     */
    suspend fun reportData(
        sc: SCConfig,
        fsuId: String,
        fsuCode: String,
        deviceId: String,
        deviceCode: String,
        dataItems: List<BInterfaceProtocol.DataItem>
    ): SCResponse {
        val xml = BInterfaceProtocol.buildReportDataXml(
            fsuId = fsuId,
            fsuCode = fsuCode,
            deviceId = deviceId,
            deviceCode = deviceCode,
            dataItems = dataItems
        )
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }

    /**
     * 上报设备信息
     */
    suspend fun reportDevInfo(
        sc: SCConfig,
        fsuId: String,
        fsuCode: String,
        devices: List<BInterfaceProtocol.DeviceInfo>
    ): SCResponse {
        val xml = BInterfaceProtocol.buildReportDevInfoXml(
            fsuId = fsuId,
            fsuCode = fsuCode,
            devices = devices
        )
        val path = probeSC(sc.scIP, sc.scPort) ?: SC_PATHS[0]
        return sendRequest(sc.scIP, sc.scPort, xml, path)
    }
}
