package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.api.generated.GoogleApi
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleOAuthService
import de.stammtischHub.terminPilot.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class GoogleController(
  private val googleOAuthService: GoogleOAuthService,
) : GoogleApi {

  override fun authorize(): ResponseEntity<Unit> {
    val authentication = SecurityContextHolder.getContext().authentication
    val principal = authentication!!.principal as? UserPrincipal
    val authorizationUrl = googleOAuthService.buildAuthorizationUrl(principal!!.id)

    return ResponseEntity
      .status(HttpStatus.FOUND)
      .location(URI.create(authorizationUrl))
      .body(null)
  }

  override fun callback(code: String, state: String): ResponseEntity<Unit> {
    val userId = state.toLong()
    googleOAuthService.handleCallback(code, userId)

    return ResponseEntity.ok(null)
  }
}
