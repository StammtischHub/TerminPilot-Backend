package de.stammtischHub.terminPilot.provider

import de.stammtischHub.terminPilot.domain.Event
import java.time.LocalDateTime

interface CalendarProvider {
  /**
   * Fetches appointments from the Calendar of the given user for the specified time range.
   *
   * @param userId The ID of the user whose calendar is queried.
   * @param start The start of the time range.
   * @param end The end of the time range.
   * @return A list of [Event]s found within the range.
   */
  fun getCalendarForTimespan(
    userId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Event>

  /**
   * Verifies that the given user's calendar is reachable and writable,
   * without creating any event. Used to fail fast before writing to any
   * participant's calendar.
   *
   * @throws [de.stammtischHub.terminPilot.exception.CalendarAccessFailedException] if not connected, auth expired, or provider unavailable.
   * @throws [de.stammtischHub.terminPilot.exception.CalendarAccessTimeoutException] if the check times out.
   */
  fun verifyAccess(userId: Long)

  /**
   * Inserts a new appointment into the Calendar of the given user.
   *
   * @param userId The ID of the user whose calendar is written to.
   * @param event The appointment to be written to the calendar.
   */
  fun writeToCalendar(
    userId: Long,
    event: Event,
  )
}
