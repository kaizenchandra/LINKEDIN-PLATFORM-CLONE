# People discovery slice

| Requirement | Classification | Frontend | Backend / data | Contract | Test | Status |
|---|---|---|---|---|---|---|
| INT-SEARCH-001 | Explicitly required by uploaded prompt | Header search, /search, result cards, profile links | PeopleSearchController; PostgreSQL profiles | GET /search/people | search.spec.ts; API tests | Integration verified |
| INT-PRIVACY-SEARCH-001 | Explicitly required by uploaded prompt | Appear in people search setting | V18 discoverable false by default; versioned writes | GET/PUT /me/discovery | Default, persistence, conflicts, authorization | Integration verified |
| BE-SEARCH-002 | Explicitly required by uploaded prompt | Only authorized results shown | Requires shared summary AND discovery opt-in; excludes caller and bilateral blocks | PeoplePage | Privacy/block API and browser checks | Integration verified |
| FE-SEARCH-003 | Explicitly required by uploaded prompt | Name/headline search, location filter, More people, refresh, errors, empty state | Parameterized literal substring queries; UUID cursor | Generated TypeScript schemas | Filter, 21-result pagination, input validation | Integration verified |
| E2E-SEARCH-001 | Inferred from interaction | Search → profile → Follow / Connect | Existing real social graph APIs | Existing follow/invitation contracts | Two real Keycloak accounts | Integration verified |

## Actual behavior and privacy

The original profile-sharing consent covered signed-in members with a link. Search therefore requires a separate explicit opt-in; V18 leaves every existing and new member undiscoverable by default. Opting into discovery alone does not make a private summary searchable. Both flags must be true. Disabling either affects subsequent search reads immediately.

Search returns only public member ID, name, headline and location. It does not return email, phone, identity-provider subject, private profile sections or about text. A discovery opt-out does not revoke an otherwise shared profile link; the UI explains that distinction. Blocking excludes results in both directions. Already delivered information cannot be retracted; refresh clears and retrieves current results.

Discovery preferences use optimistic versions, authenticated sessions and CSRF protection. Search uses session authentication and no-store responses. User input is bound as SQL parameters. Percent and underscore characters are literal; no LIKE-pattern or SQL expression is interpolated. Search terms are not explicitly logged by application code. Infrastructure access-log redaction still needs production review.

## Query and UI design

The query is a case-insensitive substring of name plus headline, with an optional location substring. Trimmed search length is 2–80 characters; location is at most 120. Results are ordered by ascending public UUID, not relevance. Pages contain at most 20 entries plus an optional cursor; cursors reset when submitted criteria change. There is no public result count.

PostgreSQL is the source of truth, so profile, privacy and block updates need no asynchronous search-index catch-up. The partial public-ID index supports discovery cursor traversal; substring filtering can still scan many opted-in rows. This is suitable for the current local functional slice, not demonstrated production search capacity. OpenSearch, linguistic analysis, typo tolerance, relevance ranking, highlighting and scale measurements remain pending.

The global header now opens people search. The previous local profile-section finder remains in the profile sidebar. Search results navigate to existing profile/Follow/Connect controls. Submitted query, applicable location and category are written to the URL; reload and browser back/forward restore the submitted state. Search history is not persisted. Older in-flight responses cannot overwrite newer searches. The page has account-synchronized appearance controls and uses the shared responsive design tokens.

## Verification

Executed `./scripts/container-up.sh --profile observability` and `./scripts/verify.sh`: both application images built, local services became healthy, 42 backend invocations passed, generated OpenAPI consistency and strict TypeScript passed, and all seven real-service Playwright journeys passed.

Backend checks cover default opt-out, preference CSRF/version/null validation, case-insensitive and literal matching, location filtering, self exclusion, disclosure limits, profile privacy, discovery opt-out, blocking, anonymous denial, query/cursor limits and 21-member pagination without duplication. The browser journey uses real Keycloak/PostgreSQL and Mailpit, verifies discovery persistence across reload, filters, search → follow/invitation, opt-out with continued shared-link access, and block suppression.

Desktop and mobile dark-mode search captures were visually reviewed. Axe checks passed on both tested states and mobile overflow checks passed. Existing profile-section navigation and design tests also passed after the header change. No pixel-diff baseline or complete manual accessibility certification is claimed.

## Remaining scope

Independent search discoverability by identifier, query rate limiting, abuse monitoring, search-history controls, relevance ranking, OpenSearch projection/rebuild/reconciliation and performance benchmarks remain planned. This implementation adds no new service boundary, event producer or AWS resource. Existing HTTP metrics apply; dedicated search latency/abuse dashboards and distributed traces are pending.


## Unified transactional search

The shared search page now has People, Companies, Jobs and Posts categories. Companies/jobs reuse their existing authoritative APIs and navigate to real company/job detail pages. People retains its discovery consent and profile navigation. Posts uses `GET /search/posts?q=…&before=…`, returning the generated FeedPage contract. It searches literal text case-insensitively and orders by descending post sequence, with 20-item cursor pages.

Post search shares `PostAccess.VISIBLE` with the feed: deleted posts are excluded, private posts are visible only to their author, and bilateral blocks apply on every read. Profile-summary privacy is independent of a deliberately member-visible post. Refresh clears previously delivered results and retrieves the current authorized state; there is no immediate revocation push. Results show persisted post text, audience and reaction count; search-result reaction/comment controls are not included.

Search requests have a ten-second transport/body deadline and abort when replaced or unmounted. Response sequence guards prevent stale requests overwriting new criteria. Category changes reset pagination, and URL history is supported. No query-history store, search index, relevance ranking or measured scale claim is introduced.

The added backend test checks authentication, malformed criteria, literal percent/underscore matching, 21-result pagination, private/deleted exclusion and blocking. `unified-search.spec.ts` checks real private-post creation, owner-only search, category URL/back/reload behavior, changing audience to members, second-account results, deletion and responsive dark-mode accessibility. The existing people-search journey remains a regression check. Verification totals are recorded in the README's latest checkpoint.
