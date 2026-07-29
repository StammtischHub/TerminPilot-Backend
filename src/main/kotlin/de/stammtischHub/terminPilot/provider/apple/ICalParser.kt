package de.stammtischHub.terminPilot.provider.apple

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Period
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VFreeBusy
import net.fortuna.ical4j.model.parameter.FbType
import net.fortuna.ical4j.model.property.DtEnd
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Duration
import net.fortuna.ical4j.model.property.FreeBusy
import net.fortuna.ical4j.model.property.RRule
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.Transp
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.StringReader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import net.fortuna.ical4j.model.Component as ICalComponent

@Component
class ICalParser {
  private val log = LoggerFactory.getLogger(ICalParser::class.java)

  fun parseFreebusy(
    icsText: String,
    from: ZonedDateTime,
    to: ZonedDateTime,
  ): List<AppleCalDavClient.BusySlot> {
    val fromInstant = from.toInstant()
    val toInstant = to.toInstant()
    val result = mutableListOf<AppleCalDavClient.BusySlot>()
    try {
      val calendar = CalendarBuilder().build(StringReader(icsText))
      val vFreeBusyList = calendar.getComponents<VFreeBusy>(ICalComponent.VFREEBUSY)
      for (vFreeBusy in vFreeBusyList) {
        val fbProps = vFreeBusy.getProperties<FreeBusy>("FREEBUSY")
        for (fbProp in fbProps) {
          val fbTypeValue = fbProp.getParameter<FbType>("FBTYPE").orElse(null)?.value ?: "BUSY"
          if (fbTypeValue.equals("FREE", ignoreCase = true)) continue
          for (interval in fbProp.intervals) {
            val s = interval.start
            val e = interval.end
            if (s.isBefore(toInstant) && e.isAfter(fromInstant)) {
              result +=
                AppleCalDavClient.BusySlot(
                  startTime = s.atOffset(ZoneOffset.UTC),
                  endTime = e.atOffset(ZoneOffset.UTC),
                )
            }
          }
        }
      }
    } catch (e: AppleCalDavException) {
      throw e
    } catch (e: Exception) {
      throw AppleCalendarParseException(detail = "Failed to parse VFREEBUSY response", cause = e)
    }
    return result
  }

  fun parseCalendarData(
    icsTexts: List<String>,
    from: ZonedDateTime,
    to: ZonedDateTime,
  ): List<AppleCalDavClient.BusySlot> {
    val result = mutableListOf<AppleCalDavClient.BusySlot>()
    for (icsText in icsTexts) {
      try {
        val calendar = CalendarBuilder().build(StringReader(icsText))
        val events = calendar.getComponents<VEvent>(ICalComponent.VEVENT)
        for (event in events) {
          result += extractSlotsFromEvent(event, from, to)
        }
      } catch (e: AppleCalDavException) {
        throw e
      } catch (e: Exception) {
        log.warn("Could not parse a calendar-data ICS entry – skipping")
      }
    }
    return result
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractSlotsFromEvent(
    event: VEvent,
    from: ZonedDateTime,
    to: ZonedDateTime,
  ): List<AppleCalDavClient.BusySlot> {
    val transp = event.getProperty<Transp>("TRANSP").orElse(null)
    if (transp?.value?.equals(Transp.VALUE_TRANSPARENT, ignoreCase = true) == true) return emptyList()

    val status = event.getProperty<Status>("STATUS").orElse(null)
    if (status?.value?.equals(Status.VALUE_CANCELLED, ignoreCase = true) == true) return emptyList()

    val dtStart = event.getProperty<DtStart<*>>("DTSTART").orElse(null) ?: return emptyList()
    val startTemporal: Temporal = dtStart.date
    val startInstant = toInstant(startTemporal) ?: return emptyList()

    val dtEnd = event.getProperty<DtEnd<*>>("DTEND").orElse(null)
    val durationProp = event.getProperty<Duration>("DURATION").orElse(null)
    val endInstant: Instant =
      when {
        dtEnd != null -> toInstant(dtEnd.date) ?: return emptyList()
        durationProp != null -> startInstant.plus(java.time.Duration.from(durationProp.duration))
        startTemporal is LocalDate -> startInstant.plus(java.time.Duration.ofDays(1))
        else -> return emptyList()
      }

    val fromInstant = from.toInstant()
    val toInstant = to.toInstant()

    val hasRRule = event.getProperty<RRule<*>>("RRULE").isPresent
    if (hasRRule) {
      return expandRecurring(event, startTemporal, from, to)
    }

    if (startInstant >= toInstant || endInstant <= fromInstant) return emptyList()

    return listOf(
      AppleCalDavClient.BusySlot(
        startTime = startInstant.atOffset(ZoneOffset.UTC),
        endTime = endInstant.atOffset(ZoneOffset.UTC),
      ),
    )
  }

  private fun expandRecurring(
    event: VEvent,
    startTemporal: Temporal,
    from: ZonedDateTime,
    to: ZonedDateTime,
  ): List<AppleCalDavClient.BusySlot> =
    try {
      when (startTemporal) {
        is LocalDate -> {
          val ldPeriod = Period(from.toLocalDate(), to.toLocalDate())

          @Suppress("UNCHECKED_CAST")
          val periods = event.calculateRecurrenceSet<LocalDate>(ldPeriod) as Set<Period<LocalDate>>
          periods.mapNotNull { p ->
            runCatching {
              val s = p.start.atStartOfDay(ZoneOffset.UTC).toInstant()
              val e = p.end.atStartOfDay(ZoneOffset.UTC).toInstant()
              AppleCalDavClient.BusySlot(
                startTime = s.atOffset(ZoneOffset.UTC),
                endTime = e.atOffset(ZoneOffset.UTC),
              )
            }.getOrNull()
          }
        }

        else -> {
          val zdtPeriod = Period(from, to)

          @Suppress("UNCHECKED_CAST")
          val periods = event.calculateRecurrenceSet<ZonedDateTime>(zdtPeriod) as Set<Period<ZonedDateTime>>
          periods.mapNotNull { p ->
            runCatching {
              AppleCalDavClient.BusySlot(
                startTime = p.start.toInstant().atOffset(ZoneOffset.UTC),
                endTime = p.end.toInstant().atOffset(ZoneOffset.UTC),
              )
            }.getOrNull()
          }
        }
      }
    } catch (e: Exception) {
      log.warn("Could not expand recurrence for event – skipping")
      emptyList()
    }

  private fun toInstant(temporal: Temporal): Instant? =
    when (temporal) {
      is ZonedDateTime -> temporal.toInstant()
      is OffsetDateTime -> temporal.toInstant()
      is LocalDateTime -> temporal.toInstant(ZoneOffset.UTC)
      is LocalDate -> temporal.atStartOfDay(ZoneOffset.UTC).toInstant()
      is Instant -> temporal
      else -> null
    }
}
