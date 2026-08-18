package de.stammtischHub.terminPilot.domain

import de.stammtischHub.terminPilot.persistence.entity.User
import java.time.LocalDateTime

data class Event(
  val title: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val participants: List<User>,
  val location: String,
  val description: String,
)
