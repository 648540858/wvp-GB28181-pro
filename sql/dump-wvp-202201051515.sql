-- 此脚本采用dbeaver导出
-- MySQL dump 10.13  Distrib 8.0.27, for Linux (x86_64)
--
-- Host: localhost    Database: wvp
-- ------------------------------------------------------
-- Server version	8.0.27-0ubuntu0.20.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device` (
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
  `port` int NOT NULL,
  `expires` int NOT NULL,
  `subscribeCycleForCatalog` int NOT NULL,
  `hostAddress` varchar(50) NOT NULL,
  `charset` varchar(50) NOT NULL,
  PRIMARY KEY (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device` DISABLE KEYS */;
INSERT INTO `device` VALUES ('34020000001320000005','IPC-HFW4433M-I2','Dahua','IPC-HFW4433M-I2','2.622.0000000.31.R,2017-12-14','UDP','UDP','1','2022-01-05 15:08:26','2022-01-05 15:15:26','192.168.1.100','2022-01-05 15:08:26','2022-01-05 15:15:26',5060,3600,0,'192.168.1.100:5060','gb2312'),('34020000002000000005','DH-NVR5864-I','Dahua','DH-NVR5864-I','4.001.0000000.3,2020-10-22','UDP','UDP','1','2022-01-05 14:07:36','2022-01-05 15:15:25','192.168.1.19','2022-01-05 15:08:25','2022-01-05 15:15:25',5060,3600,0,'192.168.1.19:5060','gb2312'),('44010000001110008008',NULL,'Mercury','MIPC368(P)W-4','1.0.1 Build 210304 Rel.60784n','UDP','UDP','1','2022-01-05 15:08:35','2022-01-05 15:14:35','192.168.1.17','2022-01-05 15:08:35','2022-01-05 15:14:35',5060,36000,0,'192.168.1.17:5060','gb2312');
/*!40000 ALTER TABLE `device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_alarm`
--

DROP TABLE IF EXISTS `device_alarm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_alarm` (
  `id` int NOT NULL AUTO_INCREMENT,
  `deviceId` varchar(50) NOT NULL,
  `channelId` varchar(50) NOT NULL,
  `alarmPriority` varchar(50) NOT NULL,
  `alarmMethod` varchar(50) DEFAULT NULL,
  `alarmTime` varchar(50) NOT NULL,
  `alarmDescription` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `alarmType` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_channel` (
  `channelId` varchar(50) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `manufacture` varchar(50) DEFAULT NULL,
  `model` varchar(50) DEFAULT NULL,
  `owner` varchar(50) DEFAULT NULL,
  `civilCode` varchar(50) DEFAULT NULL,
  `block` varchar(50) DEFAULT NULL,
  `address` varchar(50) DEFAULT NULL,
  `parentId` varchar(50) DEFAULT NULL,
  `safetyWay` int DEFAULT NULL,
  `registerWay` int DEFAULT NULL,
  `certNum` varchar(50) DEFAULT NULL,
  `certifiable` int DEFAULT NULL,
  `errCode` int DEFAULT NULL,
  `endTime` varchar(50) DEFAULT NULL,
  `secrecy` varchar(50) DEFAULT NULL,
  `ipAddress` varchar(50) DEFAULT NULL,
  `port` int DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `PTZType` int DEFAULT NULL,
  `status` int DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `streamId` varchar(50) DEFAULT NULL,
  `deviceId` varchar(50) NOT NULL,
  `parental` varchar(50) DEFAULT NULL,
  `hasAudio` bit(1) DEFAULT NULL,
  `createTime` varchar(50) NOT NULL,
  `updateTime` varchar(50) NOT NULL,
  PRIMARY KEY (`channelId`,`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_channel`
--

LOCK TABLES `device_channel` WRITE;
/*!40000 ALTER TABLE `device_channel` DISABLE KEYS */;
INSERT INTO `device_channel` VALUES ('34020000001310000001','IPC','Dahua','IPC-HFW4433M-I2','0','340200','','axy','34020000001320000005',0,1,'',0,0,NULL,'0','',0,'',0,1,0,0,'','34020000001320000005','0',NULL,'2022-01-05 15:11:21','2022-01-05 15:11:21'),('34020000001310000001','通道1','Dahua','DH-NVR5864-I','0','340200','','axy','34020000002000000005',0,1,'',0,0,NULL,'0','192.168.1.17',37777,'',0,1,0,0,'','34020000002000000005','0',NULL,'2022-01-05 15:11:25','2022-01-05 15:11:25'),('34020000001310000065','GB_Chn_065','Dahua','DH-NVR5864-I','0','340200','','axy','34020000002000000005',0,1,'',0,0,NULL,'0','',0,'',0,1,0,0,'','34020000002000000005','0',NULL,'2022-01-05 15:11:25','2022-01-05 15:11:25'),('34020000001320000001','IPCamera 01','Mercury','MIPC368(P)W-4','Owner','CivilCode','','Address','',0,1,'',0,0,NULL,'0','',0,'',0,1,0,0,'','44010000001110008008','0',NULL,'2022-01-05 15:11:26','2022-01-05 15:11:26');
/*!40000 ALTER TABLE `device_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_mobile_position`
--

DROP TABLE IF EXISTS `device_mobile_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_mobile_position` (
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
  PRIMARY KEY (`deviceId`,`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gb_stream` (
  `app` varchar(255) NOT NULL,
  `stream` varchar(255) NOT NULL,
  `gbId` varchar(50) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `streamType` varchar(50) DEFAULT NULL,
  `mediaServerId` varchar(50) DEFAULT NULL,
  `status` int DEFAULT NULL,
  PRIMARY KEY (`app`,`stream`,`gbId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gb_stream`
--

LOCK TABLES `gb_stream` WRITE;
/*!40000 ALTER TABLE `gb_stream` DISABLE KEYS */;
INSERT INTO `gb_stream` VALUES ('1000','10000001_52869999','77777777777777777777','shoulei1111',0,0,'push','XR1LEpKlfQtSg9Z1',1);
/*!40000 ALTER TABLE `gb_stream` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log`
--

DROP TABLE IF EXISTS `log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `type` varchar(50) NOT NULL,
  `uri` varchar(200) NOT NULL,
  `address` varchar(50) NOT NULL,
  `result` varchar(50) NOT NULL,
  `timing` bigint NOT NULL,
  `username` varchar(50) NOT NULL,
  `createTime` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log`
--

LOCK TABLES `log` WRITE;
/*!40000 ALTER TABLE `log` DISABLE KEYS */;
INSERT INTO `log` VALUES (1,'登录','GET','/api/user/login','127.0.0.1','200 OK',245,'admin','2022-01-05 15:09:06'),(2,'添加上级平台','POST','/api/platform/save','127.0.0.1','200 OK',88,'admin','2022-01-05 15:09:24'),(3,'[设备查询] 同步设备通道','POST','/api/device/query/devices/34020000001320000005/sync','127.0.0.1','200 OK',17,'admin','2022-01-05 15:11:21'),(4,'[设备查询] 同步设备通道','POST','/api/device/query/devices/34020000002000000005/sync','127.0.0.1','200 OK',4,'admin','2022-01-05 15:11:25'),(5,'[设备查询] 同步设备通道','POST','/api/device/query/devices/44010000001110008008/sync','127.0.0.1','200 OK',4,'admin','2022-01-05 15:11:26'),(6,'向上级平台添加国标通道','POST','/api/platform/update_channel_for_gb','127.0.0.1','200 OK',52,'admin','2022-01-05 15:11:32'),(7,'从上级平台移除国标通道','DELETE','/api/platform/del_channel_for_gb','127.0.0.1','200 OK',35,'admin','2022-01-05 15:11:34'),(8,'向上级平台添加国标通道','POST','/api/platform/update_channel_for_gb','127.0.0.1','200 OK',39,'admin','2022-01-05 15:11:35'),(9,'从上级平台移除国标通道','DELETE','/api/platform/del_channel_for_gb','127.0.0.1','200 OK',46,'admin','2022-01-05 15:14:00'),(10,'向上级平台添加国标通道','POST','/api/platform/update_channel_for_gb','127.0.0.1','200 OK',59,'admin','2022-01-05 15:14:01'),(11,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',12,'admin','2022-01-05 15:14:16'),(12,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',8,'admin','2022-01-05 15:14:17'),(13,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',6,'admin','2022-01-05 15:14:19'),(14,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',8,'admin','2022-01-05 15:14:19'),(15,'移除通道与国标的关联','DELETE','/api/gbStream/del','127.0.0.1','200 OK',11,'admin','2022-01-05 15:14:21'),(16,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',42,'admin','2022-01-05 15:14:24'),(17,'移除通道与国标的关联','DELETE','/api/gbStream/del','127.0.0.1','200 OK',43,'admin','2022-01-05 15:14:25'),(18,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',9,'admin','2022-01-05 15:14:27'),(19,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',9,'admin','2022-01-05 15:14:37'),(20,'添加通道与国标的关联','POST','/api/gbStream/add','127.0.0.1','200 OK',10,'admin','2022-01-05 15:14:38');
/*!40000 ALTER TABLE `log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `media_server`
--

DROP TABLE IF EXISTS `media_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_server` (
  `id` varchar(255) NOT NULL,
  `ip` varchar(50) NOT NULL,
  `hookIp` varchar(50) NOT NULL,
  `sdpIp` varchar(50) NOT NULL,
  `streamIp` varchar(50) NOT NULL,
  `httpPort` int NOT NULL,
  `httpSSlPort` int NOT NULL,
  `rtmpPort` int NOT NULL,
  `rtmpSSlPort` int NOT NULL,
  `rtpProxyPort` int NOT NULL,
  `rtspPort` int NOT NULL,
  `rtspSSLPort` int NOT NULL,
  `autoConfig` int NOT NULL,
  `secret` varchar(50) NOT NULL,
  `streamNoneReaderDelayMS` int NOT NULL,
  `rtpEnable` int NOT NULL,
  `rtpPortRange` varchar(50) NOT NULL,
  `sendRtpPortRange` varchar(50) NOT NULL,
  `recordAssistPort` int NOT NULL,
  `defaultServer` int NOT NULL,
  `createTime` varchar(50) NOT NULL,
  `updateTime` varchar(50) NOT NULL,
  `hookAliveInterval` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `media_server_i` (`ip`,`httpPort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media_server`
--

LOCK TABLES `media_server` WRITE;
/*!40000 ALTER TABLE `media_server` DISABLE KEYS */;
INSERT INTO `media_server` VALUES ('XR1LEpKlfQtSg9Z1','192.168.1.3','127.0.0.1','192.168.1.3','192.168.1.3',6080,0,10935,0,10000,10554,0,1,'035c73f7-bb6b-4889-a715-d9eb2d1925cc',100000,1,'30000,30500','30000,30500',18081,1,'2022-01-05 15:08:27','2022-01-05 15:08:27',10);
/*!40000 ALTER TABLE `media_server` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parent_platform`
--

DROP TABLE IF EXISTS `parent_platform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `parent_platform` (
  `id` int NOT NULL AUTO_INCREMENT,
  `enable` int DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `serverGBId` varchar(50) NOT NULL,
  `serverGBDomain` varchar(50) DEFAULT NULL,
  `serverIP` varchar(50) DEFAULT NULL,
  `serverPort` int DEFAULT NULL,
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
  `ptz` int DEFAULT NULL,
  `rtcp` int DEFAULT NULL,
  `status` bit(1) DEFAULT NULL,
  `shareAllLiveStream` int DEFAULT NULL,
  PRIMARY KEY (`id`,`serverGBId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parent_platform`
--

LOCK TABLES `parent_platform` WRITE;
/*!40000 ALTER TABLE `parent_platform` DISABLE KEYS */;
INSERT INTO `parent_platform` VALUES (1,1,'1112','1111111111111','1111111111','11.11.11.11',111111,'34020000002110000015','192.168.1.3','5060','34020000002110000015','12345678','300','60','UDP','GB2312','1111111111111',1,0,_binary '\0',1);
/*!40000 ALTER TABLE `parent_platform` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_catalog`
--

DROP TABLE IF EXISTS `platform_catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_catalog` (
  `id` varchar(50) NOT NULL,
  `platformId` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `parentId` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_catalog`
--

LOCK TABLES `platform_catalog` WRITE;
/*!40000 ALTER TABLE `platform_catalog` DISABLE KEYS */;
INSERT INTO `platform_catalog` VALUES ('1111111111','1111111111111','11122','1111111111111');
/*!40000 ALTER TABLE `platform_catalog` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_channel`
--

DROP TABLE IF EXISTS `platform_gb_channel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_gb_channel` (
  `channelId` varchar(50) NOT NULL,
  `deviceId` varchar(50) NOT NULL,
  `platformId` varchar(50) NOT NULL,
  `deviceAndChannelId` varchar(50) NOT NULL,
  `catalogId` varchar(50) NOT NULL,
  PRIMARY KEY (`deviceAndChannelId`,`platformId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_channel`
--

LOCK TABLES `platform_gb_channel` WRITE;
/*!40000 ALTER TABLE `platform_gb_channel` DISABLE KEYS */;
INSERT INTO `platform_gb_channel` VALUES ('34020000001310000001','34020000001320000005','1111111111111','34020000001320000005_34020000001310000001','1111111111'),('34020000001310000001','34020000002000000005','1111111111111','34020000002000000005_34020000001310000001','1111111111'),('34020000001310000065','34020000002000000005','1111111111111','34020000002000000005_34020000001310000065','1111111111'),('34020000001320000001','44010000001110008008','1111111111111','44010000001110008008_34020000001320000001','1111111111');
/*!40000 ALTER TABLE `platform_gb_channel` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `platform_gb_stream`
--

DROP TABLE IF EXISTS `platform_gb_stream`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_gb_stream` (
  `platformId` varchar(50) NOT NULL,
  `app` varchar(255) NOT NULL,
  `stream` varchar(255) NOT NULL,
  `catalogId` varchar(50) NOT NULL,
  PRIMARY KEY (`platformId`,`app`,`stream`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `platform_gb_stream`
--

LOCK TABLES `platform_gb_stream` WRITE;
/*!40000 ALTER TABLE `platform_gb_stream` DISABLE KEYS */;
INSERT INTO `platform_gb_stream` VALUES ('1111111111111','1000','10000001_52869999','1111111111');
/*!40000 ALTER TABLE `platform_gb_stream` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` text NOT NULL,
  `authority` text NOT NULL,
  `createTime` varchar(50) NOT NULL,
  `updateTime` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'admin','0','2021-04-13 14:14:57','2021-04-13 14:14:57');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_proxy`
--

DROP TABLE IF EXISTS `stream_proxy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stream_proxy` (
  `type` varchar(50) NOT NULL,
  `app` varchar(255) NOT NULL,
  `stream` varchar(255) NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  `src_url` varchar(255) DEFAULT NULL,
  `dst_url` varchar(255) DEFAULT NULL,
  `timeout_ms` int DEFAULT NULL,
  `ffmpeg_cmd_key` varchar(255) DEFAULT NULL,
  `rtp_type` varchar(50) DEFAULT NULL,
  `mediaServerId` varchar(50) DEFAULT NULL,
  `enable_hls` bit(1) DEFAULT NULL,
  `enable_mp4` bit(1) DEFAULT NULL,
  `enable` bit(1) NOT NULL,
  `enable_remove_none_reader` bit(1) NOT NULL,
  `createTime` varchar(50) NOT NULL,
  PRIMARY KEY (`app`,`stream`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stream_push` (
  `app` varchar(255) NOT NULL,
  `stream` varchar(255) NOT NULL,
  `totalReaderCount` varchar(50) DEFAULT NULL,
  `originType` int DEFAULT NULL,
  `originTypeStr` varchar(50) DEFAULT NULL,
  `createStamp` int DEFAULT NULL,
  `aliveSecond` int DEFAULT NULL,
  `mediaServerId` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`app`,`stream`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_push`
--

LOCK TABLES `stream_push` WRITE;
/*!40000 ALTER TABLE `stream_push` DISABLE KEYS */;
INSERT INTO `stream_push` VALUES ('1000','10000001_52869999','0',2,'rtsp_push',1641366850,0,'XR1LEpKlfQtSg9Z1');
/*!40000 ALTER TABLE `stream_push` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `roleId` int NOT NULL,
  `createTime` varchar(50) NOT NULL,
  `updateTime` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_username_uindex` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','21232f297a57a5a743894a0e4a801fc3',1,'2021-04-13 14:14:57','2021-04-13 14:14:57');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'wvp'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-01-05 15:15:35
