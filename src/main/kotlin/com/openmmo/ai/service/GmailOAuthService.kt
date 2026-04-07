package com.openmmo.ai.service

import com.openmmo.ai.entity.GmailConnection
import com.openmmo.ai.entity.GmailBotConfig
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.util.EncryptionUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.security.SecureRandom
import java.util.Base64
import kotlin.random.Random

@Service
class GmailOAuthService(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository,
    private val restTemplate: RestTemplate,
    private val authenticationService: AuthenticationService,
    @Value("\${oauth2.google.client-id}") private val clientId: String,
    @Value("\${oauth2.google.client-secret}") private val clientSecret: String,
    @Value("\${gmail.oauth.redirect-uri}") private val redirectUri: String,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String,
    @Value("\${app.web-url}") private val appWebUrl: String
) {

    companion object {
        private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GMAIL_PROFILE_URL = "https://www.googleapis.com/gmail/v1/users/me/profile"
    }

    fun generateAuthUrl(userId: String): String {
        val state = generateState(userId)

        // Get user email to suggest in Google login
        val user = authenticationService.getUserById(userId)
        val userEmail = user?.email ?: ""

        // Build OAuth URL with login_hint to suggest email
        val loginHintParam = if (userEmail.isNotEmpty()) "&login_hint=$userEmail" else ""

        return "$GOOGLE_AUTH_URL?" +
            "client_id=$clientId" +
            "&redirect_uri=$redirectUri" +
            "&response_type=code" +
            "&scope=https://www.googleapis.com/auth/gmail.modify%20https://www.googleapis.com/auth/gmail.send" +
            "&access_type=offline" +
            "&prompt=consent" +
            "&include_granted_scopes=true" +
            loginHintParam +
            "&state=$state"
    }

    fun handleCallback(code: String, state: String, userId: String): String {
        // Validate state
        validateState(state, userId)

        // Exchange code for tokens
        val tokenResponse = exchangeCodeForTokens(code)
        val accessToken = tokenResponse["access_token"] as? String ?: throw IllegalStateException("No access token")

        // If user already granted permission before, refresh_token will be null
        // In that case, reuse the existing refresh_token from database
        val newRefreshToken = tokenResponse["refresh_token"] as? String
        val existingConnection = connectionRepository.findByUserId(userId)

        val refreshToken = if (newRefreshToken != null) {
            newRefreshToken
        } else if (existingConnection != null) {
            // Reuse existing refresh token (already encrypted in DB)
            EncryptionUtil.decrypt(existingConnection.refreshTokenEnc, tokenEncKey)
        } else {
            throw IllegalStateException("No refresh token received - please reconnect with prompt=consent or clear your Google OAuth permissions")
        }

        // Get Gmail address
        val gmailAddress = getGmailProfile(accessToken)

        // Encrypt refresh token
        val encryptedRefreshToken = EncryptionUtil.encrypt(refreshToken, tokenEncKey)

        // Save/upsert connection
        val connection = if (existingConnection != null) {
            existingConnection.copy(
                gmailAddress = gmailAddress,
                refreshTokenEnc = encryptedRefreshToken,
                updatedAt = java.time.LocalDateTime.now()
            )
        } else {
            GmailConnection(
                userId = userId,
                gmailAddress = gmailAddress,
                refreshTokenEnc = encryptedRefreshToken,
                scopes = "gmail.modify gmail.send"
            )
        }
        connectionRepository.save(connection)

        // Create bot config if not exists
        if (botConfigRepository.findByUserId(userId) == null) {
            botConfigRepository.save(GmailBotConfig(userId = userId, enabled = false))
        }

        return "$appWebUrl/email-ai-bot?connected=1"
    }

    fun refreshAccessToken(refreshToken: String): String {
        val body = mapOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token"
        )

        val response = restTemplate.postForObject(GOOGLE_TOKEN_URL, body, Map::class.java)
        return response?.get("access_token") as? String ?: throw IllegalStateException("Failed to refresh token")
    }

    private fun exchangeCodeForTokens(code: String): Map<*, *> {
        val body = mapOf(
            "code" to code,
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "redirect_uri" to redirectUri,
            "grant_type" to "authorization_code"
        )

        val response = restTemplate.postForObject(GOOGLE_TOKEN_URL, body, Map::class.java)
        return response ?: throw IllegalStateException("Failed to exchange code")
    }

    private fun getGmailProfile(accessToken: String): String {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val request = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            GMAIL_PROFILE_URL,
            HttpMethod.GET,
            request,
            Map::class.java
        )

        return response.body?.get("emailAddress") as? String ?: throw IllegalStateException("Failed to get Gmail profile")
    }

    private fun generateState(userId: String): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "$userId:$randomPart"
    }

    private fun validateState(state: String, userId: String) {
        if (!state.startsWith("$userId:")) {
            throw IllegalStateException("Invalid state parameter")
        }
    }

    fun getAppWebUrl(): String = appWebUrl
}

