package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table

@Entity(name = "AppleCalendar")
@Table(name = "appleCalendars")
@PrimaryKeyJoinColumn(name = "calendar_id")
class AppleCalendar(
  user: User,
  @Column(nullable = false)
  var icloudMail: String,
  @Column(nullable = false)
  var appSpecificPassword: String,
) : Calendar(user) {
  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    } else if (other !is AppleCalendar) {
      return false
    }
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
