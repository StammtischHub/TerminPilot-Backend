package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.UserGroupsApi
import de.stammtischHub.terminPilot.model.generated.CreateUserGroupRequest
import de.stammtischHub.terminPilot.model.generated.UpdateUserGroupRequest
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.service.UserGroupService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserGroupsController(
  private val userGroupService: UserGroupService,
) : UserGroupsApi {
  override fun createUserGroup(createUserGroupRequest: CreateUserGroupRequest): ResponseEntity<UserGroupResponse> {
    val userGroupResponse =
      userGroupService.createUserGroup(
        createUserGroupRequest.name,
        createUserGroupRequest.creatorId,
        createUserGroupRequest.memberIds,
      )
    return ResponseEntity.ok(userGroupResponse)
  }

  override fun updateUserGroup(
    userGroupId: Long,
    updateUserGroupRequest: UpdateUserGroupRequest,
  ): ResponseEntity<UserGroupResponse> {
    val userGroupResponse =
      userGroupService.updateUserGroup(
        userGroupId,
        updateUserGroupRequest.name,
        updateUserGroupRequest.memberIds,
      )
    return ResponseEntity.ok(userGroupResponse)
  }

  override fun deleteUserGroup(userGroupId: Long): ResponseEntity<Unit> {
    userGroupService.deleteUserGroup(userGroupId)
    return ResponseEntity.ok().build()
  }
}
