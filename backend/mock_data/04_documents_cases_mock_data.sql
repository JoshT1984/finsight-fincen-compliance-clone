-- ============================================
-- Mock Data: Documents & Cases Service
-- Database: finsight_documents
-- Run against finsight_documents DB. Prerequisite: schema.sql
-- sar_id references compliance_event.event_id (SAR type) - use 2, 4
-- author_user_id references app_user.user_id in finsight DB
-- storage_path: S3 keys - see MOCK_DATA_PLANNING.md for S3 upload note
-- ============================================

USE finsight_documents;

-- Case files (sar_id 2 and 4 = SAR events from compliance_event)
INSERT INTO case_file (case_id, sar_id, status, created_at, referred_at, closed_at, referred_to_agency) VALUES
  (1, 2, 'OPEN', '2025-01-16 09:30:00', NULL, NULL, NULL),
  (2, 4, 'REFERRED', '2025-01-18 10:30:00', '2025-01-20 14:00:00', NULL, 'FBI') AS new_row
ON DUPLICATE KEY UPDATE status = new_row.status;

-- Case notes (1:M with case_file; author_user_id from identity mock)
-- Jane (Analyst) adds investigative notes; Bob (Compliance) adds notes; Jane refers case per MVP 4.2
INSERT INTO case_note (note_id, case_id, author_user_id, note_text, created_at) VALUES
  (1, 1, '11111111-1111-1111-1111-111111111101', 'Initial review - structuring pattern identified.', '2025-01-16 10:00:00'),
  (2, 1, '11111111-1111-1111-1111-111111111101', 'Analyst approval for escalation.', '2025-01-17 11:00:00'),
  (3, 2, '11111111-1111-1111-1111-111111111101', 'Case referred to FBI for wire fraud investigation.', '2025-01-20 14:30:00') AS new_row
ON DUPLICATE KEY UPDATE note_text = new_row.note_text;

-- Documents (storage_path = S3 key format; no actual files - see MOCK_DATA_PLANNING.md)
-- CTR docs: ctr_id only | SAR docs: sar_id required | CASE docs: case_id only
INSERT INTO document (document_id, document_type, file_name, storage_path, uploaded_at, ctr_id, sar_id, case_id) VALUES
  (1, 'CTR', 'ctr-form-1.pdf', 'ctr/1/mock-ctr-form-1.pdf', '2025-01-15 14:35:00', 1, NULL, NULL),
  (2, 'CTR', 'ctr-form-2.pdf', 'ctr/3/mock-ctr-form-2.pdf', '2025-01-17 11:05:00', 3, NULL, NULL),
  (3, 'SAR', 'sar-narrative-1.pdf', 'sar/2/mock-sar-narrative-1.pdf', '2025-01-16 09:15:00', NULL, 2, NULL),
  (4, 'SAR', 'sar-narrative-2.pdf', 'sar/4/mock-sar-narrative-2.pdf', '2025-01-18 10:15:00', NULL, 4, NULL),
  (5, 'CASE', 'case-investigation-notes.pdf', 'case/1/mock-case-notes.pdf', '2025-01-17 12:00:00', NULL, NULL, 1) AS new_row
ON DUPLICATE KEY UPDATE file_name = new_row.file_name;

-- Audit events (documents/cases service audit trail)
-- Bob (Compliance) uploads CTR/SAR PDFs; Jane (Analyst) adds notes and refers case per MVP 4.1, 4.2
INSERT INTO audit_event (audit_id, actor_user_id, action, entity_type, entity_id, metadata, created_at) VALUES
  (1, '11111111-1111-1111-1111-111111111102', 'CREATE', 'DOCUMENT', '1', '{"fileName":"ctr-form-1.pdf"}', '2025-01-15 14:35:00'),
  (2, '11111111-1111-1111-1111-111111111101', 'CREATE', 'CASE_NOTE', '1', '{"caseId":1}', '2025-01-16 10:00:00'),
  (3, '11111111-1111-1111-1111-111111111101', 'REFER', 'CASE_FILE', '2', '{"agency":"FBI"}', '2025-01-20 14:00:00') AS new_row
ON DUPLICATE KEY UPDATE action = new_row.action;
