# Public and authenticated profile access

Implemented November 1, 2026. FE/BE/INT-PROFILE-PUBLIC-001: integration verified.

Migration V29 introduces explicit `public_visible` consent, default false for all existing and new profiles. Existing member-sharing consent never implies internet publication. Profile visibility settings persist both flags using optimistic concurrency; omitted publicVisible preserves its current value for older clients.

Anonymous GET /api/v1/profiles/{id} returns only name, headline, location and about for public profiles. Nonpublic and unknown profiles both return 401 to guests. The member page redirects to the real Keycloak sign-in screen and returns to the original profile after successful authentication. Return targets are server-session UUIDs, consumed on login; arbitrary redirect URLs are not accepted.

Authenticated visitors can view public or member-visible summaries, subject to either-direction blocks. Profiles with both settings disabled remain owner-only. Login does not bypass owner privacy or blocking. Other profile sections and contact details are not published. Guest views offer sign-in instead of authenticated social actions. Responses are not cached; refreshing after revocation requires login.

Verified: all 80 Maven tests passed; generated OpenAPI types and TypeScript check passed; Docker production builds and local health checks passed. Playwright public-profile.spec.ts verifies real registration, anonymous public access, revocation, Keycloak login and return. Existing visibility.spec.ts also passed including member access, blocking and revocation. The first new browser run used a synchronous checkbox assertion for an asynchronous save; corrected to click and await persisted state, then passed. Full browser suite was not rerun for this change.

No AWS deployment performed. Public sharing necessarily makes the summary available to anonymous visitors, including someone who signs out of a blocked account.
