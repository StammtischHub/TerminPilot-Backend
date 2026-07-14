package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table

@Entity(name = "GoogleCalendar")
@Table(name = "googleCalendars")
@PrimaryKeyJoinColumn(name = "calendar_id")
class GoogleCalendar(
  user: User,
  @Column(nullable = false)
  var calendarName: String,
  @Column(nullable = false)
  var accessToken: String,
  @Column(nullable = false)
  var refreshToken: String,
  @Column(nullable = false)
  var tokenExpiry: Long,
) : Calendar(user) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    else if (other !is GoogleCalendar) return false
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
