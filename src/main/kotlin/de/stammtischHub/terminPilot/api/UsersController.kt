package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.UsersApi
import de.stammtischHub.terminPilot.api.response.toUserGroupResponse
import de.stammtischHub.terminPilot.api.response.toUserResponse
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.model.generated.UserResponse
import de.stammtischHub.terminPilot.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class UsersController(
  private val userService: UserService,
) : UsersApi {
  override fun getUserGroupsByUserId(userId: Long): ResponseEntity<List<UserGroupResponse>> {
    val userGroups = userService.getAllUserGroupsByUserId(userId)
    return ResponseEntity.ok(userGroups.map { it.toUserGroupResponse() })
  }

  override fun getUsers(): ResponseEntity<List<UserResponse>> {
    val users = userService.getAllUsers()
    return ResponseEntity.ok(users.map { it.toUserResponse() })
  }
}
