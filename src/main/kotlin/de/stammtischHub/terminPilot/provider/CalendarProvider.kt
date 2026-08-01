package de.stammtischHub.terminPilot.provider

import de.stammtischHub.terminPilot.domain.Appointment
import java.time.LocalDateTime

interface CalendarProvider {
  /**
   * Fetches appointments from the Calendar of the given user for the specified time range.
   *
   * @param userId The ID of the user whose calendar is queried.
   * @param start The start of the time range.
   * @param end The end of the time range.
   * @return A list of [Appointment]s found within the range.
   */
  fun getCalendarForTimespan(
    userId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Appointment>

  /**
   * Inserts a new appointment into the Calendar of the given user.
   *
   * @param userId The ID of the user whose calendar is written to.
   * @param appointment The appointment to be written to the calendar.
   */
  fun writeToCalendar(
    userId: Long,
    appointment: Appointment,
  )
}
