package de.stammtischHub.terminPilot.exception

import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure

/**
 * Calendar access failed for at least one participant
 * (not connected, authorization expired, or provider error)
 */
class CalendarAccessFailedException(
  val participantId: Long,
  val reason: CalendarAccessFailure.Reason,
) : RuntimeException("Calendar access failed for participant $participantId: $reason")
