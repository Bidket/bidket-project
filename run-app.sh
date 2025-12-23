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
# function
# ============================
set_env() {
  local KEY=$1
  local VALUE=$2
  local FILE=$3

  if grep -q "^${KEY}=" "$FILE"; then
    sed -i "s|^${KEY}=.*|${KEY}=${VALUE}|" "$FILE"
  else
    echo "${KEY}=${VALUE}" >> "$FILE"
  fi
}


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

# required env validation(태그 없는 배포 막기)
: "${GATEWAY_IMAGE_TAG:?GATEWAY_IMAGE_TAG is required}"
: "${USER_IMAGE_TAG:?USER_IMAGE_TAG is required}"
: "${PRODUCT_IMAGE_TAG:?PRODUCT_IMAGE_TAG is required}"
: "${ORDER_IMAGE_TAG:?ORDER_IMAGE_TAG is required}"
: "${AUCTION_IMAGE_TAG:?AUCTION_IMAGE_TAG is required}"
: "${NOTIFICATION_IMAGE_TAG:?NOTIFICATION_IMAGE_TAG is required}"
: "${QUEUE_IMAGE_TAG:?QUEUE_IMAGE_TAG is required}"

# Image Tag setting
set_env GATEWAY_IMAGE_TAG "$GATEWAY_IMAGE_TAG" "$ENV_FILE"
set_env USER_IMAGE_TAG "$USER_IMAGE_TAG" "$ENV_FILE"
set_env PRODUCT_IMAGE_TAG "$PRODUCT_IMAGE_TAG" "$ENV_FILE"
set_env ORDER_IMAGE_TAG "$ORDER_IMAGE_TAG" "$ENV_FILE"
set_env AUCTION_IMAGE_TAG "$AUCTION_IMAGE_TAG" "$ENV_FILE"
set_env NOTIFICATION_IMAGE_TAG "$NOTIFICATION_IMAGE_TAG" "$ENV_FILE"
set_env QUEUE_IMAGE_TAG "$QUEUE_IMAGE_TAG" "$ENV_FILE"

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