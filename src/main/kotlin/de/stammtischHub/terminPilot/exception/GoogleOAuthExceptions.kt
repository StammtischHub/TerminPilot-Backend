package de.stammtischHub.terminPilot.exception

/** Thrown when a referenced Google calendar entity does not exist. */
class GoogleCalendarNotFoundException(
  calendarId: Long,
) : RuntimeException("Google calendar with ID $calendarId not found.")

/** Thrown when a user has not yet completed the Google OAuth authorization flow. */
class GoogleCalendarNotConnectedException(
  userId: Long,
) : RuntimeException("User $userId has not authorized Google Calendar access.")
