-- ============================================
-- Mock Data: Suspect Registry Service
-- Database: finsight_suspects
-- Run against finsight_suspects DB. Prerequisite: schema.sql
-- Suspect IDs 1,2,3 are referenced by compliance_event snapshots.
-- ============================================

USE finsight_suspects;

-- Suspects (IDs 1, 2, 3 - referenced by suspect_snapshot in compliance_event)
-- ssn/ssn_hash NULL to avoid UNIQUE constraint conflicts
INSERT INTO suspect (suspect_id, primary_name, dob, ssn, ssn_hash, risk_level, created_at, updated_at) VALUES
  (1, 'John Smith', '1980-05-15', NULL, NULL, 'HIGH', '2025-01-10 09:00:00', '2025-01-10 09:00:00'),
  (2, 'Maria Garcia', '1975-11-22', NULL, NULL, 'MEDIUM', '2025-01-11 10:30:00', '2025-01-11 10:30:00'),
  (3, 'Robert Chen', '1990-03-08', NULL, NULL, 'LOW', '2025-01-12 14:00:00', '2025-01-12 14:00:00') AS new_row
ON DUPLICATE KEY UPDATE primary_name = new_row.primary_name;

-- Aliases (1:M - multiple aliases per suspect)
INSERT INTO suspect_alias (alias_id, suspect_id, alias_name, alias_type, is_primary, created_at) VALUES
  (1, 1, 'Johnny S', 'NICKNAME', 0, '2025-01-10 09:05:00'),
  (2, 1, 'J. Smith', 'AKA', 1, '2025-01-10 09:05:00'),
  (3, 2, 'Maria G', 'AKA', 0, '2025-01-11 10:35:00'),
  (4, 2, 'M. Garcia', 'LEGAL', 1, '2025-01-11 10:35:00'),
  (5, 3, 'Bobby Chen', 'NICKNAME', 0, '2025-01-12 14:05:00') AS new_row
ON DUPLICATE KEY UPDATE alias_name = new_row.alias_name;

-- Addresses (distinct to satisfy address_dedup_key UNIQUE)
INSERT INTO address (address_id, line1, line2, city, state, postal_code, country, created_at) VALUES
  (1, '123 Main St', NULL, 'New York', 'NY', '10001', 'US', '2025-01-01 08:00:00'),
  (2, '456 Oak Ave', 'Suite 200', 'Los Angeles', 'CA', '90001', 'US', '2025-01-02 08:00:00'),
  (3, '789 Pine Rd', NULL, 'Chicago', 'IL', '60601', 'US', '2025-01-03 08:00:00'),
  (4, '321 Elm St', 'Apt 4B', 'Miami', 'FL', '33101', 'US', '2025-01-04 08:00:00') AS new_row
ON DUPLICATE KEY UPDATE line1 = new_row.line1;

-- M:N Suspect <-> Address (shared addresses: Suspect 1 at 1,2; Suspect 2 at 2,3; Suspect 3 at 1,4)
INSERT INTO suspect_address (suspect_id, address_id, address_type, is_current, linked_at) VALUES
  (1, 1, 'HOME', 1, '2025-01-10 09:10:00'),
  (1, 2, 'WORK', 1, '2025-01-10 09:10:00'),
  (2, 2, 'HOME', 1, '2025-01-11 10:40:00'),
  (2, 3, 'MAILING', 0, '2025-01-11 10:40:00'),
  (3, 1, 'HOME', 1, '2025-01-12 14:10:00'),
  (3, 4, 'WORK', 1, '2025-01-12 14:10:00') AS new_row
ON DUPLICATE KEY UPDATE address_type = new_row.address_type;

-- Criminal organizations
INSERT INTO criminal_organization (org_id, org_name, org_type, created_at) VALUES
  (1, 'Cartel Del Norte', 'CARTEL', '2025-01-01 08:00:00'),
  (2, 'Downtown Crew', 'GANG', '2025-01-02 08:00:00'),
  (3, 'Atlantic Laundering Ring', 'MONEY_LAUNDERING', '2025-01-03 08:00:00') AS new_row
ON DUPLICATE KEY UPDATE org_name = new_row.org_name;

-- M:N Suspect <-> Criminal Organization (shared membership: Suspect 1 in 1,2; Suspect 2 in 2; Suspect 3 in 1,3)
INSERT INTO suspect_criminal_organization (suspect_id, org_id, role, linked_at) VALUES
  (1, 1, 'Lieutenant', '2025-01-10 09:15:00'),
  (1, 2, 'Associate', '2025-01-10 09:15:00'),
  (2, 2, 'Member', '2025-01-11 10:45:00'),
  (3, 1, 'Runner', '2025-01-12 14:15:00'),
  (3, 3, 'Accountant', '2025-01-12 14:15:00') AS new_row
ON DUPLICATE KEY UPDATE role = new_row.role;
