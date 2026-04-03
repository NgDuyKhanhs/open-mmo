package com.openmmo.ai.controller

import com.openmmo.ai.service.AiAgentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Agent Skills Controller - Exposes agent capabilities via REST API
 * 
 * The controller provides endpoints that leverage registered Agent Skills
 * to perform intelligent tasks in the AI backend.
 */
@RestController
@RequestMapping("/api/v1/agent")
class AgentSkillsController(
    private val aiAgentService: AiAgentService
) {

    /**
     * POST /api/v1/agent/review-code
     * 
     * Reviews code using the code-reviewer skill
     * 
     * Request body:
     * {
     *   "filePath": "src/main/kotlin/com/openmmo/ai/service/MyService.kt"
     * }
     */
    @PostMapping("/review-code")
    fun reviewCode(@RequestBody request: ReviewCodeRequest): ResponseEntity<AgentResponse> {
        val review = aiAgentService.reviewCode(request.filePath)
        return ResponseEntity.ok(AgentResponse(
            success = true,
            skill = "code-reviewer",
            result = review
        ))
    }

    /**
     * POST /api/v1/agent/business-advice
     * 
     * Gets business recommendations using the business-advisor skill
     * 
     * Request body:
     * {
     *   "question": "How should we implement AI-driven player recommendations?"
     * }
     */
    @PostMapping("/business-advice")
    fun getBusinessAdvice(@RequestBody request: BusinessAdviceRequest): ResponseEntity<AgentResponse> {
        val advice = aiAgentService.getBusinessAdvice(request.question)
        return ResponseEntity.ok(AgentResponse(
            success = true,
            skill = "business-advisor",
            result = advice
        ))
    }

    /**
     * POST /api/v1/agent/mmo-expertise
     * 
     * Gets MMO gaming expertise using the mmo-expert skill
     * 
     * Request body:
     * {
     *   "question": "What are best practices for MMO economy balance?"
     * }
     */
    @PostMapping("/mmo-expertise")
    fun getMmoExpertise(@RequestBody request: MmoExpertiseRequest): ResponseEntity<AgentResponse> {
        val expertise = aiAgentService.getMmoExpertise(request.question)
        return ResponseEntity.ok(AgentResponse(
            success = true,
            skill = "mmo-expert",
            result = expertise
        ))
    }

    /**
     * POST /api/v1/agent/execute
     * 
     * Execute general agent task that may use multiple skills
     * 
     * Request body:
     * {
     *   "task": "Analyze this code and suggest MMO-specific optimizations"
     * }
     */
    @PostMapping("/execute")
    fun executeTask(@RequestBody request: ExecuteTaskRequest): ResponseEntity<AgentResponse> {
        val result = aiAgentService.executeAgentTask(request.task)
        return ResponseEntity.ok(AgentResponse(
            success = true,
            skill = "multi-skill",
            result = result
        ))
    }
}

// Request DTOs
data class ReviewCodeRequest(
    val filePath: String
)

data class BusinessAdviceRequest(
    val question: String
)

data class MmoExpertiseRequest(
    val question: String
)

data class ExecuteTaskRequest(
    val task: String
)

// Response DTO
data class AgentResponse(
    val success: Boolean,
    val skill: String,
    val result: String
)
