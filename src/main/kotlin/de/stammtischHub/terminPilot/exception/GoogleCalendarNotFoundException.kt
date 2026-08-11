package de.stammtischHub.terminPilot.exception

/** Thrown when a referenced [de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar] does not exist. */
class GoogleCalendarNotFoundException(
  calendarId: Long,
) : RuntimeException("Google calendar with ID $calendarId not found.")
