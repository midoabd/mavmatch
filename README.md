# MavMatch 2.0

A local study-partner demo. Requires **Python 3.12 only**. Use VS Code to edit and run it. SQLite is included with Python and stores data in `.local/mavmatch.sqlite3`.

## Get updates through GitHub

This rebuild now lives in the existing repository: https://github.com/midoabd/mavmatch (branch `master`). The original Java app and its history are preserved on `backup/original-before-v2`.

For your first GitHub checkout, use VS Code's **Git: Clone** command with `https://github.com/midoabd/mavmatch.git`. If your working demo came from a ZIP, copy its `.local` folder into the cloned project before starting it; this preserves your existing accounts and messages. Keep the ZIP folder until you have verified the copied data.

For later updates, stop the server with Ctrl+C, run `git pull` in the cloned project, then run the Windows launcher below. `.local` and `.venv` are ignored by Git. Keep any local source edits committed or backed up before updating; avoid reset/clean commands that discard work.

## Windows

Open the folder containing `manage.py` in VS Code. In **Terminal → New Terminal**, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows.ps1
```

Then open **http://127.0.0.1:8000** and click **Explore with the demo account**. The script creates the Python environment, installs dependencies, sets up the database file, and starts the app. Keep the terminal open. Press Ctrl+C to stop. Use the same command next time; your data is preserved.

If Python is missing, install Python 3.12 and reopen VS Code. No Docker, WSL, or PostgreSQL installation is required.

## macOS / Linux

```bash
bash scripts/setup.sh
bash scripts/dev.sh
```

## Demo

Setup creates 24 labeled demo accounts and the reference UTA course catalog. Use the demo button or `demo@mavs.uta.edu` / `MavMatchDemo!2026`. You can create independent accounts using a `@mavs.uta.edu` address. UTA ID is optional; no email is sent.

Features include courses, weekly availability, current matching, private chat, meeting invitations, groups, profiles, reporting, and blocking. Server-managed sessions scope data to the logged-in student. Recommendations use current enrollment and unique shared hours; conversations keep stable IDs and survive course changes. User text is escaped for display.

## Optional developer tests

```bash
.venv/bin/python -m pip install -r requirements-dev.txt
.venv/bin/python manage.py test core.tests --noinput
.venv/bin/python manage.py test core --noinput
```

The full suite includes real-browser tests requiring an existing Chrome/Chromium executable. Set `CHROMIUM_PATH` to its path; the default is `/usr/bin/chromium`. Browser testing tools are not needed to run the app. Create a local moderator with `manage.py createsuperuser` and use `/admin/`.

## Local data

To back up your demo, stop the app and copy `.local/mavmatch.sqlite3`. The generated `.local/secret-key` is private. Both are excluded from Git and source ZIPs. Repeating setup does not reset account changes.

This is a local funding demo, without public hosting, email verification, university SSO, or group chat. Messaging polls every five seconds and shows the latest 200 messages per conversation. The original project is preserved on the backup branch.

The application is validated on Linux. The Windows launcher has not been executed on Windows in this workspace.
