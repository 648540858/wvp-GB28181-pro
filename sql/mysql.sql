-- MariaDB dump 10.19  Distrib 10.7.3-MariaDB, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: wvp3
-- ------------------------------------------------------
-- Server version	8.0.0-dmr

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `device`
--

DROP TABLE IF EXISTS `device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device` (
                          `id` int(11) NOT NULL AUTO_INCREMENT,
                          `deviceId` varchar(50) NOT NULL,
                          `name` varchar(255) DEFAULT NULL,
                          `manufacturer` varchar(255) DEFAULT NULL,
                          `model` varchar(255) DEFAULT NULL,
                          `firmware` varchar(255) DEFAULT NULL,
                          `transport` varchar(50) DEFAULT NULL,
                          `streamMode` varchar(50) DEFAULT NULL,
                          `online` varchar(50) DEFAULT NULL,
                          `registerTime` varchar(50) DEFAULT NULL,
                          `keepaliveTime` varchar(50) DEFAULT NULL,
                          `ip` varchar(50) NOT NULL,
                          `createTime` varchar(50) NOT NULL,
                          `updateTime` varchar(50) NOT NULL,
                          `port` int(11) NOT NULL,
                          `expires` int(11) NOT NULL,
                          `subscribeCycleForCatalog` int(11) NOT NULL,
                          `subscribeCycleForMobilePosition` int(11) NOT NULL,
                          `mobilePositionSubmissionInterval` int(11) NOT NULL DEFAULT '5',
                          `subscribeCycleForAlarm` int(11) NOT NULL,
                          `hostAddress` varchar(50) NOT NULL,
                          `charset` varchar(50) NOT NULL,
                          `ssrcCheck` int(11) DEFAULT '0',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `device_deviceId_uindex` (`deviceId`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device` DISABLE KEYS */;
/*!40000 ALTER TABLE `device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_alarm`
--

DROP TABLE IF EXISTS `device_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_alarm` (
                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                `deviceId` varchar(50) NOT NULL,
                                `channelId` varchar(50) NOT NULL,
                                `alarmPriority` varchar(50) NOT NULL,
                                `alarmMethod` varchar(50) DEFAULT NULL,
                                `alarmTime` varchar(50) NOT NULL,
                                `alarmDescription` varchar(255) DEFAULT NULL,
                                `longitude` double DEFAULT NULL,
                                `latitude` double DEFAULT NULL,
                                `alarmType` varchar(50) DEFAULT NULL,
                                PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_alarm`
--

LOCK TABLES `device_alarm` WRITE;
/*!40000 ALTER TABLE `device_alarm` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_alarm` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_channel`
--

DROP TABLE IF EXISTS `device_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_channel` (
                                  `id` int(11) NOT NULL AUTO_INCREMENT,
                                  `channelId` varchar(50) NOT NULL,
                                  `name` varchar(255) DEFAULT NULL,
                                  `manufacture` varchar(50) DEFAULT NULL,
                                  `model` varchar(50) DEFAULT NULL,
                                  `owner` varchar(50) DEFAULT NULL,
                                  `civilCode` varchar(50) DEFAULT NULL,
                                  `block` varchar(50) DEFAULT NULL,
                                  `address` varchar(50) DEFAULT NULL,
                                  `parentId` varchar(50) DEFAULT NULL,
                                  `safetyWay` int(11) DEFAULT NULL,
                                  `registerWay` int(11) DEFAULT NULL,
                                  `certNum` varchar(50) DEFAULT NULL,
                                  `certifiable` int(11) DEFAULT NULL,
                                  `errCode` int(11) DEFAULT NULL,
                                  `endTime` varchar(50) DEFAULT NULL,
                                  `secrecy` varchar(50) DEFAULT NULL,
                                  `ipAddress` varchar(50) DEFAULT NULL,
                                  `port` int(11) DEFAULT NULL,
                                  `password` varchar(255) DEFAULT NULL,
                                  `PTZType` int(11) DEFAULT NULL,
                                  `status` int(11) DEFAULT NULL,
                                  `longitude` double DEFAULT NULL,
                                  `latitude` double DEFAULT NULL,
                                  `streamId` varchar(50) DEFAULT NULL,
                                  `deviceId` varchar(50) NOT NULL,
                                  `parental` varchar(50) DEFAULT NULL,
                                  `hasAudio` bit(1) DEFAULT NULL,
                                  `createTime` varchar(50) NOT NULL,
                                  `updateTime` varchar(50) NOT NULL,
                                  `subCount` int(11) DEFAULT '0',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `device_channel_id_uindex` (`id`),
                                  UNIQUE KEY `device_channel_pk` (`channelId`,`deviceId`)
) ENGINE=InnoDB AUTO_INCREMENT=81657 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_channel`
--

LOCK TABLES `device_channel` WRITE;
/*!40000 ALTER TABLE `device_channel` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_mobile_position`
--

DROP TABLE IF EXISTS `device_mobile_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `device_mobile_position` (
                                          `id` int(11) NOT NULL AUTO_INCREMENT,
                                          `deviceId` varchar(50) NOT NULL,
                                          `channelId` varchar(50) NOT NULL,
                                          `deviceName` varchar(255) DEFAULT NULL,
                                          `time` varchar(50) NOT NULL,
                                          `longitude` double NOT NULL,
                                          `latitude` double NOT NULL,
                                          `altitude` double DEFAULT NULL,
                                          `speed` double DEFAULT NULL,
                                          `direction` double DEFAULT NULL,
                                          `reportSource` varchar(50) DEFAULT NULL,
                                          `geodeticSystem` varchar(50) DEFAULT NULL,
                                          `cnLng` varchar(50) DEFAULT NULL,
                                          `cnLat` varchar(50) DEFAULT NULL,
                                          PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6108 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_mobile_position`
--

LOCK TABLES `device_mobile_position` WRITE;
/*!40000 ALTER TABLE `device_mobile_position` DISABLE KEYS */;
/*!40000 ALTER TABLE `device_mobile_position` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gb_stream`
--

DROP TABLE IF EXISTS `gb_stream`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gb_stream` (
                             `gbStreamId` int(11) NOT NULL AUTO_INCREMENT,
                             `app` varchar(255) NOT NULL,
                             `stream` varchar(255) NOT NULL,
                             `gbId` varchar(50) NOT NULL,
                             `name` varchar(255) DEFAULT NULL,
                             `longitude` double DEFAULT NULL,
                             `latitude` double DEFAULT NULL,
                             `streamType` varchar(50) DEFAULT NULL,
                             `mediaServerId` varchar(50) DEFAULT NULL,
                             `status` int(11) DEFAULT NULL,
                             `createStamp` bigint(20) DEFAULT NULL,
                             PRIMARY KEY (`gbStreamId`) USING BTREE,
                             UNIQUE KEY `app` (`app`,`stream`) USING BTREE,
                             UNIQUE KEY `gbId` (`gbId`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=300769 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gb_stream`
--

LOCK TABLES `gb_stream` WRITE;
/*!40000 ALTER TABLE `gb_stream` DISABLE KEYS */;
/*!40000 ALTER TABLE `gb_stream` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log` (
                       `id` int(11) NOT NULL AUTO_INCREMENT,
                       `name` varchar(50) NOT NULL,
                       `type` varchar(50) NOT NULL,
                       `uri` varchar(200) NOT NULL,
                       `address` varchar(50) NOT NULL,
                       `result` varchar(50) NOT NULL,
                       `timing` bigint(20) NOT NULL,
                       `username` varchar(50) NOT NULL,
                       `createTime` varchar(50) NOT NULL,
                       PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1552 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log`
--

LOCK TABLES `log` WRITE;
/*!40000 ALTER TABLE `log` DISABLE KEYS */;
/*!40000 ALTER TABLE `log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `media_server`
--

DROP TABLE IF EXISTS `media_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `media_server` (
                                `id` varchar(255) NOT NULL,
                                `ip` varchar(50) NOT NULL,
                                `hookIp` varchar(50) NOT NULL,
                                `sdpIp` varchar(50) NOT NULL,
                                `streamIp` varchar(50) NOT NULL,
                                `httpPort` int(11) NOT NULL,
                                `httpSSlPort` int(11) NOT NULL,
                                `rtmpPort` int(11) NOT NULL,
                                `rtmpSSlPort` int(11) NOT NULL,
                                `rtpProxyPort` int(11) NOT NULL,
                                `rtspPort` int(11) NOT NULL,
                                `rtspSSLPort` int(11) NOT NULL,
                                `autoConfig` int(11) NOT NULL,
                                `secret` varchar(50) NOT NULL,
                                `streamNoneReaderDelayMS` int(11) NOT NULL,
                                `rtpEnable` int(11) NOT NULL,
                                `rtpPortRange` varchar(50) NOT NULL,
                                `sendRtpPortRange` varchar(50) NOT NULL,
                                `recordAssistPort` int(11) NOT NULL,
                                `defaultServer` int(11) NOT NULL,
                                `createTime` varchar(50) NOT NULL,
                                `updateTime` varchar(50) NOT NULL,
                                `hookAliveInterval` int(11) NOT NULL,
                                PRIMARY KEY (`id`) USING BTREE,
                                UNIQUE KEY `media_server_i` (`ip`,`httpPort`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media_server`
--

LOCK TABLES `media_server` WRITE;
/*!40000 ALTER TABLE `media_server` DISABLE KEYS */;
/*!40000 ALTER TABLE `media_server` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parent_platform`
--

DROP TABLE IF EXISTS `parent_platform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parent_platform` (
                                   `id` int(11) NOT NULL AUTO_INCREMENT,
                                   `enable` int(11) DEFAULT NULL,
                                   `name` varchar(255) DEFAULT NULL,
                                   `serverGBId` varchar(50) NOT NULL,
                                   `serverGBDomain` varchar(50) DEFAULT NULL,
                                   `serverIP` varchar(50) DEFAULT NULL,
                                   `serverPort` int(11) DEFAULT NULL,
                                   `deviceGBId` varchar(50) NOT NULL,
                                   `deviceIp` varchar(50) DEFAULT NULL,
                                   `devicePort` varchar(50) DEFAULT NULL,
                                   `username` varchar(255) DEFAULT NULL,
                                   `password` varchar(50) DEFAULT NULL,
                                   `expires` varchar(50) DEFAULT NULL,
                                   `keepTimeout` varchar(50) DEFAULT NULL,
                                   `transport` varchar(50) DEFAULT NULL,
                                   `characterSet` varchar(50) DEFAULT NULL,
                                   `catalogId` varchar(50) NOT NULL,
                                   `ptz` int(11) DEFAULT NULL,
                                   `rtcp` int(11) DEFAULT NULL,
                                   `status` bit(1) DEFAULT NULL,
                                   `shareAllLiveStream` int(11) DEFAULT NULL,
                                   `startOfflinePush` int(11) DEFAULT '0',
                                   `administrativeDivision` varchar(50) NOT NULL,
                                   `catalogGroup` int(11) DEFAULT '1',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `parent_platform_id_uindex` (`id`),
                                   UNIQUE KEY `parent_platform_pk` (`serverGBId`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parent_platform`
--

LOCK TABLES `parent_platform` WRITE;
/*!40000 ALTER TABLE `parent_platform` DISABLE KEYS */;
/*!40000 ALTER TABLE `parent_platform` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_catalog`
--

DROP TABLE IF EXISTS `platform_catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `platform_catalog` (
                                    `id` varchar(50) NOT NULL,
                                    `platformId` varchar(50) NOT NULL,
                                    `name` varchar(255) NOT NULL,
                                    `parentId` varchar(50) DEFAULT NULL,
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_catalog`
--

LOCK TABLES `platform_catalog` WRITE;
/*!40000 ALTER TABLE `platform_catalog` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_catalog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_channel`
--

DROP TABLE IF EXISTS `platform_gb_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `platform_gb_channel` (
                                       `id` int(11) NOT NULL AUTO_INCREMENT,
                                       `platformId` varchar(50) NOT NULL,
                                       `catalogId` varchar(50) NOT NULL,
                                       `deviceChannelId` int(11) NOT NULL,
                                       PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=250 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_channel`
--

LOCK TABLES `platform_gb_channel` WRITE;
/*!40000 ALTER TABLE `platform_gb_channel` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_gb_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_stream`
--

DROP TABLE IF EXISTS `platform_gb_stream`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `platform_gb_stream` (
                                      `platformId` varchar(50) NOT NULL,
                                      `catalogId` varchar(50) NOT NULL,
                                      `gbStreamId` int(11) NOT NULL,
                                      `id` int(11) NOT NULL AUTO_INCREMENT,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `platform_gb_stream_pk` (`platformId`,`catalogId`,`gbStreamId`)
) ENGINE=InnoDB AUTO_INCREMENT=301210 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_stream`
--

LOCK TABLES `platform_gb_stream` WRITE;
/*!40000 ALTER TABLE `platform_gb_stream` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_gb_stream` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_proxy`
--

DROP TABLE IF EXISTS `stream_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stream_proxy` (
                                `id` int(11) NOT NULL AUTO_INCREMENT,
                                `type` varchar(50) NOT NULL,
                                `app` varchar(255) NOT NULL,
                                `stream` varchar(255) NOT NULL,
                                `url` varchar(255) DEFAULT NULL,
                                `src_url` varchar(255) DEFAULT NULL,
                                `dst_url` varchar(255) DEFAULT NULL,
                                `timeout_ms` int(11) DEFAULT NULL,
                                `ffmpeg_cmd_key` varchar(255) DEFAULT NULL,
                                `rtp_type` varchar(50) DEFAULT NULL,
                                `mediaServerId` varchar(50) DEFAULT NULL,
                                `enable_hls` bit(1) DEFAULT NULL,
                                `enable_mp4` bit(1) DEFAULT NULL,
                                `enable` bit(1) NOT NULL,
                                `status` bit(1) NOT NULL,
                                `enable_remove_none_reader` bit(1) NOT NULL,
                                `createTime` varchar(50) NOT NULL,
                                `name` varchar(255) DEFAULT NULL,
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `stream_proxy_pk` (`app`,`stream`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_proxy`
--

LOCK TABLES `stream_proxy` WRITE;
/*!40000 ALTER TABLE `stream_proxy` DISABLE KEYS */;
/*!40000 ALTER TABLE `stream_proxy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_push`
--

DROP TABLE IF EXISTS `stream_push`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stream_push` (
                               `id` int(11) NOT NULL AUTO_INCREMENT,
                               `app` varchar(255) NOT NULL,
                               `stream` varchar(255) NOT NULL,
                               `totalReaderCount` varchar(50) DEFAULT NULL,
                               `originType` int(11) DEFAULT NULL,
                               `originTypeStr` varchar(50) DEFAULT NULL,
                               `createStamp` bigint(20) DEFAULT NULL,
                               `aliveSecond` int(11) DEFAULT NULL,
                               `mediaServerId` varchar(50) DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `stream_push_pk` (`app`,`stream`)
) ENGINE=InnoDB AUTO_INCREMENT=300838 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_push`
--

LOCK TABLES `stream_push` WRITE;
/*!40000 ALTER TABLE `stream_push` DISABLE KEYS */;
/*!40000 ALTER TABLE `stream_push` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
                        `id` int(11) NOT NULL AUTO_INCREMENT,
                        `username` varchar(255) NOT NULL,
                        `password` varchar(255) NOT NULL,
                        `roleId` int(11) NOT NULL,
                        `createTime` varchar(50) NOT NULL,
                        `updateTime` varchar(50) NOT NULL,
                        PRIMARY KEY (`id`) USING BTREE,
                        UNIQUE KEY `user_username_uindex` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES
    (1,'admin','21232f297a57a5a743894a0e4a801fc3',1,'2021 - 04 - 13 14:14:57','2021 - 04 - 13 14:14:57');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_role` (
                             `id` int(11) NOT NULL AUTO_INCREMENT,
                             `name` varchar(50) NOT NULL,
                             `authority` varchar(50) NOT NULL,
                             `createTime` varchar(50) NOT NULL,
                             `updateTime` varchar(50) NOT NULL,
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES
    (1,'admin','0','2021-04-13 14:14:57','2021-04-13 14:14:57');
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-04-18 10:50:27
