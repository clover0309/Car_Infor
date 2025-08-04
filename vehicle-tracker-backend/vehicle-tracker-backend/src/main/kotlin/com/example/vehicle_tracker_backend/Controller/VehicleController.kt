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
        
        println("차량 상태 업데이트: $status") // 디버깅용 로그
        
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
        return mapOf(
            "status" to latestStatus,
            "hasData" to (latestStatus != null)
        )
    }
    
    @GetMapping("/history")
    fun getStatusHistory(): List<VehicleStatus> {
        return statusHistory.toList()
    }
    
    @GetMapping("/test")
    fun testEndpoint(): Map<String, String> {
        return mapOf(
            "message" to "Vehicle Tracker Backend is running!",
            "timestamp" to java.time.LocalDateTime.now().toString()
        )
    }
}