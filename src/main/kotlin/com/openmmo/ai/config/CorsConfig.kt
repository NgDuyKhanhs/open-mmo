package com.openmmo.ai.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Global CORS Configuration
 * Reads allowed origins from application.yaml via CORS_ORIGINS environment variable
 * This replaces individual @CrossOrigin annotations on controllers
 */
@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins:*}")
    private val allowedOrigins: String,

    @Value("\${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private val allowedMethods: String,

    @Value("\${cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
    private val allowedHeaders: String,

    @Value("\${cors.allow-credentials:true}")
    private val allowCredentials: Boolean,

    @Value("\${cors.max-age:3600}")
    private val maxAge: Long
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        // Parse origins from comma-separated string
        val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()

        // Parse methods from comma-separated string
        val methods = allowedMethods.split(",").map { it.trim() }.toTypedArray()

        // Parse headers from comma-separated string
        val headers = allowedHeaders.split(",").map { it.trim() }.toTypedArray()

        registry
            .addMapping("/**")
            .allowedOrigins(*origins)
            .allowedMethods(*methods)
            .allowedHeaders(*headers)
            .allowCredentials(allowCredentials)
            .maxAge(maxAge)
    }
}

