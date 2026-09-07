package de.stammtischHub.terminPilot.provider.google

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import de.stammtischHub.terminPilot.domain.Event as DomainEvent

/**
 * Maps between the domain model [de.stammtischHub.terminPilot.domain.Event] and Google's [Event] representation,
 * including the underlying date/time conversions.
 *
 * Extracted from [GoogleCalendarService] so that the mapping rules can be tested
 * and changed independently of the API-calling logic (Single Responsibility Principle).
 */
@Component
class GoogleEventMapper {
  /**
   * Converts a Google Calendar [Event] into an [DomainEvent] domain model.
   */
  fun toDomainEvent(event: Event): DomainEvent =
    DomainEvent(
      title = event.summary ?: "",
      start = toLocalDateTime(event.start.dateTime ?: event.start.date),
      end = toLocalDateTime(event.end.dateTime ?: event.end.date),
      participants = emptyList(), // TODO: Check Google Attendees
      location = event.location ?: "",
      description = event.description ?: "",
    )

  /**
   * Converts an [DomainEvent] domain model into a Google Calendar [Event].
   */
  fun toEvent(event: DomainEvent): Event =
    Event()
      .setSummary(event.title)
      .setLocation(event.location)
      .setDescription(event.description)
      .setStart(EventDateTime().setDateTime(toGoogleDateTime(event.start)))
      .setEnd(EventDateTime().setDateTime(toGoogleDateTime(event.end)))

  /**
   * Converts a Java [LocalDateTime] to a Google [DateTime], e.g. for time-range queries.
   */
  fun toGoogleDateTime(localDateTime: LocalDateTime): DateTime {
    val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
    return DateTime(instant.toEpochMilli())
  }

  private fun toLocalDateTime(googleDateTime: DateTime): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(googleDateTime.value), ZoneId.systemDefault())
}
