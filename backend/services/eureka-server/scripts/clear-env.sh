
#!/usr/bin/env bash
# set -euo pipefail  # Disabled for VS Code/zsh compatibility

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
echo "Active profile: $(printenv SPRING_PROFILES_ACTIVE || echo local)"

