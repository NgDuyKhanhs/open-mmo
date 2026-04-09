package com.openmmo.ai.controller

import com.openmmo.ai.service.ICorrespondentMemoryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/gmail/memory")
class CorrespondentMemoryController(
    private val memoryService: ICorrespondentMemoryService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CorrespondentMemoryController::class.java)
    }

    /**
     * Forget one correspondent's memory
     */
    @DeleteMapping
    fun forgetCorrespondent(
        @RequestParam correspondentEmail: String,
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.info("Forgetting correspondent memory: userId=$userId, correspondent=$correspondentEmail")

        memoryService.forgetCorrespondent(userId, correspondentEmail)

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "Correspondent memory deleted"
        ))
    }

    /**
     * Forget all correspondent memories for user
     */
    @DeleteMapping("/all")
    fun forgetAll(
        authentication: Authentication
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.name
        logger.info("Forgetting all correspondent memories for user: $userId")

        memoryService.forgetAll(userId)

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "message" to "All correspondent memories deleted"
        ))
    }
}

