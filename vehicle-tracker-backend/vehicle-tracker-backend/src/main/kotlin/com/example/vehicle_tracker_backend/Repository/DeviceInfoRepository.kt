package com.example.vehicle_tracker_backend.repository

import com.example.vehicle_tracker_backend.model.DeviceInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceInfoRepository : JpaRepository<DeviceInfoEntity, Long> {
    fun findByDeviceName(deviceName: String): DeviceInfoEntity?
    fun existsByDeviceId(deviceId: String): Boolean
    fun findByDeviceId(deviceId: String): DeviceInfoEntity?
    fun findAllByDeviceNameOrderByIdxDesc(deviceName: String): List<DeviceInfoEntity>
}
