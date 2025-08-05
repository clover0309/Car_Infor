package com.example.vehicle_tracker_backend.Controller

import com.example.vehicle_tracker_backend.Model.VehicleStatus
import com.example.vehicle_tracker_backend.Model.VehicleLocation
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    private val localDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    // VehicleStatus를 받을 때 사용할 DTO
    data class VehicleStatusDto(
        val deviceId: String,
        val bluetoothDevice: String,
        val engineStatus: String,
        val speed: Double,
        val timestamp: LocalDateTime,  // LocalDateTime으로 받음
        val location: VehicleLocation? = null
    )
    
    @PostMapping("/status")
    fun updateStatus(@RequestBody statusDto: VehicleStatusDto): Map<String, String> {
        // DTO를 VehicleStatus로 변환
        val status = VehicleStatus(
        deviceId = statusDto.deviceId,
        bluetoothDevice = statusDto.bluetoothDevice,
        engineStatus = statusDto.engineStatus,
        speed = statusDto.speed,
        timestamp = statusDto.timestamp,
        location = statusDto.location
    )
        
        statusHistory.offer(status)
        
        while (statusHistory.size > 100) {
            statusHistory.poll()
        }
        
        // 로깅
        if (status.engineStatus == "ON") {
            println("🚗 [차량 시동 ON] ${status.bluetoothDevice} - 속도: ${status.speed}km/h, 기기: ${status.deviceId}")
            if (status.location != null) {
                println("📍 [위치 정보] 위도: ${status.location.latitude}, 경도: ${status.location.longitude}")
            }
        } else {
            println("🔴 [차량 시동 OFF] ${status.bluetoothDevice} - 연결 해제됨, 기기: ${status.deviceId}")
            println("⏰ [연결 해제 시간] ${status.timestamp.format(localDateTimeFormatter)}")
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
        
        if (latestStatus != null) {
            val statusIcon = if (latestStatus.engineStatus == "ON") "🟢" else "🔴"
            println("$statusIcon [현재 상태 조회] ${latestStatus.bluetoothDevice} - ${latestStatus.engineStatus}")
        } else {
            println("❓ [현재 상태 조회] 연결된 차량 없음")
        }
        
        // VehicleStatus를 JSON으로 변환할 때 timestamp를 문자열로 변환
        val statusMap = if (latestStatus != null) {
            mapOf(
                "deviceId" to latestStatus.deviceId,
                "bluetoothDevice" to latestStatus.bluetoothDevice,
                "engineStatus" to latestStatus.engineStatus,
                "speed" to latestStatus.speed,
                "timestamp" to latestStatus.timestamp.format(localDateTimeFormatter),
                "location" to latestStatus.location
            )
        } else null
        
        return mapOf(
            "status" to statusMap,
            "hasData" to (latestStatus != null)
        )
    }
    
    @GetMapping("/history")
    fun getStatusHistory(): List<Map<String, Any?>> {
        println(" [이력 조회] 총 ${statusHistory.size}개 기록 반환")
        
        // 각 VehicleStatus를 Map으로 변환하여 timestamp를 문자열로 처리
        return statusHistory.map { status ->
            mapOf(
                "deviceId" to status.deviceId,
                "bluetoothDevice" to status.bluetoothDevice,
                "engineStatus" to status.engineStatus,
                "speed" to status.speed,
                "timestamp" to status.timestamp.format(localDateTimeFormatter),
                "location" to status.location
            )
        }
    }
    
    @GetMapping("/test")
    fun testEndpoint(): Map<String, String> {
        println("🔧 [연결 테스트] 백엔드 서버 정상 동작 중")
        return mapOf(
            "message" to "Vehicle Tracker Backend is running!",
            "timestamp" to LocalDateTime.now().format(localDateTimeFormatter),
            "totalRecords" to statusHistory.size.toString()
        )
    }
}