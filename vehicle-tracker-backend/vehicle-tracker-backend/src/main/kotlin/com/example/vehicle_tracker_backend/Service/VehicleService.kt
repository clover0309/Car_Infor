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
        try {
            // 중복 결과 처리: 가장 최근에 등록된 기기 정보 반환
            val results = deviceInfoRepository.findAll()
                .filter { it.deviceName == deviceName }
                .sortedByDescending { it.idx }
            
            return if (results.isNotEmpty()) {
                results.first() // 가장 최근에 등록된 기기 정보 반환
            } else {
                null
            }
        } catch (e: Exception) {
            println("[기기 정보 조회 오류] 기기 이름: $deviceName, 오류: ${e.message}")
            return null
        }
    }

    @Transactional
    fun saveVehicleStatus(status: VehicleStatusEntity) {
        vehicleStatusRepository.save(status)

        // 시동이 꺼졌을 때(OFF) 마지막 위치 정보 저장 또는 업데이트
        if (status.engineStatus == "OFF") {
            try {
                // 먼저 device_info 테이블에 해당 기기가 존재하는지 확인
                var deviceInfo = deviceInfoRepository.findByDeviceId(status.deviceId)
                
                // 기기 ID는 존재하지만 이름이 다른 경우 업데이트
                if (deviceInfo != null && deviceInfo.deviceName != status.bluetoothDevice) {
                    deviceInfo.deviceName = status.bluetoothDevice
                    deviceInfoRepository.save(deviceInfo)
                } 
                // 기기가 존재하지 않는 경우 새로 등록
                else if (deviceInfo == null) {
                    deviceInfo = DeviceInfoEntity(
                        deviceId = status.deviceId,
                        deviceName = status.bluetoothDevice
                    )
                    deviceInfoRepository.save(deviceInfo)
                }
                
                // 위치 정보 저장
                val location = DeviceLocationEntity(
                    deviceId = status.deviceId,
                    deviceName = status.bluetoothDevice,
                    latitude = status.latitude ?: 0.0,
                    longitude = status.longitude ?: 0.0,
                    timestamp = status.timestamp
                )
                deviceLocationRepository.save(location)
            } catch (e: Exception) {
                // 오류 발생 시 로그 기록
                println("[위치 저장 오류] 기기 ID: ${status.deviceId}, 기기 이름: ${status.bluetoothDevice}, 오류: ${e.message}")
            }
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
