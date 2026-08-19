package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.domain.EventConstraints
import de.stammtischHub.terminPilot.domain.EventDraft
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
          .orElseThrow { NoSuchElementException("User with id $participantId not found") }
      }

    val event =
      Event(
        draft.title,
        draft.start,
        draft.end,
        participants,
        draft.location,
        draft.description,
      )

    participants.forEach { participant -> calendarProvider.writeToCalendar(participant.id!!, event) }
    return event
  }

  fun suggestEvents(constraints: EventConstraints): List<Suggestion> {
    // TODO: Implement
    return emptyList()
  }
}
