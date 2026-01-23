-- Migration: Update document table constraint to allow SAR documents with both sar_id and case_id
-- Run this SQL against your database to update the existing constraint

-- Drop the old constraint
ALTER TABLE document DROP CONSTRAINT IF EXISTS document_check;

-- Add the new constraint that allows SAR documents to have both sar_id and case_id
ALTER TABLE document ADD CONSTRAINT document_check CHECK (
    -- At least one ID must be set
    (ctr_id IS NOT NULL)::int +
    (sar_id IS NOT NULL)::int +
    (case_id IS NOT NULL)::int >= 1
    AND
    -- CTR documents: only ctr_id can be set
    (document_type != 'CTR' OR (ctr_id IS NOT NULL AND sar_id IS NULL AND case_id IS NULL))
    AND
    -- CASE documents: only case_id can be set
    (document_type != 'CASE' OR (case_id IS NOT NULL AND ctr_id IS NULL AND sar_id IS NULL))
    AND
    -- SAR documents: sar_id required, ctr_id not allowed (case_id is optional)
    (document_type != 'SAR' OR (sar_id IS NOT NULL AND ctr_id IS NULL))
);
