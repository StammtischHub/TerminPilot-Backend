package de.stammtischHub.terminPilot.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
  private val logger = LoggerFactory.getLogger(javaClass)

  @ExceptionHandler(UnsatisfiableConstraintsException::class)
  fun handleUnsatisfiableConstraints(ex: UnsatisfiableConstraintsException): ProblemDetail {
    logger.debug("Unsatisfiable constraints exception: ${ex.message}, ${ex.cause}")
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.UNPROCESSABLE_ENTITY,
      ex.message ?: "Unsatisfiable constraints",
    )
  }

  @ExceptionHandler(MultipleCalendarAccessFailedException::class)
  fun handleMultipleCalendarAccessFailed(ex: MultipleCalendarAccessFailedException): ProblemDetail {
    logger.debug("Calendar access failed for multiple participants: {}", ex.failures)
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.BAD_GATEWAY,
      "Calendar access failed for one or more participants",
    ).apply {
      setProperty("failures", ex.failures)
    }
  }

  @ExceptionHandler(CalendarAccessFailedException::class)
  fun handleCalendarAccessFailed(ex: CalendarAccessFailedException): ProblemDetail {
    logger.debug("Calendar access failure: ${ex.message}")
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.reason.value).apply {
      setProperty("participantId", ex.participantId)
    }
  }

  @ExceptionHandler(CalendarAccessTimeoutException::class)
  fun handleCalendarAccessTimeout(ex: CalendarAccessTimeoutException): ProblemDetail {
    logger.debug("Calendar access timeout exception: ${ex.message}")
    return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.reason.value).apply {
      setProperty("participantId", ex.participantId)
    }
  }

  @ExceptionHandler(UserNotFoundException::class)
  fun handleUserNotFound(ex: UserNotFoundException): ProblemDetail {
    logger.debug("User not found: ${ex.message}")
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "User not found")
  }

  @ExceptionHandler(AuthenticationException::class)
  fun handleAuthenticationFailure(): ProblemDetail {
    logger.info("Authentication failure")
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials")
  }

  @ExceptionHandler(UsernameTakenException::class)
  fun handleUsernameTaken(): ProblemDetail {
    logger.info("Username taken")
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username is already taken")
  }

  @ExceptionHandler(Exception::class)
  fun handleUnexpected(exception: Exception): ProblemDetail {
    logger.error("Unhandled exception", exception)
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "An unexpected error occurred",
    )
  }
}
