package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import de.stammtischHub.terminPilot.domain.Appointment
import de.stammtischHub.terminPilot.provider.google.oauth.GoogleOAuthService
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Service implementation for interacting with Google Calendar on behalf of a specific user.
 *
 * Each public method accepts a [userId] parameter to look up the user's OAuth credentials
 * and target calendar, allowing multiple users to connect their own Google Calendars
 * without sharing any application-level credentials.
 */
@Service
class GoogleCalendarService(
    private val googleCalendarConfig: GoogleCalendarConfig,
    private val googleOAuthService: GoogleOAuthService,
) {

    /**
     * Fetches appointments from the Google Calendar of the given user for the specified time range.
     *
     * @param userId The ID of the user whose calendar is queried.
     * @param start The start of the time range.
     * @param end The end of the time range.
     * @return A list of [Appointment]s found within the range.
     */
    fun getCalendarForTimespan(userId: Long, start: LocalDateTime, end: LocalDateTime): List<Appointment> {
        val credential = googleOAuthService.getCredentialForUser(userId)
        val calendarId = googleOAuthService.getCalendarIdForUser(userId)
        val calendarClient = googleCalendarConfig.buildCalendarClient(credential)

        val upcomingEvents =
            calendarClient.events().list(calendarId)
                .setTimeMin(toGoogleDateTime(start))
                .setTimeMax(toGoogleDateTime(end))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .items ?: emptyList()

        return parseAppointments(upcomingEvents)
    }

    /**
     * Inserts a new appointment into the Google Calendar of the given user.
     *
     * @param userId The ID of the user whose calendar is written to.
     * @param appointment The appointment to be written to the calendar.
     */
    fun writeToCalendar(userId: Long, appointment: Appointment) {
        val credential = googleOAuthService.getCredentialForUser(userId)
        val calendarId = googleOAuthService.getCalendarIdForUser(userId)
        val calendarClient = googleCalendarConfig.buildCalendarClient(credential)

        val event = parseEvent(appointment)
        calendarClient.events().insert(calendarId, event).execute()
    }

    /**
     * Parses a list of Google Calendar [Event] objects into a list of [Appointment] domain models.
     *
     * @param events The list of events fetched from Google Calendar.
     * @return A list of [Appointment] objects.
     */
    private fun parseAppointments(events: List<Event>): List<Appointment> =
        events.map { event ->
            Appointment(
                title = event.summary ?: "",
                start = toLocalDateTime(event.start.dateTime ?: event.start.date),
                end = toLocalDateTime(event.end.dateTime ?: event.end.date),
                location = event.location ?: "",
                description = event.description ?: "",
            )
        }

    /**
     * Maps an [Appointment] domain model to a Google Calendar [Event] object.
     *
     * @param appointment The appointment to be converted.
     * @return A Google Calendar [Event] object.
     */
    private fun parseEvent(appointment: Appointment): Event =
        Event()
            .setSummary(appointment.title)
            .setLocation(appointment.location)
            .setDescription(appointment.description)
            .setStart(EventDateTime().setDateTime(toGoogleDateTime(appointment.start)))
            .setEnd(EventDateTime().setDateTime(toGoogleDateTime(appointment.end)))

    /**
     * Converts a Google [DateTime] object to a Java [LocalDateTime].
     *
     * @param googleDateTime The Google API DateTime object.
     * @return The corresponding [LocalDateTime] in the system's default time zone.
     */
    private fun toLocalDateTime(googleDateTime: DateTime): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(googleDateTime.value), ZoneId.systemDefault())

    /**
     * Converts a Java [LocalDateTime] to a Google [DateTime] object.
     *
     * @param localDateTime The Java LocalDateTime object.
     * @return The corresponding Google API [DateTime] object.
     */
    private fun toGoogleDateTime(localDateTime: LocalDateTime): DateTime {
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        return DateTime(instant.toEpochMilli())
    }
}