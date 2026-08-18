package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.EventApi
import de.stammtischHub.terminPilot.exception.UnsatisfiableConstraintsException
import de.stammtischHub.terminPilot.model.generated.CreateEventRequest
import de.stammtischHub.terminPilot.model.generated.CreateEventResponse
import de.stammtischHub.terminPilot.model.generated.SuggestionsRequest
import de.stammtischHub.terminPilot.model.generated.SuggestionsResponse
import de.stammtischHub.terminPilot.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RestController
class EventController(
  private val eventService: EventService,
) : EventApi {
  override fun createEvent(createEventRequest: CreateEventRequest): ResponseEntity<CreateEventResponse> {
    if (!areConstraintsValid(createEventRequest)) {
      throw UnsatisfiableConstraintsException("Constraints are not logically satisfiable")
    }
    val event = eventService.createEvent(createEventRequest)

    val eventResponse = CreateEventResponse(event.title, event.start, event.end, event.participants, event.location, event.description)
    return ResponseEntity.status(201).body(eventResponse)
  }

  override fun getSuggestions(suggestionsRequest: SuggestionsRequest): ResponseEntity<SuggestionsResponse> {
    if (!areConstraintsValid(suggestionsRequest)) {
      throw UnsatisfiableConstraintsException("Constraints are not logically satisfiable")
    }
    val suggestions = eventService.suggestEvents(suggestionsRequest)
    return ResponseEntity.ok(SuggestionsResponse(suggestions = suggestions))
  }

  private fun areConstraintsValid(suggestionsRequest: SuggestionsRequest): Boolean {
    val constraints = suggestionsRequest.constraints
    if (constraints.weekdays.isEmpty()) return false
    if (constraints.dateRange.end.isBefore(constraints.dateRange.start)) return false
    if (constraints.timeRange.start >= constraints.timeRange.end) return false
    if (constraints.durationMinutes < 1) return false
    if (suggestionsRequest.participants.isEmpty()) return false

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = LocalTime.parse(constraints.timeRange.start, formatter)
    val end = LocalTime.parse(constraints.timeRange.end, formatter)
    val minutesAvailable = Duration.between(start, end).toMinutes()
    return constraints.durationMinutes <= minutesAvailable
  }
}
