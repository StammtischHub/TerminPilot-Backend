package de.stammtischHub.terminPilot.provider.google

import com.google.api.services.calendar.Calendar
import de.stammtischHub.terminPilot.domain.Appointment
import de.stammtischHub.terminPilot.provider.CalendarProvider
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleCalendarAccountService
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleCredentialProvider
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service implementation for interacting with Google Calendar on behalf of a specific user.
 *
 * Each public method accepts a [userId] parameter to look up the user's OAuth credentials
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
  /**
   * Fetches appointments from the Google Calendar of the given user for the specified time range.
   *
   * @param userId The ID of the user whose calendar is queried.
   * @param start The start of the time range.
   * @param end The end of the time range.
   * @return A list of [Appointment]s found within the range.
   */
  override fun getCalendarForTimespan(
    userId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Appointment> {
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

    return events.map(eventMapper::toAppointment)
  }

  /**
   * Inserts a new appointment into the Google Calendar of the given user.
   *
   * @param userId The ID of the user whose calendar is written to.
   * @param appointment The appointment to be written to the calendar.
   */
  override fun writeToCalendar(
    userId: Long,
    appointment: Appointment,
  ) {
    val client = buildClientForUser(userId)
    val calendarId = calendarAccountService.getCalendarIdForUser(userId)

    client.events().insert(calendarId, eventMapper.toEvent(appointment)).execute()
  }

  private fun buildClientForUser(userId: Long): Calendar {
    val credential = credentialProvider.getCredentialForUser(userId)
    return calendarClientFactory.buildClient(credential)
  }
}
