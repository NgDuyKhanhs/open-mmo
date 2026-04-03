package com.openmmo.ai.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime

/**
 * User Entity for MongoDB
 * Represents an AI backend user with authentication and OAuth support
 */
@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,

    @Indexed(unique = true)
    val email: String,

    @Indexed(unique = true)
    val username: String,

    // Password hashed with BCrypt
    var password: String? = null,

    // OAuth2 Google ID
    var googleId: String? = null,

    // User profile information
    var firstName: String? = null,
    var lastName: String? = null,
    var avatar: String? = null,

    // Email verification
    var emailVerified: Boolean = false,
    var emailVerificationToken: String? = null,
    var emailVerificationTokenExpiry: LocalDateTime? = null,

    // Password reset
    var passwordResetToken: String? = null,
    var passwordResetTokenExpiry: LocalDateTime? = null,

    // Roles - stored as list of role names (ADMIN, USER, MODERATOR)
    var roles: List<String> = listOf("USER"),

    // Account status
    var isActive: Boolean = true,
    var isLocked: Boolean = false,
    var lockUntil: LocalDateTime? = null,
    var failedLoginAttempts: Int = 0,

    // Timestamps
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var lastLoginAt: LocalDateTime? = null,

    // Additional metadata
    var loginProvider: String? = null, // "local", "google", etc.
    var metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Update last login timestamp
     */
    fun updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
        this.failedLoginAttempts = 0
        this.isLocked = false
        this.lockUntil = null
    }

    /**
     * Increment failed login attempts
     */
    fun incrementFailedLoginAttempts(): Int {
        this.failedLoginAttempts++
        if (this.failedLoginAttempts >= 5) {
            this.isLocked = true
            this.lockUntil = LocalDateTime.now().plusHours(1)
        }
        this.updatedAt = LocalDateTime.now()
        return this.failedLoginAttempts
    }

    /**
     * Reset failed login attempts
     */
    fun resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0
        this.isLocked = false
        this.lockUntil = null
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Check if account is locked
     */
    fun isAccountLocked(): Boolean {
        return isLocked && (lockUntil == null || LocalDateTime.now().isBefore(lockUntil))
    }

    /**
     * Unlock account
     */
    fun unlockAccount() {
        this.isLocked = false
        this.lockUntil = null
        this.failedLoginAttempts = 0
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Verify email
     */
    fun verifyEmail() {
        this.emailVerified = true
        this.emailVerificationToken = null
        this.emailVerificationTokenExpiry = null
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Add role to user
     */
    fun addRole(role: String) {
        if (!roles.contains(role)) {
            this.roles = roles + role
            this.updatedAt = LocalDateTime.now()
        }
    }

    /**
     * Remove role from user
     */
    fun removeRole(role: String) {
        this.roles = roles.filter { it != role }
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Check if user has role
     */
    fun hasRole(role: String): Boolean {
        return roles.contains(role)
    }

    /**
     * Check if user is admin
     */
    fun isAdmin(): Boolean {
        return hasRole("ADMIN")
    }

    /**
     * Convert user entity to response DTO
     */
    fun toUserResponse(): com.openmmo.ai.dto.UserResponse {
        return com.openmmo.ai.dto.UserResponse(
            id = id ?: "",
            email = email,
            username = username,
            firstName = firstName,
            lastName = lastName,
            avatar = avatar,
            emailVerified = emailVerified,
            roles = roles,
            isActive = isActive,
            createdAt = createdAt.toString(),
            lastLoginAt = lastLoginAt?.toString()
        )
    }
}
