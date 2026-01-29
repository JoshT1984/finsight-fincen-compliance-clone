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

-- OAuth identities (1:M with app_user)
INSERT INTO oauth_identity (oauth_id, user_id, provider, provider_user_id, email_at_provider, revoked) VALUES
  (1, '11111111-1111-1111-1111-111111111101', 'GOOGLE', 'google-oauth-101', 'jane.analyst@finsight.gov', 0),
  (2, '11111111-1111-1111-1111-111111111102', 'MICROSOFT', 'ms-oauth-102', 'bob.compliance@finsight.gov', 0) AS new_row
ON DUPLICATE KEY UPDATE provider_user_id = new_row.provider_user_id;
