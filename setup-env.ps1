# ============================================================
# Setup Environment Variables for OpenMMO AI Backend
# ============================================================
# This script helps you set up environment variables locally
# Usage: .\setup-env.ps1
# ============================================================

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "OpenMMO AI Backend - Environment Setup" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Function to validate base64
function Test-Base64String {
    param([string]$String)
    try {
        [System.Convert]::FromBase64String($String) | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

# Check if running in admin mode (recommended)
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")
if (-not $isAdmin) {
    Write-Host "⚠️  Warning: This script works better when run as Administrator" -ForegroundColor Yellow
    Write-Host "You can still continue, but some changes may not persist after restart" -ForegroundColor Yellow
    Write-Host ""
}

# Step 1: MongoDB URI
Write-Host "[1/6] MongoDB Configuration" -ForegroundColor Cyan
$mongoUri = Read-Host "Enter MongoDB URI (default: mongodb://localhost:27017/openmmo)"
if ([string]::IsNullOrWhiteSpace($mongoUri)) {
    $mongoUri = "mongodb://localhost:27017/openmmo"
}
[Environment]::SetEnvironmentVariable("MONGODB_URI", $mongoUri, "User")
Write-Host "✅ MONGODB_URI set" -ForegroundColor Green
Write-Host ""

# Step 2: Google OAuth Credentials
Write-Host "[2/6] Google OAuth Configuration" -ForegroundColor Cyan
$clientId = Read-Host "Enter Google Client ID (from Google Cloud Console)"
if ([string]::IsNullOrWhiteSpace($clientId)) {
    Write-Host "❌ Client ID is required" -ForegroundColor Red
    exit 1
}
[Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_ID", $clientId, "User")
Write-Host "✅ GOOGLE_CLIENT_ID set" -ForegroundColor Green

$clientSecret = Read-Host "Enter Google Client Secret (from Google Cloud Console)" -AsSecureString
$clientSecretPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToCoTaskMemUnicode($clientSecret))
[Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_SECRET", $clientSecretPlain, "User")
Write-Host "✅ GOOGLE_CLIENT_SECRET set" -ForegroundColor Green
Write-Host ""

# Step 3: JWT Secret
Write-Host "[3/6] JWT Secret Configuration" -ForegroundColor Cyan
Write-Host "Enter JWT Secret (minimum 256 bits, base64 encoded)" -ForegroundColor Yellow
Write-Host "Or press Enter to generate a new one automatically" -ForegroundColor Gray
$jwtSecret = Read-Host "JWT Secret"
if ([string]::IsNullOrWhiteSpace($jwtSecret)) {
    Write-Host "Generating new JWT Secret..." -ForegroundColor Yellow
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($bytes)
    $jwtSecret = [System.Convert]::ToBase64String($bytes)
    Write-Host "Generated: $jwtSecret" -ForegroundColor Green
}
[Environment]::SetEnvironmentVariable("JWT_SECRET", $jwtSecret, "User")
Write-Host "✅ JWT_SECRET set" -ForegroundColor Green
Write-Host ""

# Step 4: Token Encryption Key
Write-Host "[4/6] Token Encryption Key (AES-256)" -ForegroundColor Cyan
$tokenKey = Read-Host "Enter Token Encryption Key (32-byte base64, or press Enter to generate)"
if ([string]::IsNullOrWhiteSpace($tokenKey)) {
    Write-Host "Generating new Token Encryption Key..." -ForegroundColor Yellow
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RNGCryptoServiceProvider]::new().GetBytes($bytes)
    $tokenKey = [System.Convert]::ToBase64String($bytes)
    Write-Host "Generated: $tokenKey" -ForegroundColor Green
} else {
    if (-not (Test-Base64String $tokenKey)) {
        Write-Host "❌ Invalid base64 format" -ForegroundColor Red
        exit 1
    }
}
[Environment]::SetEnvironmentVariable("TOKEN_ENC_KEY_BASE64", $tokenKey, "User")
Write-Host "✅ TOKEN_ENC_KEY_BASE64 set" -ForegroundColor Green
Write-Host ""

# Step 5: Gemini API Key
Write-Host "[5/6] Gemini API Configuration" -ForegroundColor Cyan
Write-Host "Get your API key from: https://ai.google.dev/" -ForegroundColor Gray
$geminiKey = Read-Host "Enter Gemini API Key"
if ([string]::IsNullOrWhiteSpace($geminiKey)) {
    Write-Host "⚠️  Gemini API Key not set - Gmail auto-reply won't work" -ForegroundColor Yellow
} else {
    [Environment]::SetEnvironmentVariable("GEMINI_API_KEY", $geminiKey, "User")
    Write-Host "✅ GEMINI_API_KEY set" -ForegroundColor Green
}
Write-Host ""

# Step 6: Optional OpenAI Key
Write-Host "[6/6] Optional Configuration" -ForegroundColor Cyan
$openaiKey = Read-Host "Enter OpenAI API Key (optional, press Enter to skip)"
if (-not [string]::IsNullOrWhiteSpace($openaiKey)) {
    [Environment]::SetEnvironmentVariable("OPENAI_API_KEY", $openaiKey, "User")
    Write-Host "✅ OPENAI_API_KEY set" -ForegroundColor Green
}
Write-Host ""

# Summary
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "✅ Environment Setup Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Environment Variables Set:" -ForegroundColor Yellow
Write-Host "  ✓ MONGODB_URI" -ForegroundColor Green
Write-Host "  ✓ GOOGLE_CLIENT_ID" -ForegroundColor Green
Write-Host "  ✓ GOOGLE_CLIENT_SECRET" -ForegroundColor Green
Write-Host "  ✓ JWT_SECRET" -ForegroundColor Green
Write-Host "  ✓ TOKEN_ENC_KEY_BASE64" -ForegroundColor Green
Write-Host "  ✓ GEMINI_API_KEY" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  Note: You may need to restart your terminal/IDE for changes to take effect" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Verify environment variables:" -ForegroundColor Gray
Write-Host "   \$env:GOOGLE_CLIENT_ID" -ForegroundColor DarkGray
Write-Host "2. Start MongoDB:" -ForegroundColor Gray
Write-Host "   mongod" -ForegroundColor DarkGray
Write-Host "3. Run the backend:" -ForegroundColor Gray
Write-Host "   ./gradlew bootRun" -ForegroundColor DarkGray
Write-Host ""

