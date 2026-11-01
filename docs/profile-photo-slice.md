# Profile photo upload

FE/BE/INT-PROFILE-PHOTO-001 implemented and integration verified on November 1, 2026.

The profile avatar is a keyboard-accessible edit button with a camera badge. It opens a focused photo dialog with a large circular preview, JPEG/PNG selection, cancel, save/replace, and confirmed removal. Unsaved selections require confirmation before dismissal, and closing restores focus to the avatar. Successful uploads display immediately and survive refresh. Shared member pages display the same photo; missing photos fall back to initials.

V30 stores normalized image bytes in PostgreSQL alongside the owner profile, with a size constraint. PUT /api/v1/me/photo accepts raw binary up to 2 MiB, bounds reads even without Content-Length, inspects actual image format and dimensions before decoding (4096 pixels per side, 16 megapixels), resizes to at most 512 pixels per side, and re-encodes PNG without source metadata. SVG and other formats are rejected. Owner-only mutations use existing OIDC sessions and CSRF. Replacement is last-successful-write-wins; deletion is idempotent.

GET /api/v1/me/photo is owner-only. GET /api/v1/profiles/{id}/photo enforces public consent, member sharing, owner access and either-direction blocking in the database query. It never exposes private photos through a public static URL. Responses use no-store, image/png, nosniff and restrictive CSP. Public profiles necessarily expose photos to anonymous visitors, including signed-out blocked accounts.

Validation executed:
- Targeted Maven ProfileIntegrationTest#profilePhotosPersistValidateAndRespectVisibility passed: authentication, CSRF, invalid content, size limits, normalized dimensions, persistence, ownership, anonymous/member restrictions, public access, blocking, deletion.
- Playwright profile-photo.spec.ts passed with real Keycloak, backend and PostgreSQL: upload, preview, refresh, guest privacy, public display and removal; axe and mobile overflow checks passed; mobile screenshot reviewed.
- Playwright public-profile.spec.ts passed as a regression check.
- OpenAPI generation/drift check and TypeScript checks passed.
- Docker production builds and local service health checks passed.

The initial browser image-dimension assertion ran before the image finished loading; it was changed to await decoding and passed. Full backend/browser suites were not rerun. Storage is PostgreSQL for this bounded slice; S3, cropping, EXIF orientation correction and distribution to feed/network avatar components remain future work. No AWS deployment performed.

## Photo dialog UX verification

The November 1 dialog update preserves the existing upload API and privacy rules. Client validation prevents unsupported files from being saved; failed uploads retain the selected preview for retry. Requests have a bounded timeout, with an explicit uncertain-outcome message rather than a false success.

- `npm --prefix apps/web run typecheck` passed.
- `./scripts/container-up.sh --profile observability` rebuilt the application and passed service health checks.
- `npx playwright test profile-photo.spec.ts design.spec.ts` in `apps/web` passed all three selected journeys (photo, profile design, authentication design).
- The photo journey verified keyboard opening/Escape/focus restoration, invalid input, unsaved-change cancellation, a deliberately aborted upload followed by successful real upload, persisted image retrieval after refresh, guest privacy, public display, and removal confirmation. Only the deliberate failure used request interception.
- Desktop and mobile dark dialog screenshots were reviewed; axe and mobile overflow assertions passed. The circular preview does not provide interactive cropping or zoom.

The full backend and browser suites were not rerun for this UI-only update.

## Private S3 storage checkpoint

The same dialog and authorization contract now support private S3 objects with V42. See [LocalStack/Terraform integration, recovery behavior and limitations](localstack-media.md). Existing PostgreSQL photos remain readable. Local emulation explicitly retains a PostgreSQL recovery copy; production S3 mode does not retain image bytes by default.
