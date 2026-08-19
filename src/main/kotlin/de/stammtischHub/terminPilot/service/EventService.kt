package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.domain.EventConstraints
import de.stammtischHub.terminPilot.domain.EventDraft
import de.stammtischHub.terminPilot.model.generated.Suggestion
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventService {
  fun createEvent(draft: EventDraft): Event = Event("", LocalDateTime.now(), LocalDateTime.now(), emptyList(), "", "")

  fun suggestEvents(constraints: EventConstraints): List<Suggestion> {
    // TODO: Implement
    return emptyList()
  }
}
