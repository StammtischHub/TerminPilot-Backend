package de.stammtischHub.terminPilot

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class CalendarService(
    private val calendarClient: Calendar,
    @Value($$"${google.calendar.id}") private val calendarId: String
) {

    fun getUpcomingEvents(): List<Event> {
        val now = DateTime(System.currentTimeMillis())
        val oneWeekLater = DateTime(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)

        return calendarClient.events().list(calendarId)
            .setTimeMin(now)
            .setTimeMax(oneWeekLater)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
            .items ?: emptyList()
    }

}