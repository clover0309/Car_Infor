package com.example.vehicle_tracker_backend.Repository

import com.example.vehicle_tracker_backend.Model.DeviceInfoEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceInfoRepository : JpaRepository<DeviceInfoEntity, String> {
    fun existsByDeviceId(deviceId: String): Boolean
    fun findByDeviceId(deviceId: String): DeviceInfoEntity?
}
