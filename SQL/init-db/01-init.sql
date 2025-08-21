-- Vehicle Tracker Database Initialization Script

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS vehicle_tracker;
USE vehicle_tracker;

-- Create device_info table
CREATE TABLE IF NOT EXISTS device_info (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL UNIQUE,
    device_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create device_location table
CREATE TABLE IF NOT EXISTS device_location (
    idx BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_timestamp (timestamp)
);

-- Create vehicle_status table
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

-- Insert sample data (optional)
-- INSERT INTO device_info (device_id, device_name) VALUES 
-- ('sample_device_001', 'Sample Device 1'),
-- ('sample_device_002', 'Sample Device 2');

COMMIT;
