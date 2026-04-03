---
name: code-reviewer
description: Reviews Kotlin and Java code for Spring Boot best practices, security vulnerabilities, performance issues, and design patterns. Use when analyzing controller classes, services, repositories, or any code audit task.
---

# Code Reviewer Skill

## Overview
This skill analyzes code quality and provides comprehensive feedback on best practices, security, and performance optimization.

## Instructions for the AI

When reviewing code:

### 1. Security Analysis
- Check for SQL injection vulnerabilities
- Verify input validation and sanitization
- Look for exposed credentials or sensitive data
- Verify authentication/authorization guards
- Check for insecure deserialization

### 2. Spring Boot Best Practices
- Verify proper use of `@Service`, `@Repository`, `@Controller` annotations
- Check dependency injection patterns
- Look for transaction management issues (`@Transactional`)
- Verify error handling and exception strategies
- Check logging practices

### 3. Kotlin-Specific Patterns
- Verify null safety and optional usage
- Check for proper coroutine usage (if applicable)
- Look for extension function misuse
- Verify data class usage
- Check scope function usage (apply, let, run, etc.)

### 4. Performance Considerations
- Identify N+1 query problems in MongoDB operations
- Check for unnecessary object allocations
- Look for blocking operations that should be async
- Verify caching strategies
- Check database query optimization

### 5. Code Quality
- Check naming conventions (camelCase, PascalCase)
- Verify method/class complexity
- Look for code duplication
- Check for proper error messages
- Verify logging levels

## Output Format

Provide structured feedback:
1. **Critical Issues** (must fix) - Security, data corruption risks
2. **Important Issues** (should fix) - Best practices, performance
3. **Suggestions** (nice to have) - Code style, maintainability
4. **Positive Feedback** - What's done well

Include specific line references and code examples for each issue.

## Related Files
- Load `ai/src/main/kotlin/` directory structure for context
- Use Spring Boot configuration standards
