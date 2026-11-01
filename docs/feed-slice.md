# Text publishing and recent feed

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-FEED-001 | Explicitly required by uploaded prompts | Text composer → authenticated API → PostgreSQL → feed/reload | Real two-account browser journey | Integration verified |
| BE-FEED-002 | Explicitly required by uploaded prompts | Owner-only editing/deletion, expected versions, tombstones | PostgreSQL API tests | Integration verified |
| INT-FEED-003 | Explicitly required by uploaded prompts | Per-post Only me/Members audience and bidirectional block filtering | API and browser tests | Integration verified |
| BE-FEED-004 | Explicitly required by uploaded prompts | Recent-first sequence ordering and keyset pagination, 20 items/page | PostgreSQL pagination test | Integration verified |
| BE-FEED-005 | Explicitly required by uploaded prompts | Same-ID matching creation replay with one stored row | Eight concurrent PostgreSQL API requests | Integration verified |
| FE-FEED-006 | Explicitly required by uploaded prompts | Modal composer, pending/error handling, retained text on transport failure, cancellation confirmation | Browser journey with one explicitly intercepted failed request | Integration verified |
| BE-FEED-007 | Explicitly required by uploaded prompts | Reliable publication to downstream event consumers | No producer/outbox/consumer yet | Planned |

## Actual architecture and consistency

Content currently belongs to the existing Spring application and its PostgreSQL database. V12 introduces content_post: client-generated UUID identity, database sequence for feed order, authenticated author, plain text, audience, creation timestamp, edited flag, optimistic version and deletion timestamp. Foreign keys, audience/text constraints and recent/author indexes enforce local storage invariants. This is not a separately deployed content microservice.

The authoritative OpenAPI defines GET /feed, GET/PUT/DELETE /posts/{id}; generated TypeScript schemas are consumed by the composer and feed. React renders text as text, never HTML. The server ignores supplied identity fields and derives author ownership from the OIDC session. All writes use CSRF validation.

Creation at version zero inserts once. A duplicate UUID request succeeds only when owner, normalized text, audience and original version match a live version-one row. Different payloads, foreign ownership and reuse after deletion return 409. Updates and deletes atomically compare owner/version; stale edits or duplicate deletes return 409 and require refresh. Reads use no-store and enforce visibility/blocking at query time, including direct post lookup. No feed projection/cache exists, so there is no asynchronous visibility lag in backend queries.

The UI applies confirmed mutation responses rather than assuming success. Refresh removes cached posts before retrieving authoritative data. Already delivered content in another open browser is not retractable; that view must refresh. There are no real-time events yet. Pagination walks decreasing immutable sequence numbers; newer inserts appear on refresh, and editing does not promote old posts. Sequence order is deterministic creation allocation order, not a claim of global commit ordering under concurrency. Ranking is not personalized.

Only me is the default audience. Selecting Members explicitly shares the author's name and post text with signed-in members independently of profile-summary visibility. A post's author link can therefore lead to an unavailable/private profile. Blocking suppresses posts in both directions. Soft-deleted text is retained in the database; retention/purge and account-deletion propagation remain unimplemented.

## UI and verification

/feed uses the shared screenshot-inspired navigation, a three-column desktop arrangement and single-column mobile layout. Home now navigates to /feed; / remains the profile and post-login destination. Unimplemented reaction/comment/share controls are omitted rather than showing fake success. No fabricated counts or seeded activity are shown.

Executed: container builds/startup, scripts/verify.sh (28 backend test invocations, contract check and strict TypeScript passed; four existing browser journeys passed). The new feed journey initially failed because the test removed interception before the save request reached it, then a generic alert selector also matched Next's route announcer. After scoping the selector and waiting for the error, the targeted real-service feed test passed. Thus all five journeys passed across the regression and targeted rerun. The successful mutation and recovery paths use real local Keycloak and PostgreSQL; only one deliberate failure intercepts transport. Axe passed for the tested feed view. Desktop/mobile captures were visually inspected; no full WCAG conformance or pixel-diff regression claim is made.

## Remaining content/feed work

Reactions and uniqueness; comments/replies; reposts; bookmarks; reporting/moderation; attachments and secure media; mentions/hashtags; persisted drafts; author activity filtering; follow/connection ranking; search indexing; impression analytics; rate limits; transactional outbox/Kafka publication and consumers; live updates; performance/load testing; privacy propagation to future projections. These requirements remain required and are not satisfied by the initial text feed. The HTTP metrics already instrument these API requests, but domain-event traces and audit records remain pending.

## Like reactions checkpoint

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-FEED-008 | Explicitly required by uploaded prompts | Like/unlike, persisted count and current member state | Two-account browser journey | Integration verified |
| BE-FEED-009 | Explicitly required by uploaded prompts | One Like per member/post, idempotent state setting | PostgreSQL key and concurrent-request test | Integration verified |
| BE-FEED-010 | Explicitly required by uploaded prompts | Only visible/nondeleted posts may be reacted to; block rules apply | API privacy/block tests | Integration verified |

V13 adds post_reaction with a composite primary key (post UUID, member subject). The endpoint PUT /posts/{id}/reaction accepts `{liked: true|false}` for the authenticated member. No caller-supplied member ID is accepted. Repeated identical commands have the same database effect; a false command removes only the caller's row. Counts are computed from authoritative reaction rows and included in feed, detail and mutation responses, together with the caller's liked flag. Reaction changes do not increment the content edit version.

Reaction writes acquire the existing member-pair advisory lock, then the post row lock, and recheck visibility after acquiring both. Pair serialization coordinates with blocking; the post lock coordinates with privacy edits and deletion. All locks last until transaction completion. This intentionally serializes writes to a popular post; no high-throughput claim is made. Duplicate true commands cannot inflate counts. Aggregate counts include retained reactions even if a reactor later blocks the author; no reactor identity list is exposed. Likes on soft-deleted/private posts remain stored, but unreadable posts cannot be reacted to or returned. Retention and account deletion must eventually purge the relevant records.

The Like button exposes aria-pressed, disables while saving and changes count/state only after a successful backend response. On a transport failure it retains the previously confirmed display and asks the member to refresh to resolve an uncertain outcome. It does not falsely announce that an unconfirmed write failed to persist. Successful replays use the same desired state, not a toggle instruction. No optimistic increment or fake engagement count is used.

This increment supports Like only. Other reaction types, notification/event publication, reactor lists, rate limits and aggregate performance optimization remain pending. The earlier text-slice limitations above are historical where superseded here.


Reaction verification: `scripts/container-up.sh --profile observability` and `scripts/verify.sh` passed. Both images built; 30 PostgreSQL-backed test invocations passed, including eight concurrent Like requests producing count one, repeated unlike, missing-state validation, ownership of removal, private/blocked/deleted denial and unchanged content version. Generated contract check and strict TypeScript passed. All five browser journeys passed, including failed-reaction transport recovery, two-account count synchronization after refresh, persistence across reload, unlike and blocking. Existing axe and responsive checks passed; the new mobile reaction capture was visually reviewed. Successful writes use real services; only explicit failure cases intercept transport. No external deployment or event delivery is claimed.

## Top-level comments checkpoint

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-FEED-011 | Explicitly required by uploaded prompts | Create/read/delete own top-level comments with persisted text | Two-account browser test | Integration verified |
| BE-FEED-012 | Explicitly required by uploaded prompts | Idempotent create, author-only deletion and pagination | PostgreSQL concurrency/authorization tests | Integration verified |
| BE-FEED-013 | Explicitly required by uploaded prompts | Parent-post visibility and reader/commenter blocking | Three-principal API tests | Integration verified |

V14 adds post_comment, owned by the existing content context: UUID identity, immutable text, authenticated author, parent post, creation time, monotonic sequence and soft-deletion timestamp. GET /posts/{postId}/comments returns up to twenty readable comments in descending sequence order. The cursor continues to older comments. Parent-post visibility is enforced in the comment query itself, in addition to the unavailable-post check; bidirectional reader/commenter block filtering also applies. No commenter email or internal subject identifier is returned.

PUT /posts/{postId}/comments/{id} creates immutable plain text. Identical normalized payload/author/parent retries return the same comment. A reused ID with a different payload, author or parent returns 409; deleted IDs cannot be reused. DELETE is idempotent for the author's matching row; parent-post owners cannot delete someone else's comment. Soft deletion retains text pending a future retention/purge policy. The shared PostAccess policy now supplies parent visibility and lock ordering to reactions and comments. Mutations acquire the actor/post-author pair lock and post row lock before checking permission, coordinating with post privacy/deletion and blocking.

The expandable comment panel uses authoritative responses, retains text/request ID after failed saves, warns before navigation with a draft, and confirms draft discard or deletion. Successful save clears the draft; confirmed deletion removes the item. Refresh clears cached comments before retrieving current state. Another browser must refresh to observe changes. Already delivered content cannot be retracted. Comments follow the post audience, so changing that audience can expose or hide existing comments; the UI states this relationship.

This increment does not implement nested replies, comment editing/reactions, moderation/reporting, author notifications, search indexing, event publication, durable cross-device drafts or rate limits. None of those requirements is removed from the backlog. No new Kafka infrastructure or distributed-delivery guarantee is claimed.


Comment checkpoint evidence: `scripts/container-up.sh --profile observability` and `scripts/verify.sh` passed. Both images built; all 32 PostgreSQL-backed test invocations passed, including concurrent retries creating exactly one comment, pagination, invalid text rejection, parent privacy/deletion, commenter blocking, duplicate deletion and foreign-author deletion rejection. Generated-contract and TypeScript checks passed. All five real-service browser journeys passed, including the expanded two-account comment workflow with reload persistence, draft retention after injected transport failure, deletion and refreshed cross-account state. Existing axe checks passed with comments expanded; the desktop feed/comment capture was visually reviewed. Mock transport is limited to explicit failure tests. No deployment beyond local Compose occurred.

## Saved posts checkpoint

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-FEED-014 | Explicitly required by uploaded prompts | Save/unsave plus persistent Saved posts view | Two-account browser journey | Integration verified |
| BE-FEED-015 | Explicitly required by uploaded prompts | Member-owned bookmark uniqueness and pagination | PostgreSQL API/concurrency tests | Integration verified |
| BE-FEED-016 | Explicitly required by uploaded prompts | Saved-list filtering after privacy, blocking and deletion changes | API/browser tests | Integration verified |

V15 introduces saved_post keyed by post and member. PUT /posts/{id}/saved sets the authenticated member's desired state idempotently; it cannot modify another member's bookmarks. The existing pair/post locks coordinate with blocking and parent mutations. The returned Post schema includes only the caller's saved flag; no bookmark totals or membership list are exposed. Saving does not change the content version or issue a notification.

GET /saved-posts reuses the feed's visibility policy and twenty-item keyset pagination. Ordering follows original post sequence, not bookmark creation time. Saved records are references, not copies of post text: edits are reflected in subsequent reads; private, blocked and deleted content is suppressed. If access is restored, a retained bookmark becomes visible again. Soft-deletion retention and eventual account-data cleanup remain pending.

The frontend offers Recent posts and Saved posts views. Save/Unsave waits for the confirmed backend response; uncertain network outcomes retain the prior display with an instruction to refresh. Unsave removes the item from the saved view after confirmation. Reads are cleared/refetched on view changes, and obsolete list responses cannot replace a later request's results. View selection is local UI state and resets to Recent after page reload; bookmarks themselves persist. Switching views confirms before discarding unsaved comment text. This is not an offline reading list, collection/folder system, global search or live cross-session synchronization.


Saved-post verification: `scripts/container-up.sh --profile observability` and `scripts/verify.sh` passed. Both images built; 34 PostgreSQL-backed test invocations passed, including eight duplicate saves yielding one bookmark, pagination, repeated unsave, caller isolation, missing-state validation, CSRF and privacy/block/deletion filtering. Contract generation and strict TypeScript passed. All five real-service browser journeys passed; the feed journey now verifies failed-save feedback, bookmark persistence across reload, saved/recent navigation, blocked-content suppression, unsave removal and deleted-post suppression in the saved view. Existing axe/responsive checks passed. Only explicit failure cases intercept transport; successful paths use real local services. No external deployment occurred.

## Following feed

INT-FEED-009 (explicit prompt requirement): the feed has Recent, Following and Saved views backed by real APIs. GET /feed accepts the generated-contract enum scope=RECENT (default) or FOLLOWING. Unknown scopes return 400. FOLLOWING intersects the caller's current directed follows with the existing PostAccess policy, using PostgreSQL as source of truth. No copied feed records or additional event projection is introduced.

BE-FEED-010 (explicit prompt requirement): following never grants access to private/deleted posts or bypasses blocks. A member-visible post remains intentionally readable if its author makes only their profile summary private. Unfollowing removes that author's posts from the Following view on the next request. Blocks remove follows and deny post access; unblocking alone does not restore a follow. The view includes followed authors only, not the caller's own posts. Creating a post from Following or Saved returns to Recent to display the confirmed write.

FE-FEED-011 (explicit prompt requirement): switching views clears posts and pagination, guards unsaved comments, and ignores obsolete list responses. Following has a specific empty state and pressed-button state. View selection remains transient UI state and resets to Recent on page reload. Buttons wrap on mobile. Reads remain no-store and explicit refresh retrieves current authoritative state.

The ordering policy is descending original post sequence, with 20-item keyset pages. It is a chronological filter, not relevance ranking. Pagination is not a snapshot: following an author midway through pagination may require refresh to retrieve their newer posts. Feed fan-out, ranking, automatic cross-session refresh, load targets and production query-plan validation remain pending.

E2E-FEED-009: the existing real-service Following browser journey now creates public/private posts through the composer, follows the author, checks the Following view and switches views, then unfollows and verifies the empty state. Backend tests cover graph changes, profile versus post privacy, deletion, blocking, invalid scope/cursor, authentication and 21-post pagination. New fixtures roll back so public posts cannot contaminate unrelated pagination tests.

Verification status: INT-FEED-009, BE-FEED-010, FE-FEED-011 and E2E-FEED-009 are integration verified. Both container builds/startup succeeded. Forty backend test invocations, generated-contract check and TypeScript passed. All six browser journeys passed across the regression run and corrected targeted rerun, including existing responsive feed checks and the new Following-view axe check. No visual pixel-diff, load benchmark, asynchronous feed projection or external deployment is claimed.


## Threaded replies

Comments now support a top-level comment plus one level of direct replies. This bounded hierarchy is an implementation policy; deeper nesting is not supported. V36 adds an optional parent UUID, a composite foreign key keeping parent and reply in the same post, a self-parent check and a thread-pagination index. The application rejects deleted parents, replies used as parents and cross-post parents. Parent relationships are immutable.

The existing create-comment contract accepts optional `parentId`; omitting it preserves existing top-level behavior. GET comments returns top-level entries unless `parentId` selects a reply thread. Each thread has independent newest-first pagination with 20 entries per page. Reusing an ID succeeds only for the same author, post, normalized text and parent. Changing the parent on retry conflicts. Writes lock the post, serializing parent deletion and reply creation.

Both post audience and bilateral blocks are enforced server-side. A parent author's block against the reader hides the thread; a blocked reply author is excluded from results. Reads recheck the parent's visibility inside the data query as well as validating the requested parent. Deleting a parent removes its thread from subsequent reads while retaining authored reply records under the existing soft-delete policy. Private owner data exports still include retained authored comments/replies. Reply owners can delete their own records; post ownership alone does not permit deleting someone else's reply.

The feed renders expandable reply threads with distinct reply composers, save/error states, draft preservation, safe UUID retries and refresh controls. Draft warnings and feed-view unsaved-change protection apply to replies. Reads have a ten-second deadline, cancellation and stale-response guards. Styling uses existing responsive/theme tokens. No reply events, email/push notification or live thread fan-out is claimed.

Backend tests cover parent/post mismatch, bounded depth, replay conflicts, ownership, parent blocking/deletion, post privacy and 21-reply cursor traversal. `replies.spec.ts` exercises two real registered accounts, persisted replies after reload, the parent author's view, deletion visibility and mobile dark-mode axe/overflow checks. Verification totals are recorded in the README's current checkpoint. Remaining content work includes persistent post drafts, reposts, mentions/hashtags, attachments, moderation and asynchronous feed projections.


## Persistent private post drafts

The feed composer can save an unpublished draft, and `/drafts` provides a private list, editor, delete action and publication action. Draft text and intended audience persist in PostgreSQL through V37. Drafts are absent from content_post, feed, search and other accounts' APIs until published. The owner's business-data export includes their retained drafts.

The generated OpenAPI contract defines owner-only list/get/save/delete/publish operations. Saves use explicit versions; a stale edit returns 409 and the browser keeps its unsaved text. Exact initial UUID/payload retries return the original draft. Each owner can hold at most 100 unpublished drafts; profile-row locking serializes concurrent creation and capacity checks. Lists use ascending UUID cursors with 20 results per page. Empty text is allowed while drafting but rejected for publication.

Publication locks the draft, inserts a content_post and marks the draft published in one local transaction. The draft UUID becomes the post UUID. The same publication version may be retried without producing another post. A deleted published post is never resurrected by retrying its draft. The retained publication marker is private; editing or deleting a published draft is denied. Post edits/deletion continue through the existing post API. Draft deletion is version-checked hard deletion.

Saving is explicit, not automatic. The editor requires changes to be saved before Publish is enabled, warns before discarding unsaved edits and protects against navigation loss. Publication uses the persisted text and audience. No scheduling, draft collaboration, media drafts or asynchronous content-publication events are implemented.

Tests cover owner isolation, CSRF, stale writes, capacity, independent pagination, blank publication, repeated/concurrent publication and no resurrection after post deletion. The real-service browser journey saves from the feed, reloads the private draft list, verifies cross-account denial, rejects a stale second-tab save while retaining its text, publishes, retrieves the post from another account, and deletes another draft. Mobile dark-mode accessibility/overflow checks are included. See README for executed verification totals.


## Profile post activity

Authenticated member pages now load `/activity/{publicId}` and render real authored posts, dates, intended audience, edit indicators and reaction counts. The endpoint uses the feed's current PostAccess predicate, so it excludes drafts, deleted posts, other people's ONLY_ME posts and bilateral blocks. A deliberately MEMBERS-visible post remains accessible independently of profile-summary privacy, matching existing feed semantics. Guest public-profile views do not request this authenticated activity endpoint.

Activity uses newest-sequence cursor pagination, 20 entries per page, no-store responses and explicit refresh. The UI cancels superseded reads and clears stale results after failures. It does not show fabricated impressions, profile analytics, comments-as-activity or realtime updates. Backend tests verify authentication, invalid cursors, author isolation, private/deleted filtering, blocking and 21-result pagination. The draft browser journey confirms publication appears on the author's shared member page for a second real account.


## Content reports and moderation

The feed exposes Report post for non-owned posts. Reports are persisted through the authenticated, CSRF-protected `/reports/{id}` contract, with explicit SPAM/HARASSMENT/MISINFORMATION/OTHER categories and optional plain-text details. A reporter can submit one report per post and at most 20 new reports in a rolling day. Exact UUID/payload retries return the existing report; conflicting duplicate payloads never overwrite. The current post must be readable at initial submission, including audience, deletion and bilateral blocking. Reports do not automatically remove a post.

`/reports` shows the caller's private report statuses without reviewer identities, internal decision reasons or retained post contents. Submitted report records are included in the owner's business-data export. Another account cannot list these reports through the owner endpoint.

The restricted review view requires a row in moderation_operator. Grants are checked server-side on every queue/decision request; there is no registration privilege, self-promotion API or client-only authorization. Decisions also lock the grant row so revocation serializes with an in-flight decision. Operators cannot decide reports about their own posts or reports they submitted. The queue may show retained current text even after the author changes visibility: this is explicit administrative review access, not public access. Reporter identity is omitted from queue DTOs.

A review records REMOVED or DISMISSED, a required internal reason, expected report version and reviewed post version. An author edit after the queue is loaded rejects the stale decision until the reviewer refreshes. The post lock precedes the report lock, matching report creation lock order. Decision, immutable audit and optional post takedown commit in one PostgreSQL transaction. The same reviewer may retry the same completed decision/version/reason without another audit row; conflicting retries return 409. Dismissal never restores previously removed content. Takedown sets content_post.moderated_at and the shared PostAccess predicate denies subsequent feed, saved-post, search, activity, post-detail, reaction and comment operations, including reads by the author. Already delivered browser data requires refresh and cannot be retracted.

Local operator provisioning is explicit: obtain the fictional member's public UUID from their profile sharing link, then run `./scripts/local-moderator.sh grant MEMBER_PUBLIC_UUID`. Revoke with the same command using `revoke`. The script only targets the repository's local Compose PostgreSQL service; it accepts a validated UUID and uses psql variables without shell-interpolated SQL. Do not use this local script as a production identity/approval workflow. No moderator is seeded or granted automatically. Production operator approval, MFA/step-up policy, grant audit governance and access reviews remain pending.

Backend tests cover CSRF, duplicate reports, ownership, rate limits, inaccessible targets, privileged access, reviewer conflicts, decision replay, atomic audit, revocation and takedown propagation. `moderation.spec.ts` uses three real Keycloak accounts and real PostgreSQL, with a temporary trusted local role fixture that is revoked in cleanup. It verifies report persistence, ordinary-user denial, operator review, reporter status, feed/search/detail/comment takedown and revocation. Mobile dark-mode axe and overflow checks cover the reporter view. No successful API operation is mocked.

Current scope is post reports with removal or dismissal. Appeals/restoration, comments/member/media reports, automated abuse classification, reporter/author delivery notifications, queue assignment, operational alerting and production retention/legal policies remain unimplemented. Audit/report records and moderated post content are retained; no retention-compliance claim is made. Existing HTTP telemetry applies; dedicated moderation dashboards are pending. No new event stream is required for visibility because all current projections read authoritative PostgreSQL; future cached/search projections must implement takedown propagation before use.


## Private feed hiding

Members can hide a post from Recent and Following without reporting it or changing anyone else's access. V39 stores one private hidden_post row per caller/post. The desired-state endpoint requires authentication, CSRF and current post access, and uses idempotent insert/delete under the post lock. Hidden state is included in the owner's business-data export.

The Hidden posts feed scope lists only caller-hidden posts that are still readable under audience, deletion, moderation and bilateral-block rules. Restore removes the caller's preference. Saved posts, search and profile activity remain independent; hiding is a feed preference, not a privacy or moderation decision. Reads and mutation responses are no-store. Concurrent opposite desired-state writes are serialized, with the last committed state authoritative.

Tests cover private ownership, duplicate hide/restore, required state, CSRF, following-feed exclusion, saved/search independence and suppression after privacy changes, moderation or deletion. The two-user browser journey checks persistence across reload, no impact on the author, saved/hidden views, restoration and deleted-content filtering. The existing responsive platform suite covers the expanded feed controls.

V40 records the reviewed post revision in moderation audit records. Historical decisions made before revision tracking retain NULL; they are not assigned fabricated revision evidence. The browser moderation test edits the post after queue load, verifies rejection with the content still readable, then refreshes and completes the reviewed decision.

```mermaid
sequenceDiagram
 participant Member
 participant API
 participant DB as PostgreSQL
 participant Reviewer as Authorized reviewer
 Member->>API: Report readable post (UUID, category, details)
 API->>DB: Validate access and limits; persist report
 Reviewer->>API: Load restricted queue
 API->>DB: Verify operator grant; read current post revision
 Reviewer->>API: Decision + report/post versions + reason
 API->>DB: Lock grant, post, report; validate versions
 API->>DB: Commit decision, audit and optional takedown
 Member->>API: Feed/search/activity/comment read
 API->>DB: Apply current PostAccess predicate
 API-->>Member: Only currently authorized content
```
