# Validation

After the algorithm follow-up review, the simplified SQLite build passed all **45 tests**: 38 account/API/matching tests, 3 Chromium workflows, and 4 concurrency tests. Browser workflows include mobile onboarding and two independent student accounts. Tests create and destroy an isolated file-backed SQLite test database. See REVIEW.md for the reproduced concurrency and matching failures and fixes. Database migrations and demo seeding also completed successfully with SQLite.

Coverage includes account isolation, CSRF, failed login handling, course ownership and deletion, current shared courses and availability, unique overlapping hours, stable private conversations, preserved chat after course removal, blocking, reporting, unread state, profile persistence, meeting permissions/conflicts, and study groups.

SQLite is built into Python. Runtime setup no longer installs or invokes Docker, WSL, PostgreSQL, or browser-testing packages. Browser tools are optional development dependencies.

The application and startup operations were validated on Linux using Python 3.12 and SQLite. The PowerShell launcher has not been executed on Windows in this workspace. The original Java project is preserved on `backup/original-before-v2`. The earlier PostgreSQL build also passed 29 tests before the default was simplified.
