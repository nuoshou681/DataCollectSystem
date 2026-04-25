/*
 Navicat Premium Data Transfer

 Source Server         : dcsysdb
 Source Server Type    : MySQL
 Source Server Version : 90500 (9.5.0)
 Source Host           : localhost:3306
 Source Schema         : dcsysdb

 Target Server Type    : MySQL
 Target Server Version : 90500 (9.5.0)
 File Encoding         : 65001

 Date: 25/04/2026 12:59:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for crawler
-- ----------------------------
DROP TABLE IF EXISTS `crawler`;
CREATE TABLE `crawler` (
  `node_id` varchar(255) NOT NULL,
  `node_name` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE, OFFLINE, BUSY, IDLE',
  `version` varchar(64) DEFAULT NULL,
  `capabilities_json` json DEFAULT NULL,
  `tags_json` json DEFAULT NULL,
  `max_concurrency` int NOT NULL DEFAULT '1',
  `current_load` int NOT NULL DEFAULT '0',
  `heartbeat_timeout_sec` int NOT NULL DEFAULT '15',
  `last_online_at` datetime DEFAULT NULL,
  `last_heartbeat` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`node_id`),
  KEY `idx_crawler_status_heartbeat` (`status`,`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of crawler
-- ----------------------------
BEGIN;
INSERT INTO `crawler` (`node_id`, `node_name`, `status`, `version`, `capabilities_json`, `tags_json`, `max_concurrency`, `current_load`, `heartbeat_timeout_sec`, `last_online_at`, `last_heartbeat`, `created_at`, `updated_at`) VALUES ('node-1', NULL, 'ONLINE', NULL, NULL, NULL, 1, 0, 15, NULL, '2026-04-24 23:17:38', '2026-04-24 22:22:26', '2026-04-24 23:17:38');
COMMIT;

-- ----------------------------
-- Table structure for crawler_page_result
-- ----------------------------
DROP TABLE IF EXISTS `crawler_page_result`;
CREATE TABLE `crawler_page_result` (
  `page_result_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `node_id` varchar(255) DEFAULT NULL,
  `site_type` varchar(32) DEFAULT NULL,
  `page_url` text NOT NULL,
  `page_title` varchar(500) DEFAULT NULL,
  `total_pages` int DEFAULT NULL,
  `page_index` int NOT NULL,
  `success` tinyint(1) NOT NULL DEFAULT '0',
  `file_path` varchar(1024) DEFAULT NULL,
  `storage_type` varchar(32) NOT NULL DEFAULT 'FILE',
  `mime_type` varchar(100) DEFAULT 'multipart/related',
  `file_size_bytes` bigint DEFAULT NULL,
  `content_sha256` varchar(64) DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` text,
  `mhtml_cached` tinyint(1) NOT NULL DEFAULT '0',
  `mhtml_cached_at` datetime DEFAULT NULL,
  `mhtml_content` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`page_result_id`),
  UNIQUE KEY `uk_task_page` (`task_id`,`page_index`,`page_url`(255)),
  KEY `idx_page_result_task_id` (`task_id`),
  KEY `idx_page_result_node_id` (`node_id`),
  KEY `idx_page_result_success_created_at` (`success`,`created_at`),
  CONSTRAINT `fk_page_result_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=132436 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of crawler_page_result
-- ----------------------------
BEGIN;
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132425, 1, 'node-1', NULL, 'https://search.sohu.com/?keyword=变形金刚', '搜狐搜索（变形金刚）', 1, 1, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/1/01.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:27:25', '2026-04-24 22:27:25');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132426, 2, 'node-1', NULL, 'https://search.sohu.com/?keyword=菜谱', '搜狐搜索（菜谱）', 10, 1, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/01.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:28:27', '2026-04-24 22:28:27');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132427, 2, 'node-1', NULL, 'https://www.sohu.com/a/1007519958_119778?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.1.1777031305580E31dvgE', '从菜谱窥察历史与诗情_山家_清供_卫辉', 10, 2, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/02.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:28:37', '2026-04-24 22:28:37');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132428, 2, 'node-1', NULL, 'https://www.sohu.com/a/1012566839_120248510?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.2.1777031305580E31dvgE', '1953年毛主席视察海军时的菜谱，伙食标准低于舰员', 10, 3, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/03.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:28:44', '2026-04-24 22:28:44');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132429, 2, 'node-1', NULL, 'https://www.sohu.com/a/1005430406_116132?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.3.1777031305580E31dvgE', NULL, 10, 4, 0, NULL, 'FILE', 'multipart/related', NULL, NULL, NULL, 'Timeout 30000ms exceeded', 0, NULL, NULL, '2026-04-24 22:29:20', '2026-04-24 22:29:20');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132430, 2, 'node-1', NULL, 'https://www.sohu.com/a/1013315871_121965316?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.4.1777031305580E31dvgE', '一家五口的暖心晚餐：妈妈的拿手菜谱大公开_香菇菜心_鸡腿_柠檬', 10, 5, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/05.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:29:31', '2026-04-24 22:29:31');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132431, 2, 'node-1', NULL, 'https://www.sohu.com/a/988696034_119004?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.5.1777031305580E31dvgE', '收藏菜谱前，这些东西一定要知道！_豆果_营养_美食', 10, 6, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/06.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:29:42', '2026-04-24 22:29:42');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132432, 2, 'node-1', NULL, 'https://www.sohu.com/a/995918467_121402629?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.6.1777031305580E31dvgE', '翻开我的精神世界发现是一本厚厚的菜谱', 10, 7, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/07.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:29:49', '2026-04-24 22:29:49');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132433, 2, 'node-1', NULL, 'https://www.sohu.com/a/992655777_121213129?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.7.1777031305580E31dvgE', '顺德大学生用AI复原失传菜谱，作品获企业青睐_创作_美食_传承', 10, 8, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/08.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:29:58', '2026-04-24 22:29:58');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132434, 2, 'node-1', NULL, 'https://www.sohu.com/a/992541365_121213129?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.8.1777031305580E31dvgE', '轻断食打卡别败给单调食谱，用AI一键生成顺德风味菜谱_食材_代码_联赛', 10, 9, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/09.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:30:06', '2026-04-24 22:30:06');
INSERT INTO `crawler_page_result` (`page_result_id`, `task_id`, `node_id`, `site_type`, `page_url`, `page_title`, `total_pages`, `page_index`, `success`, `file_path`, `storage_type`, `mime_type`, `file_size_bytes`, `content_sha256`, `error_code`, `error_message`, `mhtml_cached`, `mhtml_cached_at`, `mhtml_content`, `created_at`, `updated_at`) VALUES (132435, 2, 'node-1', NULL, 'https://www.sohu.com/a/979995948_121130946?scm=10023.20001_107-20001_107-0_20001.0-0.0-1-0-0-0.0&spm=smpc.csrpage.news-list.9.1777031305580E31dvgE', '分享三道家常下饭菜谱，厨房小白也能学会_步骤_胡椒粉_猪肚', 10, 10, 1, '/Users/rain/Desktop/DataCollectSystem/shared/crawl-files/2/10.mhtml', 'FILE', 'multipart/related', NULL, NULL, NULL, NULL, 0, NULL, NULL, '2026-04-24 22:30:17', '2026-04-24 22:30:17');
COMMIT;

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `task_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned DEFAULT NULL,
  `node_id` varchar(255) DEFAULT NULL,
  `batch_id` varchar(64) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `keyword` varchar(255) DEFAULT NULL,
  `site_type` varchar(32) DEFAULT NULL,
  `task_status` varchar(50) DEFAULT NULL,
  `task_progress` int DEFAULT NULL,
  `total_pages` int DEFAULT NULL,
  `max_links_per_level` int NOT NULL DEFAULT '10',
  `priority` int NOT NULL DEFAULT '0',
  `source` varchar(32) NOT NULL DEFAULT 'manual',
  `idempotency_key` varchar(128) DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT '0',
  `cancel_requested` tinyint(1) NOT NULL DEFAULT '0',
  `last_error_message` text,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_user_idempotency` (`user_id`,`idempotency_key`),
  KEY `idx_task_created_at` (`created_at`),
  KEY `idx_task_status_created_at` (`task_status`,`created_at`),
  KEY `idx_task_user_created_at` (`user_id`,`created_at`),
  KEY `idx_task_node_status` (`node_id`,`task_status`),
  KEY `idx_task_keyword` (`keyword`),
  KEY `idx_task_site_type` (`site_type`),
  KEY `idx_task_batch_id` (`batch_id`),
  CONSTRAINT `fk_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task
-- ----------------------------
BEGIN;
INSERT INTO `task` (`task_id`, `user_id`, `node_id`, `batch_id`, `url`, `keyword`, `site_type`, `task_status`, `task_progress`, `total_pages`, `max_links_per_level`, `priority`, `source`, `idempotency_key`, `retry_count`, `cancel_requested`, `last_error_message`, `started_at`, `finished_at`, `created_at`, `updated_at`) VALUES (1, 2, 'node-1', NULL, 'https://search.sohu.com/?keyword=', '变形金刚', NULL, 'FINISHED', 100, 1, 10, 0, 'manual', NULL, 0, 0, NULL, NULL, NULL, '2026-04-24 22:27:06', '2026-04-24 23:07:52');
INSERT INTO `task` (`task_id`, `user_id`, `node_id`, `batch_id`, `url`, `keyword`, `site_type`, `task_status`, `task_progress`, `total_pages`, `max_links_per_level`, `priority`, `source`, `idempotency_key`, `retry_count`, `cancel_requested`, `last_error_message`, `started_at`, `finished_at`, `created_at`, `updated_at`) VALUES (2, 2, 'node-1', NULL, 'https://search.sohu.com/?keyword=', '菜谱', NULL, 'FINISHED', 100, 9, 10, 0, 'manual', NULL, 0, 0, NULL, NULL, NULL, '2026-04-24 22:28:14', '2026-04-24 23:07:54');
COMMIT;

-- ----------------------------
-- Table structure for task_event
-- ----------------------------
DROP TABLE IF EXISTS `task_event`;
CREATE TABLE `task_event` (
  `event_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `node_id` varchar(255) DEFAULT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_level` varchar(20) NOT NULL DEFAULT 'INFO',
  `event_message` text NOT NULL,
  `payload_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`),
  KEY `idx_task_event_task_id` (`task_id`,`created_at`),
  KEY `idx_task_event_node_id` (`node_id`,`created_at`),
  KEY `idx_task_event_type` (`event_type`,`created_at`),
  CONSTRAINT `fk_task_event_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_event
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for task_file
-- ----------------------------
DROP TABLE IF EXISTS `task_file`;
CREATE TABLE `task_file` (
  `file_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `page_result_id` bigint DEFAULT NULL,
  `file_type` varchar(32) NOT NULL DEFAULT 'MHTML',
  `storage_type` varchar(32) NOT NULL DEFAULT 'FILE',
  `mime_type` varchar(100) DEFAULT 'multipart/related',
  `file_path` varchar(1024) DEFAULT NULL,
  `content_sha256` varchar(64) DEFAULT NULL,
  `size_bytes` bigint DEFAULT NULL,
  `db_content` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`),
  KEY `idx_task_file_task_id` (`task_id`),
  KEY `idx_task_file_page_result_id` (`page_result_id`),
  KEY `idx_task_file_sha256` (`content_sha256`),
  CONSTRAINT `fk_task_file_page_result` FOREIGN KEY (`page_result_id`) REFERENCES `crawler_page_result` (`page_result_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_task_file_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_file
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for task_log
-- ----------------------------
DROP TABLE IF EXISTS `task_log`;
CREATE TABLE `task_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned DEFAULT NULL,
  `node_id` bigint DEFAULT NULL COMMENT '兼容旧后端字段，暂不删除',
  `node_key` varchar(255) DEFAULT NULL COMMENT '推荐的新节点标识字段',
  `log_message` text,
  `log_level` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_task_log_task_id` (`task_id`),
  KEY `idx_task_log_node_key` (`node_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for task_runtime
-- ----------------------------
DROP TABLE IF EXISTS `task_runtime`;
CREATE TABLE `task_runtime` (
  `task_id` bigint unsigned NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `assigned_node_id` varchar(255) DEFAULT NULL,
  `progress_percent` int NOT NULL DEFAULT '0',
  `expected_pages` int DEFAULT NULL,
  `completed_pages` int NOT NULL DEFAULT '0',
  `success_pages` int NOT NULL DEFAULT '0',
  `failed_pages` int NOT NULL DEFAULT '0',
  `retry_count` int NOT NULL DEFAULT '0',
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` text,
  `queued_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_runtime_status_updated_at` (`status`,`updated_at`),
  KEY `idx_task_runtime_node_status` (`assigned_node_id`,`status`),
  CONSTRAINT `fk_task_runtime_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of task_runtime
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'user',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` datetime DEFAULT NULL,
  `password_updated_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO `user` (`user_id`, `username`, `password`, `email`, `role`, `status`, `last_login_at`, `password_updated_at`, `created_at`, `updated_at`) VALUES (1, 'admin', '$2a$10$anG920EXLisfJbEPHE9eEOOj3A2h2Wi7vAcIPBLCCUsQ1TAOxddV2', 'admin@example.com', 'admin', 'ACTIVE', NULL, '2026-04-24 22:18:08', '2026-04-24 22:18:08', '2026-04-24 22:18:08');
INSERT INTO `user` (`user_id`, `username`, `password`, `email`, `role`, `status`, `last_login_at`, `password_updated_at`, `created_at`, `updated_at`) VALUES (2, 'rain', '$2a$10$PpgAwNKMYuUXQquaJO.tlejoiK2acdRSSiq7h5rP1NizErUzJNB.2', 'rain@qq.com', 'user', 'ACTIVE', NULL, NULL, '2026-04-24 23:07:23', '2026-04-24 23:07:23');
INSERT INTO `user` (`user_id`, `username`, `password`, `email`, `role`, `status`, `last_login_at`, `password_updated_at`, `created_at`, `updated_at`) VALUES (3, 'bob', '$2a$10$4U7FPZh6UxgRBfXJDRogUuwHNiuShWHt2sdRhRCGv4hnSQuKgWwAi', 'bob@qq.com', 'user', 'ACTIVE', NULL, NULL, '2026-04-24 23:11:19', '2026-04-24 23:11:19');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
