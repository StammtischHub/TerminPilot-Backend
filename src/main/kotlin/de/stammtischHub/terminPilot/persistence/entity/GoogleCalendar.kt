package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity(name = "GoogleCalendar")
@Table(name = "googleCalendars")
@PrimaryKeyJoinColumn(name = "google_calendar_id")
class GoogleCalendar : Calendar() {
  @NotBlank
  var calendarName: String = ""

  @NotBlank
  var accessToken: String = ""

  @NotBlank
  var refreshToken: String = ""

  @NotNull
  var tokenExpiry: Long? = null
}
