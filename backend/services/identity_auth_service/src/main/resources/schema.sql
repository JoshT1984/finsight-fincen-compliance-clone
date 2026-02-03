-- ===========================
-- Identity/Auth Service Schema
-- ===========================

CREATE TABLE IF NOT EXISTS role (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS app_user (
  user_id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255), -- nullable for OAuth-only
  first_name VARCHAR(64),
  last_name VARCHAR(64),
  phone VARCHAR(32),
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  deleted_at TIMESTAMP NULL DEFAULT NULL,
  role_id INT NOT NULL,
  FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS oauth_identity (
  oauth_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  provider_user_id VARCHAR(256) NOT NULL,
  email_at_provider VARCHAR(320),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  revoked_at TIMESTAMP NULL DEFAULT NULL,
  UNIQUE (provider, provider_user_id),
  FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

-- Seed roles per MVP Section 4: Analyst, Compliance User, Law Enforcement User
INSERT INTO role(role_name) VALUES ('ANALYST') ON DUPLICATE KEY UPDATE role_name=role_name;
INSERT INTO role(role_name) VALUES ('COMPLIANCE_USER') ON DUPLICATE KEY UPDATE role_name=role_name;
INSERT INTO role(role_name) VALUES ('LAW_ENFORCEMENT_USER') ON DUPLICATE KEY UPDATE role_name=role_name;
