package de.stammtischHub.terminPilot.provider.google.oauth

import com.google.api.client.auth.oauth2.TokenResponse
import de.stammtischHub.terminPilot.exception.GoogleCalendarNotConnectedException
import de.stammtischHub.terminPilot.exception.GoogleCalendarNotFoundException
import de.stammtischHub.terminPilot.exception.UserNotFoundException
import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.repository.GoogleCalendarRepository
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DEFAULT_CALENDAR_NAME = "primary"
private const val DEFAULT_TOKEN_TTL_MILLIS = 3_600_000L // 1 hour fallback

/**
 * Manages the [GoogleCalendar] entity: lookup by user/id, calendar-name resolution,
 * and persisting OAuth tokens.
 *
 * This is deliberately separate from [GoogleOAuthService] (which orchestrates the
 * OAuth flow itself) and from [GoogleCredentialProvider] (which turns persisted
 * tokens into a usable [com.google.api.client.auth.oauth2.Credential]), so that
 * each class has a single, testable reason to change.
 */
@Service
@Transactional
class GoogleCalendarAccountService(
  private val userRepository: UserRepository,
  private val googleCalendarRepository: GoogleCalendarRepository,
) {
  /**
   * Finds the connected Google calendar for a given user.
   *
   * @throws de.stammtischHub.terminPilot.exception.GoogleCalendarNotConnectedException if the user has not authorized Google Calendar access.
   */
  fun findByUserId(userId: Long): GoogleCalendar =
    findUser(userId)
      .calendars
      .filterIsInstance<GoogleCalendar>()
      .firstOrNull()
      ?: throw GoogleCalendarNotConnectedException(userId)

  /**
   * Finds a Google calendar by its own ID.
   *
   * @throws de.stammtischHub.terminPilot.exception.GoogleCalendarNotFoundException if no such calendar exists.
   */
  fun findById(calendarId: Long): GoogleCalendar =
    googleCalendarRepository.findByIdOrNull(calendarId)
      ?: throw GoogleCalendarNotFoundException(calendarId)

  /** Returns the Google Calendar API calendar ID (e.g. "primary") configured for the given user. */
  fun getCalendarIdForUser(userId: Long): String = findByUserId(userId).calendarName

  /** Returns the calendar name for a given calendar entity ID. */
  fun getCalendarName(calendarId: Long): String = findById(calendarId).calendarName

  // TODO: move to OAuthService (private)

  /**
   * Persists the tokens from an OAuth [tokenResponse] for the given user, creating a new
   * [GoogleCalendar] entity if the user has not connected one yet, or updating the existing one.
   */
  fun saveTokens(
    userId: Long,
    tokenResponse: TokenResponse,
  ) {
    val user = findUser(userId)
    val existingCalendar = user.calendars.filterIsInstance<GoogleCalendar>().firstOrNull()
    val calendar = existingCalendar ?: newCalendarForUser(user)

    applyTokens(calendar, tokenResponse)
    googleCalendarRepository.save(calendar)

    if (existingCalendar == null) {
      user.calendars.add(calendar)
      userRepository.save(user)
    }
  }

  /** Updates only the access token/expiry, e.g. after a silent refresh. */
  fun updateAccessToken(
    calendar: GoogleCalendar,
    accessToken: String,
    tokenExpiry: Long,
  ) {
    calendar.accessToken = accessToken
    calendar.tokenExpiry = tokenExpiry
    googleCalendarRepository.save(calendar)
  }

  private fun newCalendarForUser(user: User): GoogleCalendar =
    GoogleCalendar().apply {
      owner = user
      calendarName = DEFAULT_CALENDAR_NAME
      refreshToken = ""
    }

  private fun applyTokens(
    calendar: GoogleCalendar,
    tokenResponse: TokenResponse,
  ) {
    calendar.accessToken = tokenResponse.accessToken
    tokenResponse.refreshToken?.let { calendar.refreshToken = it }
    calendar.tokenExpiry = computeExpiry(tokenResponse.expiresInSeconds, calendar.tokenExpiry)
  }

  private fun computeExpiry(
    expiresInSeconds: Long?,
    fallback: Long?,
  ): Long {
    val now = System.currentTimeMillis()
    return expiresInSeconds?.let { now + it * 1000L }
      ?: fallback
      ?: (now + DEFAULT_TOKEN_TTL_MILLIS)
  }

  private fun findUser(userId: Long): User =
    userRepository.findByIdOrNull(userId)
      ?: throw UserNotFoundException(userId)
}
