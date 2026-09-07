package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.UserGroupsApi
import de.stammtischHub.terminPilot.api.mapping.toUserGroupResponse
import de.stammtischHub.terminPilot.model.generated.CreateUserGroupRequest
import de.stammtischHub.terminPilot.model.generated.UpdateUserGroupRequest
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.service.UserGroupService
import de.stammtischHub.terminPilot.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UserGroupsController(
  private val userGroupService: UserGroupService,
  private val userService: UserService,
) : UserGroupsApi {
  override fun createUserGroup(createUserGroupRequest: CreateUserGroupRequest): ResponseEntity<UserGroupResponse> {
    val creator = userService.getUserByUserId(createUserGroupRequest.creatorId)
    val members = createUserGroupRequest.memberIds.map { userService.getUserByUserId(it) }.toMutableSet()

    val userGroup =
      userGroupService.createUserGroup(
        createUserGroupRequest.name,
        creator,
        members,
      )
    return ResponseEntity.ok(userGroup.toUserGroupResponse())
  }

  override fun updateUserGroup(
    userGroupId: Long,
    updateUserGroupRequest: UpdateUserGroupRequest,
  ): ResponseEntity<UserGroupResponse> {
    val users =
      updateUserGroupRequest.memberIds?.map { userService.getUserByUserId(it) }?.toMutableSet()

    val userGroup =
      userGroupService.updateUserGroup(
        userGroupId,
        updateUserGroupRequest.name,
        users,
      )
    return ResponseEntity.ok(userGroup.toUserGroupResponse())
  }

  override fun deleteUserGroup(userGroupId: Long): ResponseEntity<Unit> {
    userGroupService.deleteUserGroup(userGroupId)
    return ResponseEntity.ok().build()
  }
}
