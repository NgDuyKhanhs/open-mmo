# 🚀 OpenMMO AI Backend

Make Money Online Platform with Gmail AI Bot Integration

## ⚡ Quick Start (5 minutes)

### 1. Setup Environment
```powershell
.\setup-env.ps1
```

### 2. Verify
```powershell
$env:GOOGLE_CLIENT_ID      # Should show value
$env:GOOGLE_CLIENT_SECRET  # Should show new secret
```

### 3. Run
```powershell
./gradlew bootRun
```

Expected: `Started Application in X.XXX seconds`

---

## 📋 Required Environment Variables

```
GOOGLE_CLIENT_ID           from Google Cloud Console
GOOGLE_CLIENT_SECRET       Regenerated April 7, 2026
GEMINI_API_KEY            from ai.google.dev
JWT_SECRET                Generate: openssl rand -base64 32
TOKEN_ENC_KEY_BASE64      Generate: openssl rand -base64 32
```

---

## 🔒 Security

✅ All secrets managed via environment variables
✅ No secrets in code
✅ `.env.local` is gitignored
✅ `.env.example` contains only templates
✅ Safe to commit and deploy

---

## 🛠️ Setup Methods

**PowerShell Script (Recommended):**
```powershell
.\setup-env.ps1
```

**Manual PowerShell:**
```powershell
$env:GOOGLE_CLIENT_ID = "your-id"
$env:GOOGLE_CLIENT_SECRET = "your-secret"
$env:GEMINI_API_KEY = "your-key"
./gradlew bootRun
```

**IntelliJ IDEA:**
```
Run → Edit Configurations → Environment variables tab → Add variables
```

---

## 📂 Project Structure

```
backend/
  ├─ src/main/kotlin/          # Backend code
  ├─ src/main/resources/       # Config & resources
  ├─ build.gradle.kts          # Dependencies
  └─ .env.local                # Local secrets (gitignored)

frontend/
  ├─ src/                       # Vue 3 components
  ├─ package.json             # Dependencies
  └─ vite.config.ts           # Build config

.env.example                  # Template (safe to commit)
setup-env.ps1               # Setup automation
```

---

## 🔧 Configuration Files

| File | Purpose | Visibility |
|------|---------|-----------|
| `application.yaml` | Production config | ✅ Public (no secrets) |
| `application-local.yaml` | Local dev config | ❌ Gitignored |
| `.env.example` | Template | ✅ Public (no secrets) |
| `.env.local` | Local secrets | ❌ Gitignored |

---

## 🎯 Key Features

✅ Gmail OAuth Integration (login + email automation)
✅ AI Auto-Reply via Gemini API  
✅ Email Processing & Tracking
✅ Custom Prompt Templates
✅ Bot Enable/Disable Toggle
✅ Secure Token Encryption (AES-256)
✅ MongoDB for Persistence
✅ Vue 3 Frontend with TypeScript

---

## 📞 Environment Setup Verification

```powershell
# Check if variables are set
Write-Host "CLIENT_ID: $($env:GOOGLE_CLIENT_ID)"
Write-Host "CLIENT_SECRET: $($env:GOOGLE_CLIENT_SECRET)"
Write-Host "GEMINI_KEY: $(if ($env:GEMINI_API_KEY) { '✅' } else { '❌' })"
```

---

## ⚠️ Important Notes

- Never commit `.env` file
- Never share credentials via Slack/Email
- Always use environment variables
- Regenerate credentials if exposed
- Check `.gitignore` before committing

---

**Created:** April 7, 2026  
**Status:** Production Ready ✅

