package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import org.springframework.context.annotation.Configuration

/**
 * Configuration for creating Google Calendar API clients.
 *
 * Instead of a single application-wide client backed by a service account,
 * this provides a factory method that builds a [Calendar] client scoped to an
 * individual user's OAuth 2.0 [Credential], enabling true multi-user support.
 */
@Configuration
class GoogleCalendarConfig {
  /**
   * Builds a Google Calendar API client authorized with the given [credential].
   *
   * A new client instance is created per request so that each user's API calls
   * are isolated and use their own token.
   *
   * @param credential The OAuth 2.0 credential for the authenticated user.
   * @return A configured [Calendar] client instance.
   */
  fun buildCalendarClient(credential: Credential): Calendar =
    Calendar
      .Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        credential,
      ).setApplicationName("TerminPilot")
      .build()
}
