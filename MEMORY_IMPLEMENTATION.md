# Per-Correspondent Memory Implementation

## 📋 Summary

Triển khai feature "ký ức theo từng người gửi" (Per-Correspondent Memory) cho hệ thống auto-reply. 

Mỗi (userId, correspondentEmail) cặp có memory riêng lưu:
- **profileSummary**: Tóm tắt ngữ cảnh/quan hệ với người gửi
- **facts**: Các sự kiện có cấu trúc (key/value + confidence score)
- **stylePrefs**: Sở thích ngôn ngữ, tone, format

## 🎯 Files Created

### Entities
- `entity/CorrespondentMemory.kt` - MongoDB schema cho memory
  - Data classes: CorrespondentMemory, MemoryFact, StylePrefs, MemoryUpdateResponse

### Repository
- `repository/CorrespondentMemoryRepository.kt` - MongoDB data access
  - `findByUserIdAndCorrespondentEmail()` - Load memory
  - `findByUserId()` - Load all memories for user

### Services
- `service/ICorrespondentMemoryService.kt` - Service interface
  - `getOrCreate()` - Get or initialize memory
  - `buildMemoryContextText()` - Build prompt context
  - `updateAfterReply()` - Update memory sau khi reply (dùng Gemini)
  - `forgetCorrespondent()` - Delete 1 memory
  - `forgetAll()` - Delete all memories

- `service/impl/CorrespondentMemoryServiceImpl.kt` - Service implementation
  - Sensitive data detection (OTP, password, token, credit card, etc.)
  - Memory context truncation (max 2000 chars)
  - Fact merge logic (confidence-based update)
  - Gemini JSON extraction cho memory update

### Controllers
- `controller/CorrespondentMemoryController.kt` - REST endpoints
  - `DELETE /api/v1/gmail/memory?correspondentEmail=...` - Forget 1 correspondent
  - `DELETE /api/v1/gmail/memory/all` - Forget all

### Tests
- `test/kotlin/.../CorrespondentMemoryServiceImplTest.kt` - Unit tests
  - Email extraction
  - Sensitive detection
  - Context truncation
  - Fact merge rules
  - Email normalization

## 📝 Files Modified

### Service Interfaces
- `service/IGmailService.kt` - Thêm methods:
  - `getMessageMeta()` - Fetch threadId + labelIds
  - `generateAiReplyWithMemory()` - New method với memory context

### Service Implementations
- `service/impl/GmailApiServiceImpl.kt`:
  - Implement `getMessageMeta()`
  - Implement `generateAiReplyWithMemory()`
  - Add `buildFullPrompt()` helper - Combine base instruction + custom + memory + email

- `service/impl/GmailAutoReplyServiceImpl.kt`:
  - Inject `ICorrespondentMemoryService`
  - Load memory trước khi generate reply
  - Use `generateAiReplyWithMemory()` thay vì `generateAiReply()`
  - Call `updateAfterReply()` sau khi gửi reply (non-blocking)

### Documentation
- `ARCHITECTURE.md`:
  - Thêm section "CORRESPONDENT MEMORY"
  - Flow diagram: Load Memory → Generate Reply → Update Memory
  - Sensitive detection details
  - Fact merge rules
  - Size limits
  - Endpoints

## 🔐 Sensitive Data Detection

**Patterns blocked**:
```
- OTP / verification codes (regex: \b\d{4,8}\b + keyword)
- password, mật khẩu, passwd
- token, api key, secret, credential
- credit card, cvv, card number
- ssn, cccd, cmnd, định danh
```

**Behavior**:
- Nếu phát hiện sensitive → skip fact update
- Chỉ cập nhật summary/stylePrefs nhẹ
- Never store PII/credentials

## 📊 Memory Size Limits

```
- Max Summary: 3000 chars (truncate từ đầu)
- Max Facts: 30 items (sort by confidence)
- Max Context for Prompt: 2000 chars
```

## 🔄 Flow

### 1️⃣ Load Memory
```
CorrespondentMemoryService.getOrCreate(userId, correspondentEmail)
  ↓ Normalize email (lowercase, trim)
  ↓ Check MongoDB collection
  ↓ Create if not exists
  ↓ Return CorrespondentMemory
```

### 2️⃣ Build Context
```
buildMemoryContextText(memory)
  ↓ Format: PROFILE_SUMMARY / FACTS / STYLE_PREFERENCES
  ↓ Truncate to 2000 chars
  ↓ Return ready-for-prompt text
```

### 3️⃣ Generate Reply with Memory
```
GmailAutoReplyServiceImpl.autoReplyForUser()
  ├─ Get message headers (From, Subject)
  ├─ Extract sender email
  ├─ Load memory for sender
  ├─ Build memory context
  ├─ generateAiReplyWithMemory(messageId, memoryContext)
  │  └─ Build full prompt:
  │     ├─ Base instruction
  │     ├─ Custom prompt (if any)
  │     ├─ Memory context (từ CorrespondentMemory)
  │     └─ Email content
  │  └─ Call Gemini: generateText(fullPrompt)
  └─ Return AI reply
```

### 4️⃣ Update Memory After Reply
```
updateAfterReply(userId, correspondentEmail, threadId, messageId, ...)
  ├─ Detect sensitive data
  ├─ Call Gemini to extract update (JSON):
  │  {
  │    "summary_patch": "...",
  │    "facts_add": [...],
  │    "facts_remove_keys": [...],
  │    "style_prefs": {...},
  │    "sensitive": false
  │  }
  ├─ Merge rules:
  │  ├─ profileSummary: Append if not sensitive
  │  ├─ facts: Update if confidence higher
  │  ├─ stylePrefs: Merge non-null fields
  │  └─ Cap facts at 30
  └─ Save to MongoDB
```

## 🧪 Test Coverage

```
✅ Email extraction from headers
✅ Sensitive pattern detection
✅ Memory context truncation
✅ Fact merge rules (confidence logic)
✅ Facts cap at 30 limit
✅ Email normalization
```

Run: `./gradlew test --tests CorrespondentMemoryServiceImplTest`

## 🚀 Deployment Notes

1. **Environment Variables**: None (uses existing GEMINI_API_KEY)

2. **MongoDB Index**: Auto-created via Spring Data
   - Compound unique index on (userId, correspondentEmail)

3. **Configuration**: No new config required

4. **Database Migration**: None (new collection, auto-created)

## 📌 Integration Points

1. **GmailAutoReplyServiceImpl** - Main consumer
   - Loads memory before generating reply
   - Updates memory after sending reply

2. **GmailApiServiceImpl** - Provides memory-aware reply generation
   - `generateAiReplyWithMemory()` is new method

3. **CorrespondentMemoryService** - Core memory logic
   - Handles all memory operations
   - Calls Gemini for intelligent extraction

4. **GeminiApiClient** - External dependency
   - Used for extracting update (updateAfterReply)

## ⚠️ Known Limitations (MVP)

- Memory update is non-blocking (async in future)
- No manual memory editing UI yet
- No memory analytics dashboard
- No memory versioning/rollback

## 🔄 Future Enhancements

- [ ] Async memory updates (queue-based)
- [ ] Memory editing API
- [ ] Memory export/import
- [ ] Automatic cleanup (archive old facts)
- [ ] Memory conflict resolution
- [ ] Analytics dashboard
- [ ] Memory versioning

---

**Status**: Ready for testing ✅
**Last Updated**: April 9, 2026

