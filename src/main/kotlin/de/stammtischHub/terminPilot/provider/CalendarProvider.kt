package de.stammtischHub.terminPilot.provider

import java.time.LocalDateTime
import de.stammtischHub.terminPilot.domain.Appointment

interface CalendarProvider {
    /**
     * Gets the calendar-entries from a user for the given timespan.
     */
    fun getCalendarForTimespan(start: LocalDateTime, end: LocalDateTime): List<Appointment>

    /**
     * Writes the given appointment to the users calendar.
     */
    fun writeToCalendar(appointment: Appointment)
}
