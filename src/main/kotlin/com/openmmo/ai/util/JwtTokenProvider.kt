package com.openmmo.ai.util

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.util.*
import javax.crypto.SecretKey

/**
 * JWT Token Provider
 * Handles token generation, validation, and claims extraction
 * Uses JJWT library with HS512 signature algorithm
 */
@Component
class JwtTokenProvider : InitializingBean {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private var jwtExpirationMs: Long = 86400000 // 24 hours

    @Value("\${jwt.refresh.expiration}")
    private var refreshTokenExpirationMs: Long = 604800000 // 7 days

    override fun afterPropertiesSet() {
        // Validate JWT secret after properties are injected
        if (!::jwtSecret.isInitialized || jwtSecret.isEmpty()) {
            val error = "JWT_SECRET not configured. Set JWT_SECRET environment variable or configure jwt.secret in application.yaml"
            logger.error(error)
            throw IllegalStateException(error)
        }

        // Verify secret is valid Base64
        try {
            java.util.Base64.getDecoder().decode(jwtSecret)
            logger.info("JWT secret validated successfully (Base64 format)")
        } catch (ex: IllegalArgumentException) {
            val error = "JWT_SECRET must be Base64 encoded. Current value is not valid Base64."
            logger.error(error)
            throw IllegalStateException(error, ex)
        } catch (ex: Exception) {
            logger.warn("Could not validate JWT secret format: ${ex.message}")
        }
    }

    /**
     * Generate JWT access token
     */
    fun generateAccessToken(userId: String, email: String, roles: List<String> = emptyList()): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        val claims = Jwts.claims()
            .setSubject(userId)
            .add("email", email)
            .add("roles", roles)
            .build()

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Generate JWT refresh token
     */
    fun generateRefreshToken(userId: String, email: String): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenExpirationMs)

        val claims = Jwts.claims()
            .setSubject(userId)
            .add("email", email)
            .add("type", "refresh")
            .build()

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    /**
     * Validate JWT token
     */
    fun validateToken(token: String): Boolean {
        return try {
            val signingKey = getSigningKey()
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (ex: SecurityException) {
            System.err.println("JWT Security exception: ${ex.message}")
            false
        } catch (ex: MalformedJwtException) {
            System.err.println("JWT Malformed exception: ${ex.message}")
            false
        } catch (ex: ExpiredJwtException) {
            System.err.println("JWT Expired exception: ${ex.message}")
            false
        } catch (ex: UnsupportedJwtException) {
            System.err.println("JWT Unsupported exception: ${ex.message}")
            false
        } catch (ex: IllegalArgumentException) {
            System.err.println("JWT Illegal argument exception: ${ex.message}")
            false
        } catch (ex: Exception) {
            System.err.println("JWT Validation exception: ${ex.message}")
            ex.printStackTrace()
            false
        }
    }

    /**
     * Validate token and get claims
     */
    fun validateTokenAndGetClaims(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Extract user ID from token
     */
    fun getUserIdFromToken(token: String): String? {
        return try {
            getAllClaimsFromToken(token).subject
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Extract email from token
     */
    fun getEmailFromToken(token: String): String? {
        return try {
            getAllClaimsFromToken(token)["email"]?.toString()
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Extract roles from token
     */
    @Suppress("UNCHECKED_CAST")
    fun getRolesFromToken(token: String): List<String> {
        return try {
            val roles = getAllClaimsFromToken(token)["roles"]
            when (roles) {
                is List<*> -> roles.map { it.toString() }
                else -> emptyList()
            }
        } catch (ex: Exception) {
            emptyList()
        }
    }

    /**
     * Check if token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            getAllClaimsFromToken(token).expiration.before(Date())
        } catch (ex: Exception) {
            true
        }
    }

    /**
     * Get all claims from token
     */
    private fun getAllClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * Get signing key from JWT secret
     * Converts base64 secret to SecretKey for HS512 algorithm
     */
    private fun getSigningKey(): SecretKey {
        return try {
            // Decode base64 secret
            val decodedKey = java.util.Base64.getDecoder().decode(jwtSecret)
            Keys.hmacShaKeyFor(decodedKey)
        } catch (ex: IllegalArgumentException) {
            logger.error("Failed to decode JWT secret as Base64", ex)
            throw IllegalStateException("JWT secret must be valid Base64 encoded", ex)
        }
    }

    /**
     * Refresh expired token if within grace period
     */
    fun refreshExpiredToken(token: String): String? {
        return try {
            val claims = validateTokenAndGetClaims(token)
            if (claims != null && isTokenExpired(token)) {
                val userId = claims.subject
                val email = claims["email"]?.toString() ?: return null
                @Suppress("UNCHECKED_CAST")
                val roles = (claims["roles"] as? List<*>)?.map { it.toString() } ?: emptyList()
                generateAccessToken(userId, email, roles)
            } else {
                null
            }
        } catch (ex: Exception) {
            null
        }
    }
}
