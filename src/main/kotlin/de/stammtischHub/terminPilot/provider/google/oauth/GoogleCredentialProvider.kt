package de.stammtischHub.terminPilot.provider.google.oauth

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar
import org.springframework.stereotype.Service

private const val EXPIRY_BUFFER_MILLIS = 60_000L

/**
 * Turns persisted Google OAuth tokens into a usable, up-to-date [Credential],
 * refreshing it transparently when it is about to expire.
 */
@Service
class GoogleCredentialProvider(
  private val flow: GoogleAuthorizationCodeFlow,
  private val calendarAccountService: GoogleCalendarAccountService,
) {
  /** Loads a valid, refreshed [Credential] for the given user's Google Calendar. */
  fun getCredentialForUser(userId: Long): Credential = getCredential(calendarAccountService.findByUserId(userId))

  /** Loads a valid, refreshed [Credential] for a given calendar entity ID. */
  fun getCredentialForCalendar(calendarId: Long): Credential =
    getCredential(calendarAccountService.findById(calendarId))

  private fun getCredential(calendar: GoogleCalendar): Credential {
    val credential = loadCredential(calendar)
    refreshIfExpiringSoon(calendar, credential)
    return credential
  }

  private fun loadCredential(calendar: GoogleCalendar): Credential {
    val expiresInSeconds = (calendar.tokenExpiry - System.currentTimeMillis()) / 1000L

    val tokenResponse =
      TokenResponse()
        .setAccessToken(calendar.accessToken)
        .setRefreshToken(calendar.refreshToken)
        .setExpiresInSeconds(expiresInSeconds)

    return flow.createAndStoreCredential(tokenResponse, calendar.id.toString())
  }

  private fun refreshIfExpiringSoon(
    calendar: GoogleCalendar,
    credential: Credential,
  ) {
    val expiry = credential.expirationTimeMilliseconds ?: return
    val isExpiringSoon = expiry <= System.currentTimeMillis() + EXPIRY_BUFFER_MILLIS
    if (!isExpiringSoon) return

    if (credential.refreshToken()) {
      calendarAccountService.updateAccessToken(
        calendar,
        credential.accessToken,
        credential.expirationTimeMilliseconds,
      )
    }
  }
}
