package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.exception.UsernameTakenException
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrElse
import org.springframework.security.core.userdetails.User as SecurityUser

@Service
class UserService(
  private val userRepository: UserRepository,
  private val passwordEncoder: PasswordEncoder,
) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    val user = userRepository.findByUsername(username).getOrElse { throw UsernameNotFoundException("User not found") }

    return SecurityUser
      .withUsername(user.username!!)
      .password(user.passwordHash)
      .roles(user.userType.toString())
      .build()
  }

  @Transactional
  fun register(
    username: String,
    rawPassword: String,
  ): User {
    val normalized = username.trim()

    if (userRepository.findByUsername(normalized).isPresent) {
      throw UsernameTakenException()
    }

    val user =
      User().apply {
        this.username = normalized
        passwordHash = passwordEncoder.encode(rawPassword)
        userType = UserType.USER
      }

    return try {
      userRepository.saveAndFlush(user)
    } catch (_: DataIntegrityViolationException) {
      throw UsernameTakenException()
    }
  }
}
