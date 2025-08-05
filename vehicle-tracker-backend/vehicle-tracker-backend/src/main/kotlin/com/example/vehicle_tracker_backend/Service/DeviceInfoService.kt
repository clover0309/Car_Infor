package com.example.vehicle_tracker_backend.Service

import com.example.vehicle_tracker_backend.Model.DeviceInfoEntity
import com.example.vehicle_tracker_backend.Repository.DeviceInfoRepository
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
    fun registerDevice(deviceId: String, deviceName: String): DeviceInfoEntity =
        deviceInfoRepository.save(DeviceInfoEntity(deviceId, deviceName))

    @Transactional
    fun updateDeviceName(deviceId: String, deviceName: String): DeviceInfoEntity? {
        val device = deviceInfoRepository.findByDeviceId(deviceId)
        device?.let {
            it.deviceName = deviceName
            return deviceInfoRepository.save(it)
        }
        return null
    }
}
