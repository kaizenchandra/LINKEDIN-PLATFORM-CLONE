# Meridian

Incremental full-stack implementation; **not yet a complete platform or production-ready release**.

## Architecture migration

The backend now has framework-independent domain/application layers, input and persistence ports, explicit transaction boundaries, HTTP/JDBC adapters and a Spring composition root for search history, account preferences, profile visibility, profile initialization/intro updates, six editable profile sections network invitations/follows/blocks/discovery/privacy, posts/feed/drafts/comments/replies, identity session/revocation workflows, reporting/moderation, notification management, people discovery, job preferences, organization management, job lifecycle/search/bookmark workflows, recruiter delegation, job question management, application submission/read/status workflows, notification projections, outbox publication, event consumption, messaging reads/mutations, photo object-storage boundaries, export lifecycle/queue/generation and realtime adapter boundaries. Existing API paths and schemas are preserved. Other domains remain on the existing implementation and are tracked for migration. The rebuilt backend passed **224 backend tests and all 34 real-service browser journeys** (two browser workers, no retries), API contract consistency and strict TypeScript. See [implemented architecture and migration sequence](docs/architecture/clean-hexagonal.md).

## Latest functional checkpoint

Implemented opt-in private search history across Search and Settings → Data privacy, backed by PostgreSQL and generated API contracts. Members can reuse searches, remove entries, clear history, or disable saving. Owner authorization, CSRF, consent revisions, a 20-entry limit, 30-day retention and data-export integration are enforced by the backend. See [behavior and verification](docs/search-history-slice.md).

Verified **120 backend tests**, generated OpenAPI consistency, strict TypeScript, and **all 34 standard Playwright journeys** against the real local application. Container builds/readiness and all **43 migrations** passed. The new browser journey also checks mobile dark-mode accessibility and overflow. Hosted CI was not executed here.

Run `./scripts/localstack-up.sh --profile observability` for the integrated local application at http://localhost:3100. Terraform provisions private media storage. The previous storage checkpoint additionally verified one operational browser recovery test and three offline Terraform tests; these infrastructure tests were not rerun for this search-history change. The shared AWS media root has **not** been applied to AWS and does not constitute a full platform deployment.

LocalStack 4.10.0 did not retain S3 state on restart. Local mode retains normalized photo bytes in PostgreSQL and restores missing objects after Terraform recreates the bucket. Production S3 mode defaults to no database copy. See [storage behavior and limitations](docs/localstack-media.md).

The full specification remains **incomplete**. Mobile OTP/linking, account deletion, reposting, broader media/advanced domains and complete production infrastructure remain in the [prioritized backlog](docs/remaining-backlog.md). No real AWS resources, SMS billing, public deployment or production-readiness claim is made.

## Latest UI checkpoint

The existing product pages now share screenshot-aligned navigation, card density, columns, filters and forms, with responsive light/dark/system styling. Feed, network, messaging, notifications, jobs, search, companies, applications, recruiter tools and settings retain their real APIs. See [reference coverage and verification](docs/ui-reference-map.md) for scope, 30 cross-page captures and remaining visual/product gaps. This is not a claim that every LinkedIn feature is implemented.

## Latest implementation status

The app includes real Keycloak email authentication/verification/recovery/TOTP; persistent profiles and photos; public/member visibility; connections/following/blocking; text feed; direct messaging; companies/jobs/recruiting/applications; and Kafka-backed connection/application notifications. The settings/security slice provides persistent, owner-managed **Meridian sessions**, including remote/all-device revocation, absolute expiry and WebSocket invalidation.

Use **Me → Settings & privacy → Sign in & security** to manage sessions. Account appearance and visibility are available in the same settings shell. Keycloak owns credentials and provider sessions. Provider device logout now revokes linked Meridian sessions; the Meridian-only revocation buttons do not revoke provider tokens.

See [current prioritized backlog](docs/remaining-backlog.md) and [session architecture, traceability and verification](docs/session-management-slice.md). The checkpoint entries below are historical; their older pending lists and test counts are not the current state.

## Run locally

Docker-only startup: run `./scripts/container-up.sh`. It builds the backend and frontend images, starts all five services, applies Flyway migrations, and waits for health checks. No host Java/Node is needed to run the application; host tooling is still used by the verification script. Open http://localhost:3100. Stop this mode with `docker compose --env-file .env -f infrastructure/local/compose.yml --profile app down` (volumes retained).

The host development alternative below supports editing without image rebuilds. Stop containerized web/backend before switching modes because both use the same ports.

Run `./scripts/dev.sh` from the repository root (Java 21, Maven, Node 24 and Docker required). This creates a local `.env` if absent, starts dependencies, waits for PostgreSQL/Keycloak/Mailpit health and realm discovery, installs locked web dependencies, starts both apps and checks health. Leave the script running. Dependency volumes are retained. The dependency-only fresh-volume smoke test also verifies container recreation and database persistence; full browser journeys use the normal development stack.

Manual alternative: copy `.env.example` to `.env`, then export its values in your shell.

1. `docker compose --env-file .env -f infrastructure/local/compose.yml up -d`
2. `cd apps/backend && mvn spring-boot:run`
3. In another shell: `cd apps/web && npm ci && npm run dev`
4. Open http://localhost:3100. Register through Keycloak; inspect verification mail at http://localhost:8025.

Local-only credentials and HTTP configuration must never be used in production. Keycloak owns credentials; Spring owns the HttpOnly session and CSRF validation. Next proxies same-origin profile APIs. Profile writes use PostgreSQL compare-and-swap version checks. Only the authenticated subject can access its profile. No mock identity or profile store exists.

## Current scope

Implemented source: OIDC login integration, self-profile initialization/read/update, validation, version conflicts, responsive profile form, local service definitions, Flyway migration. Email registration → Mailpit verification → Keycloak sign-in → profile save → reload passed a real-service Playwright test. Theme preference is synchronized to the account when signed in; browser-local storage provides the initial/anonymous preference.

## Remaining work, in priority order

1. Implement and verify MFA recovery codes, passkeys, session expiry and all-device revocation.
2. Mobile identifier/OTP Keycloak extension, recovery, MFA/passkeys, identifier linking, session management and negative authentication tests.
3. Complete profile sections, privacy, persisted theme and media.
4. Network state machine; content/feed; transactional outbox/Kafka and consumers.
5. Durable messaging/WebSockets, notifications, jobs/applications, companies, search, analytics/moderation.
6. Contract generation, automated integration/security/accessibility/visual tests, observability, CI/CD, AWS IaC, recovery and performance evidence.

`docs/requirements.json` preserves source-line requirement inventory without dropping source material; consolidation, semantic deduplication and operation-level traceability remain pending. Original prompts remain authoritative. The 18 screenshots are now available in `snapshot/`; see `docs/ui-reference-map.md` for analyzed references and current implementation coverage. No AWS resources were created.

## Verification checkpoint

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -q -f apps/backend/pom.xml test`: 4 tests passed against Testcontainers PostgreSQL. OIDC principals are simulated in these API tests.
- `cd apps/web && npx playwright test`: 2 real-service browser tests passed (registration, verification email through Mailpit, PKCE S256 callback, profile save/reload, theme persistence, app/Keycloak logout, unauthorized API rejection, wrong-password rejection and successful password login).
- `npm run typecheck` and `npm run build`: passed with strict TypeScript.
- `npm install` audit reported no vulnerabilities for the installed web dependencies; this is not a full security assessment.

Local ports: web 3100, backend 8081, Keycloak 28080, PostgreSQL 15432, Mailpit 8025. Wait for Keycloak discovery at `/realms/meridian/.well-known/openid-configuration` before starting Spring. The local Keycloak development database is volume-backed H2, not the production identity database design. No automatic reset is performed.

Contract: `packages/contracts/profile.openapi.yaml`. The web Profile type is generated from this contract. A regeneration check detects stale generated types; server response-schema validation and backward-compatibility checks remain pending. No claims of mobile OTP, passkey, production security, visual fidelity, or AWS verification are made.

### Authentication hardening

Authorization requests now use S256 PKCE. Logout is a CSRF-protected form POST that invalidates the Spring session and redirects through Keycloak's end-session endpoint. The identity provider displays its signed-out page; return to the app at port 3100 to sign in again. This does not implement all-device revocation or back-channel logout.

Saved-request caching is disabled: anonymous API responses must not create session cookies that race the OIDC authorization request. API tests enforce the absence of JSESSIONID on anonymous profile responses. Authentication failures log only a standardized error code, not tokens or credentials.

### Recovery and developer workflow checkpoint

Password recovery is owned by Keycloak. Browser coverage now includes reset-email delivery through Mailpit, successful password replacement, rejection of the previous password, login with the replacement password, reset-link reuse rejection, and the same generic response for known/unknown accounts. Expiry by elapsed time, notification policy and cross-device session invalidation still need verification.

`./scripts/verify.sh` runs PostgreSQL-backed API tests, strict TypeScript checks and real-service Playwright tests against the running development stack. `.github/workflows/verify.yml` configures these checks for GitHub Actions; remote CI has not run. This is an initial verification pipeline, not the complete security/deployment pipeline required by the specification.

The account-security link opens Keycloak's credential-management UI. TOTP enrollment and MFA login have been verified. Passkey and recovery-code workflows remain unverified.

### MFA and contract checkpoint

From Account security, choose Set up Authenticator application, scan the QR code with an authenticator, enter its code, and name the device. The next login requires password plus TOTP. Keycloak stores and validates the authenticator credential; Meridian does not maintain a duplicate credential system.

The real-service Playwright journey now verifies enrollment, invalid-code rejection, and successful login using a fresh TOTP after logout. The test waits for the next time window so it does not reuse the enrollment code. `otpauth` is a test-only dependency. Identity traces are disabled; generated test artifacts are ignored and must not be shared because failure snapshots may include sensitive test enrollment data.

`npm run contracts:generate` in `apps/web` regenerates `app/api-schema.d.ts`. `node check-contract.mjs` rejects stale output and runs in `scripts/verify.sh`. The profile form consumes the generated schema type. A minimatch override to 11.0.1 or later resolves the generator's transitive moderate advisory; generation/build passed and npm audit reported zero findings after installation.

Verified this checkpoint: 2 Playwright tests (including the expanded MFA journey), contract generation, strict typecheck and production web build. Backend source was unchanged; the previous 4 passing backend tests remain the latest backend evidence.

### Experience slice checkpoint

The profile now includes persistent experience entries: title, organization, start/end dates, description and current-role support. Add/edit/delete use the real backend and generated OpenAPI types. The UI includes pending/error states, cancellation confirmation and an unload warning for open drafts. Backend ownership, date rules and version checks are enforced independently of the UI.

`docs/profile-slice.md` records operation-level status and remaining profile requirements. Evidence: 5 PostgreSQL-backed API tests, 2 real-service Playwright tests including experience CRUD/reloads, contract regeneration check, strict typecheck and web build. No Kafka publication/search projection is implemented for these mutations yet.

### Education slice checkpoint

Education now persists institution, degree, field of study, dates and optional details. Generated types connect the UI to owner-authorized APIs and a versioned PostgreSQL table. Browser tests cover add/edit/cancel/delete and reload persistence. Both experience and education share parameterized API invariant tests while retaining separate schemas and endpoint contracts.

Latest verification: `./scripts/verify.sh` passed with 6 PostgreSQL-backed test invocations, contract regeneration check, strict TypeScript and 2 real-service browser tests. The browser journey still exercises registration, recovery, MFA and experience, so this addition preserves those integrated flows. `npm run build` also passed. Profile completeness, visual fidelity, public visibility and downstream projections remain unfinished.

### Certifications slice checkpoint

Licenses and certifications now have persisted CRUD for name, issuer, credential ID, optional HTTPS URL, issue date and optional expiry date. The frontend uses generated contract types, and the backend enforces ownership, date rules and version checks. External links use noopener/noreferrer; URLs with unsafe schemes or embedded credentials are rejected server-side.

Latest evidence: `./scripts/verify.sh` passed (13 PostgreSQL-backed test invocations, contract regeneration, TypeScript, and 2 real-service browser tests covering certification CRUD/reloads alongside the prior identity/profile flows). The production web build passed. Remaining platform and identity requirements are not completed by this increment.

### Projects and shared editor checkpoint

Projects support title, description, dates, optional organization and comma-separated associated skill names. CRUD persists through owner-authorized versioned APIs to PostgreSQL. Profile section editors share transport/draft handling, including an explicit state for successful writes followed by failed refreshes.

Latest evidence: 14 PostgreSQL-backed test invocations; 2 real-service browser tests including project CRUD/reloads and deliberate post-save read failure; contract regeneration and strict TypeScript; production web build. The failure test intercepts only the refresh GET; writes and the recovery read use the real backend. Normal integration tests do not mock the backend.

### Skills slice checkpoint

Profile skills now support persistent add, rename and remove. The backend trims/collapses whitespace, compares names case-insensitively, enforces per-owner uniqueness in PostgreSQL, and checks versions on writes/deletes. Browser controls use generated contract types and the shared editor hook.

Latest evidence: 16 PostgreSQL-backed test invocations, including an eight-writer duplicate race with exactly one successful insert; contract regeneration and strict TypeScript; 2 real-service browser tests; production web build. The verification script now waits for application health before beginning browser tests. Endorsements and taxonomy integration are still unimplemented.

### Languages slice checkpoint

Languages support persistent names and five proficiency levels, with add/edit/remove controls, duplicate prevention, subject ownership and version checks. Browser tests cover persistence, duplicate feedback, changing proficiency and deletion across reloads. The backend rejects unknown and absent proficiency values.

Latest verification: `./scripts/verify.sh` passed with 19 PostgreSQL-backed test invocations, contract regeneration, TypeScript and 2 real-service browser tests that also recheck previous identity/profile slices. The production web build passed. This does not implement interface/content-language preferences or public-profile visibility.

### Account appearance checkpoint

Theme selection now persists to `account_preferences` with a subject-owned, versioned API and V8 migration. Signed-in browsers retrieve the authoritative preference; anonymous use remains browser-local. Light/dark/system preferences are cached locally for initial paint. Failed saves restore the previous display preference; stale writes return 409 and the UI offers reload. Changes in other sessions are retrieved on reload, not pushed in real time.

The real-service browser journey now opens a separate browser context, signs into the same account, verifies the saved theme, changes it, and confirms the first session cannot overwrite it with a stale version. It then reloads and saves successfully. Backend tests cover persistence, defaults, invalid themes, CSRF, anonymous access, per-account isolation and version conflicts.

Latest verification: 20 PostgreSQL-backed test invocations, 2 real-service browser tests, contract generation check, TypeScript and production web build. No visibility, notification-channel, advertising, consent or account-deletion controls are implemented by this appearance slice.

### Member-summary visibility checkpoint

Profiles remain private by default. A versioned opt-in setting allows signed-in members with the profile link to read only name, headline, location and about. The backend checks visibility for every read and uses no-store responses. Private and missing profiles share a 404 response; anonymous access is rejected. Public-facing identifiers are random UUIDs distinct from Keycloak subjects.

A separate member-profile route and a two-account Playwright test verify denial, opt-in sharing, persistence, and denied access after sharing is disabled. Existing profile/identity browser tests also passed. Latest evidence: 21 PostgreSQL-backed test invocations, all 3 browser tests passed across the regression run and targeted rerun, contract check, TypeScript and production build. Earlier browser failures were test-selector/address-normalization issues and were corrected.

Revocation protects subsequent reads; it cannot retract previously delivered information. Blocking rules, discovery, per-section privacy and anonymous public profiles remain unimplemented. See `docs/settings-slice.md` for scope.

### Network slice checkpoint

Members can send invitations from a shared member summary, accept/ignore incoming requests, withdraw outgoing requests and remove accepted connections. My network shows pending relationships and an authoritative connection count. PostgreSQL enforces no self-requests and one active relationship per unordered pair; transitions check both participant authority and version.

Latest verification: 22 PostgreSQL-backed test invocations; all 3 browser tests, with the network test rerun after adding ignore/withdraw coverage; generated-contract check, TypeScript and production build. Two real registered accounts exercised request → acceptance → counts → removal, then withdrawal and ignoring. Notifications, blocking, follows, mutual connections, rate limits and Kafka publication are still pending. See `docs/network-slice.md`.

### Member blocking checkpoint

Members can block from a shared profile and manage their own block list. Blocking transactionally removes active invitations/connections and prevents profile-summary access and new invitations in both directions. Unblocking does not restore old relationships. PostgreSQL constraints and pair-scoped transaction locks protect concurrent invitation/block changes; dedicated race stress testing remains pending.

Latest verification: `bash scripts/verify.sh` passed with 24 PostgreSQL-backed test invocations, contract regeneration check, TypeScript and all 3 real-service browser tests. `npm run build` passed. No AWS deployment occurred. Remaining network/platform work is tracked in `docs/network-slice.md`; earlier checkpoint limitations above describe their historical state.

### Local readiness checkpoint

Compose now checks PostgreSQL, Keycloak and Mailpit health. Startup uses bounded `up --wait`, followed by realm discovery and application checks. `scripts/test-local.sh` verifies fresh dependency volumes and persistence after recreation in an isolated disposable project; it preserves developer volumes and is wired into CI. See [local development runbook](docs/runbooks/local-development.md) for commands, failure handling and scope limitations.

### Full local container checkpoint

`./scripts/container-up.sh` now builds and starts the frontend, backend and three dependencies with readiness ordering. Both app runtimes are non-root. Browser-facing OIDC URLs stay on localhost; the local-container profile uses private Docker addresses for server-to-server identity calls while retaining issuer validation. CI is configured to exercise this mode.

Verified: both container builds, all five healthy services, 24 backend test invocations, contract check, TypeScript, 3 real-service browser journeys against the containerized app, and the isolated dependency smoke test. Remote CI, image scanning/publishing and AWS deployment remain unverified. The broader functional platform backlog remains open.

### Monitoring checkpoint

`./scripts/container-up.sh --profile observability` adds local Prometheus at http://localhost:19090. Backend HTTP/JVM/process/pool metrics use an unpublished management port; the public port exposes minimal `/readyz` and `/livez` probes. Readiness includes PostgreSQL. Monitoring config, two alert rules, rule fixtures and live-ingestion checks are included in CI.

Verified locally: app image builds; real Prometheus ingestion; public metrics denial; Prometheus config/rule validation and firing/recovery fixtures; 25 backend test invocations; contract check; TypeScript; all 3 real-service browser journeys. See [monitoring runbook](docs/runbooks/monitoring.md). Alert delivery, distributed traces, domain telemetry, live outage drills and production SLO validation remain pending. No remote CI or AWS deployment is claimed.

### Screenshot-driven UI checkpoint

The profile now follows the supplied references' navigation, hero/avatar proportions, card hierarchy, right sidebar and blue action styling with original Meridian branding. Profile editing uses a keyboard-accessible dialog; account controls use a dropdown. Existing real APIs, persistence, theme synchronization and authorization are retained. Mobile and dark layouts were visually reviewed, and automated axe checks passed for the tested light/dark/editor states. Unsupported navigation destinations are explicitly marked Soon.

See [reference coverage](docs/ui-reference-map.md). This is the implemented profile/network/settings-controls redesign; it does not claim all 18 reference pages are complete.

### Persistent text feed checkpoint

Home now opens `/feed`, with real text-post creation/edit/delete, explicit Only me/Members visibility, recent-first pagination and server-side blocking/ownership checks. Posts persist in PostgreSQL; repeated matching creates reuse one stored record, edits compare versions and deleted IDs cannot be resurrected. The composer retains text after a failed save. The UI uses the shared screenshot-inspired responsive layout.

Verified: container builds, 28 backend test invocations, contract/type checks, all five real-service browser journeys across the regression and targeted rerun, feed axe check and desktop/mobile visual review. See [feed slice and backlog](docs/feed-slice.md). Reactions, comments, media, moderation and reliable event publication remain unimplemented; this is not the complete content module.

### Persistent reactions checkpoint

Feed posts now support Like/unlike with backend-authoritative counts and a per-member selected state. PostgreSQL enforces one Like per member/post; repeated state-setting is idempotent. Visibility, blocking, deletion and ownership are checked server-side. The UI waits for confirmation and clearly reports uncertain outcomes after transport failures.

Verified: container builds, 30 backend test invocations, contract/type checks and all five real-service browser journeys, including reaction persistence, two-account counts, unlike, duplicate concurrency and failure handling. Existing accessibility checks passed. Other reaction types, comments, media, moderation and asynchronous events remain pending; see `docs/feed-slice.md`.

### Persistent comments checkpoint

Feed posts now include expandable top-level comments with a composer, refresh, pagination and author-owned deletion. Comments inherit post visibility, and blocked commenters are filtered server-side. Creation is idempotent across retries; confirmed mutations update the UI, while failed saves retain the draft.

Verified: container builds, 32 backend test invocations, contract/type checks and all five real-service browser journeys, including two-account comment persistence/deletion, authorization, concurrent retries and failure recovery. Accessibility checks with comments expanded passed. Threaded replies, editing, moderation, notifications and event publication remain pending; see `docs/feed-slice.md`.

### Saved posts checkpoint

Feed cards now have persistent Save/Unsave controls and a Saved posts view. Bookmarks are private to the caller and do not bypass current post visibility, blocks or deletion. Duplicate saves are idempotent, pagination uses the original post order, and the UI changes state after backend confirmation.

Verified: container builds, 34 backend test invocations, generated-contract/type checks and all five browser journeys, including failed bookmark saves, reload persistence, private ownership, blocking and deletion. Existing accessibility checks passed. Collections, offline reading, notifications and event publication remain pending; see `docs/feed-slice.md`.

### Event-driven connection notifications checkpoint

Accepting an invitation now commits an outbox event with the connection transition. A Kafka worker publishes and projects it into the sender's persistent notification inbox. The inbox at [/notifications](http://localhost:3100/notifications) supports unread counts, read marking, pagination, explicit refresh and versioned connection-notification preferences. Recipient authorization and current connection/block state are enforced server-side. Shared navigation now highlights the actual page.

Delivery is at least once with transactional consumer deduplication, bounded publish timeouts, delayed retry and invalid-event quarantine. Kafka is included in the local app/events profiles. See [event architecture, traceability and recovery runbook](docs/runbooks/events.md).

Verification: container builds and healthy startup via `./scripts/container-up.sh --profile observability`; 35 backend test invocations passed through `./scripts/verify.sh`, including real Kafka/PostgreSQL replay, quarantine and failed-publish recovery; generated OpenAPI/type checks passed. The browser suite initially exposed an asynchronous checkbox assertion and accessibility issues, which were corrected. Final `npm run typecheck && npx playwright test` passed all five journeys; after the navigation correction, `npx playwright test tests/visibility.spec.ts tests/design.spec.ts` passed both affected journeys. Desktop/mobile notification axe checks and visual review passed. `node scripts/check-monitoring.mjs` passed.

This is the connection-acceptance notification slice, not the complete notification platform. Live push, other event categories, email/SMS channels, production Kafka security/replication, event cleanup/rebuild tooling, distributed tracing and event-specific alerts remain open. Mobile OTP, messaging, jobs, search, remaining profile/settings capabilities and AWS deployment also remain in the broader backlog. No production deployment or readiness claim is made.

### Persistent following checkpoint

Member profiles now provide Follow/Unfollow, independent of connections. The account profile includes a private, paginated Following list with a backend count and removal controls. Private target profiles are masked, repeated follow writes are idempotent, and blocking atomically removes both directed follows without restoring them on unblock.

Verified: `./scripts/container-up.sh --profile observability` built and started the stack with V17; `./scripts/verify.sh` passed 38 backend invocations, contract generation consistency, TypeScript and five existing browser journeys. The new browser journey initially navigated before block completion; after correcting that wait, `npx playwright test tests/following.spec.ts` passed. It covers real persistence, privacy changes, failed-request feedback, block/unblock and member-page accessibility. See [network traceability and limitations](docs/network-slice.md).

Following does not yet drive feed personalization or notifications. The broader platform backlog and production deployment remain incomplete.

### Following feed checkpoint

The feed now offers Following posts alongside Recent and Saved. Its real API filters by current follow relationships while retaining post audience, deletion and blocking enforcement. Pagination and view switching reset correctly; mobile controls wrap. Publishing from Following returns to Recent so the confirmed own post is visible.

Verified: container builds/startup, 40 backend test invocations, generated OpenAPI consistency and strict TypeScript. `./scripts/verify.sh` passed the backend/contracts/types and five existing browser journeys; the expanded Following journey passed with `npx playwright test tests/following.spec.ts` after correcting its wait for a completed privacy change. Backend tests cover 21-post pagination, invalid scope/cursor, unfollow, blocks, deletion and private post suppression. The real two-account browser journey verifies composer → follow → filtered feed → unfollow → empty state, and axe checks pass.

This is chronological filtering, not ranked recommendations or asynchronous feed materialization. View choice resets on reload and cross-session changes require refresh. No AWS deployment or production performance claim is made. See [feed requirements and policy](docs/feed-slice.md).

### Opt-in people discovery checkpoint

The shared header now searches people by name/headline, with a location filter and paginated results at `/search`. Results connect to the real profile, Follow and Connect workflows. The prior profile-section finder remains in the profile sidebar.

Discovery is a separate, default-off preference: members must enable both shared-profile visibility and “Appear in people search.” Server-side reads exclude self and blocked members, return only summary fields, and honor privacy changes directly from PostgreSQL.

Verified with `./scripts/container-up.sh --profile observability` and `./scripts/verify.sh`: healthy container builds/startup, 42 backend test invocations, contract/type checks and all seven real-service browser journeys. Desktop/mobile-dark visual review, axe and mobile overflow checks passed. See [search architecture, traceability and backlog](docs/search-slice.md).

This is literal people search, not relevance-ranked or multi-domain search. OpenSearch, search rate limits, company/job/content search and production performance/deployment remain pending.

### Durable direct messaging checkpoint

Accepted connections can now start a persistent direct conversation from a member profile. Messaging includes ordered text history, idempotent sends, unread counts, read receipts, earlier-history pagination, draft retention after failed sends and automatic reconnect recovery. Blocking denies conversation reads/sends; removing a connection retains participant history but disables sends. Connection transitions and sends share a pair lock.

Delivery currently uses REST polling every three seconds with failure backoff, not WebSockets. Private message bodies stay in PostgreSQL and are not sent to Kafka. See [messaging contracts, guarantees and backlog](docs/messaging-slice.md).

Verified: `./scripts/container-up.sh --profile observability` and the final `./scripts/verify.sh` passed: healthy builds/startup, 45 backend test invocations, generated-contract/type checks and all eight real-service browser journeys. The messaging journey proves two-account delivery/read receipts, retry after a committed response is lost, offline recovery without duplication, reload persistence and block suppression. Desktop/mobile visual review, axe and overflow checks passed.

WebSocket delivery, attachments, group chats, archive/star/mute, rate limits, message retention/deletion, messaging events and production capacity validation remain pending. No AWS deployment or overall production-readiness claim is made.

### Messaging recovery hardening checkpoint

Messaging HTTP requests now have ten-second deadlines covering headers and response bodies. Changing conversations or leaving the page cancels the active refresh; stale responses cannot replace the selected thread. A timed-out send keeps its draft and idempotency UUID for an explicit retry. A rejected session clears private history, conversation metadata and the unsent draft, stops polling, and offers sign-in.

Verified: the web container rebuilt and became healthy; `./scripts/verify.sh` passed 45 backend test invocations, generated-contract/type checks and all nine real-service browser journeys. The expanded send test proves timeout after a real committed write followed by duplicate-free retry. The new recovery journey checks delayed-response switching, history timeout/recovery and actual backend logout/session rejection. Only explicit failure cases delay transport; successful data still comes from real local services.

See [messaging guarantees and remaining work](docs/messaging-slice.md). WebSocket delivery, server-side dependency deadlines, rate limits and production deployment remain pending.

### Conversation organization checkpoint

Messaging now supports private Star/Unstar and Archive/Restore controls, with Inbox, Starred and Archived views. Preferences persist separately for each participant and use optimistic versions. A new message restores both inboxes without changing stars or read state; replaying an already-stored send does not undo an archive.

Verified: `./scripts/container-up.sh --profile observability` built and started the applications with V20; `./scripts/verify.sh` passed 47 backend invocations, generated-contract/type checks and all nine real-service browser journeys. Tests cover participant isolation, concurrent preference writes, stale versions, blocking, retry semantics, persistence, filtered views and reply-driven inbox restoration. Messaging axe/mobile overflow checks passed; the updated mobile layout was visually reviewed.

See [messaging contracts and guarantees](docs/messaging-slice.md). Conversation search, mute, WebSocket delivery, attachments and production deployment remain pending.

### Conversation search checkpoint

Messaging now searches existing conversations by counterpart name, with server-side literal case-insensitive matching across Inbox, Starred and Archived views. Search resets pagination and cancels stale refreshes without discarding the selected thread or unsent draft. Private message bodies are not searched.

Verified: `./scripts/container-up.sh --profile observability` and `./scripts/verify.sh` passed: healthy application builds/startup, 48 backend invocations, generated-contract/type checks and all nine real-service browser journeys. Tests cover 21-conversation pagination, participant/block isolation, special-character matching, query bounds, empty/clear states and draft preservation. Messaging axe/mobile overflow checks passed; the updated mobile layout was reviewed.

See [messaging scope and guarantees](docs/messaging-slice.md). WebSockets, mute, attachments, message-content search and production deployment remain pending.


### Live messaging refresh checkpoint

Messaging now receives authenticated, content-free WebSocket signals after committed changes, then fetches authorized persisted state through REST. Reconnect uses bounded exponential delay with jitter; polling recovers missed signals. Handshakes enforce the configured origin and existing OIDC session, and the server rechecks active sessions. Private conversation preferences notify only their owner.

Verified: local container build/healthy startup succeeded; `./scripts/verify.sh` passed 51 backend test invocations, contract/type checks and all nine real-service browser journeys. An additional targeted transaction test passed for commit delivery versus rollback suppression (52 distinct backend cases checked). Browser coverage includes authenticated signals, anonymous/foreign-origin rejection, reconnect and session cleanup. Updated mobile messaging layout was reviewed; axe and overflow checks passed.

See [protocol, configuration and limits](docs/messaging-slice.md#authenticated-live-refresh-signals). This implementation uses one backend instance; multi-replica delivery, explicit delivery acknowledgments, mute/attachments, mobile registration, remaining product domains and production deployment remain pending.


### Live notification synchronization checkpoint

Connection notifications now appear automatically after Kafka projection commits. Read state and connection-notification preferences synchronize across tabs through authenticated WebSocket hints and authoritative REST reads. Notification requests have ten-second deadlines, polling/backoff recovery, stale-response cancellation and revoked-session cleanup. Older/Newest navigation retains the selected cursor during live updates. Messaging and notifications share the reconnect client.

Verified: `./scripts/container-up.sh --profile observability` and `./scripts/verify.sh` succeeded: healthy builds/startup, 53 backend test invocations, generated-contract/type checks, and all ten real-service browser journeys. Checks cover projection audience, replay/preference/block suppression, automatic arrival, cross-tab read/preferences, removed connections, timeouts, offline/reconnect and actual session revocation. Notification axe/mobile overflow checks passed; the updated light mobile capture was reviewed. Existing messaging regressions passed.

See [notification implementation and remaining scope](docs/notifications-slice.md). Notification categories beyond connection acceptance, multi-instance socket delivery, mobile registration, remaining product domains and AWS deployment remain pending.


### Company pages checkpoint

Companies now supports real account-owned creation and editing, literal name/industry search, managed/followed directory views, and persistent follow/unfollow. The account menu and people-search sidebar link to the directory. Page ownership is derived from OIDC; server-side ownership checks, optimistic versions, idempotent creation retries and unique follows enforce the domain rules. V21 stores company pages and follows in PostgreSQL. Pages explicitly identify themselves as member-created and unverified.

Verified: `./scripts/container-up.sh --profile observability` built and started the applications with V21. The final `./scripts/verify.sh` passed 57 backend invocations, generated-contract/type checks and all eleven real-service browser journeys. Company checks cover persistence, unsaved-change cancellation, a real concurrent-edit conflict, unauthorized edits, duplicate operations, pagination/search, follow isolation and cross-account views. The initial pre-hydration editing bug was fixed; corrected browser tests passed. Light desktop and dark mobile company captures were reviewed; axe and mobile overflow checks passed.

See [organization contracts, data model and backlog](docs/organizations-slice.md). Delegated administrators, membership/verification, company media, posts, jobs, deletion/retention and production controls remain pending. No AWS deployment or complete-platform production-readiness claim is made.


### Company-owned jobs checkpoint

Jobs now supports owner-authorized publication, edits, closing/reopening, search by title/company/location/work arrangement, company-specific listings and private saved jobs. Jobs navigation is active and company owners can publish from their company page. V22 persists jobs and bookmarks; generated contracts connect the UI to real APIs. Closed jobs leave discovery but remain clearly marked in saved/managed views. Applications are explicitly unavailable, and saving never claims to apply.

Verified: `./scripts/container-up.sh --profile observability` succeeded with V22; `./scripts/verify.sh` passed 60 backend invocations, generated-contract/type checks and all twelve real-service browser journeys. Coverage includes company ownership, CSRF, creation retries, concurrent versioned state changes, immutable company assignment, private bookmarks, filtering/pagination, publication/persistence, closing/reopening and cross-account saved views. Following small visual corrections, the final containers rebuilt and both jobs and company browser journeys passed again. Desktop/light and mobile/dark job captures were reviewed; axe/mobile overflow checks passed.

See [job contracts, guarantees and backlog](docs/jobs-slice.md). Applications/resume uploads, tracking, delegated recruiters, recommendations/alerts, moderation, job events/search projections, performance validation and AWS deployment remain pending.


### Profile-based applications checkpoint

Members can now submit a statement with an explicitly shared name/headline snapshot, track persisted status, and withdraw while submitted or under review. Company owners receive a private review queue and can review, accept or reject applications. Server-side actor/state/version checks enforce transitions; V23 stores immutable submission snapshots and transactional audit rows. Duplicate submissions with the same normalized statement return the existing receipt; changed statements conflict. The tracker uses authenticated content-free live hints plus REST polling/recovery.

Verified: `./scripts/container-up.sh --profile observability` succeeded with V23; `./scripts/verify.sh` passed 64 backend invocations, generated-contract/type checks and all thirteen real-service browser journeys. Tests cover privacy, snapshot persistence, CSRF/ownership, terminal states, audit counts, pagination, concurrent retries, review/withdrawal races and job closure races. The new two-account browser journey proves recovery after a real committed submission times out, duplicate-free retry, live review/acceptance and withdrawal updates, and revoked-session cleanup. Application desktop/mobile light captures were reviewed; axe/mobile overflow checks passed.

See [application contracts, state machine and remaining requirements](docs/applications-slice.md). Resume uploads, required application questions, delegated recruiters, reapplication, durable application notifications, retention/export/deletion and production validation remain pending. This completes the narrower profile-based workflow, not the requested resume-based E2E-009 path. No AWS deployment occurred.


### Application history and filtering checkpoint

Applicants and company owners can now inspect committed application history, including transition order, timestamps and actor roles without account identifiers. The authorized no-store endpoint returns history and its application version from one SQL statement. Open timelines refresh when status versions change; requests are bounded and cancelled when obsolete. The tracker now filters by status before pagination while retaining caller/job authorization.

Verified: `./scripts/container-up.sh --profile observability` and `./scripts/verify.sh` passed: healthy builds/startup, 66 backend test invocations, generated-contract/type checks and all thirteen real-service browser journeys. New checks cover history denial/redaction/order, duplicate and failed-transition stability, filtered pagination across 22 records, live review/acceptance history, owner/applicant views and empty/all filter recovery. Desktop/mobile axe and mobile overflow checks passed; the updated light mobile timeline was reviewed.

See [application history and filters](docs/applications-slice.md#application-history-and-status-filters). Resume uploads, delegated recruiters, durable application notifications and the broader production backlog remain pending. No AWS deployment occurred.


### Persistent job preferences checkpoint

Members can save private title/company keywords, location and work arrangement, then use Preferred jobs to retrieve matching open roles. The backend applies preferences and explicit search filters before pagination. V24 stores caller-scoped, versioned settings; stale writes preserve the client draft and require reload. The collapsible sidebar editor supports save, cancel, reload and unsaved-change protection. These preferences enable deterministic matching, not alerts or recommendation ranking.

Verified: local containers rebuilt healthy with V24; 69 backend invocations, generated-contract checks and strict TypeScript passed. The new browser journey signs in through a separate Keycloak browser session, confirms persisted preferences, changes them there and proves a stale original-session save cannot overwrite them. An accessibility failure on a company-filter inline link was fixed with an underline. The final `npx playwright test` passed all fourteen real-service journeys; desktop/mobile axe and overflow checks passed, and corrected light screenshots were reviewed.

See [job preferences and matching guarantees](docs/jobs-slice.md#persistent-job-preferences). Resume uploads, delegated recruiters, job alerts/ranking and the remaining production backlog are still pending. No AWS deployment occurred.

### Delegated recruiter checkpoint

Company owners can invite accepted connections as recruiters. Recipients accept, decline or leave; owners revoke access. Active recruiters use the existing job editor and application review queue. Authorization is enforced against persisted grants on every request; pending and revoked grants provide no recruiting permissions. V25 stores versioned grants and transactional audit rows. Application sharing disclosures and history distinguish authorized recruiters from company owners. Company editing and team administration remain owner-only.

Verified: `./scripts/container-up.sh --profile observability` rebuilt healthy with V25. `./scripts/verify.sh` passed all 71 backend test invocations, contract consistency, strict TypeScript and all 15 real-service browser journeys. The new three-user journey covers invitations, acceptance, job editing, application review and revocation that clears an already-open review queue. Desktop axe and mobile overflow passed; desktop/mobile light screenshots were reviewed. Two test compilation mistakes during development were fixed before the successful regression run.

See [recruiter permissions, consistency guarantees and traceability](docs/recruiting-slice.md). This supersedes earlier checkpoint notes listing delegated recruiters as pending. Resume uploads, application questions, configurable roles, durable application notifications, mobile OTP, passkeys and the broader production backlog remain. Concurrent-revocation stress and recruiter-specific dark-mode checks remain unverified. No AWS deployment occurred.

### Plain-text resume checkpoint

Applicants can attach an optional UTF-8 `.txt` resume up to 20 KiB, preview/remove it and submit it with explicit sharing consent. V26 stores the resume, application and audit record atomically in PostgreSQL. Identical retries preserve one attachment; changed resume content conflicts. The tracker and receipt provide authorized downloads for the applicant, owner and current active recruiters. Revocation removes subsequent recruiter download access. Downloads use a fixed filename, no-store, nosniff and sandbox headers; application lists and events contain no resume text.

Verified: `./scripts/container-up.sh --profile observability` rebuilt healthy with V26. `./scripts/verify.sh` passed all 74 backend test invocations, generated-contract consistency, strict TypeScript and all 15 real-service browser journeys. Added checks cover invalid file names/text, UTF-8 byte boundaries, missing fields, request-size enforcement with and without declared length, duplicate/conflicting retries, private downloads and recruiter revocation. The expanded application browser journey validates bad-file feedback, a lost committed response followed by safe retry, and unchanged downloaded content for applicant and owner. Desktop light/mobile dark axe and mobile overflow passed. Screenshot scroll positioning was corrected, the application journey rerun successfully, and both captures reviewed.

See [resume contracts, privacy, storage decisions and remaining work](docs/resumes-slice.md). This completes the bounded plain-text attachment workflow; PDF/DOCX uploads, quarantine/scanning/CDR, S3 media infrastructure, retention/export/deletion and the remaining platform backlog are not complete. No AWS deployment occurred.

### Durable application notifications checkpoint

Submission and withdrawal now notify the company owner; review, acceptance and rejection notify the applicant. Each application transition commits its audit and outbox record atomically. The shared Kafka worker publishes ApplicationChanged v1 identifiers/version, projects against authoritative source records and deduplicates delivery. Statements, resumes and reviewer identities are absent from these notification payloads. Malformed application events produce redacted DLQ diagnostics. V27 adds source constraints and per-application/version uniqueness.

The notification UI now offers All, Network and Jobs filters, application context links and an independent persisted job-update preference. Global unread counts span categories; read state survives reloads. Opt-out applies during projection with no later backfill. The new dark-mode check found and fixed a shared system-theme outline-button contrast issue.

Verified: local containers rebuilt healthy with V27. The backend stage of `./scripts/verify.sh` passed 76 test invocations (71 domain/profile, two real Kafka integration, three real-time handler), contract checks and strict TypeScript. The initial browser run caught the contrast issue; subsequent test corrections wait for server-confirmed checkbox/read mutations before asserting or reloading. Mailpit polling now allows 15 seconds after a five-second identity-test timeout with the email subsequently confirmed present. The final `npx playwright test` passed all 16 real-service journeys. Desktop light/mobile system-dark axe and overflow checks passed, and both notification screenshots were reviewed.

See [application delivery guarantees and remaining scope](docs/notifications-slice.md#durable-application-updates-v27) and the [event runbook](docs/runbooks/events.md). Recruiter notification fan-out, email/push, retention/rebuild tooling, cross-replica delivery and production hardening remain pending. No AWS deployment occurred.

### Versioned application questions checkpoint

Owners and active recruiters can configure up to five required/optional text questions per published job. Applicants load the current question set, answer required fields, and submit answers with the application. V28 persists the original prompts, required flags and normalized answers atomically with the application. Later employer edits cannot change an existing submission. Question edits and submissions share organization/job locks; stale submissions return a conflict and the UI can reload questions while preserving the statement, resume and unchanged answers.

The existing private application tracker displays answer snapshots to the applicant and current authorized recruiting staff. Answers are excluded from notification/event payloads. The editor provides save/cancel, reload, unsaved-change protection and version-conflict feedback; revoked recruiters cannot edit questions.

Verified: local builds and health checks passed with V28. The backend stage of `./scripts/verify.sh` passed 79 test invocations (74 domain/profile, two Kafka integration, three real-time handler), generated-contract checks and strict TypeScript. The initial browser run found an implicit label association issue when reopening saved questions; explicit label/input associations fixed it. The targeted journey then passed, followed by a final `npx playwright test` with all 17 journeys passing. Desktop editor/mobile system-dark applicant axe and overflow checks passed; both screenshots were reviewed.

See [question contracts, concurrency and remaining work](docs/application-questions-slice.md). Structured question types, scoring, question edit audit history, retention/export/deletion, richer document uploads and the broader production backlog remain pending. No AWS deployment occurred.

### Question ordering checkpoint

Added accessible Move up/down controls to the application-question editor, preserving question identities and existing submission order. Strict TypeScript, container builds/health checks and the expanded real-service question journey passed, including keyboard reordering, persistence after reload and immutable snapshot order after a later reorder. Existing backend contracts and migrations are unchanged. Full-suite results above are from the prior checkpoint; this increment used targeted verification. No AWS deployment occurred.

### Question removal recovery checkpoint

Added one-step Undo question removal to the editor, restoring identity, prompt, required status and position before saving. Strict TypeScript, container build/health checks and the expanded real-service application-question journey passed. Backend contracts and migrations are unchanged; full-suite results above remain from the earlier checkpoint. No AWS deployment occurred.

### Screenshot-driven Network page

The top My Network tab now opens a dedicated /network page based on the supplied network screenshot. Real invitations, connection counts, accept/ignore/withdraw/remove actions, name search and following management are integrated with existing APIs. Unsupported screenshot modules are explicitly unavailable. TypeScript, container builds/health and the new two-user browser journey passed, including desktop/mobile dark accessibility checks and screenshot review. Full suites were not rerun; no AWS deployment occurred. See docs/network-slice.md for exact coverage and remaining requirements.

Profile photo upload is available in the profile introduction: JPEG/PNG up to 2 MiB, with preview, replacement and removal. Photos persist and follow profile visibility on shared member pages. See [profile photo implementation and verification](docs/profile-photo-slice.md).


### Settings and persistent session management checkpoint

Added screenshot-based Settings navigation with account preferences, visibility and Sign in & security. Meridian browser sessions now persist using Spring Session JDBC and V31. Members can inspect active sessions, sign out one session or all Meridian devices, with owner-scoped/CSRF-protected APIs and transactional audit records. Remote revocation disconnects live sockets; later API requests require authentication. A new OIDC login requires interactive authentication even if Keycloak retains an SSO cookie. Provider-wide token/session revocation remains pending and is explicitly distinguished in the UI.

Verification: `./scripts/container-up.sh --profile observability` passed builds and health checks. The backend/contracts/types stages of `./scripts/verify.sh` passed all **84 backend test invocations**, generated-contract consistency and strict TypeScript. Its first browser run found an older ambiguous search checkbox selector introduced with public visibility; after making it specific, final `npx playwright test` passed **all 21 real-service journeys**. Session tests cover private management IDs, cross-account denial, duplicate revocation, audit, expiry, stale-session-save behavior, remote WebSocket disconnection and all-device API denial. Desktop light/mobile dark axe and overflow checks passed; screenshots were reviewed against S08. No AWS deployment occurred.

See [session implementation](docs/session-management-slice.md) and [current remaining requirements](docs/remaining-backlog.md). This completes the application-session slice, not all authentication or overall platform requirements.

### Screenshot-driven authentication checkpoint

The login-registration screenshot folder now drives a responsive guest welcome page and branded real Keycloak sign-in/registration forms. `/login` starts interactive email sign-in; `/register` starts registration directly with OIDC `prompt=create`, preserving generated state, nonce, S256 PKCE and the fixed callback. Registration, verification mail, password visibility, recovery, TOTP and profile initialization use the real identity provider/backend. The theme applies to existing realms without replacing accounts or credentials, and builds against the pinned Keycloak distribution.

Verified: container builds and healthy startup; `./scripts/verify.sh` passed **86 backend tests**, generated-contract consistency, strict TypeScript and **all 22 real-service browser journeys**. Desktop/mobile welcome and login plus desktop/mobile-system-dark registration were captured and reviewed; axe and overflow checks passed. The first targeted run exposed an inherited logo contrast rule and an outdated welcome assertion, both corrected before the successful final run.

Google/Apple providers, phone login, remembered-session policy and legal consent remain unfinished. Four other images in the folder show jobs, events, followed companies and newsletters; their coverage and remaining scope are explicitly mapped in [authentication screenshot implementation](docs/authentication-screens.md). No external provider or AWS deployment was performed.

The isolated `./scripts/test-local.sh` check also passed with the custom identity image, verifying fresh realm startup, service readiness and persistence after container recreation without resetting developer volumes.
