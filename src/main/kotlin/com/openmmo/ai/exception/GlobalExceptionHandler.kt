package com.openmmo.ai.exception

import com.openmmo.ai.dto.ApiResponse
import com.openmmo.ai.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException
import java.time.LocalDateTime

/**
 * Global Exception Handler
 * Centralized exception handling for all REST endpoints
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handle Gmail refresh token exceptions
     */
    @ExceptionHandler(GmailRefreshTokenException::class)
    fun handleGmailRefreshTokenException(
        ex: GmailRefreshTokenException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Gmail refresh token exception: ${ex.message}")

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = ex.message ?: "Gmail refresh token is invalid",
                error = ex.code,
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.UNAUTHORIZED
        )
    }

    /**
     * Handle custom API exceptions
     */
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("API Exception: ${ex.code} - ${ex.message}")

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = ex.message ?: "An error occurred",
                error = ex.code,
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            ex.status
        )
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation error: ${ex.message}")

        val errors = ex.bindingResult.allErrors
            .associate { error ->
                val fieldName = if (error is FieldError) error.field else error.objectName
                fieldName to (error.defaultMessage ?: "Invalid value")
            }

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = "Validation failed",
                error = "VALIDATION_ERROR",
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(
        ex: NoHandlerFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Endpoint not found: ${ex.requestURL}")

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = "Endpoint not found",
                error = "NOT_FOUND",
                path = ex.requestURL,
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.NOT_FOUND
        )
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception: ${ex::class.java.simpleName}", ex)

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = "An unexpected error occurred",
                error = "INTERNAL_ERROR",
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message}")

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = ex.message ?: "Invalid argument",
                error = "BAD_REQUEST",
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.BAD_REQUEST
        )
    }

    /**
     * Handle IllegalStateException
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal state: ${ex.message}")

        return ResponseEntity(
            ErrorResponse(
                success = false,
                message = ex.message ?: "Invalid state",
                error = "CONFLICT",
                path = request.getDescription(false).replace("uri=", ""),
                timestamp = System.currentTimeMillis()
            ),
            HttpStatus.CONFLICT
        )
    }
}

