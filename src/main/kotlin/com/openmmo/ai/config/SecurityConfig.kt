package com.openmmo.ai.config

import com.openmmo.ai.security.JwtAuthenticationFilter
import com.openmmo.ai.util.JwtTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.slf4j.LoggerFactory

/**
 * Spring Security Configuration
 * Configures JWT authentication, CORS, and authorization rules
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:4200}")
    private lateinit var allowedOrigins: String

    @Value("\${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private lateinit var allowedMethods: String

    @Value("\${cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
    private lateinit var allowedHeaders: String

    @Value("\${cors.max-age:3600}")
    private var maxAge: Long = 3600

    /**
     * Security filter chain configuration
     */
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF (stateless JWT authentication)
            .csrf { it.disable() }

            // Enable CORS
            .cors { }

            // Set session management to stateless
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // Authorization configuration
            .authorizeHttpRequests { authz ->
                authz
                    // Allow all OPTIONS (preflight) requests
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Public endpoints
                    .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/google-login",
                        "/api/v1/auth/refresh-token",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password"
                    ).permitAll()

                    // Health endpoints
                    .requestMatchers(
                        "/health",
                        "/health/**",
                        "/api/v1/health/**",
                        "/actuator",
                        "/actuator/**"
                    ).permitAll()

                    // Swagger/API documentation
                    .requestMatchers(
                        "/swagger-ui**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api-docs**"
                    ).permitAll()

                    // Gmail OAuth callback - allow without authentication (Google calls this directly)
                    .requestMatchers("/api/v1/gmail/connect/callback")
                    .permitAll()

                    // Gmail endpoints - require authentication
                    .requestMatchers("/api/v1/gmail/**")
                    .authenticated()

                    // Agent endpoints - require authentication
                    .requestMatchers("/api/v1/agent/**")
                    .authenticated()

                    // Admin endpoints - require ADMIN role
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**")
                    .hasAnyRole("ADMIN")

                    // All other authenticated endpoints
                    .anyRequest()
                    .authenticated()
            }

            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter::class.java
            )

            // Exception handling
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { request, response, authException ->
                    response.sendError(
                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized: ${authException.message}"
                    )
                }
                ex.accessDeniedHandler { request, response, accessDeniedException ->
                    response.sendError(
                        jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN,
                        "Access Denied: ${accessDeniedException.message}"
                    )
                }
            }

        return http.build()
    }

    /**
     * Password encoder bean using BCrypt
     * Strength 12 means higher security but slower encoding
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    /**
     * Authentication manager bean
     */
    @Bean
    @Throws(Exception::class)
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    /**
     * CORS configuration source
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Parse allowed origins from config
        val origins = allowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "*" } // Filter out empty or wildcard

        logger.info("🔐 CORS origins configured: $origins")

        // Use pattern if needed for development
        if (origins.isEmpty()) {
            logger.warn("⚠️  No CORS origins configured, defaulting to localhost")
            configuration.allowedOrigins = listOf(
                "http://localhost:5173",
                "http://localhost:8080",
                "http://localhost:3000"
            )
        } else {
            configuration.allowedOrigins = origins
        }

        // Parse allowed methods
        configuration.allowedMethods = allowedMethods
            .split(",")
            .map { it.trim() }

        // Parse allowed headers
        val customHeaders = allowedHeaders
            .split(",")
            .map { it.trim() }
            .toMutableList()

        // Additional common headers
        customHeaders.addAll(listOf(
            "Accept",
            "Accept-Language",
            "Accept-Encoding",
            "User-Agent"
        ))

        configuration.allowedHeaders = customHeaders
        configuration.exposedHeaders = listOf("Authorization", "Content-Type")

        // 🔐 Enable credentials for HttpOnly cookie support
        configuration.allowCredentials = true
        configuration.maxAge = maxAge

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
