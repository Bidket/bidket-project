#!/bin/bash

# ============================
# Bidket App Deployment Script
# ============================
set -e

echo "============================"
echo "🚀 Bidket App Deployment"
echo "============================"

echo "GATEWAY_IMAGE_TAG=$GATEWAY_IMAGE_TAG"

echo "📌 Step 1. 이동: /opt/bidket/app"
cd /opt/bidket/app || exit 1

echo "📌 Step 2. 최신 이미지 Pull (app 전용)"
docker compose \
  --env-file .env.app \
  -f docker-compose.app.yml pull

echo "⏳ Step 2-1. build/push 대기 (임시)"
sleep 10

echo "📌 Step 3. docker compose 적용"
docker compose \
  --env-file .env.app \
  -f docker-compose.app.yml \
  up -d --pull always

echo "📌 Step 4. 불필요한 도커 이미지 정리"
docker image prune -f

echo "🎉 App 배포 완료!"