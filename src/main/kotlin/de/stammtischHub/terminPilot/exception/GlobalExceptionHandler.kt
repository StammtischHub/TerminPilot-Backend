package de.stammtischHub.terminPilot.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
  private val log = LoggerFactory.getLogger(javaClass)

  @ExceptionHandler(UserNotFoundException::class)
  fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<String> =
    ResponseEntity.status(404).body(ex.message)

  @ExceptionHandler(AuthenticationException::class)
  fun onAuthenticationFailure(): ProblemDetail =
    ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials")

  @ExceptionHandler(UsernameTakenException::class)
  fun onUsernameTaken(): ProblemDetail =
    ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Username is already taken")

  @ExceptionHandler(Exception::class)
  fun onUnexpected(exception: Exception): ProblemDetail {
    log.error("Unhandled exception", exception)
    return ProblemDetail.forStatusAndDetail(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "An unexpected error occurred",
    )
  }
}
