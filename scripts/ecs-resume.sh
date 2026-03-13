#!/usr/bin/env bash
# Resume Aurora cluster then ECS (scale to 1).
#
# Usage:
#   ./ecs-resume.sh [--profile PROFILE_NAME]
#
# Uses your current AWS CLI identity. If you use SSO or a named profile, pass it:
#   ./ecs-resume.sh --profile lsattar
#   ./ecs-resume.sh --profile=lsattar
#
# Sign in first if using SSO:  aws sso login --profile PROFILE_NAME

set -e
CLUSTER="finsight-dev"
DB_CLUSTER="finsight-database"
export AWS_REGION="${AWS_REGION:-us-east-1}"

while [[ $# -gt 0 ]]; do
  case $1 in
    --profile=*)
      export AWS_PROFILE="${1#*=}"
      shift
      ;;
    --profile)
      export AWS_PROFILE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      echo "Usage: $0 [--profile PROFILE_NAME]" >&2
      exit 1
      ;;
  esac
done

command -v aws &>/dev/null || { echo "Error: AWS CLI not found." >&2; exit 1; }

DB_STATUS=$(aws rds describe-db-clusters \
  --db-cluster-identifier "$DB_CLUSTER" \
  --no-cli-pager \
  --output text \
  --query 'DBClusters[0].Status')

case "$DB_STATUS" in
  available)
    echo "Aurora cluster $DB_CLUSTER is already running."
    ;;
  starting)
    echo "Aurora cluster $DB_CLUSTER is already starting."
    ;;
  *)
    echo "Starting Aurora cluster $DB_CLUSTER (current status: $DB_STATUS)..."
    aws rds start-db-cluster --db-cluster-identifier "$DB_CLUSTER" --no-cli-pager --output text --query 'DBCluster.DBClusterIdentifier'
    ;;
esac

echo "Scaling ECS services to 1..."
for SVC_ARN in $(aws ecs list-services --cluster "$CLUSTER" --output text --query 'serviceArns[]' | tr '\t' '\n'); do
  [ -z "$SVC_ARN" ] && continue
  SVC=$(basename "$SVC_ARN")
  echo "  $SVC -> 1"
  aws ecs update-service --cluster "$CLUSTER" --service "$SVC" --desired-count 1 --no-cli-pager --output text --query 'service.serviceName' >/dev/null
done
echo "Done. Database started and ECS scaled to 1."
