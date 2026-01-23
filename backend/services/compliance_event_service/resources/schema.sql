<<<<<<< HEAD
-- ============================================================
-- MySQL 8.0+ version of your schema (InnoDB)
-- Notes:
-- ============================================================

CREATE DATABASE IF NOT EXISTS compliance_event;
USE compliance_event;

=======
CREATE SCHEMA IF NOT EXISTS compliance_event;
SET search_path TO compliance_event;

>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
-- ============================================================
-- Suspect snapshot (point-in-time)
-- ============================================================
CREATE TABLE IF NOT EXISTS suspect_snapshot_at_time_of_event (
<<<<<<< HEAD
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
  suspect_id BIGINT NOT NULL,
  last_known_alias VARCHAR(256),
  last_known_address JSON,
  suspect_minimal JSON,
  captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (snapshot_id),
  KEY idx_suspect_snapshot_suspect (suspect_id, captured_at)
) ENGINE=InnoDB;
=======
  snapshot_id BIGSERIAL PRIMARY KEY,
  suspect_id BIGINT NOT NULL,
  last_known_alias VARCHAR(256),
  last_known_address JSONB,
  suspect_minimal JSONB,
  captured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_suspect_snapshot_suspect
  ON suspect_snapshot_at_time_of_event (suspect_id, captured_at DESC);

>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

-- ============================================================
-- Unified compliance event (fact table)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT,

  event_type VARCHAR(16) NOT NULL,
  CHECK (event_type IN ('CTR','SAR')),

  source_system VARCHAR(64) NOT NULL,
  source_entity_id VARCHAR(64) NOT NULL,

  external_subject_key VARCHAR(128),

<<<<<<< HEAD
  source_subject_type VARCHAR(32),
  CHECK (source_subject_type IN ('SUSPECT','CUSTOMER','ACCOUNT') OR source_subject_type IS NULL),
  source_subject_id VARCHAR(128),

  suspect_snapshot_id BIGINT NULL,

  event_time TIMESTAMP NOT NULL,
  total_amount DECIMAL(14,2),
  CHECK (total_amount IS NULL OR total_amount >= 0),
=======
  source_subject_type VARCHAR(32)
    CHECK (source_subject_type IN ('SUSPECT','CUSTOMER','ACCOUNT')),
  source_subject_id VARCHAR(128),

  suspect_snapshot_id BIGINT
    REFERENCES suspect_snapshot_at_time_of_event(snapshot_id)
    ON DELETE SET NULL,

  event_time TIMESTAMPTZ NOT NULL,
  total_amount NUMERIC(14,2) CHECK (total_amount >= 0),
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  status VARCHAR(32),

  severity_score INT,

  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  -- Generated columns to emulate Postgres partial unique indexes and guardrails
  idempotency_key_nn VARCHAR(128)
    GENERATED ALWAYS AS (IFNULL(idempotency_key, CONCAT('~NULL~', event_id))) STORED,

<<<<<<< HEAD
  audit_idempotency_key_nn VARCHAR(128)
    GENERATED ALWAYS AS (NULL) STORED, -- placeholder (not used here; see audit_action)

  PRIMARY KEY (event_id),

  UNIQUE KEY uk_event_source_entity (source_system, source_entity_id),

  -- Emulate: UNIQUE (source_system, idempotency_key) WHERE idempotency_key IS NOT NULL
  UNIQUE KEY uk_event_idempotency (source_system, idempotency_key_nn),

  KEY idx_event_type_time (event_type, event_time),
  KEY idx_event_source (source_system, source_entity_id),
  KEY idx_event_suspect (suspect_snapshot_id),
  KEY idx_event_subject (source_subject_type, source_subject_id, event_time),
  KEY idx_event_created (created_at),
=======
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
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  CONSTRAINT fk_event_suspect_snapshot
    FOREIGN KEY (suspect_snapshot_id)
    REFERENCES suspect_snapshot_at_time_of_event(snapshot_id)
    ON DELETE SET NULL,

<<<<<<< HEAD
  -- Status guardrail: status allowed by event_type (allows NULL)
  CONSTRAINT chk_status_by_event_type
  CHECK (
    status IS NULL
    OR (event_type = 'CTR' AND status IN ('CREATED','FILED'))
    OR (event_type = 'SAR' AND status IN ('DRAFT','SUBMITTED'))
  ),

  -- Severity SAR-only (prevents CTR from using severity_score)
  CONSTRAINT chk_severity_score_sar_only
  CHECK (
    severity_score IS NULL
    OR (event_type = 'SAR' AND severity_score BETWEEN 0 AND 100)
  )
) ENGINE=InnoDB;

=======
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
-- ============================================================
-- CTR Detail (1:1 with compliance_event)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_ctr_detail (
<<<<<<< HEAD
  event_id BIGINT NOT NULL,

  customer_name VARCHAR(128) NOT NULL,

  transaction_time TIMESTAMP NOT NULL,
=======
  event_id BIGINT PRIMARY KEY
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  customer_name VARCHAR(128) NOT NULL,

  -- Real timestamp now
  transaction_time TIMESTAMPTZ NOT NULL,

  ctr_form_data JSONB NOT NULL DEFAULT '{}'::jsonb,
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  ctr_form_data JSON NOT NULL,

<<<<<<< HEAD
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
=======
CREATE INDEX IF NOT EXISTS idx_ctr_detail_created
  ON compliance_event_ctr_detail (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ctr_detail_tx_time
  ON compliance_event_ctr_detail (transaction_time DESC);
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  PRIMARY KEY (event_id),

<<<<<<< HEAD
  KEY idx_ctr_detail_created (created_at),
  KEY idx_ctr_detail_tx_time (transaction_time),

  CONSTRAINT fk_ctr_detail_event
    FOREIGN KEY (event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

=======
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
-- ============================================================
-- SAR Detail (1:1 with compliance_event)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_sar_detail (
<<<<<<< HEAD
  event_id BIGINT NOT NULL,
=======
  event_id BIGINT PRIMARY KEY
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  narrative TEXT,

  activity_start TIMESTAMP NULL,
  activity_end TIMESTAMP NULL,

<<<<<<< HEAD
  form_data JSON NOT NULL,
=======
  form_data JSONB NOT NULL DEFAULT '{}'::jsonb,
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  submitted_at TIMESTAMP NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

<<<<<<< HEAD
  PRIMARY KEY (event_id),
=======
CREATE INDEX IF NOT EXISTS idx_sar_detail_created
  ON compliance_event_sar_detail (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sar_detail_submitted
  ON compliance_event_sar_detail (submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_sar_detail_activity
  ON compliance_event_sar_detail (activity_start DESC);
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  KEY idx_sar_detail_created (created_at),
  KEY idx_sar_detail_submitted (submitted_at),
  KEY idx_sar_detail_activity (activity_start),

<<<<<<< HEAD
  CONSTRAINT fk_sar_detail_event
    FOREIGN KEY (event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

=======
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
-- ============================================================
-- Audit trail (append-only)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_action (
  audit_id BIGINT NOT NULL AUTO_INCREMENT,

<<<<<<< HEAD
  event_id BIGINT NULL,
=======
  event_id BIGINT
    REFERENCES compliance_event(event_id)
    ON DELETE SET NULL,
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  actor_user_id CHAR(36) NULL,  -- store UUID as string (or BINARY(16) if you prefer)
  actor_role VARCHAR(64),

  action VARCHAR(64) NOT NULL,

<<<<<<< HEAD
  metadata JSON NOT NULL,
=======
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  correlation_id VARCHAR(128),
  idempotency_key VARCHAR(128),

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

<<<<<<< HEAD
  -- Emulate: UNIQUE (idempotency_key) WHERE idempotency_key IS NOT NULL
  idempotency_key_nn VARCHAR(128)
    GENERATED ALWAYS AS (IFNULL(idempotency_key, CONCAT('~NULL~', audit_id))) STORED,

  PRIMARY KEY (audit_id),
=======
CREATE INDEX IF NOT EXISTS idx_audit_created
  ON audit_action (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_event
  ON audit_action (event_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_idempotency
  ON audit_action (idempotency_key)
  WHERE idempotency_key IS NOT NULL;
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)

  KEY idx_audit_created (created_at),
  KEY idx_audit_event (event_id, created_at),

<<<<<<< HEAD
  UNIQUE KEY uk_audit_idempotency (idempotency_key_nn),

  CONSTRAINT fk_audit_event
    FOREIGN KEY (event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

=======
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
-- ============================================================
-- Event links (CTR ↔ SAR relationships)
-- ============================================================
CREATE TABLE IF NOT EXISTS compliance_event_link (
<<<<<<< HEAD
  from_event_id BIGINT NOT NULL,
  to_event_id BIGINT NOT NULL,

  link_type VARCHAR(32) NOT NULL,
  CHECK (link_type IN ('SAR_SUPPORTS_CTR','CTR_SUPPORTS_SAR','RELATED')),

  evidence_snapshot JSON NOT NULL,

  linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (from_event_id, to_event_id, link_type),

  CHECK (from_event_id <> to_event_id),

  KEY idx_event_link_from (from_event_id, linked_at),
  KEY idx_event_link_to (to_event_id, linked_at),

  CONSTRAINT fk_link_from_event
    FOREIGN KEY (from_event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_link_to_event
    FOREIGN KEY (to_event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- Guardrails: detail rows must match event_type (MySQL triggers)
-- ============================================================

DROP TRIGGER IF EXISTS enforce_ctr_detail_event_type_bi;
DELIMITER $$
CREATE TRIGGER enforce_ctr_detail_event_type_bi
BEFORE INSERT ON compliance_event_ctr_detail
FOR EACH ROW
BEGIN
  DECLARE v_event_type VARCHAR(16);

=======
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
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
<<<<<<< HEAD
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('CTR detail cannot be inserted: compliance_event ', NEW.event_id, ' does not exist');
  END IF;

  IF v_event_type <> 'CTR' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('CTR detail can only be attached to CTR events. event_id=', NEW.event_id, ', event_type=', v_event_type);
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS enforce_ctr_detail_event_type_bu;
DELIMITER $$
CREATE TRIGGER enforce_ctr_detail_event_type_bu
BEFORE UPDATE ON compliance_event_ctr_detail
FOR EACH ROW
BEGIN
  DECLARE v_event_type VARCHAR(16);

  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('CTR detail cannot be updated: compliance_event ', NEW.event_id, ' does not exist');
=======
    RAISE EXCEPTION 'CTR detail cannot be inserted: compliance_event % does not exist', NEW.event_id;
  END IF;

  IF v_event_type <> 'CTR' THEN
    RAISE EXCEPTION 'CTR detail can only be attached to CTR events. event_id=%, event_type=%',
      NEW.event_id, v_event_type;
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
  END IF;

  IF v_event_type <> 'CTR' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('CTR detail can only be attached to CTR events. event_id=', NEW.event_id, ', event_type=', v_event_type);
  END IF;
END$$
DELIMITER ;

<<<<<<< HEAD
DROP TRIGGER IF EXISTS enforce_sar_detail_event_type_bi;
DELIMITER $$
CREATE TRIGGER enforce_sar_detail_event_type_bi
BEFORE INSERT ON compliance_event_sar_detail
FOR EACH ROW
BEGIN
  DECLARE v_event_type VARCHAR(16);

  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('SAR detail cannot be inserted: compliance_event ', NEW.event_id, ' does not exist');
  END IF;

  IF v_event_type <> 'SAR' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('SAR detail can only be attached to SAR events. event_id=', NEW.event_id, ', event_type=', v_event_type);
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS enforce_sar_detail_event_type_bu;
DELIMITER $$
CREATE TRIGGER enforce_sar_detail_event_type_bu
BEFORE UPDATE ON compliance_event_sar_detail
FOR EACH ROW
BEGIN
  DECLARE v_event_type VARCHAR(16);

=======
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
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
  SELECT event_type INTO v_event_type
  FROM compliance_event
  WHERE event_id = NEW.event_id;

  IF v_event_type IS NULL THEN
<<<<<<< HEAD
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('SAR detail cannot be updated: compliance_event ', NEW.event_id, ' does not exist');
  END IF;

  IF v_event_type <> 'SAR' THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = CONCAT('SAR detail can only be attached to SAR events. event_id=', NEW.event_id, ', event_type=', v_event_type);
  END IF;
END$$
DELIMITER ;
=======
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
>>>>>>> 38451d1 (backend/Set up backend compliance models to match new schema)
