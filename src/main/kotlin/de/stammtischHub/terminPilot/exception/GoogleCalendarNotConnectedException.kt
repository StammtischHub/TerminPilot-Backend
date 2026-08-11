package de.stammtischHub.terminPilot.exception

/** Thrown when a user has not yet completed the Google OAuth authorization flow. */
class GoogleCalendarNotConnectedException(
  userId: Long,
) : RuntimeException("User $userId has not authorized Google Calendar access.")
