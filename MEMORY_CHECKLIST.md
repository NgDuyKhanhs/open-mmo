# Per-Correspondent Memory - Implementation Checklist

## ✅ Implementation Checklist

### Entities & Schema
- [x] CorrespondentMemory entity
  - [x] userId (indexed)
  - [x] correspondentEmail (indexed, lowercase)
  - [x] profileSummary (max 3000 chars)
  - [x] facts (List<MemoryFact>)
  - [x] stylePrefs (language, tone, formattingNotes)
  - [x] Compound unique index on (userId, correspondentEmail)
  - [x] Metadata (lastSeenAt, lastThreadId, version, createdAt, updatedAt)

- [x] MemoryFact data class
  - [x] key (snake_case)
  - [x] value
  - [x] confidence (0.0-1.0)
  - [x] sourceMessageId
  - [x] updatedAt

- [x] StylePrefs data class
  - [x] language ("vi", "en", null)
  - [x] tone ("friendly", "formal", "professional", null)
  - [x] formattingNotes

- [x] MemoryUpdateResponse (for Gemini JSON)
  - [x] summary_patch
  - [x] facts_add
  - [x] facts_remove_keys
  - [x] style_prefs
  - [x] sensitive flag

### Repository
- [x] CorrespondentMemoryRepository
  - [x] findByUserIdAndCorrespondentEmail()
  - [x] findByUserId()
  - [x] Extends MongoRepository

### Service Interface
- [x] ICorrespondentMemoryService
  - [x] getOrCreate(userId, correspondentEmail)
  - [x] buildMemoryContextText(memory)
  - [x] updateAfterReply(userId, correspondentEmail, threadId, messageId, emailSubject, emailBody, replyBody)
  - [x] forgetCorrespondent(userId, correspondentEmail)
  - [x] forgetAll(userId)

### Service Implementation
- [x] CorrespondentMemoryServiceImpl
  - [x] Inject CorrespondentMemoryRepository
  - [x] Inject GeminiApiClient
  - [x] @Service annotation
  - [x] getOrCreate() - normalize email, check exists, create if needed
  - [x] buildMemoryContextText()
    - [x] Format with PROFILE_SUMMARY / FACTS / STYLE_PREFERENCES
    - [x] Truncate to 2000 chars
  - [x] detectSensitiveData()
    - [x] Regex patterns for OTP, password, token, credit card, SSN
  - [x] extractMemoryUpdate()
    - [x] Call Gemini with JSON extraction prompt
    - [x] Parse response as MemoryUpdateResponse
  - [x] mergeMemoryUpdate()
    - [x] Merge summary (append if not sensitive)
    - [x] Merge facts (confidence-based update)
    - [x] Normalize fact keys (snake_case, lowercase)
    - [x] Cap facts at 30
    - [x] Merge stylePrefs (non-null only)
  - [x] forgetCorrespondent() - delete by userId + email
  - [x] forgetAll() - delete all by userId
  - [x] updateAfterReply() - orchestrate all above

### Gmail API Service Updates
- [x] IGmailApiService interface
  - [x] Add getMessageMeta(userId, messageId)
  - [x] Add generateAiReplyWithMemory(userId, messageId, memoryContext)

- [x] GmailApiServiceImpl implementation
  - [x] Implement getMessageMeta() - return threadId + labelIds
  - [x] Implement generateAiReplyWithMemory()
    - [x] Get message body
    - [x] Get custom prompt
    - [x] Build full prompt with memory
    - [x] Call Gemini generateText()
  - [x] Add buildFullPrompt() helper
    - [x] Base instruction
    - [x] Custom prompt section
    - [x] Memory context section
    - [x] Email section

### Auto-Reply Service Integration
- [x] GmailAutoReplyServiceImpl
  - [x] Inject ICorrespondentMemoryService
  - [x] In autoReplyForUser():
    - [x] Get message headers (From, Subject)
    - [x] Extract sender email address
    - [x] Get threadId via getMessageMeta()
    - [x] Load memory for correspondent
    - [x] Build memory context
    - [x] Use generateAiReplyWithMemory() instead of generateAiReply()
    - [x] After sending reply:
      - [x] Call updateAfterReply() (wrapped in try-catch, non-blocking)
      - [x] Log any memory update errors as warnings

### Controller
- [x] CorrespondentMemoryController
  - [x] @RestController on /api/v1/gmail/memory
  - [x] @DeleteMapping() - forgetCorrespondent(correspondentEmail)
  - [x] @DeleteMapping("/all") - forgetAll()
  - [x] Auth required (Authentication parameter)

### Unit Tests
- [x] CorrespondentMemoryServiceImplTest
  - [x] testExtractCorrespondentEmail() - regex test
  - [x] testSensitivePatternDetection() - OTP, password, token, credit card
  - [x] testMemoryContextTruncation() - max 2000 chars
  - [x] testFactsMergeRulesHigherConfidence() - update if higher
  - [x] testFactsMergeRulesLowerConfidence() - don't update if lower
  - [x] testFactsCapAtMax() - cap at 30
  - [x] testEmailNormalization() - lowercase, trim

### Documentation
- [x] ARCHITECTURE.md
  - [x] Add "CORRESPONDENT MEMORY" section
  - [x] Overview of feature
  - [x] MongoDB schema
  - [x] Flow diagram
  - [x] Sensitive detection rules
  - [x] Fact merge rules
  - [x] Size limits
  - [x] API endpoints
  - [x] Implementation details
  - [x] Testing notes
  - [x] Future enhancements
  - [x] Update version to 1.1.0

- [x] MEMORY_IMPLEMENTATION.md
  - [x] Feature summary
  - [x] Files created/modified
  - [x] Sensitive patterns
  - [x] Size limits
  - [x] Detailed flow
  - [x] Test coverage
  - [x] Deployment notes

## 🔍 Verification Checklist

### Code Quality
- [x] No hardcoded API keys/secrets
- [x] Proper null handling
- [x] Try-catch for external API calls
- [x] Non-blocking error handling in auto-reply
- [x] Logging at appropriate levels

### Security
- [x] Memory scoped per (userId, correspondentEmail)
- [x] No sensitive data stored (OTP, passwords, tokens)
- [x] Email normalization (lowercase, trim)
- [x] Input validation

### Performance
- [x] MongoDB compound index for fast lookups
- [x] Memory context truncated (2000 chars)
- [x] Facts capped (30 items)
- [x] Summary truncated (3000 chars)

### Architecture
- [x] Clean service layer separation
- [x] Interface-based DI
- [x] Repository pattern
- [x] No business logic in controller
- [x] No direct entity exposure

### API Compliance
- [x] RESTful endpoints
- [x] Proper HTTP methods (GET, POST, DELETE)
- [x] Auth required (Authentication parameter)
- [x] Response format consistent

## 🚀 Deployment Steps

1. ✅ All files created/modified
2. ✅ Code compiles without errors
3. ✅ Unit tests pass
4. ⏳ Integration tests (manual)
5. ⏳ Push to repository
6. ⏳ Deploy to production

## 📌 Integration Points

| Component | Integration | Status |
|-----------|-----------|--------|
| GmailAutoReplyServiceImpl | Loads & updates memory | ✅ Done |
| GmailApiServiceImpl | Generates reply with memory context | ✅ Done |
| GeminiApiClient | Extracts memory updates | ✅ Done |
| CorrespondentMemoryService | Core memory logic | ✅ Done |
| CorrespondentMemoryController | REST API | ✅ Done |
| MongoDB | Data persistence | ✅ Ready |

## 🎯 Expected Behavior

### Scenario: First email from john@example.com
```
1. Load memory → Create empty record
2. Build context → "" (empty)
3. Generate reply → Standard prompt (no memory context)
4. Send reply
5. Update memory → Extract facts from email + reply
6. Store → profileSummary + facts + stylePrefs
```

### Scenario: Repeat email from john@example.com
```
1. Load memory → Found!
2. Build context → "PROFILE_SUMMARY: ... FACTS: ... STYLE_PREFERENCES: ..."
3. Generate reply → Full prompt with memory (better context)
4. Send reply
5. Update memory → Merge new facts + update summary
6. Store → Updated record with enhanced facts
```

### Scenario: Forget correspondent
```
DELETE /api/v1/gmail/memory?correspondentEmail=john@example.com
→ Record deleted
→ Next email treated as "first" again
```

---

**Status**: ✅ **IMPLEMENTATION COMPLETE**  
**Ready for**: Integration testing, QA, Deployment  
**Last Updated**: April 9, 2026

