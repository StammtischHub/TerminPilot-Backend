package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.EventApi
import de.stammtischHub.terminPilot.exception.CalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.CalendarAccessTimeoutException
import de.stammtischHub.terminPilot.exception.UnsatisfiableConstraintsException
import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure
import de.stammtischHub.terminPilot.model.generated.SuggestionsRequest
import de.stammtischHub.terminPilot.model.generated.SuggestionsResponse
import de.stammtischHub.terminPilot.service.EventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalTime

@RestController
@RequestMapping("/event")
class EventController(
  private val eventService: EventService,
) : EventApi {
  override fun suggestions(suggestionsRequest: SuggestionsRequest): ResponseEntity<SuggestionsResponse> {
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

    val start = LocalTime.parse(constraints.timeRange.start)
    val end = LocalTime.parse(constraints.timeRange.end)
    val minutesAvailable = Duration.between(start, end).toMinutes()
    return constraints.durationMinutes <= minutesAvailable
  }

  @ExceptionHandler(UnsatisfiableConstraintsException::class)
  fun handleUnsatisfiableConstraints(ex: UnsatisfiableConstraintsException): ResponseEntity<String> =
    ResponseEntity.status(422).body(ex.message)

  @ExceptionHandler(CalendarAccessFailedException::class)
  fun handleCalendarAccessFailed(ex: CalendarAccessFailedException): ResponseEntity<CalendarAccessFailure> =
    ResponseEntity.status(502).body(
      CalendarAccessFailure(participantId = ex.participantId, reason = ex.reason),
    )

  @ExceptionHandler(CalendarAccessTimeoutException::class)
  fun handleCalendarAccessTimeout(ex: CalendarAccessTimeoutException): ResponseEntity<CalendarAccessFailure> =
    ResponseEntity.status(504).body(
      CalendarAccessFailure(participantId = ex.participantId, reason = ex.reason),
    )
}
