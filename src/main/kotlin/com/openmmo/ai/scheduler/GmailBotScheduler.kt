package com.openmmo.ai.scheduler

import com.openmmo.ai.service.IGmailAutoReplyService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Gmail Bot Scheduler
 * Automatically processes and replies to emails on a scheduled basis
 */
@Component
class GmailBotScheduler(
    private val autoReplyService: IGmailAutoReplyService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GmailBotScheduler::class.java)
    }

    /**
     * Auto-reply emails every 2 minutes
     * Runs: 00:00, 00:02, 00:04, 00:06, ...
     *
     * Configuration:
     * - fixedDelay = 120000 (milliseconds) = 2 minutes
     * - initialDelay = 30000 (start after 30 seconds of app startup)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    fun autoReplyEmailsScheduled() {
        try {
            logger.info("Starting scheduled auto-reply job...")

            val result = autoReplyService.autoReplyEmails()

            val status = result["status"]
            val totalProcessed = result["totalProcessed"]
            val totalReplied = result["totalReplied"]
            val totalErrors = result["totalErrors"]

            logger.info("Auto-reply job completed: processed=$totalProcessed, replied=$totalReplied, errors=$totalErrors")
        } catch (e: Exception) {
            logger.error("Error in scheduled auto-reply job: ${e.message}", e)
        }
    }

    /**
     * Alternative: Run every 5 minutes (300 seconds)
     * Uncomment and comment out autoReplyEmailsScheduled() if you prefer
     */
    // @Scheduled(fixedDelay = 300000, initialDelay = 30000)
    // fun autoReplyEmailsScheduled5Min() { ... }

    /**
     * Alternative: Run at specific times daily
     * Uncomment if you want to run only at certain times
     */
    // @Scheduled(cron = "0 0 * * * *") // Every hour
    // @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    // @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    // fun autoReplyEmailsScheduledCron() { ... }
}

