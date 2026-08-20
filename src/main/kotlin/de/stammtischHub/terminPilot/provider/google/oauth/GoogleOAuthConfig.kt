package de.stammtischHub.terminPilot.provider.google.oauth

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for the Google OAuth 2.0 Authorization Code Flow.
 *
 * Provides a [GoogleAuthorizationCodeFlow] bean configured with the application's
 * OAuth client credentials, requesting offline access (refresh tokens) and
 * full Google Calendar scope. Tokens are cached in memory during the application's
 * lifetime; persistent storage of tokens is handled separately via the database.
 */
@Configuration
class GoogleOAuthConfig {
  @Bean
  fun googleAuthorizationCodeFlow(
    @Value($$"${GOOGLE_OAUTH_CLIENT_ID}") clientId: String,
    @Value($$"${GOOGLE_OAUTH_CLIENT_SECRET}") clientSecret: String,
  ): GoogleAuthorizationCodeFlow =
    GoogleAuthorizationCodeFlow
      .Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        clientId,
        clientSecret,
        listOf(CalendarScopes.CALENDAR),
      ).setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
      .setAccessType("offline")
      .build()
}
