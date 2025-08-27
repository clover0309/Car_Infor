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
        ANDROID_ID 값을 가져오는 현상으로 인한 Android ID 패턴 확인
     */
    private fun isAndroidIdPattern(deviceId: String): Boolean {
        val androidIdPattern = "[0-9a-f]{16}".toRegex(RegexOption.IGNORE_CASE)
        return androidIdPattern.matches(deviceId)
    }

    fun getDeviceInfoByDeviceName(deviceName: String): DeviceInfoEntity? {
        return try {
            // 레포지토리 메서드로 최신 등록 정보를 우선 조회
            val results = deviceInfoRepository.findAllByDeviceNameOrderByIdxDesc(deviceName)
            val found = results.firstOrNull() ?: deviceInfoRepository.findByDeviceName(deviceName)
            if (found == null) {
                logger.info("[기기 정보 없음] 기기 이름: {}", deviceName)
            }
            found
        } catch (e: Exception) {
            logger.error("[기기 정보 조회 오류] 기기 이름: {}, 오류: {}", deviceName, e.message)
            null
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
            
            // deviceId로 기존 등록된 디바이스 정보 조회 (블루투스 MAC 주소인 경우)
            val deviceInfoByDeviceId = if (!isAndroidId) {
                deviceInfoRepository.findByDeviceId(status.deviceId)
            } else {
                null
            }
            
            // deviceId로 찾지 못한 경우 deviceName으로 조회 시도
            val deviceInfo = deviceInfoByDeviceId ?: if (status.deviceName != "Unknown Device") {
                getDeviceInfoByDeviceName(status.deviceName)
            } else {
                null
            }
            
            // 마지막 상태 조회 (deviceId 기준)
            val lastStatus = if (!isAndroidId) {
                getLatestStatus(status.deviceId)
            } else {
                null
            }

            // 추가 매핑: ANDROID_ID이거나 deviceInfo가 없을 때, deviceName 기준 최신 상태 조회
            val lastByName = if (status.deviceName != "Unknown Device") {
                vehicleStatusRepository.findFirstByDeviceNameOrderByTimestampDescIdDesc(status.deviceName)
            } else null
            
            // 실제 디바이스 ID를 사용하여 새로운 엔티티 생성
            val entityToSave = if (deviceInfo != null) {
                // 등록된 디바이스 정보가 있으면 해당 디바이스 ID 사용
                VehicleStatusEntity(
                    deviceId = deviceInfo.deviceId,
                    deviceName = deviceInfo.deviceName,
                    engineStatus = status.engineStatus,
                    latitude = status.latitude,
                    longitude = status.longitude,
                    timestamp = status.timestamp
                )
            } else if (lastStatus != null && !isAndroidId) {
                // 등록된 디바이스 정보는 없지만 이전 상태가 있는 경우
                // 이전 상태의 deviceName 유지 (Unknown Device가 아닌 경우에만)
                VehicleStatusEntity(
                    deviceId = status.deviceId,
                    deviceName = if (status.deviceName != "Unknown Device") status.deviceName else lastStatus.deviceName,
                    engineStatus = status.engineStatus,
                    latitude = status.latitude,
                    longitude = status.longitude,
                    timestamp = status.timestamp
                )
            } else if (lastByName != null) {
                // deviceName으로 매핑 가능한 최근 기록이 있는 경우 해당 deviceId로 저장
                VehicleStatusEntity(
                    deviceId = lastByName.deviceId,
                    deviceName = lastByName.deviceName,
                    engineStatus = status.engineStatus,
                    latitude = status.latitude,
                    longitude = status.longitude,
                    timestamp = status.timestamp
                )
            } else if (isAndroidId) {
                // ANDROID_ID 패턴이면서 등록된 디바이스 정보가 없는 경우
                // 임시 ID 생성 및 로깅 후 더미로 저장.
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
            
            // OFF 상태이며 위/경도가 모두 null인 경우 vehicle_status 저장을 건너뛰어줌.
            val isOffWithoutLocation = entityToSave.engineStatus.equals("OFF", ignoreCase = true) &&
                (entityToSave.latitude == null && entityToSave.longitude == null)
            if (isOffWithoutLocation) {
                logger.info("[차량 상태 저장 건너뜀] OFF + 위치 없음: deviceId={}, deviceName={}, timestamp={}", entityToSave.deviceId, entityToSave.deviceName, entityToSave.timestamp)
                return
            }

            // 차량 상태 저장
            vehicleStatusRepository.save(entityToSave)
            logger.info("차량 상태 저장 성공: 기기 ID: ${entityToSave.deviceId}, 상태: ${entityToSave.engineStatus}, 디바이스 이름: ${entityToSave.deviceName}")

            // 엔진 상태와 무관하게, 유효 좌표가 있을 때 위치 정보 저장/업데이트
            val hasLatLon = (entityToSave.latitude != null && entityToSave.longitude != null)
            if (hasLatLon) {
                saveDeviceLocation(entityToSave, deviceInfo, isAndroidId, status)
            }
        } catch (e: Exception) {
            logger.error("[차량 상태 저장 오류] 기기 ID: ${status.deviceId}, 기기 이름: ${status.deviceName}, 오류: ${e.message}")
            throw e
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
                // 위치 유효성 검사: null, 범위 초과, (0.0, 0.0)은 저장하지 않음
                val lat = entityToSave.latitude
                val lon = entityToSave.longitude

                if (lat == null || lon == null) {
                    logger.warn("[위치 저장 건너뜀] 위/경도 없음: deviceId={}, deviceName={}, engineStatus={}", deviceInfo.deviceId, entityToSave.deviceName, entityToSave.engineStatus)
                    return
                }

                val inRange = lat in -90.0..90.0 && lon in -180.0..180.0
                val isZeroZero = lat == 0.0 && lon == 0.0
                if (!inRange || isZeroZero) {
                    logger.warn("[위치 저장 건너뜀] 유효하지 않은 좌표(lat={}, lon={}): deviceId={}, deviceName={}", lat, lon, deviceInfo.deviceId, entityToSave.deviceName)
                    return
                }

                // 위치 정보 저장
                val location = DeviceLocationEntity(
                    deviceId = deviceInfo.deviceId, // 실제 등록된 기기 ID 사용
                    deviceName = entityToSave.deviceName,
                    latitude = lat,
                    longitude = lon,
                    timestamp = entityToSave.timestamp
                )
                deviceLocationRepository.save(location)
                logger.info("위치 정보 저장 성공: 기기 ID: ${deviceInfo.deviceId}, 기기 이름: ${entityToSave.deviceName}, lat={}, lon={}", lat, lon)
            } else if (isAndroidId) {
                // ANDROID_ID로 추정되는 경우 위치 정보 저장 안함
                logger.warn("ANDROID_ID 패턴이 감지되어 위치 정보 저장을 건너뜁니다: ${originalStatus.deviceId}")
            }
        } catch (e: Exception) {
            // 위치 저장 오류는 별도 로깅만 하고 예외를 전파하지 않음 (메인 트랜잭션에 영향 없음)
            logger.error("[위치 저장 오류] 기기 ID: ${entityToSave.deviceId}, 기기 이름: ${entityToSave.deviceName}, 오류: ${e.message}")
        }
    }


    fun getLatestStatus(deviceId: String): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByDeviceIdOrderByTimestampDescIdDesc(deviceId)
    }

    fun getLatestStatusByDeviceName(deviceName: String): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByDeviceNameOrderByTimestampDescIdDesc(deviceName)
    }

    fun getLatestLocation(deviceId: String): DeviceLocationEntity? {
        return deviceLocationRepository.findTopByDeviceIdOrderByTimestampDesc(deviceId)
    }

    fun getLatestStatusWithLocationByDeviceId(deviceId: String): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByDeviceIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByTimestampDescIdDesc(deviceId)
    }

    fun getLatestStatusWithLocationByDeviceName(deviceName: String): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByDeviceNameAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByTimestampDescIdDesc(deviceName)
    }

    fun getAllStatuses(deviceId: String): List<VehicleStatusEntity> {
        return vehicleStatusRepository.findByDeviceIdOrderByTimestampDescIdDesc(deviceId)
    }

    fun getLatestStatusForAllDevices(): VehicleStatusEntity? {
        return vehicleStatusRepository.findFirstByOrderByTimestampDescIdDesc()
    }

    fun getAllStatuses(): List<VehicleStatusEntity> {
        return vehicleStatusRepository.findAllByOrderByTimestampDescIdDesc()
    }
}
