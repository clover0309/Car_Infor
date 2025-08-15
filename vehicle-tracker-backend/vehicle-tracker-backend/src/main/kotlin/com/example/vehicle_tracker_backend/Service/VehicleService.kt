package com.example.vehicle_tracker_backend.service

import com.example.vehicle_tracker_backend.model.DeviceInfoEntity
import com.example.vehicle_tracker_backend.model.DeviceLocationEntity
import com.example.vehicle_tracker_backend.model.VehicleStatusEntity
import com.example.vehicle_tracker_backend.repository.DeviceLocationRepository
import com.example.vehicle_tracker_backend.repository.DeviceInfoRepository
import com.example.vehicle_tracker_backend.repository.VehicleStatusRepository

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import org.slf4j.LoggerFactory

@Service
class VehicleService(
    private val vehicleStatusRepository: VehicleStatusRepository,
    private val deviceInfoRepository: DeviceInfoRepository,
    private val deviceLocationRepository: DeviceLocationRepository
) {
    private val logger = LoggerFactory.getLogger(VehicleService::class.java)
    
    /**
     * ANDROID_ID 패턴인지 확인하는 함수
     * ANDROID_ID는 일반적으로 16자리 16진수 문자열
     */
    private fun isAndroidIdPattern(deviceId: String): Boolean {
        val androidIdPattern = "[0-9a-f]{16}".toRegex(RegexOption.IGNORE_CASE)
        return androidIdPattern.matches(deviceId)
    }

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
        try {
            // ANDROID_ID 패턴 확인
            val isAndroidId = isAndroidIdPattern(status.deviceId)
            
            if (isAndroidId) {
                logger.warn("ANDROID_ID 패턴이 감지됨: ${status.deviceId}, 기기 이름: ${status.deviceName}")
            }
            
            // 디바이스 이름으로 실제 등록된 디바이스 정보 조회
            val deviceInfo = if (status.deviceName != "Unknown Device") {
                getDeviceInfoByDeviceName(status.deviceName)
            } else {
                null
            }
            
            // 실제 디바이스 ID를 사용하여 새로운 엔티티 생성
            val entityToSave = if (deviceInfo != null) {
                // 등록된 디바이스 정보가 있으면 해당 디바이스 ID 사용
                VehicleStatusEntity(
                    deviceId = deviceInfo.deviceId,
                    deviceName = status.deviceName,
                    engineStatus = status.engineStatus,
                    latitude = status.latitude,
                    longitude = status.longitude,
                    timestamp = status.timestamp
                )
            } else if (isAndroidId) {
                // ANDROID_ID 패턴이면서 등록된 디바이스 정보가 없는 경우
                // 임시 ID 생성 및 로깅
                val tempId = "TEMP_" + UUID.randomUUID().toString().substring(0, 8)
                logger.info("임시 디바이스 ID 생성: $tempId, 원본 ANDROID_ID: ${status.deviceId}")
                
                VehicleStatusEntity(
                    deviceId = tempId,
                    deviceName = status.deviceName,
                    engineStatus = status.engineStatus,
                    latitude = status.latitude,
                    longitude = status.longitude,
                    timestamp = status.timestamp
                )
            } else {
                // 등록된 디바이스 정보가 없으면서 ANDROID_ID 패턴도 아닌 경우 원래 상태 사용
                status
            }
            
            // 차량 상태 저장
            vehicleStatusRepository.save(entityToSave)
            logger.info("차량 상태 저장 성공: 기기 ID: ${entityToSave.deviceId}, 상태: ${entityToSave.engineStatus}")

            // 시동이 꺼졌을 때(OFF) 마지막 위치 정보 저장 또는 업데이트
            if (entityToSave.engineStatus == "OFF") {
                saveDeviceLocation(entityToSave, deviceInfo, isAndroidId, status)
            }
        } catch (e: Exception) {
            // 전체 트랜잭션 오류 처리
            logger.error("[차량 상태 저장 오류] 기기 ID: ${status.deviceId}, 기기 이름: ${status.deviceName}, 오류: ${e.message}")
            throw e // 예외를 다시 던져 트랜잭션이 롤백되도록 함
        }
    }
    
    /**
     * 디바이스 위치 정보 저장 (별도 트랜잭션으로 분리)
     */
    @Transactional(noRollbackFor = [Exception::class])
    fun saveDeviceLocation(entityToSave: VehicleStatusEntity, deviceInfo: DeviceInfoEntity?, isAndroidId: Boolean, originalStatus: VehicleStatusEntity) {
        try {
            // 디바이스 이름이 Unknown Device가 아니고 등록된 디바이스 정보가 있는 경우에만 처리
            if (entityToSave.deviceName != "Unknown Device" && deviceInfo != null) {
                // 위치 정보 저장
                val location = DeviceLocationEntity(
                    deviceId = deviceInfo.deviceId, // 실제 등록된 기기 ID 사용
                    deviceName = entityToSave.deviceName,
                    latitude = entityToSave.latitude ?: 0.0,
                    longitude = entityToSave.longitude ?: 0.0,
                    timestamp = entityToSave.timestamp
                )
                deviceLocationRepository.save(location)
                logger.info("위치 정보 저장 성공: 기기 ID: ${deviceInfo.deviceId}, 기기 이름: ${entityToSave.deviceName}")
            } else if (isAndroidId) {
                // ANDROID_ID로 추정되는 경우 위치 정보 저장 안함
                logger.warn("ANDROID_ID 패턴이 감지되어 위치 정보 저장을 건너뛼니다: ${originalStatus.deviceId}")
            }
        } catch (e: Exception) {
            // 위치 저장 오류는 별도 로깅만 하고 예외를 전파하지 않음 (메인 트랜잭션에 영향 없음)
            logger.error("[위치 저장 오류] 기기 ID: ${entityToSave.deviceId}, 기기 이름: ${entityToSave.deviceName}, 오류: ${e.message}")
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
