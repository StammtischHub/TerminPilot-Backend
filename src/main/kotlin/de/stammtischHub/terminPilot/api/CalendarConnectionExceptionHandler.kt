package de.stammtischHub.terminPilot.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class CalendarConnectionExceptionHandler {
  @ExceptionHandler(NoSuchElementException::class)
  fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message ?: "Not found"))

  @ExceptionHandler(IllegalStateException::class)
  fun handleConflict(ex: IllegalStateException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(ErrorResponse(HttpStatus.CONFLICT.value(), ex.message ?: "Conflict"))

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "Bad request"))

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> =
    ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.message ?: "Access denied"))

  data class ErrorResponse(
    val status: Int,
    val message: String,
  )
}
