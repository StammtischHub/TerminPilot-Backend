package de.stammtischHub.terminPilot.domain

import java.time.LocalDateTime

data class EventDraft(
  val title: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val participantIds: List<Long>,
  val location: String?,
  val notes: String?,
)
