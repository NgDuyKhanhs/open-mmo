package com.openmmo.ai.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Cache Configuration
 *
 * Caches:
 * - "botConfig": BotConfig cached for 5 minutes (avoid 100x DB queries)
 * - "reminderContext": Conversation context cached for 15 minutes (avoid repeated Gmail API calls)
 *
 * Using ConcurrentMapCacheManager (built-in Spring, no external dependency)
 * For production with strict TTL requirements, upgrade to Redis or Caffeine
 */
@Configuration
@EnableCaching
class CacheConfig {

    /**
     * Cache manager using ConcurrentMapCacheManager
     * Simple in-memory cache manager that's built-in to Spring
     */
    @Bean
    fun cacheManager(): CacheManager {
        return ConcurrentMapCacheManager("botConfig", "reminderContext")
    }
}







