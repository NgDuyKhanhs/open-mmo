package com.openmmo.ai.repository

import com.openmmo.ai.entity.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * User Repository
 * Handles database operations for User entities in MongoDB
 */
@Repository
interface UserRepository : MongoRepository<User, String> {

    /**
     * Find user by email
     */
    fun findByEmail(email: String): Optional<User>

    /**
     * Find user by username
     */
    fun findByUsername(username: String): Optional<User>

    /**
     * Find user by email or username
     */
    @Query("{ \$or: [ { email: ?0 }, { username: ?0 } ] }")
    fun findByEmailOrUsername(emailOrUsername: String): Optional<User>

    /**
     * Find user by Google ID (for OAuth)
     */
    fun findByGoogleId(googleId: String): Optional<User>

    /**
     * Check if email exists
     */
    fun existsByEmail(email: String): Boolean

    /**
     * Check if username exists
     */
    fun existsByUsername(username: String): Boolean

    /**
     * Find all active users
     */
    @Query("{ isActive: true }")
    fun findAllActive(): List<User>

    /**
     * Find all admin users
     */
    @Query("{ roles: 'ADMIN' }")
    fun findAllAdmins(): List<User>

    /**
     * Find all locked users
     */
    @Query("{ isLocked: true }")
    fun findAllLockedUsers(): List<User>

    /**
     * Find users by role
     */
    @Query("{ roles: ?0 }")
    fun findByRole(role: String): List<User>

    /**
     * Find user by email verification token
     */
    fun findByEmailVerificationToken(token: String): Optional<User>

    /**
     * Find user by password reset token
     */
    fun findByPasswordResetToken(token: String): Optional<User>

    /**
     * Delete all users (for testing)
     */
    override fun deleteAll()
}
