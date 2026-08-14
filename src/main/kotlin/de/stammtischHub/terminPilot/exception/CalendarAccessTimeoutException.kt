package de.stammtischHub.terminPilot.exception

import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure

/**
 * Timed out while fetching a participant's calendar
 */
class CalendarAccessTimeoutException(
  val participantId: Long,
  val reason: CalendarAccessFailure.Reason = CalendarAccessFailure.Reason.timeout,
) : RuntimeException("Calendar access timed out for participant $participantId")
