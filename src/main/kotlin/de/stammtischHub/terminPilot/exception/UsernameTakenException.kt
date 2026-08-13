package de.stammtischHub.terminPilot.exception

/** Thrown when a username of a [de.stammtischHub.terminPilot.persistence.entity.User] is already used by another username in the system. */
class UsernameTakenException : RuntimeException("Username is already taken")
