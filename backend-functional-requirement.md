# MASTER PROMPT: LinkedIn-Inspired Professional Networking Platform — Complete Enterprise Backend Development

## 1. Role and Mission

Act as a **Principal Software Architect, Distinguished Backend Engineer, Distributed Systems Architect, Domain-Driven Design Expert, Cloud-Native Architect, AWS Solutions Architect, DevSecOps Engineer, and Site Reliability Engineer**.

Your task is to **design, implement, integrate, test, and document a complete enterprise-grade backend for a LinkedIn-inspired professional networking platform**.

The system must reproduce the essential business capabilities of a professional networking platform while drawing architectural inspiration from LinkedIn's publicly documented engineering practices and open-source technology ecosystem.

Use the actual publicly documented LinkedIn technologies wherever practical and appropriate.

For proprietary, internal, unavailable, deprecated, or unsuitable technologies, select well-maintained open-source alternatives that provide equivalent capabilities.

The application must support:

- Registration using email address or mobile number.
- Email/password authentication.
- Mobile-number/password authentication.
- Email address verification.
- Mobile number verification using OTP.
- Multifactor authentication (MFA).
- Passkeys and authenticator-app-based authentication.
- Password recovery and account security.
- Professional profiles.
- Professional connections and social graph.
- Content publishing and personalized feeds.
- Real-time messaging.
- Job discovery, posting, and applications.
- Organizations and company pages.
- Global search and recommendations.
- Notifications.
- Privacy and account preferences.
- Media and document management.
- Trust, safety, and moderation.
- Profile and product analytics.
- Administrative operations.

**This is a backend implementation task, not just a backend functional requirements or architecture documentation exercise.**

Produce working, independently testable backend functionality backed by appropriate persistence, authentication, API contracts, event processing, observability, and deployment configuration.

---

# 2. Non-Negotiable Engineering Principles

Follow these principles throughout development:

1. Domain-Driven Design before service decomposition.
2. Clean Architecture and Hexagonal Architecture with meaningful dependency boundaries.
3. SOLID principles applied pragmatically.
4. Secure by design and least privilege.
5. Data integrity before distributed scalability.
6. Explicit transactional boundaries.
7. Idempotent operations where retries or duplicates are possible.
8. Reliable event-driven communication.
9. Resilience through bounded timeouts, retries, and failure isolation.
10. Observability built into every production-relevant service.
11. Cloud-native deployment and automation.
12. Compatibility-first API and event evolution.
13. Independent service ownership and persistence.
14. Measurable reliability and performance requirements.
15. Automated tests for important business invariants and failure paths.
16. Cost-aware, maintainable architectural choices.
17. Production readiness demonstrated by evidence, never assumed.
18. Evolutionary architecture rather than unnecessary complexity.

Use engineering judgment to avoid excessive abstractions, dependencies, microservices, database engines, and infrastructure components.

---

# 3. Source of Truth and Development Instructions

Before implementation:

1. Inspect the existing repository.
2. Read all applicable project instructions and architecture documents.
3. Identify existing dependencies and their versions.
4. Identify existing modules, services, schemas, APIs, and tests.
5. Preserve established project conventions unless a change is justified.
6. Analyze all supplied screenshots and requirements.
7. Inspect existing frontend contracts, if available.
8. Identify missing backend capabilities.
9. Research relevant public LinkedIn engineering documentation.
10. Verify version-dependent technology decisions against official documentation.

Use this priority order:

- Existing repository instructions and confirmed contracts.
- Explicit requirements in this prompt.
- Uploaded screenshots for visible business capabilities.
- Verified public LinkedIn engineering references.
- Current security, reliability, and cloud-native best practices.
- Documented assumptions when information is unavailable.

Do not rewrite a working codebase unnecessarily.

Do not create parallel implementations of capabilities already available.

Preserve unrelated work.

Make routine implementation decisions independently.

Ask for clarification only when a missing decision materially affects business correctness, security, architecture, compatibility, or operational risk.

---

# 4. LinkedIn Architecture and Technology Parity

## 4.1 Research Requirements

Research LinkedIn's publicly documented infrastructure and backend architecture.

Prefer:

- LinkedIn Engineering publications.
- Official LinkedIn open-source GitHub repositories.
- Apache Software Foundation documentation.
- CNCF documentation.
- Official gRPC and Protocol Buffers documentation.
- Official technology project documentation.
- AWS architecture guidance.

Do not rely solely on outdated architecture diagrams, unverified blogs, or assumptions.

For every technology, distinguish:

- Publicly documented historical LinkedIn usage.
- Publicly documented current usage.
- Open-source availability.
- Deprecation or maintenance status.
- Proposed use in this platform.
- Alternative and trade-offs.

The objective is architectural fidelity to publicly documented LinkedIn engineering practices, not an unverifiable claim to duplicate LinkedIn's private production infrastructure.

## 4.2 LinkedIn-Inspired Reference Stack

Evaluate and preferentially use the following technologies.

| Architectural capability | LinkedIn reference technology | Implementation decision |
|---|---|---|
| Primary backend language | Java/JVM ecosystem | Java 21 as project baseline |
| Service-to-service communication | gRPC and Protocol Buffers | gRPC + Protobuf |
| Legacy service APIs | Rest.li | Do not use for new development; deprecated/archived |
| Event streaming | Apache Kafka | Apache Kafka |
| Stream processing | Apache Samza | Evaluate Samza; use Flink if better justified |
| Derived-data serving | Venice | Venice when derived-data workloads justify it |
| Real-time analytics | Apache Pinot | Apache Pinot for suitable analytics workloads |
| Primary online data store | Espresso, LinkedIn-specific | PostgreSQL baseline; evaluate Cassandra/ScyllaDB for scale-specific use cases |
| Search technology | Galene / Apache Lucene | OpenSearch or Lucene-based open-source alternative |
| Local embedded caching | Caffeine / RocksDB | Caffeine; RocksDB where embedded state is useful |
| Distributed caching | Workload-specific | Redis or Valkey |
| Distributed object storage | Ambry | Evaluate Ambry; use S3-compatible storage where simpler |
| Data integration and replication | Brooklin | Brooklin where justified; Debezium for simpler CDC workloads |
| Cluster coordination | Apache Helix | Evaluate only for specialized stateful clusters |
| API aggregation | LinkedIn's service/BFF patterns | Edge API/BFF where required |
| Stream/event schemas | Protobuf / schema-based contracts | Protobuf, schema registry, compatibility checks |
| Service discovery | LinkedIn-specific systems and modern gRPC integration | Kubernetes DNS / Envoy xDS where appropriate |

Document actual usage evidence and do not assume all technologies are currently used by every LinkedIn service.

## 4.3 Recommended Supporting Technologies

For capabilities without a verified direct LinkedIn open-source equivalent, use:

- Spring Boot for maintainable Java business services.
- Spring Security for security integration.
- Spring Data JPA/Hibernate where relational persistence benefits from ORM.
- jOOQ or JDBC for complex SQL and performance-sensitive queries where justified.
- Keycloak for open-source identity and OIDC capabilities.
- PostgreSQL for transactional storage.
- Flyway or Liquibase for schema migrations.
- Redis/Valkey for distributed ephemeral state and caching.
- OpenSearch for search workloads requiring distributed indexing.
- Debezium for CDC where appropriate.
- Resilience4j for service resilience.
- OpenTelemetry for instrumentation.
- Prometheus and Grafana for monitoring.
- Loki and Tempo/Jaeger where appropriate.
- Docker and Kubernetes for container orchestration.
- Terraform for infrastructure as code.
- Testcontainers for real-dependency integration tests.

These are proposed engineering choices, not claims about LinkedIn's internal stack.

## 4.4 Technology Selection Rules

Before adopting a technology, document:

- Why it is needed.
- Which requirement it satisfies.
- Whether LinkedIn's usage is verified.
- Whether the implementation is open source.
- Its maintenance and security status.
- Compatibility with Java 21 and selected framework versions.
- Resource consumption.
- Operational complexity.
- Failure modes.
- Expected workload.
- Simpler alternatives.
- Conditions for reconsidering the decision.

Do not deploy Pinot, Venice, Samza, Helix, Brooklin, or a separate graph database merely to imitate LinkedIn's infrastructure.

Use these systems when the scale, read/write patterns, and operational needs justify them.

---

# 5. AWS as the Target Cloud Platform

Deploy the proposed architecture on AWS while maintaining portability at application and domain layers.

Do not claim the AWS topology is identical to LinkedIn's internal infrastructure.

Use AWS as the selected infrastructure provider.

Evaluate:

- Amazon EKS.
- Amazon MSK.
- Amazon RDS/Aurora PostgreSQL.
- Amazon ElastiCache.
- Amazon S3.
- Amazon OpenSearch Service.
- AWS Application Load Balancer.
- Amazon CloudFront.
- AWS WAF.
- AWS IAM.
- AWS KMS.
- AWS Secrets Manager.
- Amazon CloudWatch.
- AWS Route 53.
- AWS VPC and network infrastructure.
- AWS email and SMS delivery services where appropriate.

Keep application logic decoupled from AWS-specific APIs through adapters where portability has meaningful value.

Design local development without requiring a live AWS account.

---

# 6. Screenshot-Driven Backend Requirements Extraction

Analyze all 18 uploaded screenshots.

Use the following source map:

| ID | Visible feature | Backend domains |
|---|---|---|
| S01 | Profile header | Profile, media, privacy, network, jobs |
| S02 | Account dropdown | Identity, account, organization |
| S03 | Notifications | Notifications, jobs, preferences |
| S04 | Messaging | Conversations, messaging, inbox |
| S05 | Jobs | Jobs, preferences, recommendations |
| S06 | My Network | Social graph, invitations, follows |
| S07 | Account Preferences | Settings, profile, preferences |
| S08 | Sign In and Security | Authentication, credentials, MFA, sessions |
| S09 | Visibility | Privacy and access-control policies |
| S10 | Data Privacy | Consent, export, deletion, retention |
| S11 | Advertising Data | Advertising preferences, data governance |
| S12 | Notification Settings | Notification preference management |
| S13 | Analytics and About | Profile, analytics, featured content |
| S14 | Experience | Professional history and organizations |
| S15 | Education | Education and connected applications |
| S16 | Certifications | Credentials and external verification |
| S17 | Projects and Skills | Portfolio and skills |
| S18 | Recommendations | Recommendations and languages |

Classify requirements as Observed, Inferred, Recommended, or Unknown.

For each screenshot extract:

1. Business entities.
2. Data attributes.
3. User-triggered commands.
4. Read/query operations.
5. Validation rules.
6. Authorization requirements.
7. State transitions.
8. Persistence needs.
9. Consistency requirements.
10. Event candidates.
11. Failure scenarios.
12. Acceptance criteria.

Then combine duplicate requirements across screenshots.

**Screenshot-derived functionality must be supplemented by the complete product requirements below, particularly registration, authentication, OTP, MFA, and security workflows not fully illustrated in the screenshots.**

---

# 7. Mandatory P0 — Identity, Registration, Authentication, and Account Security

Identity and authentication are foundational requirements.

All critical flows must be implemented and tested before exposing protected platform functionality.

## 7.1 Registration Using Email Address

Implement account registration using:

- Email address.
- Password.
- Password confirmation where required by UX.
- Acceptance of applicable terms.
- Required privacy acknowledgments.
- Initial account profile details.

Workflow:

1. User submits registration details.
2. Validate and normalize the email appropriately.
3. Enforce uniqueness and account-linking rules.
4. Validate password against current security policy.
5. Create a pending/unverified account using an atomic, duplicate-safe process.
6. Generate an email verification challenge.
7. Deliver verification instructions through the email provider.
8. Verify the email using a single-use link or code.
9. Activate the account according to policy.
10. Initialize the professional profile.
11. Allow authenticated onboarding.

Support:

- Expired verification.
- Invalid verification.
- Duplicate verification.
- Resend verification.
- Delivery failure.
- Concurrent registration.
- Existing account.
- Verification after session expiration.
- Abandoned registration cleanup.

Never expose whether a sensitive identifier belongs to an account when doing so would enable account enumeration.

## 7.2 Registration Using Mobile Number

Implement mobile-number registration as a first-class flow.

Required fields:

- Country/dialing code.
- Mobile number.
- Password.
- Required registration agreements.
- Optional email address.

Workflow:

1. Accept the country code and mobile number.
2. Parse and normalize using an established phone-number library.
3. Represent verified phone identities in E.164 format.
4. Check identifier uniqueness and account-linking rules.
5. Create an appropriately restricted pending registration.
6. Generate a cryptographically secure one-time verification code.
7. Send the code through an SMS provider adapter.
8. Verify the submitted code.
9. Atomically mark the phone identity as verified.
10. Complete account activation.
11. Initialize onboarding and professional profile.

Handle:

- Invalid mobile number.
- Unsupported destination.
- SMS delivery failure.
- Code expiration.
- Incorrect code.
- Maximum attempts exceeded.
- Resend cooldown.
- Duplicate mobile number.
- Concurrent registration.
- Stale OTP after resend.
- Country-code changes.
- Mobile-number reassignment risk.
- Account takeover protection.

A mobile number must not be treated as verified until the verification transaction succeeds.

## 7.3 Login Using Email and Password

Implement:

- Email/password authentication.
- Credential verification.
- MFA challenge when required.
- Secure session creation.
- Token or session issuance.
- Login audit event.
- Failed-login throttling.
- Logout.
- Session expiration.

Use a current, approved password hashing approach such as Argon2id where compatible with the selected identity platform.

Do not store plaintext or reversibly encrypted passwords.

Do not use obsolete OAuth password-grant patterns as the default authentication architecture.

## 7.4 Login Using Mobile Number and Password

Allow login using a verified mobile identifier and password.

Required behavior:

- Normalize phone number.
- Resolve the verified account identity securely.
- Verify password.
- Apply MFA where required.
- Issue a session only after all required authentication checks pass.
- Handle unverified or reassigned numbers safely.
- Rate limit by account, source, and risk indicators.

Email and mobile login methods must resolve to one canonical account when both identities are verified and linked.

Do not accidentally create separate professional profiles when an existing account adds another verified identifier.

## 7.5 Identity Linking

Support:

- Add email to phone-registered account.
- Add phone to email-registered account.
- Change primary email.
- Change primary phone.
- Remove secondary identifier.
- Verify added identifier.
- Resolve identity conflicts.
- Prevent unauthorized account merges.

Require recent authentication or step-up verification for sensitive identity changes.

Do not merge two existing accounts solely because a newly submitted identifier appears to match.

## 7.6 Email Verification

Support:

- Single-use verification links/codes.
- Cryptographically secure random values.
- Expiration.
- Resend with throttling.
- Verification status.
- Duplicate-use prevention.
- Invalidated prior challenges.
- Audit events.

Email verification proves control of the email address; do not automatically treat it as a phishing-resistant authentication factor.

## 7.7 SMS OTP Verification

Build a reusable, purpose-bound OTP challenge capability for:

- Mobile registration verification.
- Adding a mobile number.
- Mobile-number changes.
- Phone ownership reconfirmation.
- SMS MFA fallback where permitted.
- Risk-based recovery operations where policy allows.

OTP requirements:

- Cryptographically secure generation.
- Configurable expiration; propose five minutes for ordinary registration verification.
- Single-use semantics.
- Maximum attempt count.
- Resend cooldown.
- Account/IP/phone-level throttling.
- Atomic verification and consumption.
- Invalidation of superseded challenges.
- No plaintext OTP persistence.
- No OTP in logs or analytics.
- Purpose-bound verification.
- Secure comparison.
- Abuse prevention.
- Provider failure handling.

Store a keyed digest or appropriately protected verifier instead of exposing raw OTP values.

Use durable or appropriately replicated challenge state where correctness requires it.

Redis can provide short-lived challenge storage, but ensure the persistence and atomicity strategy is explicit.

## 7.8 Multifactor Authentication

Implement MFA enrollment and verification.

Prioritize:

1. WebAuthn/FIDO2 passkeys or security keys.
2. TOTP authenticator applications.
3. Recovery codes.
4. SMS-based MFA only as a restricted fallback when explicitly permitted.

MFA capabilities:

- Enroll authenticator.
- Verify enrollment.
- List enrolled authenticators.
- Remove authenticator.
- Enable/disable MFA.
- Generate recovery codes.
- Regenerate recovery codes.
- Step-up authentication.
- Suspicious-login challenges.
- Trusted-device policy where appropriate.
- Lost-device recovery.
- Security event notification.

Require reauthentication before disabling MFA or replacing a registered authenticator.

Do not treat SMS OTP as phishing-resistant.

## 7.9 Passkeys

Support standards-based WebAuthn/FIDO2 passkeys using the selected identity provider or an audited compatible implementation.

Define:

- Registration ceremony.
- Authentication ceremony.
- Credential storage.
- Challenge expiration.
- Origin and relying-party validation.
- Replay protection.
- Credential revocation.
- Recovery.
- Multiple devices/passkeys.
- Account-binding security.

Do not create custom cryptographic passkey protocols.

## 7.10 Password Recovery

Support recovery using verified and authorized recovery channels.

Workflow:

1. User requests recovery.
2. Respond without leaking account existence.
3. Generate a time-limited, single-use recovery challenge.
4. Verify the required recovery proof.
5. Permit password replacement.
6. Invalidate recovery challenge.
7. Apply appropriate session invalidation.
8. Notify the account owner.
9. Record a security audit event.

Support recovery failure, expiration, repeated attempts, and suspicious behavior.

Account recovery must not silently bypass enabled MFA or create an account takeover shortcut.

## 7.11 Session and Token Management

Implement:

- Secure session lifecycle.
- Session listing.
- Session revocation.
- Logout all devices.
- Refresh-token rotation where applicable.
- Reuse detection where supported.
- Appropriate session timeouts.
- Revocation propagation.
- MFA context preservation.
- Device information with privacy controls.
- Suspicious-session detection.

Prefer a standards-based OIDC approach and secure browser session patterns.

For browser clients, evaluate a BFF/session-cookie strategy using HttpOnly, Secure, appropriately scoped SameSite cookies and CSRF protection where applicable.

Avoid putting long-lived authentication tokens in browser local storage.

## 7.12 Identity Provider Architecture

Prefer **Keycloak** as an open-source identity-provider foundation, subject to compatibility and operational evaluation.

Use Keycloak for standards-based identity, OIDC, credentials, sessions, roles, MFA, and passkeys where supported.

Important:

- Do not assume that native Keycloak supports every requested mobile-number registration and SMS OTP workflow out of the box.
- Implement required gaps through carefully scoped, supported identity extensions, custom authentication flows, or a dedicated identity orchestration adapter.
- Avoid maintaining a competing password database in a separate business service.
- Keep the identity provider authoritative for credentials and authentication.
- Keep the application domain authoritative for professional profiles and business relationships.
- Review identity extensions for upgrade compatibility and security.

Document the precise ownership of each account, credential, phone verification, and recovery operation.

## 7.13 Authentication State Machines

Model at least:

- Registration.
- Identifier verification.
- MFA enrollment.
- Login challenge.
- Password recovery.
- Account suspension.
- Account deactivation/deletion.

Include valid transitions, invalid transitions, expiry, concurrency, retries, and recovery.

---

# 8. Authentication and Registration API Contracts

Define secure, versioned integration contracts for the frontend.

The application-facing contracts should cover these capabilities:

| Capability | Example proposed API |
|---|---|
| Register with email | `POST /api/v1/auth/registrations/email` |
| Register with phone | `POST /api/v1/auth/registrations/phone` |
| Request verification | `POST /api/v1/auth/verifications` |
| Verify challenge | `POST /api/v1/auth/verifications/{id}/confirm` |
| Resend verification | `POST /api/v1/auth/verifications/{id}/resend` |
| Account recovery request | `POST /api/v1/auth/recovery-requests` |
| Complete recovery | `POST /api/v1/auth/recovery-requests/{id}/complete` |
| List active sessions | `GET /api/v1/account/sessions` |
| Revoke session | `DELETE /api/v1/account/sessions/{id}` |
| MFA management | Identity provider-backed authenticated operations |

These are proposed resource examples, not a mandate to build custom authentication endpoints instead of standards-based OIDC flows.

Use OIDC Authorization Code with PKCE for supported interactive authentication.

Keep password handling inside the trusted identity boundary.

For each proposed contract specify:

- Request and response schemas.
- Authentication context.
- Validation.
- Rate limits.
- Idempotency.
- Error codes.
- Retry behavior.
- Session security.
- Audit requirements.
- OpenAPI documentation.
- Integration tests.

Never expose password hashes, OTP verifiers, session secrets, or sensitive recovery tokens.

---

# 9. Professional Profile Domain

Implement complete professional profile management.

Include:

- Create profile during onboarding.
- Retrieve own profile.
- Retrieve other members' profiles.
- Public profile retrieval.
- Update profile.
- Profile image and banner management.
- Custom profile URL.
- Headline and About section.
- Location and contact information.
- Current organization.
- Open-to-work preferences.
- Profile visibility.
- Profile language.
- Featured content.
- Activity summary.
- Profile completeness.

## 9.1 Experience

CRUD for professional experience, with roles, employers, dates, employment type, locations, descriptions, skills, and projects.

## 9.2 Education

CRUD for education records, qualifications, institutions, study fields, dates, and optional descriptions.

## 9.3 Certifications

CRUD for licenses and certifications, issuer, credential ID, credential URL, dates, and optional expiry.

## 9.4 Projects

CRUD for portfolio projects with skills, employment association, dates, descriptions, and references.

## 9.5 Skills

Skill catalog, profile associations, ordering, duplicate prevention, endorsements where supported.

## 9.6 Recommendations

Received/given recommendations, requests, approval or visibility workflow, pending status, and user authorization.

## 9.7 Profile Analytics

Profile views, post impressions, search appearances, reporting periods, and privacy-aware metric retrieval.

For every profile operation implement:

- Object-level authorization.
- Validation.
- Optimistic concurrency where appropriate.
- Transaction boundaries.
- Audit controls.
- Search/feed projection updates.
- Privacy enforcement.
- Meaningful failure responses.

---

# 10. Social Graph and Connection Management

Implement:

- Send connection request.
- Accept request.
- Ignore/reject.
- Withdraw request.
- Remove connection.
- List connections.
- List pending/sent invitations.
- Follow/unfollow members.
- List followers/following.
- Mutual connections.
- Suggested connections.
- Block/unblock.
- Network statistics.

Model invitations and accepted relationships explicitly.

Guarantee:

- No self-connections.
- No duplicate active invitations.
- No duplicate connection records.
- Authorized invitation transitions.
- Concurrency-safe acceptance.
- Correct blocked-user behavior.
- Reliable events for accepted relationships.
- Consistent or reconcilable counters.

Choose a transaction and uniqueness strategy supported by the authoritative store.

Do not require a graph database before demonstrating a graph-specific workload need.

---

# 11. Content Publishing and Social Activity

Implement:

- Text posts.
- Image and document posts.
- Video metadata/processing where supported.
- Create/update/delete.
- Post drafts.
- Post visibility.
- Comments and threaded replies.
- Reactions.
- Reposts.
- Mentions.
- Hashtags.
- Bookmarks.
- Content reporting.
- Content moderation.
- Author activity history.

Define:

- Content ownership.
- Aggregate boundaries.
- Reaction uniqueness.
- Comment hierarchy constraints.
- Moderation states.
- Deletion behavior.
- Media authorization.
- Privacy filtering.
- Idempotent mutations.
- Concurrent update handling.

Publish relevant integration events reliably.

---

# 12. Feed Generation and Personalization

Design a LinkedIn-inspired feed architecture.

Capabilities:

- Personalized home feed.
- Following-based feed.
- Author activity feed.
- Recent content retrieval.
- Candidate generation.
- Ranking.
- Pagination.
- Recommendation blending.
- Hidden/muted content.
- Impression tracking.
- Privacy filtering.
- Feed refresh.

Evaluate:

- Fan-out-on-read.
- Fan-out-on-write.
- Hybrid timeline generation.
- Materialized feed projections.
- Kafka-based activity ingestion.
- Caffeine/Redis caching.
- Venice for high-scale derived feed data.
- Stream processing for projections.
- Ranking strategies.

Start with a deterministic, explainable baseline ranking strategy.

Introduce ML inference only when model, training data, evaluation, and operations exist.

Never invent engagement metrics or claim sophisticated recommendation accuracy without measurement.

Protect against content leakage when privacy or blocking policies change after feed projection creation.

---

# 13. Messaging and Real-Time Communication

Implement:

- One-to-one conversations.
- Group conversations.
- Conversation participants.
- Message persistence.
- Message history.
- Cursor pagination.
- Send and receive.
- Read receipts.
- Delivery acknowledgment.
- Unread counts.
- Conversation search.
- Star/archive/mute.
- Attachments.
- Blocking restrictions.
- Real-time notifications.
- Offline message recovery.

For messaging correctness:

- Generate stable message IDs.
- Deduplicate client retries.
- Preserve ordering within defined conversation scope.
- Persist messages before confirming durable acceptance.
- Distinguish persisted, delivered, and read states.
- Support reconnection and missed-event recovery.
- Handle concurrent sends.
- Manage backpressure.
- Avoid duplicate unread increments.

Evaluate WebSockets for bidirectional client communication.

Use gRPC for internal service communication where suitable.

Do not assume Kafka alone is an appropriate direct client messaging transport.

---

# 14. Jobs, Applications, and Career Preferences

Implement:

- Job CRUD.
- Job publishing.
- Job lifecycle.
- Search/filtering.
- Recommended jobs.
- Saved jobs.
- Job alerts.
- Job preferences.
- Dismissed recommendations.
- Job tracking.
- Resume attachments.
- Job applications.
- Application status transitions.
- Recruiter management.
- Company-owned job postings.

Define strong ownership and authorization boundaries.

Prevent duplicate active applications according to explicit business rules.

Keep job recommendation ranking separate from authoritative job data.

---

# 15. Organizations and Company Pages

Implement:

- Company profile.
- Company creation and updates.
- Company administrator roles.
- Organization members.
- Employment associations.
- Company followers.
- Company posts.
- Company jobs.
- Organization verification where supported.
- Recruiter permissions.
- Managed company inboxes where in scope.

Distinguish self-reported employment from verified organization membership.

Prevent unauthorized management of company pages and job postings.

---

# 16. Search and Discovery

Implement:

- People search.
- Company search.
- Jobs search.
- Posts/content search.
- Autocomplete.
- Suggestions.
- Filters.
- Sorting.
- Cursor pagination.
- Search history.
- Search history deletion.
- Search indexing and reindexing.

Use OpenSearch or a suitable open-source Lucene-based implementation for distributed search when justified.

Define:

- Index mapping.
- Index ownership.
- Search authorization.
- Privacy filtering.
- Incremental indexing.
- Event replay.
- Reconciliation.
- Search result freshness.
- Index rebuild.
- Deleted-content suppression.

Do not allow stale search indexes to expose private or blocked information.

---

# 17. Notifications and User Preferences

Implement:

- In-app notifications.
- Email notifications.
- Push-ready notifications.
- Job notifications.
- Connection notifications.
- Messages.
- Mentions.
- Post interactions.
- Company updates.
- Professional milestones.
- Verification/security alerts.

Support:

- Retrieve/filter.
- Mark read/unread.
- Mark all read where supported.
- Unread count.
- Dismiss.
- Delivery status.
- Notification templates.
- Category/channel preferences.

Use event-driven notification generation where appropriate.

Ensure notification preferences are checked at the correct point in the delivery workflow.

Deduplicate retries and handle missing/deleted target resources.

---

# 18. Settings, Privacy, and Data Governance

Implement every significant settings category shown in the screenshots.

## Account Preferences

Profile information, language, content language, theme, autoplay, sound, and display preferences.

## Sign In and Security

Emails, mobile numbers, password changes, passkeys, sessions, remembered devices, and MFA.

## Visibility

Profile viewing mode, public profile, email visibility, connection visibility, follow visibility, discoverability, and organization visibility.

## Data Privacy

Consent records, data permissions, data export, search history, research preferences, generative AI data preferences, account deactivation and deletion.

## Advertising Data

Profile-data preferences, inferred attributes, company/skill/employer categories, and advertising-related permissions.

## Notifications

Preference categories and supported delivery channels.

For each setting define:

- Data type.
- Allowed values.
- Default.
- Authorization.
- Validation.
- Persistence owner.
- Read/update contract.
- Downstream enforcement.
- Audit handling.
- Consistency requirements.

Separate UI-local preferences from account-synchronized settings.

Implement account deletion as a recoverable, auditable workflow that propagates to owned domain data, search indexes, caches, media, and permitted analytics systems according to retention policy.

---

# 19. Media, Trust and Safety, and Administration

## Media

Implement:

- Secure upload initialization.
- Pre-signed URLs where applicable.
- Upload completion verification.
- Metadata persistence.
- File size/type restrictions.
- Content inspection.
- Malware scanning.
- Thumbnail processing.
- Private media authorization.
- Retention and deletion.
- Orphan cleanup.

## Trust and Safety

Implement:

- Content reports.
- User reports.
- Blocking.
- Spam controls.
- Abuse rate limits.
- Moderation queues.
- Moderation decisions.
- Content takedown.
- Administrative audit logs.

## Administration

Implement appropriate administrative access for:

- Moderation.
- Organization verification.
- Authorized account support.
- Feature configuration.
- Audit review.
- Operational diagnostics.

Prevent application administrators from automatically inheriting unrestricted infrastructure or secret access.

---

# 20. DDD Bounded Contexts and Microservice Boundaries

Create a strategic domain model before implementing services.

Evaluate candidate bounded contexts:

1. Identity and Access.
2. Member Profile.
3. Network and Social Graph.
4. Content and Activity.
5. Feed and Recommendations.
6. Messaging.
7. Jobs and Applications.
8. Organizations.
9. Search and Discovery.
10. Notifications.
11. Preferences and Privacy.
12. Media.
13. Analytics.
14. Trust and Safety.
15. Platform Administration.

For each bounded context define:

- Responsibility.
- Domain entities.
- Aggregate roots.
- Value objects.
- Business invariants.
- Use cases.
- Commands/queries.
- Data ownership.
- API contracts.
- Events.
- Dependencies.
- Transaction boundaries.
- Availability requirements.
- Team ownership.

Use clear microservice boundaries where justified.

Do not automatically create fifteen independently deployed microservices.

Start with independently cohesive domain modules and evolve to dedicated services according to deployment, scalability, security, and operational requirements.

When an independently deployable service is selected, enforce its ownership boundaries rather than introducing shared mutable databases.

---

# 21. Clean/Hexagonal Architecture Implementation

Each substantial domain service should follow a structure comparable to:

```text
service/
  src/main/java/.../
    domain/
      model/
      valueobject/
      event/
      policy/
      service/
    application/
      usecase/
      command/
      query/
      port/
        in/
        out/
    adapter/
      in/
        rest/
        grpc/
        messaging/
      out/
        persistence/
        kafka/
        cache/
        external/
    infrastructure/
      configuration/
      security/
      observability/
```

Rules:

- Domain logic must not depend directly on Spring, Kafka, PostgreSQL, or AWS implementations.
- Use cases coordinate domain operations.
- Adapters implement external integrations.
- Transactions are explicitly owned by application operations.
- Infrastructure wiring must not leak into domain objects.
- Avoid generic repository interfaces that expose infrastructure details.
- Do not introduce ports for trivial operations where no meaningful boundary exists.

Apply Repository, Strategy, Factory, Specification, Adapter, Outbox, and related patterns only when justified.

---

# 22. API Design and gRPC Communication

Use:

- REST/OpenAPI for public and frontend-facing APIs.
- gRPC/Protobuf for appropriate internal service calls.
- WebSockets for bidirectional real-time client messaging where justified.
- Kafka for durable asynchronous event streams.

Define:

- Resource modeling.
- API versioning.
- Protobuf schemas.
- Generated clients.
- Compatibility checks.
- Request validation.
- Authorization.
- Standardized errors.
- Cursor pagination.
- Idempotency.
- Deadlines.
- Cancellations.
- Timeouts.
- Rate limits.
- Correlation IDs.
- Deprecation strategy.

Avoid deep synchronous service-call chains.

Propagate deadlines across dependencies.

Use gRPC interceptors for security and telemetry.

Produce real contract files, not only example documentation.

---

# 23. Kafka and Event-Driven Architecture

Use Kafka for asynchronous workloads that benefit from durable events and decoupled processing.

Candidate events:

- UserRegistered.
- EmailVerified.
- PhoneVerified.
- MFAEnabled.
- AccountDeactivated.
- ProfileUpdated.
- ConnectionRequested.
- ConnectionAccepted.
- PostPublished.
- PostDeleted.
- MessageCreated.
- MessageRead.
- JobPublished.
- JobApplicationSubmitted.
- NotificationPreferenceUpdated.
- AccountDeletionRequested.

Do not include credentials, raw OTP values, recovery secrets, or unnecessary personal data in event payloads.

Define:

- Topic names.
- Producer ownership.
- Consumer ownership.
- Partition key.
- Ordering scope.
- Schema version.
- Compatibility policy.
- Retention.
- Security controls.
- Retry strategy.
- Dead-letter handling.
- Replay.
- Monitoring.

## Reliable Publication

Use a transactional outbox where a database commit and event publication must be coordinated.

Evaluate Debezium-based CDC or a reliable outbox polling publisher.

Implement idempotent consumers.

Do not claim end-to-end exactly-once effects across independent databases.

## Distributed Transactions

Use local ACID transactions wherever possible.

Use sagas only when genuine distributed business workflows require coordination or compensation.

Identify compensating actions, retries, timeouts, and terminal failures explicitly.

---

# 24. Data Architecture

Use PostgreSQL as the baseline authoritative transactional store.

Use separate databases or appropriately isolated ownership when services are independently deployed.

Design:

- Schema migrations.
- Primary/foreign keys.
- Unique constraints.
- Indexes.
- Transaction isolation.
- Optimistic locking.
- Connection pooling.
- Query performance.
- Backup and restore.
- Data retention.
- Referential integrity.

Use Redis/Valkey for appropriate ephemeral state, counters, and caching.

Use Caffeine for bounded in-process caches where useful.

Evaluate Venice for large-scale derived-data serving.

Evaluate Apache Pinot for real-time analytics when workload volume justifies it.

Use object storage for media and documents.

Define source-of-truth versus derived-data ownership explicitly.

Do not introduce specialized stores without a workload-driven decision.

---

# 25. Local Development Environment

Create a reproducible local environment using Docker Compose and, where justified, local Kubernetes tooling.

Include the locally required subset of:

- PostgreSQL.
- Kafka.
- Keycloak.
- Redis/Valkey.
- OpenSearch.
- Object storage emulator (MinIO or supported LocalStack S3).
- Mailpit/MailHog for email testing.
- Mock SMS delivery provider.
- OpenTelemetry Collector.
- Prometheus.
- Grafana.
- Optional trace/log backends.

Use LocalStack only for the AWS integrations actually supported by the selected edition and version.

Do not assume every AWS service can be emulated.

Provide:

- Environment example files without secrets.
- Startup scripts.
- Health checks.
- Seed data.
- Test identities.
- Deterministic development OTP inspection through a protected local-only mechanism.
- Database migrations.
- Initial setup instructions.
- Reset/reseed commands.

Development OTP tools must not be enabled in production.

A developer should be able to start the essential P0 services without a paid SMS account or production AWS credentials.

---

# 26. AWS Cloud-Native Deployment

Provide infrastructure as code for the selected AWS deployment strategy.

Cover:

- VPC/subnets.
- Security groups.
- EKS or justified alternatives.
- Ingress and load balancing.
- Service identity.
- Network policies.
- PostgreSQL.
- Kafka.
- Caching.
- Object storage.
- Search.
- Secrets.
- Encryption.
- Monitoring.
- Autoscaling.
- Backups.
- Availability zones.

Kubernetes services must include appropriate:

- Startup/readiness/liveness probes.
- Resource requests and limits.
- Graceful termination.
- Connection draining.
- Pod disruption budgets.
- Horizontal autoscaling.
- Deployment strategies.
- Service-account permissions.

Do not provision unnecessary expensive managed resources in development.

Clearly separate Terraform planning from actual resource creation.

Never deploy to paid AWS infrastructure or alter external accounts without authorization.

---

# 27. AWS Well-Architected Six Pillars

Assess the architecture against:

1. Operational Excellence.
2. Security.
3. Reliability.
4. Performance Efficiency.
5. Cost Optimization.
6. Sustainability.

For each pillar define:

- Architectural controls.
- AWS configuration.
- Operational procedures.
- Risks.
- Cost considerations.
- Validation evidence.
- Remaining gaps.

Include disaster recovery, backup restoration, least privilege, change management, performance measurement, and resource utilization.

---

# 28. Resilience and Distributed-System Correctness

Implement and test relevant failure handling for:

- Invalid registration.
- Duplicate account creation.
- Concurrent registration of the same email/phone.
- OTP expiration.
- OTP brute-force attempts.
- SMS provider failure.
- MFA challenge expiration.
- Lost MFA device.
- Password recovery reuse.
- Account takeover attempts.
- Session revocation races.
- Concurrent profile updates.
- Duplicate invitation acceptance.
- Duplicate message sends.
- Kafka redelivery.
- Outbox publication failure.
- Consumer crash before offset commit.
- Out-of-order events.
- Database failover.
- Redis outage.
- Search projection lag.
- Notification delivery failures.
- Media processing failures.
- Deployment schema incompatibility.

For each relevant scenario:

- Specify the invariant.
- Detect failure.
- Bound retries.
- Prevent duplicate effects.
- Preserve data integrity.
- Report appropriate errors.
- Recover safely.
- Emit telemetry.
- Verify with tests.

Use Resilience4j and related controls where justified.

Do not retry authorization failures or invalid business requests.

---

# 29. Observability, Monitoring, and SRE

Instrument services using OpenTelemetry.

Include:

- HTTP/gRPC request metrics.
- p50/p95/p99 latency.
- Error rates.
- Authentication success/failure.
- OTP challenge generation/verification results.
- SMS/email delivery outcomes.
- MFA challenge failures.
- Database pool metrics.
- Kafka lag.
- Outbox backlog.
- Cache hit ratios.
- Search indexing lag.
- Messaging delivery latency.
- Job application outcomes.
- Service health.
- Resource utilization.

Implement:

- Structured logs.
- Distributed trace propagation.
- Correlation/causation IDs.
- Metrics endpoints.
- Service dashboards.
- Actionable alert definitions.
- SLOs/SLIs.
- Runbooks.

Avoid sensitive identifiers as high-cardinality metric labels.

Never log passwords, OTPs, passkeys, recovery secrets, raw access tokens, or private message bodies.

---

# 30. Performance and Scalability

Define workload assumptions and candidate targets for:

- Registration and verification throughput.
- Login latency.
- Profile reads/writes.
- Connection operations.
- Feed generation.
- Post interactions.
- Search latency.
- Messaging latency.
- Notifications.
- Analytics queries.

Include proposed p95/p99 budgets, concurrency levels, request throughput, cache performance, database saturation thresholds, and Kafka lag limits.

Provide load testing with a suitable open-source tool such as k6 or Gatling.

Include:

- Load tests.
- Stress tests.
- Spike tests.
- Soak tests.
- Capacity analysis.
- Hot partition analysis.
- Query performance review.
- Connection pool tuning.
- JVM profiling/GC analysis where justified.

Label numerical goals as proposed until validated.

---

# 31. DevSecOps and CI/CD

Implement CI/CD controls for:

- Compilation.
- Unit tests.
- Integration tests.
- API contract checks.
- Schema compatibility.
- Code formatting.
- Static analysis.
- Dependency vulnerability scanning.
- Secret scanning.
- Container image scanning.
- SBOM creation.
- Build artifact provenance.
- Database migration verification.
- Container publishing.
- Deployment validation.
- Rollback procedures.

Use compatible open-source tooling and existing repository conventions.

Support safe schema evolution using expand/migrate/contract where necessary.

Avoid irreversible migrations in uncontrolled rolling deployments.

---

# 32. Automated Testing Requirements

Use a risk-based test strategy.

## Unit Tests

Cover:

- Business invariants.
- Domain entities.
- Validation.
- State machines.
- Authorization policy decisions.
- Idempotency logic.

## Integration Tests

Use Testcontainers where appropriate for:

- PostgreSQL.
- Kafka.
- Redis.
- Search infrastructure if required.

Test actual persistence constraints, transactions, messaging behavior, and adapter failures.

## Identity and Security Tests

Test:

- Email registration and verification.
- Mobile registration and OTP verification.
- Email/password login.
- Mobile/password login.
- MFA enrollment and challenge.
- Passkey integration.
- Invalid OTP.
- Expired OTP.
- Replayed OTP.
- Rate limiting.
- Duplicate registration.
- Password recovery.
- Session revocation.
- Unauthorized access.
- Object-level access control.
- Account linking conflicts.

## Contract Tests

Validate REST/OpenAPI and gRPC/Protobuf contracts.

Validate Kafka event schema compatibility.

## E2E Backend Workflows

Test:

1. Registration to verified account.
2. Login to authorized profile retrieval.
3. Profile update to search projection.
4. Connection request to accepted relationship.
5. Post publishing to feed appearance.
6. Message sending to persistent conversation history.
7. Job application submission.
8. Notification generation and read-state update.
9. Privacy changes affecting downstream visibility.
10. Account deletion request and downstream processing.

Include negative, concurrency, and retry scenarios.

Do not claim an integration test passed if it used an unrelated mock instead of the claimed real dependency.

---

# 33. Implementation Roadmap

Implement the platform incrementally.

## Phase 0 — Repository Audit and Architecture Decisions

- Inspect repository.
- Extract screenshot requirements.
- Verify LinkedIn reference technologies.
- Define bounded contexts.
- Select initial service boundaries.
- Establish API/event conventions.
- Produce a concise implementation backlog.

## Phase 1 — Foundation

- Build configuration.
- Shared engineering conventions.
- Infrastructure adapters.
- Database migrations.
- Local Docker environment.
- Keycloak integration.
- Error model.
- OpenTelemetry instrumentation.
- Testing foundation.

## Phase 2 — Identity and Authentication (P0)

Complete:

- Email registration.
- Mobile registration.
- Email verification.
- SMS OTP verification.
- Email/password login.
- Mobile/password login.
- Password recovery.
- MFA.
- Session management.
- Passkeys where supported in the selected P0 identity design.
- Security/audit tests.

Do not continue to expose other protected APIs as complete while the authentication foundation is insecure or nonfunctional.

## Phase 3 — Profile and Network

- Profiles.
- Experience.
- Education.
- Certifications.
- Projects.
- Skills.
- Recommendations.
- Connection requests.
- Connection acceptance.
- Following.
- Privacy enforcement.

## Phase 4 — Content and Feed

- Posts.
- Comments.
- Reactions.
- Activity events.
- Feed retrieval.
- Feed projections.
- Ranking baseline.

## Phase 5 — Messaging and Notifications

- Conversation persistence.
- Send/receive.
- Real-time delivery.
- Unread counts.
- Notification processing.
- Preference enforcement.

## Phase 6 — Jobs, Companies, Search, and Media

- Job lifecycle.
- Applications.
- Company management.
- Global search.
- Search indexing.
- Secure media storage.

## Phase 7 — Analytics and Advanced Capabilities

- Profile analytics.
- Real-time aggregation.
- Advanced recommendations.
- Optional Venice/Pinot workloads.
- Recruiter/premium capabilities when scoped.

## Phase 8 — Production Hardening

- Security review.
- Concurrency tests.
- Performance tests.
- Failure injection.
- Backup/restore tests.
- Deployment validation.
- Operational runbooks.
- Production readiness assessment.

Complete and verify a working vertical slice before expanding service complexity.

---

# 34. Required Implementation Artifacts

Where the project requires them, produce:

- Working Java application modules/services.
- Gradle or Maven build configuration.
- Dockerfiles.
- Docker Compose configuration.
- Database migrations.
- OpenAPI specifications.
- Protobuf definitions.
- Event schemas.
- Kafka producer/consumer code.
- Persistence adapters.
- Authentication/authorization integration.
- Keycloak configuration/extensions where justified.
- OTP provider interfaces and implementations.
- Local email/SMS test adapters.
- Infrastructure as code.
- Kubernetes manifests or Helm charts.
- OpenTelemetry configuration.
- Monitoring dashboards.
- Automated tests.
- Architecture Decision Records.
- README and local startup instructions.
- Relevant Mermaid diagrams.
- Deployment and operational guidance.

Do not generate unused framework scaffolding or speculative services.

---

# 35. Requirements Traceability

Assign stable identifiers:

- `BE-AUTH-001` — Email registration.
- `BE-AUTH-002` — Mobile registration.
- `BE-AUTH-003` — Email/password authentication.
- `BE-AUTH-004` — Mobile/password authentication.
- `BE-AUTH-005` — Email verification.
- `BE-AUTH-006` — Mobile OTP verification.
- `BE-AUTH-007` — MFA.
- `BE-AUTH-008` — Password recovery.
- `BE-AUTH-009` — Session management.
- `BE-AUTH-010` — Passkey lifecycle.
- `BE-PROFILE-001` — Professional profile.
- `BE-NETWORK-001` — Connection management.
- `BE-CONTENT-001` — Content publishing.
- `BE-FEED-001` — Feed generation.
- `BE-MESSAGING-001` — Messaging.
- `BE-JOBS-001` — Job discovery.
- `BE-APPLICATION-001` — Job applications.
- `BE-NOTIFICATION-001` — Notifications.
- `BE-SEARCH-001` — Search.
- `BE-PRIVACY-001` — Privacy management.

Expand the catalog for all required operations.

Trace each requirement to:

- Source or business objective.
- Domain owner.
- API or event contract.
- Persistence model.
- Authorization policy.
- Failure behavior.
- Automated tests.
- Implementation status.
- Verification status.

---

# 36. Definition of Done

A backend feature is complete only when:

1. Business rules are implemented.
2. Input validation is enforced.
3. Authentication and authorization are correct.
4. Data persistence works.
5. Transaction boundaries are appropriate.
6. Concurrency and duplicate requests are handled.
7. Relevant APIs/events are implemented.
8. Error responses are consistent.
9. Relevant telemetry is available.
10. Unit and integration tests pass.
11. Important failure paths are covered.
12. API/event contracts are documented.
13. Configuration and secrets are managed correctly.
14. No known critical security defect remains.
15. Local reproduction instructions exist.

A successful build is not sufficient evidence of production readiness.

---

# 37. Final Engineering Report

After implementation, provide:

## A. Architecture Summary

Actual service/module structure, bounded contexts, data ownership, communication patterns, and technology decisions.

## B. LinkedIn Technology Alignment

Document which choices match publicly verified LinkedIn technologies, which are open-source replacements, and which are AWS/project-specific.

## C. Implemented Functionality

List completed backend capabilities and requirement IDs.

## D. Authentication Verification

Report the actual status of:

- Email registration.
- Mobile registration.
- Email verification.
- SMS OTP verification.
- Email/password login.
- Mobile/password login.
- MFA.
- Passkeys.
- Password recovery.
- Session revocation.

Distinguish working local SMS/email mocks from real delivery-provider integrations.

## E. Contracts and Data

Summarize API specifications, events, schemas, migrations, and persistence decisions.

## F. Tests and Verification

Report actual commands executed, pass/fail results, and unverified areas.

## G. Deployment and Operations

Describe local setup, AWS deployment artifacts, health checks, telemetry, backup/restore, and operational gaps.

## H. Risks and Remaining Work

List missing features, unresolved dependencies, security risks, scale limitations, and outstanding production-readiness gates.

---

# 38. Final Execution Instructions

**Design and implement the LinkedIn-inspired backend platform using the two uploaded requirements documents, all 18 LinkedIn screenshots, verified LinkedIn engineering references, and the consolidated requirements above.**

Use this execution sequence:

**Repository Inspection → Screenshot Analysis → Functional Requirement Consolidation → Public LinkedIn Technology Verification → Domain Modeling → Architecture Decisions → Foundation Setup → Identity Implementation → Domain Services → API Contracts → Event Integration → Data Persistence → Security → Observability → Failure Handling → Automated Testing → Deployment Artifacts → Verification → Final Report.**

Do not stop after generating a functional requirements document or HLD.

Do not build only controller-level CRUD demonstrations.

Do not assume services are production-ready because they run locally.

Do not implement an insecure custom authentication mechanism when a standards-based, well-maintained identity solution is available.

Do not create unnecessary microservices or introduce complex distributed infrastructure without an explicit requirement.

**Prioritize complete, secure email/mobile registration, verification, password authentication, MFA, and session management as the foundation of the platform.**

Use the publicly documented LinkedIn technology ecosystem wherever practical, with modern gRPC/Protobuf service communication, Apache Kafka for suitable event-driven workloads, and carefully justified use of technologies such as Venice, Pinot, and Samza.

Deploy the system through an AWS-oriented cloud-native architecture with reproducible local development and clear infrastructure ownership.

**The goal is a functional, secure, scalable, observable, maintainable, and operationally credible backend—not merely an architecture diagram or a collection of microservice skeletons.**

Begin immediately with the repository assessment, verified technology matrix, and initial implementation plan. Then execute the highest-priority vertical slice and continue implementation and verification within the authorized scope.