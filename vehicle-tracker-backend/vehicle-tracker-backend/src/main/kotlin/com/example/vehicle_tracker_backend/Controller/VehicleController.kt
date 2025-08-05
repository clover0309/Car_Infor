package com.example.vehicle_tracker_backend.Controller

import com.example.vehicle_tracker_backend.Model.VehicleStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentLinkedQueue

@RestController
@RequestMapping("/api/vehicle")
@CrossOrigin(
    origins = ["http://localhost:3000",
                "http://10.0.2.2:3000",
                "http://0.0.0.0:3000",
                "http://192.168.1.219:3000"
    ],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS],
    allowedHeaders = ["*"]
)
class VehicleController {
    
    // 메모리 기반 저장소 (최대 100개 항목 유지)
    private val statusHistory = ConcurrentLinkedQueue<VehicleStatus>()
    
    @PostMapping("/status")
    fun updateStatus(@RequestBody status: VehicleStatus): Map<String, String> {
        statusHistory.offer(status)
        
        // 최대 100개 항목만 유지
        while (statusHistory.size > 100) {
            statusHistory.poll()
        }
        
        // 엔진 상태에 따른 구분된 로깅
        if (status.engineStatus == "ON") {
            println("🚗 [차량 시동 ON] ${status.bluetoothDevice} - 속도: ${status.speed}km/h, 기기: ${status.deviceId}")
            if (status.location != null) {
                println("📍 [위치 정보] 위도: ${status.location.latitude}, 경도: ${status.location.longitude}")
            }
        } else {
            println("🔴 [차량 시동 OFF] ${status.bluetoothDevice} - 연결 해제됨, 기기: ${status.deviceId}")
            println("⏰ [연결 해제 시간] ${status.timestamp}")
        }
        
        return mapOf("message" to "Status updated successfully")
    }
    
    @GetMapping("/status")
    fun getStatusEndpointInfo(): Map<String, String> {
        return mapOf(
            "message" to "이 엔드포인트는 POST형식만 지원합니다.",
            "method" to "POST",
            "contentType" to "application/json",
            "example" to """
                {
                  "deviceId": "test-device-001",
                  "bluetoothDevice": "Car Audio XYZ", 
                  "engineStatus": "ON",
                  "speed": 45.5,
                  "location": {
                    "latitude": 37.5665,
                    "longitude": 126.9780
                  }
                }
            """.trimIndent()
        )
    }
    
    @GetMapping("/current")
    fun getCurrentStatus(): Map<String, Any?> {
        val latestStatus = statusHistory.lastOrNull()
        
        // 현재 상태 로그 출력
        if (latestStatus != null) {
            val statusIcon = if (latestStatus.engineStatus == "ON") "🟢" else "🔴"
            println("$statusIcon [현재 상태 조회] ${latestStatus.bluetoothDevice} - ${latestStatus.engineStatus}")
        } else {
            println("❓ [현재 상태 조회] 연결된 차량 없음")
        }
        
        return mapOf(
            "status" to latestStatus,
            "hasData" to (latestStatus != null)
        )
    }
    
    @GetMapping("/history")
    fun getStatusHistory(): List<VehicleStatus> {
        val historyList = statusHistory.toList()
        println("📊 [이력 조회] 총 ${historyList.size}개 기록 반환")
        return historyList
    }
    
    @GetMapping("/test")
    fun testEndpoint(): Map<String, String> {
        println("🔧 [연결 테스트] 백엔드 서버 정상 동작 중")
        return mapOf(
            "message" to "Vehicle Tracker Backend is running!",
            "timestamp" to java.time.LocalDateTime.now().toString(),
            "totalRecords" to statusHistory.size.toString()
        )
    }
}