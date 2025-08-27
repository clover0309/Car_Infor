DROP TABLE IF EXISTS `device_location`;

CREATE TABLE `device_location` (
  `idx` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(128) NOT NULL,
  `device_name` varchar(128) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `timestamp` datetime NOT NULL,
  `speed` int NOT NULL,
  PRIMARY KEY (`idx`),
  KEY `fk_device_location_info` (`device_id`,`device_name`),
  CONSTRAINT `fk_device_location_id_name` FOREIGN KEY (`device_id`, `device_name`) REFERENCES `device_info` (`device_id`, `device_name`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_device_location_info` FOREIGN KEY (`device_id`, `device_name`) REFERENCES `device_info` (`device_id`, `device_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


LOCK TABLES `device_location` WRITE;

UNLOCK TABLES;

