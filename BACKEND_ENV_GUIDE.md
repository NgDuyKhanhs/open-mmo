# Backend Environment Variables Configuration

## Overview
Backend sử dụng Spring Boot profiles để quản lý các environment khác nhau:
- **application.yaml** - Default configuration (fallback)
- **application-local.yaml** - Development environment
- **application-prod.yaml** - Production environment

## How to Activate Profiles

### Development (Local)
```bash
# Set environment variable
export SPRING_PROFILES_ACTIVE=local

# Or using Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Or using Gradle
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Production
```bash
export SPRING_PROFILES_ACTIVE=prod
```

## Environment Variables

### 🎯 Critical Variables (Must Set in Production)

#### API Configuration
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `APP_API_URL` | ✅ Yes | `http://localhost:8080` | Backend API base URL (used for OAuth redirects) | `https://app.openmmo.ai` |
| `APP_WEB_URL` | ✅ Yes | `http://localhost:5173` | Frontend URL (for email links, redirects) | `https://openmmo.ai` |

#### Database
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `MONGODB_URI` | ✅ Yes | `mongodb://localhost:27017/openmmo` | MongoDB connection string | `mongodb+srv://user:pass@cluster.mongodb.net/openmmo?retryWrites=true&w=majority` |
| `MONGODB_DATABASE` | ❌ No | `openmmo` | Database name | `openmmo` |

#### Authentication & Security
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `JWT_SECRET` | ✅ Yes | (empty) | 512-bit secret for JWT signing | `dk1qrTUJeUPfeJaog/lsC7P9i27rla7514968Ib0v61mEv7t15gp1/Bvo6WSjrqyfBsTednLTrMg8VvqKLCRxw==` |
| `TOKEN_ENC_KEY_BASE64` | ❌ No | (empty) | Base64 encoded encryption key for tokens | (Base64 string) |

#### Google OAuth
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `GOOGLE_CLIENT_ID` | ✅ Yes | (empty) | Google OAuth Client ID | `779295627515-log3h37u35emf3j7l64brh8rd6hm4g6t.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | ✅ Yes | (empty) | Google OAuth Client Secret | (secret value) |

#### AI Services
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `OPENAI_API_KEY` | ❌ No | (empty) | OpenAI API key | `sk-...` |
| `GEMINI_API_KEY` | ❌ No | (empty) | Google Gemini API key | (API key) |

#### CORS
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `CORS_ORIGINS` | ✅ Yes (prod) | `*` | Comma-separated list of allowed origins | `https://openmmo.ai,https://www.openmmo.ai,https://app.openmmo.ai` |

#### Server
| Variable | Required | Default | Description | Example |
|----------|----------|---------|-------------|---------|
| `SERVER_PORT` | ❌ No | `8080` | Server port | `8080` |
| `SPRING_PROFILES_ACTIVE` | ✅ Yes (prod) | (default config) | Active Spring profiles | `prod` or `local` |

---

## 📋 Configuration Examples

### Local Development
```bash
# .env.local (if using environment file)
export SPRING_PROFILES_ACTIVE=local
export APP_API_URL=http://localhost:8080
export APP_WEB_URL=http://localhost:5173
export MONGODB_URI=mongodb://localhost:27017/openmmo
export JWT_SECRET=your-512-bit-secret-here
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
export OPENAI_API_KEY=your-openai-key
export GEMINI_API_KEY=your-gemini-key
```

### Production (Docker)
```bash
# docker-compose.yml or deployment environment
SPRING_PROFILES_ACTIVE=prod
APP_API_URL=https://app.openmmo.ai
APP_WEB_URL=https://openmmo.ai
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/openmmo
JWT_SECRET=your-512-bit-secret-here
GOOGLE_CLIENT_ID=prod-client-id
GOOGLE_CLIENT_SECRET=prod-client-secret
OPENAI_API_KEY=prod-openai-key
GEMINI_API_KEY=prod-gemini-key
CORS_ORIGINS=https://openmmo.ai,https://www.openmmo.ai,https://app.openmmo.ai
```

### Production (Kubernetes)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: openmmo-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  APP_API_URL: "https://app.openmmo.ai"
  APP_WEB_URL: "https://openmmo.ai"
  MONGODB_URI: "mongodb+srv://user:pass@cluster.mongodb.net/openmmo"
  CORS_ORIGINS: "https://openmmo.ai,https://www.openmmo.ai,https://app.openmmo.ai"
---
apiVersion: v1
kind: Secret
metadata:
  name: openmmo-secrets
type: Opaque
stringData:
  JWT_SECRET: "your-512-bit-secret"
  GOOGLE_CLIENT_SECRET: "your-google-secret"
  OPENAI_API_KEY: "your-openai-key"
  GEMINI_API_KEY: "your-gemini-key"
```

---

## 🔄 How APP_API_URL is Used

The `APP_API_URL` environment variable is used to construct OAuth redirect URIs:

```yaml
# In application.yaml
oauth2:
  google:
    redirect-uri: ${APP_API_URL:http://localhost:8080}/api/v1/auth/google-callback

gmail:
  oauth:
    redirect-uri: ${APP_API_URL:http://localhost:8080}/api/v1/gmail/connect/callback
```

**Examples:**
- Local: `http://localhost:8080/api/v1/auth/google-callback`
- Production: `https://app.openmmo.ai/api/v1/auth/google-callback`

---

## 🚀 Deployment Scenarios

### Scenario 1: Change Domain for Production
```bash
# Before: http://localhost:8080
# After: https://app.openmmo.ai

# Set environment variable
APP_API_URL=https://app.openmmo.ai
APP_WEB_URL=https://openmmo.ai
```

**Resulting URLs:**
- Google OAuth Callback: `https://app.openmmo.ai/api/v1/auth/google-callback`
- Gmail OAuth Callback: `https://app.openmmo.ai/api/v1/gmail/connect/callback`

### Scenario 2: Staging Environment
```bash
APP_API_URL=https://staging-api.openmmo.ai
APP_WEB_URL=https://staging.openmmo.ai
```

### Scenario 3: Custom Domain
```bash
APP_API_URL=https://your-custom-domain.com
APP_WEB_URL=https://your-custom-domain.com
```

---

## ⚙️ Configuration Priority

Spring Boot reads configuration in this order (later overrides earlier):
1. `application.yaml` (default)
2. `application-{profile}.yaml` (if profile is set)
3. Environment variables

Example with `SPRING_PROFILES_ACTIVE=local`:
```
application.yaml (read first)
  ↓
application-local.yaml (overrides default)
  ↓
Environment variables (highest priority)
```

---

## 🔒 Security Notes

1. **Never commit sensitive values** to git:
   - `JWT_SECRET`
   - `GOOGLE_CLIENT_SECRET`
   - `OPENAI_API_KEY`
   - `GEMINI_API_KEY`
   - `MONGODB_URI` (contains credentials)

2. **Use environment variables** or secrets management:
   - Docker Secrets
   - Kubernetes Secrets
   - Cloud provider secret managers (AWS Secrets Manager, GCP Secret Manager, etc.)

3. **Use HTTPS** for production URLs

4. **Set CORS properly** - restrict to known origins in production

---

## 🐛 Troubleshooting

### OAuth Redirect Not Working
Check:
1. `APP_API_URL` is correct and matches the domain
2. Google OAuth settings have the correct redirect URI configured
3. CORS_ORIGINS includes your frontend domain

### MongoDB Connection Failed
Check:
1. `MONGODB_URI` is correct
2. Database server is running
3. Network/firewall allows connection

### JWT Validation Error
Check:
1. `JWT_SECRET` is set and same across all instances
2. Token hasn't expired

---

## 📚 Related Files

- `src/main/resources/application.yaml` - Default configuration
- `src/main/resources/application-local.yaml` - Development configuration
- `src/main/resources/application-prod.yaml` - Production configuration

