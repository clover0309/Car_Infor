package com.example.vehicletracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiService {
    companion object {
        private const val TAG = "ApiService"
        // 백엔드 서버 주소 (Spring Boot)
        private const val BASE_URL = "http://192.168.1.219:8080/api/vehicle"
    }

    // 서버 연결 테스트
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/test")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d(TAG, "연결 테스트 응답 코드: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d(TAG, "연결 테스트 성공: $response")
                    true
                } else {
                    Log.e(TAG, "연결 테스트 실패: HTTP $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "연결 테스트 중 오류", e)
                false
            }
        }
    }

    suspend fun getLatestVehicleStatus(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/current") // 최신 상태를 가져오는 엔드포인트
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // 성공적으로 데이터를 받아왔을 때
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    // 실패했을 때
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching latest status: ${e.message}")
                null
            }
        }
    }

    // 테스트 데이터 전송
    suspend fun sendTestData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/status")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // 테스트 데이터 생성
                val testData = JSONObject().apply {
                    put("deviceId", "android-emulator-test")
                    put("bluetoothDevice", "Test Device")
                    put("engineStatus", "ON")
                    put("speed", 50.5)
                    put("location", JSONObject().apply {
                        put("latitude", 37.5665)
                        put("longitude", 126.9780)
                    })
                }

                Log.d(TAG, "전송할 데이터: $testData")

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(testData.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                Log.d(TAG, "데이터 전송 응답 코드: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    Log.d(TAG, "데이터 전송 성공: $response")
                    true
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                    Log.e(TAG, "데이터 전송 실패: $errorResponse")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "데이터 전송 중 오류", e)
                false
            }
        }
    }
}