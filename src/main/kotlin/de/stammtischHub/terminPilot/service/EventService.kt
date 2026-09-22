package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Candidate
import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.domain.EventConstraints
import de.stammtischHub.terminPilot.domain.EventDraft
import de.stammtischHub.terminPilot.domain.ScoredSlot
import de.stammtischHub.terminPilot.domain.SlotCoverage
import de.stammtischHub.terminPilot.domain.TimeSlot
import de.stammtischHub.terminPilot.exception.CalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.CalendarAccessTimeoutException
import de.stammtischHub.terminPilot.exception.MultipleCalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.UserNotFoundException
import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure
import de.stammtischHub.terminPilot.model.generated.Coverage
import de.stammtischHub.terminPilot.model.generated.Suggestion
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import de.stammtischHub.terminPilot.provider.CalendarProvider
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@Service
class EventService(
  val userRepository: UserRepository,
  val calendarProvider: CalendarProvider,
) {
  fun createEvent(draft: EventDraft): Event {
    val participants: List<User> =
      draft.participantIds.map { participantId ->
        userRepository
          .findById(participantId)
          .orElseThrow { UserNotFoundException(participantId) }
      }

    // Fail-fast
    verifyAllAccess(participants)

    val event =
      Event(
        draft.title,
        draft.start,
        draft.end,
        participants,
        draft.location,
        draft.description,
      )

    participants.forEach { participant ->
      calendarProvider.writeToCalendar(participant.id, event)
    }
    return event
  }

  fun suggestEvents(constraints: EventConstraints): List<Suggestion> {
    val participants: List<User> =
      constraints.participantIds.map { participantId ->
        userRepository
          .findById(participantId)
          .orElseThrow { UserNotFoundException(participantId) }
      }

    // Fail-fast
    verifyAllAccess(participants)

    val freeSlotsPerParticipant: Map<Long, List<TimeSlot>> =
      participants.associate { participant ->
        participant.id to
          getFreeSlotsForParticipant(
            participant,
            constraints.weekdays,
            constraints.dateRange,
            constraints.timeRange,
          )
      }

    val coverageSegments = intersectFreeSlots(freeSlotsPerParticipant)
    val filteredSegments = filterByDuration(coverageSegments, constraints.duration)

    val scoredSlots =
      scoreFreeSlot(
        filteredSegments,
        constraints.duration,
        constraints.timeRange,
        participants.size,
      )

    val allParticipantIds = participants.map { it.id }.toSet()

    return scoredSlots.map { slot ->
      Suggestion(
        start = slot.slot.start.atOffset(ZoneOffset.UTC), // weil openApi-Problme hat mit LocalDateTime
        end = slot.slot.end.atOffset(ZoneOffset.UTC),
        coverage =
          Coverage(
            totalParticipants = allParticipantIds.size,
            availableParticipantIds = slot.freeParticipantIds.toList(),
            missingParticipantIds = (allParticipantIds - slot.freeParticipantIds).toList(),
          ),
      )
    }
  }

  private fun getFreeSlotsForParticipant(
    user: User,
    weekdays: Set<DayOfWeek>,
    dateRange: ClosedRange<LocalDate>,
    timeRange: ClosedRange<LocalTime>,
  ): List<TimeSlot> {
    val startDateTime = dateRange.start.atTime(timeRange.start)
    val endDateTime = dateRange.endInclusive.atTime(timeRange.endInclusive)

    val busyEvents =
      calendarProvider.getCalendarForTimespan(
        user.id,
        startDateTime,
        endDateTime,
      )

    val mergedBusy =
      mergeIntervals(
        busyEvents.map { TimeSlot(it.start, it.end) },
      )

    return dateRange
      .toList()
      .filter { it.dayOfWeek in weekdays }
      .flatMap { day ->
        val dayStart = day.atTime(timeRange.start)
        val dayEnd = day.atTime(timeRange.endInclusive)
        freeSlotsInWindow(dayStart, dayEnd, mergedBusy)
      }
  }

  private fun mergeIntervals(slots: List<TimeSlot>): List<TimeSlot> {
    if (slots.isEmpty()) return emptyList()

    val sorted = slots.sortedBy { it.start }
    val merged = mutableListOf(sorted.first())

    for (current in sorted.drop(1)) {
      val last = merged.last()
      if (current.start <= last.end) {
        if (current.end > last.end) {
          merged[merged.lastIndex] = last.copy(end = current.end)
        }
      } else {
        merged.add(current)
      }
    }
    return merged
  }

  private fun freeSlotsInWindow(
    windowStart: LocalDateTime,
    windowEnd: LocalDateTime,
    busy: List<TimeSlot>,
  ): List<TimeSlot> {
    val relevant =
      busy
        .filter { it.end > windowStart && it.start < windowEnd }
        .sortedBy { it.start }

    val free = mutableListOf<TimeSlot>()
    var cursor = windowStart

    for ((start, end) in relevant) {
      val busyStart = maxOf(start, windowStart)
      val busyEnd = minOf(end, windowEnd)

      if (busyStart > cursor) {
        free.add(TimeSlot(cursor, busyStart))
      }
      if (busyEnd > cursor) {
        cursor = busyEnd
      }
    }

    if (cursor < windowEnd) {
      free.add(TimeSlot(cursor, windowEnd))
    }

    return free
  }

  private fun intersectFreeSlots(freeSlotsPerParticipant: Map<Long, List<TimeSlot>>): List<SlotCoverage> {
    data class SweepPoint(
      val time: LocalDateTime,
      val participantId: Long,
      val delta: Int,
    )

    val points =
      freeSlotsPerParticipant
        .flatMap { (participantId, slots) ->
          slots.flatMap { slot ->
            listOf(
              SweepPoint(slot.start, participantId, +1),
              SweepPoint(slot.end, participantId, -1),
            )
          }
        }.sortedWith(compareBy({ it.time }, { it.delta }))

    val currentlyFree = mutableSetOf<Long>()
    val result = mutableListOf<SlotCoverage>()
    var segmentStart: LocalDateTime? = null

    for ((time, participantId, delta) in points) {
      if (segmentStart != null && segmentStart != time && currentlyFree.isNotEmpty()) {
        result.add(SlotCoverage(TimeSlot(segmentStart, time), currentlyFree.toSet()))
      }

      if (delta > 0) currentlyFree.add(participantId) else currentlyFree.remove(participantId)
      segmentStart = time
    }

    return result
  }

  private fun filterByDuration(
    segments: List<SlotCoverage>,
    duration: Int,
  ): List<SlotCoverage> {
    val minDuration = Duration.ofMinutes(duration.toLong())
    return segments.filter { Duration.between(it.slot.start, it.slot.end) >= minDuration }
  }

  private fun scoreFreeSlot(
    segments: List<SlotCoverage>,
    duration: Int,
    timeRange: ClosedRange<LocalTime>,
    totalParticipants: Int,
    topN: Int = 5,
  ): List<ScoredSlot> {
    val requiredDuration = Duration.ofMinutes(duration.toLong())

    val fullyCoveredCandidates =
      segments
        .filter { it.freeParticipantIds.size == totalParticipants }
        .flatMap { segment -> generateCandidates(segment, requiredDuration) }

    return fullyCoveredCandidates
      .map { candidate ->
        ScoredSlot(
          candidate.slot,
          candidate.freeParticipantIds,
          scoreCandidate(candidate, requiredDuration, timeRange),
        )
      }.sortedByDescending { it.score }
      .take(topN)
  }

  private fun generateCandidates(
    segment: SlotCoverage,
    requiredDuration: Duration,
  ): List<Candidate> {
    val segmentDuration = Duration.between(segment.slot.start, segment.slot.end)
    val slack = segmentDuration.minus(requiredDuration)

    val earliestStart = segment.slot.start
    val earliest =
      Candidate(
        TimeSlot(earliestStart, earliestStart + requiredDuration),
        segment.freeParticipantIds,
        segment.slot,
      )

    if (slack.isZero) return listOf(earliest)

    val centeredStart = earliestStart + slack.dividedBy(2)
    val centered =
      Candidate(
        TimeSlot(centeredStart, centeredStart + requiredDuration),
        segment.freeParticipantIds,
        segment.slot,
      )

    return listOf(earliest, centered)
  }

  private fun scoreCandidate(
    candidate: Candidate,
    requiredDuration: Duration,
    timeRange: ClosedRange<LocalTime>,
  ): Double {
    // Zentrierung: wie nah am Mittelpunkt des Segments? 1.0 = perfekt zentriert
    val segmentDuration = Duration.between(candidate.segment.start, candidate.segment.end)
    val slack = segmentDuration.minus(requiredDuration)
    val centeringScore =
      if (slack.isZero) {
        1.0
      } else {
        val idealOffset = slack.dividedBy(2).toMinutes()
        val actualOffset = Duration.between(candidate.segment.start, candidate.slot.start).toMinutes()
        1.0 - (kotlin.math.abs(actualOffset - idealOffset).toDouble() / idealOffset).coerceIn(0.0, 1.0)
      }

    // Frühzeitigkeit: Position im Tagesfenster (timeRange), 1.0 = ganz früh am Tag
    val dayWindowMinutes = Duration.between(timeRange.start, timeRange.endInclusive).toMinutes()
    val daySlack = dayWindowMinutes - requiredDuration.toMinutes()
    val earlinessScore =
      if (daySlack <= 0) {
        1.0
      } else {
        val minutesFromDayStart = Duration.between(timeRange.start, candidate.slot.start.toLocalTime()).toMinutes()
        1.0 - (minutesFromDayStart.toDouble() / daySlack).coerceIn(0.0, 1.0)
      }

    return (centeringScore + earlinessScore) / 2.0 // gleichgewichtet
  }

  private fun verifyAllAccess(participants: List<User>) {
    val failures = mutableListOf<CalendarAccessFailure>()

    participants.forEach { participant ->
      try {
        calendarProvider.verifyAccess(participant.id)
      } catch (e: CalendarAccessFailedException) {
        failures += CalendarAccessFailure(participant.id, e.reason)
      } catch (e: CalendarAccessTimeoutException) {
        failures += CalendarAccessFailure(participant.id, e.reason)
      }
    }

    if (failures.isNotEmpty()) {
      throw MultipleCalendarAccessFailedException(failures)
    }
  }

  private fun ClosedRange<LocalDate>.toList(): List<LocalDate> {
    val days = mutableListOf<LocalDate>()
    var d = start
    while (!d.isAfter(endInclusive)) {
      days.add(d)
      d = d.plusDays(1)
    }
    return days
  }
}
