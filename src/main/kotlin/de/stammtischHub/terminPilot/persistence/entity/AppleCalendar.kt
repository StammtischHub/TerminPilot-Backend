package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity(name = "AppleCalendar")
@Table(name = "appleCalendars")
@PrimaryKeyJoinColumn(name = "apple_calendar_id")
class AppleCalendar : Calendar() {
  @NotBlank
  var icloudMail: String = ""

  @NotBlank
  var appSpecificPassword: String = ""
}
