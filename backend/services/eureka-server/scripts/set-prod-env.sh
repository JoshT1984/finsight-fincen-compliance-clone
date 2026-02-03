#!/usr/bin/env bash
# eureka-dev-env.sh
# Usage: source ./eureka-dev-env.sh

echo "🔧 Setting DEV environment variables for Eureka Server..."

# ----------------------
# Spring profile
# ----------------------
export SPRING_PROFILES_ACTIVE="prod"
# (You are intentionally using application-prod.yml even in dev ECS)

# ----------------------
# Server
# ----------------------
export SERVER_PORT="8761"

# ----------------------
# Safety: explicitly disable things Eureka must not try to use
# ----------------------
unset DB_URL
unset DB_USER
unset DB_PASS

unset RABBITMQ_HOST
unset RABBITMQ_PORT
unset RABBITMQ_USERNAME
unset RABBITMQ_PASSWORD

unset AWS_S3_BUCKET
unset AWS_REGION

# ----------------------
# Friendly output
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

echo "✅ Eureka DEV environment set"
safe_print "SPRING_PROFILES_ACTIVE"
safe_print "SERVER_PORT"

echo "ℹ️  Tip: run with 'source ./eureka-dev-env.sh'"
