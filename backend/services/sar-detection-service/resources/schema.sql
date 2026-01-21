-- ======================================
-- SAR + Detection Service Schema
-- ======================================

CREATE TABLE IF NOT EXISTS sar (
  sar_id BIGSERIAL PRIMARY KEY,
  suspicion_score INT NOT NULL CHECK (suspicion_score BETWEEN 0 AND 100),
  narrative TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED'))
);

-- Join table linking SARs to CTRs (CTR IDs are external; no FK)
CREATE TABLE IF NOT EXISTS sar_ctr (
  sar_id BIGINT NOT NULL REFERENCES sar(sar_id) ON DELETE CASCADE,
  ctr_id BIGINT NOT NULL,
  PRIMARY KEY (sar_id, ctr_id)
);

CREATE TABLE IF NOT EXISTS detection_rule (
  rule_code VARCHAR(64) PRIMARY KEY,
  description TEXT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sar_rule_hit (
  sar_id BIGINT NOT NULL REFERENCES sar(sar_id) ON DELETE CASCADE,
  rule_code VARCHAR(64) NOT NULL REFERENCES detection_rule(rule_code) ON DELETE RESTRICT,
  details JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (sar_id, rule_code)
);

CREATE INDEX IF NOT EXISTS idx_sar_created ON sar(created_at);
CREATE INDEX IF NOT EXISTS idx_rule_active ON detection_rule(is_active);

-- Seed a few example rules (safe inserts)
INSERT INTO detection_rule(rule_code, description, is_active)
VALUES ('CASH_OVER_10K', 'Aggregate cash activity exceeds $10,000 threshold in a day', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO detection_rule(rule_code, description, is_active)
VALUES ('RAPID_STRUCTURING', 'Multiple cash deposits just below reporting threshold', TRUE)
ON CONFLICT DO NOTHING;
