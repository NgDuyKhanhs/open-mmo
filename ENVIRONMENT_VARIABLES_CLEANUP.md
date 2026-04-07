# ✅ ENVIRONMENT VARIABLES CLEANUP COMPLETE

## 📋 Summary

Tất cả secrets đã được xóa khỏi config files. Đây là cách hoạt động:

### 🔒 Cách Spring Boot Load Environment Variables

```yaml
# application.yaml (Production - NO DEFAULT VALUES)
jwt:
  secret: ${JWT_SECRET:}      # ❌ Không có giá trị mặc định
  
token:
  enc:
    key-base64: ${TOKEN_ENC_KEY_BASE64:}  # ❌ Không có giá trị mặc định
```

**Cú pháp:** `${VARIABLE_NAME:DEFAULT_VALUE}`
- `${JWT_SECRET:}` = Lấy từ `JWT_SECRET` env var, không có default
- `${JWT_SECRET:my-secret}` = Lấy từ env var, nếu không có dùng `my-secret`

### ✅ Files Đã Cleanup

1. **`application.yaml`** (Production)
   - ❌ Xóa hardcoded JWT secret
   - ❌ Xóa hardcoded TOKEN_ENC_KEY_BASE64
   - ✅ Giữ environment variable references

2. **`application-local.yaml`** (Development)
   - ✅ Giữ test values cho local dev (không bị commit vì đã ignore)

3. **`.env.example`** (Template)
   - ✅ Updated với tất cả Gmail OAuth variables
   - ✅ Clear instructions

4. **`.gitignore`**
   - ✅ Added: `application-local.yaml`, `application.properties*`, `*.jks`, etc
   - ✅ Ensures secrets never committed

## 🎯 Environment Variables Required

### Production (application.yaml)
```
❌ REQUIRED (no default):
   - JWT_SECRET
   - TOKEN_ENC_KEY_BASE64
   - GOOGLE_CLIENT_ID
   - GOOGLE_CLIENT_SECRET
   - GEMINI_API_KEY

✅ OPTIONAL (có default):
   - MONGODB_URI (default: mongodb://localhost:27017/openmmo)
   - GOOGLE_REDIRECT_URI (default: http://localhost:8081/api/v1/auth/google-callback)
   - GMAIL_OAUTH_REDIRECT_URI (default: http://localhost:8080/api/v1/gmail/connect/callback)
   - CORS_ORIGINS (default: *)
   - APP_WEB_URL (default: http://localhost:5173)
```

### Development (application-local.yaml)
```
❌ REQUIRED:
   - GOOGLE_CLIENT_ID
   - GOOGLE_CLIENT_SECRET
   - GEMINI_API_KEY

✅ OPTIONAL (có fallback values):
   - JWT_SECRET (default: dGhpcyBpcyBhIGxvbmcgand0IHNlY3JldCBrZXkgZm9yIExPQ0FMSURWIGFOR1ZUVE9QTUVOVCBD)
   - TOKEN_ENC_KEY_BASE64 (default: dGVzdC1rZXktZm9yLWxvY2FsLWRldmVsb3BtZW50LW9ubHktbm90LXNlY3VyZQ==)
```

## 🛠️ Cách Set Environment Variables

### Option 1: PowerShell (Persistent)
```powershell
# Run setup script
.\setup-env.ps1

# Or manually
$env:GOOGLE_CLIENT_ID = "your-client-id"
$env:JWT_SECRET = "your-jwt-secret"
# etc...
```

### Option 2: IntelliJ IDEA Run Configuration
```
Run → Edit Configurations
→ Select bootRun
→ Environment variables tab
→ Add: GOOGLE_CLIENT_ID=..., JWT_SECRET=..., etc
```

### Option 3: Direct Command
```powershell
$env:JWT_SECRET="..."; $env:TOKEN_ENC_KEY_BASE64="..."; ./gradlew bootRun
```

## ✨ New Files Created

1. **`SETUP_ENV.md`** - Complete setup guide (Vietnamese)
2. **`.env.example`** - Updated with all variables
3. **`setup-env.ps1`** - PowerShell helper script
4. **`ENVIRONMENT_VARIABLES_CLEANUP.md`** - This file

## 🔐 Security Benefits

✅ Secrets không trong repository
✅ Secrets không dalam config files
✅ Secrets chỉ trong memory/environment
✅ Different credentials cho dev/prod
✅ Easy rotation (chỉ cần update env var)
✅ Safe để commit code (no credentials leaked)

## 📝 Checklist Before Running

- [ ] MongoDB chạy ở `localhost:27017`
- [ ] `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` set
- [ ] `JWT_SECRET` set (minimum 256 bits)
- [ ] `TOKEN_ENC_KEY_BASE64` set (32-byte base64)
- [ ] `GEMINI_API_KEY` set
- [ ] Run `.env.example` để verify all variables

## 🚀 Next: Run Application

```powershell
# Setup env vars (if not done)
.\setup-env.ps1

# Start MongoDB
mongod

# Run backend
./gradlew bootRun

# In another terminal, run frontend
cd frontend
npm run dev
```

## ❓ FAQs

**Q: Nếu quên set environment variable?**
A: App sẽ throw error hoặc request sẽ fail. Check logs.

**Q: Làm sao verify env vars đã set?**
A: `$env:GOOGLE_CLIENT_ID` trong PowerShell

**Q: .env file là gì?**
A: Convention file, Spring Boot auto-load nếu tồn tại. Nhưng .env đã ignore, không commit.

**Q: Khác biệt application.yaml vs application-local.yaml?**
A: 
- `application.yaml` = production, all env vars required
- `application-local.yaml` = local dev, có fallback values, auto-ignored

---

**Status: ✅ COMPLETE**

All secrets removed from config files. Environment variables system ready!

