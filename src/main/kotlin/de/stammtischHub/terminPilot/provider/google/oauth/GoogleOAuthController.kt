package de.stammtischHub.terminPilot.provider.google.oauth

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that exposes the Google OAuth 2.0 authorization endpoints.
 *
 * Provides two endpoints:
 * - `GET /api/google/oauth/authorize` – initiates the OAuth flow by redirecting the user
 *   to the Google consent screen.
 * - `GET /api/google/oauth/callback` – processes Google's redirect after the user grants access,
 *   exchanges the authorization code for tokens, and stores them in the database.
 *
 * The [userId] is passed as a query parameter to `/authorize` and round-tripped via the OAuth
 * `state` parameter so the callback can associate the tokens with the correct user.
 */
@RestController
@RequestMapping("/api/google/oauth")
class GoogleOAuthController(
    private val googleOAuthService: GoogleOAuthService,
) {
    /**
     * Initiates the Google OAuth 2.0 authorization flow for a user.
     *
     * Redirects the user's browser to the Google OAuth consent screen.
     * After granting access, Google redirects back to `/api/google/oauth/callback`.
     *
     * @param userId The ID of the user who wishes to connect their Google Calendar.
     * @param response The HTTP response used to issue the browser redirect.
     */
    @GetMapping("/authorize")
    fun authorize(
        @RequestParam userId: Long,
        response: HttpServletResponse,
    ) {
        val authorizationUrl = googleOAuthService.buildAuthorizationUrl(userId)
        response.sendRedirect(authorizationUrl)
    }

    /**
     * Handles the OAuth 2.0 redirect callback from Google after the user grants access.
     *
     * Google appends the one-time authorization [code] and the original [state] (which encodes
     * the [userId]) to this endpoint's URL. The code is exchanged for access and refresh tokens,
     * which are then persisted to the database.
     *
     * @param code The authorization code issued by Google.
     * @param state The OAuth state parameter containing the user ID set during `/authorize`.
     * @return A confirmation payload with a success message and the connected user ID.
     */
    @GetMapping("/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
    ): Map<String, String> {
        val userId = state.toLong()
        googleOAuthService.handleCallback(code, userId)
        return mapOf(
            "message" to "Google Calendar successfully connected for user $userId.",
            "userId" to userId.toString(),
        )
    }
}
