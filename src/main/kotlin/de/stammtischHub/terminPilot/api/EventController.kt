package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.EventApi
import de.stammtischHub.terminPilot.domain.EventConstraints
import de.stammtischHub.terminPilot.domain.EventDraft
import de.stammtischHub.terminPilot.exception.UnsatisfiableConstraintsException
import de.stammtischHub.terminPilot.model.generated.CreateEventRequest
import de.stammtischHub.terminPilot.model.generated.CreateEventResponse
import de.stammtischHub.terminPilot.model.generated.SuggestionsRequest
import de.stammtischHub.terminPilot.model.generated.SuggestionsResponse
import de.stammtischHub.terminPilot.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RestController
class EventController(
  private val eventService: EventService,
) : EventApi {
  override fun createEvent(createEventRequest: CreateEventRequest): ResponseEntity<CreateEventResponse> {
    val draft =
      try {
        EventDraft(
          title = createEventRequest.title,
          start = createEventRequest.start.toLocalDateTime(), // TODO: Timezones!
          end = createEventRequest.end.toLocalDateTime(),
          participantIds = createEventRequest.participants,
          location = createEventRequest.location,
          description = createEventRequest.notes,
        )
      } catch (e: IllegalArgumentException) {
        throw UnsatisfiableConstraintsException("Constraints are not logically satisfiable", e)
      }

    val event = eventService.createEvent(draft)
    val eventResponse =
      CreateEventResponse(
        event.title,
        event.start.atOffset(ZoneOffset.UTC),
        event.end.atOffset(ZoneOffset.UTC),
        event.participants.toUserDtoList(),
        event.location,
        event.description,
      )
    return ResponseEntity.status(201).body(eventResponse)
  }

  override fun getSuggestions(suggestionsRequest: SuggestionsRequest): ResponseEntity<SuggestionsResponse> {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val constraints =
      try {
        EventConstraints(
          weekdays =
            suggestionsRequest.constraints.weekdays
              .toSet()
              .toDayOfWeekSet(),
          dateRange = suggestionsRequest.constraints.dateRange.start..suggestionsRequest.constraints.dateRange.end,
          timeRange =
            LocalTime.parse(
              suggestionsRequest.constraints.timeRange.start,
              formatter,
            )..LocalTime.parse(suggestionsRequest.constraints.timeRange.end, formatter),
          duration = suggestionsRequest.constraints.durationMinutes,
          participantIds = suggestionsRequest.participants,
        )
      } catch (e: IllegalArgumentException) {
        throw UnsatisfiableConstraintsException("Constraints are not logically satisfiable", e)
      }

    val suggestions = eventService.suggestEvents(constraints)
    return ResponseEntity.ok(SuggestionsResponse(suggestions = suggestions))
  }
}
