-- =========================================================
-- Mock Data: Documents + Cases Service
-- Tables: case_file, case_note, document, audit_event
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE case_note;
TRUNCATE TABLE document;
TRUNCATE TABLE case_file;
TRUNCATE TABLE audit_event;
SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------
-- CASE FILES
-- sar_id is external (SAR service), must be UNIQUE here
-- -------------------------
INSERT INTO case_file (sar_id, status, created_at, referred_at, closed_at, referred_to_agency)
VALUES
  (10001, 'OPEN',     NOW() - INTERVAL 10 DAY, NULL, NULL, NULL),
  (10002, 'REFERRED', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 7 DAY, NULL, 'FinCEN / Law Enforcement Liaison'),
  (10003, 'CLOSED',   NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 28 DAY, NOW() - INTERVAL 5 DAY, 'IRS-CI');

-- -------------------------
-- CASE NOTES
-- author_user_id is external (Identity service), no FK here
-- -------------------------
INSERT INTO case_note (case_id, author_user_id, note_text, created_at)
VALUES
  (1, '11111111-1111-1111-1111-111111111111', 'Initial review: SAR indicates structured deposits over multiple days.', NOW() - INTERVAL 9 DAY),
  (1, '22222222-2222-2222-2222-222222222222', 'Requested supporting docs from reporting FI.', NOW() - INTERVAL 8 DAY),
  (2, '11111111-1111-1111-1111-111111111111', 'Referred case to liaison team with summary of suspicious pattern.', NOW() - INTERVAL 7 DAY),
  (3, '33333333-3333-3333-3333-333333333333', 'Closed: insufficient corroboration. Documented rationale and retention notes.', NOW() - INTERVAL 5 DAY);

-- -------------------------
-- DOCUMENTS
-- document_type CHECK rules enforced by your schema:
-- CTR: ctr_id only
-- CASE: case_id only
-- SAR: sar_id required; ctr_id must be NULL; case_id optional
-- -------------------------

-- CASE documents (case_id only)
INSERT INTO document (document_type, file_name, storage_path, uploaded_at, ctr_id, sar_id, case_id)
VALUES
  ('CASE', 'case_1_summary.pdf', 's3://fincen-dev/documents/cases/1/case_1_summary.pdf', NOW() - INTERVAL 9 DAY, NULL, NULL, 1),
  ('CASE', 'case_2_referral_packet.pdf', 's3://fincen-dev/documents/cases/2/case_2_referral_packet.pdf', NOW() - INTERVAL 7 DAY, NULL, NULL, 2);

-- SAR documents (sar_id required; case_id optional)
INSERT INTO document (document_type, file_name, storage_path, uploaded_at, ctr_id, sar_id, case_id)
VALUES
  ('SAR', 'sar_10001_original.pdf', 's3://fincen-dev/documents/sars/10001/sar_10001_original.pdf', NOW() - INTERVAL 10 DAY, NULL, 10001, 1),
  ('SAR', 'sar_10002_original.pdf', 's3://fincen-dev/documents/sars/10002/sar_10002_original.pdf', NOW() - INTERVAL 8 DAY,  NULL, 10002, 2),
  ('SAR', 'sar_10003_original.pdf', 's3://fincen-dev/documents/sars/10003/sar_10003_original.pdf', NOW() - INTERVAL 30 DAY, NULL, 10003, 3);

-- CTR documents (ctr_id only)
INSERT INTO document (document_type, file_name, storage_path, uploaded_at, ctr_id, sar_id, case_id)
VALUES
  ('CTR', 'ctr_90001_export.csv', 's3://fincen-dev/documents/ctrs/90001/ctr_90001_export.csv', NOW() - INTERVAL 12 DAY, 90001, NULL, NULL),
  ('CTR', 'ctr_90002_export.csv', 's3://fincen-dev/documents/ctrs/90002/ctr_90002_export.csv', NOW() - INTERVAL 3 DAY,  90002, NULL, NULL);

-- -------------------------
-- AUDIT EVENTS
-- metadata is JSON
-- -------------------------
INSERT INTO audit_event (actor_user_id, action, entity_type, entity_id, metadata, created_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'CASE_CREATED',   'case_file',  '1', JSON_OBJECT('sar_id', 10001, 'status', 'OPEN'), NOW() - INTERVAL 10 DAY),
  ('22222222-2222-2222-2222-222222222222', 'NOTE_ADDED',     'case_note',  '1', JSON_OBJECT('case_id', 1), NOW() - INTERVAL 9 DAY),
  ('11111111-1111-1111-1111-111111111111', 'CASE_REFERRED',  'case_file',  '2', JSON_OBJECT('referred_to', 'FinCEN / Law Enforcement Liaison'), NOW() - INTERVAL 7 DAY),
  ('33333333-3333-3333-3333-333333333333', 'CASE_CLOSED',    'case_file',  '3', JSON_OBJECT('reason', 'insufficient corroboration'), NOW() - INTERVAL 5 DAY),
  (NULL,                                     'DOC_UPLOADED',  'document',   '1', JSON_OBJECT('type', 'SAR', 'sar_id', 10001), NOW() - INTERVAL 10 DAY);
