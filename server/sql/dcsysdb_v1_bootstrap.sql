SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `dcsysdb`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `dcsysdb`;

DROP TABLE IF EXISTS `task_event`;
DROP TABLE IF EXISTS `task_file`;
DROP TABLE IF EXISTS `task_runtime`;
DROP TABLE IF EXISTS `crawler_page_result`;
DROP TABLE IF EXISTS `task_log`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `crawler`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `crawler` (
  `node_id` varchar(255) NOT NULL,
  `node_name` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE, OFFLINE, BUSY, IDLE',
  `version` varchar(64) DEFAULT NULL,
  `capabilities_json` json DEFAULT NULL,
  `tags_json` json DEFAULT NULL,
  `max_concurrency` int NOT NULL DEFAULT 1,
  `current_load` int NOT NULL DEFAULT 0,
  `heartbeat_timeout_sec` int NOT NULL DEFAULT 15,
  `last_online_at` datetime DEFAULT NULL,
  `last_heartbeat` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`node_id`),
  KEY `idx_crawler_status_heartbeat` (`status`, `last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  `max_links_per_level` int NOT NULL DEFAULT 10,
  `priority` int NOT NULL DEFAULT 0,
  `source` varchar(32) NOT NULL DEFAULT 'manual',
  `idempotency_key` varchar(128) DEFAULT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `cancel_requested` tinyint(1) NOT NULL DEFAULT 0,
  `last_error_message` text,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_created_at` (`created_at`),
  KEY `idx_task_status_created_at` (`task_status`, `created_at`),
  KEY `idx_task_user_created_at` (`user_id`, `created_at`),
  KEY `idx_task_node_status` (`node_id`, `task_status`),
  KEY `idx_task_keyword` (`keyword`),
  KEY `idx_task_site_type` (`site_type`),
  KEY `idx_task_batch_id` (`batch_id`),
  UNIQUE KEY `uk_task_user_idempotency` (`user_id`, `idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `task_runtime` (
  `task_id` bigint unsigned NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `assigned_node_id` varchar(255) DEFAULT NULL,
  `progress_percent` int NOT NULL DEFAULT 0,
  `expected_pages` int DEFAULT NULL,
  `completed_pages` int NOT NULL DEFAULT 0,
  `success_pages` int NOT NULL DEFAULT 0,
  `failed_pages` int NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `last_error_code` varchar(64) DEFAULT NULL,
  `last_error_message` text,
  `queued_at` datetime DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_task_runtime_status_updated_at` (`status`, `updated_at`),
  KEY `idx_task_runtime_node_status` (`assigned_node_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `crawler_page_result` (
  `page_result_id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint unsigned NOT NULL,
  `node_id` varchar(255) DEFAULT NULL,
  `site_type` varchar(32) DEFAULT NULL,
  `page_url` text NOT NULL,
  `page_title` varchar(500) DEFAULT NULL,
  `total_pages` int DEFAULT NULL,
  `page_index` int NOT NULL,
  `success` tinyint(1) NOT NULL DEFAULT 0,
  `file_path` varchar(1024) DEFAULT NULL,
  `storage_type` varchar(32) NOT NULL DEFAULT 'FILE',
  `mime_type` varchar(100) DEFAULT 'multipart/related',
  `file_size_bytes` bigint DEFAULT NULL,
  `content_sha256` varchar(64) DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` text,
  `mhtml_cached` tinyint(1) NOT NULL DEFAULT 0,
  `mhtml_cached_at` datetime DEFAULT NULL,
  `mhtml_content` longtext,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`page_result_id`),
  UNIQUE KEY `uk_task_page` (`task_id`, `page_index`, `page_url`(255)),
  KEY `idx_page_result_task_id` (`task_id`),
  KEY `idx_page_result_node_id` (`node_id`),
  KEY `idx_page_result_success_created_at` (`success`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  KEY `idx_task_file_sha256` (`content_sha256`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  KEY `idx_task_event_task_id` (`task_id`, `created_at`),
  KEY `idx_task_event_node_id` (`node_id`, `created_at`),
  KEY `idx_task_event_type` (`event_type`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

ALTER TABLE `task`
  ADD CONSTRAINT `fk_task_user`
  FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
  ON DELETE SET NULL
  ON UPDATE CASCADE;

ALTER TABLE `task_runtime`
  ADD CONSTRAINT `fk_task_runtime_task`
  FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `crawler_page_result`
  ADD CONSTRAINT `fk_page_result_task`
  FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `task_file`
  ADD CONSTRAINT `fk_task_file_task`
  FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

ALTER TABLE `task_file`
  ADD CONSTRAINT `fk_task_file_page_result`
  FOREIGN KEY (`page_result_id`) REFERENCES `crawler_page_result` (`page_result_id`)
  ON DELETE SET NULL
  ON UPDATE CASCADE;

ALTER TABLE `task_event`
  ADD CONSTRAINT `fk_task_event_task`
  FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;

INSERT INTO `user` (`user_id`, `username`, `password`, `email`, `role`, `status`, `password_updated_at`)
VALUES
  (1, 'admin', '$2a$10$anG920EXLisfJbEPHE9eEOOj3A2h2Wi7vAcIPBLCCUsQ1TAOxddV2', 'admin@example.com', 'admin', 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE
  `username` = VALUES(`username`),
  `password` = VALUES(`password`),
  `role` = VALUES(`role`),
  `status` = VALUES(`status`);

SET FOREIGN_KEY_CHECKS = 1;
