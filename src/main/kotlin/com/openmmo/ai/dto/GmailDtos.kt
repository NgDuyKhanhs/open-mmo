package com.openmmo.ai.dto

data class ConnectGmailResponse(
    val url: String
)

data class GmailStatusResponse(
    val connected: Boolean,
    val gmailAddress: String,
    val botEnabled: Boolean,
    val triggerSubject: String,
    val lastRunAt: String?,
    val lastError: String?
)

data class MailboxItemResponse(
    val id: String,
    val threadId: String,
    val from: String,
    val subject: String,
    val date: String,
    val snippet: String,
    val labels: List<String>,
    val aiprovider: String? = null
)

data class MailboxPageResponse(
    val emails: List<MailboxItemResponse>,
    val nextPageToken: String? = null,
    val totalCount: Int = 0
)

data class GmailBotConfigRequest(
    val triggerSubject: String? = null,
    val customPrompt: String? = null,
    val enabled: Boolean? = null
)
