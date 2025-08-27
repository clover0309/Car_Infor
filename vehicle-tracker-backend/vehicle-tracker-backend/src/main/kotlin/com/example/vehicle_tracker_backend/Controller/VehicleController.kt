package com.example.vehicle_tracker_backend.controller

import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.model.VehicleStatusEntity
import com.example.vehicle_tracker_backend.service.VehicleService
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import org.slf4j.LoggerFactory

data class LocationDto(
    @JsonAlias("lat")
    val latitude: Double?,
    @JsonAlias("lon", "lng")
    val longitude: Double?
)

data class VehicleStatusResponse(
    val deviceId: String,
    val deviceName: String,
    val engineStatus: String,
    val speed: Int,
    val timestamp: LocalDateTime,
    val location: LocationDto?
)

@RestController
@RequestMapping("/api/vehicle")
class VehicleController(private val vehicleService: VehicleService) {
    private val logger = LoggerFactory.getLogger(VehicleController::class.java)

    private fun VehicleStatusEntity.toResponse(): VehicleStatusResponse {
        val deviceInfo = vehicleService.getDeviceInfoByDeviceName(this.deviceName)
        val effectiveDeviceId = deviceInfo?.deviceId ?: this.deviceId

        val locationDto: LocationDto? = if (this.latitude != null && this.longitude != null) {
            LocationDto(this.latitude, this.longitude)
        } else {
            val lastStatusWithLocById = vehicleService.getLatestStatusWithLocationByDeviceId(effectiveDeviceId)
            val lastStatusWithLoc = lastStatusWithLocById ?: vehicleService.getLatestStatusWithLocationByDeviceName(this.deviceName)
            if (lastStatusWithLoc != null) {
                LocationDto(lastStatusWithLoc.latitude, lastStatusWithLoc.longitude)
            } else {
                val lastLoc = vehicleService.getLatestLocation(effectiveDeviceId)
                if (lastLoc != null) LocationDto(lastLoc.latitude, lastLoc.longitude) else null
            }
        }

        return VehicleStatusResponse(
            deviceId = effectiveDeviceId,
            deviceName = this.deviceName,
            engineStatus = this.engineStatus,
            speed = 0, 
            timestamp = this.timestamp,
            location = locationDto
        )
    }

    data class VehicleStatusDto(
        val deviceId: String,
        @JsonProperty("deviceName")
        @JsonAlias("bluetoothDevice")
        val deviceName: String,
        val engineStatus: String,
        val speed: Double,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val timestamp: LocalDateTime,
        val location: LocationDto?
    )

    @PostMapping("/status")
    fun updateStatus(@RequestBody statusDto: VehicleStatusDto): ResponseEntity<Map<String, String>> {
        logger.info("[수신] deviceId={}, deviceName={}, engineStatus={}, speed={}, timestamp={}, hasLocation={}",
            statusDto.deviceId,
            statusDto.deviceName,
            statusDto.engineStatus,
            statusDto.speed,
            statusDto.timestamp,
            statusDto.location != null
        )
        
        statusDto.location?.let {
            logger.info("[수신 위치] lat={}, lon={}", it.latitude, it.longitude)
            val lat = it.latitude
            val lon = it.longitude
            val inRange = (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0)
            val isZeroZero = (lat == 0.0 && lon == 0.0)
            if (!inRange || isZeroZero) {
                logger.warn("[수신 위치 경고] 유효하지 않은 좌표(lat={}, lon={})", lat, lon)
            }
        }
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