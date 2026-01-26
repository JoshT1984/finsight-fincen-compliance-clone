CREATE SCHEMA IF NOT EXISTS compliance_event;
SET search_path TO compliance_event;

-- ============================================================
-- Suspect snapshot (point-in-time)
-- ============================================================
CREATE TABLE IF NOT EXISTS suspect_snapshot_at_time_of_event (
  snapshot_id BIGSERIAL PRIMARY KEY,
  suspect_id BIGINT NOT NULL,
  last_known_alias VARCHAR(256),
  last_known_address JSONB,
  suspect_minimal JSONB,
  captured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_suspect_snapshot_suspect
  ON suspect_snapshot_at_time_of_event (suspect_id, captured_at DESC);


-- ============================================================
-- Unified compliance event (fact table)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event (
  event_id BIGSERIAL PRIMARY KEY,

  event_type VARCHAR(16) NOT NULL
    CHECK (event_type IN ('CTR','SAR')),

  source_system VARCHAR(64) NOT NULL,
  source_entity_id VARCHAR(64) NOT NULL,

  external_subject_key VARCHAR(128),

  source_subject_type VARCHAR(32)
    CHECK (source_subject_type IN ('SUSPECT','CUSTOMER','ACCOUNT')),
  source_subject_id VARCHAR(128),

  suspect_snapshot_id BIGINT
    REFERENCES suspect_snapshot_at_time_of_event(snapshot_id)
    ON DELETE SET NULL,

  event_time TIMESTAMPTZ NOT NULL,
  total_amount NUMERIC(14,2) CHECK (total_amount >= 0),

  status VARCHAR(32),

  severity_score INT,

  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE (source_system, source_entity_id)
);

-- Status guardrail: status allowed by event_type (allows NULL, app can default)
ALTER TABLE compliance_event
DROP CONSTRAINT IF EXISTS chk_status_by_event_type;

ALTER TABLE compliance_event
ADD CONSTRAINT chk_status_by_event_type
CHECK (
  status IS NULL
  OR (event_type = 'CTR' AND status IN ('CREATED','FILED'))
  OR (event_type = 'SAR' AND status IN ('DRAFT','SUBMITTED'))
);

-- Severity SAR-only (prevents CTR from using severity_score)
ALTER TABLE compliance_event
DROP CONSTRAINT IF EXISTS chk_severity_score_sar_only;

ALTER TABLE compliance_event
ADD CONSTRAINT chk_severity_score_sar_only
CHECK (
  severity_score IS NULL
  OR (event_type = 'SAR' AND severity_score BETWEEN 0 AND 100)
);

-- Idempotency enforcement (concurrent-safe)
CREATE UNIQUE INDEX IF NOT EXISTS uk_event_idempotency
ON compliance_event (source_system, idempotency_key)
WHERE idempotency_key IS NOT NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_type_time
  ON compliance_event (event_type, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_event_source
  ON compliance_event (source_system, source_entity_id);

CREATE INDEX IF NOT EXISTS idx_event_suspect
  ON compliance_event (suspect_snapshot_id);

CREATE INDEX IF NOT EXISTS idx_event_subject
  ON compliance_event (source_subject_type, source_subject_id, event_time DESC);

CREATE INDEX IF NOT EXISTS idx_event_created
  ON compliance_event (created_at DESC);


-- ============================================================
-- CTR Detail (1:1 with compliance_event)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_ctr_detail (
  event_id BIGINT PRIMARY KEY
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  customer_name VARCHAR(128) NOT NULL,

  -- Real timestamp now
  transaction_time TIMESTAMPTZ NOT NULL,

  ctr_form_data JSONB NOT NULL DEFAULT '{}'::jsonb,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ctr_detail_created
  ON compliance_event_ctr_detail (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ctr_detail_tx_time
  ON compliance_event_ctr_detail (transaction_time DESC);


-- ============================================================
-- SAR Detail (1:1 with compliance_event)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_sar_detail (
  event_id BIGINT PRIMARY KEY
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  narrative TEXT,

  activity_start TIMESTAMPTZ,
  activity_end TIMESTAMPTZ,

  form_data JSONB NOT NULL DEFAULT '{}'::jsonb,

  submitted_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sar_detail_created
  ON compliance_event_sar_detail (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sar_detail_submitted
  ON compliance_event_sar_detail (submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_sar_detail_activity
  ON compliance_event_sar_detail (activity_start DESC);


-- ============================================================
-- Audit trail (append-only)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_action (
  audit_id BIGSERIAL PRIMARY KEY,

  event_id BIGINT
    REFERENCES compliance_event(event_id)
    ON DELETE SET NULL,

  actor_user_id UUID,
  actor_role VARCHAR(64),

  action VARCHAR(64) NOT NULL,

  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_created
  ON audit_action (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_event
  ON audit_action (event_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_idempotency
  ON audit_action (idempotency_key)
  WHERE idempotency_key IS NOT NULL;


-- ============================================================
-- Event links (CTR ↔ SAR relationships)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_link (
  from_event_id BIGINT NOT NULL
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  to_event_id BIGINT NOT NULL
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  link_type VARCHAR(32) NOT NULL
    CHECK (link_type IN ('SAR_SUPPORTS_CTR','CTR_SUPPORTS_SAR','RELATED')),

  evidence_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,

  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  PRIMARY KEY (from_event_id, to_event_id, link_type),

  CHECK (from_event_id <> to_event_id)
);

CREATE INDEX IF NOT EXISTS idx_event_link_from
  ON compliance_event_link (from_event_id, linked_at DESC);

CREATE INDEX IF NOT EXISTS idx_event_link_to
  ON compliance_event_link (to_event_id, linked_at DESC);


-- ============================================================
-- Guardrails: detail rows must match event_type
-- ============================================================

-- CTR detail only allowed for CTR events
CREATE OR REPLACE FUNCTION trg_enforce_ctr_detail_event_type()
RETURNS TRIGGER AS $$
DECLARE
  v_event_type TEXT;
BEGIN
  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
    RAISE EXCEPTION 'CTR detail cannot be inserted: compliance_event % does not exist', NEW.event_id;
  END IF;

  IF v_event_type <> 'CTR' THEN
    RAISE EXCEPTION 'CTR detail can only be attached to CTR events. event_id=%, event_type=%',
      NEW.event_id, v_event_type;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS enforce_ctr_detail_event_type ON compliance_event_ctr_detail;

CREATE TRIGGER enforce_ctr_detail_event_type
BEFORE INSERT OR UPDATE ON compliance_event_ctr_detail
FOR EACH ROW
EXECUTE FUNCTION trg_enforce_ctr_detail_event_type();


-- SAR detail only allowed for SAR events
CREATE OR REPLACE FUNCTION trg_enforce_sar_detail_event_type()
RETURNS TRIGGER AS $$
DECLARE
  v_event_type TEXT;
BEGIN
  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
    RAISE EXCEPTION 'SAR detail cannot be inserted: compliance_event % does not exist', NEW.event_id;
  END IF;

  IF v_event_type <> 'SAR' THEN
    RAISE EXCEPTION 'SAR detail can only be attached to SAR events. event_id=%, event_type=%',
      NEW.event_id, v_event_type;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS enforce_sar_detail_event_type ON compliance_event_sar_detail;

CREATE TRIGGER enforce_sar_detail_event_type
BEFORE INSERT OR UPDATE ON compliance_event_sar_detail
FOR EACH ROW
EXECUTE FUNCTION trg_enforce_sar_detail_event_type();
