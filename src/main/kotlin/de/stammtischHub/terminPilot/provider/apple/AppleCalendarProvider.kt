package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.provider.CalendarProvider
import java.time.LocalDateTime

class AppleCalendarProvider : CalendarProvider {
  override fun getCalendarForTimespan(
    userId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Event> {
    TODO("Not yet implemented")
  }

  override fun writeToCalendar(
    userId: Long,
    event: Event,
  ) {
    TODO("Not yet implemented")
  }
}
