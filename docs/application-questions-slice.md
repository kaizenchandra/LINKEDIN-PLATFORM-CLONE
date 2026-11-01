# Versioned job application questions

This slice adds employer-defined plain-text questions to existing jobs and applications. Owners and active recruiters configure up to five questions after publishing a job. Applicants answer required questions before submission; optional answers may be blank. Multi-choice, branching, scoring and automatic rejection are not implemented.

| Requirement | Source classification | Implementation |
|---|---|---|
| INT-QUESTION-001 | Explicit application requirements / authorized recruiter actions | Owner/recruiter question editor, GET/PUT job questions, V28 |
| INT-QUESTION-002 | Explicit server validation / persistent applications | Version check, required-answer validation, atomic snapshots |
| INT-QUESTION-003 | Inferred interaction and recommended consistency detail | Stale-form recovery, immutable prompts and answers, retry reconciliation |
| BE-QUESTION-004 | Explicit authorization and concurrency | Current recruiting grants, company/job locks, private application reads |

## Contract and invariants

GET /jobs/{jobId}/questions returns a no-store versioned question set to authenticated members. A job with no saved form returns version 0 and an empty list. PUT requires recruiting authority, CSRF and the current question version. Questions have client UUIDs, unique within the ordered set, prompts of 1–300 characters and required flags. Empty sets remove questions for future submissions. Every save advances question version, independent of the job-description version. Stale saves return 409; ambiguous results must be reloaded before another edit.

Application submission accepts questionVersion and up to five {questionId,text} answers, each at most 1,000 characters. Omitted version means zero for existing clients applying to jobs without saved questions. The server trims answers, rejects duplicate/unknown IDs (400), missing required answers (422), stale versions (409), control characters and invalid Unicode. The shared 192 KiB JSON request bound also covers question updates and remains sufficient for maximum escaped statement, resume and answers.

V28 stores job_question_form plus question_version and answers JSONB on job_application. SQL constraints bound each JSON array to five elements; Java enforces element shape, text limits and identity uniqueness. Each submitted snapshot contains the original prompt, required flag, question ID and normalized answer. Blank optional answers are represented explicitly. Application, answer snapshot, optional resume, audit and outbox commit in one local transaction.

Question edits and submission acquire organization then job locks, sharing the order used for recruiter revocation and job publishing. If a submission commits first it retains the old snapshot; if the question edit commits first, a stale submission conflicts. Existing snapshots never change after employer edits. Exact retries compare the original question version and snapshot, not the current question set; changed answers cannot overwrite an application. Answer ordering in the request does not affect equality.

## UI, privacy and recovery

The job details page exposes question management only to current recruiting staff. The editor has add/remove, required toggles, save/cancel, reload, unsaved-change warnings, bounded requests and explicit error feedback. Authorization is always rechecked server-side.

The applicant form loads current questions alongside the account and existing receipt. A stale submission preserves the statement, resume and answers. Reloading questions asks before clearing potentially affected answers, keeps answers only when ID/prompt/required flag are unchanged, and resets sharing consent. Required answers block the submit button, but server validation remains authoritative. Failed initial loading has an explicit retry. No answers are stored in browser localStorage.

Applicants and currently authorized recruiting staff see the original answers in a disclosure on the application tracker. They inherit the existing object-level application authorization and session cleanup. Prompts are plain text; answers are rendered as text. Answers are absent from public job/search responses, notifications, Kafka and WebSocket messages. The sharing disclosure now covers question answers.

## Verification and remaining work

PostgreSQL tests cover owner/recruiter permissions and revocation, CSRF, stale saves, duplicate IDs, required/unknown/oversized answers, private snapshots, exact/conflicting retries, one outbox event and concurrent question edit/submission. The browser journey creates questions, reloads persisted settings, encounters a real stale-version conflict, recovers without losing the statement, submits answers and confirms original prompts remain after another employer edit. Build/test counts and screenshot review are recorded in README after execution.

Remaining work includes structured question types, richer form accessibility review, question edit audit history, retention/export/deletion, recruiter notes/interviews, richer resume storage and overall production hardening. No scoring, hiring eligibility decision or production deployment is claimed.

## Question ordering controls

The editor now provides keyboard-accessible Move up/down buttons, disabled at list boundaries, and a status announcement of the new position. Reordering preserves question UUIDs, prompts and required flags, marks the draft dirty, and persists only on Save using the existing expected-version contract. Existing application snapshots retain their original order.

Verified this increment with strict TypeScript, healthy rebuilt local containers and the expanded real-service application-questions browser journey. The test reorders with the keyboard, confirms persisted order after reload, submits an application, reorders again and verifies the original snapshot order. Its desktop/mobile dark accessibility and overflow checks passed. The full backend/browser suites were not rerun for this frontend-only increment; their earlier checkpoint results remain historical.

## Undo removal

Removing a question now retains one undo entry in component memory until save, cancel, reload or another removal. Undo restores the question UUID, prompt, required flag and original position (or the nearest remaining position if the draft became shorter). It is disabled when the five-question limit is reached. Nothing changes in persistence until Save succeeds, and existing optimistic-version checks still apply.

Verified strict TypeScript, healthy local container rebuild and the expanded real-service question journey, including restored optional status/text and subsequent persisted ordering/submission. The full suites were not rerun for this frontend-only increment.
