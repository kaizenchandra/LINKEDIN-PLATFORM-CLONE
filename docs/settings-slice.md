# Account settings: appearance and member-summary visibility

| Requirement | Source classification | Frontend | Backend/data | Contract | Test | Status |
|---|---|---|---|---|---|---|
| INT-SETTINGS-THEME-001 | Explicitly required by uploaded prompt | Appearance selector | account_preferences, V8 migration | getMyPreferences/updateMyPreferences | Two independent browser contexts | Integration verified |
| BE-SETTINGS-THEME-002 | Recommended implementation detail | Conflict message and rollback | Subject-bound compare-and-swap | version field; 409 | API and browser stale-write rejection | Integration verified |
| FE-SETTINGS-THEME-003 | Explicitly required by uploaded prompt | Light/dark/system; local initial theme | Account value is authoritative after load | Preferences enum | Persisted dark/light values across sessions | Integration verified |

Appearance and member-summary visibility are synchronized. Other privacy controls, interface/content language, autoplay, sound, account deletion and advertising settings remain planned; notification preferences and scoped business-data export are implemented. No inert controls are presented for these capabilities.

An initial static bootstrap reads only a validated local theme value before hydration. Local storage failures are tolerated. A new browser without a cached theme starts with system appearance until the authenticated preference is fetched. Cross-device changes require reload; there is no push synchronization. Production CSP integration for the static bootstrap and visual/accessibility regression checks remain pending.

## Member-summary visibility

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PRIVACY-001 | Explicitly required by uploaded prompt | Opt-in member-summary visibility, versioned setting, member page | Two distinct accounts through real UI | Integration verified |
| BE-PRIVACY-002 | Explicitly required by uploaded prompt | SQL authorization predicate on every summary read; missing/private both 404 | Default denial, opt-in access and revocation tests | Integration verified |
| BE-PRIVACY-003 | Recommended implementation detail | Random public UUID distinct from identity-provider subject; minimal summary DTO; no-store | API assertions | Integration verified |

Profiles are private by default. Owners can opt into sharing name, headline, location and about with authenticated members who possess the link. Anonymous access is available only when the owner separately enables public sharing; authenticated member sharing remains separate. Experience, education, certifications, projects, skills, languages, account settings and identifiers remain self-only. The member page reloads on focus and manual refresh and clears its displayed summary while refreshing.

Revocation applies to subsequent server reads after the visibility transaction commits. Content already delivered to another browser cannot be retracted, and no live revocation push is implemented. Discovery opt-in and bilateral blocking are enforced. Connection-only sharing, section-specific privacy and email visibility remain pending. The setting must not be interpreted as authorization to expose other profile sections.


## Durable business-data export

Settings → Data Privacy provides request, polling, download and removal through the real session-authenticated `/api/v1/me/exports` API. `V34__account_exports.sql` owns jobs, bounded archive bytes and request/download/cancel audit records. The API contract and generated browser schema describe the state machine. This is a scoped business-data export, not complete identity-provider portability or account deletion.

Request, download and removal require a validated OIDC ID token with `auth_time` within five minutes. The fixed reauthentication link requests `prompt=login` and `max_age=0`, preserves PKCE/state/nonce and returns only to `/settings#privacy`. Missing/stale authentication returns 428. The server checks the signed claim; client route state is not proof. See the [OIDC authentication-time requirements](https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest).

The request UUID is an idempotency key. A profile row lock and unique active-job constraint serialize new requests per owner. One new request per ten minutes is allowed. Jobs transition QUEUED → PROCESSING → READY, or FAILED after bounded retries. Workers claim with `FOR UPDATE SKIP LOCKED`, use a two-minute lease and an attempt guard; old workers and canceled requests cannot publish. Retry delay is 30 seconds, with three attempts. No private archive contents enter Kafka or logs.

The snapshot uses a read-only repeatable-read transaction and bounded JDBC fetches. It contains the owner's profile/photo, professional sections, preferences, relationships, authored posts/comments, reactions/bookmarks, sent messages, owned company/job data and their own applications/resume text. It excludes identity credentials/tokens, other members' received-message bodies and other applicants' submissions. The archive explains this scope. Sections are capped at 10,000 records and the whole JSON archive at 10 MiB; exceeding either fails the job with no partial download.

Downloads enforce ownership again, return no-store and attachment headers, and expire 24 hours after completion. Removal immediately clears the payload and prevents late worker publication. The worker also clears expired payloads; if the worker is down, the endpoint still denies expired downloads. PostgreSQL stores the bounded archive; S3 storage and larger exports are not implemented. Metadata/audit retention, provider-data export, consent and account deactivation/deletion remain pending.

`meridian.exports.completed` and `meridian.exports.failures` counters track processing outcomes. Operators should inspect queued age and failed counts without printing payloads. Recover a stopped worker by restarting the application; expired leases become claimable. A TOO_LARGE result cannot be resolved by repeated retries and requires future larger-export support. Production encryption, archival retention and alert dashboards remain deployment gaps.

Tests cover CSRF/authentication/recency, owner isolation, duplicate requests, cooldown, leased-worker recovery, cancellation races, expiry and explicit oversized failure. `data-export.spec.ts` uses real Keycloak reauthentication, PostgreSQL persistence, a downloaded JSON archive, cross-account denial, removal, reload and desktop/mobile dark-mode axe checks. It waits for confirmed profile persistence before exporting and disables traces/videos of private archives.
