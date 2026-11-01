# Local backend monitoring

Start the app and optional Prometheus service:

```sh
./scripts/container-up.sh --profile observability
node scripts/check-monitoring.mjs
docker compose --env-file .env -f infrastructure/local/compose.yml --profile app --profile observability exec -T prometheus promtool check config /etc/prometheus/prometheus.yml
docker compose --env-file .env -f infrastructure/local/compose.yml --profile app --profile observability exec -T prometheus promtool test rules /etc/prometheus/alerts.test.yml
```

Open http://localhost:19090 for Prometheus queries, targets and alerts. This local UI is unauthenticated and bound only to loopback. Metrics storage is volume-backed with seven-day/512 MB retention limits; these are retention thresholds, not hard disk quotas. Stop with the same Compose command and both profiles followed by `down`; volumes remain. Starting without observability does not stop an already-running Prometheus container.

The backend exposes `/readyz` and `/livez` on application port 8081. Both return only aggregate status. Readiness includes database health; liveness does not depend on the database. Scripts and the container probe use readiness. Metrics are on separate management port 9091, which is not published by Docker. Host development binds management to 127.0.0.1; the local-container profile's environment allows Docker network access. Spring Security permits the selected management endpoints only on that local port, denies `/actuator/**` on the public application port even for signed-in members, and retains normal API authentication and CSRF. Management port 9091 is reserved and must remain distinct from server.port.

Micrometer uses Spring Boot's managed dependency version. Prometheus v3.15.1 scrapes every ten seconds. Collected series cover HTTP request count/latency, JVM, process and JDBC pool metrics; HTTP latency histograms are enabled. No custom user IDs, email addresses, tokens or request/message bodies are added as labels. This increment does not add distributed tracing, structured audit logs, frontend Web Vitals, or domain-event telemetry.

Useful PromQL queries:

```promql
up{job="meridian-backend"}
sum(rate(http_server_requests_seconds_count{job="meridian-backend"}[5m]))
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="meridian-backend"}[5m])))
sum(jvm_memory_used_bytes{job="meridian-backend"})
```

The latency query describes all observed HTTP requests, including probes. It is not evidence of a business-workflow SLO; filter route labels when measuring a particular API. Short local runs may have insufficient samples for rate estimates.

## Alerts and recovery

`BackendScrapeUnavailable` fires after one minute of failed scrapes. Check the Compose backend state, Prometheus target error, and container-network reachability. A failed scrape does not prove the business API is unavailable. Check `/livez` and `/readyz` independently. If readiness fails while liveness succeeds, check PostgreSQL connectivity and credentials before restarting anything. These probes do not automatically restart containers.

`BackendHighErrorRate` fires when HTTP 5xx responses exceed five percent over five minutes, sustained for five minutes, with at least twenty requests in the window. Inspect affected route/status metrics and local backend logs; avoid sharing logs containing sensitive identity URLs. Thresholds are proposed local defaults, not measured production SLOs.

Alert rules are evaluated and visible in Prometheus only. Alertmanager, paging/channel delivery and Grafana dashboards are not configured. Unit fixtures verify scrape-failure firing/recovery and sustained error-rate firing; these are simulated time series, not a live outage drill. The real scrape check verifies live JVM/HTTP ingestion and public metrics denial. Database outage/recovery drills, host-mode probe regression, telemetry authentication/TLS for cloud use and full service tracing remain pending.

Sources: [Spring Boot actuator endpoints](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html), [Prometheus release](https://github.com/prometheus/prometheus/releases/tag/v3.15.1). CI includes monitoring checks but has not been run remotely.

Checkpoint evidence (2026-11-01): container image builds and six-service startup passed. `node scripts/check-monitoring.mjs`, `promtool check config`, `promtool test rules` and `scripts/verify.sh` all passed. Verification included 25 PostgreSQL-backed test invocations, contract/type checks and all 3 real-service browser journeys. Existing volumes were retained.
