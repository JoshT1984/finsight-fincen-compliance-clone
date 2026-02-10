#!/bin/sh
set -eu

: "${JWT_PUBLIC_KEY_PATH:=/run/secrets/jwt_public.pem}"
: "${JWT_PRIVATE_KEY_PATH:=/run/secrets/jwt_private.pem}"

mkdir -p "$(dirname "$JWT_PUBLIC_KEY_PATH")" "$(dirname "$JWT_PRIVATE_KEY_PATH")"

if [ -n "${JWT_PUBLIC_KEY_PEM:-}" ]; then
  printf "%b" "$JWT_PUBLIC_KEY_PEM" > "$JWT_PUBLIC_KEY_PATH"
fi

if [ -n "${JWT_PRIVATE_KEY_PEM:-}" ]; then
  printf "%b" "$JWT_PRIVATE_KEY_PEM" > "$JWT_PRIVATE_KEY_PATH"
fi

chmod 400 "$JWT_PUBLIC_KEY_PATH" "$JWT_PRIVATE_KEY_PATH" 2>/dev/null || true

exec java $JAVA_OPTS -jar /app/app.jar
