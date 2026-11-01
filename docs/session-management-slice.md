# Persistent application sessions and Settings

## Scope and ownership

Keycloak remains authoritative for passwords, verification, MFA and identity-provider SSO. Spring Security authenticates the browser using OIDC Authorization Code with S256 PKCE. Spring Session JDBC now persists application sessions in PostgreSQL rather than Tomcat memory. Spring Boot 3.5.7 manages Spring Session 3.5.3; the implementation follows the official [Spring Session JDBC guidance](https://docs.spring.io/spring-session/reference/3.5/configuration/jdbc.html). No competing credential store was introduced.

V31 creates Spring Session's PostgreSQL tables and an application-session revocation audit table. The existing HttpOnly, SameSite=Lax JSESSIONID cookie name is retained; production requires HTTPS and Secure cookies. Existing pre-migration in-memory sessions require a fresh sign-in. Expired records use Spring Session's cleanup task. Session attributes contain authentication material and require restricted database access, encrypted storage/backups and a deployment-specific retention policy; these production controls remain outstanding.

## Behavior and guarantees

- `/settings` uses the S08 category/sidebar and grouped-card hierarchy with original styling, plus mobile and dark themes. The account dropdown opens it. Account preferences and visibility reuse the existing real APIs; notifications link to the existing inbox. Unimplemented categories are labeled unavailable.
- `GET /api/v1/me/sessions` returns the caller's newest 100 active application sessions, creation/activity/idle-expiry timestamps, and the current-session marker. Returned UUIDs are database management IDs, not cookie/session credentials. No IP address, browser fingerprint, tokens, or other-account sessions are returned.
- `DELETE /api/v1/me/sessions/{id}` revokes only a caller-owned session. Missing and foreign IDs have the same idempotent response. Deletion and the audit count commit in one transaction. Repeat requests do not duplicate audit effects.
- `DELETE /api/v1/me/sessions` atomically revokes currently persisted sessions for the caller, including current. A new login after that transaction remains possible. Requests already authorized before revocation may finish; subsequent requests must authenticate again. A stale repository save cannot recreate the deleted session.
- WebSocket checks consult the database before sending refresh hints and every five seconds, so a retained HttpSession wrapper cannot bypass remote revocation. Database-check failure closes the socket. REST remains authoritative for private content.
- Every OIDC sign-in now requests `prompt=login`, preventing silent recreation through a surviving identity-provider SSO cookie. This requires an interactive Keycloak authentication, including configured MFA. Keycloak SSO sessions, credentials, and tokens are not remotely revoked by the Meridian controls; the UI explains this boundary and links to provider session management.
- The security page refreshes on focus and every 15 seconds, has a bounded request deadline, clears displayed account/session information after 401, confirms sign-out, and retains explicit error/retry feedback. No automatic retry of revocation mutations is performed.

A 30-minute idle timeout is inherited from existing configuration. Activity can extend the idle timeout, but the new absolute lifetime below now bounds the session. No risk-scoring policy is claimed. Browser family/device labels, pagination beyond 100 sessions, session audit browsing and session-specific alerts remain pending.

## Traceability

| ID | Source | Frontend | Backend/contract/data | Evidence | Status |
|---|---|---|---|---|---|
| INT-AUTH-SESSION-001 | Explicit backend §7.11, frontend §9.3 | Where you are signed in list | SessionController, listMySessions, V31 | API ownership/redaction tests; two-browser journey | Integration verified |
| INT-AUTH-SESSION-002 | Explicit backend §7.11, frontend §9.3 | Individual and all-device Meridian sign-out | revokeMySession/revokeAllMySessions; transactional audit | Real browser revocation and API denial; duplicate/foreign ID tests | Integration verified |
| BE-AUTH-SESSION-003 | Recommended security implementation | Revoked session notice | Spring Session expiry and SessionAccess checks | Expired-session API test, retained-socket test, stale-save regression | Integration verified |
| FE-SETTINGS-SHELL-001 | Observed S07–S09; explicit frontend §9 | Category navigation, account/access/visibility content | Existing preference/privacy APIs | Persisted theme after navigation/reload; axe desktop/mobile-dark | Integration verified |
| BE-AUTH-009 | Explicit master requirement | Partial coverage above | Provider revocation implemented below; recovery/device policy remain | See remaining backlog | In progress |

## Verification

Local Docker production builds and health checks passed with V31. The full backend stage of `./scripts/verify.sh` passed 84 invocations (78 ProfileIntegrationTest, two Kafka integration tests, four realtime tests), generated-contract consistency and strict TypeScript. Initial regression work corrected a mock WebSocket fixture missing its persisted session and an inaccessible test-only repository type. The real-session browser test passed individual revocation, socket disconnection, credential reauthentication, all-device API denial, theme persistence, desktop light/mobile dark axe and overflow checks. Both screenshots were reviewed against S08.

The initial full browser run passed 20/21 and exposed an old search selector ambiguous after the public-profile checkbox addition. It was changed to target the exact member-sharing label. The final `npx playwright test` passed all 21 real-service journeys. No mocked identity, database, or successful session-revocation response is used in browser evidence. API tests inject OIDC principals and use real Testcontainers PostgreSQL. No AWS resources were created.


## Absolute lifetime and native credentials checkpoint

INT-AUTH-SESSION-004 adds a configurable absolute session lifetime (default 12 hours, `meridian.security.session-maximum`, supported range one minute to seven days). The deadline is measured from application-session creation and cannot be extended by HTTP polling. The filter invalidates expired authenticated sessions before authorization; `SessionAccess` applies the same creation-time boundary to WebSockets. Lists omit expired sessions and return the earlier of idle and absolute deadlines in `expiresAt`. Requests already in progress may finish; periodic socket checks run every five seconds. This does not revoke identity-provider sessions.

Unit tests cover filter denial/invalidation and policy bounds. A real PostgreSQL test gives a session a future idle expiry but an old creation time and verifies it disappears from the list and cannot authorize socket access. Existing real-browser session revocation tests remain regression coverage.

The security settings page now opens native passkey/recovery management. Credential ownership remains with Keycloak; the application never receives private keys or recovery-code values.

## Provider-initiated logout (V41)

**INT-AUTH-SESSION-005 — Integration verified.** From Settings → Sign in & security → identity provider sessions, members can use Keycloak's native Device activity page to end one other provider session or all provider sessions. Keycloak sends a signed logout token to Meridian. Linked application sessions are revoked, HTTP access requires a new login, and existing sockets close on their next authorization check (at most the five-second periodic interval under a healthy database). The settings explanation now reflects this integration. The Meridian-only revocation buttons retain their existing scope.

### Protocol contract and trust boundary

| Element | Implemented behavior |
|---|---|
| Contract ID | API-OIDC-LOGOUT-001 |
| Method/path | `POST /logout/connect/back-channel/keycloak` on the backend, not the browser BFF |
| Request | `application/x-www-form-urlencoded`, required `logout_token` signed JWT |
| Authentication | Spring Security validates signature using the configured Keycloak JWKS, issuer, client audience, required claims, logout event and absence of nonce; no browser cookie/CSRF token is required for this provider protocol |
| Additional freshness | Issued within the preceding five minutes, allowing at most 60 seconds of future clock skew; expiration is validated when present |
| Success | HTTP 200, no-store, after database transaction commits; matching sessions need not exist |
| Invalid request | HTTP 400 for invalid signature, audience, supported claim failures or freshness; dependency failures do not return a successful revocation |
| Idempotency | Unique `(issuer, client_id, jti)`; redelivery within the validity window does not reapply revocation or change the audit count |
| Scope | SID targets its provider session; when subject is also present it must match. Subject-only logout affects bindings whose ID token was issued at or before logout issuance |
| Persistence | V41 provider/session bindings and logout audit/tombstones, plus deletion of matching Spring Session rows in one PostgreSQL transaction |
| Evidence | `ProviderLogoutIntegrationTest`, `provider-logout.spec.ts`, full session/identity regression suite |

This implements the standard [OpenID Connect Back-Channel Logout protocol](https://openid.net/specs/openid-connect-backchannel-1_0.html) using [Spring Security 6.5.6's supported logout handler and registry extension points](https://github.com/spring-projects/spring-security/blob/6.5.6/config/src/main/java/org/springframework/security/config/annotation/web/configurers/oauth2/client/OidcLogoutConfigurer.java). Credential verification and cryptographic signature validation remain in the standards-based identity/security libraries.

```mermaid
sequenceDiagram
 participant Member
 participant Keycloak
 participant Security as Spring Security
 participant DB as PostgreSQL
 participant App as HTTP / live connection
 Member->>Keycloak: Device activity: sign out
 Keycloak->>Security: POST signed logout_token
 Security->>Security: Verify signature, issuer, audience, claims, freshness
 Security->>DB: Transaction: insert revocation; delete linked sessions
 DB-->>Security: Commit
 Security-->>Keycloak: 200
 App->>DB: Check current session and revocation
 DB-->>App: Revoked
 App-->>Member: Authentication required / disconnect
```

### Persistence, races and retention

`ProviderSessions` replaces the in-memory OIDC registry with minimal database mappings. It stores provider issuer/client/subject/SID, application session ID and ID-token issuance time; it never stores logout JWTs, ID tokens or CSRF tokens in these new tables. Existing Spring Session authentication attributes remain sensitive and retain the previously documented storage requirements.

Bindings deliberately have no foreign key to Spring Session: OIDC registers a session before the HTTP response commits it. Revocation tombstones therefore remain authoritative even if a login callback or in-flight request writes a session after the delete. The HTTP authorization filter and WebSocket session guard consult these records. Bindings survive local sign-out long enough to cover late writes. A new backend instance can make the same decision from PostgreSQL; no local registry state is required.

SID revocation applies to all bindings with that SID. Subject-only revocation uses token issuance time so later sign-ins remain valid; timestamps in the same second are conservatively treated as revoked. Binding/audit retention is eight days, exceeding the supported seven-day maximum application session lifetime. Hourly cleanup removes older records. This is bounded operational retention, not a finalized legal/audit retention policy.

### Local configuration and operational limitations

The local realm import and explicitly development-only identity migration configure the Meridian client's back-channel URL from `MERIDIAN_LOCAL_BACKCHANNEL_URL`, require SID, and do not enable offline-token revocation. Host development defaults to `http://host.docker.internal:8081/logout/connect/back-channel/keycloak`; container startup and the media Compose override select `http://backend:8081/logout/connect/back-channel/keycloak` so Linux CI does not depend on reaching host-loopback published ports. Compose supplies the host-gateway mapping. This route was verified on the current Docker Desktop host; other host networking environments require reachability validation. Existing realms receive the setting when the local identity container restarts; fresh imports contain it directly. The migration remains disabled outside the explicitly enabled local setup.

Production must configure a reachable HTTPS back-channel endpoint and the correct client registration, private database access, clock synchronization and provider delivery monitoring. If Keycloak cannot deliver the request or PostgreSQL cannot commit it, provider logout alone is not proof of application revocation. Delivery retry/reconciliation and recovery-triggered provider invalidation remain open. Application idle/absolute expiry still bounds access. Already-authorized in-flight business requests can finish. Cross-replica database authorization is designed into the store, but a multi-replica deployment/load test has not been performed.

For local investigation, inspect only aggregate counts in `provider_logout_event` (`received_at`, `revoked_count`) and service health; do not log tokens, session IDs or subjects. Reproduce the native device-logout journey after changing identity networking. Do not remove tombstones as a way to restore a revoked session; authenticate again.

### Verification checkpoint

Executed `./scripts/container-up.sh --profile observability` successfully: backend/web/identity production images built, all local health checks passed, and V41 applied. `./scripts/verify.sh` passed **113 backend tests, generated OpenAPI consistency, strict TypeScript and all 33 real-service Playwright journeys**.

The new backend tests use a real Testcontainers PostgreSQL database and a test-only JWKS server to exercise Spring's signed-token filter, invalid signature/issuer/audience/event/nonce/expiry/freshness/JTI cases, SID/subject boundaries, duplicate delivery, and a late session write after revocation. A separate filter assertion checks denial of an already-held revoked HTTP session. Browser tests use actual Keycloak registration/login and native Device activity controls: one-device logout, unrelated session survival, WebSocket closure, new sign-in and all-device logout. Email transport remains the existing explicit local Mailpit adapter. No successful identity/API response is mocked in this browser journey. No AWS deployment occurred.
