# Requires Python 3.12 only. SQLite is included with Python.
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed (exit $LASTEXITCODE). See the output above." }
}
if (-not (Get-Command py -ErrorAction SilentlyContinue)) {
    throw 'Install Python 3.12, then reopen VS Code.'
}
Invoke-Checked -Program 'py' -Arguments @('-3.12', '--version')
if (-not (Test-Path '.venv\Scripts\python.exe')) {
    Invoke-Checked -Program 'py' -Arguments @('-3.12', '-m', 'venv', '.venv')
}
$Python = Join-Path (Get-Location) '.venv\Scripts\python.exe'
Invoke-Checked -Program $Python -Arguments @('-m', 'pip', 'install', '-r', 'requirements.txt')
Invoke-Checked -Program $Python -Arguments @('manage.py', 'migrate', '--noinput')
Invoke-Checked -Program $Python -Arguments @('manage.py', 'seed_demo')
Invoke-Checked -Program $Python -Arguments @('manage.py', 'check')
Write-Host 'Open http://127.0.0.1:8000 and click Explore with the demo account.' -ForegroundColor Cyan
Write-Host 'Keep this window open. Press Ctrl+C to stop.'
Invoke-Checked -Program $Python -Arguments @('manage.py', 'runserver', '127.0.0.1:8000', '--noreload')
