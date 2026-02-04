-- =========================================================
-- DEV BOOTSTRAP SCRIPT
-- Spring Boot / Angular / MySQL / FinCEN Project
--
-- Loads 4 schemas + mock data in correct dependency order
-- Safe to re-run (mock scripts handle truncation)
-- =========================================================

-- ---------------------------------------------------------
-- 1) IDENTITY / AUTH SERVICE
-- ---------------------------------------------------------
-- Provides users + roles referenced by other services
-- ---------------------------------------------------------

SOURCE 01_identity_schema.sql;
SOURCE 01_identity_mock.sql;

-- ---------------------------------------------------------
-- 2) DOCUMENTS + CASES SERVICE
-- ---------------------------------------------------------
-- Uses SAR / CTR IDs, user UUIDs (no cross-schema FKs)
-- ---------------------------------------------------------

SOURCE 02_documents_cases_schema.sql;
SOURCE 02_documents_cases_mock.sql;

-- ---------------------------------------------------------
-- 3) COMPLIANCE EVENT SERVICE
-- CTR + SAR + EVENT LINKING
-- ---------------------------------------------------------

SOURCE 03_compliance_event_schema.sql;
SOURCE 03_compliance_event_mock.sql;

-- ---------------------------------------------------------
-- 4) SUSPECT REGISTRY SERVICE
-- System of record for suspects, aliases, addresses, orgs
-- ---------------------------------------------------------

SOURCE 04_suspect_registry_schema.sql;
SOURCE 04_suspect_registry_mock.sql;

-- =========================================================
-- BOOTSTRAP COMPLETE
-- =========================================================
