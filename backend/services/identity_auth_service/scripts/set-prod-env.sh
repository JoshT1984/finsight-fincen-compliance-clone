#!/usr/bin/env bash
# prod-env.sh
# Usage: source ./prod-env.sh

export SPRING_PROFILES_ACTIVE=prod
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
