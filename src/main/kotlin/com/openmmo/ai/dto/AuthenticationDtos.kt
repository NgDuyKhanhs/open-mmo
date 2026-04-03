package com.openmmo.ai.dto

/**
 * Registration Request DTO
 */
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val confirmPassword: String,
    val firstName: String? = null,
    val lastName: String? = null
) {
    fun validate(): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            username.isBlank() -> "Tên người dùng không được để trống"
            username.length < 3 -> "Tên người dùng phải ít nhất 3 ký tự"
            password.isBlank() -> "Mật khẩu không được để trống"
            password.length < 6 -> "Mật khẩu phải ít nhất 6 ký tự"
            password != confirmPassword -> "Mật khẩu không khớp"
            !email.contains("@") -> "Email không hợp lệ"
            else -> null
        }
    }
}

/**
 * Login Request DTO
 */
data class LoginRequest(
    val emailOrUsername: String,
    val password: String
) {
    fun validate(): String? {
        return when {
            emailOrUsername.isBlank() -> "Email hoặc tên người dùng không được để trống"
            password.isBlank() -> "Mật khẩu không được để trống"
            else -> null
        }
    }
}

/**
 * Google Login Request DTO
 */
data class GoogleLoginRequest(
    val idToken: String,
    val accessToken: String? = null
) {
    fun validate(): String? {
        return when {
            idToken.isBlank() -> "ID token không được để trống"
            else -> null
        }
    }
}

/**
 * Refresh Token Request DTO
 */
data class RefreshTokenRequest(
    val refreshToken: String
) {
    fun validate(): String? {
        return when {
            refreshToken.isBlank() -> "Refresh token không được để trống"
            else -> null
        }
    }
}

/**
 * Change Password Request DTO
 */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
) {
    fun validate(): String? {
        return when {
            currentPassword.isBlank() -> "Mật khẩu hiện tại không được để trống"
            newPassword.isBlank() -> "Mật khẩu mới không được để trống"
            newPassword.length < 6 -> "Mật khẩu mới phải ít nhất 6 ký tự"
            newPassword != confirmPassword -> "Mật khẩu mới không khớp"
            currentPassword == newPassword -> "Mật khẩu mới phải khác mật khẩu hiện tại"
            else -> null
        }
    }
}

/**
 * Verify Email Request DTO
 */
data class VerifyEmailRequest(
    val token: String
) {
    fun validate(): String? {
        return when {
            token.isBlank() -> "Token không được để trống"
            else -> null
        }
    }
}

/**
 * Forgot Password Request DTO
 */
data class ForgotPasswordRequest(
    val email: String
) {
    fun validate(): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            !email.contains("@") -> "Email không hợp lệ"
            else -> null
        }
    }
}

/**
 * Reset Password Request DTO
 */
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
    val confirmPassword: String
) {
    fun validate(): String? {
        return when {
            token.isBlank() -> "Token không được để trống"
            newPassword.isBlank() -> "Mật khẩu mới không được để trống"
            newPassword.length < 6 -> "Mật khẩu mới phải ít nhất 6 ký tự"
            newPassword != confirmPassword -> "Mật khẩu mới không khớp"
            else -> null
        }
    }
}

/**
 * Authentication Response DTO
 */
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: UserResponse? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null
)

/**
 * User Response DTO
 */
data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val avatar: String?,
    val emailVerified: Boolean,
    val roles: List<String>,
    val isActive: Boolean,
    val createdAt: String,
    val lastLoginAt: String?
)

/**
 * Token Response DTO
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)

/**
 * Error Response DTO
 */
data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val error: String? = null,
    val path: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
