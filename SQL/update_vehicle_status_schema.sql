-- vehicle_status 테이블 스키마 업데이트
-- bluetooth_device 컬럼 제거 (device_name만 유지)

USE vehicle_tracker;

-- 1. 현재 테이블 구조 확인
DESCRIBE vehicle_status;

-- 2. bluetooth_device 컬럼 제거 (이미 제거되었을 수 있으므로 조건부 제거)
SELECT COUNT(*) INTO @column_exists 
FROM information_schema.columns 
WHERE table_schema = 'vehicle_tracker' 
AND table_name = 'vehicle_status' 
AND column_name = 'bluetooth_device';

SET @drop_stmt = IF(@column_exists > 0, 
                    'ALTER TABLE vehicle_status DROP COLUMN bluetooth_device', 
                    'SELECT "bluetooth_device column does not exist" AS message');

PREPARE stmt FROM @drop_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. vehicle_status 테이블에 외래키 제약조건 추가
ALTER TABLE vehicle_status 
ADD CONSTRAINT fk_vehicle_status_device_info 
FOREIGN KEY (device_id, device_name) 
REFERENCES device_info (device_id, device_name) 
ON DELETE CASCADE 
ON UPDATE CASCADE;

-- 4. 인덱스 추가 (성능 향상을 위해)
CREATE INDEX idx_vehicle_status_device ON vehicle_status (device_id, device_name);

-- 5. 변경사항 확인
DESCRIBE vehicle_status;
SHOW CREATE TABLE vehicle_status;
