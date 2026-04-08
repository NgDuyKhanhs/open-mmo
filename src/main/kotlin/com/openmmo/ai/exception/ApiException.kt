package com.openmmo.ai.exception

import org.springframework.http.HttpStatus

/**
 * Custom exception for API errors
 * Maps to HTTP status codes
 */
open class ApiException(
    message: String,
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val code: String = "INTERNAL_ERROR",
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Authentication related exceptions
 */
class AuthenticationException(
    message: String,
    code: String = "AUTH_ERROR",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.UNAUTHORIZED, code, cause)

/**
 * Authorization related exceptions
 */
class AuthorizationException(
    message: String,
    code: String = "FORBIDDEN_ERROR",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.FORBIDDEN, code, cause)

/**
 * Resource not found exceptions
 */
class ResourceNotFoundException(
    message: String,
    code: String = "NOT_FOUND",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.NOT_FOUND, code, cause)

/**
 * Bad request exceptions (validation errors)
 */
class BadRequestException(
    message: String,
    code: String = "BAD_REQUEST",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.BAD_REQUEST, code, cause)

/**
 * Conflict exceptions (duplicate data, etc)
 */
class ConflictException(
    message: String,
    code: String = "CONFLICT",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.CONFLICT, code, cause)

/**
 * Upstream service exceptions (Gmail, Gemini, etc)
 */
class UpstreamException(
    message: String,
    code: String = "UPSTREAM_ERROR",
    cause: Throwable? = null
) : ApiException(message, HttpStatus.BAD_GATEWAY, code, cause)

