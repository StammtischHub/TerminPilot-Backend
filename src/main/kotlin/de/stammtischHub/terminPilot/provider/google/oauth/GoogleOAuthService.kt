package de.stammtischHub.terminPilot.provider.google.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Orchestrates the Google OAuth 2.0 Authorization Code Flow for individual users.
 *
 * Responsibilities are intentionally narrow:
 * - Building the Google authorization URL with a user-bound `state` parameter.
 * - Exchanging authorization codes for tokens and delegating their persistence.
 *
 * Token/entity persistence lives in [GoogleCalendarAccountService], and turning
 * stored tokens into a live [com.google.api.client.auth.oauth2.Credential] lives
 * in [GoogleCredentialProvider].
 */
@Service
class GoogleOAuthService(
  private val flow: GoogleAuthorizationCodeFlow,
  private val calendarAccountService: GoogleCalendarAccountService,
  @Value("\${google.oauth.redirect-uri}") private val redirectUri: String,
) {
  /**
   * Builds the Google OAuth authorization URL for a specific user.
   *
   * The user's ID is embedded into the OAuth `state` parameter so the callback
   * can associate the returned tokens with the correct account.
   */
  fun buildAuthorizationUrl(userId: Long): String =
    flow
      .newAuthorizationUrl()
      .setRedirectUri(redirectUri)
      .setState(userId.toString())
      .set("prompt", "consent")
      .build()

  /**
   * Handles the OAuth callback by exchanging the authorization code for tokens
   * and persisting them for the corresponding user.
   */
  fun handleCallback(
    code: String,
    userId: Long,
  ) {
    val tokenResponse =
      flow
        .newTokenRequest(code)
        .setRedirectUri(redirectUri)
        .execute()

    calendarAccountService.saveTokens(userId, tokenResponse)
  }
}
