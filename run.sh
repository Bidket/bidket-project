#!/bin/bash

# ============================
# Bidket Deployment Script
# ============================
set -e

echo "EUREKA_IMAGE_TAG=$EUREKA_IMAGE_TAG"
echo "GATEWAY_IMAGE_TAG=$GATEWAY_IMAGE_TAG"

echo "📌 Step 1. 이동: /opt/bidket"
cd /opt/bidket || exit

echo "📌 Step 2. 최신 이미지 Pull"
docker compose  --env-file .env.dev \
  -f docker-compose.prod.yml pull

echo "⏳ Step 2-1. build/push 대기 (임시)"
sleep 20

echo "📌 Step 3. docker compose 적용"
docker compose --env-file .env.dev \
  -f docker-compose.prod.yml \
  up -d --pull always

echo "📌 Step 4. 불필요한 도커 이미지 정리"
docker image prune -f

echo "🎉 배포 완료! Bidket 프로젝트가 최신 상태로 실행되었습니다."