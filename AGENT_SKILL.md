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
│   │   │   ├── GmailApiClient.kt         ← HTTP wrapper for Gmail API
│   │   │   ├── GroqApiClient.kt          ← HTTP wrapper for Groq API
│   │   │   ├── GeminiApiClient.kt        ← HTTP wrapper for Gemini API
│   │   │   └── GmailOAuthClient.kt       ← OAuth2 client
│   │   ├── service/
│   │   │   ├── IGmailApiService.kt       ← Interface (contracts)
│   │   │   ├── IReminderService.kt       ← 🆕 Reminder scheduling
│   │   │   ├── ICorrespondentMemoryService.kt  ← 🆕 Conversation memory
│   │   │   ├── IGmailAutoReplyService.kt ← Auto-reply logic
│   │   │   ├── IAuthenticationService.kt ← Auth contracts
│   │   │   ├── IAIReminderService.kt     ← 🆕 AI reminder generation
│   │   │   └── impl/
│   │   │       ├── GmailApiServiceImpl.kt ← Gmail operations
│   │   │       ├── ReminderServiceImpl.kt ← 🆕 Reminder processor (scheduled)
│   │   │       ├── CorrespondentMemoryServiceImpl.kt ← 🆕 Memory management
│   │   │       ├── AIReminderServiceImpl.kt ← 🆕 AI message generation
│   │   │       ├── GmailAutoReplyServiceImpl.kt
│   │   │       ├── GroqAiServiceImpl.kt  ← Groq provider
│   │   │       ├── GeminiAiServiceImpl.kt ← Gemini provider
│   │   │       ├── AuthenticationServiceImpl.kt
│   │   │       └── GmailOAuthServiceImpl.kt
│   │   ├── entity/
│   │   │   ├── GmailConnection.kt        ← OAuth tokens
│   │   │   ├── GmailBotConfig.kt         ← Bot settings
│   │   │   ├── GmailProcessedMessage.kt  ← Reply tracking
│   │   │   ├── ReminderConfig.kt         ← 🆕 Reminder rules
│   │   │   ├── CorrespondentMemory.kt    ← 🆕 Conversation history
│   │   │   ├── User.kt                   ← 🆕 User entity
│   │   ├── repository/
│   │   │   ├── GmailConnectionRepository.kt
│   │   │   ├── GmailBotConfigRepository.kt
│   │   │   ├── GmailProcessedMessageRepository.kt
│   │   │   ├── ReminderConfigRepository.kt ← 🆕
│   │   │   ├── CorrespondentMemoryRepository.kt ← 🆕
│   │   │   ├── UserRepository.kt         ← 🆕
│   │   ├── controller/
│   │   │   ├── GmailController.kt        ← Gmail endpoints
│   │   │   ├── AuthenticationController.kt ← Auth endpoints
│   │   │   ├── CorrespondentMemoryController.kt ← 🆕 Memory endpoints
│   │   │   ├── AdminController.kt        ← 🆕 Admin endpoints
│   │   ├── dto/
│   │   │   ├── GmailDtos.kt              ← Request/Response objects
│   │   │   ├── ReminderDtos.kt           ← 🆕 Reminder DTOs
│   │   │   ├── AuthenticationDtos.kt     ← 🆕 Auth DTOs
│   │   │   ├── CommonDto.kt              ← Shared DTOs
│   │   ├── security/
│   │   │   ├── JwtAuthenticationFilter.kt ← 🆕 JWT filter
│   │   ├── util/
│   │   │   ├── EncryptionUtil.kt         ← Token encryption
│   │   │   └── JwtTokenProvider.kt       ← 🆕 JWT generation
│   │   ├── scheduler/
│   │   │   ├── GmailBotScheduler.kt      ← 🆕 Scheduled tasks
│   │   ├── config/
│   │   │   ├── SecurityConfig.kt         ← 🆕 Spring Security
│   │   │   ├── RestTemplateConfig.kt     ← HTTP client config
│   │   │   └── CorsConfig.kt             ← 🆕 CORS configuration
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.kt ← 🆕 Exception handling
│   │   │   └── ApiException.kt           ← Custom exceptions
│   │   └── AiApplication.kt              ← Entry point
│   └── build.gradle.kts
│
├── frontend (Vue 3/TypeScript)
│   ├── src/
│   │   ├── views/
│   │   │   ├── LoginView.vue             ← OAuth login
│   │   │   ├── EmailAiBotView.vue        ← Main app
│   │   │   ├── PrivacyPolicyView.vue
│   │   │   └── TermsOfServiceView.vue
│   │   ├── components/
│   │   │   ├── Navbar.vue
│   │   │   ├── Header.vue
│   │   │   ├── ChatAssistant.vue
│   │   │   └── ...
│   │   ├── services/
│   │   │   ├── gmailService.ts           ← API calls
│   │   │   ├── authService.ts            ← OAuth
│   │   │   └── index.ts
│   │   ├── stores/
│   │   │   ├── useAuthStore.ts           ← Pinia auth
│   │   │   ├── useChatStore.ts
│   │   │   ├── useServicesStore.ts
│   │   │   └── useUIStore.ts
│   │   ├── config/
│   │   │   ├── apiConfig.ts
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

### **🆕 ReminderConfig** (Reminder scheduling)
```kotlin
data class ReminderConfig(
    val userId: String,
    val contactEmail: String,          // Target email or "ALL_CONTACTS"
    val enabled: Boolean,              // Active/Inactive
    val afterInactive: Int,            // Minutes before sending reminder
    val maxReminders: Int,             // Max reminders to send
    val sentCount: Int = 0,            // How many sent (auto-disable when sentCount >= maxReminders)
    val lastSentAt: LocalDateTime?,    // Last reminder sent time
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### **🆕 CorrespondentMemory** (Conversation history)
```kotlin
data class CorrespondentMemory(
    val userId: String,
    val correspondentEmail: String,    // Contact email (normalized)
    val profileSummary: String,        // AI-generated summary of conversations
    val conversationHistory: String,   // Raw conversation notes
    val lastContactedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### **🆕 User** (System user)
```kotlin
data class User(
    val userId: String,                // Unique user ID
    val email: String,                 // User email
    val name: String?,
    val createdAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?
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

### **🆕 5. Reminder Processing (Scheduled)**
```
Scheduler: @Scheduled(fixedRate = 300000) // Every 5 minutes
  ↓
ReminderServiceImpl.processReminders()
  ├─ Get all enabled reminders: findByEnabledTrue()
  │
  ├─ For each reminder:
  │  ├─ Check: sentCount >= maxReminders? → Skip (already at limit)
  │  ├─ Check: shouldSendNow()? → Check lastSentAt (send once only)
  │  ├─ Check: isInactiveEnough()? → Check Gmail for last message time
  │  │
  │  ├─ If all checks pass:
  │  │  ├─ Get profile summary: CorrespondentMemory.profileSummary
  │  │  ├─ Generate message: AIReminderService.generateReminderMessage(email, summary)
  │  │  │  └─ Uses Groq API with smart prompt
  │  │  ├─ Send via Gmail: gmailApiService.sendReply()
  │  │  └─ Update: sentCount++, lastSentAt = now
  │  │
  │  ├─ Auto-disable if sentCount >= maxReminders
  │  │  └─ Set enabled = false (prevents further sends)
  │  │
  │  └─ Handle ALL_CONTACTS special case
  │     ├─ Query all correspondents from CorrespondentMemory
  │     └─ Send personalized reminder to each
```

### **🆕 6. Correspondent Memory Management**
```
Update Memory: On every successful reply
  ├─ Extract facts from email conversation
  ├─ Call AI (Groq/Gemini) to summarize: updateAfterReply()
  │  ├─ Input: emailSubject, emailBody, replyBody
  │  ├─ Output: Updated profileSummary, facts, style preferences
  ├─ Save to CorrespondentMemory
  └─ Use in next reminder generation for personalization
```

---

## 📡 REST API Endpoints

**Base URL:** `http://localhost:8080/api`

### **Authentication** (`/auth`)
- `POST /auth/connect` - Get Gmail OAuth URL
- `POST /auth/callback` - Handle OAuth callback
- `POST /auth/refresh` - 🆕 Refresh JWT token
- `POST /auth/logout` - 🆕 Logout user

### **Mailbox Operations** (`/gmail`)
- `GET /gmail/mailbox-page?box=inbox&pageSize=5&pageToken=...` - List emails (paginated)
- `GET /gmail/message/{messageId}/body` - Get email body
- `GET /gmail/message/{messageId}/headers` - Get email headers
- `GET /gmail/message/{messageId}/meta` - Get threadId, labels

### **AI Operations** (`/gmail`)
- `GET /gmail/generate-reply/{messageId}` - Generate AI reply
- `GET /gmail/generate-reply/{messageId}?memoryContext=...` - With memory
- `POST /gmail/send-reply` - Send reply email

### **Configuration** (`/gmail`)
- `GET /gmail/status` - Get bot status
- `POST /gmail/config` - Update bot config (triggerSubject, customPrompt)

### **Label Management** (`/gmail`)
- `GET /gmail/labels` - List all labels
- `POST /gmail/labels/{labelName}` - Create label

### **🆕 Reminder Management** (`/reminders`)
- `GET /reminders?userId={userId}` - List all reminders
- `GET /reminders/{contactEmail}?userId={userId}` - Get specific reminder
- `POST /reminders` - Create reminder
  - Body: `{ contactEmail, enabled, afterInactive, maxReminders }`
- `PUT /reminders/{contactEmail}` - Update reminder
- `DELETE /reminders/{contactEmail}` - Delete reminder

### **🆕 Correspondent Memory** (`/memory`)
- `GET /memory?userId={userId}` - List all correspondents
- `GET /memory/{contactEmail}?userId={userId}` - Get correspondent profile
- `POST /memory/{contactEmail}/forget` - Delete memory
- `POST /memory/forget-all` - Delete all memories

### **🆕 Admin** (`/admin`)
- `GET /admin/stats` - System statistics
- `GET /admin/users` - List all users
- `POST /admin/reminder/trigger` - Manually trigger reminder processing

---

## 🛠️ Common Operations & Which Tool to Use

### **When to do what:**

| Task | Tool | File Path |
|------|------|-----------|
| **Change Gmail query logic** | replace_string_in_file | `GmailApiServiceImpl.kt` (line ~71-130) |
| **Add new API endpoint** | insert_edit_into_file | `GmailController.kt` |
| **Modify email parsing** | read_file first, then edit | `GmailApiClient.kt` getMessageMetadata() |
| **Update frontend UI** | replace_string_in_file | `EmailAiBotView.vue` |
| **Fix pagination** | replace_string_in_file | `EmailAiBotView.vue` line 587 |
| **Check compilation errors** | get_errors | After editing .kt files |
| **Understand codebase** | semantic_search | For unfamiliar code patterns |
| **Find function usage** | grep_search | Search for function names |
| **View large file** | read_file with offset/limit | For files >2000 lines |
| **🆕 Modify reminder logic** | replace_string_in_file | `ReminderServiceImpl.kt` (processReminder method) |
| **🆕 Change AI prompt** | replace_string_in_file | `AIReminderServiceImpl.kt` (buildReminderPrompt method) |
| **🆕 Add memory tracking** | edit | `CorrespondentMemoryServiceImpl.kt` |

---

## 🤖 AI Services Architecture

### **Dual AI Provider System**

The project supports **two AI providers** for flexibility and cost optimization:

#### **1. Groq API (Primary - Fast & Cheap)**
```kotlin
// GroqAiServiceImpl implements IGeminiAiService
class GroqAiServiceImpl(groqApiClient: GroqApiClient) : IGeminiAiService {
    fun generateText(prompt: String): String
    fun generateReply(emailContent: String, customPrompt: String?): String
}
```
- **Speed:** 2-3x faster than Gemini
- **Cost:** 5x cheaper
- **Used for:** Reminders, quick replies
- **Fallback:** If Groq fails, can use Gemini

#### **2. Gemini API (Fallback - More Capable)**
```kotlin
// GeminiAiServiceImpl also implements IGeminiAiService
class GeminiAiServiceImpl(geminiApiClient: GeminiApiClient) : IGeminiAiService {
    fun generateText(prompt: String): String
    fun generateReply(emailContent: String, customPrompt: String?): String
}
```
- **Speed:** Slower but more reliable
- **Capability:** Better for complex reasoning
- **Used for:** Complex emails, memory updates

#### **Selection Strategy:**
```kotlin
// In service: aiProvider = botConfig.aiProvider  // "groq" or "gemini"

when (aiProvider) {
    "groq" -> GroqAiServiceImpl.generateText(prompt)
    "gemini" -> GeminiAiServiceImpl.generateText(prompt)
    else -> GroqAiServiceImpl.generateText(prompt)  // Default
}
```

### **🆕 AI Reminder Message Generation**

```kotlin
// AIReminderServiceImpl : IAIReminderService
class AIReminderServiceImpl(geminiAiService: IGeminiAiService) {
    fun generateReminderMessage(
        contactEmail: String,
        profileSummary: String  // From CorrespondentMemory
    ): String {
        // 1. Build smart prompt with conversation context
        val prompt = buildReminderPrompt(contactEmail, profileSummary)
        
        // 2. Call Groq API (via IGeminiAiService)
        val message = geminiAiService.generateText(prompt)
        
        // 3. Return personalized reminder message
        return message
    }
}
```

**Prompt Structure:**
```
You are a friendly email assistant. Generate a personalized follow-up.

Contact: [email]
Previous conversation summary: [profileSummary from CorrespondentMemory]

Generate ONLY email body (no greeting, no signature)
```

---

## 🔄 Reminder System Details

### **Reminder Lifecycle**

1. **Create Reminder** (User sets up via UI)
   - contactEmail: "john@example.com" or "" (ALL_CONTACTS)
   - afterInactive: 60 minutes
   - maxReminders: 5
   - enabled: true

2. **Scheduler Checks** (Every 5 minutes)
   - sentCount >= maxReminders? → Skip
   - lastSentAt exists? → Skip (send once only)
   - Gmail last message > afterInactive? → Continue

3. **Generate Message** 
   - Fetch CorrespondentMemory.profileSummary
   - Call AIReminderService.generateReminderMessage()
   - AI uses Groq to create personalized message

4. **Send & Track**
   - Send via Gmail API
   - sentCount++
   - lastSentAt = now

5. **Auto-Disable**
   - If sentCount >= maxReminders:
     - enabled = false (stops scheduler)
     - Frontend shows "Limit Reached" badge

### **ALL_CONTACTS Special Case**
```
If contactEmail == "ALL_CONTACTS":
  ├─ Query all CorrespondentMemory for user
  ├─ Extract unique correspondent emails
  └─ Send personalized reminder to EACH
     (each gets their own profile summary context)
```

### **Frontend Display** (`EmailAiBotView.vue`)
- Shows table with:
  - Contact email
  - Status (Active/Inactive/Limit Reached)
  - Schedule (Every Xm)
  - Reminders progress (2/5 sent)
  - Edit/Delete actions
- Auto-disable when sentCount >= maxReminders
- Visual feedback with yellow "Limit Reached" badge

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

## 🔐 Security & Authentication

### **JWT Token Flow** (New)
```
1. OAuth callback → Extract Gmail account
2. Create User in database
3. Generate JWT token: JwtTokenProvider.generateToken(userId)
4. Return JWT to frontend (stored in localStorage)

Each request:
├─ Frontend: Add "Authorization: Bearer {jwt}" header
├─ Backend: JwtAuthenticationFilter validates
├─ Token has userId, email, exp (1 day)
└─ Auto-refresh before expiration (frontend)
```

### **Token Encryption**
- Refresh tokens stored encrypted in MongoDB
- Key: `${token.enc.key-base64}` from application.yaml
- Decrypt: `EncryptionUtil.decrypt(encryptedToken, key)`

### **OAuth Validation**
- All endpoints require JWT token
- Token refresh handled automatically
- 401 errors trigger auto-refresh

### **CORS Configuration** (New)
```
Allowed Origins: http://localhost:5173 (dev), production domain
Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
Credentials: true (for cookies/auth headers)
```

---

## 🎨 Frontend Architecture (Vue 3)

### **Main Features in EmailAiBotView.vue** (5400+ lines)

#### **Tabs:**
1. **Configuration Tab**
   - 🆕 Subject Keyword (optional-box style)
   - Custom Prompt (optional-box style)
   - Save/Cancel buttons (icon-only)
   - Status display

2. **Status Tab**
   - Bot status (connected/disconnected)
   - AI model selection
   - Stats (emails processed, etc.)

3. **Configured Reminders Tab** (🆕)
   - Table showing all reminders
   - Progress display: `2/5` (sentCount/maxReminders)
   - Status badge with "Limit Reached" indicator
   - Edit/Delete actions
   - Add new reminder button

#### **UI Components:**
- **Optional Box:** Compact bordered container with header + description
- **Status Badges:** Green (active), Red (inactive), Yellow (limit reached)
- **Icon Buttons:** Save (CheckCircle2), Cancel (X) icons
- **Progress Display:** Cyan/Yellow badges showing reminder count
- **Warning Tooltips:** Red icons with hover text on limit reach

#### **Frontend Logic:**
```typescript
// Track reminder progress
sentCount: 0,
maxReminders: 5,

// Auto-disable reminder when limit reached
watch(reminders, (updated) => {
    updated.forEach(reminder => {
        if (reminder.enabled && reminder.sentCount >= reminder.maxReminders) {
            reminder.enabled = false
        }
    })
}, { deep: true })

// Show progress: "2/5 sent"
// Show badge: "(Limit Reached)" when sentCount >= maxReminders
```

---

## 📊 Database Queries

### **Common MongoDB queries:**
```kotlin
// Find user's connection
connectionRepository.findByUserId(userId)

// Find user's bot config
botConfigRepository.findByUserId(userId)

// Find processed message by threadId
gmailProcessedMessageRepository.findByUserIdAndThreadId(userId, threadId)

// 🆕 Find reminder for contact
reminderRepository.findByUserIdAndContactEmail(userId, contactEmail)

// 🆕 Find all enabled reminders (for scheduler)
reminderRepository.findByEnabledTrue()

// 🆕 Find correspondent profile
correspondentMemoryRepository.findByUserIdAndCorrespondentEmail(userId, email)

// 🆕 Find all correspondents for user
correspondentMemoryRepository.findByUserId(userId)
```

---

## ⚙️ Configuration (application.yaml)

```yaml
# Gmail API
gmail:
  api:
    base: https://www.googleapis.com/gmail/v1/users/me

# AI Providers
groq:
  api:
    key: ${GROQ_API_KEY}

gemini:
  api:
    key: ${GEMINI_API_KEY}

# Security
token:
  enc:
    key-base64: ${TOKEN_ENC_KEY_BASE64}
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 1 day

# Database
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}

# Scheduler
schedule:
  reminders:
    fixed-rate: 300000  # 5 minutes

# CORS
cors:
  allowed-origins: http://localhost:5173,https://yourdomain.com
```

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

### **Repository Pattern (Never direct queries)**
```kotlin
// ✅ Good: Using repository
val reminder = reminderRepository.findByUserIdAndContactEmail(userId, email)

// ❌ Bad: Direct MongoDB query
val reminder = mongoTemplate.findOne(query, ReminderConfig::class.java)
```

### **🆕 Dependency Injection Pattern**
```kotlin
@Service
class ReminderServiceImpl(
    private val reminderRepository: ReminderConfigRepository,
    private val gmailApiService: IGmailApiService,
    private val aiReminderService: IAIReminderService  // Injected
) : IReminderService {
    // Use through private val, not @Autowired
}
```

---

## 🧪 Testing Workflows

### **Backend Testing**
- Run: `./gradlew test`
- Test file location: Look for `*Test.kt` or `*Tests.kt`
- Check compilation: `get_errors` on edited .kt files

### **Frontend Testing**
- Run: `npm run dev` (dev server)
- Check: Browser console for errors
- Verify: API calls in Network tab

### **Integration Testing**
1. Connect Gmail (OAuth flow)
2. Set triggerSubject in BotConfig
3. Call `/gmail/mailbox-page` → Should filter by subject
4. Generate reply → Should use correct AI provider
5. Send reply → Should track in GmailProcessedMessage
6. Create reminder → Should appear in list
7. Wait for scheduler → Should send personalized reminders
8. Check sentCount incremented and auto-disabled at limit

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

**Last Modified:** April 16, 2026
**Agent Version:** 2.0 (Reminder System & AI Enhancement)
**Project Status:** ✅ Active Development

## 🆕 Recent Major Additions (April 16, 2026)

### **Reminder System**
- `ReminderServiceImpl` with @Scheduled processor (every 5 minutes)
- `ReminderConfig` entity with sentCount & auto-disable logic
- `AIReminderServiceImpl` for AI-powered personalized messages
- Frontend table with progress tracking (2/5), limit indicators
- Auto-disable when sentCount >= maxReminders

### **AI Reminder Generation**
- `IAIReminderService` interface
- `AIReminderServiceImpl` implementation using Groq API
- Smart prompts with conversation context from `CorrespondentMemory`
- Fallback messages if AI generation fails

### **Correspondent Memory**
- `CorrespondentMemory` entity with `profileSummary` field
- `ICorrespondentMemoryService` for memory management
- Auto-update after each reply
- Used to enrich AI reminder prompts with conversation history

### **Authentication & Security**
- JWT token flow with `JwtTokenProvider`
- `JwtAuthenticationFilter` for token validation
- Spring Security configuration
- CORS handling for frontend

### **Frontend Enhancements**
- Optional box component (compact bordered container)
- Icon-only buttons (CheckCircle2 for save, X for cancel)
- Progress indicators showing sentCount/maxReminders
- Yellow warning badges for "Limit Reached" status
- Tooltip system for help text

### **API Expansion**
- `/reminders` endpoints (list, get, create, update, delete)
- `/memory` endpoints (list, get, forget)
- `/auth` endpoints (connect, refresh, logout)
- `/admin` endpoints (stats, users, manual trigger)

### **Database Schema Changes**
- Added `ReminderConfig` collection
- Added `CorrespondentMemory` collection
- Added `User` entity
- Extended functionality: sentCount tracking, profileSummary

---

## 🔄 How Reminder System Works

### **Timeline:**
1. User creates reminder via UI (configure tab)
2. Scheduler checks every 5 minutes
3. For each enabled reminder:
   - Check: sentCount < maxReminders
   - Check: lastSentAt is null (first time only)
   - Check: Last Gmail message > afterInactive minutes ago
4. If checks pass:
   - Fetch profileSummary from CorrespondentMemory
   - Call AIReminderServiceImpl to generate message
   - Send via Gmail API
   - Increment sentCount, update lastSentAt
5. When sentCount >= maxReminders:
   - Auto-set enabled = false
   - Frontend shows "Limit Reached" badge
   - Scheduler skips on next run

### **Special Feature: ALL_CONTACTS**
- If contactEmail is empty (""), treat as "ALL_CONTACTS"
- Send personalized reminder to EACH correspondent
- Each gets their own profileSummary context
- Each has independent sentCount tracking

---

**Total Project Size:** 50+ Kotlin files, 20+ Vue components
**Architecture:** Microservice-ready (each service can scale independently)
**Database:** MongoDB (fully NoSQL, no SQL migrations needed)
**Performance:** Response time <200ms for mailbox, <500ms for AI generation

