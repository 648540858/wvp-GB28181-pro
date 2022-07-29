-- MySQL dump 10.13  Distrib 8.0.29, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: wvp2
-- ------------------------------------------------------
-- Server version	8.0.29-0ubuntu0.22.04.3

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = ' + 00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `device`
--

DROP TABLE IF EXISTS `device`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device`
(
    `id`                               int                                                          NOT NULL AUTO_INCREMENT,
    `deviceId`                         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `name`                             varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `manufacturer`                     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `model`                            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `firmware`                         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `transport`                        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `streamMode`                       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `online`                           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `registerTime`                     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `keepaliveTime`                    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `ip`                               varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `createTime`                       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `updateTime`                       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `port`                             int                                                          NOT NULL,
    `expires`                          int                                                          NOT NULL,
    `subscribeCycleForCatalog`         int                                                          NOT NULL,
    `hostAddress`                      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `charset`                          varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `subscribeCycleForMobilePosition`  int                                                           DEFAULT NULL,
    `mobilePositionSubmissionInterval` int                                                           DEFAULT '5 ',
    `subscribeCycleForAlarm`           int                                                           DEFAULT NULL,
    `ssrcCheck`                        int                                                           DEFAULT '0 ',
    `geoCoordSys`                      varchar(50) COLLATE utf8mb4_general_ci                       NOT NULL,
    `treeType`                         varchar(50) COLLATE utf8mb4_general_ci                       NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `device_deviceId_uindex` (`deviceId`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 53
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_alarm`
--

DROP TABLE IF EXISTS `device_alarm`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_alarm`
(
    `id`               int                                                          NOT NULL AUTO_INCREMENT,
    `deviceId`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `channelId`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `alarmPriority`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `alarmMethod`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `alarmTime`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `alarmDescription` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `longitude`        double                                                        DEFAULT NULL,
    `latitude`         double                                                        DEFAULT NULL,
    `alarmType`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `createTime`       varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_alarm`
--

LOCK TABLES `device_alarm` WRITE;
/*!40000 ALTER TABLE `device_alarm`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_alarm`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_channel`
--

DROP TABLE IF EXISTS `device_channel`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_channel`
(
    `id`              int                                                          NOT NULL AUTO_INCREMENT,
    `channelId`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `name`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `manufacture`     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `model`           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `owner`           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `civilCode`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `block`           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `address`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `parentId`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `safetyWay`       int                                                           DEFAULT NULL,
    `registerWay`     int                                                           DEFAULT NULL,
    `certNum`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `certifiable`     int                                                           DEFAULT NULL,
    `errCode`         int                                                           DEFAULT NULL,
    `endTime`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `secrecy`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `ipAddress`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `port`            int                                                           DEFAULT NULL,
    `password`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `PTZType`         int                                                           DEFAULT NULL,
    `status`          int                                                           DEFAULT NULL,
    `longitude`       double                                                        DEFAULT NULL,
    `latitude`        double                                                        DEFAULT NULL,
    `streamId`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `deviceId`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `parental`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `hasAudio`        bit(1)                                                        DEFAULT NULL,
    `createTime`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `updateTime`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `subCount`        int                                                           DEFAULT '0 ',
    `longitudeGcj02`  double                                                        DEFAULT NULL,
    `latitudeGcj02`   double                                                        DEFAULT NULL,
    `longitudeWgs84`  double                                                        DEFAULT NULL,
    `latitudeWgs84`   double                                                        DEFAULT NULL,
    `businessGroupId` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `gpsTime`         varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `device_channel_id_uindex` (`id`) USING BTREE,
    UNIQUE KEY `device_channel_pk` (`channelId`, `deviceId`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 19496
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_channel`
--

LOCK TABLES `device_channel` WRITE;
/*!40000 ALTER TABLE `device_channel`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_channel`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_mobile_position`
--

DROP TABLE IF EXISTS `device_mobile_position`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_mobile_position`
(
    `id`             int                                                          NOT NULL AUTO_INCREMENT,
    `deviceId`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `channelId`      varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    `deviceName`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `time`           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `longitude`      double                                                       NOT NULL,
    `latitude`       double                                                       NOT NULL,
    `altitude`       double                                                        DEFAULT NULL,
    `speed`          double                                                        DEFAULT NULL,
    `direction`      double                                                        DEFAULT NULL,
    `reportSource`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `longitudeGcj02` double                                                        DEFAULT NULL,
    `latitudeGcj02`  double                                                        DEFAULT NULL,
    `longitudeWgs84` double                                                        DEFAULT NULL,
    `latitudeWgs84`  double                                                        DEFAULT NULL,
    `createTime`     varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 6956
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_mobile_position`
--

LOCK TABLES `device_mobile_position` WRITE;
/*!40000 ALTER TABLE `device_mobile_position`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `device_mobile_position`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gb_stream`
--

DROP TABLE IF EXISTS `gb_stream`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gb_stream`
(
    `gbStreamId`    int                                                           NOT NULL AUTO_INCREMENT,
    `app`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `stream`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `gbId`          varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `name`          varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `longitude`     double                                                        DEFAULT NULL,
    `latitude`      double                                                        DEFAULT NULL,
    `streamType`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `mediaServerId` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `createTime`    varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    `gpsTime`       varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    PRIMARY KEY (`gbStreamId`) USING BTREE,
    UNIQUE KEY `app` (`app`, `stream`) USING BTREE,
    UNIQUE KEY `gbId` (`gbId`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 301754
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gb_stream`
--

LOCK TABLES `gb_stream` WRITE;
/*!40000 ALTER TABLE `gb_stream`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `gb_stream`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log`
(
    `id`         int                                                           NOT NULL AUTO_INCREMENT,
    `name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `type`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `uri`        varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `address`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `result`     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `timing`     bigint                                                        NOT NULL,
    `username`   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `createTime` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 42703
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log`
--

LOCK TABLES `log` WRITE;
/*!40000 ALTER TABLE `log`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `log`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `media_server`
--

DROP TABLE IF EXISTS `media_server`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_server`
(
    `id`                      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `ip`                      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `hookIp`                  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `sdpIp`                   varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `streamIp`                varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `httpPort`                int                                                           NOT NULL,
    `httpSSlPort`             int                                                           NOT NULL,
    `rtmpPort`                int                                                           NOT NULL,
    `rtmpSSlPort`             int                                                           NOT NULL,
    `rtpProxyPort`            int                                                           NOT NULL,
    `rtspPort`                int                                                           NOT NULL,
    `rtspSSLPort`             int                                                           NOT NULL,
    `autoConfig`              int                                                           NOT NULL,
    `secret`                  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `streamNoneReaderDelayMS` int                                                           NOT NULL,
    `rtpEnable`               int                                                           NOT NULL,
    `rtpPortRange`            varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `sendRtpPortRange`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `recordAssistPort`        int                                                           NOT NULL,
    `defaultServer`           int                                                           NOT NULL,
    `createTime`              varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `updateTime`              varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `hookAliveInterval`       int                                                           NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `media_server_i` (`ip`, `httpPort`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media_server`
--

LOCK TABLES `media_server` WRITE;
/*!40000 ALTER TABLE `media_server`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `media_server`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parent_platform`
--

DROP TABLE IF EXISTS `parent_platform`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_platform`
(
    `id`                     int                                                          NOT NULL AUTO_INCREMENT,
    `enable`                 int                                                           DEFAULT NULL,
    `name`                   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `serverGBId`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `serverGBDomain`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `serverIP`               varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `serverPort`             int                                                           DEFAULT NULL,
    `deviceGBId`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `deviceIp`               varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `devicePort`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `username`               varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `password`               varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `expires`                varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `keepTimeout`            varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `transport`              varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `characterSet`           varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `catalogId`              varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `ptz`                    int                                                           DEFAULT NULL,
    `rtcp`                   int                                                           DEFAULT NULL,
    `status`                 bit(1)                                                        DEFAULT NULL,
    `startOfflinePush`       int                                                           DEFAULT '0 ',
    `administrativeDivision` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `catalogGroup`           int                                                           DEFAULT '1 ',
    `createTime`             varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    `updateTime`             varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    `treeType`               varchar(50) COLLATE utf8mb4_general_ci                       NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `parent_platform_id_uindex` (`id`) USING BTREE,
    UNIQUE KEY `parent_platform_pk` (`serverGBId`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 40
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parent_platform`
--

LOCK TABLES `parent_platform` WRITE;
/*!40000 ALTER TABLE `parent_platform`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `parent_platform`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_catalog`
--

DROP TABLE IF EXISTS `platform_catalog`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_catalog`
(
    `id`              varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `platformId`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `name`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `parentId`        varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `civilCode`       varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    `businessGroupId` varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_catalog`
--

LOCK TABLES `platform_catalog` WRITE;
/*!40000 ALTER TABLE `platform_catalog`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_catalog`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_channel`
--

DROP TABLE IF EXISTS `platform_gb_channel`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_gb_channel`
(
    `id`              int                                                          NOT NULL AUTO_INCREMENT,
    `platformId`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `catalogId`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `deviceChannelId` int                                                          NOT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 4915
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_channel`
--

LOCK TABLES `platform_gb_channel` WRITE;
/*!40000 ALTER TABLE `platform_gb_channel`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_gb_channel`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_stream`
--

DROP TABLE IF EXISTS `platform_gb_stream`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_gb_stream`
(
    `platformId` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `catalogId`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `gbStreamId` int                                                          NOT NULL,
    `id`         int                                                          NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `platform_gb_stream_pk` (`platformId`, `catalogId`, `gbStreamId`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 302149
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_stream`
--

LOCK TABLES `platform_gb_stream` WRITE;
/*!40000 ALTER TABLE `platform_gb_stream`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_gb_stream`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_proxy`
--

DROP TABLE IF EXISTS `stream_proxy`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stream_proxy`
(
    `id`                        int                                                           NOT NULL AUTO_INCREMENT,
    `type`                      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `app`                       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `stream`                    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `url`                       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `src_url`                   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `dst_url`                   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `timeout_ms`                int                                                           DEFAULT NULL,
    `ffmpeg_cmd_key`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `rtp_type`                  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `mediaServerId`             varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL,
    `enable_hls`                bit(1)                                                        DEFAULT NULL,
    `enable_mp4`                bit(1)                                                        DEFAULT NULL,
    `enable`                    bit(1)                                                        NOT NULL,
    `status`                    bit(1)                                                        NOT NULL,
    `enable_remove_none_reader` bit(1)                                                        NOT NULL,
    `createTime`                varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `name`                      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `updateTime`                varchar(50) COLLATE utf8mb4_general_ci                        DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `stream_proxy_pk` (`app`, `stream`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 66
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_proxy`
--

LOCK TABLES `stream_proxy` WRITE;
/*!40000 ALTER TABLE `stream_proxy`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `stream_proxy`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_push`
--

DROP TABLE IF EXISTS `stream_push`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stream_push`
(
    `id`               int                                                           NOT NULL AUTO_INCREMENT,
    `app`              varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `stream`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `totalReaderCount` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `originType`       int                                                          DEFAULT NULL,
    `originTypeStr`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `createTime`       varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    `aliveSecond`      int                                                          DEFAULT NULL,
    `mediaServerId`    varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `serverId`         varchar(50) COLLATE utf8mb4_general_ci                        NOT NULL,
    `pushTime`         varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    `updateTime`       varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    `status`           int                                                          DEFAULT NULL,
    `pushIng`          int                                                          DEFAULT NULL,
    `self`             int                                                          DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `stream_push_pk` (`app`, `stream`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 305415
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_push`
--

LOCK TABLES `stream_push` WRITE;
/*!40000 ALTER TABLE `stream_push`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `stream_push`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user`
(
    `id`         int                                                           NOT NULL AUTO_INCREMENT,
    `username`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `password`   varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `roleId`     int                                                           NOT NULL,
    `createTime` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `updateTime` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL,
    `pushKey`    varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `user_username_uindex` (`username`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user`
    DISABLE KEYS */;
INSERT INTO `user`
VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', 1, '2021 - 04 - 13 14:14:57', '2021 - 04 - 13 14:14:57',
        '01685cb9573ae25ec6c52142402da7c5');
/*!40000 ALTER TABLE `user`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role`
(
    `id`         int                                                          NOT NULL AUTO_INCREMENT,
    `name`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `authority`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `createTime` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `updateTime` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role`
    DISABLE KEYS */;
INSERT INTO `user_role`
VALUES (1, 'admin', '0 ', '2021 - 04 - 13 14:14:57', '2021 - 04 - 13 14:14:57');
/*!40000 ALTER TABLE `user_role`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2022-07-27 14:51:08
