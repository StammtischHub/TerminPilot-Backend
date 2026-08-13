package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import org.springframework.stereotype.Component

private const val APPLICATION_NAME = "TerminPilot" // TODO: Extract in application.yaml

/**
 * Abstraction over the creation of Google [Calendar] API clients.
 *
 * Depending on this interface allows [GoogleCalendarService] to be
 * unit-tested without hitting the real Google transport layer.
 */
interface CalendarClientFactory {
  /**
   * Builds a Google Calendar API client authorized with the given [credential].
   *
   * @param credential The OAuth 2.0 credential for the authenticated user.
   * @return A configured [Calendar] client instance.
   */
  fun buildClient(credential: Credential): Calendar
}

/**
 * Default [CalendarClientFactory] backed by the real Google API client.
 *
 * A new client instance is created per call so that each user's API calls
 * are isolated and use their own token.
 */
@Component
class GoogleCalendarClientFactory : CalendarClientFactory {
  override fun buildClient(credential: Credential): Calendar =
    Calendar
      .Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        credential,
      ).setApplicationName(APPLICATION_NAME)
      .build()
}
