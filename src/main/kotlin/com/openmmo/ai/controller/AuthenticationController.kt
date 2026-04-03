package com.openmmo.ai.controller

import com.openmmo.ai.dto.*
import com.openmmo.ai.service.AuthenticationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

/**
 * Authentication Controller
 * REST endpoints for user authentication and authorization
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = ["*"], maxAge = 3600)
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {

    private val logger = LoggerFactory.getLogger(AuthenticationController::class.java)

    /**
     * User registration endpoint
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        logger.info("User registration request for: ${request.email}")
        val response = authenticationService.register(request)
        return ResponseEntity(response, if (response.success) HttpStatus.CREATED else HttpStatus.BAD_REQUEST)
    }

    /**
     * User login endpoint
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        logger.info("User login request for: ${request.emailOrUsername}")
        val response = authenticationService.login(request)
        return ResponseEntity(response, if (response.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    /**
     * Google login endpoint
     * POST /api/v1/auth/google-login
     */
    @PostMapping("/google-login")
    fun googleLogin(@RequestBody request: GoogleLoginRequest): ResponseEntity<AuthResponse> {
        logger.info("Google login request")
        val response = authenticationService.googleLogin(request)
        return ResponseEntity(response, if (response.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    /**
     * Refresh token endpoint
     * POST /api/v1/auth/refresh-token
     */
    @PostMapping("/refresh-token")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        logger.info("Token refresh request")
        val response = authenticationService.refreshToken(request)
        return ResponseEntity(response, if (response.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    /**
     * Change password endpoint
     * POST /api/v1/auth/change-password
     */
    @PostMapping("/change-password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
        authentication: Authentication
    ): ResponseEntity<AuthResponse> {
        val userId = authentication.principal as? String
        if (userId == null) {
            return ResponseEntity(
                AuthResponse(
                    success = false,
                    message = "Không xác định được người dùng"
                ),
                HttpStatus.UNAUTHORIZED
            )
        }

        logger.info("Change password request for user: $userId")
        val response = authenticationService.changePassword(userId, request)
        return ResponseEntity(response, if (response.success) HttpStatus.OK else HttpStatus.BAD_REQUEST)
    }

    /**
     * Get current user profile
     * GET /api/v1/auth/profile
     */
    @GetMapping("/profile")
    fun getCurrentUserProfile(authentication: Authentication): ResponseEntity<Any> {
        val userId = authentication.principal as? String
        if (userId == null) {
            return ResponseEntity(
                ErrorResponse(
                    success = false,
                    message = "Không xác định được người dùng"
                ),
                HttpStatus.UNAUTHORIZED
            )
        }

        val user = authenticationService.getUserById(userId)
        if (user == null) {
            return ResponseEntity(
                ErrorResponse(
                    success = false,
                    message = "Người dùng không tồn tại"
                ),
                HttpStatus.NOT_FOUND
            )
        }

        logger.info("Retrieved profile for user: $userId")
        return ResponseEntity(
            ApiResponse(
                success = true,
                message = "Lấy thông tin người dùng thành công",
                data = user
            ),
            HttpStatus.OK
        )
    }

    /**
     * Logout endpoint (client-side, but provided for API completeness)
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    fun logout(authentication: Authentication): ResponseEntity<AuthResponse> {
        val userId = authentication.principal as? String
        logger.info("User logout request for: $userId")
        
        // In JWT-based systems, logout is typically handled by client side
        // (client deletes tokens). This endpoint is provided for completeness.
        return ResponseEntity(
            AuthResponse(
                success = true,
                message = "Đăng xuất thành công"
            ),
            HttpStatus.OK
        )
    }

    /**
     * Verify email endpoint
     * POST /api/v1/auth/verify-email
     */
    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest): ResponseEntity<AuthResponse> {
        logger.info("Email verification request")
        // TODO: Implement email verification logic
        return ResponseEntity(
            AuthResponse(
                success = false,
                message = "Email verification not yet implemented"
            ),
            HttpStatus.NOT_IMPLEMENTED
        )
    }

    /**
     * Forgot password endpoint
     * POST /api/v1/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<AuthResponse> {
        logger.info("Forgot password request for: ${request.email}")
        // TODO: Implement password reset email sending
        return ResponseEntity(
            AuthResponse(
                success = false,
                message = "Forgot password feature not yet implemented"
            ),
            HttpStatus.NOT_IMPLEMENTED
        )
    }

    /**
     * Reset password endpoint
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<AuthResponse> {
        logger.info("Reset password request")
        // TODO: Implement password reset logic
        return ResponseEntity(
            AuthResponse(
                success = false,
                message = "Reset password feature not yet implemented"
            ),
            HttpStatus.NOT_IMPLEMENTED
        )
    }

    /**
     * Health check for authentication service
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Any> {
        return ResponseEntity(
            mapOf(
                "status" to "UP",
                "service" to "Authentication Service",
                "timestamp" to System.currentTimeMillis()
            ),
            HttpStatus.OK
        )
    }
}
