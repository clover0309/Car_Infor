package com.example.vehicle_tracker_backend.repository

import com.example.vehicle_tracker_backend.model.VehicleStatusEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VehicleStatusRepository : JpaRepository<VehicleStatusEntity, Long> {
    fun findAllByOrderByTimestampDesc(): List<VehicleStatusEntity>
    fun findFirstByDeviceIdOrderByTimestampDesc(deviceId: String): VehicleStatusEntity?
    fun findByDeviceIdOrderByTimestampDesc(deviceId: String): List<VehicleStatusEntity>
    fun findFirstByOrderByTimestampDesc(): VehicleStatusEntity?
}
