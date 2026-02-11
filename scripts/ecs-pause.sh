#!/usr/bin/env bash
# Pause ECS (scale to 0) and Aurora cluster to save costs.
#
# Usage:
#   ./ecs-pause.sh [--profile PROFILE_NAME]
#
# Uses your current AWS CLI identity. If you use SSO or a named profile, pass it:
#   ./ecs-pause.sh --profile lsattar
#   ./ecs-pause.sh --profile=lsattar
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

echo "Scaling ECS services to 0..."
for SVC_ARN in $(aws ecs list-services --cluster "$CLUSTER" --output text --query 'serviceArns[]' | tr '\t' '\n'); do
  [ -z "$SVC_ARN" ] && continue
  SVC=$(basename "$SVC_ARN")
  echo "  $SVC -> 0"
  aws ecs update-service --cluster "$CLUSTER" --service "$SVC" --desired-count 0 --no-cli-pager --output text --query 'service.serviceName' >/dev/null
done
echo "Stopping Aurora cluster $DB_CLUSTER..."
aws rds stop-db-cluster --db-cluster-identifier "$DB_CLUSTER" --no-cli-pager --output text --query 'DBCluster.DBClusterIdentifier'
echo "Done. ECS scaled to 0 and database stopped."
