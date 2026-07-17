package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.jetbrains.annotations.NotNull

@Entity(name = "AppleCalendar")
@Table(name = "appleCalendars")
@PrimaryKeyJoinColumn(name = "apple_calendar_id")
class AppleCalendar(
  user: User? = null,
) : Calendar(user) {
  @NotNull
  @NotBlank
  var icloudMail: String? = null

  @NotNull
  @NotBlank
  var appSpecificPassword: String? = null
}
