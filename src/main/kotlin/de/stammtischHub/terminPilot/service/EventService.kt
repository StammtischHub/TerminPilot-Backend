package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.model.generated.Suggestion
import de.stammtischHub.terminPilot.model.generated.SuggestionsRequest
import org.springframework.stereotype.Service

@Service
class EventService {
  fun suggestEvents(suggestionsRequest: SuggestionsRequest): List<Suggestion> {
    // TODO: Implement
    return emptyList()
  }
}
