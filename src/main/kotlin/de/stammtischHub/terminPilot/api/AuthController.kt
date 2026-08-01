package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.AuthApi
import de.stammtischHub.terminPilot.model.generated.LoginRequest
import de.stammtischHub.terminPilot.model.generated.RegisterRequest
import de.stammtischHub.terminPilot.model.generated.UserResponse
import de.stammtischHub.terminPilot.model.generated.UserRole
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import de.stammtischHub.terminPilot.security.UserPrincipal
import de.stammtischHub.terminPilot.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
  private val authenticationManager: AuthenticationManager,
  private val securityContextRepository: SecurityContextRepository,
  private val request: HttpServletRequest,
  private val response: HttpServletResponse,
  private val userService: UserService,
) : AuthApi {
  private val csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()

  override fun register(registerRequest: RegisterRequest): ResponseEntity<UserResponse> {
    val user =
      userService.register(
        registerRequest.username,
        registerRequest.password,
      )
    return ResponseEntity.status(HttpStatus.CREATED).body(user.toUserResponse())
  }

  override fun login(loginRequest: LoginRequest): ResponseEntity<UserResponse> {
    val authentication =
      authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(
          loginRequest.username,
          loginRequest.password,
        ),
      )

    val newToken = csrfTokenRepository.generateToken(request)
    csrfTokenRepository.saveToken(newToken, request, response)

    request.getSession(true)
    request.changeSessionId()

    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = authentication
    SecurityContextHolder.setContext(context)
    securityContextRepository.saveContext(context, request, response)

    return ResponseEntity.ok(authentication.toUserResponse())
  }

  override fun logout(): ResponseEntity<Unit> {
    SecurityContextLogoutHandler().logout(
      request,
      response,
      SecurityContextHolder.getContext().authentication,
    )

    csrfTokenRepository.saveToken(null, request, response)
    return ResponseEntity.noContent().build()
  }

  override fun me(): ResponseEntity<UserResponse> {
    val authentication = SecurityContextHolder.getContext().authentication
    return ResponseEntity.ok(authentication?.toUserResponse())
  }

  private fun User.toUserResponse() =
    UserResponse(
      id = id!!,
      username = username,
      roles = listOfNotNull(userType.toUserRole()),
    )

  private fun UserType.toUserRole(): UserRole? =
    when (this) {
      UserType.ADMIN -> UserRole.admin
      UserType.USER -> UserRole.user
    }

  private fun Authentication.toUserResponse(): UserResponse {
    val principal =
      principal as? UserPrincipal
        ?: error("Unerwarteter Principal-Typ: ${principal!!::class}")

    return UserResponse(
      id = principal.id,
      username = principal.username,
      roles = authorities.mapNotNull { it.authority?.toUserRole() },
    )
  }

  private fun String.toUserRole(): UserRole? =
    when (this) {
      "ADMIN" -> UserRole.admin
      "USER" -> UserRole.user
      else -> null
    }
}
