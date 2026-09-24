package de.stammtischHub.terminPilot.domain

data class SlotCoverage(
  val slot: TimeSlot,
  val freeParticipantIds: Set<Long>,
)
