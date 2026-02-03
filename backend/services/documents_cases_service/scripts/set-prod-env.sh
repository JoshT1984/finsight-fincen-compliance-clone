#!/usr/bin/env bash
# prod-env.sh
# Usage: source ./prod-env.sh

## set -euo pipefail  # Disabled for VS Code/zsh compatibility

echo "🔧 Setting PROD environment variables..."

# ----------------------
# Spring profile
# ----------------------
export SPRING_PROFILES_ACTIVE="prod"

# ----------------------
# Database (Aurora/RDS)
# ----------------------
export DB_URL='jdbc:mysql://finsight-database-instance-1.chet1jfzht8q.us-east-1.rds.amazonaws.com:3306/finsight_documents_cases?serverTimezone=UTC&useSSL=true&allowPublicKeyRetrieval=true'
export DB_USER="admin"
export DB_PASS='swordfish$fincen1117'   # NOTE: single quotes protect $

# ----------------------
# RabbitMQ (Amazon MQ)
# ----------------------
export RABBITMQ_HOST="b-e5e91599-235a-4dbc-9a34-e6457271d60e.mq.us-east-1.on.aws"
export RABBITMQ_PORT="5671"
export RABBITMQ_USERNAME="finsight-app"
export RABBITMQ_PASSWORD='swordfish$fincen1117'  # NOTE: single quotes protect $

# ----------------------
# S3
# ----------------------
export AWS_S3_BUCKET="finsight-documents-2026"
export AWS_REGION="us-east-1"

# ----------------------
# Friendly output (safe with -u)
# ----------------------
safe_print() {
  local name="$1"
  local value="$(printenv "$name")"
  if [[ -z "$value" ]]; then
    printf '%s: %s\n' "$name" "<unset>"
  else
    printf '%s: %s\n' "$name" "$value"
  fi
}

echo "✅ PROD environment set"
safe_print "SPRING_PROFILES_ACTIVE"
safe_print "DB_URL"
safe_print "DB_USER"
safe_print "RABBITMQ_HOST"
safe_print "RABBITMQ_PORT"
safe_print "RABBITMQ_USERNAME"
safe_print "AWS_S3_BUCKET"
safe_print "AWS_REGION"

echo "ℹ️  Tip: run with 'source ./prod-env.sh' so variables remain in this shell."
