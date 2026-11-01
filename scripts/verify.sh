#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ "$(uname)" == Darwin ]]; then export JAVA_HOME="$(/usr/libexec/java_home -v 21)"; fi
mvn -q -f apps/backend/pom.xml test
(cd apps/web && node check-contract.mjs)
node -e "const s=require('./packages/contracts/events/connection-accepted.v1.schema.json'); if(s.properties.schemaVersion.const!==1 || s.properties.type.const!=='ConnectionAccepted' || s.additionalProperties!==false)process.exit(1)"
node -e "const s=require('./packages/contracts/events/application-changed.v1.schema.json'); if(s.properties.schemaVersion.const!==1 || s.properties.type.const!=='ApplicationChanged' || s.additionalProperties!==false || s.required.length!==5)process.exit(1)"
npm --prefix apps/web run typecheck
for attempt in {1..60}; do
  if curl --fail --silent --max-time 2 http://localhost:8081/readyz >/dev/null && curl --fail --silent --max-time 2 http://localhost:3100 >/dev/null; then break; fi
  if [[ "$attempt" == 60 ]]; then echo 'Start scripts/dev.sh before running browser checks.' >&2; exit 1; fi
  sleep 1
done
(cd apps/web && npx playwright test)
