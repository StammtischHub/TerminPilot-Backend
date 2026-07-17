package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity(name = "GoogleCalendar")
@Table(name = "googleCalendars")
@PrimaryKeyJoinColumn(name = "google_calendar_id")
class GoogleCalendar(
  user: User? = null,
) : Calendar(user) {
  @NotNull
  @NotBlank
  var calendarName: String? = null

  @NotNull
  @NotBlank
  var accessToken: String? = null

  @NotNull
  @NotBlank
  var refreshToken: String? = null

  @NotNull
  var tokenExpiry: Long? = null
}
