#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then cp .env.example .env; chmod 600 .env; fi
export MERIDIAN_LOCAL_BACKCHANNEL_URL=http://backend:8081/logout/connect/back-channel/keycloak
docker compose --ansi never --progress plain --env-file .env -f infrastructure/local/compose.yml --profile app "$@" up -d --build --wait --wait-timeout 180
echo 'Meridian ready: http://localhost:3100 — Mailpit: http://localhost:8025'
