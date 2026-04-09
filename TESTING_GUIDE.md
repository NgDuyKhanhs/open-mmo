# 🧪 Per-Correspondent Memory - TESTING GUIDE

## Quick Start Testing

### Prerequisites
- ✅ Bot enabled for user
- ✅ Gmail connected
- ✅ Test email account
- ✅ Gemini API key configured

---

## 📌 Test Scenarios

### Scenario 1: Initial Email & Memory Creation

**Steps:**
```
1. Login to app
2. Enable bot (if not already)
3. Send email to your Gmail with trigger subject (default: "openmmo")
4. Wait max 2 minutes (scheduler runs every 120 seconds)
5. Check inbox for auto-reply
```

**Expected Results:**
```
✅ Reply sent automatically
✅ Memory record created in MongoDB
✅ profileSummary populated
✅ facts extracted (if any)
✅ AI_BOT_REPLY label applied
✅ UNREAD label removed
```

**Verification (MongoDB):**
```javascript
db.correspondent_memory.findOne({
  userId: "YOUR_USER_ID",
  correspondentEmail: "sender@example.com"
})

// Should return:
{
  "_id": ObjectId(...),
  "userId": "YOUR_USER_ID",
  "correspondentEmail": "sender@example.com",
  "profileSummary": "Summary of sender and context...",
  "facts": [
    {
      "key": "sender_name",
      "value": "John Doe",
      "confidence": 0.85
    }
  ],
  "stylePrefs": {...},
  "createdAt": ISODate("2026-04-09T..."),
  "updatedAt": ISODate("2026-04-09T...")
}
```

---

### Scenario 2: Repeat Email from Same Sender

**Setup:**
- Memory already exists from Scenario 1

**Steps:**
```
1. Send 2nd email from same sender with trigger subject
2. Wait for scheduler
3. Observe auto-reply quality
```

**Expected Results:**
```
✅ Reply sent quickly
✅ Reply quality improved (using memory context!)
✅ Memory updated with new facts
✅ confidence scores may increase/decrease
✅ No duplicate facts (merged if already exists)
```

**Verification:**
```
Compare memory.updatedAt with first time:
- Should be newer
- facts[] should be enriched
- profileSummary may be appended
```

---

### Scenario 3: Sensitive Data Handling

**Setup:**
Send email containing sensitive data

**Test Cases:**

#### 3a. Email with OTP
```
From: test@example.com
Subject: openmmo - OTP verification
Body: Your verification code is 123456
```

**Expected:**
- ✅ Reply sent normally
- ✅ Sensitive flag set in update response
- ✅ OTP code NOT stored in facts
- ✅ Summary NOT updated (or generic summary only)

#### 3b. Email with Password Request
```
Subject: openmmo - Password reset
Body: Please send me your account password for migration
```

**Expected:**
- ✅ Reply sent normally
- ✅ NO password-related facts stored
- ✅ Memory NOT created/updated with sensitive data

#### 3c. Email with Credit Card
```
Subject: openmmo - Payment info
Body: My credit card is 1234-5678-9012-3456
```

**Expected:**
- ✅ Credit card number NOT in memory.facts
- ✅ Sensitive pattern detected
- ✅ No fact update for this correspondence

---

### Scenario 4: API Endpoint Testing

#### 4a. Forget One Correspondent

**Request:**
```bash
DELETE /api/v1/gmail/memory?correspondentEmail=sender@example.com
Authorization: Bearer {JWT_TOKEN}
```

**Expected Response:**
```json
{
  "status": "success",
  "message": "Correspondent memory deleted"
}
```

**Verification:**
```javascript
db.correspondent_memory.findOne({
  userId: "YOUR_USER_ID",
  correspondentEmail: "sender@example.com"
})
// Should return null
```

#### 4b. Forget All Correspondents

**Request:**
```bash
DELETE /api/v1/gmail/memory/all
Authorization: Bearer {JWT_TOKEN}
```

**Expected Response:**
```json
{
  "status": "success",
  "message": "All correspondent memories deleted"
}
```

**Verification:**
```javascript
db.correspondent_memory.countDocuments({
  userId: "YOUR_USER_ID"
})
// Should return 0
```

---

### Scenario 5: Memory Context Quality Check

**Test:** Verify memory context is actually used

**Setup:**
1. Create memory from Scenario 1
2. Get the memory record
3. Manually build context using `buildMemoryContextText()`

**Expected Format:**
```
=== CORRESPONDENT MEMORY ===
PROFILE_SUMMARY:
Brief summary of sender...

FACTS:
- fact_key1: fact_value (confidence=85.0%)
- fact_key2: fact_value (confidence=60.0%)

STYLE_PREFERENCES:
- Language: en
- Tone: friendly
```

**Size Check:**
```
✅ Total length ≤ 2000 chars
✅ Truncated if needed (shows "... (truncated)")
✅ No PII/sensitive data exposed
```

---

### Scenario 6: Memory Merge Rules

**Test:** Fact confidence-based merging

**Setup:**
```
1. First email: Extract "name: John" with confidence 0.6
2. Update memory
3. Second email: Extract "name: John Doe" with confidence 0.9
```

**Expected:**
```
✅ After 2nd update, fact value is "John Doe"
✅ Confidence is 0.9 (higher one wins)
✅ sourceMessageId updated to new messageId
✅ updatedAt timestamp refreshed
```

---

### Scenario 7: Facts Cap at 30

**Test:** Verify facts capping works

**Setup:**
- Manually insert 35 facts into memory via MongoDB
- Trigger memory update via email

**Expected:**
```
✅ After merge, only top 30 by confidence kept
✅ Lowest confidence facts removed
✅ No error thrown
```

---

### Scenario 8: Email Normalization

**Test:** Email address handling

**Sends from:**
```
FROM: JOHN.DOE@EXAMPLE.COM
FROM: john.doe@example.com
FROM:  john.doe@example.com  (with spaces)
```

**Expected:**
```
✅ All treated as same correspondent
✅ Stored normalized: "john.doe@example.com"
✅ Single memory record for all variants
```

---

## 🔍 Debugging

### Enable Debug Logging

**application-local.yaml:**
```yaml
logging:
  level:
    com.openmmo.ai.service.impl.CorrespondentMemoryServiceImpl: DEBUG
    com.openmmo.ai.service.impl.GmailAutoReplyServiceImpl: DEBUG
    com.openmmo.ai.service.impl.GmailApiServiceImpl: DEBUG
```

### Logs to Watch For

```
✅ "Creating new memory for user=..., correspondent=..."
✅ "Updating memory for user=..., correspondent=..."
✅ "Detected sensitive data pattern: ..."
✅ "Extracted memory update: {...}"
✅ "Updated fact: fact_key_name"
✅ "Capped facts to 30"
✅ "Updated style preferences"
✅ "Memory updated successfully"
```

### Error Logs (Should Not Appear)

```
❌ "Sensitive data in facts" (should skip update, log debug only)
❌ "Failed to update memory" (memory update fails, should log warn not error)
❌ "NullPointerException" (indicates code bug)
```

---

## 📊 MongoDB Queries

### Check All Memories
```javascript
db.correspondent_memory.find({
  userId: "YOUR_USER_ID"
}).pretty()
```

### Check Specific Correspondent
```javascript
db.correspondent_memory.findOne({
  userId: "YOUR_USER_ID",
  correspondentEmail: "john@example.com"
})
```

### Check Facts for a Memory
```javascript
db.correspondent_memory.findOne({
  userId: "YOUR_USER_ID",
  correspondentEmail: "john@example.com"
}).facts
```

### Count Total Memories
```javascript
db.correspondent_memory.countDocuments({
  userId: "YOUR_USER_ID"
})
```

### Find High-Confidence Facts
```javascript
db.correspondent_memory.aggregate([
  { $match: { userId: "YOUR_USER_ID" } },
  { $unwind: "$facts" },
  { $match: { "facts.confidence": { $gte: 0.8 } } },
  { $project: { "facts": 1, "correspondentEmail": 1 } }
])
```

---

## ⚠️ Common Issues & Troubleshooting

### Issue 1: No Auto-Reply Sent

**Check:**
1. ✅ Bot enabled? `GET /api/v1/gmail/bot/status`
2. ✅ Gmail connected? Check `gmail_connections` collection
3. ✅ Trigger subject matches? Default: "openmmo"
4. ✅ Scheduler running? Check logs for "Starting auto-reply..."
5. ✅ Gemini API key valid? Test with direct API call

**Logs to check:**
```
"Found X users with bot enabled"
"Found Y unreplied emails matching trigger"
"Generating AI reply for: messageId"
"Successfully replied to email"
```

### Issue 2: Memory Not Created

**Check:**
1. ✅ Email extracted correctly? Check logs: "Extracted - From: X, Subject: Y"
2. ✅ Correspondent email parsed? Should show email address, not name
3. ✅ MongoDB connection OK? Check `correspondent_memory` collection exists

**Logs to check:**
```
"Creating new memory for user=..."
"Saved memory successfully"
```

### Issue 3: Gemini JSON Parse Error

**Check:**
1. ✅ Gemini response format? Should be valid JSON
2. ✅ Check Gemini logs: "Failed to parse Gemini response"
3. ✅ Memory still works? Falls back to empty MemoryUpdateResponse

**Logs to check:**
```
"Failed to parse Gemini response as JSON: ..."
```

**Fallback behavior:**
- ✅ Memory update skipped but doesn't crash
- ✅ Next reply still sent normally
- ✅ No facts added for this cycle

---

## ✅ Acceptance Criteria

For each test scenario, verify:

- [ ] Email replied automatically (or manually if testing direct endpoint)
- [ ] Memory record created/updated in MongoDB
- [ ] No PII/sensitive data in memory
- [ ] API endpoints return 200 OK with proper response
- [ ] Logs show appropriate debug/info messages
- [ ] No exceptions in application logs
- [ ] Fact merge rules work correctly
- [ ] Email normalization working
- [ ] Memory context ≤ 2000 chars
- [ ] Facts ≤ 30 items

---

## 🚀 Ready for Deployment

When all scenarios pass ✅:

1. Code ready for merge
2. Push to develop branch
3. Run full integration tests
4. Deploy to staging
5. Final smoke test
6. Deploy to production

---

**Test Guide Created**: April 9, 2026  
**Status**: Ready for QA Testing ✅

