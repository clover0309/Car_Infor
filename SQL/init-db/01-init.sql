-- 초기에 스키마가 존재하지 않으면 생성 및 지정.
CREATE DATABASE IF NOT EXISTS vehicle_tracker;
USE vehicle_tracker;

-- device_info 테이블 생성
CREATE TABLE IF NOT EXISTS device_info (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- device_location 테이블 생성
CREATE TABLE IF NOT EXISTS device_location (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_timestamp (timestamp)
);

-- vehicle_status 테이블 생성
CREATE TABLE IF NOT EXISTS vehicle_status (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    bluetooth_device VARCHAR(255),
    engine_status VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_engine_status (engine_status)
);


COMMIT;
