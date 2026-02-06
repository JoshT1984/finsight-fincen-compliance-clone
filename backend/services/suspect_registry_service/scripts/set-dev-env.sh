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

# ✅ IMPORTANT: actually set dev profile
export SPRING_PROFILES_ACTIVE=dev

# ✅ DB settings used by application-dev.yml
export DB_USER="admin"
export DB_PASS="swordfish$fincen1117"
export DB_URL="jdbc:mysql://127.0.0.1:13306/suspect_registry?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

# ✅ If JwtConfig uses jwt.public-key, make sure these exist too (or rely on the defaults in YAML)
export JWT_PUBLIC_KEY="file:/Users/xxblackhawkdevxx/Downloads/secrets/jwt_public.pem"
export JWT_PRIVATE_KEY="file:/Users/xxblackhawkdevxx/Downloads/secrets/jwt_private.pem"

export EUREKA_SERVER_URL="http://localhost:8761/eureka/"

echo "✅ Environment set"
echo "Active profile: ${SPRING_PROFILES_ACTIVE}"
