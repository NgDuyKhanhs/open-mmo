package com.openmmo.ai.controller

import com.openmmo.ai.dto.*
import com.openmmo.ai.service.IAuthenticationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory
import jakarta.servlet.http.HttpServletResponse

/**
 * Authentication Controller
 * REST endpoints for user authentication and authorization
 * CORS is handled globally by CorsConfig
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authenticationService: IAuthenticationService
) {

    private val logger = LoggerFactory.getLogger(AuthenticationController::class.java)

    companion object {
        private const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        private const val REFRESH_TOKEN_COOKIE_MAX_AGE = 604800 // 7 days in seconds
    }

    /**
     * Set refresh token as HttpOnly, Secure, SameSite cookie
     */
    private fun setRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val cookieValue = "$REFRESH_TOKEN_COOKIE_NAME=$refreshToken; " +
                "Max-Age=$REFRESH_TOKEN_COOKIE_MAX_AGE; " +
                "Path=/; " +
                "HttpOnly; " +
                "SameSite=Lax"

        // For HTTPS in production, add: Secure;
        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue)
        logger.debug("✅ Set HttpOnly refresh token cookie")
    }

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
    fun login(
        @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        logger.info("User login request for: ${request.emailOrUsername}")
        var authResponse = authenticationService.login(request)

        // Set refresh token in HttpOnly cookie if login successful
        if (authResponse.success && !authResponse.refreshToken.isNullOrBlank()) {
            setRefreshTokenCookie(response, authResponse.refreshToken ?: "")
            // Remove refreshToken from response body for security
            authResponse = authResponse.copy(refreshToken = null)
        }

        return ResponseEntity(authResponse, if (authResponse.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    /**
     * Google login endpoint
     * POST /api/v1/auth/google-login
     */
    @PostMapping("/google-login")
    fun googleLogin(
        @RequestBody request: GoogleLoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        logger.info("Google login request")
        var authResponse = authenticationService.googleLogin(request)

        // Set refresh token in HttpOnly cookie if login successful
        if (authResponse.success && !authResponse.refreshToken.isNullOrBlank()) {
            setRefreshTokenCookie(response, authResponse.refreshToken ?: "")
            // Remove refreshToken from response body for security
            authResponse = authResponse.copy(refreshToken = null)
        }

        return ResponseEntity(authResponse, if (authResponse.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
    }

    /**
     * Refresh token endpoint
     * POST /api/v1/auth/refresh-token
     *
     * Frontend can either:
     * 1. Send refreshToken in cookie (auto sent by browser)
     * 2. Send refreshToken in request body for manual refresh
     */
    @PostMapping("/refresh-token")
    fun refreshToken(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        @CookieValue(REFRESH_TOKEN_COOKIE_NAME, required = false) refreshTokenCookie: String?,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        logger.info("Token refresh request")

        // Use token from cookie or request body
        val refreshToken = refreshTokenCookie ?: request?.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity(
                AuthResponse(success = false, message = "No refresh token provided"),
                HttpStatus.UNAUTHORIZED
            )
        }

        val authResponse = authenticationService.refreshToken(RefreshTokenRequest(refreshToken))

        // Set new refresh token in HttpOnly cookie if refresh successful
        if (authResponse.success && !authResponse.refreshToken.isNullOrBlank()) {
            setRefreshTokenCookie(response, authResponse.refreshToken ?: "")
            // Remove refreshToken from response body for security
            return ResponseEntity(authResponse.copy(refreshToken = null), HttpStatus.OK)
        }

        return ResponseEntity(authResponse, if (authResponse.success) HttpStatus.OK else HttpStatus.UNAUTHORIZED)
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
    fun logout(
        authentication: Authentication,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val userId = authentication.principal as? String
        logger.info("User logout request for: $userId")
        
        // Clear refresh token cookie
        val clearCookie = "$REFRESH_TOKEN_COOKIE_NAME=; " +
                "Max-Age=0; " +
                "Path=/; " +
                "HttpOnly; " +
                "SameSite=Lax"
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie)
        logger.debug("✅ Cleared refresh token cookie")

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
