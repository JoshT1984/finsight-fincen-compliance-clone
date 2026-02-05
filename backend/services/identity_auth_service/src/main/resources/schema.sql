-- ===========================
-- Identity/Auth Service Schema (MySQL 8+ friendly)
-- Fixes: removes integer display width warnings (TINYINT(1)),
-- and aligns FK column types (VARCHAR(36) everywhere).
-- ===========================

-- Optional (recommended): ensure consistent engine + charset/collation
-- Adjust collation if your org standard differs.
-- (No need to run if your DB already enforces these defaults.)
-- SET NAMES utf8mb4;
-- SET collation_connection = 'utf8mb4_0900_ai_ci';

DROP TABLE oauth_identity cascade;
DROP TABLE app_user cascade;

CREATE TABLE IF NOT EXISTS role (
  role_id INT NOT NULL AUTO_INCREMENT,
  role_name VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_id),
  UNIQUE KEY uq_role_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS app_user (
  user_id VARCHAR(36) NOT NULL,
  email VARCHAR(320) NOT NULL,
  password_hash VARCHAR(255) NULL,         -- nullable for OAuth-only
  first_name VARCHAR(64) NULL,
  last_name VARCHAR(64) NULL,
  phone VARCHAR(32) NULL,

  is_active BOOLEAN NOT NULL DEFAULT TRUE, -- replaces TINYINT(1)
  deleted  BOOLEAN NOT NULL DEFAULT FALSE, -- replaces TINYINT(1)

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL DEFAULT NULL,

  role_id INT NOT NULL,

  PRIMARY KEY (user_id),
  UNIQUE KEY uq_app_user_email (email),
  KEY idx_app_user_role_id (role_id),

  CONSTRAINT fk_app_user_role
    FOREIGN KEY (role_id) REFERENCES role(role_id)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS oauth_identity (
  oauth_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id VARCHAR(36) NOT NULL,            -- matches app_user.user_id type
  provider VARCHAR(32) NOT NULL,
  provider_user_id VARCHAR(256) NOT NULL,
  email_at_provider VARCHAR(320) NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  revoked BOOLEAN NOT NULL DEFAULT FALSE,  -- replaces TINYINT(1)
  revoked_at TIMESTAMP NULL DEFAULT NULL,

  PRIMARY KEY (oauth_id),
  UNIQUE KEY uq_oauth_provider_user (provider, provider_user_id),
  KEY idx_oauth_user_id (user_id),

  CONSTRAINT fk_oauth_identity_user
    FOREIGN KEY (user_id) REFERENCES app_user(user_id)
    ON DELETE CASCADE
    ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed roles per MVP Section 4: Analyst, Compliance User, Law Enforcement User
INSERT INTO role(role_name) VALUES ('ANALYST')
  ON DUPLICATE KEY UPDATE role_name = role_name;

INSERT INTO role(role_name) VALUES ('COMPLIANCE_USER')
  ON DUPLICATE KEY UPDATE role_name = role_name;

INSERT INTO role(role_name) VALUES ('LAW_ENFORCEMENT_USER')
  ON DUPLICATE KEY UPDATE role_name = role_name;
