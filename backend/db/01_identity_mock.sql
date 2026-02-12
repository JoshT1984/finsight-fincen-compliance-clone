-- =========================================================
-- Mock Data: Identity/Auth Service
-- Tables: role, app_user, oauth_identity
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE oauth_identity;
TRUNCATE TABLE app_user;
-- keep role table if you want stable role ids, or truncate and reseed
-- TRUNCATE TABLE role;
SET FOREIGN_KEY_CHECKS = 1;

-- Roles (idempotent)
INSERT INTO role(role_name) VALUES ('ANALYST')
  ON DUPLICATE KEY UPDATE role_name = role_name;
INSERT INTO role(role_name) VALUES ('COMPLIANCE_USER')
  ON DUPLICATE KEY UPDATE role_name = role_name;
INSERT INTO role(role_name) VALUES ('LAW_ENFORCEMENT_USER')
  ON DUPLICATE KEY UPDATE role_name = role_name;

INSERT INTO organization (organization_id, name) VALUES
  ('org-1234-5678-9012-345678901234', 'Fincen Analytics Team')
  ON DUPLICATE KEY UPDATE name = name,
  ('org-2345-6789-0123-456789012345', 'Chase Bank 5')
  ON DUPLICATE KEY UPDATE name = name,
  ('org-3456-7890-1234-567890123456', 'Old National Bank')
  ON DUPLICATE KEY UPDATE name = name,
  ('org-4567-8901-2345-678901234567', 'National Financial Crimes Task Force (NFCTF)')
  ON DUPLICATE KEY UPDATE name = name,
  ('org-5678-9012-3456-789012345678', 'Interstate Economic Crimes Bureau (IECB)')
  ON DUPLICATE KEY UPDATE name = name;

-- Map role ids
-- (If you prefer fixed role_ids, insert with explicit role_id values instead)
SELECT role_id, role_name FROM role;

-- Users (use deterministic UUIDs so other services can reference them)
-- NOTE: password_hash is just mock, not real bcrypt.
INSERT INTO app_user
  (user_id, email, password_hash, first_name, last_name, phone, is_active, deleted, deleted_at, role_id, organization_id)
VALUES
  (UUID(), 'matthew.wright9630@gmail.com',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Matthew', 'Wright', '2146067487', TRUE, FALSE, NOW(), NOW(), NULL, 2, 'org-2345-6789-0123-456789012345'),
-- Analysts
(UUID(), 'test@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Test', 'Analyst', '555-0100', TRUE, FALSE, NOW(), NOW(), NULL, 1, 'org-1234-5678-9012-345678901234'),

(UUID(), 'leahanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Leah', 'Analyst', '555-0101', TRUE, FALSE, NOW(), NOW(), NULL, 1, 'org-1234-5678-9012-345678901234'),

(UUID(), 'joshanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Josh', 'Analyst', '555-0102', TRUE, FALSE, NOW(), NOW(), NULL, 1, 'org-1234-5678-9012-345678901234'),

(UUID(), 'matthewanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Matthew', 'Analyst', '555-0103', TRUE, FALSE, NOW(), NOW(), NULL, 1, 'org-1234-5678-9012-345678901234'),

-- Compliance
(UUID(), 'leahcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Leah', 'Compliance', '555-0201', TRUE, FALSE, NOW(), NOW(), NULL, 2, 'org-2345-6789-0123-456789012345'),

(UUID(), 'joshcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Josh', 'Compliance', '555-0202', TRUE, FALSE, NOW(), NOW(), NULL, 2, 'org-3456-7890-1234-567890123456'),

(UUID(), 'matthewcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Matthew', 'Compliance', '555-0203', TRUE, FALSE, NOW(), NOW(), NULL, 2, 'org-3456-7890-1234-567890123456'),

-- LEU
(UUID(), 'leahleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Leah', 'LEU', '555-0301', TRUE, FALSE, NOW(), NOW(), NULL, 3, 'org-4567-8901-2345-678901234567'),

(UUID(), 'joshleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Josh', 'LEU', '555-0302', TRUE, FALSE, NOW(), NOW(), NULL, 3, 'org-4567-8901-2345-678901234567'),

(UUID(), 'matthewleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Matthew', 'LEU', '555-0303', TRUE, FALSE, NOW(), NOW(), NULL, 3, 'org-5678-9012-3456-789012345678');
-- OAuth identities
INSERT INTO oauth_identity
  (user_id, provider, provider_user_id, email_at_provider, created_at, revoked, revoked_at)
VALUES
  ('33333333-3333-3333-3333-333333333333', 'google', 'google-oauth2|leo1', 'leo1@fincen.local', NOW() - INTERVAL 20 DAY, FALSE, NULL);
