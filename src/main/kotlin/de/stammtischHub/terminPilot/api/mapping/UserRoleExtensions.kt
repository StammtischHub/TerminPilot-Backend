package de.stammtischHub.terminPilot.api.mapping

import de.stammtischHub.terminPilot.model.generated.UserRole
import de.stammtischHub.terminPilot.persistence.entity.UserType

fun String.toUserRole(): UserRole? =
  when (this) {
    "ADMIN" -> UserRole.admin
    "USER" -> UserRole.user
    else -> null
  }

fun UserType.toUserRole(): UserRole? =
  when (this) {
    UserType.ADMIN -> UserRole.admin
    UserType.USER -> UserRole.user
  }
