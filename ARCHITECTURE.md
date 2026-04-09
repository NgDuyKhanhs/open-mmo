# 🏗️ GMAIL AI BOT BACKEND - ARCHITECTURE & FLOW

## 📊 PROJECT STRUCTURE

```
src/main/kotlin/com/openmmo/ai/

├── controller/                     [HTTP Endpoints]
│   ├── AuthenticationController     - Register, Login, OAuth, Refresh Token
│   ├── GmailController             - Gmail operations (list, search, send, labels)
│   └── AdminController             - Admin operations
│
├── service/                        [Business Logic Interfaces]
│   ├── IAuthenticationService      - Auth contracts
│   ├── IGmailService               - Gmail contracts (3 methods)
│   ├── IGeminiAiService            - Gemini AI contracts
│   └── impl/                       [Service Implementations]
│       ├── AuthenticationServiceImpl
│       ├── GmailOAuthServiceImpl
│       ├── GmailApiServiceImpl
│       ├── GmailBotServiceImpl
│       └── GeminiAiServiceImpl
│
├── client/                         [HTTP Wrappers - NO Logic]
│   ├── GmailOAuthClient            - Google OAuth API calls
│   ├── GmailApiClient              - Gmail API calls (base: gmail.api.base config)
│   └── GeminiApiClient             - Gemini AI API calls
│
├── repository/                     [MongoDB Data Access - NO Logic]
│   ├── UserRepository
│   ├── GmailConnectionRepository
│   └── GmailBotConfigRepository
│
├── entity/                         [MongoDB Models]
│   ├── User
│   ├── GmailConnection
│   └── GmailBotConfig
│
├── dto/                            [API Request/Response]
│   ├── AuthDtos (RegisterRequest, LoginRequest, AuthResponse, etc.)
│   └── GmailDtos (MailboxItemResponse, etc.)
│
├── exception/                      [Error Handling]
│   ├── ApiException.kt             - 5 exception types:
│   │   ├── BadRequestException (400)
│   │   ├── UnauthorizedException (401)
│   │   ├── NotFoundException (404)
│   │   ├── ConflictException (409)
│   │   └── UpstreamException (502)
│   └── GlobalExceptionHandler      - Centralized error mapping
│
├── security/                       [JWT Authentication]
│   └── JwtAuthenticationFilter
│
└── config/                         [Spring Configuration]
    ├── SecurityConfig              - JWT, CORS, Auth rules
    └── RestTemplateConfig          - HTTP client bean
```

---

## 🔄 MAILBOT FLOW - DETAILED

### 1️⃣ USER REGISTRATION & LOGIN

```
POST /api/v1/auth/register
├─ Input: RegisterRequest (email, password, name)
├─ AuthenticationController.register()
│  └─ AuthenticationServiceImpl.register()
│     ├─ Validate input
│     ├─ Check email not exists (UserRepository.existsByEmail)
│     ├─ Hash password (BCryptPasswordEncoder)
│     ├─ Save user (UserRepository.save)
│     └─ Generate tokens (JwtTokenProvider)
├─ Output: AuthResponse (user, accessToken, refreshToken)
└─ Status: 201 Created

POST /api/v1/auth/login
├─ Input: LoginRequest (email/username, password)
├─ AuthenticationController.login()
│  └─ AuthenticationServiceImpl.login()
│     ├─ Find user (UserRepository.findByEmailOrUsername)
│     ├─ Verify password (passwordEncoder.matches)
│     ├─ Generate tokens
│     └─ Set refreshToken in HttpOnly cookie
├─ Output: AuthResponse (user, accessToken)
└─ Status: 200 OK

POST /api/v1/auth/google-login
├─ Input: GoogleLoginRequest (idToken from Google)
├─ AuthenticationController.googleLogin()
│  └─ AuthenticationServiceImpl.googleLogin()
│     ├─ Verify Google token (GoogleIdTokenVerifier)
│     ├─ Find or create user
│     ├─ Generate JWT tokens
│     └─ Set refreshToken in HttpOnly cookie
├─ Output: AuthResponse (user, accessToken)
└─ Status: 200 OK
```

---

### 2️⃣ GMAIL OAUTH FLOW

```
GET /api/v1/gmail/connect/authorize
├─ Input: None (user must be authenticated)
├─ GmailController.authorizeGmail()
│  └─ GmailOAuthServiceImpl.generateAuthUrl()
│     ├─ Generate OAuth state (random + userId)
│     └─ Return Google OAuth URL
├─ Output: { url: "https://accounts.google.com/o/oauth2/v2/auth?..." }
└─ User → Redirected to Google → Grants permission

POST /api/v1/gmail/connect/callback
├─ Input: code=AUTH_CODE&state=STATE (from Google)
├─ GmailController.handleOAuthCallback()
│  └─ GmailOAuthServiceImpl.handleCallback()
│     ├─ Validate state (OAuth security)
│     ├─ Exchange code for tokens (GmailOAuthClient.exchangeCodeForTokens)
│     │  └─ POST https://oauth2.googleapis.com/token
│     │     ├─ Input: code, clientId, clientSecret
│     │     └─ Output: accessToken, refreshToken
│     ├─ Get Gmail profile (GmailOAuthClient.getGmailProfile)
│     │  └─ GET https://www.googleapis.com/gmail/v1/users/me/profile
│     │     └─ Output: emailAddress
│     ├─ Encrypt refreshToken (EncryptionUtil.encrypt)
│     ├─ Save connection (GmailConnectionRepository.save)
│     │  └─ GmailConnection { userId, gmailAddress, refreshTokenEnc }
│     └─ Create bot config (GmailBotConfigRepository.save)
│        └─ GmailBotConfig { userId, enabled: false }
└─ Status: 302 Redirect to /email-ai-bot?connected=1
```

---

### 3️⃣ GMAIL LIST/SEARCH EMAILS

```
GET /api/v1/gmail/mailbox?box=inbox&maxResults=20
├─ Input: box (inbox, sent, draft, etc.)
├─ GmailController.getMailbox()
│  └─ GmailApiServiceImpl.getMailbox()
│     ├─ Get user from auth
│     ├─ Find Gmail connection (GmailConnectionRepository.findByUserId)
│     ├─ Decrypt refreshToken (EncryptionUtil.decrypt)
│     ├─ Refresh accessToken if expired (GmailOAuthClient.refreshAccessToken)
│     │  └─ POST https://oauth2.googleapis.com/token
│     ├─ List messages (GmailApiClient.listMessages)
│     │  └─ GET https://www.googleapis.com/gmail/v1/users/me/messages?q=in:inbox
│     │     └─ Output: List of message IDs
│     ├─ For each message: Get details (GmailApiClient.getMessage)
│     │  └─ GET https://www.googleapis.com/gmail/v1/users/me/messages/{id}
│     └─ Convert to DTO (MailboxItemResponse)
├─ Output: List<MailboxItemResponse> { id, from, subject, date, snippet }
└─ Status: 200 OK

GET /api/v1/gmail/search?query=from:sender@example.com
├─ Input: query (Gmail search syntax)
├─ GmailController.searchMessages()
│  └─ GmailApiServiceImpl.searchMessages()
│     ├─ Refresh accessToken
│     ├─ Call GmailApiClient.listMessages(query=...)
│     └─ Return message IDs
├─ Output: List<String> [messageId1, messageId2, ...]
└─ Status: 200 OK
```

---

### 4️⃣ GMAIL SEND REPLY

```
POST /api/v1/gmail/send-reply
├─ Input: GmailReplyRequest { threadId, to, subject, body }
├─ GmailController.sendReply()
│  └─ GmailApiServiceImpl.sendReply()
│     ├─ Get user + Gmail connection
│     ├─ Refresh accessToken
│     ├─ Encode message to Base64
│     ├─ Send via GmailApiClient.sendMessage()
│     │  └─ POST https://www.googleapis.com/gmail/v1/users/me/messages/send
│     ├─ Apply AI_BOT_REPLY label (GmailApiClient.modifyLabels)
│     │  └─ POST https://www.googleapis.com/gmail/v1/users/me/messages/{id}/modify
│     └─ Log success
├─ Output: { success: true, message: "Email sent" }
└─ Status: 200 OK
```

---

### 4️⃣B GENERATE AI REPLY (NEW)

```
POST /api/v1/gmail/ai-reply?messageId={id}
├─ Input: messageId (query parameter)
├─ GmailController.generateAiReply()
│  └─ GmailApiServiceImpl.generateAiReply()
│     ├─ Get message body (GmailApiServiceImpl.getMessageBody)
│     ├─ Get custom prompt from bot config (GmailBotConfigRepository.findByUserId)
│     ├─ Generate reply (GeminiAiServiceImpl.generateReply)
│     │  └─ GeminiApiClient.generateReply()
│     │     └─ POST https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
│     │        ├─ Input: emailContent + customPrompt
│     │        └─ Output: Generated reply text
│     └─ Return reply text
├─ Output: { status: "success", reply: "Generated text..." }
└─ Status: 200 OK / 500 Error
```

---

### 5️⃣ BOT CONFIGURATION

```
GET /api/v1/gmail/bot/status
├─ Input: None (user must be authenticated)
├─ GmailController.getBotStatus()
│  └─ GmailBotServiceImpl.getStatus()
│     ├─ Find Gmail connection
│     ├─ Find bot config
│     └─ Return status
├─ Output: GmailStatusResponse { connected, gmailAddress, botEnabled, ... }
└─ Status: 200 OK

POST /api/v1/gmail/bot/enable
├─ Input: None
├─ GmailController.enableBot()
│  └─ GmailBotServiceImpl.enableBot()
│     ├─ Check Gmail is connected
│     ├─ Get or create bot config
│     ├─ Set enabled=true (GmailBotConfigRepository.save)
│     └─ Return success
├─ Output: { status: "Bot enabled" }
└─ Status: 200 OK

POST /api/v1/gmail/bot/disable
├─ Input: None
├─ GmailController.disableBot()
│  └─ GmailBotServiceImpl.disableBot()
│     ├─ Get bot config
│     ├─ Set enabled=false
│     └─ Return success
├─ Output: { status: "Bot disabled" }
└─ Status: 200 OK

POST /api/v1/gmail/bot/config
├─ Input: { triggerSubject: "openmmo" }
├─ GmailController.updateConfig()
│  └─ GmailBotServiceImpl.updateConfig()
│     ├─ Get or create bot config
│     ├─ Update triggerSubject
│     ├─ Save to MongoDB
│     └─ Return success
├─ Output: { status: "Config updated" }
└─ Status: 200 OK

GET /api/v1/gmail/bot/prompt
├─ GmailBotServiceImpl.getPrompt()
│  └─ Get custom prompt from config
├─ Output: { customPrompt: "..." }
└─ Status: 200 OK

POST /api/v1/gmail/bot/prompt
├─ Input: { customPrompt: "..." }
├─ GmailBotServiceImpl.updatePrompt()
│  └─ Update custom AI prompt
├─ Output: { status: "Prompt updated" }
└─ Status: 200 OK
```

---

## 🔐 SECURITY FLOW

```
1. User logs in → Get accessToken + refreshToken (in HttpOnly cookie)

2. Every request:
   ├─ Client sends: Authorization: Bearer {accessToken}
   ├─ JwtAuthenticationFilter intercepts request
   ├─ Verify JWT signature + expiry (JwtTokenProvider.validateToken)
   ├─ Extract userId from token (JwtTokenProvider.getUserIdFromToken)
   └─ Set authentication in context

3. AccessToken expires (24 hours):
   ├─ Client sends: refreshToken (in cookie or body)
   ├─ POST /api/v1/auth/refresh-token
   ├─ Generate new accessToken
   └─ Update refreshToken in cookie

4. Gmail API calls:
   ├─ Get Gmail connection + refreshToken
   ├─ Refresh Gmail accessToken if expired (valid for ~1 hour)
   ├─ Use new Gmail accessToken for API calls
   └─ Store encrypted refreshToken in MongoDB for future use
```

---

## 📋 AUTHENTICATION ENDPOINTS

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | /api/v1/auth/register | ❌ | Register new user |
| POST | /api/v1/auth/login | ❌ | Login with email + password |
| POST | /api/v1/auth/google-login | ❌ | Login with Google OAuth |
| POST | /api/v1/auth/refresh-token | ❌ | Refresh access token |
| POST | /api/v1/auth/change-password | ✅ | Change password |
| GET | /api/v1/auth/profile | ✅ | Get user profile |
| POST | /api/v1/auth/logout | ✅ | Logout (client-side) |

---

## 📋 GMAIL ENDPOINTS

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| GET | /api/v1/gmail/connect/authorize | ✅ | Get OAuth URL |
| GET | /api/v1/gmail/connect/callback | ❌ | OAuth callback (Google) |
| GET | /api/v1/gmail/mailbox | ✅ | List emails |
| GET | /api/v1/gmail/search | ✅ | Search emails |
| GET | /api/v1/gmail/message/{id} | ✅ | Get email details |
| POST | /api/v1/gmail/send-reply | ✅ | Send reply |
| POST | /api/v1/gmail/ai-reply | ✅ | Generate AI reply using Gemini |
| GET | /api/v1/gmail/bot/status | ✅ | Get bot status |
| POST | /api/v1/gmail/bot/enable | ✅ | Enable bot |
| POST | /api/v1/gmail/bot/disable | ✅ | Disable bot |
| POST | /api/v1/gmail/bot/config | ✅ | Update config |
| GET | /api/v1/gmail/bot/prompt | ✅ | Get custom prompt |
| POST | /api/v1/gmail/bot/prompt | ✅ | Update custom prompt |

---

## 🗄️ MONGODB COLLECTIONS

```
users
├─ _id: ObjectId
├─ email: String (unique)
├─ username: String (unique)
├─ password: String (hashed)
├─ firstName: String
├─ lastName: String
├─ googleId: String
├─ avatar: String (URL)
├─ emailVerified: Boolean
├─ isActive: Boolean
├─ roles: [String] (USER, ADMIN)
├─ loginProvider: String (local, google)
├─ createdAt: DateTime
└─ updatedAt: DateTime

gmail_connections
├─ _id: ObjectId
├─ userId: String (FK)
├─ gmailAddress: String
├─ refreshTokenEnc: String (encrypted)
├─ scopes: String
├─ createdAt: DateTime
└─ updatedAt: DateTime

gmail_bot_configs
├─ _id: ObjectId
├─ userId: String (FK)
├─ enabled: Boolean (default: false)
├─ triggerSubject: String (default: "openmmo")
├─ customPrompt: String (optional)
├─ lastRunAt: DateTime
├─ lastError: String
├─ createdAt: DateTime
└─ updatedAt: DateTime
```

---

## 🛠️ EXCEPTION HANDLING

```
All exceptions caught by GlobalExceptionHandler:

BadRequestException (400)
├─ Input validation errors
├─ Malformed requests
└─ Example: Invalid email format

UnauthorizedException (401)
├─ Missing/invalid JWT token
├─ Invalid credentials
├─ Expired refresh token
└─ Example: Invalid Google OAuth token

NotFoundException (404)
├─ Resource not found
├─ User doesn't exist
├─ Gmail connection missing
└─ Example: Gmail not connected for user

ConflictException (409)
├─ Duplicate data
├─ Email already exists
├─ User already connected to Gmail
└─ Example: Email already registered

UpstreamException (502)
├─ External API errors
├─ Gmail API failures
├─ Google OAuth errors
└─ Example: Gmail API timeout

Response Format:
{
  "success": false,
  "message": "Error description",
  "error": "ERROR_CODE",
  "path": "/api/v1/endpoint",
  "timestamp": 1712604000000
}
```

---

## 🚀 DEPLOYMENT

```
1. Compile:
   ./gradlew build

2. Output:
   build/libs/ai-0.0.1-SNAPSHOT.jar

3. Environment Variables:
   - GOOGLE_CLIENT_ID
   - GOOGLE_CLIENT_SECRET
   - GMAIL_API_BASE
   - JWT_SECRET
   - TOKEN_ENC_KEY_BASE64
   - MONGODB_URI
   - CORS_ORIGINS

4. Run:
   java -jar ai-0.0.1-SNAPSHOT.jar

5. Docker:
   docker build -t ai-backend .
   docker run -p 8080:8080 ai-backend
```

---

## 🧠 CORRESPONDENT MEMORY (Per-User, Per-Correspondent)

### Overview
Implements intelligent memory management per correspondent to improve reply quality over time.
Each (userId, correspondentEmail) pair has its own memory with:
- **profileSummary**: Context/relationship/key insights
- **facts**: Structured key-value pairs with confidence scores
- **stylePrefs**: Language, tone, formatting preferences

### Architecture

```
correspondent_memory Collection (MongoDB)
├── userId (String, indexed)
├── correspondentEmail (String, lowercase, unique per user)
├── profileSummary (String, max 3000 chars)
├── facts (List<MemoryFact>)
│   ├── key (String, snake_case)
│   ├── value (String)
│   ├── confidence (Double 0.0-1.0)
│   └── sourceMessageId (String)
├── stylePrefs
│   ├── language ("vi", "en", null)
│   ├── tone ("friendly", "formal", "professional", null)
│   └── formattingNotes (String, null)
└── Metadata: lastSeenAt, lastThreadId, version, createdAt, updatedAt
```

### Flow: Auto-Reply with Memory

```
1. Load Memory
   ├─ CorrespondentMemoryService.getOrCreate(userId, correspondentEmail)
   └─ Build context: buildMemoryContextText()

2. Generate Reply
   ├─ GmailAutoReplyServiceImpl.autoReplyForUser()
   │  ├─ Get message (headers, body, threadId)
   │  ├─ Extract correspondent email
   │  └─ GmailApiServiceImpl.generateAiReplyWithMemory()
   │     ├─ Build full prompt (base + custom + memory + email)
   │     └─ GeminiApiClient.generateText(fullPrompt)

3. Send & Update Memory
   ├─ gmailApiService.sendReply()
   ├─ Apply AI_BOT_REPLY label
   └─ correspondentMemoryService.updateAfterReply()
      ├─ Call Gemini to extract facts (JSON response)
      ├─ Parse & merge (handle sensitive data)
      ├─ Update summary + facts + stylePrefs
      └─ Save to MongoDB
```

### Sensitive Data Detection

**Patterns blocked** (not stored in memory):
- OTP / verification codes (4-8 digits + keyword)
- password, token, api key, secret
- credit card, cvv, ssn, cccd, cmnd
- Custom detection: regex + keyword matching

**Behavior**:
- If sensitive detected → skip fact updates
- May update summary generically or skip
- Never store PII/credentials

### Fact Merge Rules

```
1. Normalize key: lowercase + snake_case
2. If key exists:
   ├─ Update if confidence(new) > confidence(old)
   └─ Keep if confidence(new) <= confidence(old)
3. If key is new: add it
4. Cap facts at 30 (remove lowest confidence if exceeded)
```

### Memory Context Size Limits

```
- Max Summary: 3000 chars (truncate from start if over)
- Max Facts: 30 (sort by confidence, keep top 30)
- Max Context Text: 2000 chars (for prompt injection)
```

### Endpoints

```
DELETE /api/v1/gmail/memory?correspondentEmail=...
  ├─ Forget one correspondent
  └─ Auth required (JWT)

DELETE /api/v1/gmail/memory/all
  ├─ Forget all correspondents for user
  └─ Auth required (JWT)
```

### Implementation Details

**CorrespondentMemoryServiceImpl**:
- `getOrCreate()`: Load or create memory record
- `buildMemoryContextText()`: Format for prompt injection
- `updateAfterReply()`: Call Gemini to extract update (JSON)
- `forgetCorrespondent()`: Delete one record
- `forgetAll()`: Delete all for user
- `detectSensitiveData()`: Pattern matching
- `extractMemoryUpdate()`: Gemini JSON extraction
- `mergeMemoryUpdate()`: Apply update safely

**GmailApiServiceImpl** additions:
- `getMessageMeta()`: Fetch threadId + labelIds
- `generateAiReplyWithMemory()`: New method with context
- `buildFullPrompt()`: Construct prompt with memory

**GmailAutoReplyServiceImpl** integration:
- Load memory before generating reply
- Use `generateAiReplyWithMemory()` instead of `generateAiReply()`
- Update memory after sending reply (async in MVP)

### Testing

**Unit tests** cover:
- Email extraction from headers
- Sensitive pattern detection
- Memory context truncation
- Fact merge rules (confidence logic)
- Facts cap at 30 limit
- Email normalization (lowercase, trim)

See: `CorrespondentMemoryServiceImplTest.kt`

### Future Enhancements

- Async memory updates (non-blocking)
- Manual memory editing UI
- Memory export/import per correspondent
- Automatic memory cleanup (archive old facts)
- Memory conflict resolution (multiple sources)
- Memory analytics (fact usage, confidence trends)

---

## ✅ KEY PRINCIPLES

✅ **Thin Controllers** - No logic, only parse/validate/delegate
✅ **Fat Services** - All business logic here
✅ **Separated Clients** - Pure HTTP, no logic
✅ **Repository Pattern** - MongoDB only, no logic
✅ **Interface-Based DI** - Easy to test/mock
✅ **Centralized Error Handling** - GlobalExceptionHandler
✅ **Clean Architecture** - Clear separation of concerns
✅ **Security** - JWT auth + OAuth + encrypted tokens
✅ **Logging** - Proper logging at each layer
✅ **DTOs** - No entity exposure via API

---

*Last Updated: April 9, 2026*
*Version: 1.1.0 (with Per-Correspondent Memory)*
*Status: Production Ready ✅*

