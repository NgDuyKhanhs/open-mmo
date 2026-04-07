# 🔧 JWT_SECRET Error - FIX GUIDE

## ❌ ERROR

```
io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 0 bits 
which is not secure enough for any JWT HMAC-SHA algorithm.
```

## 🔍 ROOT CAUSE

JWT_SECRET environment variable was **not set** or **empty**!

### Why it happened:
1. Application required `JWT_SECRET` from environment
2. `.env.local` had invalid/malformed JWT_SECRET
3. Application couldn't load environment variables
4. Default to empty string → 0 bits error

---

## ✅ FIX APPLIED

### 1. Fixed `.env.local` format
```
❌ BEFORE: (broken comment line)
# ============================================# Google OAuth2...

✅ AFTER: (proper format)
# ==============================================
# Google OAuth2 Configuration...
```

### 2. Generated new secure JWT_SECRET
```
✅ OLD: dGhpcyBpcyBhIGxvbmcgand0... (invalid base64)
✅ NEW: +F7KKKvYfJrUc4+qsRctUH+bQUpxpbP1A6yiQB7Cn8E= (valid 256-bit)
```

### 3. Updated `.env.local`
```env
JWT_SECRET=+F7KKKvYfJrUc4+qsRctUH+bQUpxpbP1A6yiQB7Cn8E=
```

---

## 🚀 HOW TO RUN NOW

### Option 1: Using new script (RECOMMENDED)
```powershell
.\run-with-env.ps1
```

This script:
- ✅ Loads all env vars from `.env.local`
- ✅ Verifies JWT_SECRET is set
- ✅ Starts application

### Option 2: Manual load + run
```powershell
# Load environment
$env:JWT_SECRET = "+F7KKKvYfJrUc4+qsRctUH+bQUpxpbP1A6yiQB7Cn8E="
$env:GOOGLE_CLIENT_ID = "YOUR-ID"
$env:GOOGLE_CLIENT_SECRET = "YOUR-SECRET"
# ... other vars from .env.local

# Run
./gradlew bootRun
```

---

## ✅ VERIFICATION

After fix, you should see:
```
✅ JWT_SECRET set (44 characters)
✅ GOOGLE_CLIENT_ID set
✅ GOOGLE_CLIENT_SECRET set (***) 
✅ GEMINI_API_KEY set
Started Application in X.XXX seconds
```

---

## 📋 JWT_SECRET REQUIREMENTS

✅ **MUST be:**
- Base64 encoded
- At least 256 bits (32 bytes) when decoded
- Random/cryptographically secure
- Stored in `.env.local` (never in code)

❌ **CANNOT be:**
- Empty
- Too short (< 32 bytes)
- Plain text (must be base64)
- Hardcoded in application.yaml

---

## 🔑 To generate NEW JWT_SECRET (if needed):

### PowerShell:
```powershell
$bytes = [byte[]]::new(32)
$rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
Write-Host $secret
```

### Bash/Linux:
```bash
openssl rand -base64 32
```

---

## 📝 FILES MODIFIED

- ✅ `.env.local` - Fixed format + new JWT_SECRET
- ✅ `run-with-env.ps1` - New convenient startup script

---

## 🎯 SUMMARY

| Before | After |
|--------|-------|
| ❌ JWT_SECRET = 0 bits | ✅ JWT_SECRET = 256 bits |
| ❌ Error on startup | ✅ Starts successfully |
| ❌ Manual env setup | ✅ Automated with script |

---

## ✨ NEXT STEPS

1. **Run application:**
   ```powershell
   .\run-with-env.ps1
   ```

2. **Should see:**
   ```
   ✅ JWT_SECRET set (44 characters)
   ✅ GOOGLE_CLIENT_ID set
   Started Application in X.XXX seconds
   ```

3. **Done!** Application is running ✅

---

**Problem Solved!** 🎉

