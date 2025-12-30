#!/bin/bash
set -eE


# ============================
# variable
# ============================
APP_DIR="/opt/bidket/app"
ENV_FILE="$APP_DIR/.env.app"
BACKUP_FILE="$APP_DIR/.env.app.backup"
COMPOSE_FILE="$APP_DIR/docker-compose.app.yml"


# ============================
# rollback handler
# ============================
rollback() {
  echo "❌ Deploy failed. Rolling back..."

  if [ -f "$BACKUP_FILE" ]; then
    cp "$BACKUP_FILE" "$ENV_FILE"
    docker compose \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      up -d
    echo "🔙 Rollback completed"
  else
    echo "⚠️ Backup file not found. Manual recovery required."
  fi
}

trap rollback ERR


# ============================
# Bidket App Deployment Script
# ============================

echo "============================"
echo "🚀 Bidket App Deployment"
echo "============================"

echo "📌 Step 1. 이동: $APP_DIR"
cd "$APP_DIR"

# Backup env
cp "$ENV_FILE" "$BACKUP_FILE"

echo "📌 Step 2. 최신 이미지 Pull (app 전용)"
docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
   pull

echo "📌 Step 3. docker compose 적용"
docker compose \
  --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    up -d --pull always

echo "📌 Step 4. 불필요한 도커 이미지 정리"
docker image prune -f
rm -f "$BACKUP_FILE"

echo "🎉 App 배포 완료!"