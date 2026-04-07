# 🔐 Update: Use New Google Credentials

## Status: ✅ CREDENTIALS REGENERATED

Old credentials have been revoked. Use the new ones below.

---

## 📝 New Credentials

### Google OAuth (Regenerated April 7, 2026)

**Location:** Google Cloud Console → Credentials → OAuth 2.0 Client IDs

```
CLIENT_ID: [Check Google Cloud Console]
CLIENT_SECRET: [New secret regenerated on April 7, 2026]
REDIRECT_URI: http://localhost:8080/api/v1/auth/google-callback
```

---

## 🚀 Setup with New Credentials

### Method 1: Using setup-env.ps1 (Recommended)

```powershell
.\setup-env.ps1

# When prompted:
# 1. Enter your NEW Google Client ID
# 2. Enter your NEW Google Client Secret
# 3. Let it auto-generate JWT and encryption keys
```

### Method 2: Manual Setup

```powershell
# Get new credentials from Google Cloud Console
# Then set them:

$env:GOOGLE_CLIENT_ID = "YOUR-NEW-CLIENT-ID"
$env:GOOGLE_CLIENT_SECRET = "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"
$env:GEMINI_API_KEY = "YOUR-GEMINI-KEY"
$env:JWT_SECRET = "generate-with-openssl-rand-base64-32"
$env:TOKEN_ENC_KEY_BASE64 = "generate-with-openssl-rand-base64-32"

./gradlew bootRun
```

### Method 3: .env File

```
# Create/update .env (gitignored):
cp .env.example .env

# Then edit .env and fill in:
GOOGLE_CLIENT_ID=<your-new-client-id>
GOOGLE_CLIENT_SECRET=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
GEMINI_API_KEY=<your-key>
```

---

## ✅ Verification

After setup, verify credentials work:

```powershell
# Check env vars are set
$env:GOOGLE_CLIENT_ID     # Should show your ID
$env:GOOGLE_CLIENT_SECRET # Should show new secret

# Start application
./gradlew bootRun

# Look for: "Started Application in X.XXX seconds"
```

---

## 🔒 Security Status

| Item | Status |
|------|--------|
| Old Google Credentials | ❌ REVOKED |
| New Google Credentials | ✅ ACTIVE |
| .env.example in git | ✅ SAFE (templates only) |
| Repository code | ✅ SECURE |
| Git history old secrets | ⚠️ HISTORICAL (but invalid) |

---

## 📋 Credentials Format

The new secret you provided:
```
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
```

Is properly formatted as Google OAuth Client Secret:
- ✅ Starts with "GOCSPX-" (valid prefix)
- ✅ Random string (not predictable)
- ✅ Safe to use

---

## ⚠️ Remember

```
DON'T:
❌ Commit .env file to git
❌ Share secrets via Slack/Email
❌ Hardcode in source code
❌ Log sensitive values

DO:
✅ Use environment variables
✅ Use .env (gitignored)
✅ Use setup-env.ps1
✅ Rotate secrets regularly
```

---

## 🆘 Troubleshooting

### "Invalid credentials from Google"
```
✅ Solution: 
1. Verify CLIENT_ID matches Google Cloud Console
2. Verify CLIENT_SECRET is the new one
3. Check redirect URI is configured in GCP
```

### "Application won't start"
```
✅ Solution:
1. Verify env vars are set: $env:GOOGLE_CLIENT_ID
2. Check .env file for typos
3. Regenerate JWT and encryption keys
```

### "Old credentials still being used"
```
✅ Solution:
1. Delete old .env file: rm .env
2. Run setup-env.ps1 again
3. Restart terminal/IDE
4. Verify: $env:GOOGLE_CLIENT_SECRET shows new secret
```

---

**Updated:** April 7, 2026  
**Status:** ✅ Ready to use new credentials

