package de.stammtischHub.terminPilot.domain

data class ScoredSlot(
  val slot: TimeSlot,
  val freeParticipantIds: Set<Long>,
  val score: Double,
)
