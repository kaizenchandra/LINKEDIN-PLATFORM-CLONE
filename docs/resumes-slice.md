# Plain-text application resumes

This increment adds optional UTF-8 `.txt` resumes to the existing job application workflow. PDF/DOCX ingestion, antivirus/CDR processing and S3 storage remain pending. It is a deliberately bounded text attachment capability, not completion of the broader media domain.

| Requirement | Source | Implementation | Evidence |
|---|---|---|---|
| INT-RESUME-001 | Explicit resume uploads/internal applications | Job form file selection, preview/remove, consent and atomic submission | Application browser journey |
| BE-RESUME-002 | Explicit private media authorization | Applicant/owner/current recruiter download; no public URLs | PostgreSQL tests and recruiter revocation browser journey |
| BE-RESUME-003 | Explicit duplicate/failure prevention | Resume equality in retry checks; application/attachment/audit transaction | Backend retry/validation checks and delayed-response browser test |
| FE-RESUME-004 | Explicit responsive themes/accessibility | Shared form, bounded preview and download error state | Desktop/mobile dark screenshots and axe checks |

## Contract and persistence

PUT /jobs/{jobId}/application accepts the existing statement and an optional ResumeSubmission with fileName and text. The file must have a `.txt` name of at most 120 characters without path separators or control characters. The server accepts nonblank Unicode text of at most 20,480 UTF-8 bytes. It rejects unpaired surrogates and control characters except tab, carriage return and newline. Browser decoding rejects malformed UTF-8 and checks the selected file's size before reading it. The standard decoder removes a leading UTF-8 BOM; remaining decoded text is preserved without whitespace normalization.

The supplied filename is validated but not retained. V26 stores application_resume(application_id, body), with a primary/foreign key and a database byte-size constraint. Application, resume and submission audit row commit in the same PostgreSQL transaction; there is no staged object or orphan upload. This trades object-store scalability for simple atomicity at the current 20 KiB bound. Resume text is not returned in application lists/history and is not put in Kafka or WebSocket messages. The application DTO includes only hasResume.

The authenticated, CSRF-protected endpoint bounds raw JSON to 192 KiB before deserialization, including requests without Content-Length. This accommodates escaped Unicode plus the application statement. Oversized requests return a fixed RFC 9457-style 413 problem without input content. This endpoint-specific bound is not a platform-wide rate-limit or storage quota.

Identical normalized statement and identical resume text/presence return the existing application, including after job closure or terminal status. A different filename with identical content is a valid retry because filenames are not business data. Changed or omitted resume text conflicts if a resume was previously stored. Resume replacement and deletion independent of application retention are not implemented.

GET /applications/{id}/resume authorizes the applicant, owner or current active recruiter in the database query. Unknown, missing and unauthorized resumes return 404. The response is UTF-8 text/plain with no-store, nosniff, sandbox CSP and a fixed attachment filename resume.txt. The UI fetches it through the bounded request helper, reports errors, clears the parent on session expiry, and revokes its temporary browser object URL after download. It never renders the attachment as HTML. A successful read authorized before a later revocation cannot be recalled.

## UX and privacy

Members select a file, inspect a plain-text preview, remove it or submit it with the statement. Invalid selection blocks submission until replaced or removed. Changing the attachment resets sharing consent. Files remain in component memory until submission; there is no localStorage draft. Reloading retrieves the persisted receipt and download action. A lost submission response preserves the draft for an explicit identical retry. The tracker exposes the same authorized download to applicants and recruiting staff.

The disclosure includes any contact information the applicant deliberately writes into the resume. Withdrawal retains the application and resume under the existing application policy. Account deletion/export, retention expiry, automated storage encryption configuration and malware/document processing are not delivered by this slice.

## Security rationale and remaining work

The design follows [OWASP file-upload guidance](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) on limited types/sizes, authorization, CSRF, generated download names and storage outside the web root. Plain-text content is treated as untrusted and downloaded as an attachment. There is no document parser or malware-scanning claim. Supporting PDF/DOCX requires a separate private object-store, quarantine, scanning/CDR and release lifecycle before recruiter access.

Existing request metrics cover submission and download endpoints. Dedicated storage metrics, download audit policy, quotas, production encryption, retention/export/deletion and AWS deployment remain backlog. Test and build outcomes are recorded in README after execution.
