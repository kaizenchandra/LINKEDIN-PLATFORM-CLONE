#!/usr/bin/env bash
# Explicit local operator provisioning; no application self-service role grants.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# != 2 || ! "$2" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]; then
  echo 'Usage: scripts/local-moderator.sh grant|revoke MEMBER_PUBLIC_UUID' >&2
  exit 2
fi
case "$1" in
 grant)
  docker compose --env-file .env -f infrastructure/local/compose.yml exec -T postgres psql -X -v ON_ERROR_STOP=1 -U meridian -d meridian -v member_id="$2" <<'SQL'
insert into moderation_operator(subject) select subject from profile where public_id=:'member_id'::uuid on conflict do nothing returning granted_at;
SQL
 ;;
 revoke)
  docker compose --env-file .env -f infrastructure/local/compose.yml exec -T postgres psql -X -v ON_ERROR_STOP=1 -U meridian -d meridian -v member_id="$2" <<'SQL'
delete from moderation_operator where subject in (select subject from profile where public_id=:'member_id'::uuid);
SQL
 ;;
 *) echo 'Expected grant or revoke' >&2; exit 2;;
esac
