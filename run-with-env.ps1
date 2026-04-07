#!/usr/bin/env pwsh
# Load .env.local and run application

Write-Host "Loading environment from .env.local..." -ForegroundColor Cyan

# Load all variables from .env.local
$envContent = Get-Content .env.local
$envContent | Where-Object { $_ -match "^[A-Z_]+=.+" -and $_ -notmatch "^#" } | ForEach-Object {
    $line = $_
    $key = $line.Split('=')[0]
    $value = $line.Split('=',2)[1]
    if ($value -and $value -ne "") {
        [Environment]::SetEnvironmentVariable($key, $value, "Process")
        if ($key -like "*SECRET*" -or $key -like "*KEY*") {
            Write-Host "✅ $key set (***)" -ForegroundColor Green
        } else {
            Write-Host "✅ $key set" -ForegroundColor Green
        }
    }
}

# Verify JWT_SECRET
Write-Host "`n" -ForegroundColor Cyan
Write-Host "Verifying JWT_SECRET..." -ForegroundColor Yellow
if ($env:JWT_SECRET -and $env:JWT_SECRET -ne "") {
    Write-Host "✅ JWT_SECRET is set: $($env:JWT_SECRET.Length) characters" -ForegroundColor Green
} else {
    Write-Host "❌ JWT_SECRET is EMPTY!" -ForegroundColor Red
    exit 1
}

# Verify other critical variables
Write-Host "`nVerifying critical variables..." -ForegroundColor Cyan
$critical = @("GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "GEMINI_API_KEY", "TOKEN_ENC_KEY_BASE64")
foreach ($var in $critical) {
    if (Test-Path env:$var) {
        $value = Get-Item env:$var | Select-Object -ExpandProperty Value
        if ($value -and $value -ne "") {
            Write-Host "✅ $var set" -ForegroundColor Green
        } else {
            Write-Host "⚠️  $var is empty" -ForegroundColor Yellow
        }
    } else {
        Write-Host "⚠️  $var not set" -ForegroundColor Yellow
    }
}

# Run application
Write-Host "`n" -ForegroundColor Cyan
Write-Host "Starting application..." -ForegroundColor Green
Write-Host "=" * 50 -ForegroundColor Cyan
./gradlew bootRun

