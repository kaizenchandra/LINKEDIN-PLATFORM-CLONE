# MASTER PROMPT — Production-Grade LinkedIn Clone Frontend Development

## 1. Role and Mission

Act as a **Principal Frontend Architect, Staff Frontend Engineer, Senior UI/UX Designer, Design Systems Architect, and Quality Engineering Lead** with extensive experience building enterprise-grade SaaS, professional networking, and high-traffic social platforms.

Your task is to **design, architect, implement, test, and refine a complete LinkedIn-inspired professional networking frontend application**, using the uploaded LinkedIn screenshots as the primary visual and functional reference.

Deliver a working application, not merely a requirements document, wireframe, prototype, or collection of static pages.

The final application must be:

- Modern, immersive, elegant, visually polished, and professional.
- Clean, minimalist, and easy to navigate.
- Functionally comprehensive, with working interactive controls.
- Consistent across pages, components, and workflows.
- Fully responsive across desktop, tablet, and mobile.
- Accessible, targeting WCAG 2.2 AA.
- Equipped with light, dark, and system theme support.
- Architecturally maintainable, testable, scalable, and secure.
- Designed for integration with production backend APIs.
- Verified through appropriate automated and manual checks.

**Primary principle: Build a functional professional networking platform with screenshot-grounded visual fidelity, not a static screenshot reproduction.**

The screenshots are design references. Use an original product identity and appropriately licensed or original assets rather than copying LinkedIn trademarks, proprietary images, advertising creatives, or personal data.

---

# 2. Source of Truth and Priority

Use the following inputs in this priority order:

1. Existing repository instructions, architecture, dependencies, and established conventions.
2. Uploaded screenshots for observed visual details, page composition, and visible functionality.
3. Explicit product and functional requirements in this prompt.
4. Established accessibility, security, responsiveness, and frontend engineering best practices.
5. Reasonable, documented implementation assumptions where evidence is unavailable.

When requirements conflict, preserve repository compatibility and explicitly specified functionality while keeping screenshot-derived UI behavior wherever practical.

Do not silently substitute generic social-media layouts for the provided references.

Do not infer unseen functionality as something directly observed.

## 2.1 Screenshot Evidence Classification

Classify screenshot-derived requirements as:

- **Observed:** Directly visible in the screenshot.
- **Inferred:** Reasonably implied by visible controls.
- **Recommended:** Added to create a functional, accessible, production-quality application.
- **Unknown:** Cannot be determined from available references.

For example, a visible edit icon confirms an edit affordance but does not establish whether LinkedIn opens a modal, drawer, or separate page. Choose an appropriate implementation and mark that detail as an inferred or recommended design decision.

## 2.2 Screenshot Inventory

Analyze all 18 screenshots individually.

| Reference | Page / visible content |
|---|---|
| S01 | Main profile header, cover, avatar, professional identity, Open to Work, profile actions |
| S02 | Opened account/profile dropdown and navigation |
| S03 | Notifications list, filters, read/unread states |
| S04 | Messaging workspace, conversations, search, filters, selected message |
| S05 | Jobs recommendations, preferences, job tracker, job cards |
| S06 | My Network, invitations, connections, Grow/Catch Up |
| S07 | Settings — Account Preferences |
| S08 | Settings — Sign In and Security |
| S09 | Settings — Visibility |
| S10 | Settings — Data Privacy |
| S11 | Settings — Advertising Data |
| S12 | Settings — Notifications |
| S13 | Profile Analytics, About, Featured |
| S14 | Experience |
| S15 | Education and Connected Apps |
| S16 | Licenses and Certifications |
| S17 | Projects and Skills |
| S18 | Recommendations and Languages |

Verify this inventory against the actual images.

Record screenshot coverage before implementation. Do not omit secondary controls, section footers, action icons, counts, verification indicators, tabs, badges, contextual menus, or truncated content.

Where portions of a screen are cropped or hidden, do not assume their exact appearance.

---

# 3. Execution Requirements

**This is a frontend development task, not solely a documentation task.**

Perform the work in the following order:

1. Inspect the existing repository and applicable instructions.
2. Identify the installed framework, dependency versions, routing architecture, design system, and project conventions.
3. Analyze the screenshots and extract page/component requirements.
4. Build a concise implementation plan and requirements traceability matrix.
5. Establish or refine the frontend architecture and design tokens.
6. Implement shared components and application layouts.
7. Implement the required pages and their complete user interactions.
8. Integrate available backend APIs or create a clearly separated development mock layer.
9. Implement responsive layouts, light/dark/system themes, and accessibility.
10. Test critical workflows, handle failure cases, and refine visual fidelity.
11. Run relevant verification commands.
12. Report completed features, tests performed, unresolved issues, and limitations.

Do not stop after producing an FRD or implementation plan unless the environment genuinely prevents code changes.

Resolve routine implementation decisions independently.

Ask for clarification only when an unresolved decision materially affects correctness, architecture, security, or compatibility.

Preserve unrelated repository changes.

---

# 4. Preferred Frontend Technology Stack

For a new project, use the following reference stack, selecting compatible, currently maintained versions.

| Concern | Preferred technology |
|---|---|
| Framework | Next.js App Router |
| UI library | React |
| Language | TypeScript, strict mode |
| Styling | Tailwind CSS |
| Accessible UI | shadcn/ui and Radix-based primitives where suitable |
| Icons | Lucide React |
| Server state | TanStack Query where appropriate |
| Forms | React Hook Form |
| Validation | Zod |
| Theme management | A compatible theme solution with system preference support |
| Local state | React state/context; lightweight store only when justified |
| API integration | Typed fetch client, OpenAPI-generated types if available |
| API mocking | MSW or an equivalent isolated mock adapter |
| Unit/component tests | Vitest and React Testing Library |
| E2E tests | Playwright |
| Accessibility testing | axe-core / Playwright accessibility checks |
| Formatting/linting | Existing repository tooling |

If the repository already has a working technology stack, follow it rather than migrating unnecessarily.

Verify compatibility before adding or upgrading dependencies.

Avoid introducing large state-management frameworks, animation libraries, or additional styling systems without a clear requirement.

---

# 5. Product Experience and Visual Design Direction

Build a professional networking platform with a **modern, immersive, refined, premium SaaS aesthetic**.

## 5.1 Visual Principles

The interface should feel:

**Elegant:** Refined typography, subtle borders, controlled depth, and balanced contrast.

**Minimalist:** Clear hierarchy, comfortable whitespace, purposeful components, and limited visual noise.

**Professional:** Trustworthy layout, consistent business-oriented components, and predictable interactions.

**Immersive:** Smooth transitions, contextual interactions, responsive feedback, and coherent page navigation.

**Modern:** Contemporary component styling, adaptive layouts, thoughtful iconography, and polished states.

**Accessible:** Strong readability, keyboard operability, appropriate touch targets, and perceptible focus states.

Avoid excessive glassmorphism, heavy gradients, unnecessary animations, oversized cards, inconsistent shadows, and visually cluttered dashboards.

## 5.2 Layout Fidelity

Use the screenshots to guide:

- Header proportions.
- Page content widths.
- Three-column versus two-column compositions.
- Sidebar placement.
- Card spacing.
- Typography hierarchy.
- Avatar sizes.
- Profile hero proportions.
- Button styling.
- Tab and filter styling.
- Active navigation indicators.
- Notification highlights.
- Selected conversation states.
- Settings card grouping.
- Sticky and floating elements.

Prioritize visual similarity in information hierarchy and interaction design while maintaining an original visual identity.

---

# 6. Design System Implementation

Create a consistent, token-driven design system.

## 6.1 Semantic Color Tokens

Implement semantic tokens for:

- Background.
- Foreground.
- Surface.
- Elevated surface.
- Muted surface.
- Primary action.
- Primary hover.
- Secondary action.
- Accent.
- Border.
- Input.
- Focus ring.
- Success.
- Warning.
- Destructive.
- Information.
- Selected item.
- Unread notification.
- Disabled control.
- Overlay.
- Shadow.

Use CSS variables or an equivalent token system so the theme can change without rewriting component styles.

## 6.2 Theme Support

Implement:

1. Light mode.
2. Dark mode.
3. System mode.

Requirements:

- Theme switching without reload.
- Persistent user preference.
- System preference detection.
- Correct initial theme rendering.
- No flash of an incorrect theme.
- Accessible contrast.
- Theme-aware icons, dropdowns, drawers, dialogs, and tooltips.
- Consistent hover, focus, selected, and disabled states.
- No hard-coded light-only backgrounds in reusable components.
- Support for reduced motion.

The screenshots primarily show light mode. Derive dark mode as an accessible, visually coherent extension rather than claiming it is directly observed.

## 6.3 Typography

Use a refined, highly legible type system.

Define:

- Display and heading hierarchy.
- Page titles.
- Section headings.
- Card titles.
- Body text.
- Secondary text.
- Captions.
- Metadata.
- Navigation labels.
- Button labels.
- Form labels.
- Error and helper text.

Support appropriate truncation, word wrapping, expansion, and responsive scaling.

## 6.4 Spacing and Surfaces

Create shared tokens for:

- Spacing scale.
- Border radius.
- Border width.
- Card padding.
- Page gutters.
- Grid gaps.
- Navigation height.
- Content max-width.
- Sidebar widths.
- Button heights.
- Icon dimensions.
- Shadows and elevation.
- Transition durations.

Use values approximated from screenshots where appropriate, documenting any significant design adjustments.

## 6.5 Component States

Every interactive component must support relevant states:

- Default.
- Hover.
- Focus-visible.
- Active.
- Selected.
- Disabled.
- Loading.
- Success.
- Error.
- Pending.
- Expanded.
- Collapsed.

Interactive state must be visible without relying only on color.

---

# 7. Global Application Shell and Navigation

Implement the reusable application shell.

## 7.1 Desktop Header

Include:

- Original product brand/logo.
- Global search.
- Home.
- My Network.
- Jobs.
- Messaging.
- Notifications.
- Profile menu.
- Business/organization menu where applicable.
- Additional business/advertising navigation where implemented.
- Theme access.

Match the observed navigation density, alignment, icon/label hierarchy, and active-state treatments.

## 7.2 Profile Dropdown

Implement the account menu containing appropriate navigation to:

- Profile.
- Premium features.
- Settings and privacy.
- Help.
- Language.
- Posts and Activity.
- Managed organizations.
- Job posting account.
- Sign out.

Use role-aware visibility for actions requiring appropriate permissions.

Menus must close correctly on dismissal, keyboard interaction, route changes, and outside clicks.

## 7.3 Application Layouts

Build reusable layouts for:

- Three-column dashboard/feed.
- Profile with right sidebar.
- Network with left navigation.
- Jobs with filters/sidebar.
- Messaging split-pane workspace.
- Notifications list.
- Settings category navigation.
- Focused authentication/onboarding screens.

Support sticky header behavior where appropriate.

Include a floating messaging dock on relevant desktop pages if within the implemented scope.

Avoid duplicated global headers and inconsistent content containers.

---

# 8. Full Functional Module Requirements

Implement the modules below using reusable domain components.

Prioritize screenshot-covered features for visual fidelity and complete their critical interactions. Implement additional product capabilities according to the release plan.

## 8.1 Authentication and Onboarding

Build:

- Sign in.
- Registration.
- Email verification flow.
- Password recovery.
- Reset password.
- OAuth entry points where supported.
- MFA UI where supported.
- Session expiration handling.
- First-time profile onboarding.
- Profile photo selection.
- Headline, experience, location, and skills entry.
- Onboarding progress.
- Profile completion.

Provide:

- Field validation.
- Password visibility controls.
- Submission states.
- Inline errors.
- Accessible error announcements.
- Success feedback.
- Route guards where appropriate.
- Safe return-to navigation.

Do not implement insecure fake authentication while representing it as real security.

When no identity backend is available, use a clearly labeled development-only mock authentication adapter.

## 8.2 Home Feed and Content Discovery

Build a functional professional feed with:

- Feed cards.
- Author avatar and details.
- Post timestamp.
- Post visibility.
- Text posts.
- Image posts.
- Video/document cards where supported.
- Article previews.
- Polls where supported.
- Reactions.
- Comments.
- Replies.
- Reposts.
- Save/unsave.
- Share.
- Follow/unfollow.
- Report/hide actions.
- Feed refresh.
- Pagination or infinite scrolling.

### Post Composer

Include:

- Post creation dialog/editor.
- Text input.
- Mentions and hashtags where supported.
- Image/document attachment UI.
- Attachment preview.
- Upload progress.
- Visibility selection.
- Draft handling.
- Publish action.
- Edit/delete actions.
- Unsaved changes confirmation.
- Validation and error recovery.

A newly created post must appear consistently in the relevant feed.

A reaction must visibly update its state and count.

A deleted post must disappear from affected local views after confirmed completion.

Do not implement fake action feedback without updating the underlying frontend data model.

## 8.3 Professional Profile

This is one of the highest-priority modules because multiple screenshots document it in detail.

### Profile Hero

Implement:

- Cover photo.
- Cover editing action.
- Avatar.
- Avatar editing action.
- Display name.
- Verification indicator.
- Professional headline.
- Current organization.
- Location.
- Contact information.
- Connection count.
- Open-to-work card.
- Profile action toolbar.
- Public profile information.
- Profile language.
- Profile-related sidebars.

### Profile Actions

Implement supported workflows for:

- Open to.
- Add section.
- Add custom button.
- Resources.
- Edit intro.
- Contact info.
- Show details.
- Profile enhancement where in scope.

### Profile Sections

Build reusable editable sections for:

- Analytics.
- About.
- Featured.
- Activity.
- Experience.
- Education.
- Licenses and certifications.
- Projects.
- Skills.
- Endorsements.
- Recommendations.
- Languages.
- Publications.
- Volunteer experience.
- Additional professional achievements.

For each editable section, support appropriate CRUD behavior, validation, cancel/save, data refresh, loading states, and failure handling.

### Experience

Support:

- Job title.
- Employment type.
- Organization.
- Logo.
- Location.
- Location type.
- Start/end dates.
- Current position.
- Description.
- Associated skills.
- Associated projects.

### Education

Support institution, degree, field of study, date range, optional details, and entry management.

### Certifications

Support issuer, name, issue/expiry dates where appropriate, credential ID, credential URL, and safe external navigation.

### Projects

Support title, description, date range, associated organization, and skills.

### Recommendations

Support received/given tabs, expanded content, request management where in scope, and pending status.

### Profile Analytics

Display relevant metrics such as profile views, post impressions, and search appearances, with privacy-aware data access.

Do not fabricate real analytics. Use labeled fictional demo data in mock mode.

## 8.4 My Network

Implement:

- Network sidebar.
- Connections.
- Followers/following.
- Pending invitations.
- Sent invitations.
- Accept.
- Ignore.
- Withdraw.
- Remove connection.
- Suggested connections.
- Mutual connections.
- Connect/follow actions.
- Grow/Catch Up tabs.
- Network search where supported.

Model connection state transitions explicitly.

Prevent duplicate submission.

Ensure accepted invitations move out of the pending list and update applicable connection views.

## 8.5 Messaging

Recreate the screenshot-grounded messaging workspace.

Include:

- Conversation list.
- Conversation search.
- Selected conversation.
- Message thread.
- New conversation composer.
- Contact selection.
- Message input.
- Send action.
- Message timestamps.
- Read/unread states.
- Starred conversations.
- Archive/mute actions.
- Focused/Jobs/Unread/Connections/InMail/Starred filters where applicable.
- Responsive split-pane layout.
- Desktop messaging dock.

Support:

- Sending state.
- Sent state.
- Failed send.
- Retry.
- Message deduplication.
- Conversation ordering.
- Reconnection behavior.
- Unread count synchronization.

Use actual backend events when available.

If running against mock data, simulate incoming messages only as explicit demo behavior; never imply that a real remote user is connected.

## 8.6 Jobs

Implement:

- Recommended jobs.
- Search jobs.
- Job filters.
- Job preferences.
- Job details.
- Save/unsave.
- Dismiss recommendation.
- Job tracker.
- Application history.
- Application status.
- Easy Apply where supported.
- External application redirects.
- Job alerts.
- Post a job where authorized.
- Manage job posts.
- Career insight sections where in scope.

Job cards should preserve screenshot-observed hierarchy:

- Job title.
- Company.
- Logo.
- Location.
- Work arrangement.
- Labels.
- Status.
- Recommended/promoted indicators.
- Contextual actions.

Clearly separate promoted placements and premium-gated features from ordinary recommendations.

Never display fictional job application completion as a real submission to an employer.

## 8.7 Notifications

Implement:

- Notifications page.
- Notification list.
- All/Jobs/My Posts/Mentions filters.
- Unread highlights.
- Unread indicator.
- Timestamp.
- Notification source.
- Context action.
- Navigation to related content.
- Mark read/unread.
- Manage notification preferences.
- Pagination.
- Unread count synchronization.

Notification actions must update the relevant application state.

## 8.8 Global Search

Implement a unified search interface for:

- People.
- Jobs.
- Companies.
- Posts.
- Articles/topics where supported.

Include:

- Search suggestions.
- Keyboard navigation.
- Search result tabs.
- Filters.
- Sorting.
- Pagination.
- Search history where appropriate.
- Loading.
- No results.
- Error recovery.
- URL-driven query state.

Search must produce actual results from its configured data source, whether real API or explicitly mocked data.

## 8.9 Company Pages

Implement:

- Company overview.
- Company logo and banner.
- Industry.
- Company size.
- Location.
- Website.
- Company posts.
- Employees.
- Jobs.
- Follow/unfollow.
- Related companies.

Differentiate visitor and administrator functionality.

## 8.10 Additional Features

Evaluate and phase:

- Groups.
- Events.
- Newsletters.
- Creator features.
- Premium.
- Recruiter functionality.
- Learning.
- Analytics.
- AI-assisted profile/content suggestions.
- Connected applications.
- Organization inboxes.

Do not silently present unimplemented premium or external-provider functionality as working.

---

# 9. Settings — Detailed Implementation Requirements

Treat Settings as a complete, separate feature area.

Use screenshots S07–S12 as the principal reference.

## 9.1 Settings Shell

Implement:

- Settings header.
- Left category navigation.
- Active category indicator.
- Grouped settings cards.
- Section headings.
- Preference rows.
- Current-value labels.
- Navigation chevrons.
- Nested details.
- Responsive settings navigation.

## 9.2 Account Preferences

Include:

- Name/location/industry.
- Demographic information where supported.
- Verification settings.
- Display theme.
- Language.
- Content language.
- Autoplay videos.
- Sound effects.
- Profile photo preferences.

## 9.3 Sign In and Security

Include:

- Email addresses.
- Phone numbers.
- Change password.
- Passkeys where supported.
- Signed-in sessions.
- Remembered devices.
- Two-factor authentication.
- Session revocation.

Sensitive actions must use safe confirmation and authorization-aware flows.

## 9.4 Visibility

Include preferences for:

- Profile viewing visibility.
- Page visit visibility.
- Public profile.
- Email visibility.
- Connection visibility.
- Follow visibility.
- Last name visibility.
- Organization/interests visibility.
- Off-platform visibility.
- Email discoverability.
- Phone discoverability.

## 9.5 Data Privacy

Include:

- Data permissions.
- Data export.
- Search history clearing.
- Demographic information.
- Research data preferences.
- Generative AI data preferences.
- Connection invitation permissions.
- Messaging permissions.
- Research invitations.

## 9.6 Advertising Data

Include the observed advertising preference groups:

- Connections.
- Followed companies.
- Groups.
- Education.
- Skills.
- Job information.
- Employer.
- Display format.
- Profile location.
- Inferred location.
- Interests.

Implement preference state without claiming that the application has an actual targeted-advertising engine.

## 9.7 Notification Settings

Include categories for:

- Job searching.
- Hiring.
- Connections.
- Network updates.
- Posting/commenting.
- Messaging.
- Groups.
- Pages.
- Events.
- News.
- Profile updates.
- Verifications.

Build a reusable settings row and preference detail component system.

Ensure changes persist through the configured persistence adapter.

---

# 10. Reusable Frontend Component Architecture

Use three component layers.

## 10.1 Design-System Primitives

Build or reuse:

- Button.
- IconButton.
- Avatar.
- Badge.
- Card.
- Input.
- Textarea.
- SearchField.
- Select.
- Checkbox.
- Radio.
- Switch.
- Tabs.
- DropdownMenu.
- Popover.
- Dialog.
- Drawer.
- Tooltip.
- Skeleton.
- Spinner.
- Toast.
- Divider.
- Pagination.
- EmptyState.
- ErrorState.

## 10.2 Shared Application Components

Include:

- AppHeader.
- MainNavigation.
- AccountMenu.
- AppShell.
- SidebarNavigation.
- PageContainer.
- ProfileSummaryCard.
- SettingsLayout.
- SettingsSection.
- SettingsRow.
- SectionHeader.
- ConfirmDialog.
- MediaPreview.
- FilterChips.
- SearchResults.
- LoadMoreControls.

## 10.3 Domain Components

Include:

- ProfileHero.
- ProfileAnalytics.
- ExperienceEntry.
- EducationEntry.
- CertificationEntry.
- ProjectCard.
- SkillItem.
- RecommendationCard.
- FeedPost.
- PostComposer.
- ReactionBar.
- CommentThread.
- ConnectionCard.
- InvitationCard.
- ConversationList.
- ConversationRow.
- MessageThread.
- MessageComposer.
- NotificationItem.
- JobCard.
- JobFilterPanel.
- JobDetails.
- CompanyCard.

For every component, establish:

- Responsibility.
- Props.
- State ownership.
- Accessible semantics.
- Event handlers.
- Variants.
- Responsive rules.
- Test coverage.
- Reuse boundaries.

Avoid generic abstractions that obscure domain meaning.

---

# 11. Frontend Architecture and Project Organization

Follow feature-oriented modular architecture.

For a new Next.js project, a possible structure is:

```text
src/
  app/
    (auth)/
    (platform)/
      feed/
      profile/
      network/
      messaging/
      jobs/
      notifications/
      search/
      companies/
      settings/
    layout.tsx

  components/
    ui/
    shared/
    layout/

  features/
    auth/
    feed/
    profile/
    network/
    messaging/
    jobs/
    notifications/
    search/
    companies/
    settings/

  lib/
    api/
    auth/
    errors/
    validation/
    utils/

  providers/
  hooks/
  styles/
  types/

  mocks/
    handlers/
    fixtures/

tests/
  unit/
  integration/
  e2e/
```

Use this as a reference, not a mandate to restructure an existing repository.

## 11.1 Architecture Principles

Apply:

- Single responsibility.
- Composition over inheritance.
- Dependency inversion at meaningful integration boundaries.
- Clear presentation/domain/API separation.
- Feature cohesion.
- Reusable shared components.
- Explicit data ownership.
- Minimal global state.
- Typed contracts.
- Predictable error handling.

Use Clean/Hexagonal Architecture principles pragmatically for frontend integration boundaries.

Do not replicate backend-style repository/service/use-case layers mechanically when they only add indirection.

## 11.2 State Management

Differentiate:

**Server state:** API-backed entities, lists, search results, notifications, jobs, profiles, and messages.

**URL state:** Search terms, filters, pagination, tabs, and shareable navigation context.

**Local UI state:** Open dialogs, dropdowns, temporary selections, and transient expansion state.

**Form state:** Inputs, touched fields, validation, submission status, and unsaved changes.

**Persistent preferences:** Theme and other supported user settings.

Avoid duplicating server-owned data in multiple independent client stores.

## 11.3 Rendering Strategy

Use server rendering or server components for appropriate initial read-oriented views.

Use client components for interactive states and browser-dependent behavior.

Keep client boundaries focused to avoid unnecessarily large JavaScript bundles.

Explicitly handle hydration differences, cache behavior, and data refresh.

---

# 12. Backend Integration and Development Mock Strategy

The frontend must be designed for integration with real backend services.

Use typed contracts wherever available.

## 12.1 API Layer

Provide:

- Centralized API client.
- Request validation.
- Typed response models.
- Error normalization.
- Authentication integration.
- Timeout/cancellation handling.
- Safe retries.
- Consistent pagination.
- Cache invalidation.
- Mutation error handling.

Do not hard-code endpoint assumptions as existing backend capabilities.

If an OpenAPI specification exists, use it as the authoritative contract.

## 12.2 Development Mock Mode

If real backend endpoints are unavailable:

- Build a dedicated mock adapter, preferably using MSW.
- Use realistic fictional data.
- Keep fixtures outside UI components.
- Support CRUD state transitions within the mock environment.
- Simulate loading, empty, and failure conditions.
- Support deterministic demo data for tests.
- Distinguish simulated behavior from real backend persistence.
- Make switching to real API integration straightforward.

Do not scatter `setTimeout` calls and fake success messages throughout components.

Do not use browser storage as a substitute for production authentication or authorization.

A development demo can be functional without claiming real multi-user persistence.

## 12.3 Critical Integration Cases

Handle:

- 401 authentication expiration.
- 403 authorization failure.
- 404 deleted resource.
- 409 conflicting updates.
- 422 validation errors.
- 429 rate limiting.
- 5xx server failures.
- Timeout.
- Offline.
- Partial response.
- Concurrent mutations.
- Duplicate submission.
- Real-time disconnection.

Keep error recovery contextual rather than resetting the entire application unnecessarily.

---

# 13. Responsive Design

Implement mobile-first layouts.

Recommended viewport test coverage:

| Viewport | Width |
|---|---|
| Small mobile | 320px |
| Mobile | 375px |
| Large mobile | 430px |
| Tablet | 768px |
| Laptop | 1024px |
| Desktop | 1440px |
| Wide desktop | 1920px |

Use content-driven breakpoints rather than blindly designing around exact device models.

## Responsive Expectations

**Desktop**

- Full navigation.
- Multi-column layouts.
- Persistent contextual sidebars.
- Split-pane messaging.
- Optional floating messaging dock.

**Tablet**

- Reduced sidebar prominence.
- Adaptive navigation.
- Narrower content areas.
- Contextual drawer behavior.

**Mobile**

- Compact header.
- Bottom or consolidated navigation where appropriate.
- Single-column layouts.
- Full-screen messaging conversations.
- Stacked cards.
- Accessible action menus.
- Mobile-friendly filters.
- Bottom sheets where appropriate.
- Safe-area support.

No horizontal page overflow should occur on supported viewports.

All major workflows must remain usable with touch input.

---

# 14. Accessibility and Interaction Standards

Target WCAG 2.2 AA.

Requirements include:

- Semantic HTML.
- Correct heading hierarchy.
- Keyboard navigation.
- Visible keyboard focus.
- Screen-reader names.
- Accessible icon buttons.
- Accessible tabs and menus.
- Modal focus management.
- Focus restoration.
- Appropriate ARIA use.
- Form labels and validation announcements.
- Error summaries where needed.
- Status announcements.
- Accessible media.
- Reduced-motion preferences.
- Sufficient contrast.
- Touch-target accessibility.
- 200% zoom and responsive reflow.

Use actual buttons for actions and links for navigation.

Avoid click-only `div` elements.

Do not rely solely on hover to expose essential functionality.

---

# 15. Security, Privacy, and Data Handling

Implement frontend security practices appropriate to the architecture.

Include:

- Safe authentication integration.
- Secure handling of sessions.
- No hard-coded secrets.
- No sensitive values in client bundles.
- No tokens or passwords in application logs.
- Safe redirects.
- Rich-text sanitization.
- XSS resistance.
- CSRF considerations where applicable.
- Safe external URL handling.
- Client-side validation.
- Authorization-aware UI rendering.
- Privacy-aware content display.
- Safe file upload UX.
- Confirmation of destructive actions.

Frontend permission checks are usability controls, not replacements for backend authorization.

Use synthetic data for demonstrations.

Do not embed personal contact details, private messages, or identifiable information from screenshots into committed test fixtures.

---

# 16. Complete Functional Interaction Policy

**Every visible interactive control within the implemented scope must have meaningful behavior.**

Do not create decorative buttons that appear functional but do nothing.

For each action, implement one of:

1. Real working functionality.
2. Explicit development-mock functionality.
3. A clear disabled state with an honest explanation if unavailable.

Avoid generic "Coming soon" toasts as substitutes for required functionality.

## Critical Interactions

Validate at least these workflows:

- Open and close account menu.
- Navigate through top navigation.
- Switch light/dark/system theme.
- Open and edit profile information.
- Add and edit experience.
- Add education/certifications/projects/skills.
- Expand and collapse profile content.
- Switch recommendation tabs.
- Accept/ignore invitations.
- Connect/follow/unfollow.
- Filter messages.
- Select conversations.
- Compose and send messages.
- Filter notifications.
- Mark notifications as read.
- Search and filter jobs.
- Save and unsave jobs.
- Edit job preferences.
- Update settings.
- Change privacy preferences.
- Create a post.
- React and comment.
- Search people/companies/jobs.
- Recover from a failed mutation.

Make UI state consistent across routes where the same entity appears.

---

# 17. UI States, Errors, and Recovery

Every major data-driven feature must implement relevant states:

- Initial.
- Loading.
- Skeleton.
- Loaded.
- Empty.
- Filtered empty.
- No search results.
- Pending mutation.
- Success.
- Validation error.
- Authorization failure.
- Network error.
- Server error.
- Rate limited.
- Offline.
- Retry.
- Concurrent update conflict.
- Deleted/unavailable entity.

Use localized recovery where possible.

For optimistic updates:

- Clearly distinguish pending and confirmed states.
- Roll back or reconcile after failure.
- Prevent duplicate actions.
- Avoid data loss.
- Preserve user input when possible.

For forms:

- Validate on appropriate triggers.
- Preserve unsaved data where safe.
- Offer cancellation.
- Warn before discarding changes.
- Display field-specific errors.
- Handle duplicate submit attempts.

---

# 18. Performance and Reliability Requirements

Target strong Core Web Vitals using current recommended thresholds.

Use initial proposed targets such as:

- LCP at or below 2.5 seconds at the 75th percentile.
- INP at or below 200 milliseconds at the 75th percentile.
- CLS at or below 0.1 at the 75th percentile.

Treat targets as engineering goals to validate through measurement, not as verified performance claims.

Implement:

- Route-based code splitting.
- Efficient image loading.
- Image sizing to avoid layout shifts.
- Lazy loading for below-the-fold content.
- Efficient list rendering.
- Cursor-based pagination where appropriate.
- Search request debouncing.
- Request cancellation.
- Cache-aware data fetching.
- Avoidance of unnecessary rerenders.
- Stable React keys.
- Bundle size awareness.
- Efficient initial rendering.
- Reduced-motion support.
- Minimal unnecessary dependencies.

Use list virtualization only where performance measurements or expected data volume justify it.

---

# 19. Observability and Frontend Diagnostics

Introduce lightweight but meaningful frontend observability.

Cover:

- Runtime error boundaries.
- Error reporting integration points.
- Client-side performance metrics.
- Web Vitals collection.
- API failure categorization.
- Route/navigation errors.
- Critical user journey events.
- Feature-level diagnostics.
- Release/version identifiers.
- Privacy-aware telemetry.

Avoid unnecessary user-identifying data collection.

Do not log private messages, credentials, or sensitive profile details.

Ensure observability tooling cannot break the primary application experience.

---

# 20. Testing Strategy

Verify meaningful behavior rather than merely checking that components render.

## 20.1 Unit and Component Tests

Test:

- Validation rules.
- Domain state transitions.
- Theme switching.
- Reusable controls.
- Filters.
- Search interactions.
- Conditional rendering.
- Error handling.
- Settings persistence.
- Profile editing flows.

## 20.2 Integration Tests

Test:

- API adapters.
- Cache invalidation.
- Optimistic updates.
- Failed mutations.
- Authentication expiry.
- Form submissions.
- Pagination.
- Duplicate action prevention.
- Data reconciliation.

## 20.3 E2E Tests

Use Playwright to cover critical user journeys.

At minimum test:

1. Authentication/onboarding where implemented.
2. Global navigation.
3. Profile viewing and editing.
4. Connection invitation handling.
5. Feed publishing and reactions.
6. Messaging.
7. Job search and save actions.
8. Notifications.
9. Settings updates.
10. Theme persistence.
11. Search.
12. Mobile navigation.

## 20.4 Visual Regression

Use uploaded screenshots as visual references.

Capture application screenshots at matching or comparable desktop viewport dimensions.

Compare:

- Header.
- Container widths.
- Grid layout.
- Profile hero.
- Card hierarchy.
- Typography.
- Spacing.
- Borders.
- Selected states.
- Icon alignment.
- Dropdown positioning.
- Settings structure.

Fix meaningful visual discrepancies without relying on brittle absolute positioning.

Also capture dark-mode and mobile snapshots.

## 20.5 Accessibility Verification

Perform automated checks where tooling permits, supplemented with keyboard and focus-flow verification.

Automated accessibility tools do not replace manual inspection.

---

# 21. Development Phases and Implementation Priorities

Use an incremental delivery strategy.

## Phase 0 — Repository and Requirements Audit

- Inspect repository.
- Determine actual technologies.
- Inventory screenshots.
- Map requirements to routes.
- Identify API availability.
- Establish initial acceptance criteria.
- Identify gaps and blockers.

## Phase 1 — Design System and Application Shell

- Tokens.
- Typography.
- Light/dark/system theme.
- Shared components.
- Header.
- Navigation.
- Page layouts.
- Responsive shell.

## Phase 2 — Screenshot-Grounded Core Pages

Implement and visually refine:

- Profile.
- Account dropdown.
- Settings.
- My Network.
- Messaging.
- Notifications.
- Jobs.

These have the richest direct screenshot evidence.

## Phase 3 — Broader Core Product

Implement:

- Authentication/onboarding.
- Feed.
- Post composer.
- Global search.
- Company pages.
- Missing primary user journeys.

## Phase 4 — Data and Interaction Hardening

- API integration.
- Mock adapter cleanup.
- Loading/error states.
- Optimistic mutation handling.
- Privacy and authorization-aware rendering.
- Functional consistency across routes.

## Phase 5 — Quality and Production-Oriented Refinement

- Accessibility.
- Responsive QA.
- Visual regression.
- Performance optimization.
- Test coverage.
- Error diagnostics.
- Build verification.
- Documentation.

Prioritize security, correctness, accessibility, and data integrity throughout all phases, rather than deferring them until the end.

---

# 22. Requirements Traceability

Assign stable requirement IDs:

- `FE-NAV-001`
- `FE-AUTH-001`
- `FE-FEED-001`
- `FE-PROFILE-001`
- `FE-NETWORK-001`
- `FE-MESSAGE-001`
- `FE-JOBS-001`
- `FE-NOTIFICATION-001`
- `FE-SEARCH-001`
- `FE-SETTINGS-001`
- `FE-PRIVACY-001`
- `FE-THEME-001`

For each key requirement, maintain:

| Field | Required information |
|---|---|
| ID | Unique requirement identifier |
| Module | Business feature |
| Description | Testable behavior |
| Screenshot | Supporting reference, if any |
| Evidence | Observed / Inferred / Recommended |
| Priority | P0 / P1 / P2 |
| Route | Page or navigation destination |
| Component | Responsible component |
| Data | Required entity/API |
| Behavior | Interaction states |
| Tests | Acceptance/test references |
| Status | Planned / Implemented / Verified / Blocked |

Do not spend excessive time maintaining documentation at the expense of completing and verifying the application.

---

# 23. Definition of Done

A feature is complete only when:

- Its required UI is implemented.
- Its primary interactions work.
- Navigation works.
- Forms validate correctly.
- Data changes are reflected accurately.
- Loading and failure states are handled.
- Mobile layout is usable.
- Light and dark themes render correctly.
- Keyboard access is supported.
- No obvious console/runtime errors remain.
- Relevant tests pass.
- API/mock behavior is accurately documented.

Do not mark a feature as verified based only on successful compilation.

---

# 24. Final Delivery Requirements

At completion, provide a concise but evidence-based engineering report containing:

### A. Implemented Features

List completed modules, pages, and critical workflows.

### B. Architecture

Summarize the actual framework, feature organization, state handling, design system, and API boundary.

### C. Visual Fidelity

Describe which screenshot-covered screens were implemented and which could not be visually verified.

### D. Backend Integration

Clearly distinguish:

- Real API integrations.
- Development mocks.
- Missing API contracts.
- Features requiring backend support.

### E. Verification

Report commands actually executed and their outcomes, including applicable:

- Type checking.
- Linting.
- Unit tests.
- Integration tests.
- E2E tests.
- Production build.
- Accessibility checks.
- Visual regression checks.

Do not claim tests passed unless they were actually run successfully.

### F. Remaining Work

Identify incomplete functionality, limitations, risks, and blockers.

Do not claim production readiness without supporting evidence.

---

# 25. Strict Engineering Instructions

Follow these principles throughout implementation:

1. **Implement functionality, not just attractive screens.**
2. Treat uploaded screenshots as the primary evidence for observed visual requirements.
3. Preserve important UI hierarchy, navigation patterns, and workflows.
4. Use a coherent, original visual identity.
5. Follow existing repository conventions.
6. Avoid unnecessary dependencies and architectural layers.
7. Build reusable components without excessive abstraction.
8. Keep TypeScript strict and type contracts explicit.
9. Use semantic HTML and accessible UI primitives.
10. Make all interactive controls functional or explicitly unavailable.
11. Implement light, dark, and system themes consistently.
12. Ensure responsive behavior across supported viewports.
13. Keep API and mock infrastructure separate from presentation components.
14. Handle validation, failures, retries, concurrency, and state reconciliation.
15. Never expose secrets or sensitive data in frontend code or telemetry.
16. Avoid hard-coded user-specific information from screenshots.
17. Avoid placeholder functionality masquerading as complete features.
18. Use deterministic fictional demo data when backend services are unavailable.
19. Verify functionality after making changes.
20. Fix regressions introduced by your implementation.
21. Avoid modifying unrelated repository files.
22. Clearly distinguish implemented, tested, mocked, and unverified behavior.
23. Prefer focused, maintainable solutions over speculative complexity.
24. Continue through implementation and verification within the authorized scope rather than stopping after planning.

---

# 26. Final Execution Command

**Build the complete LinkedIn-inspired frontend application using the uploaded screenshots and the comprehensive functional requirements in this prompt.**

Your workflow must be:

**Inspect Repository → Analyze Screenshots → Extract Requirements → Design Architecture → Establish Design System → Build Shared Components → Implement Pages → Connect Functional Interactions → Integrate API/Mocks → Implement Responsive Themes → Test → Refine Visual Fidelity → Verify → Deliver.**

The finished application should combine:

- The professional information architecture demonstrated by the LinkedIn screenshots.
- An elegant, contemporary, immersive, and uncluttered SaaS-style user experience.
- Complete functional workflows appropriate to the implemented release scope.
- Clean, reusable, maintainable frontend architecture.
- Accessible components.
- Consistent light/dark/system theming.
- Responsive desktop, tablet, and mobile experiences.
- Secure, typed backend integration boundaries.
- Reliable UI behavior across successful, pending, and failed operations.
- Measurable performance and verification evidence.

**Start by inspecting the actual project and all available screenshot assets. Then implement the application incrementally, completing and verifying each major feature before proceeding to the next.**

Do not stop at design documentation.

Do not deliver a collection of disconnected static pages.

Do not report incomplete or simulated workflows as production functionality.

**The goal is a cohesive, working, visually refined, enterprise-quality professional networking frontend—not merely a LinkedIn-looking UI.**