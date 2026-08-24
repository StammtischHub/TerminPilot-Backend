package de.stammtischHub.terminPilot.domain

import java.time.LocalDateTime

data class EventDraft(
  val title: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val participantIds: Set<Long>,
  val location: String?,
  val description: String?,
) {
  init {
    require(title.isNotBlank()) { "title must not be blank" }
    require(title.length <= 255) { "title too long" }
    require(start.isBefore(end)) { "start must be before end" }
    require(participantIds.isNotEmpty()) { "participants must not be empty" }
    require(participantIds.distinct().size == participantIds.size) { "duplicate participant ids" }
    require(location == null || location.isNotBlank()) { "location must not be blank if set" }
    require(location == null || location.length < 255) { "location too long" }
    require(description == null || description.isNotBlank()) { "description ust not be blank if set" }
    require(description == null || description.length < 1000) { "description too long" }
  }
}
