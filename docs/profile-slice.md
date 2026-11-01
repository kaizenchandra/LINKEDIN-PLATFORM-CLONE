# Profile slice traceability

| Requirement | Source classification | Frontend | Backend/data | Contract | Evidence | Status |
|---|---|---|---|---|---|---|
| INT-PROFILE-001 | Explicitly required by uploaded prompt | Profile intro form | Subject-owned profile; V1 migration | getMyProfile/updateMyProfile | API and browser persistence tests | Integration verified |
| INT-PROFILE-EXP-001 | Explicitly required by uploaded prompt | Experience add/edit/delete | Subject-owned experience; V2 migration | listMyExperience/saveExperience/deleteExperience | Browser add/edit/delete and reload | Integration verified |
| BE-PROFILE-EXP-002 | Explicitly required by uploaded prompt | Error feedback | Owner constraints on every mutation | Experience operations | Cross-user update/delete rejection | Integration verified |
| BE-PROFILE-EXP-003 | Recommended implementation detail | Version carried in generated type | Compare-and-swap update/delete | version field | Stale update/delete and duplicate create tests | Integration verified |
| BE-PROFILE-EXP-004 | Explicitly required by uploaded prompt | Required fields, date input bounds | Length constraints, dates, database check | Experience schema | Invalid date-range test | Integration verified |
| FE-PROFILE-EXP-005 | Explicitly required by uploaded prompt | Cancel confirmation and browser unload warning | No mutation on cancel | N/A | Implemented; dedicated browser cancellation test pending | Implemented |

Experience currently includes title, organization text, start/end dates and description. Employment type, linked company/logo, location/type, associated skills/projects, media and downstream search/feed projections remain planned. This is not completion of the full experience specification. Education is implemented as described below. Certifications are implemented as described below. Projects are implemented as described below. Recommendations and other profile sections remain planned.

Profile section APIs remain self-only. An opt-in member-summary read now exposes only name, headline, location and about to authenticated members; see settings-slice.md for enforcement and remaining privacy gaps. Experience reads are not paginated; introduce explicit limits/pagination before exposing large imports. Version-zero PUT creates a client-generated UUID, returns version one, and rejects duplicate retries with 409 without creating duplicate rows. Update and delete also reject stale versions. Errors do not distinguish nonexistent entries from entries owned by other subjects.

The screenshots are still unavailable; no screenshot fidelity, accessibility audit or visual-regression result is claimed. UI uses the existing semantic theme variables and responsive layout.

## Education slice

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PROFILE-EDU-001 | Explicitly required by uploaded prompt | Education add/edit/delete form; subject-owned table; V3 migration; listMyEducation/saveEducation/deleteEducation contracts | Browser create/edit/delete and reload | Integration verified |
| BE-PROFILE-EDU-002 | Explicitly required by uploaded prompt | Owner checks on reads, updates and deletes | Cross-user denial tests against PostgreSQL | Integration verified |
| BE-PROFILE-EDU-003 | Recommended implementation detail | Client UUID; versioned writes/deletes; date-range checks | Duplicate creation, stale deletion and invalid-date tests | Integration verified |
| FE-PROFILE-EDU-004 | Explicitly required by uploaded prompt | Confirmed cancel discards unsaved edits | Browser edit/cancel keeps saved degree | Integration verified |

Fields: institution, degree, field of study, start date, optional end date and optional descriptive text. Empty end date displays “In progress”; expected future graduation dates are accepted when after the start date. Dates use calendar dates without time zones. Education currently belongs exclusively to the self-profile; public sharing, institution logos, search projection and linked institution entities remain unimplemented.

## Certifications slice

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PROFILE-CERT-001 | Explicitly required by uploaded prompt | Name, issuer, credential ID/URL, issue/expiry dates; CRUD UI/API; V4 migration | Browser create/edit/delete/reload | Integration verified |
| BE-PROFILE-CERT-002 | Explicitly required by uploaded prompt | Subject-owned reads and writes, version comparisons | PostgreSQL cross-user and stale-write tests | Integration verified |
| BE-PROFILE-CERT-003 | Recommended implementation detail | Optional absolute HTTPS URL, host required, no embedded credentials | Six unsafe URL variants rejected by API tests | Integration verified |
| FE-PROFILE-CERT-004 | Explicitly required by uploaded prompt | New-tab credential link with noopener/noreferrer | Browser link-attribute assertion | Integration verified |

Blank expiry date means no expiry. Expiry must not precede issue date, and issue date cannot be in the future. Credential IDs and URLs are optional empty strings. Links are user-provided references, not verification of credential authenticity; the backend never fetches them. Issuer directory integration, credential verification, expiry reminders, media and search projections remain unimplemented. HTTPS links can still lead to untrusted external sites; no reputation service is implemented.

## Projects slice

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PROFILE-PROJ-001 | Explicitly required by uploaded prompt | Project CRUD, description/dates/organization/skills; V5 migration; generated types | Browser create/edit/delete/reload | Integration verified |
| BE-PROFILE-PROJ-002 | Explicitly required by uploaded prompt | Owner-only reads/writes; date and version constraints | PostgreSQL parameterized invariant tests | Integration verified |
| FE-PROFILE-PROJ-003 | Inferred from interaction | Separate saved-but-refresh-failed state and manual reload | Browser transport-failure injection after real save | Integration verified |

Associated skills are currently stored as comma-separated display names, not references to a normalized skill catalog. Organization is optional free text, not a company relationship. Project media, collaborators, public visibility and search/feed propagation remain unimplemented. End date may be omitted for ongoing work. List pagination remains a gap.

Experience, education, certifications and projects now share `use-profile-section.ts` for loading, CSRF-protected mutations, unsaved-draft unload warnings, busy/error state and refresh recovery. Forms and backend invariants remain domain-specific. Failed mutations retain the draft; confirmed mutations whose subsequent read fails report that the change was saved and offer reload.

## Skills slice

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PROFILE-SKILL-001 | Explicitly required by uploaded prompt | Add/rename/remove skills; V6 migration; generated Skill contract | Browser CRUD and reload | Integration verified |
| BE-PROFILE-SKILL-002 | Recommended implementation detail | Case-insensitive per-profile uniqueness after trimming/collapsing whitespace | Duplicate and eight-writer concurrency tests | Integration verified |
| BE-PROFILE-SKILL-003 | Explicitly required by uploaded prompt | Subject-owned reads/writes and version-checked deletion | Cross-user and stale-version tests | Integration verified |

Skills are member-supplied names, sorted alphabetically. Unicode confusable/synonym normalization and a shared taxonomy are not implemented. Project-associated skill text is not automatically linked to this table. Endorsements, skill ordering/pinning, suggestions, public visibility, and search projections remain planned. Duplicate submission returns 409 with no duplicate rows; this is duplicate prevention, not replay of the original response.

## Languages slice

| Requirement | Source classification | Implementation | Evidence | Status |
|---|---|---|---|---|
| INT-PROFILE-LANG-001 | Explicitly required by uploaded prompt | Language CRUD and proficiency selector; V7 migration; generated Language type | Browser create/update/delete and reload | Integration verified |
| BE-PROFILE-LANG-002 | Explicitly required by uploaded prompt | Ownership, version checks, duplicate-name constraint | PostgreSQL cross-user, duplicate and stale-delete tests | Integration verified |
| BE-PROFILE-LANG-003 | Recommended implementation detail | Required proficiency enum and matching SQL CHECK | Null/unknown proficiency rejection tests | Integration verified |

Supported self-reported proficiency levels: elementary, limited working, professional working, full professional, native/bilingual. Language names are free text with the same whitespace/case normalization as skills. ISO language identifiers, localized aliases, third-party proficiency verification, ordering, public-profile visibility and search projections remain unimplemented. This section does not change application/content-language settings.
