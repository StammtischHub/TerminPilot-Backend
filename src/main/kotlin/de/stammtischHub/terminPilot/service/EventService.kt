package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.model.generated.CreateEventRequest
import de.stammtischHub.terminPilot.model.generated.Suggestion
import de.stammtischHub.terminPilot.model.generated.SuggestionsRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService {
  fun createEvent(createEventRequest: CreateEventRequest): Event =
    Event("", LocalDateTime.now(), LocalDateTime.now(), emptyList(), "", "")

  fun suggestEvents(suggestionsRequest: SuggestionsRequest): List<Suggestion> {
    // TODO: Implement
    return emptyList()
  }
}
