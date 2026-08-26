package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity(name = "apple_calendar")
@Table(name = "apple_calendars")
@PrimaryKeyJoinColumn(name = "apple_calendar_id")
class AppleCalendar : Calendar() {
  @NotBlank
  lateinit var icloudMail: String

  @NotBlank
  lateinit var appSpecificPassword: String
}
