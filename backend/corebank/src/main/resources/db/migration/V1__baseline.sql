-- Canonical Pre-Ledger Baseline Schema (Phase 4A.5 / Slice 4B-1)
-- Captured from unmodified Hibernate DDL on clean MySQL 8.4
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `accounts` (
  `account_id` binary(16) NOT NULL,
  `account_number` varchar(50) NOT NULL,
  `account_status` enum('ACTIVE','CLOSED','FROZEN') NOT NULL,
  `account_type` enum('CHECKING','FIXED_DEPOSIT','SAVINGS') NOT NULL,
  `balance` decimal(19,4) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `branch_id` int DEFAULT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `UK6kplolsdtr3slnvx97xsy2kc8` (`account_number`),
  KEY `FK6s1ks79nqt6d16ub5ygm7nm7t` (`branch_id`),
  KEY `FKn6x8pdp50os8bq5rbb792upse` (`customer_id`),
  CONSTRAINT `FK6s1ks79nqt6d16ub5ygm7nm7t` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`branch_id`),
  CONSTRAINT `FKn6x8pdp50os8bq5rbb792upse` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `ai_models` (
  `ai_model_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `last_trained_at` datetime(6) DEFAULT NULL,
  `model_name` varchar(100) NOT NULL,
  `model_type` varchar(50) NOT NULL,
  `model_version` varchar(50) NOT NULL,
  `status` enum('ACTIVE','DEPRECATED') NOT NULL,
  PRIMARY KEY (`ai_model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `audit_logs` (
  `audit_log_id` binary(16) NOT NULL,
  `action_type` varchar(100) NOT NULL,
  `description` text,
  `ip_address` varchar(45) DEFAULT NULL,
  `timestamp` datetime(6) NOT NULL,
  `device_id` binary(16) DEFAULT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`audit_log_id`),
  KEY `FKfy0qnglyra8bv4ttd8y4r9a6i` (`device_id`),
  KEY `FKjs4iimve3y0xssbtve5ysyef0` (`user_id`),
  CONSTRAINT `FKfy0qnglyra8bv4ttd8y4r9a6i` FOREIGN KEY (`device_id`) REFERENCES `devices` (`device_id`),
  CONSTRAINT `FKjs4iimve3y0xssbtve5ysyef0` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `branches` (
  `branch_id` int NOT NULL AUTO_INCREMENT,
  `address` text NOT NULL,
  `branch_name` varchar(255) NOT NULL,
  `contact_number` varchar(50) DEFAULT NULL,
  `manager_employee_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`branch_id`),
  KEY `FKgtxr6ylr53ynkekntdxn0mwt5` (`manager_employee_id`),
  CONSTRAINT `FKgtxr6ylr53ynkekntdxn0mwt5` FOREIGN KEY (`manager_employee_id`) REFERENCES `employees` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `cards` (
  `card_id` binary(16) NOT NULL,
  `card_number` varchar(20) NOT NULL,
  `card_type` enum('CREDIT','DEBIT','PREPAID') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `cvv` varchar(255) NOT NULL,
  `expiry_date` date NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `status` enum('ACTIVE','BLOCKED','EXPIRED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `account_id` binary(16) NOT NULL,
  `linked_customer_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`card_id`),
  UNIQUE KEY `UKqualp9iflk959u561wanavuj1` (`card_number`),
  KEY `FKdjw7dinkpc0f01yk4m57uq2u2` (`account_id`),
  KEY `FK51nux5che7tdrmnod78yj60qx` (`linked_customer_id`),
  CONSTRAINT `FK51nux5che7tdrmnod78yj60qx` FOREIGN KEY (`linked_customer_id`) REFERENCES `customers` (`customer_id`),
  CONSTRAINT `FKdjw7dinkpc0f01yk4m57uq2u2` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `credit_scores` (
  `credit_score_id` binary(16) NOT NULL,
  `evaluation_date` datetime(6) NOT NULL,
  `notes` text,
  `score` int NOT NULL,
  `source` varchar(100) NOT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`credit_score_id`),
  KEY `FK12r5yw2j8107i0aw0k81poeiv` (`customer_id`),
  CONSTRAINT `FK12r5yw2j8107i0aw0k81poeiv` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `customers` (
  `customer_id` binary(16) NOT NULL,
  `address` text,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date NOT NULL,
  `email` varchar(150) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','PENDING','RESIGNED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `UKrfbvkrffamfql7cjmen8v976v` (`email`),
  UNIQUE KEY `UKeuat1oase6eqv195jvb71a93s` (`user_id`),
  CONSTRAINT `FKrh1g1a20omjmn6kurd35o3eit` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `devices` (
  `device_id` binary(16) NOT NULL,
  `device_token` varchar(255) NOT NULL,
  `device_type` varchar(100) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `registered_at` datetime(6) NOT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `UKng9gr5temxcsc7ybgoyolhg9x` (`device_token`),
  KEY `FKmemo5fyn0w6ownvxob5lcd4e9` (`customer_id`),
  CONSTRAINT `FKmemo5fyn0w6ownvxob5lcd4e9` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `employees` (
  `employee_id` binary(16) NOT NULL,
  `address` varchar(255) NOT NULL,
  `date_of_birth` date NOT NULL,
  `department` varchar(100) DEFAULT NULL,
  `email` varchar(150) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') NOT NULL,
  `hire_date` datetime(6) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `resignation_date` datetime(6) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE','PENDING','RESIGNED') NOT NULL,
  `manager_id` binary(16) DEFAULT NULL,
  `role_id` binary(16) NOT NULL,
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `UKj9xgmd0ya5jmus09o0b8pqrpb` (`email`),
  KEY `FKi4365uo9af35g7jtbc2rteukt` (`manager_id`),
  KEY `FKah490190ww1q2a4piuv41hk6e` (`role_id`),
  CONSTRAINT `FKah490190ww1q2a4piuv41hk6e` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`),
  CONSTRAINT `FKi4365uo9af35g7jtbc2rteukt` FOREIGN KEY (`manager_id`) REFERENCES `employees` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `feedbacks` (
  `feedback_id` binary(16) NOT NULL,
  `feedback_text` text NOT NULL,
  `rating` int NOT NULL,
  `response_status` enum('PENDING','REVIEWED') NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`feedback_id`),
  KEY `FKi9b9keigxngo4a35fgwt4h2v6` (`customer_id`),
  CONSTRAINT `FKi9b9keigxngo4a35fgwt4h2v6` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `file_storage` (
  `file_id` binary(16) NOT NULL,
  `checksum` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` int DEFAULT NULL,
  `file_type` varchar(100) NOT NULL,
  `uploaded_at` datetime(6) NOT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`file_id`),
  KEY `FK2d5rvig5oq6sxbritf923obpe` (`customer_id`),
  CONSTRAINT `FK2d5rvig5oq6sxbritf923obpe` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `kyc_cases` (
  `id` varchar(36) NOT NULL,
  `address_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `decided_at` datetime(6) DEFAULT NULL,
  `decision_reason` varchar(500) DEFAULT NULL,
  `doc_back_id` varchar(36) NOT NULL,
  `doc_front_id` varchar(36) NOT NULL,
  `processing` bit(1) NOT NULL,
  `reviewed_by` binary(16) DEFAULT NULL,
  `reviewer_notes` varchar(1000) DEFAULT NULL,
  `selfie_id` varchar(36) NOT NULL,
  `status` enum('APPROVED','AUTO_REVIEW','NEEDS_MORE_INFO','PENDING','REJECTED','SUPERSEDED','UNDER_REVIEW') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kyc_cases_user_id` (`user_id`),
  KEY `idx_kyc_cases_status` (`status`),
  KEY `idx_kyc_cases_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `kyc_checks` (
  `id` varchar(36) NOT NULL,
  `case_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `details_json` text,
  `passed` bit(1) DEFAULT NULL,
  `score` double DEFAULT NULL,
  `type` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kyc_checks_case` (`case_id`),
  KEY `idx_kyc_checks_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `kyc_idem_keys` (
  `id` binary(16) NOT NULL,
  `case_id` varchar(36) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `idem_key` varchar(100) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kik_user_idem_key` (`user_id`,`idem_key`),
  KEY `idx_kik_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `kyc_uploads` (
  `id` binary(16) NOT NULL,
  `checksum_sha256` varchar(64) DEFAULT NULL,
  `content_type` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `original_filename` varchar(255) DEFAULT NULL,
  `size_bytes` bigint NOT NULL,
  `storage_path` varchar(1024) NOT NULL,
  `stored_filename` varchar(255) NOT NULL,
  `type` varchar(40) NOT NULL,
  `uploaded_by` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_kyc_uploads_uploaded_by` (`uploaded_by`),
  KEY `idx_kyc_uploads_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `loan_repayments` (
  `repayment_id` binary(16) NOT NULL,
  `amount` decimal(19,4) NOT NULL,
  `payment_date` datetime(6) NOT NULL,
  `payment_method` varchar(50) NOT NULL,
  `loan_id` binary(16) NOT NULL,
  `transaction_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`repayment_id`),
  KEY `FKmvfjvk48bhsvwbdis0s9uwn1t` (`loan_id`),
  KEY `FKqwxvbwl0d8bsq52a086x8axsy` (`transaction_id`),
  CONSTRAINT `FKmvfjvk48bhsvwbdis0s9uwn1t` FOREIGN KEY (`loan_id`) REFERENCES `loans` (`loan_id`),
  CONSTRAINT `FKqwxvbwl0d8bsq52a086x8axsy` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `loans` (
  `loan_id` binary(16) NOT NULL,
  `amount` decimal(19,4) NOT NULL,
  `application_date` datetime(6) NOT NULL,
  `approval_date` datetime(6) DEFAULT NULL,
  `due_date` datetime(6) NOT NULL,
  `interest_rate` decimal(5,2) NOT NULL,
  `loan_term` int NOT NULL,
  `loan_type` enum('MORTGAGE','PERSONAL') NOT NULL,
  `payment_frequency` enum('MONTHLY','QUARTERLY','YEARLY') NOT NULL,
  `remaining_balance` decimal(19,4) NOT NULL,
  `status` enum('APPROVED','PAID','PENDING','REJECTED') NOT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`loan_id`),
  KEY `FK3s60kbg0a404doyf8ii6qwb9g` (`customer_id`),
  CONSTRAINT `FK3s60kbg0a404doyf8ii6qwb9g` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `notifications` (
  `notification_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `delivery_status` enum('FAILED','PENDING','SENT') NOT NULL,
  `message` text NOT NULL,
  `notification_type` enum('EMAIL','IN_APP','SMS') NOT NULL,
  `read_status` bit(1) NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK30dp6ycner3dgso3scgc9vghy` (`customer_id`),
  CONSTRAINT `FK30dp6ycner3dgso3scgc9vghy` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `used` bit(1) NOT NULL,
  `user_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKla2ts67g4oh2sreayswhox1i6` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `roles` (
  `role_id` binary(16) NOT NULL,
  `description` text,
  `permissions` json DEFAULT NULL,
  `role_name` varchar(50) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `UK716hgxp60ym1lifrdgp67xt5k` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `scheduled_transactions` (
  `scheduled_transaction_id` binary(16) NOT NULL,
  `amount` decimal(19,4) NOT NULL,
  `recurrence_pattern` varchar(50) DEFAULT NULL,
  `scheduled_date` datetime(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','PENDING') NOT NULL,
  `transaction_type` enum('DEPOSIT','TRANSFER','WITHDRAWAL') NOT NULL,
  `account_id` binary(16) NOT NULL,
  PRIMARY KEY (`scheduled_transaction_id`),
  KEY `FKo7vcwhrhl74tx4j8gt0opw2ah` (`account_id`),
  CONSTRAINT `FKo7vcwhrhl74tx4j8gt0opw2ah` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `sessions` (
  `session_id` binary(16) NOT NULL,
  `expiry_time` datetime(6) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `login_time` datetime(6) NOT NULL,
  `logout_time` datetime(6) DEFAULT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `UKnr21vuswfbmr91q57xdbnou5f` (`token`),
  KEY `FKruie73rneumyyd1bgo6qw8vjt` (`user_id`),
  CONSTRAINT `FKruie73rneumyyd1bgo6qw8vjt` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `support_tickets` (
  `ticket_id` binary(16) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `priority` enum('HIGH','LOW','MEDIUM') NOT NULL,
  `status` enum('CLOSED','IN_PROGRESS','OPEN') NOT NULL,
  `subject` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `assigned_to_employee_id` binary(16) DEFAULT NULL,
  `customer_id` binary(16) NOT NULL,
  PRIMARY KEY (`ticket_id`),
  KEY `FKtitqodi631mckrlj9gnx1e6yk` (`assigned_to_employee_id`),
  KEY `FKbj61s5pm6gwms5405fcdvgm1t` (`customer_id`),
  CONSTRAINT `FKbj61s5pm6gwms5405fcdvgm1t` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`),
  CONSTRAINT `FKtitqodi631mckrlj9gnx1e6yk` FOREIGN KEY (`assigned_to_employee_id`) REFERENCES `employees` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `system_configs` (
  `config_id` binary(16) NOT NULL,
  `config_key` varchar(100) NOT NULL,
  `config_type` varchar(50) NOT NULL,
  `config_value` text NOT NULL,
  `description` text,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `UKpk5mof051xp5r3e75s2e23s8s` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `transactions` (
  `transaction_id` binary(16) NOT NULL,
  `amount` decimal(19,2) NOT NULL,
  `balance_after` decimal(19,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `type` enum('DEPOSIT','TRANSFER_IN','TRANSFER_OUT','WITHDRAWAL') NOT NULL,
  `account_id` binary(16) NOT NULL,
  PRIMARY KEY (`transaction_id`),
  KEY `FK20w7wsg13u9srbq3bd7chfxdh` (`account_id`),
  CONSTRAINT `FK20w7wsg13u9srbq3bd7chfxdh` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `users` (
  `user_id` binary(16) NOT NULL,
  `address` text,
  `city` text,
  `country` text,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `email_verification_token` varchar(255) DEFAULT NULL,
  `email_verification_token_created_at` datetime(6) DEFAULT NULL,
  `is_email_verified` bit(1) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `home_number` text,
  `is_active` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `mobile_number` text,
  `new_email` varchar(150) DEFAULT NULL,
  `office_number` text,
  `password_hash` varchar(255) NOT NULL,
  `postal_code` text,
  `state` text,
  `username` varchar(100) NOT NULL,
  `work_number` text,
  `customer_id` binary(16) DEFAULT NULL,
  `employee_id` binary(16) DEFAULT NULL,
  `role_id` binary(16) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK3xurg673vujjqiapp8uijx8jj` (`new_email`),
  UNIQUE KEY `UKd1s31g1a7ilra77m65xmka3ei` (`employee_id`),
  KEY `FKchxdoybbydcaj5smgxe0qq5mk` (`customer_id`),
  KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
  CONSTRAINT `FK6p2ib82uai0pj9yk1iassppgq` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`employee_id`),
  CONSTRAINT `FKchxdoybbydcaj5smgxe0qq5mk` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`),
  CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `verification_token` (
  `id` binary(16) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp678btf3r9yu6u8aevyb4ff0m` (`token`),
  UNIQUE KEY `UKq6jibbenp7o9v6tq178xg88hg` (`user_id`),
  CONSTRAINT `FK3asw9wnv76uxu3kr1ekq4i1ld` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_card_sessions` (
  `id` varchar(64) NOT NULL,
  `consumed` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_cards` (
  `id` binary(16) NOT NULL,
  `brand` varchar(32) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `customer_token` varchar(128) NOT NULL,
  `expiry_month` int DEFAULT NULL,
  `expiry_year` int DEFAULT NULL,
  `is_default` bit(1) NOT NULL,
  `last4` varchar(8) DEFAULT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_idempotency` (
  `idem_key` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `request_hash` varchar(255) NOT NULL,
  `response_json` longtext NOT NULL,
  PRIMARY KEY (`idem_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `wallet_payment_intents` (
  `id` varchar(64) NOT NULL,
  `amount_currency` varchar(8) NOT NULL,
  `amount_value` double NOT NULL,
  `biller_id` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `idempotency_key` varchar(255) DEFAULT NULL,
  `merchant_ref` varchar(255) DEFAULT NULL,
  `msisdn` varchar(255) DEFAULT NULL,
  `provider_client_secret` varchar(255) DEFAULT NULL,
  `qr_data` varchar(255) DEFAULT NULL,
  `reference` varchar(255) DEFAULT NULL,
  `return_url` varchar(255) DEFAULT NULL,
  `status` enum('CANCELED','FAILED','PENDING','PROCESSING','SUCCESS') NOT NULL,
  `type` enum('BILL','QR','RELOAD') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
