cd ~
cat > reset_reload_db.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

# ---------
# Config
# ---------
RDS="${RDS:-finsight-database-instance-1.chet1jfzht8q.us-east-1.rds.amazonaws.com}"
DB_DIR="${DB_DIR:-$HOME/db}"

# Choose auth method:
# 1) Interactive password prompt (default): mysql -p
# 2) Login path (recommended): export MYSQL_AUTH="--login-path=rdsadmin"
MYSQL_AUTH="${MYSQL_AUTH:--u admin -p}"

MYSQL="mysql -h \"$RDS\" $MYSQL_AUTH"
run() { bash -lc "$MYSQL $*"; }

need_file() {
  local f="$1"
  [[ -f "$f" ]] || { echo "Missing file: $f" >&2; exit 1; }
}

echo "==> Using RDS host: $RDS"
echo "==> Using DB dir:  $DB_DIR"
echo

# ---------
# Validate files exist
# ---------
need_file "$DB_DIR/01_identity_schema.sql"
need_file "$DB_DIR/02_documents_cases_schema.sql"
need_file "$DB_DIR/03_compliance_event_schema.sql"
need_file "$DB_DIR/04_suspect_registry_schema.sql"

need_file "$DB_DIR/01_identity_mock.sql"
need_file "$DB_DIR/02_documents_cases_mock.sql"
need_file "$DB_DIR/03_compliance_event_mock.sql"
need_file "$DB_DIR/04_suspect_registry_mock.sql"

echo "==> 1) Load schemas (idempotent) in required order"
run "< \"$DB_DIR/01_identity_schema.sql\""
run "< \"$DB_DIR/02_documents_cases_schema.sql\""
run "< \"$DB_DIR/03_compliance_event_schema.sql\""
run "< \"$DB_DIR/04_suspect_registry_schema.sql\""
echo "==> Schemas loaded"
echo

echo "==> 2) Truncate all tables (resets AUTO_INCREMENT)"
# Identity
run "-D identity -e \"\
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE oauth_identity;
TRUNCATE TABLE app_user;
TRUNCATE TABLE role;
SET FOREIGN_KEY_CHECKS=1;\""

# Documents + Cases (include mapping table)
run "-D documents_cases -e \"\
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE document;
TRUNCATE TABLE case_note;
TRUNCATE TABLE case_file;
TRUNCATE TABLE audit_event;
TRUNCATE TABLE sar_compliance_event_map;
SET FOREIGN_KEY_CHECKS=1;\""

# Compliance
run "-D compliance_event -e \"\
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE compliance_event_link;
TRUNCATE TABLE audit_action;
TRUNCATE TABLE compliance_event_ctr_detail;
TRUNCATE TABLE compliance_event_sar_detail;
TRUNCATE TABLE compliance_event;
TRUNCATE TABLE suspect_snapshot_at_time_of_event;
SET FOREIGN_KEY_CHECKS=1;\""

# Suspect registry
run "-D suspect_registry -e \"\
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE suspect_criminal_organization;
TRUNCATE TABLE suspect_address;
TRUNCATE TABLE suspect_alias;
TRUNCATE TABLE criminal_organization;
TRUNCATE TABLE address;
TRUNCATE TABLE suspect;
SET FOREIGN_KEY_CHECKS=1;\""

echo "==> Truncates complete"
echo

echo "==> 3) Load mock data in required order"
run "< \"$DB_DIR/01_identity_mock.sql\""
run "< \"$DB_DIR/02_documents_cases_mock.sql\""
run "< \"$DB_DIR/03_compliance_event_mock.sql\""
run "< \"$DB_DIR/04_suspect_registry_mock.sql\""
echo "==> Mock data loaded"
echo

echo "==> 4) Ensure docs-cases SAR mapping table exists + seed"
run "-D documents_cases -e \"\
CREATE TABLE IF NOT EXISTS sar_compliance_event_map (
  sar_id BIGINT NOT NULL PRIMARY KEY,
  compliance_source_system VARCHAR(64) NOT NULL DEFAULT 'case-mgmt',
  compliance_source_entity_id VARCHAR(64) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_source (compliance_source_system, compliance_source_entity_id)
);\""

# Seed 10001-10003 mapping (idempotent)
run "-D documents_cases -e \"\
INSERT INTO sar_compliance_event_map (sar_id, compliance_source_system, compliance_source_entity_id)
VALUES
  (10001, 'case-mgmt', 'SAR-10001'),
  (10002, 'case-mgmt', 'SAR-10002'),
  (10003, 'case-mgmt', 'SAR-10003')
ON DUPLICATE KEY UPDATE
  compliance_source_system=VALUES(compliance_source_system),
  compliance_source_entity_id=VALUES(compliance_source_entity_id);\""

echo "==> Mapping ensured"
echo

echo "==> 5) Verification snapshot"
run "-e \"\
SELECT 'identity.roles' AS what, COUNT(*) AS cnt FROM identity.role
UNION ALL SELECT 'identity.users', COUNT(*) FROM identity.app_user
UNION ALL SELECT 'docs.cases', COUNT(*) FROM documents_cases.case_file
UNION ALL SELECT 'docs.documents', COUNT(*) FROM documents_cases.document
UNION ALL SELECT 'docs.notes', COUNT(*) FROM documents_cases.case_note
UNION ALL SELECT 'docs.audit_events', COUNT(*) FROM documents_cases.audit_event
UNION ALL SELECT 'docs.sar_map', COUNT(*) FROM documents_cases.sar_compliance_event_map
UNION ALL SELECT 'compliance.events', COUNT(*) FROM compliance_event.compliance_event
UNION ALL SELECT 'compliance.ctr_details', COUNT(*) FROM compliance_event.compliance_event_ctr_detail
UNION ALL SELECT 'compliance.sar_details', COUNT(*) FROM compliance_event.compliance_event_sar_detail
UNION ALL SELECT 'registry.suspects', COUNT(*) FROM suspect_registry.suspect
UNION ALL SELECT 'registry.orgs', COUNT(*) FROM suspect_registry.criminal_organization
UNION ALL SELECT 'registry.addresses', COUNT(*) FROM suspect_registry.address;\""

echo
echo "==> 6) Auto-increment check (should equal row_count + 1)"
run "-e \"\
SELECT table_schema, table_name, auto_increment
FROM information_schema.tables
WHERE table_schema IN ('identity','documents_cases','compliance_event','suspect_registry')
  AND auto_increment IS NOT NULL
ORDER BY table_schema, table_name;\""

echo
echo "✅ Reset & reload complete."
EOF

chmod +x reset_reload_db.sh
