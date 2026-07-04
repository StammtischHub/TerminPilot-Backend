package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.CalendarApi
import de.stammtischHub.terminPilot.model.generated.CalendarEvent
import de.stammtischHub.terminPilot.provider.google.GoogleCalendarService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/calendar")
class CalendarController(
  private val googleCalendarService: GoogleCalendarService,
) : CalendarApi {
  @GetMapping("/events")
  override fun getEvents(
    @RequestParam userId: Long,
  ): ResponseEntity<List<CalendarEvent>> {
    val now = LocalDateTime.now()
    val end = now.plusMonths(1)
    val events =
      googleCalendarService.getCalendarForTimespan(userId, now, end).map { appointment ->
        CalendarEvent(
          title = appointment.title,
          start = appointment.start.toString(),
          end = appointment.end.toString(),
          location = appointment.location,
          description = appointment.description,
        )
      }
    return ResponseEntity.ok(events)
  }
}
