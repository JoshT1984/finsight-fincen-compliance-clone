#!/usr/bin/env bash

echo "🧹 Clearing environment variables..."

unset SPRING_PROFILES_ACTIVE
unset SPRING_DATASOURCE_URL
unset SPRING_DATASOURCE_USERNAME
unset SPRING_DATASOURCE_PASSWORD

unset DB_URL
unset DB_USER
unset DB_PASS

unset RABBITMQ_HOST
unset RABBITMQ_PORT
unset RABBITMQ_USERNAME
unset RABBITMQ_PASSWORD

# --- REQUIRED: dev profile + DB connection ---
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:13306/documents_cases?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="admin"
export SPRING_DATASOURCE_PASSWORD='swordfish$fincen1117'

echo "✅ Environment set"
echo "PROFILE=$SPRING_PROFILES_ACTIVE"
echo "URL=$SPRING_DATASOURCE_URL"
echo "USER=$SPRING_DATASOURCE_USERNAME"
echo "PASS_SET=$([ -n "$SPRING_DATASOURCE_PASSWORD" ] && echo yes || echo no)"
