# Screenshot-driven UI checkpoint

All 18 supplied screenshots are now present in `snapshot/`. IDs below follow chronological filenames, matching the original prompt's sequence. They replace the earlier discovery statement that screenshots were unavailable.

| Reference | Source file | Observed structure | Implementation coverage |
|---|---|---|---|
| S01 | [Screenshot 2026-11-01 at 12.51.47 AM.png](../snapshot/Screenshot 2026-11-01 at 12.51.47 AM.png) | Profile hero, overlapping avatar, wide content/narrow right column | Applied to profile/member pages |
| S02 | [Screenshot 2026-11-01 at 12.51.56 AM.png](../snapshot/Screenshot 2026-11-01 at 12.51.56 AM.png) | Account dropdown and profile link | Applied to account dropdown |
| S03 | [Screenshot 2026-11-01 at 12.52.03 AM.png](../snapshot/Screenshot 2026-11-01 at 12.52.03 AM.png) | Notification list, category tabs, sidebars | Connection/application inbox, All/Network/Jobs filters and sidebars implemented |
| S04 | [Screenshot 2026-11-01 at 12.52.08 AM.png](../snapshot/Screenshot 2026-11-01 at 12.52.08 AM.png) | Messaging split pane and conversation list | Direct-text list/thread/composer, name search and star/archive views implemented; mute and advanced actions pending |
| S05 | [Screenshot 2026-11-01 at 12.52.27 AM.png](../snapshot/Screenshot 2026-11-01 at 12.52.27 AM.png) | Job discovery cards and compact sidebar | Company-owned publication, discovery, saved/managed views and job details implemented; profile applications/tracking implemented; recommendations/resume uploads pending |
| S06 | [Screenshot 2026-11-01 at 12.52.53 AM.png](../snapshot/Screenshot 2026-11-01 at 12.52.53 AM.png) | Network management and invitation actions | Dedicated sidebar, Grow tab, invitations, search and connection actions implemented |
| S07 | [Screenshot 2026-11-01 at 12.53.08 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.08 AM.png) | Account settings navigation and grouped rows | Account/settings rail, grouped rows and persisted appearance implemented |
| S08 | [Screenshot 2026-11-01 at 12.53.14 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.14 AM.png) | Sign-in/security settings rows | Custom security page with identity-provider links and application-session management |
| S09 | [Screenshot 2026-11-01 at 12.53.22 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.22 AM.png) | Visibility settings groups | Visibility category integrates profile sharing, discovery and blocking |
| S10 | [Screenshot 2026-11-01 at 12.53.27 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.27 AM.png) | Data privacy settings groups | Pending data privacy module |
| S11 | [Screenshot 2026-11-01 at 12.53.35 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.35 AM.png) | Advertising preference rows | Pending advertising settings |
| S12 | [Screenshot 2026-11-01 at 12.53.42 AM.png](../snapshot/Screenshot 2026-11-01 at 12.53.42 AM.png) | Notification preference groups | Connection preference implemented; full category page pending |
| S13 | [Screenshot 2026-11-01 at 12.54.17 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.17 AM.png) | Profile analytics, about, featured cards | About card applied; analytics/featured pending |
| S14 | [Screenshot 2026-11-01 at 12.54.24 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.24 AM.png) | Experience entries and section actions | Applied to existing experience editor/cards |
| S15 | [Screenshot 2026-11-01 at 12.54.32 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.32 AM.png) | Education and section card rhythm | Applied to existing education editor/cards |
| S16 | [Screenshot 2026-11-01 at 12.54.39 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.39 AM.png) | Certifications and credentials | Applied to existing certification editor/cards |
| S17 | [Screenshot 2026-11-01 at 12.54.50 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.50 AM.png) | Projects and skills | Applied to existing projects/skills editor/cards |
| S18 | [Screenshot 2026-11-01 at 12.54.57 AM.png](../snapshot/Screenshot 2026-11-01 at 12.54.57 AM.png) | Recommendations and languages | Language cards styled; recommendations pending |

## Implemented design choices

UI-REF-001 (observed): compact sticky navigation with icon/label hierarchy, account dropdown, search field and active underline. The header now opens opt-in people search; the existing profile-section finder remains in the profile sidebar. Company/job/content search remains pending. Jobs is marked Soon and unavailable; messaging opens the direct-text inbox and notifications open the connection inbox. No invented counts, advertisements or private screenshot content are used.

UI-REF-002 (observed): warm-gray canvas, white bordered cards, blue primary/outlined actions, approximately 804/300 desktop columns with 24-pixel gutter, overlapping circular avatar and geometric cover. Meridian retains its own brand and initials avatars. User photo/media upload remains pending.

UI-REF-003 (observed/inferred): profile sections appear as individual cards, with compact action buttons and separate About content. Introduction editing uses a named native modal dialog, keyboard Escape, focus containment, confirmation before discarding edits, pending/error states and real persisted saves. Other section editors retain their integrated inline edit flows.

UI-REF-004 (explicit prompt requirement): responsive single-column layout at mobile width, light/dark/system theme tokens, reduced-motion handling, skip navigation and visible focus indicators. Account theme persistence remains backend-authoritative.

## Verification

Container production build and strict TypeScript passed. The existing real-service identity, profile and network journeys passed with updated selectors reflecting the new editor and account menu. The added design journey creates a fictional account through real Keycloak, saves its profile and experience to PostgreSQL, verifies cancel behavior and section navigation, checks mobile overflow, and captures desktop light/dark, mobile and mobile editor views. Those captures were visually inspected against the supplied references.

Axe checks with WCAG A/AA tags passed on desktop light, desktop dark and the mobile editor after correcting a missing document title. These automated checks do not establish complete WCAG conformance. No screenshot pixel-diff baseline or full manual assistive-technology audit is claimed. Screenshots use fictional test data and are regenerated under `apps/web/test-results`; no authentication secrets are captured in the design images.

25 backend test invocations passed during this UI checkpoint. All four browser journeys passed across the regression run and final targeted accessibility rerun. No backend authority was moved into presentation code. Full notification, jobs, messaging, feed and settings-page coverage still depends on the corresponding functional modules.

## Current coverage updates (November 1, 2026)

S06 now has the dedicated network page; S01 includes persisted profile-photo upload and public-summary access. S07–S09 now have the `/settings` category layout, reused persisted appearance/visibility/discovery controls and an integrated application-session security view. The S08 desktop light and mobile dark layouts were reviewed with passing axe/overflow checks. Data privacy/advertising categories remain unavailable, and provider-wide session controls and remaining identity capabilities remain backlog. Earlier table entries describe the original UI checkpoint.


## Platform-wide visual refinement — November 1, 2026

UI-REF-005 (observed from S01–S18; integration verified): `app/platform.css` applies the reference layout and density consistently across the existing pages, after the established component styles. It standardizes the 53px desktop header, 1128px content frame, 8px cards, neutral canvas, compact typography, blue actions, green selected filters, and responsive columns. The original Meridian branding and real user data remain; no LinkedIn logos, photos, ads, fabricated counts or placeholder business results were introduced.

- Shared navigation has a separate Business dropdown linking to actual company, recruiter, jobs and application routes; the existing Me dropdown remains independently operable.
- Feed uses a cover-backed member card, avatar/composer row, compact filter/sort controls and consistent post actions.
- Network uses denser invitation rows and navigation; notifications use grouped unread/read list styling and existing functional category filters.
- Messaging joins the conversation list and thread into a bordered split pane, adds a useful empty state and desktop resource rail, and stacks on mobile.
- Jobs uses a compact navigation rail, company marks and denser result rows. On mobile, job-view controls and discovery precede secondary preferences.
- Search, companies, applications, recruiter tools, profile sections and settings share the same form, card, heading and action styles. Welcome typography is aligned with the reference; existing real Keycloak login/registration styling remains.
- Light, explicit dark and system theme colors retain account persistence. System-dark reaction buttons and filled link buttons now have adequate text contrast.

Verification: `npm --prefix apps/web run typecheck` and container production builds passed. `./scripts/container-up.sh --profile observability` passed local health checks. `npx playwright test` passed all 23 real-service browser journeys. The new `platform-design.spec.ts` visits 10 routes at desktop light, mobile light and system-dark sizes, checks horizontal overflow and axe WCAG A/AA rules, and captures 30 screenshots. Desktop, mobile and dark contact sheets were visually reviewed. The mobile jobs ordering refinement received a targeted jobs/preferences/design rerun. Existing journey tests also exercise populated company/job details, profile editors, messaging, notifications and authentication.

This is reference-aligned presentation for implemented workflows, not pixel-identical reproduction of every LinkedIn screen. Data privacy/advertising pages, premium/ads, analytics, advanced composer tools and other unimplemented product requirements remain in `remaining-backlog.md`. Automated axe checks are not full WCAG certification; screenshot captures are not pixel-diff regression baselines. Backend code/contracts were unchanged, so backend unit/integration suites were not separately rerun in this UI checkpoint. No cloud deployment was performed.
