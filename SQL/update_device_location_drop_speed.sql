-- Drop speed column from device_location as it is not persisted anymore
ALTER TABLE device_location DROP COLUMN IF EXISTS speed;
