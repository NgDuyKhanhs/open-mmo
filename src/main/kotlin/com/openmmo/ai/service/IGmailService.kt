package com.openmmo.ai.service

import com.openmmo.ai.dto.ConnectGmailResponse
import com.openmmo.ai.dto.GmailStatusResponse
import com.openmmo.ai.dto.GmailBotConfigRequest
import com.openmmo.ai.dto.MailboxItemResponse

/**
 * Gmail OAuth Service Interface
 */
interface IGmailOAuthService {
    fun generateAuthUrl(userId: String): String
    fun handleCallback(code: String, state: String, userId: String): String
    fun refreshAccessToken(refreshToken: String): String
    fun getAppWebUrl(): String
}

/**
 * Gmail API Service Interface
 */
interface IGmailApiService {
    fun getMailbox(userId: String, boxType: String, maxResults: Int = 20): List<MailboxItemResponse>
    fun searchMessages(userId: String, query: String, maxResults: Int = 10): List<String>
    fun getMessageBody(userId: String, messageId: String): String
    fun sendReply(userId: String, threadId: String, toEmail: String, subject: String, bodyText: String)
    fun modifyLabels(userId: String, messageId: String, addLabels: List<String>, removeLabels: List<String>)
    fun ensureLabel(userId: String, labelName: String): String
}

/**
 * Gmail Bot Service Interface
 */
interface IGmailBotService {
    fun getStatus(userId: String): GmailStatusResponse
    fun enableBot(userId: String): Map<String, String>
    fun disableBot(userId: String): Map<String, String>
    fun updateConfig(userId: String, triggerSubject: String): Map<String, String>
    fun getPrompt(userId: String): Map<String, String>
    fun updatePrompt(userId: String, customPrompt: String): Map<String, String>
}


