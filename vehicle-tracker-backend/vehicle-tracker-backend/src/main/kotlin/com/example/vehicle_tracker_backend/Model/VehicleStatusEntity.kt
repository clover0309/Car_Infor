package com.example.vehicle_tracker_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "vehicle_status")
data class VehicleStatusEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "device_id", nullable = false)
    val deviceId: String = "",

    @Column(name = "device_name", nullable = false)
    val deviceName: String = "",

    @Column(name = "engine_status", nullable = false)
    val engineStatus: String = "",

    @Column(nullable = true)
    val latitude: Double? = null,

    @Column(nullable = true)
    val longitude: Double? = null,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    // DeviceInfo와의 관계 매핑 (외래키는 수동으로 DB에서 설정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        value = [
            JoinColumn(name = "device_id", referencedColumnName = "device_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)),
            JoinColumn(name = "device_name", referencedColumnName = "device_name", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
        ]
    )
    val deviceInfo: DeviceInfoEntity? = null
) {
    // DeviceInfo 관계를 통해 디바이스 이름 가져오기
    fun getDeviceNameFromRelation(): String = deviceInfo?.deviceName ?: deviceName
}
