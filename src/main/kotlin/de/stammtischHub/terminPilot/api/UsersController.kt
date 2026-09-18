package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.UsersApi
import de.stammtischHub.terminPilot.api.mapping.toUserGroupResponse
import de.stammtischHub.terminPilot.api.mapping.toUserResponse
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.model.generated.UserResponse
import de.stammtischHub.terminPilot.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UsersController(
  private val userService: UserService,
) : UsersApi {
  override fun getUserGroupsByUserId(
    userId: Long,
    userGroupId: Long?,
  ): ResponseEntity<List<UserGroupResponse>> {
    val userGroups = userService.getUserGroupsByUserId(userId, userGroupId)
    return ResponseEntity.ok(userGroups.map { it.toUserGroupResponse() })
  }

  override fun getUsers(): ResponseEntity<List<UserResponse>> {
    val users = userService.getAllUsers()
    return ResponseEntity.ok(users.map { it.toUserResponse() })
  }
}
