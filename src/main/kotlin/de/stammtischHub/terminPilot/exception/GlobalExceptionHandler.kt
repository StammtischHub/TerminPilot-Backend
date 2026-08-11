package de.stammtischHub.terminPilot.exception

import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
  private val logger = LoggerFactory.getLogger(javaClass)

  @ExceptionHandler(UnsatisfiableConstraintsException::class)
  fun handleUnsatisfiableConstraints(ex: UnsatisfiableConstraintsException): ResponseEntity<String> {
    logger.debug("Unsatisfiable constraints exception: ${ex.message}")
    return ResponseEntity.status(422).body(ex.message)
  }

  @ExceptionHandler(CalendarAccessFailedException::class)
  fun handleCalendarAccessFailed(ex: CalendarAccessFailedException): ResponseEntity<CalendarAccessFailure> {
    logger.debug("Calendar access failure: ${ex.message}")
    return ResponseEntity.status(502).body(
      CalendarAccessFailure(participantId = ex.participantId, reason = ex.reason),
    )
  }

  @ExceptionHandler(CalendarAccessTimeoutException::class)
  fun handleCalendarAccessTimeout(ex: CalendarAccessTimeoutException): ResponseEntity<CalendarAccessFailure> {
    logger.debug("Calendar access timeout exception: ${ex.message}")
    return ResponseEntity.status(504).body(
      CalendarAccessFailure(participantId = ex.participantId, reason = ex.reason),
    )
  }

  @ExceptionHandler(UserNotFoundException::class)
  fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<String> {
    logger.debug("User not found: ${ex.message}")
    return ResponseEntity.status(404).body(ex.message)
  }

  @ExceptionHandler(AuthenticationException::class)
  fun onAuthenticationFailure(): ProblemDetail {
    logger.info("Authentication failure")
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials")
  }

  @ExceptionHandler(UsernameTakenException::class)
  fun onUsernameTaken(): ProblemDetail {
    logger.info("Username taken")
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username is already taken")
  }

  @ExceptionHandler(Exception::class)
  fun onUnexpected(exception: Exception): ProblemDetail {
    logger.error("Unhandled exception", exception)
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "An unexpected error occurred",
    )
  }
}
