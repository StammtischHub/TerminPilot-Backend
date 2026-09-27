package de.stammtischHub.terminPilot.exception

/** Thrown when a referenced [de.stammtischHub.terminPilot.persistence.entity.UserGroup] does not exist. */
class UserGroupNotFoundException(
  userGroupId: Long,
) : RuntimeException("UserGroup with ID '$userGroupId' not found.")
