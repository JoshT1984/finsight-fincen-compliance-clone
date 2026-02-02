#!/usr/bin/env bash

echo "🔧 Setting PROD environment variables..."

export SPRING_PROFILES_ACTIVE=prod

# Database
export DB_URL=jdbc:mysql://localhost:3306/compliance_event
export DB_USER=mysql
export DB_PASS=mysql

# RabbitMQ (Amazon MQ)
export RABBITMQ_HOST=b-xxxx.mq.us-east-1.amazonaws.com
export RABBITMQ_PORT=5671
export RABBITMQ_USERNAME=finsight
export RABBITMQ_PASSWORD=secret

echo "✅ PROD environment set"
echo "Active profile: $SPRING_PROFILES_ACTIVE"

