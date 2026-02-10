-- Migration to relax severity_score constraint for CTR and SAR
ALTER TABLE compliance_event
DROP CONSTRAINT IF EXISTS chk_severity_score_sar_only;

ALTER TABLE compliance_event
ADD CONSTRAINT chk_severity_score_ctr_sar_only
CHECK (
  severity_score IS NULL OR event_type IN ('CTR', 'SAR')
);