DROP TABLE IF EXISTS `vehicle_status`;

CREATE TABLE `vehicle_status` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(128) NOT NULL,
  `bluetooth_device` varchar(128) NOT NULL,
  `engine_status` varchar(32) NOT NULL,
  `timestamp` datetime NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `speed` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

LOCK TABLES `vehicle_status` WRITE;

UNLOCK TABLES;

