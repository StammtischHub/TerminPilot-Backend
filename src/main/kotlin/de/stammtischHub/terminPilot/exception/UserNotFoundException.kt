package de.stammtischHub.terminPilot.exception

/** Thrown when a referenced [de.stammtischHub.terminPilot.persistence.entity.User] does not exist. */
class UserNotFoundException(
  userId: Long,
) : RuntimeException("User with ID $userId not found.")
