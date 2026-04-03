# Agent Skills Integration Guide - OpenMMO AI Backend

## Overview

Agent Skills have been integrated into the OpenMMO AI backend using **Spring AI Agent Utils**. This enables the backend to leverage modular, reusable capabilities for:

- **Code Review**: Analyze Java/Kotlin code for best practices and security
- **Business Strategy**: Generate AI-driven business recommendations
- **MMO Expertise**: Specialized knowledge for game mechanics and economy

## Architecture

### Components

1. **SkillsTool** - Discovers and loads skill definitions from `.claude/skills/` directory
2. **FileSystemTools** - Allows agents to read reference files and code
3. **ShellTools** - Executes helper scripts for data processing
4. **ChatClient Bean** - Configured in `AiApplication.kt` with all tools

### Skill Directory Structure

```
.claude/skills/
├── code-reviewer/
│   ├── SKILL.md
│   └── scripts/
├── business-advisor/
│   ├── SKILL.md
│   └── scripts/
└── mmo-expert/
    ├── SKILL.md
    └── references/
```

## Skills Description

### 1. code-reviewer
**Purpose**: Reviews Kotlin/Java code for Spring Boot best practices

**Use Cases**:
- Analyze controller classes
- Review service implementations
- Security vulnerability assessment
- Performance optimization suggestions

**Capabilities**:
- Spring Boot best practices
- Kotlin null-safety checks
- Security analysis
- Performance profiling
- Code quality metrics

### 2. business-advisor
**Purpose**: Strategic business recommendations for AI solutions

**Use Cases**:
- AI feature prioritization
- Business ROI analysis
- Implementation strategy
- Market analysis

**Capabilities**:
- Market opportunity analysis
- AI implementation roadmaps
- Risk assessment
- Success metrics definition
- Resource planning

### 3. mmo-expert
**Purpose**: Specialized MMO game design expertise

**Use Cases**:
- Game mechanic optimization
- Economy system design
- Player engagement strategy
- Performance tuning

**Capabilities**:
- Game mechanics knowledge
- Player psychology
- Economy systems
- Community management
- Technical MMO architecture

## How to Use

### Via REST API

#### Code Review

```bash
curl -X POST http://localhost:8080/api/v1/agent/review-code \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "src/main/kotlin/com/openmmo/ai/service/AiAgentService.kt"
  }'
```

#### Business Advice

```bash
curl -X POST http://localhost:8080/api/v1/agent/business-advice \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How should we implement AI-driven player recommendations in the MMO?"
  }'
```

#### MMO Expertise

```bash
curl -X POST http://localhost:8080/api/v1/agent/mmo-expertise \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are best practices for designing a balanced MMO economy?"
  }'
```

#### Multi-Skill Task

```bash
curl -X POST http://localhost:8080/api/v1/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Analyze the PaymentService.kt controller and suggest MMO-specific optimizations for handling player transactions"
  }'
```

### Programmatic Usage

```kotlin
@Autowired
private lateinit var aiAgentService: AiAgentService

// Review code
val review = aiAgentService.reviewCode("src/main/kotlin/MyService.kt")

// Get business advice
val advice = aiAgentService.getBusinessAdvice("How to monetize AI features?")

// Get MMO expertise
val mmoTips = aiAgentService.getMmoExpertise("Best raid system design?")

// Execute combined task
val result = aiAgentService.executeAgentTask(
    "Analyze and optimize player matching algorithm with MMO best practices"
)
```

## Configuration

### Environment Variables

```bash
# Required: OpenAI API Key (or other LLM provider)
export OPENAI_API_KEY=sk-...

# Optional: Customize LLM model (defaults to gpt-4-turbo)
export SPRING_AI_OPENAI_CHAT_MODEL=gpt-4-turbo
```

### Application Properties

See `application.yaml` for Spring AI configuration:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      # chat:
      #   model: gpt-4-turbo
      #   temperature: 0.7
```

## Creating New Skills

To add a new skill:

1. **Create Directory**:
   ```bash
   mkdir -p .claude/skills/new-skill/scripts
   ```

2. **Create SKILL.md** with YAML frontmatter:
   ```markdown
   ---
   name: new-skill
   description: Brief description of what the skill does
   ---
   
   # Skill Name
   
   ## Instructions
   
   Step-by-step instructions for the AI...
   ```

3. **Add Helper Scripts** (optional):
   - Place Python, bash, or other scripts in `scripts/` folder
   - AI can execute them with ShellTools

4. **Reference Files** (optional):
   - Create `references/` folder for documentation
   - AI can read them with FileSystemTools

5. **Restart Application**: Changes are picked up at startup

## How Skills Work

### Discovery (Startup)
- `SkillsTool` scans `.claude/skills/` directory
- Parses `SKILL.md` frontmatter (name + description)
- Builds lightweight registry in tool description

### Semantic Matching (Runtime)
- User makes request to agent
- LLM examines skill descriptions
- Automatically selects relevant skills

### Execution (When Matched)
- AI loads full SKILL.md content
- Follows instructions in skill file
- Can load references or execute scripts as needed

## Security Considerations

⚠️ **Important**: Scripts execute on your local machine without sandboxing!

- Always review skill scripts before use
- Pre-install required runtimes (Python, Node.js, etc.)
- Consider running in container for production
- Restrict file system access as needed

## LLM Portability

This implementation is **LLM-agnostic** and works with:
- ✅ OpenAI (GPT-4, GPT-4-turbo)
- ✅ Anthropic (Claude 3, Claude 3.5)
- ✅ Google Gemini
- ✅ Other Spring AI supported models

**No vendor lock-in** - Switch LLM providers without changing skill code!

## Related Dependencies

- `org.springframework.ai:spring-ai-core:1.0.0-M2`
- `org.springframework.ai:spring-ai-openai:1.0.0-M2`
- `org.springaicommunity:spring-ai-agent-utils:0.4.2`

## Resources

- 📖 [Spring AI Agent Utils Documentation](https://github.com/spring-ai-community/spring-ai-agent-utils)
- 🎯 [Agent Skills Specification](https://agentskills.io/specification)
- 🏗️ [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- 💡 [Spring Blog: Agent Skills](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills)

## Development Tips

1. **Test Skills Incrementally**: Start with simple tasks before complex ones
2. **Monitor Token Usage**: Large code reviews consume more tokens
3. **Cache Results**: Implement caching for repeated tasks
4. **Error Handling**: LLM responses should be validated and sanitized
5. **Logging**: Enable debug logging to see skill invocations:
   ```yaml
   logging:
     level:
       org.springaicommunity: DEBUG
   ```

## Next Steps

1. Set up your OpenAI API key
2. Test endpoints with sample requests
3. Customize skills based on your needs
4. Create domain-specific skills for your use cases
5. Integrate with frontend applications
