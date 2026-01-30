#!/usr/bin/env bash

echo "🧹 Clearing environment variables..."

unset SPRING_PROFILES_ACTIVE

unset DB_URL
unset DB_USER
unset DB_PASS

unset RABBITMQ_HOST
unset RABBITMQ_PORT
unset RABBITMQ_USERNAME
unset RABBITMQ_PASSWORD

echo "✅ Environment cleared"
echo "Active profile: ${SPRING_PROFILES_ACTIVE:-local}"

