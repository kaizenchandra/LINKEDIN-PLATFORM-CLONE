# Private search history (V43)

This slice adds opt-in account-synchronized search history to `/search` and Settings → Data privacy. It closes the bounded search-history portion of REM-SEARCH-001 and adds a specific privacy control under REM-PRIVACY-001. It does not complete general consent management, account deletion, autocomplete, search indexing or the remaining platform backlog.

## User behavior

- Saving is off by default. Enabling it is explicit and persists across sessions.
- The search-page form stores submitted criteria for People, Companies, Jobs and Posts. Header navigation, URL visits, keystrokes, tab changes, pagination, refresh, and browser Back/Forward do not automatically store queries. The interface explains this scope.
- Up to 20 distinct searches are retained, ordered by last explicit submission. Repeated normalized criteria update one entry. Companies and Posts discard location instead of retaining an unused field.
- A recent-search action restores the search fields, category and URL and executes the current authorized search API. It does not store result bodies or bypass visibility filtering.
- Members can remove individual entries, clear all entries, or disable saving. Disabling atomically clears stored entries. Re-enabling does not restore them.
- History is owner-only and absent from public profiles, other members' APIs and Kafka events. This module emits no query values in logs/metrics.
- Search continues to work when saving is disabled or unavailable. Failed history writes have explicit refresh/retry feedback and are not represented as successful persistence.

Browser history and existing export archives/downloads are independent copies. Clearing search history does not erase those copies; existing server exports can be removed using the export controls. New exports include only the current retained history, exclude internal deduplication keys, and remain protected by existing recent-authentication checks.

## Contracts and state

| Contract | Behavior |
|---|---|
| `getMySearchHistory` | Owner-only no-store preference revision and retained items |
| `setSearchHistoryPreference` | CSRF, current revision, atomic consent change and purge on disable |
| `rememberMySearch` | CSRF, opt-in/current revision, normalized deduplication, newest-20 bound |
| `removeMySearchHistoryEntry` | Owner-scoped deletion; foreign/missing IDs cannot affect another account |
| `clearMySearchHistory` | Atomic owner purge and revision advance |

Authoritative schemas are in `packages/contracts/profile.openapi.yaml`, with generated TypeScript types in `apps/web/app/api-schema.d.ts`. Invalid criteria return 400, unauthenticated calls 401, missing CSRF 403, and disabled/stale writes 409. Search results and history are separate APIs; recording a query does not assert that results were found.

All writes serialize on the caller's profile row. Preference changes and deletion increment a consent revision. Record requests must carry the current revision: a request started before deletion cannot recreate the cleared entry afterward. Concurrent searches at the same consent revision serialize and preserve the 20-entry bound. Normalized criteria are unique per owner/category/query/location. Record retries cannot create duplicate rows, but can update recency. Deletion/preference retries require a refreshed revision rather than blind retries.

The UI confirms destructive clear/disable actions, keeps failed actions visible, prevents overlapping local mutations, and discards stale read responses. It refreshes on focus and every 15 seconds. Other already-open browser sessions may display an older list until that refresh; backend deletion and stale-write rejection are immediate.

## Retention and operations

History older than 30 days is excluded immediately from lists and newly generated exports. Owner mutations purge expired records. An isolated worker also deletes up to 1,000 expired rows per minute with an indexed query, skip-locked batching and a five-second database statement deadline. It does not share the Kafka/realtime scheduler. Failed cleanup is retried on the next tick; physical cleanup may lag under outage or backlog.

Counters `meridian.search.history.expired` and `meridian.search.history.cleanup.failures` contain no personal labels. Inspect aggregate expired-row counts and service/database health when failures persist. No search text is needed for that diagnosis. This implements a retention mechanism, not a legal retention-policy assessment or a measured cleanup SLO.

## Traceability and verification

`INT-SEARCH-HISTORY-001` maps the frontend hook/panel and Settings integration to `SearchHistoryController`, V43, `SearchHistoryRetention`, the five contracts above, owner-scoped export projections, backend privacy/retention tests and `search-history.spec.ts`.

Backend tests cover default-disabled consent, CSRF, duplicate normalization, owner isolation, foreign-ID deletion, max-20 storage, unused-location removal, export inclusion/removal, stale writes after clearing/disabling, and expiration cleanup. The browser journey uses real Keycloak/backend/PostgreSQL for opt-in, search reuse, persisted state, second-account isolation, Settings deletion, stale write denial, clear/disable, and mobile dark-mode accessibility/overflow checks. No successful business response is mocked.

### Executed checkpoint

- `./scripts/localstack-up.sh --profile observability`: container builds/readiness passed; Flyway applied all 43 migrations.
- `./scripts/verify.sh`: 120 backend tests passed, contract generation consistency and strict TypeScript passed. The first browser run passed 33 journeys and exposed an immediate-checkbox assertion in the new journey; the test now waits for server-confirmed state and the UI displays mutation progress.
- After rebuilding the final web image, the targeted search-history browser journey passed, followed by a complete `npx playwright test` run from `apps/web`: **34 passed**.

No production deployment, hosted CI execution or performance certification was performed for this slice. The remaining requirements stay tracked in `remaining-backlog.md`.
