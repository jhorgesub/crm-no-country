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

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `active` tinyint(1) DEFAULT NULL,
  `account_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','OWNER','USER') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_user_id` bigint DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `company_name` varchar(255) NOT NULL,
  `date_format` varchar(255) DEFAULT NULL,
  `industry` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `time_zone` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjin9lrk7x32q57psf2feiytrv` (`owner_user_id`),
  CONSTRAINT `FKjin9lrk7x32q57psf2feiytrv` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `users` ADD CONSTRAINT `FKfm8rm8ks0kgj4fhlmmljkj17x` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`);

DROP TABLE IF EXISTS `crm_lead`;
CREATE TABLE `crm_lead` (
  `deleted` bit(1) NOT NULL,
  `account_id` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `channel` enum('EMAIL','WHATSAPP') DEFAULT NULL,
  `stage` enum('ACTIVE_LEAD','CLIENT','FOLLOW_UP','LOST') DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbygvcik6lx2u4qj9n7vdb878k` (`email`),
  KEY `FKkj2rnspcesd4cv405lh0qyb1g` (`account_id`),
  KEY `FK7erhxnytwi59wucx506jm5ths` (`owner_id`),
  CONSTRAINT `FK7erhxnytwi59wucx506jm5ths` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKkj2rnspcesd4cv405lh0qyb1g` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `automation_rule`;
CREATE TABLE `automation_rule` (
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actions` json DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `trigger_value` varchar(255) DEFAULT NULL,
  `trigger_event` enum('CONTRACT_SIGNED','DEMO_COMPLETED','INVOICE_SENT','LEAD_CREATED','NO_RESPONSE_7_DAYS','PAYMENT_RECEIVED','STAGE_CHANGED') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8ffmn9eiwf7fsxpmlbtjfef5s` (`created_by`),
  CONSTRAINT `FK8ffmn9eiwf7fsxpmlbtjfef5s` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `unread_count` int DEFAULT NULL,
  `assigned_user_id` bigint DEFAULT NULL,
  `first_inbound_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_message_at` datetime(6) DEFAULT NULL,
  `lead_id` bigint DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `external_id` varchar(255) DEFAULT NULL,
  `last_message_text` text,
  `channel` enum('EMAIL','WHATSAPP') NOT NULL,
  `last_message_direction` enum('INBOUND','OUTBOUND') DEFAULT NULL,
  `status` enum('CLOSED','OPEN') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbx7iwrttrg52lh89j7wrnt2mv` (`assigned_user_id`),
  KEY `FK7wu7y6blnyda8wouru45wcwxt` (`lead_id`),
  CONSTRAINT `FK7wu7y6blnyda8wouru45wcwxt` FOREIGN KEY (`lead_id`) REFERENCES `crm_lead` (`id`),
  CONSTRAINT `FKbx7iwrttrg52lh89j7wrnt2mv` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `email_log`;
CREATE TABLE `email_log` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lead_id` bigint DEFAULT NULL,
  `body` text,
  `provider_message_id` varchar(255) DEFAULT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `status` enum('DELIVERED','FAILED','OPENED','SENT') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKiscymqjfap456aobbjk35rydm` (`lead_id`),
  CONSTRAINT `FKiscymqjfap456aobbjk35rydm` FOREIGN KEY (`lead_id`) REFERENCES `crm_lead` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `email_template`;
CREATE TABLE `email_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `body` text,
  `name` varchar(255) NOT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `integration_config`;
CREATE TABLE `integration_config` (
  `is_connected` tinyint(1) NOT NULL DEFAULT '0',
  `account_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `credentials` json DEFAULT NULL,
  `integration_type` enum('EMAIL','WHATSAPP') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK38oker98ko1s03wo6ym40g9bi` (`account_id`),
  CONSTRAINT `FK38oker98ko1s03wo6ym40g9bi` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `lead_history`;
CREATE TABLE `lead_history` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lead_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `attribute` varchar(255) DEFAULT NULL,
  `state` text,
  `action` enum('ASSIGNED','CREATED','NOTE','STATUS_CHANGE','UPDATED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj2fee3gur6h7bmyk807uoibtv` (`lead_id`),
  KEY `FKd638te7te3nvnxttkali3oyhy` (`user_id`),
  CONSTRAINT `FKd638te7te3nvnxttkali3oyhy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj2fee3gur6h7bmyk807uoibtv` FOREIGN KEY (`lead_id`) REFERENCES `crm_lead` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `lead_tag`;
CREATE TABLE `lead_tag` (
  `lead_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`lead_id`,`tag_id`),
  KEY `FKoupixk123y0elgq6wdlnyl9pl` (`tag_id`),
  CONSTRAINT `FK39k89lwy314oj4b3ls5a7lx05` FOREIGN KEY (`lead_id`) REFERENCES `crm_lead` (`id`),
  CONSTRAINT `FKoupixk123y0elgq6wdlnyl9pl` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `conversation_id` bigint DEFAULT NULL,
  `email_template_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_lead_id` bigint DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `content` text,
  `external_message_id` varchar(255) DEFAULT NULL,
  `media_caption` text,
  `media_file_name` varchar(255) DEFAULT NULL,
  `media_type` varchar(255) DEFAULT NULL,
  `media_url` varchar(255) DEFAULT NULL,
  `direction` enum('INBOUND','OUTBOUND') NOT NULL,
  `message_type` enum('AUDIO','DOCUMENT','EMAIL','IMAGE','STICKER','TEXT','VIDEO') NOT NULL,
  `sender_type` enum('LEAD','USER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6yskk3hxw5sklwgi25y6d5u1l` (`conversation_id`),
  KEY `FKtaijajmniskfximpejauxup5f` (`email_template_id`),
  CONSTRAINT `FK6yskk3hxw5sklwgi25y6d5u1l` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`),
  CONSTRAINT `FKtaijajmniskfximpejauxup5f` FOREIGN KEY (`email_template_id`) REFERENCES `email_template` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lead_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `body` text,
  `entity_type` enum('CONVERSATION','LEAD','TASK') DEFAULT NULL,
  `type` enum('AUTOMATION_ALERT','NEW_MESSAGE','STAGE_CHANGE','TASK_DUE') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKr0tgpe8ibsdtby9mvtd5gjbk4` (`lead_id`),
  KEY `FKnk4ftb5am9ubmkv1661h15ds9` (`user_id`),
  CONSTRAINT `FKnk4ftb5am9ubmkv1661h15ds9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKr0tgpe8ibsdtby9mvtd5gjbk4` FOREIGN KEY (`lead_id`) REFERENCES `crm_lead` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `saved_view`;
CREATE TABLE `saved_view` (
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_used` datetime(6) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `filters` json DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5avjlscasst6b71g5jsac0luy` (`user_id`),
  CONSTRAINT `FK5avjlscasst6b71g5jsac0luy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1wdpsed5kna2y38hnbgrnhi5b` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks` (
  `is_automated` bit(1) DEFAULT NULL,
  `is_completed` bit(1) DEFAULT NULL,
  `assigned_to` bigint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `crm_lead_id` bigint DEFAULT NULL,
  `due_date` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `priority` enum('HIGH','LOW','MEDIUM') DEFAULT NULL,
  `task_type` enum('EMAIL','MESSAGE') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2vjo8mbre3rvpbd6e7976b54m` (`assigned_to`),
  KEY `FKes5ivp7g99wtxc533lqjg1hn3` (`crm_lead_id`),
  CONSTRAINT `FK2vjo8mbre3rvpbd6e7976b54m` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`id`),
  CONSTRAINT `FKes5ivp7g99wtxc533lqjg1hn3` FOREIGN KEY (`crm_lead_id`) REFERENCES `crm_lead` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `user_preferences`;
CREATE TABLE `user_preferences` (
  `auto_save_conv` tinyint(1) DEFAULT '1',
  `dark_mode` tinyint(1) DEFAULT '0',
  `notify_automation_trigger` tinyint(1) DEFAULT '1',
  `notify_new_messages` tinyint(1) DEFAULT '1',
  `notify_stage_change` tinyint(1) DEFAULT '1',
  `notify_task_reminders` tinyint(1) DEFAULT '1',
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `FKepakpib0qnm82vmaiismkqf88` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;