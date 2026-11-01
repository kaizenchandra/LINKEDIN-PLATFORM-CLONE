#!/usr/bin/env bash
# Uses only a unique disposable Compose project; never resets developer volumes.
set -euo pipefail
cd "$(dirname "$0")/.."
project="meridian-smoke-$(date +%s)-$$"
export POSTGRES_PORT=0 KEYCLOAK_PORT=0 MAILPIT_PORT=0
export OIDC_SECRET=local-only-replace-before-any-deployment
export DB_PASSWORD="smoke-only-$project"
compose=(docker compose --ansi never --progress plain --project-name "$project" -f infrastructure/local/compose.yml)
cleanup() {
  local result=$?
  trap - EXIT
  if [[ "$result" != 0 ]]; then "${compose[@]}" ps >&2 || true; fi
  "${compose[@]}" down --volumes --remove-orphans >/dev/null || true
  exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
"${compose[@]}" config --quiet
"${compose[@]}" up -d --build --wait --wait-timeout 180
port() { "${compose[@]}" port "$1" "$2" | sed 's/.*://'; }
identity_port="$(port keycloak 8080)"
mail_port="$(port mailpit 8025)"
curl --fail --silent --max-time 5 "http://127.0.0.1:$identity_port/realms/meridian/.well-known/openid-configuration" | node -e '
let body="";process.stdin.on("data",c=>body+=c);process.stdin.on("end",()=>{
const doc=JSON.parse(body);if(doc.issuer!=="http://localhost:28080/realms/meridian"||!doc.code_challenge_methods_supported.includes("S256"))process.exit(1);
});'
curl --fail --silent --max-time 5 "http://127.0.0.1:$mail_port/readyz" >/dev/null
"${compose[@]}" exec -T postgres psql -U meridian -d meridian -v ON_ERROR_STOP=1 -c "CREATE TABLE smoke_persistence (value integer PRIMARY KEY); INSERT INTO smoke_persistence VALUES (42);" >/dev/null
# Recreate containers while retaining this test project's volumes.
"${compose[@]}" up -d --force-recreate --wait --wait-timeout 180
value="$("${compose[@]}" exec -T postgres psql -U meridian -d meridian -At -c 'SELECT value FROM smoke_persistence')"
[[ "$value" == 42 ]]
identity_port="$(port keycloak 8080)"
curl --fail --silent --max-time 5 "http://127.0.0.1:$identity_port/realms/meridian/.well-known/openid-configuration" >/dev/null
echo 'Fresh local dependencies, realm import, readiness and database persistence verified.'
