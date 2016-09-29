CREATE TABLE `country_test` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `code` varchar(255) NOT NULL,
  `fips104` varchar(255) DEFAULT NULL,
  `geoWorldMapId` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_5s4ptnuqtd24d4p9au2rv53qm` (`code`),
  UNIQUE KEY `UK_gcade75e0l46k395a1fyx8gu7` (`geoWorldMapId`)
);

CREATE TABLE `region_test` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `code` varchar(255) NOT NULL,
  `country.id` bigint(20) NOT NULL,
  `geoWorldMapId` int(11) NOT NULL,
  `shortCode` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_qccxjq01rw62v9j39fvmubam6` (`geoWorldMapId`),
  KEY `geoWorldMapId_Idx` (`geoWorldMapId`),
  KEY `FK_otariwhls1krq0bo780jtvbqn` (`country.id`)
);

CREATE TABLE `city_test` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` bigint(20) NOT NULL,
  `code` varchar(255) NOT NULL,
  `geoWorldMapId` int(11) NOT NULL,
  `latitude` float NOT NULL,
  `longitude` float NOT NULL,
  `region.id` bigint(20) NOT NULL,
  `shortCode` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_clvabryjx2qhvpxb55ttsh7di` (`region.id`)
);