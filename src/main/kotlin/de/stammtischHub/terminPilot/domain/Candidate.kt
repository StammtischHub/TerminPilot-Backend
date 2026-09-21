package de.stammtischHub.terminPilot.domain

data class Candidate(
  val slot: TimeSlot,
  val freeParticipantIds: Set<Long>,
  val segment: TimeSlot,
)
