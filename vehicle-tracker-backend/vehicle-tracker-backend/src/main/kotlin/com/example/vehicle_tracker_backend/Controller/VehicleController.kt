package com.example.vehicle_tracker_backend.controller

import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.model.VehicleStatusEntity
import com.example.vehicle_tracker_backend.service.VehicleService
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

data class LocationDto(
    val latitude: Double,
    val longitude: Double
)

data class VehicleStatusResponse(
    val deviceId: String,
    val deviceName: String,
    val engineStatus: String,
    val speed: Int, // speed 필드 추가
    val timestamp: LocalDateTime,
    val location: LocationDto?
)

@RestController
@RequestMapping("/api/vehicle")
class VehicleController(private val vehicleService: VehicleService) {

        private fun VehicleStatusEntity.toResponse(): VehicleStatusResponse {
        val deviceInfo = vehicleService.getDeviceInfoByDeviceName(this.deviceName)
        return VehicleStatusResponse(
            deviceId = deviceInfo?.deviceId ?: this.deviceId, // deviceInfo에서 찾은 실제 deviceId 사용, 없으면 기존 ID 사용
            deviceName = this.deviceName,
            engineStatus = this.engineStatus,
            speed = 0, // speed는 현재 엔티티에 없으므로 기본값 0으로 설정
            timestamp = this.timestamp,
            location = if (this.latitude != null && this.longitude != null) LocationDto(this.latitude, this.longitude) else null
        )
    }

    data class VehicleStatusDto(
        val deviceId: String,
        @JsonProperty("deviceName")
        @JsonAlias("bluetoothDevice") // 안드로이드 앱 호환성을 위해 bluetoothDevice도 deviceName으로 매핑
        val deviceName: String,
        val engineStatus: String,
        val speed: Int,
        val timestamp: LocalDateTime,
        val location: LocationDto?
    )

    @PostMapping("/status")
    fun updateStatus(@RequestBody statusDto: VehicleStatusDto): ResponseEntity<Map<String, String>> {
        val statusEntity = VehicleStatusEntity(
            deviceId = statusDto.deviceId,
            deviceName = statusDto.deviceName,
            engineStatus = statusDto.engineStatus,
            latitude = statusDto.location?.latitude,
            longitude = statusDto.location?.longitude,
            timestamp = statusDto.timestamp
        )
        vehicleService.saveVehicleStatus(statusEntity)
        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    @GetMapping("/status/{deviceId}")
    fun getLatestStatus(@PathVariable deviceId: String): ResponseEntity<VehicleStatusResponse> {
        val latestStatus = vehicleService.getLatestStatus(deviceId)
        return if (latestStatus != null) {
            ResponseEntity.ok(latestStatus.toResponse())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/location/{deviceId}")
    fun getLatestLocation(@PathVariable deviceId: String): ResponseEntity<DeviceLocationEntity> {
        val latestLocation = vehicleService.getLatestLocation(deviceId)
        return if (latestLocation != null) {
            ResponseEntity.ok(latestLocation)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/history/{deviceId}")
    fun getHistory(@PathVariable deviceId: String): ResponseEntity<List<VehicleStatusResponse>> {
        val history = vehicleService.getAllStatuses(deviceId).map { it.toResponse() }
        return ResponseEntity.ok(history)
    }

    @GetMapping("/history")
    fun getHistory(): ResponseEntity<List<VehicleStatusResponse>> {
        val history = vehicleService.getAllStatuses().map { it.toResponse() }
        return ResponseEntity.ok(history)
    }

    @GetMapping("/test")
    fun testEndpoint(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "success", "message" to "Test endpoint is working!"))
    }

    @GetMapping("/current")
    fun getCurrentStatus(): ResponseEntity<VehicleStatusResponse> {
        val latestStatus = vehicleService.getLatestStatusForAllDevices()
        return if (latestStatus != null) {
            ResponseEntity.ok(latestStatus.toResponse())
        } else {
            ResponseEntity.noContent().build()
        }
    }
}