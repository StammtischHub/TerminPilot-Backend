package de.stammtischHub.terminPilot.exception

import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure

/**
 * Calendar access failed for one or more participants; contains one entry per affected participant
 * so the frontend can report all problems at once instead of one at a time.
 */
class MultipleCalendarAccessFailedException(
  val failures: List<CalendarAccessFailure>,
) : RuntimeException(
  "Calendar access failed for participants: ${failures.joinToString { "${it.participantId}(${it.reason})" }}",
)
