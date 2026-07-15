package de.stammtischHub.terminPilot.provider.google.oauth

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar
import de.stammtischHub.terminPilot.persistence.repository.GoogleCalendarRepository
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service responsible for managing the Google OAuth 2.0 Authorization Code Flow
 * on behalf of individual users.
 *
 * Handles:
 * - Generating the Google authorization URL (redirects user to consent screen)
 * - Processing the OAuth callback and persisting the issued tokens in the database
 * - Loading and refreshing per-user [Credential] objects for downstream API calls
 */
@Service
class GoogleOAuthService(
  private val flow: GoogleAuthorizationCodeFlow,
  private val userRepository: UserRepository,
  private val googleCalendarRepository: GoogleCalendarRepository,
  @Value("\${google.oauth.redirect-uri}") private val redirectUri: String,
) {

  fun buildAuthorizationUrl(userId: Long): String =
    flow
      .newAuthorizationUrl()
      .setRedirectUri(redirectUri)
      .setState(userId.toString())
      .build()

  fun handleCallback(code: String, userId: Long) {
    val tokenResponse =
      flow
        .newTokenRequest(code)
        .setRedirectUri(redirectUri)
        .execute()

    val user = userRepository.findById(userId)
      ?: throw IllegalArgumentException("User with ID $userId not found.")

    // Prüfen, ob bereits ein GoogleCalendar existiert
    val existingCalendar = user.calendars
      .filterIsInstance<GoogleCalendar>()
      .firstOrNull()

    if (existingCalendar != null) {
      existingCalendar.accessToken = tokenResponse.accessToken
      if (tokenResponse.refreshToken != null) {
        existingCalendar.refreshToken = tokenResponse.refreshToken
      }
      existingCalendar.tokenExpiry =
        tokenResponse.expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
          ?: existingCalendar.tokenExpiry

      googleCalendarRepository.save(existingCalendar)
    } else {
      val newCalendar = GoogleCalendar(
        user = user,
        calendarName = "primary",
        accessToken = tokenResponse.accessToken,
        refreshToken = tokenResponse.refreshToken ?: "",
        tokenExpiry = System.currentTimeMillis() +
          (tokenResponse.expiresInSeconds ?: 3600) * 1000L
      )

      googleCalendarRepository.save(newCalendar)
      user.calendars.add(newCalendar)
      userRepository.save(user)
    }
  }

  fun getCredentialForCalendar(calendarId: Long): Credential {
    val calendar = googleCalendarRepository.findById(calendarId) ?: throw IllegalArgumentException("Calendar with ID $calendarId not found.")

    val expiresInSeconds =
      (calendar.tokenExpiry - System.currentTimeMillis()) / 1000L

    val tokenResponse =
      TokenResponse()
        .setAccessToken(calendar.accessToken)
        .setRefreshToken(calendar.refreshToken)
        .setExpiresInSeconds(expiresInSeconds)

    val credential = flow.createAndStoreCredential(tokenResponse, calendarId.toString())

    if (credential.expirationTimeMilliseconds != null &&
      credential.expirationTimeMilliseconds!! <= System.currentTimeMillis() + 60_000L
    ) {
      val refreshed = credential.refreshToken()
      if (refreshed) {
        calendar.accessToken = credential.accessToken
        calendar.tokenExpiry = credential.expirationTimeMilliseconds!!
        googleCalendarRepository.save(calendar)
      }
    }

    return credential
  }

  fun getCalendarName(calendarId: Long): String {
    val calendar = googleCalendarRepository.findById(calendarId) ?: throw IllegalArgumentException("Calendar with ID $calendarId not found.")

    return calendar.calendarName
  }
}

