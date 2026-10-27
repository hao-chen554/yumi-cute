-- ============================================================
--  YuMi-cute 数据库结构
--
--  本文件由 mysqldump 从运行中的库导出，保证与实际结构一致。
--  手写 schema 容易漏 —— 本项目就漏过 user_file 整张表、photo_order.client_token 列，
--  以及 uk_client_token / uk_user_type_biz / uk_url 三个唯一索引。
--  所以：结构一律以导出为准，只允许补注释，不允许改 DDL。
--
--  用法（--default-character-set=utf8mb4 不能省，原因见 data.sql 里的说明）：
--    mysql -uroot -p --default-character-set=utf8mb4 < sql/schema.sql
--    mysql -uroot -p --default-character-set=utf8mb4 yumi_cute < sql/data.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS yumi_cute
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yumi_cute;

SET NAMES utf8mb4;


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
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '所属用户id',
  `name` varchar(30) NOT NULL DEFAULT '' COMMENT '宠物名字',
  `species` varchar(20) NOT NULL DEFAULT '' COMMENT '种类 dog/cat',
  `breed` varchar(50) NOT NULL DEFAULT '' COMMENT '品种',
  `avatar_url` varchar(500) NOT NULL DEFAULT '' COMMENT '头像地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='宠物表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `photo_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(32) NOT NULL COMMENT '对外订单号',
  `client_token` varchar(64) DEFAULT NULL COMMENT '客户端幂等键：同一个 token 只会产生一个订单',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `pet_id` bigint DEFAULT NULL COMMENT '拍摄宠物id（第一版可为空）',
  `style_id` bigint NOT NULL COMMENT '风格模板',
  `person_photo_url` varchar(500) NOT NULL DEFAULT '' COMMENT '本人照片',
  `pet_photo_url` varchar(500) NOT NULL DEFAULT '' COMMENT '宠物照片',
  `order_type` tinyint NOT NULL DEFAULT '1' COMMENT '1试看单 2整套单',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总张数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功张数',
  `points_cost` int NOT NULL DEFAULT '0' COMMENT '消耗算力',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待处理 1生成中 2完成 3部分失败 4失败 5已取消',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `share_token` varchar(64) DEFAULT NULL COMMENT '分享令牌；NULL = 未分享或已撤销',
  `share_expire_at` datetime DEFAULT NULL COMMENT '分享失效时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '用户删除时间；NULL=未删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_client_token` (`user_id`,`client_token`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_user_deleted` (`user_id`,`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='写真订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `photo_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '所属订单',
  `seq_no` int NOT NULL DEFAULT '0' COMMENT '第几张',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待生成 1生成中 2成功 3失败',
  `image_url` varchar(500) NOT NULL DEFAULT '' COMMENT '结果图地址',
  `error_msg` varchar(500) NOT NULL DEFAULT '' COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生图任务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `change_amount` int NOT NULL COMMENT '正数增加 负数消耗',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `type` tinyint NOT NULL COMMENT '1注册赠送 2生成消费 3充值 4退款',
  `biz_id` bigint NOT NULL DEFAULT '0' COMMENT '关联业务id',
  `remark` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type_biz` (`user_id`,`type`,`biz_id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=68 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算力流水表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recharge_no` varchar(32) NOT NULL COMMENT '充值单号',
  `user_id` bigint NOT NULL,
  `package_id` int NOT NULL,
  `points` int NOT NULL COMMENT '到账算力快照 = 基础 + 赠送',
  `amount_cent` int NOT NULL COMMENT '应付金额快照（分）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待支付 1已支付 2已到账 3已关闭',
  `client_token` varchar(64) NOT NULL COMMENT '客户端幂等键',
  `pay_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recharge_no` (`recharge_no`),
  UNIQUE KEY `uk_client_token` (`user_id`,`client_token`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_status_time` (`status`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='充值订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recharge_package` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '套餐名',
  `points` int NOT NULL COMMENT '基础算力',
  `bonus_points` int NOT NULL DEFAULT '0' COMMENT '赠送算力',
  `amount_cent` int NOT NULL COMMENT '售价（分）',
  `sort_order` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1上架 0下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='充值套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `style_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '风格名',
  `cover_url` varchar(500) NOT NULL DEFAULT '' COMMENT '封面图',
  `description` varchar(255) NOT NULL DEFAULT '' COMMENT '简介',
  `prompt` text COMMENT '给AI的提示词',
  `points_cost` int NOT NULL DEFAULT '0' COMMENT '消耗算力',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1上架 0下架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='风格模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录名',
  `password` varchar(100) NOT NULL COMMENT '加密后的密码',
  `nickname` varchar(50) NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar_url` varchar(500) NOT NULL DEFAULT '' COMMENT '头像地址',
  `points` int NOT NULL DEFAULT '0' COMMENT '算力余额',
  `vip_level` tinyint NOT NULL DEFAULT '0' COMMENT '会员等级',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1正常 0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '上传者',
  `url` varchar(500) NOT NULL COMMENT '访问地址',
  `biz_type` tinyint NOT NULL DEFAULT '0' COMMENT '1头像 2人物参考图 3宠物参考图 4生成结果',
  `size_bytes` bigint NOT NULL DEFAULT '0' COMMENT '文件大小',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间，NULL=不过期',
  `deleted_at` datetime DEFAULT NULL COMMENT '物理删除时间，NULL=未删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url` (`url`),
  KEY `idx_user` (`user_id`,`biz_type`),
  KEY `idx_expire` (`expire_at`,`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=190 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户文件表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

