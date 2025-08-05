package com.example.vehicle_tracker_backend.Service

import com.example.vehicle_tracker_backend.Model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.Repository.DeviceLocationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceLocationService(
    private val deviceLocationRepository: DeviceLocationRepository
) {
    @Transactional
    fun saveLocation(entity: DeviceLocationEntity): DeviceLocationEntity =
        deviceLocationRepository.save(entity)

    fun getLatestLocation(): DeviceLocationEntity? =
        deviceLocationRepository.findLatestLocation()

    fun getLatestLocationByDeviceId(deviceId: String): DeviceLocationEntity? =
        deviceLocationRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId)
}
