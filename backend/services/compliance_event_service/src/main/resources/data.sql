-- =========================================================
-- MOCK DATA
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE compliance_event_link;
TRUNCATE TABLE audit_action;
TRUNCATE TABLE compliance_event_ctr_detail;
TRUNCATE TABLE compliance_event_sar_detail;
TRUNCATE TABLE compliance_event;
TRUNCATE TABLE suspect_snapshot_at_time_of_event;
TRUNCATE TABLE cash_transaction;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------
-- A) Seed transactions that will generate a CTR (SUBJ-001)
-- -------------------------
INSERT INTO cash_transaction
  (source_system, source_txn_id, external_subject_key, source_subject_type, source_subject_id, subject_name,
   txn_time, cash_in, cash_out, currency, channel, location)
VALUES
  ('TXN_SEED', 'T-1001', 'SUBJ-001', 'CUSTOMER', 'CUST-001', 'John Doe',
   '2026-02-01 09:00:00.000', 6000.00, 0.00, 'USD', 'BRANCH', 'Austin'),
  ('TXN_SEED', 'T-1002', 'SUBJ-001', 'CUSTOMER', 'CUST-001', 'John Doe',
   '2026-02-01 11:15:00.000', 5500.00, 0.00, 'USD', 'BRANCH', 'Austin');

-- -------------------------
-- B) Seed "structuring-style" pattern (SUBJ-002) for future SAR rules
-- (each day < 10k, but multiple days in a short window)
-- -------------------------
INSERT INTO cash_transaction
  (source_system, source_txn_id, external_subject_key, source_subject_type, source_subject_id, subject_name,
   txn_time, cash_in, cash_out, currency, channel, location)
VALUES
  ('TXN_SEED', 'T-2001', 'SUBJ-002', 'CUSTOMER', 'CUST-002', 'Jane Smith',
   '2026-01-30 10:00:00.000', 9500.00, 0.00, 'USD', 'BRANCH', 'Killeen'),
  ('TXN_SEED', 'T-2002', 'SUBJ-002', 'CUSTOMER', 'CUST-002', 'Jane Smith',
   '2026-01-31 10:30:00.000', 9200.00, 0.00, 'USD', 'BRANCH', 'Killeen'),
  ('TXN_SEED', 'T-2003', 'SUBJ-002', 'CUSTOMER', 'CUST-002', 'Jane Smith',
   '2026-02-01 14:00:00.000', 9800.00, 0.00, 'USD', 'BRANCH', 'Killeen');

-- -------------------------
-- C) Optional snapshots (only used if you want to attach suspect_snapshot_id)
-- -------------------------
INSERT INTO suspect_snapshot_at_time_of_event
  (suspect_id, last_known_alias, last_known_address, suspect_minimal, captured_at)
VALUES
  (101, 'John Doe',
   JSON_OBJECT('line1','1200 Main St','city','Austin','state','TX','postal_code','78701','country','US'),
   JSON_OBJECT('primary_name','John Doe','risk_level','MEDIUM'),
   CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY
  );

-- -------------------------
-- D) What the DB looks like AFTER your generator runs (example rows)
-- NOTE: Your actual generator will create these.
-- This section is optional: you can insert them manually for UI demos,
-- or leave it empty and let the service populate it.
-- -------------------------

-- Example generated CTR event for SUBJ-001 on 2026-02-01
INSERT INTO compliance_event
  (event_type, source_system, source_entity_id, external_subject_key,
   source_subject_type, source_subject_id, suspect_snapshot_id,
   event_time, total_amount, status, severity_score,
   correlation_id, idempotency_key)
VALUES
  ('CTR', 'AUTO_FROM_TXNS', 'AGGREGATION', 'SUBJ-001',
   'CUSTOMER', 'CUST-001', (SELECT snapshot_id FROM suspect_snapshot_at_time_of_event LIMIT 1),
   '2026-02-01 00:00:00.000', 11500.00, 'CREATED', NULL,
   'corr-ctr-subj-001-2026-02-01', 'CTR:SUBJ-001:2026-02-01'
  );

INSERT INTO compliance_event_ctr_detail
  (event_id, event_type, customer_name, transaction_time, ctr_form_data)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE idempotency_key='CTR:SUBJ-001:2026-02-01'),
    'CTR',
    'John Doe',
    '2026-02-01 09:00:00.000',
    JSON_OBJECT(
      'source','AUTO_FROM_TXNS',
      'subjectKey','SUBJ-001',
      'txnDay','2026-02-01',
      'totalCashIn','11500.00',
      'totalCashOut','0.00',
      'totalCashAmount','11500.00',
      'txnCount', 2,
      'contributingTxnIds', JSON_ARRAY(
        (SELECT txn_id FROM cash_transaction WHERE source_txn_id='T-1001' LIMIT 1),
        (SELECT txn_id FROM cash_transaction WHERE source_txn_id='T-1002' LIMIT 1)
      )
    )
  );

-- Audit example for the generated CTR
INSERT INTO audit_action
  (event_id, actor_user_id, actor_role, action, metadata, correlation_id, idempotency_key)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE idempotency_key='CTR:SUBJ-001:2026-02-01'),
    '11111111-1111-1111-1111-111111111111', 'SYSTEM',
    'CTR_CREATED',
    JSON_OBJECT('notes','CTR created from cash_transaction daily aggregation'),
    'corr-ctr-subj-001-2026-02-01',
    'audit:CTR:SUBJ-001:2026-02-01:created'
  );
