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

-- Map role ids
-- (If you prefer fixed role_ids, insert with explicit role_id values instead)
SELECT role_id, role_name FROM role;

-- Users (use deterministic UUIDs so other services can reference them)
-- NOTE: password_hash is just mock, not real bcrypt.
INSERT INTO app_user
  (user_id, email, password_hash, first_name, last_name, phone, is_active, deleted, deleted_at, role_id)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'analyst1@fincen.local',  '$2a$10$mockhashAnalyst', 'Alex',  'Analyst',  '555-0101', TRUE, FALSE, NULL,
    (SELECT role_id FROM role WHERE role_name='ANALYST')),
  ('22222222-2222-2222-2222-222222222222', 'comp1@fincen.local',     '$2a$10$mockhashComp',    'Casey', 'Compliance','555-0102', TRUE, FALSE, NULL,
    (SELECT role_id FROM role WHERE role_name='COMPLIANCE_USER')),
  ('33333333-3333-3333-3333-333333333333', 'leo1@fincen.local',      NULL,                     'Taylor','LEO',       '555-0103', TRUE, FALSE, NULL,
    (SELECT role_id FROM role WHERE role_name='LAW_ENFORCEMENT_USER'));

-- OAuth identities
INSERT INTO oauth_identity
  (user_id, provider, provider_user_id, email_at_provider, created_at, revoked, revoked_at)
VALUES
  ('33333333-3333-3333-3333-333333333333', 'google', 'google-oauth2|leo1', 'leo1@fincen.local', NOW() - INTERVAL 20 DAY, FALSE, NULL);
