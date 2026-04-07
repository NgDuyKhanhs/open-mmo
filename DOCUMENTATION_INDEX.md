# 📚 DOCUMENTATION INDEX

## 🎯 Quick Navigation

### ⚡ I want to get started in 5 minutes
👉 **Read:** [`QUICKSTART.md`](./QUICKSTART.md)

### 📖 I want a complete setup guide
👉 **Read:** [`SETUP_ENV.md`](./SETUP_ENV.md)

### 🔍 I want to understand how it works
👉 **Read:** [`HOW_ENVIRONMENT_VARIABLES_WORK.md`](./HOW_ENVIRONMENT_VARIABLES_WORK.md)

### 🤖 I want to automate setup
👉 **Run:** `.\setup-env.ps1`

### ✅ I want to verify everything is working
👉 **Follow:** [`POST_CLEANUP_CHECKLIST.md`](./POST_CLEANUP_CHECKLIST.md)

---

## 📋 Complete Documentation Map

### Cleanup & Configuration

| Document | Purpose | Read Time | For Whom |
|----------|---------|-----------|----------|
| **QUICKSTART.md** | 5-minute quick start guide | 5 min | Everyone |
| **SETUP_ENV.md** | Complete step-by-step setup with Vietnamese instructions | 20 min | Developers |
| **HOW_ENVIRONMENT_VARIABLES_WORK.md** | Technical deep-dive with diagrams and examples | 30 min | Senior devs, DevOps |
| **ENVIRONMENT_VARIABLES_CLEANUP.md** | Detailed cleanup summary and explanation | 15 min | Project leads |
| **POST_CLEANUP_CHECKLIST.md** | Pre-startup verification and troubleshooting | 10 min | Before running app |
| **CLEANUP_CHANGES_DETAILED.md** | File-by-file changes and statistics | 10 min | Code reviewers |

---

## 📁 Configuration Files

### Templates
- **`.env.example`** - Template of all environment variables (copy to `.env` and fill in)

### Configuration
- **`src/main/resources/application.yaml`** - Production configuration (uses env variables)
- **`src/main/resources/application-local.yaml`** - Local development configuration (gitignored)

### Gitignore
- **`.gitignore`** - Enhanced to prevent secret files from being committed

---

## 🤖 Automation Scripts

### PowerShell Setup Script
- **`setup-env.ps1`** - Automates environment variable setup for Windows
  ```powershell
  .\setup-env.ps1
  ```
  **Features:**
  - Interactive prompts
  - Auto-generates secure keys
  - Sets Windows environment variables
  - Validation and verification

---

## 🔐 Environment Variables Reference

### Required for Production
```
❌ Must be provided:
  • GOOGLE_CLIENT_ID (from Google Cloud Console)
  • GOOGLE_CLIENT_SECRET (from Google Cloud Console)
  • JWT_SECRET (256+ bits, base64 encoded)
  • TOKEN_ENC_KEY_BASE64 (32 bytes, base64 encoded)
  • GEMINI_API_KEY (from https://ai.google.dev/)
```

### Required for Development
```
❌ Must be provided:
  • GOOGLE_CLIENT_ID
  • GOOGLE_CLIENT_SECRET
  • GEMINI_API_KEY

✅ Optional (have defaults):
  • JWT_SECRET
  • TOKEN_ENC_KEY_BASE64
```

### Optional for Both
```
✅ Can be skipped:
  • MONGODB_URI (default: mongodb://localhost:27017/openmmo)
  • CORS_ORIGINS
  • APP_WEB_URL
  • OPENAI_API_KEY
```

---

## 🚀 Getting Started - 3 Steps

### Step 1: Setup Variables
```powershell
# Option A: Automated
.\setup-env.ps1

# Option B: Manual
$env:GOOGLE_CLIENT_ID = "your-id"
$env:GOOGLE_CLIENT_SECRET = "your-secret"
$env:JWT_SECRET = "your-secret"
$env:TOKEN_ENC_KEY_BASE64 = "your-key"
$env:GEMINI_API_KEY = "your-key"
```

### Step 2: Verify
```powershell
$env:GOOGLE_CLIENT_ID    # Should show your ID
$env:GEMINI_API_KEY      # Should show your key
```

### Step 3: Run
```powershell
./gradlew bootRun
```

---

## 📊 Files Changed

### Modified: 4 files
```
✅ src/main/resources/application.yaml
   └─ Removed: 2 hardcoded secrets

✅ src/main/resources/application-local.yaml
   └─ Fixed: JWT secret default value

✅ .env.example
   └─ Added: Gmail OAuth, Gemini, encryption variables

✅ .gitignore
   └─ Added: 8 additional secret file patterns
```

### Created: 10 files
```
✅ setup-env.ps1 (PowerShell script)
✅ QUICKSTART.md (5-minute guide)
✅ SETUP_ENV.md (Complete Vietnamese guide)
✅ HOW_ENVIRONMENT_VARIABLES_WORK.md (Technical guide)
✅ ENVIRONMENT_VARIABLES_CLEANUP.md (Cleanup summary)
✅ POST_CLEANUP_CHECKLIST.md (Verification checklist)
✅ CLEANUP_CHANGES_DETAILED.md (File-by-file changes)
✅ DOCUMENTATION_INDEX.md (This file)
```

---

## 🔍 Troubleshooting Quick Links

### Problem: "Could not resolve placeholder 'JWT_SECRET'"
👉 **Solution:** Set environment variables - see [`QUICKSTART.md`](./QUICKSTART.md#-troubleshooting)

### Problem: "Invalid Google credentials"
👉 **Solution:** Check Google Cloud Console - see [`SETUP_ENV.md`](./SETUP_ENV.md#-troubleshooting)

### Problem: "Token Encryption Key invalid"
👉 **Solution:** Generate new 32-byte key - see [`HOW_ENVIRONMENT_VARIABLES_WORK.md`](./HOW_ENVIRONMENT_VARIABLES_WORK.md)

### Problem: "Variables still not working after setup"
👉 **Solution:** Try different setup methods - see [`SETUP_ENV.md`](./SETUP_ENV.md#-cách-2-intellij-idea-run-configuration)

---

## 🎓 Learning Paths

### For First-Time Setup
1. Read [`QUICKSTART.md`](./QUICKSTART.md) (5 min)
2. Run `.\setup-env.ps1` (3 min)
3. Verify `$env:GOOGLE_CLIENT_ID` (1 min)
4. Start app `./gradlew bootRun` (Wait for startup)

### For Understanding the System
1. Read [`HOW_ENVIRONMENT_VARIABLES_WORK.md`](./HOW_ENVIRONMENT_VARIABLES_WORK.md) (30 min)
2. Review [`application.yaml`](./src/main/resources/application.yaml) (5 min)
3. Review [`.env.example`](./.env.example) (3 min)
4. Compare with [`application-local.yaml`](./src/main/resources/application-local.yaml) (3 min)

### For Setup Automation
1. Read [`setup-env.ps1`](./setup-env.ps1) (10 min)
2. Run it: `.\setup-env.ps1` (5 min)
3. Verify results (2 min)

### For Troubleshooting
1. Check [`POST_CLEANUP_CHECKLIST.md`](./POST_CLEANUP_CHECKLIST.md) (5 min)
2. Find error in troubleshooting sections (5 min)
3. Apply solution (varies)

---

## ✅ Success Indicators

After successful setup, you should see:

```powershell
# Environment variables are set
$env:GOOGLE_CLIENT_ID     # Shows: "779295..."
$env:GEMINI_API_KEY       # Shows: "AIzaSy..."
$env:JWT_SECRET           # Shows: "dGhpc..."

# Application starts without error
./gradlew bootRun         # Shows: "Started Application in X.XXX seconds"

# No error messages containing:
"Could not resolve placeholder"
"Invalid credentials"
"Authentication failed"
```

---

## 🔐 Security Verification

Before committing code, verify:

```powershell
# 1. No secrets in config files
gc src/main/resources/application.yaml | grep -i "secret\|key"
# Result: Should show placeholders like ${JWT_SECRET:} with NO VALUES

# 2. .env is in gitignore
cat .gitignore | grep "\.env"
# Result: Should show ".env"

# 3. No secrets in git history
git log --all --oneline | grep -i "secret\|key"
# Result: Should show NOTHING

# 4. Commit is safe
git status
# Result: Should NOT show .env or application-local.yaml
```

---

## 📞 Need Help?

| Question | Answer Location |
|----------|-----------------|
| "How do I get started?" | [`QUICKSTART.md`](./QUICKSTART.md) |
| "Step-by-step guide?" | [`SETUP_ENV.md`](./SETUP_ENV.md) |
| "How does it work?" | [`HOW_ENVIRONMENT_VARIABLES_WORK.md`](./HOW_ENVIRONMENT_VARIABLES_WORK.md) |
| "What variables do I need?" | [`.env.example`](./.env.example) |
| "How do I verify?" | [`POST_CLEANUP_CHECKLIST.md`](./POST_CLEANUP_CHECKLIST.md) |
| "What changed?" | [`CLEANUP_CHANGES_DETAILED.md`](./CLEANUP_CHANGES_DETAILED.md) |
| "I have an error" | [`QUICKSTART.md` Troubleshooting](./QUICKSTART.md#-troubleshooting) |

---

## 📊 Quick Reference

### File Locations
```
Configuration:
  └─ src/main/resources/
     ├─ application.yaml (production)
     └─ application-local.yaml (dev, gitignored)

Setup:
  ├─ setup-env.ps1 (automation)
  └─ .env.example (template)

Documentation:
  ├─ QUICKSTART.md
  ├─ SETUP_ENV.md
  ├─ HOW_ENVIRONMENT_VARIABLES_WORK.md
  ├─ POST_CLEANUP_CHECKLIST.md
  └─ others...
```

### Environment Variables Resolution Order
```
1. System environment variables (highest priority)
2. .env file (if exists)
3. Default value in YAML after : (if specified)
4. Error (if nothing found)
```

### Setup Methods
```
A. Automated: .\setup-env.ps1
B. Manual PowerShell: $env:VAR = "value"
C. Manual CMD: set VAR=value
D. IDE: Run → Edit Configurations → Environment variables
E. File: Create .env and fill in values
```

---

## 🎯 Main Takeaway

```
Before:  Config file → hardcoded secret → git → exposed ❌
After:   Environment variable → Spring Boot → memory only ✅

Key Benefits:
✅ Secrets never in code
✅ Safe to commit to git
✅ Different values per environment
✅ Easy secret rotation
✅ Production ready
```

---

## 📝 Checklist Before Deploying

- [ ] Read [`QUICKSTART.md`](./QUICKSTART.md) or [`SETUP_ENV.md`](./SETUP_ENV.md)
- [ ] Run `.\setup-env.ps1` or set variables manually
- [ ] Verify: `$env:GOOGLE_CLIENT_ID` shows value
- [ ] No hardcoded secrets in `application.yaml`
- [ ] `.env` and `application-local.yaml` in `.gitignore`
- [ ] Application starts: `./gradlew bootRun`
- [ ] No "Could not resolve placeholder" errors
- [ ] Code committed and ready to push

---

## 🚀 You're Ready!

All setup is complete. Your project now has:
- ✅ Secure configuration management
- ✅ Environment variable system
- ✅ Complete documentation
- ✅ Automation scripts
- ✅ Troubleshooting guides

**Next Step:** Follow [`QUICKSTART.md`](./QUICKSTART.md) to get running in 5 minutes! 🎉

---

**Last Updated:** April 7, 2026
**Status:** ✅ All documentation complete and ready
**Audience:** Developers, DevOps, Project leads

