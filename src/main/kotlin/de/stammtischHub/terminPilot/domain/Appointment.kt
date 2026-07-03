package de.stammtischHub.terminPilot.domain

import java.time.LocalDateTime

data class Appointment(
  val title: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: String,
  val description: String,
)
