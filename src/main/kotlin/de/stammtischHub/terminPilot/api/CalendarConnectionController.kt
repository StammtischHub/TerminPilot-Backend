package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.dto.AppleConnectRequest
import de.stammtischHub.terminPilot.api.dto.CalendarConnectionResponse
import de.stammtischHub.terminPilot.api.dto.ExternalCalendarResponse
import de.stammtischHub.terminPilot.api.dto.ExternalCalendarUpdateRequest
import de.stammtischHub.terminPilot.security.UserPrincipal
import de.stammtischHub.terminPilot.service.CalendarConnectionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CalendarConnectionController(
  private val calendarConnectionService: CalendarConnectionService,
) {
  @PostMapping("/api/calendar-connections/apple")
  fun connectApple(
    @Valid @RequestBody request: AppleConnectRequest,
  ): ResponseEntity<CalendarConnectionResponse> {
    val connection =
      calendarConnectionService.connectAppleCalendar(
        userId = currentUserId(),
        email = request.email,
        appSpecificPassword = request.appSpecificPassword,
      )
    return ResponseEntity.status(HttpStatus.CREATED).body(CalendarConnectionResponse.from(connection))
  }

  @GetMapping("/api/calendar-connections")
  fun listConnections(): ResponseEntity<List<CalendarConnectionResponse>> {
    val connections =
      calendarConnectionService
        .listConnections(currentUserId())
        .map { CalendarConnectionResponse.from(it) }
    return ResponseEntity.ok(connections)
  }

  @DeleteMapping("/api/calendar-connections/{connectionId}")
  fun disconnect(
    @PathVariable connectionId: UUID,
  ): ResponseEntity<Void> {
    calendarConnectionService.disconnect(currentUserId(), connectionId)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/api/calendar-connections/{connectionId}/sync")
  fun triggerSync(
    @PathVariable connectionId: UUID,
  ): ResponseEntity<CalendarConnectionResponse> {
    val connection = calendarConnectionService.triggerSync(currentUserId(), connectionId)
    return ResponseEntity.ok(CalendarConnectionResponse.from(connection))
  }

  @GetMapping("/api/calendar-connections/{connectionId}/calendars")
  fun listCalendars(
    @PathVariable connectionId: UUID,
  ): ResponseEntity<List<ExternalCalendarResponse>> {
    val calendars =
      calendarConnectionService
        .listCalendars(currentUserId(), connectionId)
        .map { ExternalCalendarResponse.from(it) }
    return ResponseEntity.ok(calendars)
  }

  @PatchMapping("/api/calendar-connections/{connectionId}/calendars/{calendarId}")
  fun updateCalendar(
    @PathVariable connectionId: UUID,
    @PathVariable calendarId: UUID,
    @RequestBody request: ExternalCalendarUpdateRequest,
  ): ResponseEntity<ExternalCalendarResponse> {
    val calendar =
      calendarConnectionService.updateCalendarSelection(
        userId = currentUserId(),
        connectionId = connectionId,
        calendarId = calendarId,
        selected = request.selected,
      )
    return ResponseEntity.ok(ExternalCalendarResponse.from(calendar))
  }

  private fun currentUserId(): Long {
    val principal =
      SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal
        ?: throw AuthenticationCredentialsNotFoundException("Not authenticated")
    return principal.id
  }
}
