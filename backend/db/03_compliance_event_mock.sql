-- =========================================================
-- Mock Data: compliance_event schema
-- Includes:
--   Suspect Registry tables
--   suspect_snapshot_at_time_of_event
--   compliance_event + CTR/SAR details
--   audit_action
--   compliance_event_link
-- =========================================================

USE compliance_event;

-- -------------------------
-- 1) TRUNCATE IN FK-SAFE ORDER
-- -------------------------
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE compliance_event_link;
TRUNCATE TABLE audit_action;

TRUNCATE TABLE compliance_event_ctr_detail;
TRUNCATE TABLE compliance_event_sar_detail;

TRUNCATE TABLE compliance_event;
TRUNCATE TABLE suspect_snapshot_at_time_of_event;

TRUNCATE TABLE suspect_criminal_organization;
TRUNCATE TABLE suspect_address;
TRUNCATE TABLE suspect_alias;
TRUNCATE TABLE criminal_organization;
TRUNCATE TABLE address;
TRUNCATE TABLE suspect;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------
-- 2) SUSPECT REGISTRY MOCK DATA
-- -------------------------

-- SUSPECTS
-- ssn is "encrypted base64 string" placeholder (fake), ssn_hash is 64-hex placeholder
INSERT INTO suspect (primary_name, dob, ssn, ssn_hash, risk_level)
VALUES
  ('John A. Doe', '1984-06-12', 'ZW5jOkZBS0VfU1NOX0VOQ19KRE9F', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'MEDIUM'),
  ('Acme Imports LLC', NULL, NULL, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'HIGH'),
  ('Jane Roe', '1991-11-03', 'ZW5jOkZBS0VfU1NOX0VOQ19KUk9F', 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'LOW'),
  ('Unknown Subject', NULL, NULL, 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'UNKNOWN');

-- ALIASES
INSERT INTO suspect_alias (suspect_id, alias_name, alias_type, is_primary)
VALUES
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'), 'John Andrew Doe', 'LEGAL', TRUE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'), 'Jonathan Doe', 'AKA', FALSE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'), 'Acme Imports LLC', 'LEGAL', TRUE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'), 'Acme Importers', 'BUSINESS', FALSE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'), 'Jane Roe', 'LEGAL', TRUE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'), 'J. Roe', 'AKA', FALSE);

-- ADDRESSES (deduped via generated key unique index)
INSERT INTO address (line1, line2, city, state, postal_code, country)
VALUES
  ('1200 Main St', NULL, 'Austin', 'TX', '78701', 'US'),
  ('55 Commerce Blvd', 'Suite 200', 'Houston', 'TX', '77002', 'US'),
  ('901 Market St', NULL, 'San Antonio', 'TX', '78205', 'US'),
  ('400 Compliance Way', NULL, 'Washington', 'DC', '20001', 'US');

-- SUSPECT <-> ADDRESS
INSERT INTO suspect_address (suspect_id, address_id, address_type, is_current)
VALUES
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'),
   (SELECT address_id FROM address WHERE line1='1200 Main St' AND city='Austin'),
   'HOME', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'),
   (SELECT address_id FROM address WHERE line1='55 Commerce Blvd' AND city='Houston'),
   'WORK', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'),
   (SELECT address_id FROM address WHERE line1='901 Market St' AND city='San Antonio'),
   'HOME', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Unknown Subject'),
   (SELECT address_id FROM address WHERE line1='400 Compliance Way' AND city='Washington'),
   'UNKNOWN', TRUE);

-- ORGS
INSERT INTO criminal_organization (org_name, org_type)
VALUES
  ('Gulf Coast Fraud Ring', 'FRAUD_RING'),
  ('Southwest Laundering Network', 'MONEY_LAUNDERING'),
  ('Street Group 12', 'GANG');

-- SUSPECT <-> ORG
INSERT INTO suspect_criminal_organization (suspect_id, org_id, role)
VALUES
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'),
   (SELECT org_id FROM criminal_organization WHERE org_name='Gulf Coast Fraud Ring'),
   'Associate'),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'),
   (SELECT org_id FROM criminal_organization WHERE org_name='Southwest Laundering Network'),
   'Front Company'),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Unknown Subject'),
   (SELECT org_id FROM criminal_organization WHERE org_name='Street Group 12'),
   'Unknown');

-- -------------------------
-- 3) SUSPECT SNAPSHOTS (point-in-time)
-- NOTE: No FK to suspect table by design (guardrails are app-level)
-- -------------------------
INSERT INTO suspect_snapshot_at_time_of_event
  (suspect_id, last_known_alias, last_known_address, suspect_minimal, captured_at)
VALUES
  (
    (SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'),
    'John Andrew Doe',
    JSON_OBJECT('line1','1200 Main St','city','Austin','state','TX','postal_code','78701','country','US'),
    JSON_OBJECT('primary_name','John A. Doe','risk_level','MEDIUM','dob','1984-06-12'),
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY
  ),
  (
    (SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'),
    'Acme Imports LLC',
    JSON_OBJECT('line1','55 Commerce Blvd','line2','Suite 200','city','Houston','state','TX','postal_code','77002','country','US'),
    JSON_OBJECT('primary_name','Acme Imports LLC','risk_level','HIGH'),
    CURRENT_TIMESTAMP(3) - INTERVAL 6 DAY
  ),
  (
    (SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'),
    'Jane Roe',
    JSON_OBJECT('line1','901 Market St','city','San Antonio','state','TX','postal_code','78205','country','US'),
    JSON_OBJECT('primary_name','Jane Roe','risk_level','LOW','dob','1991-11-03'),
    CURRENT_TIMESTAMP(3) - INTERVAL 2 DAY
  );

-- -------------------------
-- 4) COMPLIANCE EVENTS (CTR + SAR)
-- Constraints enforced:
-- event_type in ('CTR','SAR')
-- status: CTR -> CREATED|FILED, SAR -> DRAFT|SUBMITTED
-- severity_score only allowed for SAR (0..100)
-- total_amount null or >= 0
-- uniqueness:
--   (source_system, source_entity_id)
--   (source_system, idempotency_key)
-- -------------------------

-- CTR EVENTS
INSERT INTO compliance_event
  (event_type, source_system, source_entity_id, external_subject_key,
   source_subject_type, source_subject_id, suspect_snapshot_id,
   event_time, total_amount, status, severity_score,
   correlation_id, idempotency_key)
VALUES
  (
    'CTR', 'bank-core', 'CTR-90001', 'CUST-001',
    'CUSTOMER', 'CUST-001',
    (SELECT snapshot_id FROM suspect_snapshot_at_time_of_event ORDER BY snapshot_id ASC LIMIT 1),
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY,
    15000.00, 'FILED', NULL,
    'corr-ctr-90001', 'idem-bank-core-ctr-90001'
  ),
  (
    'CTR', 'bank-core', 'CTR-90002', 'CUST-002',
    'CUSTOMER', 'CUST-002',
    NULL,
    CURRENT_TIMESTAMP(3) - INTERVAL 3 DAY,
    22000.00, 'CREATED', NULL,
    'corr-ctr-90002', 'idem-bank-core-ctr-90002'
  );

-- SAR EVENTS
INSERT INTO compliance_event
  (event_type, source_system, source_entity_id, external_subject_key,
   source_subject_type, source_subject_id, suspect_snapshot_id,
   event_time, total_amount, status, severity_score,
   correlation_id, idempotency_key)
VALUES
  (
    'SAR', 'case-mgmt', 'SAR-10001', 'SUBJ-DOE-1984',
    'SUSPECT', 'SUS-DOE-1',
    (SELECT snapshot_id FROM suspect_snapshot_at_time_of_event ORDER BY snapshot_id ASC LIMIT 1),
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY,
    48000.00, 'SUBMITTED', 72,
    'corr-sar-10001', 'idem-case-mgmt-sar-10001'
  ),
  (
    'SAR', 'case-mgmt', 'SAR-10002', 'SUBJ-ACME',
    'SUSPECT', 'SUS-ACME-1',
    (SELECT snapshot_id FROM suspect_snapshot_at_time_of_event ORDER BY snapshot_id ASC LIMIT 1 OFFSET 1),
    CURRENT_TIMESTAMP(3) - INTERVAL 6 DAY,
    125000.00, 'DRAFT', 88,
    'corr-sar-10002', 'idem-case-mgmt-sar-10002'
  );

-- -------------------------
-- 5) DETAILS TABLES (COMPOSITE FK: (event_id, event_type))
-- -------------------------

-- CTR DETAILS
INSERT INTO compliance_event_ctr_detail
  (event_id, event_type, customer_name, transaction_time, ctr_form_data, created_at)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE source_system='bank-core' AND source_entity_id='CTR-90001'),
    'CTR',
    'Customer One',
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY,
    JSON_OBJECT(
      'ctrNumber','90001',
      'currency','USD',
      'amount',15000.00,
      'financialInstitution', JSON_OBJECT('name','Mock National Bank','routing','000111222'),
      'transaction', JSON_OBJECT('type','cash_deposit','method','in_branch')
    ),
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='bank-core' AND source_entity_id='CTR-90002'),
    'CTR',
    'Customer Two',
    CURRENT_TIMESTAMP(3) - INTERVAL 3 DAY,
    JSON_OBJECT(
      'ctrNumber','90002',
      'currency','USD',
      'amount',22000.00,
      'financialInstitution', JSON_OBJECT('name','Mock National Bank','routing','000111222'),
      'transaction', JSON_OBJECT('type','cash_withdrawal','method','atm')
    ),
    CURRENT_TIMESTAMP(3) - INTERVAL 3 DAY
  );

-- SAR DETAILS
INSERT INTO compliance_event_sar_detail
  (event_id, event_type, narrative, activity_start, activity_end, form_data, submitted_at, created_at)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10001'),
    'SAR',
    'Narrative: Activity consistent with structuring and rapid movement of funds across accounts. Pattern observed over multiple days with deposits just below reporting thresholds.',
    CURRENT_TIMESTAMP(3) - INTERVAL 12 DAY,
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY,
    JSON_OBJECT(
      'sarNumber','10001',
      'activityType','structuring',
      'jurisdiction','US',
      'summary','Multiple deposits under threshold followed by consolidation transfers.',
      'subjects', JSON_ARRAY(JSON_OBJECT('type','individual','name','John A. Doe'))
    ),
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY,
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10002'),
    'SAR',
    'Narrative: Repeated wires with inconsistent invoice data and unusual routing through intermediary accounts. Potential trade-based laundering indicators.',
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY,
    CURRENT_TIMESTAMP(3) - INTERVAL 6 DAY,
    JSON_OBJECT(
      'sarNumber','10002',
      'activityType','trade_based_money_laundering',
      'jurisdiction','US',
      'summary','Invoice mismatch and round-dollar transfers via intermediaries.',
      'subjects', JSON_ARRAY(JSON_OBJECT('type','business','name','Acme Imports LLC'))
    ),
    NULL,
    CURRENT_TIMESTAMP(3) - INTERVAL 6 DAY
  );

-- -------------------------
-- 6) AUDIT ACTIONS
-- actor_user_id aligns with your Identity service UUIDs (mock)
-- -------------------------
INSERT INTO audit_action
  (event_id, actor_user_id, actor_role, action, metadata, correlation_id, idempotency_key, created_at)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE source_system='bank-core' AND source_entity_id='CTR-90001'),
    '22222222-2222-2222-2222-222222222222', 'COMPLIANCE_USER',
    'CTR_FILED',
    JSON_OBJECT('notes','Filed automatically via bank-core feed'),
    'corr-ctr-90001',
    'audit-idem-ctr-90001-filed',
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10001'),
    '11111111-1111-1111-1111-111111111111', 'ANALYST',
    'SAR_SUBMITTED',
    JSON_OBJECT('review','completed','risk_score',72),
    'corr-sar-10001',
    'audit-idem-sar-10001-submitted',
    CURRENT_TIMESTAMP(3) - INTERVAL 9 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10002'),
    '11111111-1111-1111-1111-111111111111', 'ANALYST',
    'SAR_DRAFT_SAVED',
    JSON_OBJECT('autosave',true),
    'corr-sar-10002',
    'audit-idem-sar-10002-draft',
    CURRENT_TIMESTAMP(3) - INTERVAL 6 DAY
  );

-- -------------------------
-- 7) EVENT LINKS (no self-links)
-- link_type must be: SAR_SUPPORTS_CTR | CTR_SUPPORTS_SAR | RELATED
-- -------------------------
INSERT INTO compliance_event_link
  (from_event_id, to_event_id, link_type, evidence_snapshot, linked_at)
VALUES
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10001'),
    (SELECT event_id FROM compliance_event WHERE source_system='bank-core' AND source_entity_id='CTR-90001'),
    'SAR_SUPPORTS_CTR',
    JSON_OBJECT('evidence','SAR narrative references CTR-90001 cash deposit pattern','confidence',0.74),
    CURRENT_TIMESTAMP(3) - INTERVAL 8 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='bank-core' AND source_entity_id='CTR-90002'),
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10002'),
    'CTR_SUPPORTS_SAR',
    JSON_OBJECT('evidence','CTR-90002 aligns with SAR-10002 activity window','confidence',0.63),
    CURRENT_TIMESTAMP(3) - INTERVAL 3 DAY
  ),
  (
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10001'),
    (SELECT event_id FROM compliance_event WHERE source_system='case-mgmt' AND source_entity_id='SAR-10002'),
    'RELATED',
    JSON_OBJECT('evidence','Shared FI routing and similar behavior indicators','confidence',0.51),
    CURRENT_TIMESTAMP(3) - INTERVAL 5 DAY
  );
