package com.openmmo.ai.security

import com.openmmo.ai.util.JwtTokenProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

/**
 * JWT Authentication Filter
 * Validates JWT tokens from Authorization header and sets up Spring Security context
 */
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip JWT validation for CORS preflight OPTIONS requests
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = getJwtFromRequest(request)

            if (token != null && jwtTokenProvider.validateToken(token)) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                val email = jwtTokenProvider.getEmailFromToken(token)
                val roles = jwtTokenProvider.getRolesFromToken(token)

                if (userId != null) {
                    // Convert roles to GrantedAuthority
                    val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }

                    // Create authentication token
                    val authenticationToken = UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                    )

                    // Set authentication details
                    authenticationToken.details = mapOf(
                        "email" to email,
                        "token" to token
                    )

                    // Set in security context
                    SecurityContextHolder.getContext().authentication = authenticationToken

                    logger.debug("Set Spring Security authentication for user: $userId")
                }
            }
        } catch (ex: Exception) {
            logger.debug("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: Authorization: Bearer <token>
     */
    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring("Bearer ".length)
        } else {
            null
        }
    }
}
