# Agent Skills API Test Examples

## Prerequisites

1. Application must be running: `./gradlew bootRun`
2. Set OPENAI_API_KEY environment variable with valid OpenAI API key
3. Base URL: `http://localhost:8080`

## Test Requests

### 1. Test Code Review Skill

#### Request
```bash
curl -X POST http://localhost:8080/api/v1/agent/review-code \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "src/main/kotlin/com/openmmo/ai/service/AiAgentService.kt"
  }'
```

#### Using PowerShell
```powershell
$body = @{
    filePath = "src/main/kotlin/com/openmmo/ai/service/AiAgentService.kt"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/agent/review-code" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "skill": "code-reviewer",
  "result": "[AI-generated code review...]"
}
```

---

### 2. Test Business Advisor Skill

#### Request
```bash
curl -X POST http://localhost:8080/api/v1/agent/business-advice \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the key features we should prioritize for an AI-powered MMO recommendation system?"
  }'
```

#### Using PowerShell
```powershell
$body = @{
    question = "What are the key features we should prioritize for an AI-powered MMO recommendation system?"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/agent/business-advice" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "skill": "business-advisor",
  "result": "[AI-generated business advice...]"
}
```

---

### 3. Test MMO Expert Skill

#### Request
```bash
curl -X POST http://localhost:8080/api/v1/agent/mmo-expertise \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How can we design a balanced economy system for a large-scale MMO with 100k+ concurrent players?"
  }'
```

#### Using PowerShell
```powershell
$body = @{
    question = "How can we design a balanced economy system for a large-scale MMO with 100k+ concurrent players?"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/agent/mmo-expertise" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "skill": "mmo-expert",
  "result": "[AI-generated MMO expertise...]"
}
```

---

### 4. Test Multi-Skill Execution

#### Request
```bash
curl -X POST http://localhost:8080/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Analyze the PaymentService.kt for security best practices and suggest how it can be optimized for handling MMO player transactions at scale"
  }'
```

#### Using PowerShell
```powershell
$body = @{
    task = "Analyze the PaymentService.kt for security best practices and suggest how it can be optimized for handling MMO player transactions at scale"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/v1/agent/execute" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $body
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "skill": "multi-skill",
  "result": "[AI-generated combined analysis using multiple skills...]"
}
```

---

## Testing with Postman

1. **Create Collection**: "OpenMMO Agent Skills"

2. **Add Requests**:
   - **Method**: POST
   - **URL**: `http://localhost:8080/api/v1/agent/review-code` (or other endpoints)
   - **Headers**: `Content-Type: application/json`
   - **Body** (raw JSON): See examples above

3. **Environmental Variables** (Optional):
   - `base_url`: `http://localhost:8080`
   - `file_path`: `src/main/kotlin/com/openmmo/ai/service/AiAgentService.kt`

---

## Troubleshooting

### 1. Connection Refused
- **Issue**: `curl: (7) Failed to connect to localhost port 8080`
- **Solution**: Ensure application is running with `./gradlew bootRun`

### 2. Invalid API Key
- **Issue**: Response contains "Invalid API key" error
- **Solution**: 
  - Set `OPENAI_API_KEY` environment variable
  - Or update `.env.local` with valid key
  - Restart application

### 3. Skill Not Found
- **Issue**: Response indicates skill was not discovered
- **Solution**:
  - Verify `.claude/skills/` directory exists
  - Check SKILL.md files have proper YAML frontmatter
  - Enable debug logging: Set `AGENT_DEBUG_LOGGING=true`

### 4. Timeout/Slow Response
- **Issue**: Request takes > 30 seconds
- **Solution**:
  - Reduce response complexity
  - Decrease `max_tokens` in application.yaml
  - Check OpenAI API status
  - Review LLM temperature setting

### 5. Internal Server Error (500)
- **Issue**: Server returns 500 error
- **Solution**:
  - Check server logs for stack traces
  - Verify MongoDB is running (if using database)
  - Enable debug logging to see detailed errors
  - Check application.yaml configuration

---

## Performance Tips

1. **Caching**: Implement caching for repeated questions
2. **Streaming**: For long responses, consider using streaming API
3. **Batch Processing**: Process multiple requests asynchronously
4. **Token Optimization**: Keep prompts concise to reduce token usage
5. **Model Selection**: Use faster models (GPT-3.5-turbo) for quick tasks

---

## Next Steps

1. ✅ Test basic skill endpoints
2. 🔧 Customize skills in `.claude/skills/` directory
3. 📊 Add logging and monitoring
4. 🔐 Implement authentication/authorization
5. 🚀 Deploy to production
6. 📱 Integrate with frontend applications
