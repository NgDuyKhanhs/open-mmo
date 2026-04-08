package com.openmmo.ai.client

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.slf4j.LoggerFactory

/**
 * Gmail OAuth Client
 * Handles OAuth2 token exchange with Google
 */
@Component
class GmailOAuthClient(
    private val restTemplate: RestTemplate
) {

    companion object {
        private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val GMAIL_PROFILE_URL = "https://www.googleapis.com/gmail/v1/users/me/profile"
        private val logger = LoggerFactory.getLogger(GmailOAuthClient::class.java)
    }

    /**
     * Exchange authorization code for tokens
     */
    fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): Map<String, Any> {
        logger.debug("Exchanging authorization code for tokens")

        try {
            val body = mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "redirect_uri" to redirectUri
            )

            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }

            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                HttpEntity(body, headers),
                Map::class.java
            ).body as? Map<String, Any> ?: emptyMap<String, Any>()

            logger.debug("Token exchange successful")
            return response
        } catch (e: HttpClientErrorException) {
            logger.error("Token exchange failed: ${e.statusCode} - ${e.responseBodyAsString}")
            throw e
        }
    }

    /**
     * Refresh access token using refresh token
     */
    fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): String {
        logger.debug("Refreshing access token")

        try {
            val body = mapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_id" to clientId,
                "client_secret" to clientSecret
            )

            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                HttpEntity(body),
                Map::class.java
            ).body as? Map<String, Any> ?: throw IllegalStateException("No response from token endpoint")

            val accessToken = response["access_token"] as? String
                ?: throw IllegalStateException("No access token in response")

            logger.debug("Access token refreshed successfully")
            return accessToken
        } catch (e: Exception) {
            logger.error("Failed to refresh access token: ${e.message}")
            throw e
        }
    }

    /**
     * Get Gmail profile/address
     */
    fun getGmailProfile(accessToken: String): String {
        logger.debug("Fetching Gmail profile")

        try {
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
            val request = HttpEntity<String>(headers)

            @Suppress("UNCHECKED_CAST")
            val response = restTemplate.exchange(
                GMAIL_PROFILE_URL,
                HttpMethod.GET,
                request,
                Map::class.java
            ).body as? Map<String, Any> ?: throw IllegalStateException("No response from Gmail profile")

            val emailAddress = response["emailAddress"] as? String
                ?: throw IllegalStateException("No emailAddress in response")

            logger.debug("Gmail profile fetched: $emailAddress")
            return emailAddress
        } catch (e: Exception) {
            logger.error("Failed to get Gmail profile: ${e.message}")
            throw e
        }
    }
}




