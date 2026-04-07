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

