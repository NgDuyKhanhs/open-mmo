package com.openmmo.ai.scheduler

import com.openmmo.ai.service.IGmailAutoReplyService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * Gmail Bot Scheduler Configuration
 * ThreadPool setup for async email processing
 */
@Configuration
class SchedulerConfig {

    @Bean(name = ["gmailTaskScheduler"])
    fun gmailTaskScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 3
        scheduler.setThreadNamePrefix("gmail-auto-reply-")
        scheduler.initialize()
        return scheduler
    }
}

/**
 * Gmail Bot Scheduler
 * Automatically processes and replies to emails on a scheduled basis (async processing)
 *
 * Option 4: Async + ThreadPool with timeout protection
 */
@Component
class GmailBotScheduler(
    private val autoReplyService: IGmailAutoReplyService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailBotScheduler::class.java)
        private const val JOB_TIMEOUT_MINUTES = 5L
    }

    // Track last job finish time for timeout protection
    private val lastJobFinishTime = AtomicLong(0)
    private val isJobRunning = AtomicLong(0)

    /**
     * Scheduler trigger - Async email processing every 1 minute
     *
     * Configuration:
     * - fixedDelay = 60000 (milliseconds) = 1 minute
     * - initialDelay = 30000 (start after 30 seconds of app startup)
     *
     * Flow:
     * 1. Main thread checks if job already running (timeout protection)
     * 2. If not running, submit async task to ThreadPool
     * 3. Async task runs in background thread
     * 4. Main thread returns immediately (non-blocking)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    fun autoReplyEmailsScheduled() {
        try {
            // Timeout protection: Check if last job exceeded 5 minutes
            val currentTime = System.currentTimeMillis()
            val lastFinish = lastJobFinishTime.get()
            val jobDuration = currentTime - lastFinish

            if (lastFinish > 0 && jobDuration > JOB_TIMEOUT_MINUTES * 60 * 1000) {
                logger.warn("⚠️  Previous job exceeded timeout ($JOB_TIMEOUT_MINUTES min), skipping this trigger")
                return
            }

            // Check if job already running
            val previousValue = isJobRunning.getAndSet(currentTime)
            if (previousValue > 0) {
                logger.debug("Job already running, skipping this trigger")
                return
            }

            logger.info("📨 Scheduler triggered - submitting async job")

            // Submit async task (non-blocking)
            submitAutoReplyJobAsync()

        } catch (e: Exception) {
            logger.error("❌ Error in scheduler trigger: ${e.message}", e)
            isJobRunning.set(0) // Reset on error
        }
    }

    /**
     * Async task - processes and replies to emails
     * Runs in background thread from ThreadPool
     */
    @Async("gmailTaskScheduler")
    fun submitAutoReplyJobAsync() {
        val jobStartTime = System.currentTimeMillis()
        val jobId = java.util.UUID.randomUUID().toString().substring(0, 8)

        try {
            logger.info("[Job-$jobId] Starting async auto-reply processing...")

            val result = autoReplyService.autoReplyEmails()

            val totalProcessed = result["totalProcessed"]
            val totalReplied = result["totalReplied"]
            val totalErrors = result["totalErrors"]

            val jobDuration = System.currentTimeMillis() - jobStartTime
            logger.info("✅ [Job-$jobId] Auto-reply completed in ${jobDuration}ms: " +
                    "processed=$totalProcessed, replied=$totalReplied, errors=$totalErrors")

            lastJobFinishTime.set(System.currentTimeMillis())

        } catch (e: Exception) {
            val jobDuration = System.currentTimeMillis() - jobStartTime
            logger.error("❌ [Job-$jobId] Error in async auto-reply job after ${jobDuration}ms: ${e.message}", e)
            lastJobFinishTime.set(System.currentTimeMillis())

        } finally {
            isJobRunning.set(0) // Mark job as finished
        }
    }

    /**
     * Manual trigger endpoint (for testing/API)
     * Can be called from REST controller to trigger immediately
     */
    fun triggerAutoReplyManual(): CompletableFuture<Map<String, Any>> {
        logger.info("🔔 Manual auto-reply trigger requested")
        return CompletableFuture.supplyAsync({
            try {
                autoReplyService.autoReplyEmails()
            } catch (e: Exception) {
                logger.error("Error in manual trigger: ${e.message}", e)
                mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            }
        })
    }

    /**
     * Get current scheduler status
     */
    fun getSchedulerStatus(): Map<String, Any> {
        val isRunning = isJobRunning.get() > 0
        val lastFinish = lastJobFinishTime.get()
        val timeSinceLastJob = if (lastFinish > 0) {
            System.currentTimeMillis() - lastFinish
        } else {
            -1L
        }

        return mapOf(
            "isRunning" to isRunning,
            "lastJobFinishTime" to lastFinish,
            "timeSinceLastJobMs" to timeSinceLastJob,
            "schedulerActive" to true
        )
    }

    /**
     * Alternative configurations (uncomment as needed)
     */

    /**
     * Alternative: Run every 5 minutes
     * Use when: Lower frequency, less server load needed
     */
    // @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    // fun autoReplyEmails5MinInterval() { ... }

    /**
     * Alternative: Cron expressions for specific times
     */
    // @Scheduled(cron = "0 0 * * * *")         // Every hour
    // @Scheduled(cron = "0 */5 * * * *")       // Every 5 minutes
    // @Scheduled(cron = "0 0 9 * * *")         // Every day at 9 AM
    // fun autoReplyEmailsScheduledCron() { ... }
}






