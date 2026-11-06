# Serious-bug review — MavMatch 2.0

Reviewed the local SQLite build, focusing on account isolation, authorization, data preservation, and concurrent actions. This is a review of this repository; a downloaded Windows copy updates only when its source files are replaced.

## Reproduced and fixed

### Simultaneous writes could fail with HTTP 500

Two overlapping availability replacements produced statuses `[200, 500]`. Two overlapping acceptances for conflicting invitations also produced `[200, 500]`. SQLite ignores `select_for_update()`, and the default deferred transactions could fail when upgrading a read transaction to a write. The existing sequential tests did not cover this.

Fix: begin SQLite transactions in IMMEDIATE mode so writers wait before making transactional reads. The concurrency regressions now require two complete schedule saves, or one accepted invitation and one useful HTTP 409 conflict response. Only one overlapping meeting is confirmed. SQLite lock exhaustion is also translated to a controlled retry response, while unrelated database errors are not hidden.

### A message could be admitted after blocking completed

A paused message request read the block state, another request completed blocking, and the first request then saved the message. The permission check and insert were separate operations. A deterministic interleaving reproduced this before the fix.

Fix: authenticated API mutations execute their permission checks and writes in one transaction. Combined with SQLite IMMEDIATE mode, a block cannot commit in the middle of a message's authorization-and-insert operation. The regression checks message creation against block confirmation.

## Important checks that passed

- Submitted student/sender IDs cannot change which authenticated account is acting.
- A third student cannot read, send into, or report another pair's private conversation.
- Registration cannot grant staff, superuser, or demo privileges through extra JSON fields.
- A stale browser tab cannot mutate another account after the shared login changes.
- Deactivating an account revokes its existing session.
- An outsider cannot delete another student's study group or remove its members.
- Blocking prevents reconnecting and new meeting invitations.
- Removing a course preserves messages, reports, and meeting history while invalidating recommendations.
- User-supplied HTML in messages is displayed as text in the browser workflow.

## Validation and limits

The full SQLite suite passed **38 tests**: 32 API/data tests, 3 Chromium workflows, and 3 concurrency tests. Concurrent requests use independent connections against an isolated file-backed SQLite test database, matching the local app's locking behavior. Browser tests include desktop/mobile, separate students, and failed network requests during authentication.

No cross-account leak, outsider authorization bypass, or destructive course-to-chat cascade was reproduced in the reviewed paths. This is not a guarantee that every possible bug is absent. The app remains a local demo with documented sample credentials and no email verification; public deployment and real-student operational security were outside this review. PowerShell itself was not executed on Windows here.

## Matching follow-up

Two additional issues were reproduced and fixed:

- **Mixed-state recommendations:** while one student dropped a shared course and then added overlapping availability, another request combined the old enrollment with the new hours. It recommended that student even though there was no instant when both requirements held. Recommendation calculation now reads a single transactional snapshot, including blocks and active-account state.
- **An incomplete candidate broke the entire list:** an admin-created student with enrollment/availability but no profile caused the endpoint to return HTTP 404 for the requesting student, hiding valid partners as well. Incomplete candidates are now excluded until their profiles exist.

Targeted tests confirmed that hours on different days or adjacent hours do not overlap; multiple shared courses neither duplicate partners nor multiply their hours; matching still requires a shared course; reverse blocks and disabled accounts are excluded; and ranking uses hours first, then number of shared courses. These rules were retained rather than silently changing the product's weighting.

The full suite now passes **45 tests**, including the new concurrent matching regression and six targeted algorithm tests. Weekly compatibility remains distinct from a date-specific free/busy calendar: confirmed meetings are checked for conflicts when accepted, rather than subtracted permanently from the weekly pattern.
