package com.openmmo.ai.service.impl

import com.openmmo.ai.client.GmailOAuthClient
import com.openmmo.ai.entity.GmailConnection
import com.openmmo.ai.entity.GmailBotConfig
import com.openmmo.ai.exception.UpstreamException
import com.openmmo.ai.repository.GmailConnectionRepository
import com.openmmo.ai.repository.GmailBotConfigRepository
import com.openmmo.ai.service.IGmailOAuthService
import com.openmmo.ai.service.IAuthenticationService
import com.openmmo.ai.util.EncryptionUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.security.SecureRandom
import java.util.Base64

@Service
class GmailOAuthServiceImpl(
    private val connectionRepository: GmailConnectionRepository,
    private val botConfigRepository: GmailBotConfigRepository,
    private val oauthClient: GmailOAuthClient,
    private val authenticationService: IAuthenticationService,
    @Value("\${oauth2.google.client-id}") private val clientId: String,
    @Value("\${oauth2.google.client-secret}") private val clientSecret: String,
    @Value("\${gmail.oauth.redirect-uri}") private val redirectUri: String,
    @Value("\${token.enc.key-base64}") private val tokenEncKey: String,
    @Value("\${app.web-url}") private val appWebUrl: String
) : IGmailOAuthService {

    companion object {
        private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private val logger = LoggerFactory.getLogger(GmailOAuthServiceImpl::class.java)
    }

    override fun generateAuthUrl(userId: String): String {
        // Validate client ID is configured
        if (clientId.isBlank()) {
            throw IllegalStateException("GOOGLE_CLIENT_ID not configured. Please set the GOOGLE_CLIENT_ID environment variable.")
        }

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

    override fun handleCallback(code: String, state: String, userId: String): String {
        // Validate state
        validateState(state, userId)

        try {
            // Exchange code for tokens using client
            val tokenResponse = oauthClient.exchangeCodeForTokens(code, clientId, clientSecret, redirectUri)
            val accessToken = tokenResponse["access_token"] as? String
                ?: throw UpstreamException("No access token in response")

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
                throw UpstreamException("No refresh token received - please reconnect with prompt=consent or clear your Google OAuth permissions")
            }

            // Get Gmail address using client
            val gmailAddress = oauthClient.getGmailProfile(accessToken)

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

            logger.info("Gmail OAuth callback handled successfully for user: $userId")
            return "$appWebUrl/email-ai-bot?connected=1"
        } catch (e: Exception) {
            logger.error("Error handling Gmail OAuth callback: ${e.message}")
            throw e
        }
    }

    override fun refreshAccessToken(refreshToken: String): String {
        try {
            logger.debug("Refreshing Gmail access token")
            val accessToken = oauthClient.refreshAccessToken(refreshToken, clientId, clientSecret)
            logger.debug("Gmail access token refreshed successfully")
            return accessToken
        } catch (e: HttpClientErrorException) {
            logger.error("HTTP error refreshing Gmail token: ${e.statusCode}")
            throw UpstreamException("Failed to refresh Gmail token: ${e.statusCode}")
        } catch (e: Exception) {
            logger.error("Error refreshing Gmail token: ${e.message}")
            throw UpstreamException("Failed to refresh Gmail token: ${e.message}")
        }
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
            throw UpstreamException("Invalid state parameter")
        }
    }

    override fun getAppWebUrl(): String = appWebUrl
}
