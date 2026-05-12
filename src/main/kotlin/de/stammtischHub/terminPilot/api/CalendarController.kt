package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.provider.google.GoogleCalendarService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/calendar")
final class CalendarController(private val googleCalendarService: GoogleCalendarService) {

    @GetMapping("/events")
    final fun getEvents(): List<Map<String, String>> {
        val now = LocalDateTime.now()
        val end = now.plusMonths(1)
        return googleCalendarService.getCalendarForTimespan(now, end).map { appointment ->
            mapOf(
                "title" to appointment.title,
                "start" to appointment.start.toString(),
                "end" to appointment.end.toString(),
                "location" to appointment.location,
                "description" to appointment.description
            )
        }
    }
}