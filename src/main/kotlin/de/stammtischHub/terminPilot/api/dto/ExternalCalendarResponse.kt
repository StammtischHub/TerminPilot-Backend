package de.stammtischHub.terminPilot.api.dto

import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import java.util.UUID

data class ExternalCalendarResponse(
  val id: UUID,
  val displayName: String,
  val selected: Boolean,
) {
  companion object {
    fun from(calendar: ExternalCalendar) =
      ExternalCalendarResponse(
        id = calendar.publicId,
        displayName = calendar.displayName,
        selected = calendar.selected,
      )
  }
}
