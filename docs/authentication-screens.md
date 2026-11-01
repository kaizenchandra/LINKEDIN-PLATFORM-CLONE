# Login and registration screenshot implementation

Source folder: `snapshot/login-registration`. All seven images were inspected. This folder includes four non-authentication references in addition to the three authentication screens; they are not silently treated as login requirements.

| Reference | Filename time | Observed functionality | Current delivery |
|---|---|---|---|
| AUTH-S01 | 11.55.26 AM | Guest welcome, sign in, join, content/people/jobs navigation | Responsive Meridian welcome at `/`; original CSS illustration; real navigation and sign-in/join entry points |
| AUTH-S02 | 11.55.37 AM | Email/phone password login, password visibility, recovery, social providers, remembered sign-in | Branded real Keycloak email/password form; built-in show/hide and recovery; phone/social/remembered policy not implemented |
| AUTH-S03 | 11.56.05 AM | Registration, password visibility, join, existing-member navigation, provider and policy links | Direct OIDC registration entry; real Keycloak email/name/password/confirmation, verification and profile initialization; remaining items below |
| EXTRA-S01 | 11.58.32 AM | Job split-pane, filters, saved/apply, dismiss and matching | Existing job search/preferences/save/application workflows remain; exact split-pane/dismiss/ranking/premium scope pending |
| EXTRA-S02 | 11.58.52 AM | Events listing, create, online/live, premium events | Event domain/UI not implemented; retained in backlog |
| EXTRA-S03 | 11.59.05 AM | Followed company pages/counts/unfollow | Existing company following APIs/UI remain; screenshot-specific network list presentation pending |
| EXTRA-S04 | 11.59.16 AM | Newsletter subscriptions, creation and menus | Newsletter domain/UI not implemented; retained in backlog |

## Real frontend/backend integration

`/login` redirects to Spring's OIDC authorization entry. `/register` adds the allowlisted `screen=register` value. `AuthenticationRequests` changes only the generated authorization request's prompt to `create`. It preserves Spring-generated state, nonce, S256 challenge and fixed callback. All other screen values retain `prompt=login`; arbitrary redirect parameters are not accepted. See the official [Keycloak registration flow documentation](https://www.keycloak.org/docs/26.1.5/server_admin/) for `prompt=create`.

Credentials never pass through Next.js or a new application password endpoint. Keycloak retains registration validation/uniqueness, password checking, email verification, recovery, TOTP and credential persistence. Meridian initializes the subject-owned professional profile after verified authentication through the existing real API. PostgreSQL-backed application sessions, CSRF and private-profile return behavior remain intact.

The identity image is built from the pinned Keycloak 26.4.2 distribution. A narrowly scoped Theme Selector SPI selects `meridian` only for LOGIN pages in the meridian realm, delegating every other case to the built-in selector. This applies branding to existing local realms without importing over users, credentials or settings. The theme extends `keycloak.v2` and changes CSS, labels and footer only; native form actions, fields, hidden security state, accessibility behavior and password visibility scripts remain inherited. The provider compiles against the exact distribution jars during the Docker build. This follows the official [theme and Theme Selector SPI guidance](https://www.keycloak.org/ui-customization/themes).

`./scripts/container-up.sh --profile observability` builds and installs the theme/provider with the application. Local identity data volumes are preserved. Future Keycloak upgrades must rebuild and regression-test the theme/provider. There is no copied LinkedIn logo, private photo, advertisement or proprietary illustration.

## Traceability

| ID | Source classification | Contract/implementation | Acceptance evidence |
|---|---|---|---|
| FE-AUTH-WELCOME-001 | Observed AUTH-S01 | Welcome component, guest root and actual navigation | auth-design.spec.ts desktop/mobile capture and axe |
| INT-AUTH-REGISTER-002 | Observed AUTH-S03; recommended standards integration | `/register` → OIDC `prompt=create` → Keycloak → verified profile | Direct join, Mailpit verification and real persisted profile |
| BE-AUTH-REQUEST-003 | Recommended security implementation | AuthenticationRequests wrapper | Two tests: preserved state/nonce/PKCE/callback; unknown modes/null resolver |
| FE-AUTH-THEME-004 | Observed AUTH-S02/S03 | Inherited Keycloak forms plus meridian theme | Login/register visual, password visibility, mobile/system-dark and accessibility checks |
| INT-AUTH-REGRESSION-005 | Explicit uploaded prompts | Existing real identity workflows | identity.spec.ts registration/recovery/logout/TOTP journey |

## Deliberate limitations

- Email sign-in is implemented. Mobile identifiers/OTP/account linking remain a separate P0 identity extension requirement; the form does not misleadingly accept a phone number as a verified identity.
- Google and Apple require configured providers/client credentials and verified callback/domain configuration. No fake social-login buttons or simulated account creation are provided. The UI states their availability accurately.
- The screenshot's remembered-login checkbox is not implemented. Meridian retains its 30-minute activity-based application session policy and interactive reauthentication; no indefinite credential persistence is implied.
- No legal consent is fabricated. Terms/privacy/cookie policy content and versioned acceptance are still required before production; the registration action is labeled Register rather than claiming acceptance of nonexistent documents.
- First and last name and password confirmation remain Keycloak's required registration fields. MFA/recovery forms are visually inherited; they are not replaced with a second authentication protocol.
- Four non-authentication images remain scoped as shown in the table. Events/newsletters and premium/AI matching are not presented as completed features.

Verification results are recorded in the README checkpoint. No external identity providers or AWS services were configured.

## Final application verification

`./scripts/container-up.sh --profile observability` rebuilt all application/identity images and passed health checks. Final `./scripts/verify.sh` passed 86 backend invocations (78 domain/profile, two Kafka, four realtime, two new authorization-request tests), contract drift check, strict TypeScript and all 22 Playwright journeys. This includes registration/verification, recovery, TOTP, session revocation, private-profile login return and all previously integrated business workflows. Desktop/light and mobile/system-dark screenshots were reviewed; automated axe and overflow checks passed. An inherited Keycloak !important logo color and an obsolete logout-page test assertion were corrected before the final successful run; footer wrapping was corrected after visual inspection and reverified.

`./scripts/test-local.sh` also passed: isolated fresh realm import, readiness, container recreation and PostgreSQL persistence. Only that disposable test project's volumes were removed; normal development accounts/data were preserved.


## Native passkeys and recovery codes

INT-AUTH-PASSKEY-001 and INT-AUTH-RECOVERY-001 use the installed Keycloak 26.4.2 implementation. The local-only `LocalIdentityPolicyFactory` runs after realm import and applies a versioned policy once to the Meridian realm. `MERIDIAN_LOCAL_IDENTITY_POLICY=true` is set only in local Compose. It enables native passwordless WebAuthn with required user verification, discoverable credentials, localhost RP ID and 120-second enrollment timeout, plus recovery codes as an alternative factor in the browser flow. It does not replace users, credentials or the default browser flow. Do not enable this localhost policy in deployment environments; provision the correct HTTPS RP identity there.

The factory is compiled against the installed Keycloak distribution, following its [native passkey support](https://www.keycloak.org/2025/09/passkeys-support-26-4) and [recovery-code configuration](https://www.keycloak.org/docs/26.5.7/server_admin/index.html#_recovery-codes) guidance. The post-import hook was checked against the pinned 26.4.2 source. No custom credential protocol is implemented.

`passkeys.spec.ts` registers through real email verification, enrolls with the browser's virtual CTAP2 authenticator, signs in without entering a password, revokes the credential in the account UI and verifies protected access is not granted afterward. This is actual Keycloak/WebAuthn integration using a virtual device, not hardware-device certification. `recovery-codes.spec.ts` enrolls native codes, requires a password first, uses a code to sign in, rejects reuse, and accepts the next code. Codes stay in test memory; traces, video and screenshots are disabled for these tests. Physical devices/cross-device sync, recovery-code regeneration and comprehensive step-up/lost-device policies remain unverified. Mobile OTP, identifier linking, security notifications and back-channel logout remain backlog.
