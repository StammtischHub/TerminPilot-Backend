package de.StammtischHub.TerminPilot

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/calendar")
class CalendarController(private val calendarService: CalendarService) {

    @GetMapping("/events")
    fun getEvents(): List<Map<String, String>> =
        calendarService.getUpcomingEvents().map { event ->
            mapOf(
                "title" to (event.summary ?: "(kein Titel)"),
                "start" to (event.start.dateTime?.toString()
                    ?: event.start.date.toString()),
                "end" to (event.end.dateTime?.toString()
                    ?: event.end.date.toString()),
                "location" to (event.location ?: "(keine Adresse)"),
                "description" to (event.description ?: "(keine Beschreibung)")
            )
        }
}