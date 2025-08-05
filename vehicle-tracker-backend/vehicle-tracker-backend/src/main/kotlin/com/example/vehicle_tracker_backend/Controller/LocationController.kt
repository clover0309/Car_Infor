package com.example.vehicle_tracker_backend.Controller

import com.example.vehicle_tracker_backend.Model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.Service.DeviceLocationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@RestController
@RequestMapping("/api/location")
@CrossOrigin(
    origins = [
        "http://localhost:3000",
        "http://10.0.2.2:3000",
        "http://127.0.0.1:3000",
        "http://192.168.1.219:3000"
    ],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS],
    allowedHeaders = ["*"]
)
class LocationController(
    private val deviceLocationService: DeviceLocationService
) {
    data class LocationRequest(
        val deviceId: String,
        val deviceName: String,
        val latitude: Double,
        val longitude: Double,
        val timestamp: LocalDateTime? = null,
        val speed: Int = 0
    )

    @PostMapping("")
    fun saveLocation(@RequestBody req: LocationRequest): ResponseEntity<String> {
        val entity = DeviceLocationEntity(
            deviceId = req.deviceId,
            deviceName = req.deviceName,
            latitude = req.latitude,
            longitude = req.longitude,
            timestamp = req.timestamp ?: LocalDateTime.now(),
            speed = req.speed
        )
        deviceLocationService.saveLocation(entity)
        return ResponseEntity.ok("Location saved")
    }

    // deviceId별 마지막 위치 반환
    @GetMapping("/last")
    fun getLastLocation(
        @RequestParam(required = false) deviceId: String?
    ): ResponseEntity<DeviceLocationEntity?> {
        val location = if (deviceId != null)
            deviceLocationService.getLatestLocationByDeviceId(deviceId)
        else
            deviceLocationService.getLatestLocation()
        return ResponseEntity.ok(location)
    }
}
