package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.exception.UserNotFoundException
import de.stammtischHub.terminPilot.exception.UsernameTakenException
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.model.generated.UserResponse
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import de.stammtischHub.terminPilot.security.UserPrincipal
import de.stammtischHub.terminPilot.service.mapping.toUserGroupResponse
import de.stammtischHub.terminPilot.service.mapping.toUserResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrElse

@Service
class UserService(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    val user = userRepository.findByUsername(username).getOrElse { throw UsernameNotFoundException("User not found") }
    return UserPrincipal(user)
  }

  @Transactional
  fun register(
    username: String,
    rawPassword: String,
  ): UserResponse {
    val normalizedUsername = username.trim()

    if (userRepository.findByUsername(normalizedUsername).isPresent) {
      throw UsernameTakenException()
    }

    val user =
      User().apply {
        this.username = normalizedUsername
        password = passwordEncoder.encode(rawPassword).toString()
        userType = UserType.USER
      }

    return try {
      val savedUser = userRepository.saveAndFlush(user)
      savedUser.toUserResponse()
    } catch (_: DataIntegrityViolationException) {
      throw UsernameTakenException()
    }
  }

  @Transactional(readOnly = true)
  fun getUserGroupsByUserId(
    userId: Long,
    userGroupId: Long?,
  ): List<UserGroupResponse> {
    val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
    val userGroups =
      userGroupId?.let { userGroupId ->
        user.userGroups.filter { it.id == userGroupId }
      } ?: user.userGroups.toList()
    return userGroups.map { it.toUserGroupResponse() }
  }

  @Transactional(readOnly = true)
  fun getUserByUserId(userId: Long): User =
    userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

  @Transactional(readOnly = true)
  fun getAllUsers(): List<UserResponse> = userRepository.findAll().map { it.toUserResponse() }
}
