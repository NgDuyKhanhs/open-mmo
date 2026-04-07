package com.openmmo.ai.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.openmmo.ai.dto.*
import com.openmmo.ai.entity.User
import com.openmmo.ai.repository.UserRepository
import com.openmmo.ai.util.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * Authentication Service
 * Handles user registration, login, OAuth, and token management
 */
@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${oauth2.google.client-id}") private val googleClientId: String
) {

    private val logger = LoggerFactory.getLogger(AuthenticationService::class.java)
    
    // Google OAuth Configuration - Built lazily
    private val GOOGLE_ID_TOKEN_VERIFIER: GoogleIdTokenVerifier?
        get() {
            if (googleClientId.isBlank()) {
                logger.error("GOOGLE_CLIENT_ID is not configured. Please set the GOOGLE_CLIENT_ID environment variable.")
                return null
            }
            return try {
                GoogleIdTokenVerifier.Builder(
                    NetHttpTransport(),
                    GsonFactory()
                ).setAudience(Collections.singletonList(googleClientId)).build()
            } catch (ex: Exception) {
                logger.error("Failed to initialize Google ID Token Verifier", ex)
                null
            }
        }

    /**
     * Register new user with email and password
     */
    fun register(request: RegisterRequest): AuthResponse {
        // Validate input
        val validationError = request.validate()
        if (validationError != null) {
            return AuthResponse(
                success = false,
                message = validationError
            )
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            return AuthResponse(
                success = false,
                message = "Email đã được sử dụng"
            )
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.username)) {
            return AuthResponse(
                success = false,
                message = "Tên người dùng đã tồn tại"
            )
        }

        // Create new user
        val user = User(
            email = request.email,
            username = request.username,
            password = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            roles = listOf("USER"),
            loginProvider = "local"
        )

        try {
            val savedUser = userRepository.save(user)
            logger.info("New user registered: ${savedUser.email}")

            // Generate tokens
            val accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.id!!, 
                savedUser.email,
                savedUser.roles
            )
            val refreshToken = jwtTokenProvider.generateRefreshToken(
                savedUser.id, 
                savedUser.email
            )

            return AuthResponse(
                success = true,
                message = "Đăng ký thành công",
                user = savedUser.toUserResponse(),
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = 86400000 // 24 hours
            )
        } catch (ex: Exception) {
            logger.error("Error during user registration", ex)
            return AuthResponse(
                success = false,
                message = "Lỗi khi đăng ký: ${ex.message}"
            )
        }
    }

    /**
     * Login user with email/username and password
     */
    fun login(request: LoginRequest): AuthResponse {
        // Validate input
        val validationError = request.validate()
        if (validationError != null) {
            return AuthResponse(
                success = false,
                message = validationError
            )
        }

        // Find user by email or username
        val userOptional = userRepository.findByEmailOrUsername(request.emailOrUsername)
        if (userOptional.isEmpty) {
            logger.warn("Login attempt with non-existent user: ${request.emailOrUsername}")
            return AuthResponse(
                success = false,
                message = "Email hoặc tên người dùng không tồn tại"
            )
        }

        val user = userOptional.get()

        // Check if account is locked
        if (user.isAccountLocked()) {
            return AuthResponse(
                success = false,
                message = "Tài khoản bị khóa. Vui lòng thử lại sau"
            )
        }

        // Check if account is active
        if (!user.isActive) {
            return AuthResponse(
                success = false,
                message = "Tài khoản không hoạt động"
            )
        }

        // Validate password
        if (!passwordEncoder.matches(request.password, user.password)) {
            user.incrementFailedLoginAttempts()
            userRepository.save(user)
            
            logger.warn("Failed login attempt for user: ${user.email}")
            return AuthResponse(
                success = false,
                message = "Mật khẩu không đúng"
            )
        }

        // Update last login and reset failed attempts
        user.updateLastLogin()
        userRepository.save(user)

        // Generate tokens
        val accessToken = jwtTokenProvider.generateAccessToken(
            user.id!!, 
            user.email,
            user.roles
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(
            user.id, 
            user.email
        )

        logger.info("User logged in successfully: ${user.email}")

        return AuthResponse(
            success = true,
            message = "Đăng nhập thành công",
            user = user.toUserResponse(),
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 86400000
        )
    }

    /**
     * Login with Google OAuth token
     * Verifies token and creates/updates user
     */
    fun googleLogin(request: GoogleLoginRequest): AuthResponse {
        // Validate input
        val validationError = request.validate()
        if (validationError != null) {
            return AuthResponse(
                success = false,
                message = validationError
            )
        }

        try {
            logger.info("Processing Google login with token")
            
            // Check if Google Client ID is configured
            if (googleClientId.isBlank()) {
                logger.error("GOOGLE_CLIENT_ID is not configured. Please set the GOOGLE_CLIENT_ID environment variable.")
                return AuthResponse(
                    success = false,
                    message = "Google OAuth không được cấu hình. Vui lòng liên hệ với quản trị viên hệ thống."
                )
            }

            // Verify Google ID Token using Google's official verifier
            val verifier = GOOGLE_ID_TOKEN_VERIFIER
            if (verifier == null) {
                logger.error("Failed to initialize Google ID Token Verifier")
                return AuthResponse(
                    success = false,
                    message = "Không thể xác thực với Google. Vui lòng thử lại sau."
                )
            }

            val idToken = verifier.verify(request.idToken)
            if (idToken == null) {
                logger.error("Failed to verify Google token - token verification returned null. Client ID: $googleClientId")
                return AuthResponse(
                    success = false,
                    message = "Google token không hợp lệ hoặc Client ID không khớp"
                )
            }

            val payload = idToken.payload
            val email = payload.email ?: ""
            val googleId = payload.subject
            val firstName = payload["given_name"]?.toString()
            val lastName = payload["family_name"]?.toString()
            val picture = payload["picture"]?.toString()

            if (email.isBlank() || googleId.isBlank()) {
                logger.error("Google token missing required fields: email=$email, googleId=$googleId")
                return AuthResponse(
                    success = false,
                    message = "Token Google không chứa thông tin cần thiết"
                )
            }

            // Find or create user
            logger.info("Looking for user with email: $email")
            val userOptional = userRepository.findByEmail(email)
            
            val user = if (userOptional.isPresent) {
                // Update existing user
                logger.info("User found, updating login info")
                val existingUser = userOptional.get()
                existingUser.updateLastLogin()
                if (firstName != null) existingUser.firstName = firstName
                if (lastName != null) existingUser.lastName = lastName
                if (picture != null) existingUser.avatar = picture
                if (existingUser.googleId.isNullOrBlank()) {
                    existingUser.googleId = googleId
                }
                existingUser.emailVerified = true
                existingUser.loginProvider = "google"
                userRepository.save(existingUser)
                existingUser
            } else {
                // Create new user
                logger.info("Creating new user for email: $email")
                val newUser = User(
                    email = email,
                    username = email.split("@")[0] + "_" + UUID.randomUUID().toString().take(4),
                    password = null,
                    googleId = googleId,
                    firstName = firstName,
                    lastName = lastName,
                    avatar = picture,
                    emailVerified = true,
                    roles = listOf("USER"),
                    isActive = true,
                    loginProvider = "google"
                )
                userRepository.save(newUser)
                newUser
            }

            // Generate application JWT tokens
            val accessToken = jwtTokenProvider.generateAccessToken(
                user.id!!, 
                user.email,
                user.roles
            )
            val refreshToken = jwtTokenProvider.generateRefreshToken(
                user.id, 
                user.email
            )

            logger.info("Google login successful for user: ${user.email}")

            return AuthResponse(
                success = true,
                message = "Đăng nhập bằng Google thành công",
                user = user.toUserResponse(),
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = 86400000
            )
        } catch (ex: Exception) {
            logger.error("Error during Google login", ex)
            return AuthResponse(
                success = false,
                message = "Lỗi khi đăng nhập bằng Google: ${ex.message}"
            )
        }
    }

    /**
     * Refresh access token using refresh token
     */
    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        // Validate input
        val validationError = request.validate()
        if (validationError != null) {
            return AuthResponse(
                success = false,
                message = validationError
            )
        }

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            return AuthResponse(
                success = false,
                message = "Refresh token không hợp lệ"
            )
        }

        // Extract user info from token
        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        val email = jwtTokenProvider.getEmailFromToken(request.refreshToken)

        if (userId == null || email == null) {
            return AuthResponse(
                success = false,
                message = "Token không chứa thông tin người dùng"
            )
        }

        // Find user
        val userOptional = userRepository.findById(userId)
        if (userOptional.isEmpty) {
            return AuthResponse(
                success = false,
                message = "Người dùng không tồn tại"
            )
        }

        val user = userOptional.get()

        // Generate new access token
        val newAccessToken = jwtTokenProvider.generateAccessToken(
            user.id!!, 
            user.email,
            user.roles
        )

        logger.info("Token refreshed for user: ${user.email}")

        return AuthResponse(
            success = true,
            message = "Token làm mới thành công",
            accessToken = newAccessToken,
            expiresIn = 86400000
        )
    }

    /**
     * Change password for authenticated user
     */
    fun changePassword(userId: String, request: ChangePasswordRequest): AuthResponse {
        // Validate input
        val validationError = request.validate()
        if (validationError != null) {
            return AuthResponse(
                success = false,
                message = validationError
            )
        }

        // Find user
        val userOptional = userRepository.findById(userId)
        if (userOptional.isEmpty) {
            return AuthResponse(
                success = false,
                message = "Người dùng không tồn tại"
            )
        }

        val user = userOptional.get()

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            return AuthResponse(
                success = false,
                message = "Mật khẩu hiện tại không đúng"
            )
        }

        // Update password
        user.password = passwordEncoder.encode(request.newPassword)
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        logger.info("Password changed for user: ${user.email}")

        return AuthResponse(
            success = true,
            message = "Đổi mật khẩu thành công"
        )
    }

    /**
     * Get user by ID
     */
    fun getUserById(userId: String): User? {
        return userRepository.findById(userId).orElse(null)
    }

    /**
     * Get user by email
     */
    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email).orElse(null)
    }
}

/**
 * Extension function to convert User entity to UserResponse DTO
 */
fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id!!,
        email = this.email,
        username = this.username,
        firstName = this.firstName,
        lastName = this.lastName,
        avatar = this.avatar,
        emailVerified = this.emailVerified,
        roles = this.roles,
        isActive = this.isActive,
        createdAt = this.createdAt.toString(),
        lastLoginAt = this.lastLoginAt?.toString()
    )
}
