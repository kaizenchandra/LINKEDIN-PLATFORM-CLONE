# MASTER PROMPT — ENTERPRISE-GRADE LINKEDIN-INSPIRED PLATFORM
## Complete End-to-End Design, Full-Stack Development, Integration, Testing, and Cloud Deployment

### 1. Role and Mission

Act as a cross-functional engineering team consisting of:

- Principal Solution Architect.
- Distinguished Backend Engineer.
- Principal Frontend Architect.
- Lead UI/UX and Design Systems Engineer.
- Domain-Driven Design Architect.
- Distributed Systems and Event-Driven Architecture Expert.
- Cloud-Native AWS Solutions Architect.
- Application Security and Identity Architect.
- DevSecOps and Platform Engineer.
- Site Reliability Engineer.
- Software Quality and Performance Engineering Lead.

Your task is to **design, implement, integrate, test, deploy, and document a complete LinkedIn-inspired professional networking platform** using the two uploaded master prompts and all available LinkedIn website screenshots.

You must deliver one cohesive, working full-stack application consisting of:

1. A modern, elegant, responsive frontend.
2. A secure, domain-driven backend.
3. Fully integrated frontend-to-backend workflows.
4. Functional account registration and authentication.
5. Persistent business data and transaction processing.
6. Reliable event-driven communication.
7. Real-time messaging and notifications.
8. Reproducible local development infrastructure.
9. Automated functional, integration, contract, security, and E2E tests.
10. Cloud-native deployment configuration.
11. Observability, reliability, and security controls.
12. CI/CD and production-readiness evidence.

**This is an implementation task, not merely an architecture design, requirements document, proof of concept, or static UI prototype.**

Implement the system incrementally, completing and verifying working end-to-end vertical slices.

Do not stop after producing a technical design document.

---

### 2. Authoritative Input Documents

You have two uploaded documents:

**Document A — Frontend Development Master Prompt**

Contains:

- Screenshot-driven UI/UX extraction.
- Complete frontend functional requirements.
- Modern React/Next.js architecture.
- Design system.
- Professional profile interfaces.
- Home feed.
- Network management.
- Messaging.
- Jobs.
- Notifications.
- Settings and privacy.
- Light/dark/system themes.
- Responsive design.
- Accessibility.
- Frontend validation and testing.
- Production-grade UI engineering.

**Document B — Backend Development Master Prompt**

Contains:

- Backend functional requirements.
- LinkedIn-inspired technology architecture.
- Java 21 and Spring Boot.
- Domain-Driven Design.
- Clean/Hexagonal Architecture.
- Microservices.
- Kafka and event-driven architecture.
- gRPC/Protobuf.
- PostgreSQL, Redis, and search infrastructure.
- Authentication, OTP, MFA, and passkeys.
- Professional networking domains.
- Security and distributed-system correctness.
- AWS cloud-native infrastructure.
- Observability and SRE.
- CI/CD and automated testing.
- Production-readiness criteria.

Also analyze the **18 previously uploaded LinkedIn website screenshots**.

Treat the documents as complementary requirements that must be implemented together.

#### Requirements Consolidation

Before implementation:

1. Read both documents completely.
2. Extract all frontend and backend requirements.
3. Remove duplicates without losing functionality.
4. Identify conflicting requirements.
5. Map frontend functionality to backend capabilities.
6. Identify missing API contracts.
7. Identify missing business workflows.
8. Identify missing state transitions.
9. Identify missing security and authorization requirements.
10. Identify integration dependencies and operational requirements.

Create a unified requirement catalog with stable identifiers.

Use source classifications:

- Observed from screenshot.
- Explicitly required by uploaded prompt.
- Inferred from interaction.
- Recommended implementation detail.
- Unresolved/unknown.

Do not silently remove requirements from either document.

---

### 3. Primary Engineering Objectives

The platform must satisfy the following objectives.

**Functional completeness**

All P0 workflows must be executable from frontend through backend to persistent storage.

**Visual fidelity**

The frontend should closely follow the supplied screenshots' layout, navigation structure, information hierarchy, and interaction patterns while using an original visual identity.

**Backend correctness**

All important business rules must be enforced server-side.

**Security**

Authentication, authorization, verification, sessions, private data, and sensitive operations must be secure by design.

**Data consistency**

Frontend state must reflect authoritative backend data. Distributed updates must have explicit consistency guarantees.

**Reliability**

Expected failure scenarios must have defined error handling and recovery.

**Maintainability**

Apply DDD, Clean/Hexagonal Architecture, SOLID, and reusable component principles pragmatically.

**Observability**

Critical workflows must be traceable across frontend, APIs, services, Kafka, and persistence.

**Performance**

Establish measurable latency, throughput, and frontend Core Web Vitals targets.

**Deployment**

The application must support local development, automated testing, staging, and controlled production deployment to AWS.

---

### 4. Source-of-Truth and Repository Rules

Before editing code:

- Inspect the repository structure.
- Read repository instructions.
- Identify existing frontend/backend modules.
- Inspect package manifests and build files.
- Review current dependency versions.
- Inspect existing database migrations.
- Identify API and event contracts.
- Check existing CI/CD pipelines.
- Determine local development tooling.
- Review current tests.
- Preserve unrelated changes.

Follow existing architectural conventions unless they conflict with a significant requirement or engineering correctness.

For a new project, use the reference stack in this prompt.

For an existing project, adapt the implementation to its established stack.

Make routine choices independently.

Ask questions only when ambiguity materially affects correctness, security, architecture, compatibility, or operational risk.

---

### 5. Technology Stack and Architecture

#### 5.1 Frontend Stack

Use compatible, supported versions of:

| Layer | Technology |
|---|---|
| Web framework | Next.js App Router |
| UI | React |
| Language | TypeScript strict mode |
| Styling | Tailwind CSS |
| Design system | shadcn/ui and accessible primitives |
| Icons | Lucide React |
| Server state | TanStack Query when appropriate |
| Forms | React Hook Form |
| Validation | Zod |
| Authentication integration | OIDC-based secure session/BFF |
| API contracts | OpenAPI-generated TypeScript types |
| Real-time client communication | WebSockets where justified |
| Component testing | Vitest and React Testing Library |
| E2E testing | Playwright |
| Accessibility testing | axe-core |
| UI validation | Visual regression testing |

Avoid unnecessary frontend dependencies and duplicate state-management systems.

#### 5.2 Backend Stack

Use:

| Layer | Technology |
|---|---|
| Primary language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security |
| Identity provider | Keycloak |
| Public APIs | REST/OpenAPI |
| Internal communication | gRPC/Protocol Buffers where justified |
| Event streaming | Apache Kafka |
| Relational persistence | PostgreSQL |
| ORM | Hibernate/Spring Data JPA where appropriate |
| Complex SQL | jOOQ/JDBC where justified |
| Distributed cache | Redis or Valkey |
| Local cache | Caffeine |
| Search | OpenSearch |
| Object storage | S3-compatible storage |
| CDC | Debezium where appropriate |
| Reliability | Resilience4j |
| Database migrations | Flyway or Liquibase |
| Integration tests | Testcontainers |
| Observability | OpenTelemetry |

Use current official documentation to verify compatibility before choosing exact dependency versions.

#### 5.3 LinkedIn Architectural Reference

Research LinkedIn's publicly documented engineering practices and open-source ecosystem.

Evaluate:

- Apache Kafka.
- Apache Pinot.
- Venice.
- Apache Samza.
- Apache Helix.
- Brooklin.
- Ambry.
- gRPC/Protobuf.
- Lucene-based search technology.

Use a verified technology where the workload justifies it.

For proprietary or unsuitable technology, choose a maintained open-source substitute.

Distinguish historical LinkedIn usage, currently verified usage, and project-specific recommendations.

Do not claim to reproduce LinkedIn's private infrastructure exactly.

Do not introduce every LinkedIn-associated technology merely for architectural similarity.

#### 5.4 Cloud and Infrastructure

Use AWS as the target deployment platform.

Evaluate:

- Amazon EKS.
- Amazon MSK.
- Amazon RDS/Aurora PostgreSQL.
- Amazon ElastiCache.
- Amazon S3.
- Amazon OpenSearch Service.
- AWS Load Balancing.
- CloudFront.
- WAF.
- IAM.
- KMS.
- Secrets Manager.
- CloudWatch.
- Route 53.
- VPC networking.

Use Terraform as the default infrastructure-as-code approach for a new project, unless an existing solution is already established.

---

### 6. Unified End-to-End Architecture

Design one coherent application architecture.

Expected logical flow:

**Browser / Responsive Frontend → Next.js BFF / API Gateway → Domain Services → Databases / Kafka / Caches / Search / Media Storage**

Identity is handled through a trusted OIDC identity provider.

Long-running and asynchronous processing should use dedicated workers or event consumers where necessary.

The architecture must define:

- Public frontend entry points.
- Authentication boundaries.
- Browser/BFF session ownership.
- API gateway responsibilities.
- Business service boundaries.
- Database ownership.
- Synchronous service dependencies.
- Asynchronous event flows.
- Authorization enforcement.
- Error propagation.
- Distributed tracing.
- Deployment boundaries.

Do not expose internal gRPC endpoints or private databases directly to browsers.

Do not make frontend code depend on Kafka or database schemas.

Avoid exposing internal service topology unnecessarily through public API contracts.

#### 6.1 BFF and API Gateway Responsibilities

Evaluate an application-specific BFF where it materially simplifies:

- Secure browser sessions.
- API aggregation.
- Frontend-oriented DTOs.
- Request orchestration.
- CSRF protection.
- Authentication redirects.
- Cookie management.
- Response normalization.
- Cache control.

Avoid duplicating business logic and authorization decisions across the BFF and domain services.

#### 6.2 Contract-First Integration

Create and maintain authoritative contracts before implementing dependent UI behavior.

Use:

- OpenAPI for frontend-facing REST APIs.
- Protocol Buffers for selected internal gRPC operations.
- Schema-controlled Kafka event contracts.
- Shared error conventions.
- Explicit authentication and authorization semantics.

Generate TypeScript clients/types from authoritative OpenAPI specifications where practical.

Backend and frontend must not independently invent incompatible request/response models.

---

### 7. Unified Functional Modules

Implement the product domains from both uploaded documents.

| Module | Frontend responsibility | Backend responsibility |
|---|---|---|
| Identity | Registration, login, verification, MFA, recovery | Identity orchestration, credentials, sessions, verification |
| Profile | Display/edit professional identity | Profile persistence, validation, authorization |
| Network | Connections, invitations, follow actions | Social graph invariants, relationships, state transitions |
| Feed | Posts, reactions, comments, timeline | Content management, feed generation, ranking |
| Messaging | Conversations, composer, live updates | Durable messages, ordering, delivery, unread state |
| Jobs | Discovery, filters, saved jobs, applications | Job lifecycle, matching, applications |
| Organizations | Company pages and administration | Organization ownership, roles, jobs, followers |
| Search | Global search and filtering | Indexing, retrieval, ranking, privacy filtering |
| Notifications | Lists, filters, unread state | Event generation, delivery, preference enforcement |
| Settings | Account, privacy, security, preferences | Preference storage and cross-domain enforcement |
| Media | Upload/preview/manage files | Secure object storage and processing |
| Analytics | Profile and activity metrics | Event aggregation and privacy-aware analytics |
| Moderation | Report and review workflows | Abuse prevention, moderation, audit |

Implement these as coherent vertical slices, not isolated page and API demonstrations.

---

### 8. Mandatory P0 — Full Authentication Integration

Authentication is the foundation of the application.

Implement all critical flows across frontend, identity provider, backend, and persistence.

#### 8.1 Email Registration

End-to-end flow:

1. User opens registration page.
2. User enters email and password.
3. Frontend validates required fields.
4. Registration request reaches trusted identity boundary.
5. Backend/identity provider validates identity rules and uniqueness.
6. A pending account is created.
7. Verification email is generated.
8. User opens the verification link or enters a verification code.
9. Verification is validated and consumed exactly once.
10. Account transitions to the verified/active state.
11. Professional profile initialization occurs.
12. User can sign in and continue onboarding.

Handle duplicate accounts, expired verification, resend, delivery failure, and interrupted registration.

#### 8.2 Mobile Registration with OTP

Implement registration using a mobile number and password.

Flow:

1. Select country code.
2. Enter mobile number.
3. Normalize to E.164.
4. Validate phone format and registration rules.
5. Create pending registration.
6. Generate a secure OTP.
7. Deliver via an SMS provider adapter.
8. User enters the OTP.
9. Backend validates OTP, expiry, purpose, and attempt limits.
10. Atomically consume the OTP.
11. Mark the mobile number verified.
12. Activate the account and initialize the profile.

Provide OTP resend with cooldown and throttling.

Never persist or log raw OTP values unnecessarily.

For local development, use a protected development-only SMS simulator.

#### 8.3 Email/Password Login

Implement secure login using the selected identity provider.

Include:

- Invalid credential handling.
- Verification requirements.
- MFA challenge.
- Secure session creation.
- Authenticated redirect.
- Session expiry.
- Logout.
- Account lockout/throttling.
- Audit events.

#### 8.4 Mobile/Password Login

Support login using a verified mobile number and password.

Resolve the mobile identifier to the canonical identity securely.

Do not create a duplicate account/profile.

#### 8.5 MFA

Implement supported MFA capabilities:

- TOTP authenticator.
- Recovery codes.
- Passkeys/WebAuthn where supported.
- Step-up authentication.
- MFA management.
- Recovery flows.
- Trusted-device policy where appropriate.
- Restricted SMS fallback if explicitly permitted.

Use standards-based implementations.

Do not design custom cryptographic authentication protocols.

#### 8.6 Account Linking

Support:

- Add email to mobile account.
- Add mobile to email account.
- Verify new identifier.
- Change primary identifier.
- Remove secondary identifier.
- Account-link conflict handling.
- Reauthentication for sensitive operations.

#### 8.7 Password Recovery

Implement:

- Request reset.
- Generic account-enumeration-safe response.
- Secure recovery challenge.
- Expiration.
- Single use.
- New password validation.
- Session invalidation policy.
- Security notification.
- Audit record.

#### 8.8 Session Management

Support:

- Secure cookies/session storage.
- Session listing.
- Logout.
- Logout all devices.
- Session revocation.
- Timeout.
- Refresh-token rotation where applicable.
- Compromised-session response.

Prefer OIDC Authorization Code with PKCE and appropriate browser/BFF session handling.

Avoid storing long-lived sensitive tokens in browser local storage.

Use CSRF protection where the authentication architecture requires it.

#### 8.9 Mandatory Authentication E2E Verification

Verify all critical paths from actual frontend UI to the configured identity provider and backend:

- Email registration and verification.
- Mobile registration and OTP verification.
- Email/password login.
- Mobile/password login.
- MFA challenge.
- Password recovery.
- Session persistence.
- Logout/session revocation.
- Unauthorized API access.
- Expired OTP.
- Duplicate registration.
- Incorrect credentials.

These tests must use real local services and persistence, with only external email/SMS transport replaced by explicit test adapters when necessary.

---

### 9. Professional Profile — Full-Stack Development

Use screenshot references S01 and S13–S18.

Implement:

- Profile header.
- Cover/banner image.
- Avatar.
- Name.
- Headline.
- Current organization.
- Location.
- Contact information.
- Profile URL.
- Connection count.
- Open-to-work preferences.
- Verification indicators.
- Profile actions.
- Analytics.
- About.
- Featured.
- Experience.
- Education.
- Certifications.
- Projects.
- Skills.
- Endorsements.
- Recommendations.
- Languages.

Every editable field/section must connect to the authoritative backend.

Requirements:

- Create/read/update/delete where appropriate.
- Proper validation.
- Save/cancel.
- Unsaved-change handling.
- Loading states.
- API error handling.
- Object-level authorization.
- Optimistic concurrency where useful.
- Data refresh/cache invalidation.
- Cross-page consistency.
- Search/feed projection propagation.

When a user updates a profile section, a refresh must retrieve the persisted value rather than reverting to a hard-coded fixture.

---

### 10. My Network — Full-Stack Development

Use S06.

Implement:

- Send invitation.
- Accept.
- Ignore.
- Withdraw.
- Remove connection.
- Follow/unfollow.
- List connections.
- Mutual connections.
- Suggested members.
- Pending invitations.
- Network counts.
- Search/filtering.

Enforce server-side rules:

- No self-connections.
- No duplicate active invitations.
- Only recipient may accept.
- No contradictory invitation state.
- Blocking restrictions.
- Idempotent retry outcomes.
- Concurrency-safe state transitions.

When an invitation is accepted:

- Backend state must update.
- Invitation list must refresh.
- Connection list/count must update.
- Relevant notifications must be generated if enabled.
- Duplicate requests must not create duplicate relationships.

---

### 11. Content Publishing and Feed

Implement:

- Feed retrieval.
- Personalized/recent feed.
- Create post.
- Edit/delete post.
- Reactions.
- Comments.
- Replies.
- Reposts.
- Save/unsave.
- Follow authors.
- Mentions.
- Hashtags.
- Media attachments.
- Content moderation/reporting.

Use reliable publication of appropriate events.

Define feed materialization and ranking policies.

Frontend behavior must reconcile with confirmed backend state.

An optimistic reaction must revert or reconcile after an API failure.

Deleted or private content must not remain visible through stale feed caches without appropriate visibility enforcement.

---

### 12. Messaging and Real-Time Integration

Use S04.

Build a functional conversation experience.

Frontend:

- Conversation list.
- Search.
- Filters.
- Selected conversation.
- Compose.
- Send.
- Read/unread.
- Star/archive/mute.
- Timestamps.
- Responsive messaging layout.
- Reconnection feedback.

Backend:

- Conversation ownership.
- Participants.
- Message persistence.
- Ordering within conversation.
- Idempotent sends.
- Read/delivery acknowledgment.
- Unread counts.
- Pagination.
- Offline recovery.
- Blocking permissions.
- Backpressure handling.

Real-time connection:

**Frontend WebSocket Client → Authorized Real-Time Endpoint → Messaging Service → Persistent Store → Delivery Events**

Use Kafka internally where beneficial.

Never make Kafka the direct browser transport.

#### Mandatory Integration Test

Using two test users:

1. User A signs in.
2. User B signs in from another browser context.
3. User A sends a message.
4. Backend persists the message.
5. User B receives the update.
6. User B opens the conversation.
7. Read status updates.
8. User A sees the appropriate read state.
9. Disconnect/reconnect is simulated.
10. Missed messages are recovered without duplication.

---

### 13. Jobs and Applications

Use S05.

Implement:

- Job recommendations.
- Job search.
- Filters.
- Preferences.
- Saved jobs.
- Dismissed recommendations.
- Job details.
- Job tracker.
- Resume uploads.
- Internal applications.
- Application status.
- Job alerts.
- Company-owned postings.
- Authorized recruiter actions.

Job application end-to-end:

1. User opens job.
2. User selects Apply.
3. Frontend loads application requirements.
4. User completes form.
5. Resume is uploaded securely.
6. Backend validates the submission.
7. Duplicate applications are prevented.
8. Application is persisted.
9. Confirmation is returned.
10. Tracker reflects status.
11. Relevant notification/event is generated.

Do not show successful submission if backend persistence fails.

---

### 14. Notifications

Use S03 and S12.

Implement:

- Notification list.
- All/Jobs/My Posts/Mentions filters.
- Read/unread state.
- Unread count.
- Open notification.
- Contextual actions.
- Preferences.
- Real-time notification updates where appropriate.

Backend notifications should be generated from valid domain events.

Handle event deduplication, authorization, delivery preferences, deleted targets, and read-state synchronization.

---

### 15. Settings, Security, and Privacy

Use S07–S12.

Implement all major settings categories:

**Account Preferences**
- Name/location/industry.
- Language.
- Content language.
- Theme.
- Video autoplay.
- Sound preferences.

**Sign In and Security**
- Email addresses.
- Mobile numbers.
- Password.
- MFA.
- Passkeys.
- Active sessions.
- Remembered devices.

**Visibility**
- Public profile.
- Email visibility.
- Follow/connection visibility.
- Profile-viewing mode.
- Discoverability.

**Data Privacy**
- Consent.
- Data export.
- Search history.
- Research/data-processing preferences.
- Account deactivation/deletion.

**Advertising Data**
- Relevant profile-data preferences and permitted advertising controls.

**Notifications**
- Category/channel preferences.

Every account-synchronized setting must persist to the backend.

Privacy settings must affect actual backend authorization and data filtering—not merely hide frontend controls.

Test cross-session persistence.

---

### 16. Search, Companies, Media, Analytics, and Moderation

Implement the requirements in both source prompts for:

- Global search.
- People search.
- Company search.
- Jobs search.
- Content search.
- Search indexing.
- Organization pages.
- Organization administrators.
- Company followers.
- Company posts/jobs.
- Secure profile and document media.
- Profile analytics.
- Search appearances.
- Post impressions.
- Reporting and moderation.

Use OpenSearch where justified.

Use PostgreSQL as source of truth for transactional business records unless a documented alternative is selected.

Use asynchronous projection updates when suitable, with rebuild/reconciliation capabilities.

---

### 17. Frontend Design System and UI/UX Quality

Build an original, premium professional design.

Match the screenshots' observed information hierarchy, layout, interaction structure, and relative component proportions.

Implement:

- Shared top navigation.
- Global search.
- Profile dropdown.
- Multi-column layouts.
- Profile cards.
- Settings panels.
- Messaging split panes.
- Notification lists.
- Job cards.
- Network invitation cards.
- Reusable dialogs/drawers.
- Loading skeletons.
- Toasts.
- Empty states.
- Error states.

Use semantic design tokens.

Provide:

- Light mode.
- Dark mode.
- System mode.
- Persistent theme preference.
- Accessible contrast.
- Consistent focus indicators.
- Theme-aware overlays.

Support mobile, tablet, desktop, and wide desktop.

Target WCAG 2.2 AA.

Respect `prefers-reduced-motion`.

Do not rely on color alone for status.

Avoid copying LinkedIn's actual logos, user photos, private content, or proprietary advertising imagery.

---

### 18. Shared API Contracts and Type Safety

Establish a contract-first integration workflow.

Required deliverables:

- OpenAPI specifications.
- TypeScript API client/types.
- Java request/response DTOs aligned with contracts.
- Standardized error model.
- Authentication requirements.
- Object-level authorization rules.
- Pagination conventions.
- Idempotency conventions.
- API versioning.
- Contract compatibility checks.

For each operation define:

| Contract element | Requirement |
|---|---|
| API identifier | Stable reference |
| HTTP method/path | Resource-oriented |
| Request | Validated schema |
| Response | Typed schema |
| Authentication | Required identity context |
| Authorization | Role/resource ownership |
| Errors | Standard code and message |
| Idempotency | Duplicate-request behavior |
| Consistency | Read-your-writes/eventual |
| Pagination | Cursor/filter/sort |
| Observability | Trace/correlation ID |
| Tests | Contract and integration tests |

Use generated types to prevent frontend/backend drift.

#### Error Handling

Standardize:

- 400 — invalid request.
- 401 — unauthenticated.
- 403 — forbidden.
- 404 — resource unavailable.
- 409 — conflict.
- 422 — semantic validation error where appropriate.
- 429 — rate limited.
- 5xx — server/dependency failure.

Use an RFC 9457-style problem response where appropriate.

Do not expose stack traces or secrets to clients.

---

### 19. Shared State, Caching, and Consistency

Define clear ownership:

**Backend**
- Authoritative business state.
- Identity/authorization.
- Transactions.
- Domain rules.
- Persistent data.

**Frontend**
- Form state.
- UI state.
- Navigation state.
- Cached server responses.
- Optimistic presentation.

Avoid duplicating business authority in frontend code.

Use TanStack Query or an equivalent established mechanism where appropriate for:

- Caching.
- Invalidation.
- Background refetching.
- Optimistic updates.
- Failure rollback.
- Pagination.
- Request deduplication.

For every mutation define:

- Pending state.
- Confirmed state.
- Failed state.
- Reconciliation strategy.
- Cache invalidation.
- Cross-component update behavior.

Privacy and security-sensitive decisions must always be enforced by the backend.

---

### 20. Event-Driven Architecture

Use Kafka for suitable asynchronous workflows:

- Profile updates to search indexing.
- Connection acceptance to notifications.
- Post publishing to feed projections.
- Job publication to search.
- Messaging-related downstream events.
- Analytics event aggregation.
- Account deletion propagation.

Implement:

- Domain/integration event separation.
- Schema versioning.
- Partition keys.
- Defined ordering scope.
- At-least-once delivery handling.
- Idempotent consumers.
- Retry topics or equivalent retry orchestration.
- Poison-message/DLQ handling.
- Transactional outbox where required.
- Consumer lag monitoring.
- Replay/rebuild.
- Event retention.

Do not claim exactly-once effects across independent persistence systems without proof.

Do not publish passwords, OTPs, recovery secrets, or private message bodies into general-purpose event streams.

---

### 21. Database and Transaction Architecture

Design databases around business ownership and invariants.

Include:

- Schema migrations.
- Unique constraints.
- Foreign keys where ownership permits.
- Indexing.
- Transaction isolation.
- Optimistic locking.
- Concurrency handling.
- Consistent identifiers.
- Audit records.
- Soft/hard delete policies.
- Retention.
- Backup/recovery.

Use local ACID transactions wherever possible.

Do not rely on distributed transactions for ordinary business operations.

For independent services, avoid shared mutable schemas and cross-service foreign-key assumptions.

Use reliable messaging for downstream state synchronization.

---

### 22. Local Development and Full-Stack Environment

Provide a reproducible development environment.

A developer must be able to run the application locally without production AWS credentials.

Use Docker Compose and suitable local emulators.

Include the required subset of:

- Frontend application.
- Backend services.
- Keycloak.
- PostgreSQL.
- Kafka.
- Redis/Valkey.
- OpenSearch.
- S3-compatible object storage.
- Local email testing service.
- Development-only SMS/OTP test adapter.
- OpenTelemetry Collector.
- Prometheus/Grafana.

Use LocalStack only for supported AWS emulation capabilities.

Provide:

- Example environment configuration.
- Local startup command.
- Health checks.
- Database migrations.
- Test users.
- Seed data.
- Test fixtures.
- Reset/reseed commands.
- Service readiness checks.
- Port and dependency documentation.

Do not hard-code production credentials or use committed real customer data.

#### One-Command Development Goal

Provide an appropriate developer command or documented short sequence that:

1. Starts dependencies.
2. Initializes databases.
3. Starts backend services.
4. Starts frontend.
5. Verifies service health.
6. Makes the application available for local E2E testing.

Do not claim a one-command environment works without executing it.

---

### 23. End-to-End Testing Strategy

Tests must verify actual integration between frontend, identity provider, backend services, and persistence.

#### 23.1 Backend Tests

Use JUnit 5 and Testcontainers as appropriate.

Cover:

- Domain invariants.
- Validation.
- Security.
- Database transactions.
- Concurrency.
- Kafka.
- Idempotency.
- Retry behavior.
- API contracts.
- Event contracts.

#### 23.2 Frontend Tests

Use Vitest and React Testing Library.

Cover:

- Forms.
- Validation.
- Components.
- Navigation.
- State transitions.
- Error handling.
- Theme behavior.
- Accessibility.

#### 23.3 Contract Tests

Prevent drift between Java backend contracts and TypeScript clients.

Validate:

- OpenAPI.
- Protobuf.
- Kafka event schemas.
- Compatible API changes.
- Standard error responses.

#### 23.4 Full-Stack Playwright E2E Tests

Test at least these journeys:

| ID | End-to-end workflow |
|---|---|
| E2E-001 | Email registration → verification → login → profile |
| E2E-002 | Mobile registration → SMS OTP → login |
| E2E-003 | MFA enrollment → MFA login |
| E2E-004 | Password recovery → new password login |
| E2E-005 | Profile edit → backend persistence → refresh |
| E2E-006 | Connection request → acceptance → updated network |
| E2E-007 | Create post → feed → comment/reaction |
| E2E-008 | Message send → second user receives |
| E2E-009 | Job search → save → application tracking |
| E2E-010 | Domain event → notification → mark read |
| E2E-011 | Privacy update → unauthorized visibility blocked |
| E2E-012 | Theme toggle → persistence across navigation |
| E2E-013 | Session expiration → protected route enforcement |
| E2E-014 | User data export/deletion lifecycle where implemented |

These tests must use the real local backend for integration validation.

Mock only external providers when necessary and disclose that substitution.

#### 23.5 Failure and Regression Tests

Cover:

- Expired OTP.
- Duplicate OTP use.
- Wrong password.
- Session expiry.
- Unauthorized object access.
- API timeout.
- Duplicate invitation acceptance.
- Duplicate message send.
- Concurrent profile edit.
- Kafka redelivery.
- Database failure.
- Cache failure.
- Search indexing delay.
- Invalid job application.
- Media upload failure.
- Offline/reconnect.
- Schema incompatibility.

#### 23.6 Visual Regression

Compare screenshots against the uploaded references at comparable viewport dimensions.

Review:

- Header.
- Page widths.
- Sidebars.
- Card hierarchy.
- Spacing.
- Typography.
- Active states.
- Dropdowns.
- Messaging layout.
- Profile hero.
- Settings layout.

Also capture mobile and dark-mode screenshots.

---

### 24. Security Requirements

Apply secure development principles across the system.

Include:

- OIDC-based identity integration.
- Strong credential storage.
- MFA and passkeys.
- Least privilege.
- Object-level authorization.
- Field-level privacy.
- CSRF protection where applicable.
- XSS prevention.
- Safe redirect validation.
- Secure file uploads.
- Rate limiting.
- Brute-force protection.
- Credential stuffing mitigations.
- Session revocation.
- Secrets management.
- Audit logging.
- Data minimization.
- Encryption in transit and at rest.
- Account recovery abuse prevention.
- PII masking/redaction.

Use OWASP ASVS and OWASP API Security guidance as applicable.

Threat-model registration, account recovery, mobile verification, profile access, messaging, file uploads, and administrative operations.

Do not treat client-side route guards as authorization enforcement.

---

### 25. Reliability and Resilience

Implement relevant:

- Timeouts.
- Bounded retries with jitter.
- Circuit breakers.
- Bulkheads.
- Rate limiting.
- Backpressure.
- Load shedding.
- Idempotency.
- Graceful shutdown.
- Readiness/liveness probes.
- Fallbacks where safe.
- Recovery after partial failures.

For every critical dependency, define failure impact and recovery.

Examples:

- SMS provider unavailable.
- Keycloak unavailable.
- PostgreSQL failover.
- Kafka broker unavailable.
- Redis outage.
- Search cluster failure.
- WebSocket disconnection.
- Expired credentials.
- Event consumer lag.
- Outbox publication delay.

Do not retry non-idempotent operations without duplicate-prevention guarantees.

---

### 26. Observability and Distributed Tracing

Implement telemetry across the whole user journey.

Example trace:

**Frontend interaction → BFF/API request → backend service → database/Kafka → downstream consumer → notification/message update**

Use:

- W3C trace context.
- OpenTelemetry.
- Structured JSON logs.
- Metrics.
- Distributed traces.
- Correlation IDs.
- Causation IDs for asynchronous processing.

Monitor:

- Authentication success/failure.
- OTP verification outcomes.
- API latency.
- Error rates.
- Database performance.
- Kafka consumer lag.
- Outbox backlog.
- Cache effectiveness.
- Search indexing delay.
- Messaging latency.
- Notification delivery.
- Frontend Web Vitals.

Define dashboards, alerts, SLOs, and runbooks.

Never log raw passwords, OTP values, access tokens, recovery tokens, or private message content.

---

### 27. Performance Requirements

Define measurable, proposed targets for:

- Frontend Core Web Vitals.
- API p95/p99 latency.
- Authentication latency.
- Search response time.
- Feed retrieval latency.
- Message delivery latency.
- Notification processing delay.
- Concurrent users.
- Database throughput.
- Kafka lag.
- Cache hit ratio.

Use realistic workload assumptions and disclose them.

Run load/stress testing where the environment supports it.

Do not claim performance targets have been achieved without measurements.

---

### 28. CI/CD and DevSecOps

Implement a pipeline with:

1. Source checkout.
2. Dependency resolution.
3. Compilation/build.
4. Formatting/linting.
5. Type checking.
6. Unit tests.
7. Integration tests.
8. Contract compatibility.
9. E2E tests.
10. Security scans.
11. Container build.
12. SBOM generation.
13. Artifact publishing.
14. Infrastructure validation.
15. Staging deployment.
16. Smoke tests.
17. Approval gates where required.
18. Controlled production rollout.
19. Post-deployment verification.
20. Rollback/failure response.

Use existing CI/CD conventions where available.

Evaluate blue-green or canary releases based on service needs.

Protect secrets and use workload identity instead of static cloud credentials wherever practical.

---

### 29. AWS Deployment Strategy

Prepare deployment artifacts for:

**Development**

Local Docker and test infrastructure.

**Integration/Staging**

Production-like deployment for contract, E2E, performance, and operational tests.

**Production**

Controlled AWS infrastructure with appropriate availability, monitoring, security, and recovery.

#### Infrastructure Requirements

Cover:

- VPC networking.
- Private/public subnet design.
- Load balancing.
- TLS.
- Container orchestration.
- Database deployment.
- Kafka deployment.
- Cache.
- Object storage.
- Search.
- Secrets.
- Encryption.
- Service identity.
- Autoscaling.
- Logging.
- Monitoring.
- Backup/recovery.
- DNS.
- Network policies.

#### AWS Well-Architected Framework

Assess all six pillars:

- Operational Excellence.
- Security.
- Reliability.
- Performance Efficiency.
- Cost Optimization.
- Sustainability.

Define a practical gap-analysis and validation checklist.

#### Deployment Authorization

You may create, inspect, test, and validate infrastructure code within the authorized repository.

Do not create billable AWS resources, change external accounts, configure real SMS billing, expose public services, or deploy to production without the necessary authorization and credentials.

When external deployment is not authorized or available, provide validated deployment artifacts and explicitly report the remaining deployment steps.

Never claim a production deployment occurred unless it actually occurred and was verified.

---

### 30. Project Structure

For a new project, prefer a cohesive monorepo or coordinated repository structure that keeps contracts and development infrastructure discoverable.

Example:

- `apps/web` — Next.js frontend.
- `apps/backend` or `services/*` — Java backend modules/services.
- `packages/contracts` — OpenAPI/Protobuf/schema definitions.
- `packages/api-client` — Generated frontend client.
- `infrastructure/local` — Docker Compose and emulators.
- `infrastructure/aws` — Terraform.
- `infrastructure/kubernetes` — Kubernetes/Helm.
- `observability` — Collector configuration, dashboards, alerts.
- `tests/e2e` — Full-stack Playwright tests.
- `tests/performance` — k6/Gatling scenarios.
- `docs/architecture` — System and domain architecture.
- `docs/adr` — Architecture Decision Records.
- `docs/runbooks` — Operational instructions.

Choose a structure that fits the repository and build tools.

Avoid forced repository restructuring.

---

### 31. Implementation Strategy — Vertical Slices

Do not develop the entire frontend and then the entire backend independently.

Implement each feature as a complete vertical slice.

For every slice:

1. Define business acceptance criteria.
2. Define domain ownership.
3. Define API/event contract.
4. Implement database migration/model.
5. Implement backend use case.
6. Implement authorization and validation.
7. Implement API adapter.
8. Generate or update typed frontend client.
9. Implement frontend page/components.
10. Connect frontend to the real API.
11. Add loading/error/empty states.
12. Add unit/integration/contract/E2E tests.
13. Run verification.
14. Document status.
15. Continue to the next slice.

#### Recommended Sequence

**Phase 0 — Discovery and Architecture**

Repository audit, requirement consolidation, screenshot mapping, technology verification, domain model, contracts, architecture decisions.

**Phase 1 — Local Platform Foundation**

Build tooling, Docker Compose, PostgreSQL, Kafka, Keycloak, shared contracts, migrations, telemetry, frontend shell.

**Phase 2 — Authentication End to End**

Email/mobile registration, verification, OTP, login, MFA, recovery, sessions, protected frontend routes.

**Phase 3 — Professional Profile**

Profile pages and backend, media, experience, education, certifications, projects, skills, recommendations.

**Phase 4 — Social Graph**

Connections, invitations, followers, mutual connections, network UI.

**Phase 5 — Feed and Content**

Posts, comments, reactions, media, timeline, events, projections.

**Phase 6 — Messaging and Notifications**

Real-time delivery, persistence, read/unread states, notification preferences.

**Phase 7 — Jobs and Organizations**

Job discovery, applications, company pages, recruiter permissions.

**Phase 8 — Search, Privacy, and Analytics**

Indexing, search, profile metrics, privacy enforcement, account-data workflows.

**Phase 9 — Hardening and Deployment**

Performance, accessibility, security, resilience, CI/CD, staging validation, deployment preparation, recovery tests.

Prioritize foundational security and data correctness in every phase.

---

### 32. Unified Requirements Traceability

Use stable requirement IDs across both projects.

Examples:

- `FE-AUTH-001` — Email registration UI.
- `BE-AUTH-001` — Email registration backend.
- `INT-AUTH-001` — Complete email registration integration.
- `E2E-AUTH-001` — Registration verification test.

Similarly:

- `FE-PROFILE-001`
- `BE-PROFILE-001`
- `INT-PROFILE-001`
- `E2E-PROFILE-001`

Create a matrix mapping:

| Requirement | Frontend | Backend | Contract | Data/Event | Test | Status |
|---|---|---|---|---|---|---|

Use a status vocabulary such as:

- Planned.
- In progress.
- Implemented.
- Integration verified.
- Blocked.
- Deferred.

Do not claim "complete" if the UI works only against mock data while the real backend integration is missing.

---

### 33. Definition of Done — Full-Stack Feature

A feature is complete only when:

- UI is implemented.
- UX follows the approved design.
- Navigation works.
- Form validation works.
- Backend API exists.
- Authorization works.
- Domain rules are enforced.
- Data persists correctly.
- Frontend displays persisted data.
- Cache invalidation is correct.
- Errors are handled.
- Relevant retries/idempotency work.
- Events are processed reliably where required.
- Responsive layout works.
- Light/dark theme works.
- Accessibility expectations are met.
- Unit/integration tests pass.
- Full-stack E2E test passes.
- Relevant telemetry exists.
- Documentation is updated.

Distinguish feature completion from overall production readiness.

---

### 34. Required Design and Documentation Artifacts

Produce documents only to the extent they support actual implementation and review.

Include:

- Consolidated functional requirements.
- System context diagram.
- Domain/bounded-context map.
- Service/component architecture.
- Frontend component architecture.
- Authentication sequence diagrams.
- Database ERD.
- API contract catalog.
- Event catalog.
- Messaging sequence diagram.
- Kafka outbox flow.
- AWS deployment diagram.
- Security trust-boundary diagram.
- Observability architecture.
- CI/CD architecture.
- Architecture Decision Records.
- Deployment and operational runbooks.

Use Mermaid for appropriate diagrams.

Diagrams must match the actual implementation rather than depict imaginary services.

---

### 35. Strict Quality and Execution Rules

Follow these rules:

1. Read both uploaded prompts fully.
2. Preserve significant requirements from both.
3. Analyze the 18 screenshots.
4. Do not fabricate LinkedIn's internal architecture.
5. Do not start with unnecessary microservices.
6. Build frontend and backend together.
7. Use authoritative contracts.
8. Generate typed API clients where appropriate.
9. Use one consistent identity model.
10. Secure email/mobile registration and MFA.
11. Do not invent insecure authentication shortcuts.
12. Do not maintain duplicate credential systems.
13. Implement backend authorization.
14. Prevent duplicate operations.
15. Handle concurrency and transaction boundaries.
16. Implement meaningful error recovery.
17. Use Kafka where asynchronous processing is justified.
18. Protect PII and secrets.
19. Keep demo/test data fictional.
20. Use one consistent frontend design system.
21. Make visible interactive controls functional or clearly unavailable.
22. Implement responsive light/dark/system themes.
23. Test with real local backend services.
24. Do not treat mocked E2E tests as proof of real backend integration.
25. Verify actual build and test results.
26. Do not claim AWS deployment without verification.
27. Preserve unrelated repository work.
28. Avoid unnecessary abstractions and dependencies.
29. Fix regressions introduced by implementation.
30. Do not stop after planning if code changes are within the authorized scope.

---

### 36. Final Delivery Format

At completion provide an engineering summary.

**1. What was implemented**

Report completed frontend, backend, and integration modules.

**2. Architecture**

Summarize actual bounded contexts, deployment services, data ownership, API/event architecture, and technology choices.

**3. Authentication**

Report actual verification status for registration, OTP, MFA, login, recovery, and sessions.

**4. Frontend integration**

State which features use real APIs versus development mocks.

**5. UI/UX**

Report screenshot coverage, responsive layouts, theme behavior, and accessibility checks.

**6. Testing**

List actual commands executed, tests passed/failed, and anything not verified.

**7. Infrastructure**

Report local environment status, CI/CD status, AWS IaC validation, and actual deployment status.

**8. Risks and blockers**

List remaining functional, security, performance, operational, and deployment issues.

Do not describe unverified functionality as production-ready.

---

# FINAL EXECUTION INSTRUCTION

**Your task is to fully implement the LinkedIn-inspired professional networking platform end to end using the uploaded frontend and backend master prompts, the 18 screenshots, repository instructions, and this unified specification.**

Execute the workflow:

**Analyze Inputs → Inspect Repository → Consolidate Requirements → Define Domains → Design Architecture → Establish Contracts → Build Local Infrastructure → Implement Identity → Build Integrated Feature Slices → Add Messaging and Events → Secure the Platform → Instrument Observability → Test Full-Stack Journeys → Optimize → Prepare and Validate Deployment → Deliver Verified Results.**

The final system must have:

- Modern responsive frontend.
- Professional UI/UX and consistent design system.
- Functional light/dark/system themes.
- Complete authentication and authorization.
- Email/mobile registration and OTP verification.
- MFA, passkeys where supported, and password recovery.
- Working professional networking capabilities.
- Functional feed, messaging, jobs, search, notifications, and settings.
- Secure persistent backend services.
- Explicit API and event contracts.
- Reliable asynchronous event processing.
- Reproducible local development.
- Automated full-stack testing.
- AWS-oriented cloud-native deployment artifacts.
- Operational observability.
- Evidence-based production-readiness assessment.

**Do not deliver merely a polished UI, a collection of backend endpoints, architectural diagrams, or separate applications that have not been integrated.**

Implement complete working vertical slices, verify them, and report remaining limitations accurately.

Where the task exceeds a single execution scope, preserve a clear implementation backlog and completed state. Continue with the highest-priority authorized work rather than replacing implementation with generic planning.

**The ultimate objective is one cohesive, secure, scalable, observable, well-tested, and deployable professional networking platform whose frontend and backend operate together as a complete product.**