package de.stammtischHub.terminPilot.service.mapping

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.model.generated.User as UserDTO

fun User.toUserDTO(): UserDTO =
  UserDTO(
    id = id,
    name = username,
  )
