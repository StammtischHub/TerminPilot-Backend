package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
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
  lateinit var calendarName: String

  @Column(columnDefinition = "TEXT")
  @NotBlank
  lateinit var accessToken: String

  @Column(columnDefinition = "TEXT")
  @NotBlank
  lateinit var refreshToken: String

  @Column(name = "token_expiry")
  @NotNull
  private var _tokenExpiry: Long? = null

  var tokenExpiry: Long
    get() = _tokenExpiry ?: error("tokenExpiry wurde noch nicht initialisiert")
    set(value) {
      _tokenExpiry = value
    }
}
