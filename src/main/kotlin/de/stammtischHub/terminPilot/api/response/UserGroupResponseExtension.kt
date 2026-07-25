package de.stammtischHub.terminPilot.api.response

import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.persistence.entity.UserGroup

fun UserGroup.toUserGroupResponse(): UserGroupResponse =
  UserGroupResponse(
    id = this.id!!,
    name = this.name,
    members = this.members.map { it.id!! },
  )
