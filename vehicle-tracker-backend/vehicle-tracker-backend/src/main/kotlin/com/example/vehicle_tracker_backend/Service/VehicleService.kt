package com.example.vehicle_tracker_backend.service

import com.example.vehicle_tracker_backend.model.DeviceInfoEntity
import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.model.VehicleStatusEntity
import com.example.vehicle_tracker_backend.repository.DeviceLocationRepository
import com.example.vehicle_tracker_backend.repository.DeviceInfoRepository
import com.example.vehicle_tracker_backend.repository.VehicleStatusRepository

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VehicleService(
    private val vehicleStatusRepository: VehicleStatusRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val deviceLocationRepository: DeviceLocationRepository
) {

        fun getDeviceInfoByDeviceName(deviceName: String): DeviceInfoEntity? {
        return deviceInfoRepository.findByDeviceName(deviceName)
    }

    @Transactional
    fun saveVehicleStatus(status: VehicleStatusEntity) {
        vehicleStatusRepository.save(status)

        // 시동이 꺼졌을 때(OFF) 마지막 위치 정보 저장 또는 업데이트
        if (status.engineStatus == "OFF") {
            val location = DeviceLocationEntity(
                deviceId = status.deviceId,
                deviceName = status.bluetoothDevice, // VehicleStatus의 bluetoothDevice를 사용
                latitude = status.latitude ?: 0.0,
                longitude = status.longitude ?: 0.0,
                timestamp = status.timestamp
            )
            deviceLocationRepository.save(location) // UPSERT 동작 (같은 deviceId면 덮어쓰기)
        }
    }

    fun getLatestStatus(deviceId: String): VehicleStatusEntity? {
        return vehicleStatusRepository.findByDeviceIdOrderByTimestampDesc(deviceId).firstOrNull()
    }

    fun getLatestLocation(deviceId: String): DeviceLocationEntity? {
        return deviceLocationRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId)
    }

    fun getAllStatuses(deviceId: String): List<VehicleStatusEntity> {
        return vehicleStatusRepository.findByDeviceIdOrderByTimestampDesc(deviceId)
    }

    fun getLatestStatusForAllDevices(): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByOrderByTimestampDesc()
    }

    fun getAllStatuses(): List<VehicleStatusEntity> {
        return vehicleStatusRepository.findAllByOrderByTimestampDesc()
    }
}
