package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.domain.EventConstraints
import de.stammtischHub.terminPilot.domain.EventDraft
import de.stammtischHub.terminPilot.exception.CalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.CalendarAccessTimeoutException
import de.stammtischHub.terminPilot.exception.MultipleCalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.UserNotFoundException
import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure
import de.stammtischHub.terminPilot.model.generated.Suggestion
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import de.stammtischHub.terminPilot.provider.CalendarProvider
import org.springframework.stereotype.Service

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
      calendarProvider.writeToCalendar(participant.id!!, event)
    }
    return event
  }

  fun suggestEvents(constraints: EventConstraints): List<Suggestion> {
    // TODO: Implement
    return emptyList()
  }

  private fun verifyAllAccess(participants: List<User>) {
    val failures = mutableListOf<CalendarAccessFailure>()

    participants.forEach { participant ->
      try {
        calendarProvider.verifyAccess(participant.id!!)
      } catch (e: CalendarAccessFailedException) {
        failures += CalendarAccessFailure(participant.id!!, e.reason)
      } catch (e: CalendarAccessTimeoutException) {
        failures += CalendarAccessFailure(participant.id!!, e.reason)
      }
    }

    if (failures.isNotEmpty()) {
      throw MultipleCalendarAccessFailedException(failures)
    }
  }
}
