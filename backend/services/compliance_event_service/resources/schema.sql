-- ======================================
-- COMPLIANCE EVENT SERVICE SCHEMA 
-- Unified CTR + SAR fact table + separate detail tables
-- Includes point-in-time suspect snapshot + optional CTR<->SAR links
CREATE SCHEMA IF NOT EXISTS compliance_event;
SET search_path TO compliance_event;

CREATE TABLE IF NOT EXISTS suspect_snapshot_at_time_of_event (
  snapshot_id BIGSERIAL PRIMARY KEY,

  -- External reference to suspect_registry_service (no FK)
  suspect_id BIGINT NOT NULL,

  last_known_alias VARCHAR(256),
  last_known_address JSONB,   -- {line1,line2,city,state,postal_code,country}

  -- optional minimal rendering payload
  suspect_minimal JSONB,      -- {"primary_name":"...", "risk_level":"HIGH"}

  captured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_suspect_snapshot_suspect
  ON suspect_snapshot_at_time_of_event(suspect_id, captured_at DESC);


-- ----------------------
-- 1) Unified compliance event (CTR + SAR combined)
-- This is the primary "fact" record for timelines/reporting.
-- ----------------------
CREATE TABLE IF NOT EXISTS compliance_event (
  event_id BIGSERIAL PRIMARY KEY,

  event_type VARCHAR(16) NOT NULL
    CHECK (event_type IN ('CTR','SAR')),

  -- upstream identifiers (no FK, avoids coupling)
  source_system VARCHAR(64) NOT NULL,      -- e.g., 'ctr_sar_service'
  source_entity_id VARCHAR(64) NOT NULL,   -- ctr_id or sar_id as string

  -- Optional stable subject key (customer/account/etc.)
  external_subject_key VARCHAR(128),

  -- Optional subject reference (most often SUSPECT)
  -- This is useful even when you do not have a suspect_snapshot_id.
  source_subject_type VARCHAR(32)
    CHECK (source_subject_type IN ('SUSPECT','CUSTOMER','ACCOUNT')),
  source_subject_id VARCHAR(128),

  -- point-in-time suspect snapshot (preferred when event involves a suspect)
  suspect_snapshot_id BIGINT REFERENCES suspect_snapshot_at_time_of_event(snapshot_id)
    ON DELETE SET NULL,

  -- normalized fields common to CTR and SAR
  event_time TIMESTAMPTZ NOT NULL,
  total_amount NUMERIC(14,2) CHECK (total_amount >= 0),

  -- SAR status could be DRAFT/SUBMITTED; CTR status could be FILED/CREATED, etc.
  status VARCHAR(32),

  -- SAR suspicion score (0..100); can be NULL for CTRs
  severity_score INT CHECK (severity_score BETWEEN 0 AND 100),

  -- tracing / ops
  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE (source_system, source_entity_id)
);

CREATE INDEX IF NOT EXISTS idx_event_type_time
  ON compliance_event(event_type, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_event_source
  ON compliance_event(source_system, source_entity_id);

CREATE INDEX IF NOT EXISTS idx_event_suspect
  ON compliance_event(suspect_snapshot_id);

CREATE INDEX IF NOT EXISTS idx_event_subject
  ON compliance_event(source_subject_type, source_subject_id, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_event_created
  ON compliance_event(created_at DESC);


-- ----------------------
-- 2) CTR detail (unique CTR fields)
-- One row per CTR event (1:1 with compliance_event)
-- ----------------------
CREATE TABLE IF NOT EXISTS compliance_event_ctr_detail (
  event_id BIGINT PRIMARY KEY REFERENCES compliance_event(event_id) ON DELETE CASCADE,

  customer_name VARCHAR(128) NOT NULL,
  transaction_date DATE NOT NULL,

  -- optional CTR-specific payload (form fields, extra metadata)
  ctr_form_data JSONB,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ctr_detail_date
  ON compliance_event_ctr_detail(transaction_date);


-- ----------------------
-- 3) SAR detail (unique SAR fields)
-- One row per SAR event (1:1 with compliance_event)
-- ----------------------
CREATE TABLE IF NOT EXISTS compliance_event_sar_detail (
  event_id BIGINT PRIMARY KEY REFERENCES compliance_event(event_id) ON DELETE CASCADE,

  narrative TEXT,

  activity_start TIMESTAMPTZ,
  activity_end TIMESTAMPTZ,

  -- store SAR form fields as JSONB
  form_data JSONB NOT NULL DEFAULT '{}'::jsonb,

  submitted_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sar_detail_submitted
  ON compliance_event_sar_detail(submitted_at DESC);


-- ----------------------
-- 4) Optional: CTR <-> SAR relationships captured in this service
-- Useful for "Which CTRs support this SAR?" without calling upstream services.
-- ----------------------
CREATE TABLE IF NOT EXISTS compliance_event_link (
  from_event_id BIGINT NOT NULL REFERENCES compliance_event(event_id) ON DELETE CASCADE,
  to_event_id BIGINT NOT NULL REFERENCES compliance_event(event_id) ON DELETE CASCADE,

  link_type VARCHAR(32) NOT NULL
    CHECK (link_type IN ('SAR_SUPPORTS_CTR','CTR_SUPPORTS_SAR','RELATED')),

  -- optional stable snapshot for UI/reporting
  evidence_snapshot JSONB,

  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (from_event_id, to_event_id, link_type)
);

CREATE INDEX IF NOT EXISTS idx_event_link_from
  ON compliance_event_link(from_event_id, linked_at DESC);

CREATE INDEX IF NOT EXISTS idx_event_link_to
  ON compliance_event_link(to_event_id, linked_at DESC);


-- ----------------------
-- 5) Append-only audit actions (who did what)
-- Separate from compliance_event (fact record).
-- ----------------------
CREATE TABLE IF NOT EXISTS audit_action (
  audit_id BIGSERIAL PRIMARY KEY,

  -- link action to a CTR/SAR compliance event when applicable
  event_id BIGINT REFERENCES compliance_event(event_id) ON DELETE SET NULL,

  actor_user_id UUID,
  actor_role VARCHAR(64),

  action VARCHAR(64) NOT NULL, -- CREATE, UPDATE, SUBMIT, LINK, CLOSE, etc.

  metadata JSONB,

  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_action_event
  ON audit_action(event_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_action_created
  ON audit_action(created_at DESC);


-- ----------------------
-- 6) Guardrail: ensure CTR details only for CTR events, SAR details only for SAR events
-- Implemented as triggers (Postgres CHECK cannot look up other table values)
-- ----------------------
CREATE OR REPLACE FUNCTION enforce_event_type_for_details()
RETURNS TRIGGER AS $$
DECLARE
  v_type VARCHAR(16);
BEGIN
  SELECT event_type INTO v_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF TG_TABLE_NAME = 'compliance_event_ctr_detail' AND v_type <> 'CTR' THEN
    RAISE EXCEPTION 'CTR detail requires compliance_event.event_type=CTR (event_id=%)', NEW.event_id;
  END IF;

  IF TG_TABLE_NAME = 'compliance_event_sar_detail' AND v_type <> 'SAR' THEN
    RAISE EXCEPTION 'SAR detail requires compliance_event.event_type=SAR (event_id=%)', NEW.event_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_enforce_ctr_detail_type ON compliance_event_ctr_detail;
CREATE TRIGGER trg_enforce_ctr_detail_type
BEFORE INSERT OR UPDATE ON compliance_event_ctr_detail
FOR EACH ROW EXECUTE FUNCTION enforce_event_type_for_details();

DROP TRIGGER IF EXISTS trg_enforce_sar_detail_type ON compliance_event_sar_detail;
CREATE TRIGGER trg_enforce_sar_detail_type
BEFORE INSERT OR UPDATE ON compliance_event_sar_detail
FOR EACH ROW EXECUTE FUNCTION enforce_event_type_for_details();
