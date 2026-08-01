package de.stammtischHub.terminPilot.provider.google.oauth

import de.stammtischHub.terminPilot.security.UserPrincipal
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// TODO: Move into ApiSpec!

/**
 * REST controller providing Google OAuth 2.0 authorization endpoints.
 *
 * Endpoints:
 * - `GET /api/google/oauth/authorize`
 *     Starts the OAuth flow by redirecting the authenticated user to Google's consent screen.
 *
 * - `GET /api/google/oauth/callback`
 *     Handles Google's redirect, exchanges the authorization code for tokens,
 *     and persists them for the authenticated user.
 *
 * The authenticated user's ID is embedded into the OAuth `state` parameter to ensure
 * the callback can associate the returned tokens with the correct account.
 */
@RestController
@RequestMapping("/api/google/oauth")
class GoogleOAuthController(
  private val googleOAuthService: GoogleOAuthService,
) {
  /**
   * Initiates the Google OAuth 2.0 authorization flow.
   *
   * The authenticated user's ID is encoded into the OAuth `state` parameter.
   * The browser is then redirected to Google's OAuth consent screen.
   *
   * @param userPrincipal The authenticated user initiating the OAuth connection.
   * @param response The HTTP response used to perform the redirect.
   */
  @GetMapping("/authorize")
  fun authorize(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
    response: HttpServletResponse,
  ) {
    val authorizationUrl = googleOAuthService.buildAuthorizationUrl(userPrincipal.id)
    response.sendRedirect(authorizationUrl)
  }

  /**
   * Processes Google's OAuth callback.
   *
   * Google returns:
   * - `code`: a one-time authorization code
   * - `state`: the user ID encoded during `/authorize`
   *
   * The authorization code is exchanged for access/refresh tokens,
   * which are then stored for the corresponding user.
   *
   * @param code The authorization code issued by Google.
   * @param state The encoded user ID used to associate the tokens with the correct account.
   * @return A confirmation payload containing a success message and the user ID.
   */
  @GetMapping("/callback")
  fun callback(
    @RequestParam code: String,
    @RequestParam state: String,
  ): GoogleOAuthCallbackResponse {
    val userId = state.toLong()
    googleOAuthService.handleCallback(code, userId)

    return GoogleOAuthCallbackResponse(
      message = "Google Calendar successfully connected for user $userId.",
      userId = userId.toString(),
    )
  }
}

/** Response payload returned by [GoogleOAuthController.callback]. */
data class GoogleOAuthCallbackResponse(
  val message: String,
  val userId: String,
)
