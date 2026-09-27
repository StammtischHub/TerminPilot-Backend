package de.stammtischHub.terminPilot.exception

/** Thrown when the name of a [de.stammtischHub.terminPilot.persistence.entity.UserGroup] is already used by the creator. */
class UserGroupNameTakenException : RuntimeException("UserGroup name is already taken")
