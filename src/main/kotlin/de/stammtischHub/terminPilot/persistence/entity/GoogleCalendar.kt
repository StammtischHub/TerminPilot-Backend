package de.stammtischHub.terminPilot.persistence.entity

import de.stammtischHub.terminPilot.persistence.converter.EncryptedStringConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity(name = "google_calendar")
@Table(name = "google_calendars")
@PrimaryKeyJoinColumn(name = "google_calendar_id")
class GoogleCalendar : Calendar() {
  @NotBlank
  lateinit var calendarName: String

  @Column(columnDefinition = "TEXT")
  @Convert(converter = EncryptedStringConverter::class)
  @NotBlank
  lateinit var accessToken: String

  @Column(columnDefinition = "TEXT")
  @Convert(converter = EncryptedStringConverter::class)
  @NotBlank
  lateinit var refreshToken: String

  @Column(name = "token_expiry")
  @NotNull
  private var _tokenExpiry: Long? = 0L

  var tokenExpiry: Long
    get() = _tokenExpiry ?: error("tokenExpiry wurde noch nicht initialisiert")
    set(value) {
      _tokenExpiry = value
    }
}
