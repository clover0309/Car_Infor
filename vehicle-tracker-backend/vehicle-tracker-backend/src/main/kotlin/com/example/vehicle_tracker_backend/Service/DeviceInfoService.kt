package com.example.vehicle_tracker_backend.service

import com.example.vehicle_tracker_backend.model.DeviceInfoEntity
import com.example.vehicle_tracker_backend.repository.DeviceInfoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceInfoService(
    private val deviceInfoRepository: DeviceInfoRepository
) {
    fun existsByDeviceId(deviceId: String): Boolean =
        deviceInfoRepository.existsByDeviceId(deviceId)

    fun findByDeviceId(deviceId: String): DeviceInfoEntity? =
        deviceInfoRepository.findByDeviceId(deviceId)

    @Transactional
    fun registerDevice(deviceId: String, deviceName: String): Pair<DeviceInfoEntity, Boolean> {
        val existingDevice = findByDeviceId(deviceId)
        // 기존 기기인 경우, isNew = false
        if (existingDevice != null) {
            return Pair(existingDevice, false) 
        }
        // 새 기기인 경우, isNew = true.
        val newDevice = deviceInfoRepository.save(DeviceInfoEntity(deviceId = deviceId, deviceName = deviceName))
        return Pair(newDevice, true) 
    }

    @Transactional
    fun updateDeviceName(deviceId: String, deviceName: String): DeviceInfoEntity? {
        val device = deviceInfoRepository.findByDeviceId(deviceId)
        if (device != null) {
            device.deviceName = deviceName
            return deviceInfoRepository.save(device)
        }
        return null
    }
}
