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
    fun findFirstByDeviceNameOrderByTimestampDesc(deviceName: String): VehicleStatusEntity?

    // Deterministic ordering with tie-breaker on id when timestamps are equal
    fun findAllByOrderByTimestampDescIdDesc(): List<VehicleStatusEntity>
    fun findFirstByDeviceIdOrderByTimestampDescIdDesc(deviceId: String): VehicleStatusEntity?
    fun findByDeviceIdOrderByTimestampDescIdDesc(deviceId: String): List<VehicleStatusEntity>
    fun findFirstByOrderByTimestampDescIdDesc(): VehicleStatusEntity?
    fun findFirstByDeviceNameOrderByTimestampDescIdDesc(deviceName: String): VehicleStatusEntity?

    // Latest record that contains valid coordinates
    fun findFirstByDeviceIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByTimestampDescIdDesc(deviceId: String): VehicleStatusEntity?
    fun findFirstByDeviceNameAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByTimestampDescIdDesc(deviceName: String): VehicleStatusEntity?
}
