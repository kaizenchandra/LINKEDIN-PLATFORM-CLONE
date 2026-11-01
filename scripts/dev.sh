#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f .env ]]; then
  cp .env.example .env
  chmod 600 .env
fi
set -a
source .env
set +a
if [[ "$(uname)" == Darwin ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi
for port in 3100 8081; do
  if curl --max-time 1 -s "http://localhost:$port" >/dev/null; then
    echo "Port $port is in use. Stop this project's existing application before starting it." >&2
    exit 1
  fi
done
docker compose --env-file .env -f infrastructure/local/compose.yml --profile events up -d --build --wait --wait-timeout 180
wait_for() {
  local url="$1"
  for attempt in {1..90}; do
    if curl --fail --silent --max-time 2 "$url" >/dev/null; then return 0; fi
    sleep 1
  done
  echo "Service did not become ready: $url" >&2
  return 1
}
wait_for http://localhost:28080/realms/meridian/.well-known/openid-configuration
npm --prefix apps/web ci
backend_pid=''
web_pid=''
cleanup() {
  [[ -z "$backend_pid" ]] || kill "$backend_pid" 2>/dev/null || true
  [[ -z "$web_pid" ]] || kill "$web_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM
mvn -q -f apps/backend/pom.xml spring-boot:run &
backend_pid=$!
wait_for http://localhost:8081/readyz
npm --prefix apps/web run dev &
web_pid=$!
wait_for http://localhost:3100
echo 'Meridian ready: http://localhost:3100 — Mailpit: http://localhost:8025'
wait "$backend_pid" "$web_pid"
