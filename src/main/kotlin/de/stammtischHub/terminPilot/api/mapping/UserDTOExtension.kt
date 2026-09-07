package de.stammtischHub.terminPilot.api.mapping

import de.stammtischHub.terminPilot.model.generated.User as UserDTO
import de.stammtischHub.terminPilot.persistence.entity.User as UserEntity

fun UserEntity.toUserDTO(): UserDTO =
  UserDTO(
    id = id,
    name = username,
  )
