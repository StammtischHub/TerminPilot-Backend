package de.stammtischHub.terminPilot.service.mapping

import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.persistence.entity.UserGroup

fun UserGroup.toUserGroupResponse(): UserGroupResponse =
  UserGroupResponse(
    id = this.id,
    name = this.name,
    creator = this.creator!!.toUserDTO(),
    members = this.members.map { it.toUserDTO() },
  )
