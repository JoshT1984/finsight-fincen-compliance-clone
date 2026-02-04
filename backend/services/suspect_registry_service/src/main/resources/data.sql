-- =========================================================
-- Mock Data: Suspect Registry Service
-- Tables:
--   suspect
--   suspect_alias
--   address
--   suspect_address
--   criminal_organization
--   suspect_criminal_organization
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE suspect_criminal_organization;
TRUNCATE TABLE suspect_address;
TRUNCATE TABLE suspect_alias;
TRUNCATE TABLE criminal_organization;
TRUNCATE TABLE address;
TRUNCATE TABLE suspect;
SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------
-- SUSPECTS
-- risk_level: UNKNOWN|LOW|MEDIUM|HIGH
-- ssn: fake encrypted string (placeholder)
-- ssn_hash: 64-char hex (SHA-256 style)
-- -------------------------
INSERT INTO suspect (primary_name, dob, ssn, ssn_hash, risk_level)
VALUES
  ('John A. Doe', '1984-06-12', 'ZW5jOkZBS0VfU1NOX0VOQ19KRE9F', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'MEDIUM'),
  ('Acme Imports LLC', NULL, NULL, 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'HIGH'),
  ('Jane Roe', '1991-11-03', 'ZW5jOkZBS0VfU1NOX0VOQ19KUk9F', 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'LOW'),
  ('Unknown Subject', NULL, NULL, 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'UNKNOWN');

-- Capture ids for repeatable inserts (works in Workbench)
SELECT suspect_id, primary_name FROM suspect ORDER BY suspect_id;

-- -------------------------
-- ALIASES
-- alias_type: AKA|LEGAL|NICKNAME|BUSINESS
-- -------------------------
INSERT INTO suspect_alias (suspect_id, alias_name, alias_type, is_primary)
VALUES
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'), 'Jonathan Doe', 'AKA', FALSE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'), 'John Andrew Doe', 'LEGAL', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'), 'Acme Importers', 'BUSINESS', FALSE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'), 'Acme Imports LLC', 'LEGAL', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'), 'J. Roe', 'AKA', FALSE),
  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'), 'Jane Roe', 'LEGAL', TRUE);

-- -------------------------
-- ADDRESSES (deduped via generated key)
-- -------------------------
INSERT INTO address (line1, line2, city, state, postal_code, country)
VALUES
  ('1200 Main St', NULL, 'Austin', 'TX', '78701', 'US'),
  ('55 Commerce Blvd', 'Suite 200', 'Houston', 'TX', '77002', 'US'),
  ('901 Market St', NULL, 'San Antonio', 'TX', '78205', 'US'),
  ('1 Infinite Loop', NULL, 'Cupertino', 'CA', '95014', 'US');

SELECT address_id, line1, city, state FROM address ORDER BY address_id;

-- -------------------------
-- SUSPECT <-> ADDRESS LINKS
-- address_type: HOME|WORK|MAILING|UNKNOWN
-- -------------------------
INSERT INTO suspect_address (suspect_id, address_id, address_type, is_current)
VALUES
  -- John Doe: home + mailing
  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'),
   (SELECT address_id FROM address WHERE line1='1200 Main St' AND city='Austin'),
   'HOME', TRUE),

  ((SELECT suspect_id FROM suspect WHERE primary_name='John A. Doe'),
   (SELECT address_id FROM address WHERE line1='901 Market St' AND city='San Antonio'),
   'MAILING', FALSE),

  -- Acme: work
  ((SELECT suspect_id FROM suspect WHERE primary_name='Acme Imports LLC'),
   (SELECT address_id FROM address WHERE line1='55 Commerce Blvd' AND city='Houston'),
   'WORK', TRUE),

  -- Jane Roe: home
  ((SELECT suspect_id FROM suspect WHERE primary_name='Jane Roe'),
   (SELECT address_id FROM address WHERE line1='901 Market St' AND city='San Antonio'),
   'HOME', TRUE),

  -- Unknown subject: unknown address
  ((SELECT suspect_id FROM suspect WHERE primary_name='Unknown Subject'),
   (SELECT address_id FROM address WHERE line1='1 Infinite Loop' AND city='Cupertino'),
   'UNKNOWN', TRUE);

-- -------------------------
-- CRIMINAL ORGANIZATIONS
-- org_type: CARTEL|GANG|TERRORIST|FRAUD_RING|MONEY_LAUNDERING|OTHER
-- -------------------------
INSERT INTO criminal_organization (org_name, org_type)
VALUES
  ('Gulf Coast Fraud Ring', 'FRAUD_RING'),
  ('Southwest Laundering Network', 'MONEY_LAUNDERING'),
  ('Street Group 12', 'GANG');

SELECT org_id, org_name FROM criminal_organization ORDER BY org_id;

-- -------------------------
-- SUSPECT <-> ORG LINKS
-- -------------------------
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
