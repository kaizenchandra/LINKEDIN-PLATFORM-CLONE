# Local infrastructure

Run `./scripts/dev.sh` with Java 21, Maven, Node 24, Docker and Docker Compose v2 supporting `up --wait`. It creates `.env` from the local-only example if absent, waits for all dependency health checks, verifies realm discovery, installs locked web packages, starts Spring/Next and verifies their HTTP endpoints. Existing database volumes are retained. Stop the foreground script with Ctrl-C to stop the apps; dependency containers remain available.

| Service | Host endpoint | Readiness |
|---|---|---|
| PostgreSQL | localhost:15432 | pg_isready for meridian database/user |
| Keycloak | http://localhost:28080 | internal port 9000 /health/ready, then realm discovery from host |
| Mailpit | http://localhost:8025 | mailpit readyz |
| Backend | http://localhost:8081 | /readyz |
| Web | http://localhost:3100 | root HTTP response |

All published dependency ports bind to loopback. SMTP port 1025 and Keycloak's management port stay inside Docker. Keycloak waits for the email test service to be healthy. Compose health is a readiness signal, not an automatic restart/recovery policy. Realm import on startup does not overwrite an existing realm; editing realm.json does not migrate an existing identity database.

Inspect service state without printing resolved environment secrets:

```sh
docker compose --env-file .env -f infrastructure/local/compose.yml config --quiet
docker compose --env-file .env -f infrastructure/local/compose.yml ps
```

If startup times out, inspect the failing service's local logs, correct the underlying error and rerun the startup script. Do not publish logs containing identity URLs or test secrets. Check Docker availability, port collisions and disk space. Changing DB_PASSWORD does not change the password in an already initialized PostgreSQL volume. OIDC_SECRET must match the imported local realm client; production secret configuration is not implemented.

Stop dependencies without deleting stored profiles or identities:

```sh
docker compose --env-file .env -f infrastructure/local/compose.yml down
```

## Isolated infrastructure verification

`./scripts/test-local.sh` creates a uniquely named disposable Compose project with fresh volumes and dynamically assigned loopback ports. It waits for every health check, checks imported realm discovery and PKCE S256 support, checks Mailpit, writes a PostgreSQL marker, recreates the containers, and verifies persisted data and realm availability. An exit trap removes only that test project's containers, network and volumes. No developer reset is performed. The test is also configured in CI; remote CI execution remains unverified.

POSTGRES_PORT, KEYCLOAK_PORT and MAILPIT_PORT overrides exist for this isolated dependency test. They do not reconfigure the browser application: issuer, callback and frontend/backend URLs currently use fixed development ports. The fresh-volume smoke test is not proof of a full fresh-volume browser journey. `./scripts/verify.sh` separately runs real-service browser journeys against the normal development stack and backend tests against fresh Testcontainers PostgreSQL.

Keycloak health configuration follows its [official health guide](https://www.keycloak.org/observability/health). The fixed version container's Bash TCP probe avoids depending on curl inside the minimal image. Mailpit uses its image's readyz command. Keycloak retains its existing local H2 volume: migration to a dedicated production-style identity PostgreSQL database, Kafka/outbox, cache/search/media services, distributed tracing and AWS deployment remain backlog items. Add those dependencies alongside the features that use them.

## Executed evidence

The isolated smoke test passed on 2026-11-01, including fresh volumes, recreation, marker verification and cleanup. Normal `scripts/dev.sh` startup then passed with all three dependencies healthy. `scripts/verify.sh` passed: 24 PostgreSQL-backed test invocations, generated contract check, strict TypeScript and 3 real-service Playwright journeys. No application source changed in this checkpoint; a production web build was not repeated. No cloud deployment or remote CI run was performed.

## Containerized application mode

Run `./scripts/container-up.sh` with Docker Compose available. The optional `app` profile adds backend and web to the existing three dependencies; dependency-only commands and smoke tests leave it disabled. The script builds both images before waiting for all five services to be healthy. Backend uses a multi-stage Maven/Java 21 build and a non-root JRE runtime. Web uses locked npm dependencies, Next standalone output, a Node 24 runtime and the non-root node user. Neither image copies local .env files, node_modules or target output.

The `local-container` Spring profile sets an explicit OIDC registration: public issuer/auth/logout/callback URLs stay on localhost for browsers; token, signing-key and user-info requests use the internal Keycloak service. ID-token issuer verification, signature verification, PKCE and CSRF remain enabled. This profile is strictly local: it is not the AWS identity configuration. The backend connects to PostgreSQL by service name; the web image's build-time BACKEND_ORIGIN selects its internal API proxy destination. Rebuild the web image to change that destination. Public application URLs remain fixed to localhost.

```sh
./scripts/container-up.sh
./scripts/verify.sh
# Stop all containers, retaining identity and business-data volumes:
docker compose --env-file .env -f infrastructure/local/compose.yml --profile app down
```

To switch to host development, stop the container mode first and run `scripts/dev.sh`. To switch back, stop the host development script and run `scripts/container-up.sh`. Never run both application modes on the same ports. Rebuild images after source changes in container mode. Base image tags currently float within major runtime versions; release digest pinning, SBOM/scanning, image publishing and AWS configuration remain pending. Maven's image-build packaging step skips tests; `scripts/verify.sh` and CI run them separately against real PostgreSQL and the running application.

CI now invokes the container startup script before the real-service tests. This workflow configuration has not been executed remotely.

Container checkpoint evidence (2026-11-01): `scripts/container-up.sh` successfully built both images and brought all five services to healthy status. `scripts/verify.sh` then passed 24 PostgreSQL-backed test invocations, the generated-contract check, strict TypeScript and all 3 real-service Playwright journeys against the containerized backend and standalone frontend. The isolated `scripts/test-local.sh` also passed again with the optional app profile disabled. Existing developer identity and profile volumes were preserved. This is verified local container deployment, not production deployment.


Optional local metrics: `./scripts/container-up.sh --profile observability` starts Prometheus at localhost:19090. See [monitoring runbook](monitoring.md). Backend application health checks now use `/readyz`; detailed metrics moved to the unpublished management port 9091.
