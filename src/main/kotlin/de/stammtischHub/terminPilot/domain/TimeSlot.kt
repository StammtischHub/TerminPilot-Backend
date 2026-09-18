package de.stammtischHub.terminPilot.domain

import java.time.LocalDateTime

data class TimeSlot(
  val start: LocalDateTime,
  val end: LocalDateTime,
)
