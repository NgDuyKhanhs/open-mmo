package com.openmmo.ai.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Global CORS Configuration
 * Reads allowed origins from application.yaml via CORS_ORIGINS environment variable
 *
 * NOTE: When allowCredentials is true, wildcard "*" is not allowed.
 * Use allowedOriginPatterns(".*") instead to support all origins with credentials.
 * For production, explicitly list allowed origins (e.g., https://app.example.com)
 */
@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins:*}")
    private val allowedOrigins: String,

    @Value("\${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private val allowedMethods: String,

    @Value("\${cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
    private val allowedHeaders: String,

    @Value("\${cors.allow-credentials:false}")
    private val allowCredentials: Boolean,

    @Value("\${cors.max-age:3600}")
    private val maxAge: Long
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        // Parse methods from comma-separated string
        val methods = allowedMethods.split(",").map { it.trim() }.toTypedArray()

        // Parse headers from comma-separated string
        val headers = allowedHeaders.split(",").map { it.trim() }.toTypedArray()

        val corsRegistry = registry
            .addMapping("/**")
            .allowedMethods(*methods)
            .allowedHeaders(*headers)
            .maxAge(maxAge)

        // Handle origins based on credentials requirement
        if (allowCredentials) {
            // When allowCredentials is true, wildcard "*" is NOT allowed
            if (allowedOrigins.trim() == "*") {
                // Use pattern to match all origins (but this won't send Access-Control-Allow-Credentials header)
                // For production, it's better to explicitly list origins
                corsRegistry.allowedOriginPatterns(".*")
            } else {
                // Parse and use explicit origins
                val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()
                corsRegistry.allowedOrigins(*origins)
            }
        } else {
            // When credentials are not needed, wildcard "*" is allowed
            if (allowedOrigins.trim() == "*") {
                corsRegistry.allowedOrigins("*")
            } else {
                val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()
                corsRegistry.allowedOrigins(*origins)
            }
        }

        corsRegistry.allowCredentials(allowCredentials)
    }
}



