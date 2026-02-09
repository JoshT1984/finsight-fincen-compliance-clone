-- ============================================
-- Mock Data: Identity/Auth Service
-- Database: finsight
-- Run against finsight DB. Prerequisite: schema.sql
-- Roles per MVP Section 4: Analyst, Compliance User, Law Enforcement User
-- ============================================

USE finsight;

-- Ensure roles exist with explicit IDs (MVP Section 4)
-- 1=ANALYST, 2=COMPLIANCE_USER, 3=LAW_ENFORCEMENT_USER
INSERT INTO role (role_id, role_name) VALUES
  (1, 'ANALYST'),
  (2, 'COMPLIANCE_USER'),
  (3, 'LAW_ENFORCEMENT_USER') AS new_row
ON DUPLICATE KEY UPDATE role_name = new_row.role_name;

-- App users (fixed UUIDs for case_note.author_user_id references)
-- Jane=Analyst (reviews SARs, adds notes, refers cases), Bob=Compliance (uploads CTR/SAR PDFs), Alice=Law Enforcement (read-only referred cases)
INSERT INTO app_user (user_id, email, password_hash, first_name, last_name, phone, is_active, role_id) VALUES
  ('11111111-1111-1111-1111-111111111101', 'jane.analyst@finsight.gov', NULL, 'Jane', 'Analyst', '555-0101', 1, 1),
  ('11111111-1111-1111-1111-111111111102', 'bob.compliance@finsight.gov', NULL, 'Bob', 'Compliance', '555-0102', 1, 2),
  ('11111111-1111-1111-1111-111111111103', 'alice.leo@finsight.gov', NULL, 'Alice', 'Leo', '555-0103', 1, 3) AS new_row
ON DUPLICATE KEY UPDATE email = new_row.email;

INSERT INTO oauth_identity (oauth_id, user_id, provider, provider_user_id, email_at_provider, revoked) VALUES
  (1, '11111111-1111-1111-1111-111111111101', 'GOOGLE', 'google-oauth-101', 'jane.analyst@finsight.gov', 0),
  (2, '11111111-1111-1111-1111-111111111102', 'MICROSOFT', 'ms-oauth-102', 'bob.compliance@finsight.gov', 0) AS new_row
ON DUPLICATE KEY UPDATE provider_user_id = new_row.provider_user_id;


-- Additional app_user mock data for testing
INSERT INTO app_user (
  user_id, email, password_hash, first_name, last_name,
  phone, is_active, deleted, created_at, updated_at, deleted_at, role_id
) VALUES 
(UUID(), 'matthew.wright9630@gmail.com',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Matthew', 'Wright', '2146067487', TRUE, FALSE, NOW(), NOW(), NULL, 2),
-- Analysts
(UUID(), 'test@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Test', 'Analyst', '555-0100', TRUE, FALSE, NOW(), NOW(), NULL, 1),

(UUID(), 'leahanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Leah', 'Analyst', '555-0101', TRUE, FALSE, NOW(), NOW(), NULL, 1),

(UUID(), 'joshanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Josh', 'Analyst', '555-0102', TRUE, FALSE, NOW(), NOW(), NULL, 1),

(UUID(), 'matthewanalyst@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Matthew', 'Analyst', '555-0103', TRUE, FALSE, NOW(), NOW(), NULL, 1),

-- Compliance
(UUID(), 'leahcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Leah', 'Compliance', '555-0201', TRUE, FALSE, NOW(), NOW(), NULL, 2),

(UUID(), 'joshcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Josh', 'Compliance', '555-0202', TRUE, FALSE, NOW(), NOW(), NULL, 2),

(UUID(), 'matthewcompliance@fincen.local',
 '$2a$12$5P/qyGmiJEX1PAwNklkiVe2CqsrTIRqIQEDS0OMEFUxHrS00zpNJS',
 'Matthew', 'Compliance', '555-0203', TRUE, FALSE, NOW(), NOW(), NULL, 2),

-- LEU
(UUID(), 'leahleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Leah', 'LEU', '555-0301', TRUE, FALSE, NOW(), NOW(), NULL, 3),

(UUID(), 'joshleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Josh', 'LEU', '555-0302', TRUE, FALSE, NOW(), NOW(), NULL, 3),

(UUID(), 'matthewleu@fincen.local',
 '$2a$10$6NwfldQ4mygbmfdwUIjU/O316yraEmGTvPSYPHiuw2zm51vvgrs4u',
 'Matthew', 'LEU', '555-0303', TRUE, FALSE, NOW(), NOW(), NULL, 3);
