package de.stammtischHub.terminPilot.api.response

import de.stammtischHub.terminPilot.model.generated.UserResponse
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.security.UserPrincipal
import org.springframework.security.core.Authentication

fun User.toUserResponse(): UserResponse =
  UserResponse(
    id = id!!,
    username = username,
    roles = listOfNotNull(userType.toUserRole()),
  )

fun Authentication.toUserResponse(): UserResponse {
  val principal =
    principal as? UserPrincipal
      ?: error("Unerwarteter Principal-Typ: ${principal!!::class}")

  return UserResponse(
    id = principal.id,
    username = principal.username,
    roles = authorities.mapNotNull { it.authority?.toUserRole() },
  )
}
