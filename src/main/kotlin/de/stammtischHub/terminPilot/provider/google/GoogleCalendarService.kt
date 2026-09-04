package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.calendar.Calendar
import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.exception.CalendarAccessFailedException
import de.stammtischHub.terminPilot.exception.CalendarAccessTimeoutException
import de.stammtischHub.terminPilot.exception.GoogleCalendarNotConnectedException
import de.stammtischHub.terminPilot.model.generated.CalendarAccessFailure
import de.stammtischHub.terminPilot.provider.CalendarProvider
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleCalendarAccountService
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleCredentialProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDateTime

/**
 * Service implementation for interacting with Google Calendar on behalf of a specific user.
 *
 * Each public method accepts a [de.stammtischHub.terminPilot.persistence.entity.User.id] parameter to look up the user's OAuth credentials
 * and target calendar, allowing multiple users to connect their own Google Calendars
 * without sharing any application-level credentials.
 *
 * Thin orchestrator: credential handling lives in
 * [GoogleCredentialProvider], calendar-account lookups in [GoogleCalendarAccountService],
 * and domain <-> API mapping in [GoogleEventMapper].
 */
@Service
class GoogleCalendarService(
  private val calendarClientFactory: CalendarClientFactory,
  private val credentialProvider: GoogleCredentialProvider,
  private val calendarAccountService: GoogleCalendarAccountService,
  private val eventMapper: GoogleEventMapper,
) : CalendarProvider {
  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Fetches appointments from the Google Calendar of the given user for the specified time range.
   *
   * @param userId The ID of the user whose calendar is queried.
   * @param start The start of the time range.
   * @param end The end of the time range.
   * @return A list of [Event]s found within the range.
   */
  override fun getCalendarForTimespan(
    userId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Event> {
    val client = buildClientForUser(userId)
    val calendarId = calendarAccountService.getCalendarIdForUser(userId)

    val events =
      client
        .events()
        .list(calendarId)
        .setTimeMin(eventMapper.toGoogleDateTime(start))
        .setTimeMax(eventMapper.toGoogleDateTime(end))
        .setOrderBy("startTime")
        .setSingleEvents(true)
        .execute()
        .items ?: emptyList()

    return events.map(eventMapper::toDomainEvent)
  }

  override fun verifyAccess(userId: Long) =
    withCalendarAccessHandling(userId) {
      val client = buildClientForUser(userId)
      val calendarId = calendarAccountService.getCalendarIdForUser(userId)
      client.calendarList().get(calendarId).execute()
      Unit
    }

  /**
   * Inserts a new appointment into the Google Calendar of the given user.
   *
   * @param userId The ID of the user whose calendar is written to.
   * @param event The appointment to be written to the calendar.
   */
  override fun writeToCalendar(
    userId: Long,
    event: Event,
  ) = withCalendarAccessHandling(userId) {
    val client = buildClientForUser(userId)
    val calendarId = calendarAccountService.getCalendarIdForUser(userId)
    client.events().insert(calendarId, eventMapper.toEvent(event)).execute()
    Unit
  }

  /**
   * Exception handling and logging for calendar access. Translation to domain exceptions / reasons.
   */
  private fun <T> withCalendarAccessHandling(
    userId: Long,
    action: () -> T,
  ): T {
    try {
      return action()
    } catch (e: GoogleCalendarNotConnectedException) {
      throw CalendarAccessFailedException(userId, CalendarAccessFailure.Reason.not_connected)
    } catch (e: SocketTimeoutException) {
      logger.warn("Calendar operation timed out for user $userId", e)
      throw CalendarAccessTimeoutException(userId)
    } catch (e: GoogleJsonResponseException) {
      val reason =
        when (e.statusCode) {
          401, 403 -> CalendarAccessFailure.Reason.reauth_required
          else -> CalendarAccessFailure.Reason.provider_unavailable
        }
      logger.warn("Calendar operation failed for user $userId (status ${e.statusCode})", e)
      throw CalendarAccessFailedException(userId, reason)
    } catch (e: IOException) {
      logger.warn("Calendar operation failed for user $userId", e)
      throw CalendarAccessFailedException(userId, CalendarAccessFailure.Reason.provider_unavailable)
    }
  }

  private fun buildClientForUser(userId: Long): Calendar {
    val credential = credentialProvider.getCredentialForUser(userId)
    return calendarClientFactory.buildClient(credential)
  }
}
