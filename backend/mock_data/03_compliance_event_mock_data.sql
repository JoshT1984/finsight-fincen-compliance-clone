-- ============================================
-- Mock Data: Compliance Event Service
-- Database: compliance_event
-- Run against compliance_event DB. Prerequisite: schema.sql
-- suspect_id references suspect.suspect_id in finsight_suspects (logical, no FK)
-- Event IDs 1,3 = CTR; 2,4 = SAR (referenced by case_file.sar_id and document)
-- ============================================

USE compliance_event;

-- Suspect snapshots (suspect_id 1,2,3 from suspect_registry)
INSERT INTO suspect_snapshot_at_time_of_event (snapshot_id, suspect_id, last_known_alias, last_known_address, suspect_minimal, captured_at) VALUES
  (1, 1, 'John Smith', '{"line1":"123 Main St","city":"New York"}', '{"name":"John Smith","risk":"HIGH"}', '2025-01-15 10:00:00.000'),
  (2, 2, 'Maria Garcia', '{"line1":"456 Oak Ave","city":"Los Angeles"}', '{"name":"Maria Garcia","risk":"MEDIUM"}', '2025-01-15 11:00:00.000'),
  (3, 3, 'Robert Chen', '{"line1":"789 Pine Rd","city":"Chicago"}', '{"name":"Robert Chen","risk":"LOW"}', '2025-01-15 12:00:00.000') AS new_row
ON DUPLICATE KEY UPDATE last_known_alias = new_row.last_known_alias;

-- Compliance events: 1=CTR, 2=SAR, 3=CTR, 4=SAR (source_entity_id must be unique per source_system)
INSERT INTO compliance_event (event_id, event_type, source_system, source_entity_id, source_subject_type, source_subject_id, suspect_snapshot_id, event_time, total_amount, status, severity_score, created_at) VALUES
  (1, 'CTR', 'CTR_SERVICE', '1', 'SUSPECT', '1', 1, '2025-01-15 14:30:00.000', 15000.00, 'FILED', NULL, '2025-01-15 14:30:00.000'),
  (2, 'SAR', 'SAR_SERVICE', '1', 'SUSPECT', '1', 1, '2025-01-16 09:00:00.000', NULL, 'SUBMITTED', 75, '2025-01-16 09:00:00.000'),
  (3, 'CTR', 'CTR_SERVICE', '2', 'SUSPECT', '2', 2, '2025-01-17 11:00:00.000', 25000.00, 'FILED', NULL, '2025-01-17 11:00:00.000'),
  (4, 'SAR', 'SAR_SERVICE', '2', 'SUSPECT', '2', 2, '2025-01-18 10:00:00.000', NULL, 'SUBMITTED', 85, '2025-01-18 10:00:00.000') AS new_row
ON DUPLICATE KEY UPDATE event_type = new_row.event_type;

-- CTR Detail (1:1 with CTR events 1 and 3)
INSERT INTO compliance_event_ctr_detail (event_id, customer_name, transaction_time, ctr_form_data, created_at) VALUES
  (1, 'John Smith', '2025-01-15 14:30:00.000', '{"amount":15000,"currency":"USD","transactionType":"CASH"}', '2025-01-15 14:30:00.000'),
  (3, 'Maria Garcia', '2025-01-17 11:00:00.000', '{"amount":25000,"currency":"USD","transactionType":"WIRE"}', '2025-01-17 11:00:00.000') AS new_row
ON DUPLICATE KEY UPDATE customer_name = new_row.customer_name;

-- SAR Detail (1:1 with SAR events 2 and 4)
INSERT INTO compliance_event_sar_detail (event_id, narrative, activity_start, activity_end, form_data, submitted_at, created_at) VALUES
  (2, 'Suspicious structuring activity involving multiple cash deposits under reporting threshold.', '2025-01-01 00:00:00.000', '2025-01-15 23:59:59.000', '{"suspiciousActivity":"STRUCTURING","amountInvolved":45000}', '2025-01-16 09:00:00.000', '2025-01-16 09:00:00.000'),
  (4, 'Wire transfers to high-risk jurisdiction with no apparent business purpose.', '2025-01-10 00:00:00.000', '2025-01-18 23:59:59.000', '{"suspiciousActivity":"WIRE_FRAUD","amountInvolved":120000}', '2025-01-18 10:00:00.000', '2025-01-18 10:00:00.000') AS new_row
ON DUPLICATE KEY UPDATE narrative = new_row.narrative;

-- M:N Event links (CTR <-> SAR): SAR 2 supports CTR 1; CTR 3 supports SAR 4
INSERT INTO compliance_event_link (from_event_id, to_event_id, link_type, evidence_snapshot, linked_at) VALUES
  (2, 1, 'SAR_SUPPORTS_CTR', '{"linkedBy":"investigator","reason":"SAR filed based on CTR pattern"}', '2025-01-16 10:00:00.000'),
  (3, 4, 'CTR_SUPPORTS_SAR', '{"linkedBy":"analyst","reason":"CTR provides supporting transaction data"}', '2025-01-18 11:00:00.000') AS new_row
ON DUPLICATE KEY UPDATE link_type = new_row.link_type;

-- Audit actions (1:M with compliance_event) - MVP roles: Analyst, Compliance User
INSERT INTO audit_action (audit_id, event_id, actor_user_id, actor_role, action, metadata, created_at) VALUES
  (1, 1, '11111111-1111-1111-1111-111111111102', 'COMPLIANCE_USER', 'CTR_FILED', '{"amount":15000}', '2025-01-15 14:35:00.000'),
  (2, 2, '11111111-1111-1111-1111-111111111102', 'COMPLIANCE_USER', 'SAR_SUBMITTED', '{"severity":75}', '2025-01-16 09:05:00.000'),
  (3, 4, '11111111-1111-1111-1111-111111111101', 'ANALYST', 'SAR_REVIEWED', '{"approved":true}', '2025-01-18 10:30:00.000') AS new_row
ON DUPLICATE KEY UPDATE action = new_row.action;
