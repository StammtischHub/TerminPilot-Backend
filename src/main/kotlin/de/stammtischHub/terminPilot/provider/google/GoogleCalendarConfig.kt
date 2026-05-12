package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class GoogleCalendarConfig {

    @Value($$"${google.calendar.credentials-path}")
    private lateinit var credentialsResource: Resource

    @Bean
    fun googleCalendarClient(): Calendar {
        val credentials = GoogleCredentials
            .fromStream(credentialsResource.inputStream)
            .createScoped(listOf(CalendarScopes.CALENDAR))

        return Calendar.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName("TerminPilot")
            .build()
    }
}