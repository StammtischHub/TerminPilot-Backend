package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.UsersApi
import de.stammtischHub.terminPilot.model.generated.UpdatePasswordRequest
import de.stammtischHub.terminPilot.model.generated.UpdateUsernameRequest
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
    val userGroupResponses = userService.getUserGroupsByUserId(userId, userGroupId)
    return ResponseEntity.ok(userGroupResponses)
  }

  override fun getUsers(): ResponseEntity<List<UserResponse>> {
    val users = userService.getAllUsers()
    return ResponseEntity.ok(users)
  }

  override fun updateUsername(
    userId: Long,
    updateUsernameRequest: UpdateUsernameRequest,
  ): ResponseEntity<UserResponse> {
    val userResponse = userService.updateUsername(userId, updateUsernameRequest.username)
    return ResponseEntity.ok(userResponse)
  }

  override fun updatePassword(
    userId: Long,
    updatePasswordRequest: UpdatePasswordRequest,
  ): ResponseEntity<Unit> {
    userService.updatePassword(userId, updatePasswordRequest.oldPassword, updatePasswordRequest.newPassword)
    return ResponseEntity.ok().build()
  }
}
