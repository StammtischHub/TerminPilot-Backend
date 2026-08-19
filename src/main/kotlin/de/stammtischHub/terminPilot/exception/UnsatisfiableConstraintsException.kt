package de.stammtischHub.terminPilot.exception

/** Thrown when the give constraints are logically not processable, e.g. end- before start-time. */
class UnsatisfiableConstraintsException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message)
