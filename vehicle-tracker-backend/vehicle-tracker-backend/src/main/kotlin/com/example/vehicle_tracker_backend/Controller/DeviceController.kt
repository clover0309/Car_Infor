package com.example.vehicle_tracker_backend.controller

import com.example.vehicle_tracker_backend.model.DeviceInfoEntity
import com.example.vehicle_tracker_backend.service.DeviceInfoService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/device")
@CrossOrigin(
    origins = [
        "http://localhost:3000",
        "http://10.0.2.2:3000",
        "http://127.0.0.1:3000",
        "http://192.168.1.219:3000"
    ],
    methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS],
    allowedHeaders = ["*"]
)
class DeviceController(
    private val deviceInfoService: DeviceInfoService
) {
    data class DeviceRegisterRequest(
        val deviceId: String,
        val deviceName: String
    )

    @GetMapping("/exists")
    fun exists(@RequestParam deviceId: String): ResponseEntity<Boolean> =
        ResponseEntity.ok(deviceInfoService.existsByDeviceId(deviceId))

    @PostMapping("/register")
    fun register(@RequestBody req: DeviceRegisterRequest): ResponseEntity<DeviceInfoEntity> {
        val (device, isNew) = deviceInfoService.registerDevice(req.deviceId, req.deviceName)
        return if (isNew) {
            ResponseEntity.status(201).body(device)
        } else {
            ResponseEntity.ok(device)
        }
    }

    @PutMapping("/name")
    fun updateName(@RequestBody req: DeviceRegisterRequest): ResponseEntity<DeviceInfoEntity?> =
        ResponseEntity.ok(deviceInfoService.updateDeviceName(req.deviceId, req.deviceName))

    @GetMapping("/info")
    fun getInfo(@RequestParam deviceId: String): ResponseEntity<DeviceInfoEntity?> =
        ResponseEntity.ok(deviceInfoService.findByDeviceId(deviceId))
}
