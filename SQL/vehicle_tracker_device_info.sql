

DROP TABLE IF EXISTS `device_info`;

CREATE TABLE `device_info` (
  `idx` bigint NOT NULL AUTO_INCREMENT,
  `device_name` varchar(128) NOT NULL,
  `device_id` varchar(128) NOT NULL,
  PRIMARY KEY (`idx`),
  UNIQUE KEY `uq_device_id_name` (`device_id`,`device_name`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


LOCK TABLES `device_info` WRITE;

UNLOCK TABLES;
