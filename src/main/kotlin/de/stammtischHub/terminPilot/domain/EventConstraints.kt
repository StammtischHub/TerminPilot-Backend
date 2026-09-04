package de.stammtischHub.terminPilot.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class EventConstraints(
  val weekdays: Set<DayOfWeek>,
  val dateRange: ClosedRange<LocalDate>,
  val timeRange: ClosedRange<LocalTime>,
  val duration: Int,
  val participantIds: Set<Long>,
) {
  init {
    require(weekdays.isNotEmpty()) { "weekdays must not be empty" }
    require(dateRange.endInclusive.isAfter(dateRange.start)) { "date range invalid" }
    require(timeRange.start < timeRange.endInclusive) { "time range invalid" }
    require(duration >= 1) { "duration must be greater than 1 minute" }
    require(participantIds.isNotEmpty()) { "participants must not be empty" }
    require(duration <= Duration.between(timeRange.start, timeRange.endInclusive).toMinutes()) {
      "duration exceeds available time range"
    }
  }
}
