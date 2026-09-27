package de.stammtischHub.terminPilot.exception

/** Thrown when an invalid password is provided while trying to update a user's password. */
class InvalidPasswordException : RuntimeException("Password is invalid.")
