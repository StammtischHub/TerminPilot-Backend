package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity(name = "cal_dav_calendar")
@Table(name = "cal_dav_calendars")
@PrimaryKeyJoinColumn(name = "cal_dav_calendar_id")
class CalDavCalendar : Calendar() {
  @NotBlank
  lateinit var address: String

  @NotBlank
  lateinit var appSpecificPassword: String
}
