# 🤖 Agent Skill - Project OpenMMO AI Assistant

**Last Updated:** April 14, 2026
**Project Type:** Full-stack Kotlin + Vue.js Gmail AI Bot
**Status:** Active Development

---

## 📋 Project Overview

**OpenMMO AI Email Assistant** - Automated Gmail reply system using AI (Groq/Gemini) to:
- Filter emails by subject keyword (triggerSubject from BotConfig)
- Generate intelligent replies using Groq API
- Track processed emails
- Support memory context for conversation history
- Provide web UI for management

---

## 🏗️ Architecture

### **Backend Stack**
- **Language:** Kotlin
- **Framework:** Spring Boot 3.x
- **Database:** MongoDB (Gmail connections, configs, processed messages)
- **API Client:** RestTemplate
- **AI Provider:** Groq API (primary), Gemini fallback
- **Auth:** OAuth 2.0 (Google Gmail)
- **Token Encryption:** Custom EncryptionUtil with Base64 key

### **Frontend Stack**
- **Framework:** Vue 3 + TypeScript
- **Build:** Vite
- **UI Components:** Radix Vue, Lucide icons
- **State:** Pinia stores
- **Styling:** Tailwind CSS + Custom CSS
- **API Communication:** Fetch API with token refresh

### **Key Technologies**
- Gmail API (REST)
- MongoDB (NoSQL)
- Docker
- Gradle (build)
- Spring Cache (@Cacheable for optimization)

---

## 📁 Project Structure

```
D:\TAI VE\ai/
├── backend (Kotlin/Spring Boot)
│   ├── src/main/kotlin/com/openmmo/ai/
│   │   ├── client/
│   │   │   ├── GmailApiClient.kt         ← HTTP wrapper, no business logic
│   │   │   └── GroqApiClient.kt
│   │   ├── service/
│   │   │   ├── IGmailApiService.kt       ← Interface (contracts)
│   │   │   └── impl/
│   │   │       └── GmailApiServiceImpl.kt ← Business logic
│   │   ├── entity/
│   │   │   ├── GmailConnection.kt        ← MongoDB: OAuth tokens
│   │   │   ├── GmailBotConfig.kt         ← MongoDB: Bot settings
│   │   │   └── GmailProcessedMessage.kt  ← MongoDB: Reply tracking
│   │   ├── repository/
│   │   │   ├── GmailConnectionRepository.kt
│   │   │   ├── GmailBotConfigRepository.kt
│   │   │   └── GmailProcessedMessageRepository.kt
│   │   ├── controller/
│   │   │   └── GmailApiController.kt     ← REST endpoints
│   │   ├── dto/
│   │   │   └── GmailDtos.kt              ← Request/Response objects
│   │   ├── util/
│   │   │   └── EncryptionUtil.kt         ← Token encryption
│   │   └── exception/
│   │       └── GmailRefreshTokenException.kt
│   └── build.gradle.kts
│
├── frontend (Vue 3/TypeScript)
│   ├── src/
│   │   ├── views/
│   │   │   ├── LoginView.vue             ← OAuth login
│   │   │   ├── EmailAiBotView.vue        ← Main app (mailbox + pagination)
│   │   │   ├── PrivacyPolicyView.vue
│   │   │   └── TermsOfServiceView.vue
│   │   ├── components/
│   │   │   ├── Navbar.vue
│   │   │   ├── Header.vue
│   │   │   ├── ChatAssistant.vue
│   │   │   └── ...
│   │   ├── services/
│   │   │   ├── gmailService.ts           ← API calls (with token refresh)
│   │   │   ├── authService.ts            ← OAuth token management
│   │   │   └── index.ts
│   │   ├── stores/
│   │   │   ├── useAuthStore.ts           ← Pinia auth state
│   │   │   ├── useChatStore.ts
│   │   │   ├── useServicesStore.ts
│   │   │   └── useUIStore.ts
│   │   ├── config/
│   │   │   ├── apiConfig.ts              ← API base URL
│   │   │   └── ...
│   │   └── App.vue
│   └── vite.config.ts
│
└── 📄 Documentation
    ├── ARCHITECTURE.md
    ├── BACKEND_ENV_GUIDE.md
    ├── TESTING_GUIDE.md
    ├── MEMORY_IMPLEMENTATION.md
    └── AGENT_SKILL.md (this file)
```

---

## 🔌 Core Entities (MongoDB)

### **GmailConnection** (OAuth tokens)
```kotlin
data class GmailConnection(
    val userId: String,
    val gmailAddress: String,
    val refreshTokenEnc: String,  // Encrypted refresh token
    val connectedAt: LocalDateTime
)
```

### **GmailBotConfig** (Bot settings)
```kotlin
data class GmailBotConfig(
    val userId: String,
    val enabled: Boolean,
    val triggerSubject: String,        // Email filter keyword (e.g., "openmmo")
    val customPrompt: String,          // AI context
    val aiProvider: String,            // "groq" or "gemini"
    val processedLabel: String         // Gmail label for replied emails
)
```

### **GmailProcessedMessage** (Reply tracking)
```kotlin
data class GmailProcessedMessage(
    val userId: String,
    val messageId: String,
    val threadId: String,
    val aiProvider: String,            // Which AI generated the reply
    val processedAt: LocalDateTime
)
```

---

## 🔄 Key Workflows

### **1. Email Mailbox Listing (Pagination)**
```
Frontend: loadEmails() 
  ↓
Backend: getMailboxPage(userId, boxType="inbox", pageSize=5, pageToken=null)
  ├─ Build query: "in:inbox subject:openmmo"
  ├─ listMessages(accessToken, query, pageSize)
  ├─ Parallel fetch metadata (80% smaller payload)
  ├─ Extract: from, subject, date, snippet
  └─ Return: MailboxPageResponse { emails, nextPageToken }
  ↓
Frontend: Display 5 emails + pagination buttons
```

**Key:** Subject filter happens at **Gmail API** level (server-side), not client-side filtering.

### **2. Generating AI Reply**
```
Frontend: selectEmail(email) → generateAiReply()
  ↓
Backend: generateAiReply(userId, messageId)
  ├─ Get email body: getMessageBody()
  ├─ Get bot config (cached 5min): getOrCacheBotConfig()
  ├─ Build prompt: buildPrompt(emailContent, customPrompt, memoryContext)
  ├─ Call Groq: groqApiClient.generateText(prompt)
  └─ Return: AI-generated reply text
  ↓
Frontend: Display reply in modal
```

### **3. Sending Reply**
```
Frontend: confirmSendReply(toEmail, subject, bodyText)
  ↓
Backend: sendReply(userId, threadId, toEmail, subject, bodyText)
  ├─ Validate email address
  ├─ Create MIME message (Base64 encoded)
  ├─ Send via Gmail API
  ├─ Track: saveProcessedMessage(userId, threadId, "groq")
  └─ Apply label: modifyLabels(..., [processedLabel], ...)
  ↓
Frontend: Show success toast
```

### **4. Token Refresh (Auto)**
```
Any API call → 401 Unauthorized
  ├─ Clear token cache
  ├─ refreshAndCacheToken()
  │  ├─ Decrypt refreshTokenEnc
  │  ├─ Call oauthService.refreshAccessToken()
  │  └─ Cache new token (ThreadLocal)
  └─ Retry original request
```

---

## 📡 REST API Endpoints

**Base URL:** `http://localhost:8080/api/gmail`

### **Authentication**
- `POST /connect` - Get Gmail OAuth URL
- `POST /callback` - Handle OAuth callback

### **Mailbox Operations**
- `GET /mailbox-page?box=inbox&pageSize=5&pageToken=...` - List emails (paginated)
- `GET /message/{messageId}/body` - Get email body
- `GET /message/{messageId}/headers` - Get email headers
- `GET /message/{messageId}/meta` - Get threadId, labels

### **AI Operations**
- `GET /generate-reply/{messageId}` - Generate AI reply
- `GET /generate-reply/{messageId}?memoryContext=...` - With memory
- `POST /send-reply` - Send reply email

### **Configuration**
- `GET /status` - Get bot status
- `POST /config` - Update bot config (triggerSubject, customPrompt)

### **Label Management**
- `GET /labels` - List all labels
- `POST /labels/{labelName}` - Create label

---

## 🛠️ Common Operations & Which Tool to Use

### **When to do what:**

| Task | Tool | File Path |
|------|------|-----------|
| **Change Gmail query logic** | replace_string_in_file | `GmailApiServiceImpl.kt` (line ~71-130) |
| **Add new API endpoint** | insert_edit_into_file | `GmailApiController.kt` |
| **Modify email parsing** | read_file first, then edit | `GmailApiClient.kt` getMessageMetadata() |
| **Update frontend UI** | replace_string_in_file | `EmailAiBotView.vue` |
| **Fix pagination** | replace_string_in_file | `EmailAiBotView.vue` line 587 |
| **Check compilation errors** | get_errors | After editing .kt files |
| **Understand codebase** | semantic_search | For unfamiliar code patterns |
| **Find function usage** | grep_search | Search for function names |
| **View large file** | read_file with offset/limit | For files >2000 lines |

---

## ⚡ Performance Optimizations

### **Token Caching (ThreadLocal)**
- Prevents 100x refresh when processing 100 emails
- Cached per userId during request lifecycle
- Cleared on 401 Unauthorized

### **Config Caching (@Cacheable)**
- BotConfig cached 5 minutes (Spring cache)
- `getOrCacheBotConfig(userId)` → Database query only on first call
- Avoids 100x DB queries for same user

### **Metadata Endpoint**
- Uses `format=metadata` → 80% smaller payload
- Only fetches: From, Subject, Date headers
- Perfect for mailbox listing

### **Parallel Processing**
- Uses ForkJoinPool to fetch metadata for all emails simultaneously
- `CompletableFuture.allOf()` waits for all to complete

---

## 🔑 Important Code Patterns

### **Safe Null Handling**
```kotlin
// ✅ Safe: Handles null at each step
botConfig?.triggerSubject?.takeIf { it.isNotBlank() }?.let { triggerSubject ->
    query += " subject:$triggerSubject"
}

// ❌ Avoid: Can throw NPE
if (!botConfig?.triggerSubject.isNullOrBlank()) { // WRONG!
    val value = botConfig!!.triggerSubject
}
```

### **Token Refresh Retry Pattern**
```kotlin
try {
    // Make request
} catch (e: HttpClientErrorException) {
    if (e.statusCode.value() == 401) {
        // Force refresh
        cache.remove(userId)
        accessToken = getAccessToken(connection)  // Gets fresh token
        // Retry request
    }
}
```

### **Response Type Casting**
```kotlin
@Suppress("UNCHECKED_CAST")
val response = restTemplate.exchange(..., Map::class.java).body as? Map<String, Any>
val messages = response?.get("messages") as? List<Map<String, String>> ?: emptyList()
```

---

## 🧪 Testing Workflows

### **Backend Testing**
- Run: `./gradlew test`
- Test file: Look for `*Test.kt` or `*Tests.kt`
- Check compilation: `get_errors` on edited .kt files

### **Frontend Testing**
- Run: `npm run dev` (dev server)
- Check: Browser console for errors
- Verify: API calls in Network tab

### **Integration Testing**
1. Connect Gmail (OAuth flow)
2. Set triggerSubject in BotConfig
3. Call `/mailbox-page` → Should filter by subject
4. Generate reply → Should use correct prompt
5. Send reply → Should track in GmailProcessedMessage

---

## 🔐 Security Notes

### **Token Encryption**
- Refresh tokens stored encrypted in MongoDB
- Key: `${token.enc.key-base64}` from application.yaml
- Decrypt: `EncryptionUtil.decrypt(encryptedToken, key)`

### **OAuth Validation**
- All endpoints require JWT token
- Token refresh handled automatically
- 401 errors trigger auto-refresh

---

## 📊 Database Queries

### **Common MongoDB queries:**
```kotlin
// Find user's connection
connectionRepository.findByUserId(userId)

// Find user's bot config
botConfigRepository.findByUserId(userId)

// Find processed message by threadId (to lookup aiProvider)
gmailProcessedMessageRepository.findByUserIdAndThreadId(userId, threadId)

// Save processed message record
gmailProcessedMessageRepository.save(GmailProcessedMessage(...))
```

---

## 🚀 Deployment

### **Environment Variables** (application.yaml)
```yaml
gmail:
  api:
    base: https://www.googleapis.com/gmail/v1/users/me
groq:
  api:
    key: ${GROQ_API_KEY}
token:
  enc:
    key-base64: ${TOKEN_ENC_KEY_BASE64}
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
```

### **Docker**
- Backend: `Dockerfile.backend`
- Frontend: `frontend/Dockerfile`
- Compose: `docker-compose.yml`

---

## 🎯 Common Issues & Solutions

| Issue | Solution | File |
|-------|----------|------|
| **Compilation error on `triggerSubject`** | Field name is `triggerSubject` not `subjectKeyword` | `GmailBotConfig.kt` |
| **Pagination buttons disappear** | Check `v-if="currentPageIndex > 0 \|\| hasNextPage"` | `EmailAiBotView.vue` line 587 |
| **401 Unauthorized repeating** | Token refresh failing → Check `GROQ_API_KEY` or MongoDB connection | Check logs |
| **No emails showing** | Verify `triggerSubject` is set in BotConfig | Check `/status` endpoint |
| **Subject filter not working** | Filter must be in Gmail query (`subject:keyword`), not client-side | `GmailApiServiceImpl.kt` line ~90 |
| **Memory leak in token cache** | ThreadLocal is cleared per request, safe to use | `tokenCache.remove(userId)` on 401 |

---

## 📝 Code Review Checklist

When editing code, verify:

- [ ] **Kotlin files:** Run `get_errors` after edits
- [ ] **Type safety:** Use `as? Type` for casting, never `as Type`
- [ ] **Null handling:** Use `?.let {}` or `takeIf {}`
- [ ] **Logging:** Add `logger.info()` for important operations
- [ ] **Error handling:** Catch specific exceptions, not generic `Exception`
- [ ] **Database:** Use repository pattern, not direct queries
- [ ] **API responses:** Always include error handling + proper status codes
- [ ] **Frontend:** Test pagination edge cases (empty pages, last page)
- [ ] **Security:** Never log passwords/tokens

---

## 📚 References

- Gmail API: https://developers.google.com/gmail/api/reference/rest
- Groq API: https://console.groq.com
- Spring Boot Docs: https://spring.io/projects/spring-boot
- Vue 3 Docs: https://vuejs.org
- MongoDB Docs: https://docs.mongodb.com

---

## 🎓 Agent Decision Tree

```
User asks to modify something
  ├─ "Filter emails by X"
  │  └─ Edit: GmailApiServiceImpl.kt (getMailboxPage method)
  │     └─ Modify Gmail query building logic
  │
  ├─ "Add new API endpoint"
  │  └─ Edit: GmailApiController.kt
  │     └─ Use @GetMapping/@PostMapping
  │
  ├─ "Fix UI issue"
  │  └─ Edit: EmailAiBotView.vue
  │     └─ Check: v-if conditions, computed properties
  │
  ├─ "Understand how X works"
  │  └─ Use: semantic_search or grep_search
  │     └─ Then: read_file for full context
  │
  └─ "Generate AI reply not working"
     └─ Check: GmailApiServiceImpl.buildPrompt()
        └─ Verify: groqApiClient integration
```

---

**Last Modified:** April 14, 2026
**Agent Version:** 1.0
**Project Status:** ✅ Active Development

