package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.config.AppleCalDavProperties
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.ComponentList
import net.fortuna.ical4j.model.ParameterList
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Version
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.StringWriter
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class AppleCalDavClient(
  private val properties: AppleCalDavProperties,
  private val httpClient: CalDavHttpClient,
  private val xmlParser: CalDavXmlParser,
  private val iCalParser: ICalParser,
) {
  private val log = LoggerFactory.getLogger(AppleCalDavClient::class.java)

  fun discoverCalendars(
    email: String,
    appSpecificPassword: String,
  ): List<DiscoveredCalendar> {
    val principalResponse =
      httpClient.propfind(
        url = properties.baseUrl,
        email = email,
        password = appSpecificPassword,
        depth = "0",
        body = PROPFIND_PRINCIPAL,
      )
    val principalHref = xmlParser.extractPrincipalHref(principalResponse)
    val principalUrl = httpClient.resolveUrl(properties.baseUrl, principalHref)

    val homeSetResponse =
      httpClient.propfind(
        url = principalUrl,
        email = email,
        password = appSpecificPassword,
        depth = "0",
        body = PROPFIND_CALENDAR_HOME,
      )
    val homeHref = xmlParser.extractCalendarHomeHref(homeSetResponse)
    val homeUrl = httpClient.resolveUrl(principalUrl, homeHref)

    val calendarsResponse =
      httpClient.propfind(
        url = homeUrl,
        email = email,
        password = appSpecificPassword,
        depth = "1",
        body = PROPFIND_CALENDARS,
      )
    val collections = xmlParser.extractCalendarCollections(calendarsResponse)

    if (collections.isEmpty()) throw AppleNoCalendarsFoundException()

    log.info("Discovered {} calendar(s) for connection", collections.size)
    return collections.map { DiscoveredCalendar(href = it.href, displayName = it.displayName) }
  }

  fun fetchBusyIntervals(
    email: String,
    appSpecificPassword: String,
    calendarHref: String,
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): List<BusySlot> {
    val fromUtc = from.toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC)
    val toUtc = to.toZonedDateTime().withZoneSameInstant(ZoneOffset.UTC)
    val startStr = TIME_RANGE_FMT.format(fromUtc)
    val endStr = TIME_RANGE_FMT.format(toUtc)
    val calendarUrl = httpClient.resolveUrl(properties.baseUrl, calendarHref)

    log.info(
      "Fetching busy intervals from Apple CalDAV (range: {} – {})",
      startStr,
      endStr,
    )

    return try {
      fetchViaFreeBusyQuery(email, appSpecificPassword, calendarUrl, startStr, endStr, fromUtc, toUtc)
    } catch (e: AppleFreeBusyNotSupportedException) {
      fetchViaCalendarQuery(email, appSpecificPassword, calendarUrl, startStr, endStr, fromUtc, toUtc)
    } catch (e: AppleFreeBusyBadRequestException) {
      fetchViaCalendarQuery(email, appSpecificPassword, calendarUrl, startStr, endStr, fromUtc, toUtc)
    } catch (e: AppleCalendarConflictException) {
      fetchViaCalendarQuery(email, appSpecificPassword, calendarUrl, startStr, endStr, fromUtc, toUtc)
    }
  }

  fun createEvent(
    email: String,
    appSpecificPassword: String,
    calendarHref: String,
    title: String,
    start: OffsetDateTime,
    end: OffsetDateTime,
    eventUid: String? = null,
  ): CreatedEvent {
    require(title.isNotBlank()) { "title must not be blank" }
    require(start.isBefore(end)) { "start must be before end" }

    val normalizedStart = start.withOffsetSameInstant(ZoneOffset.UTC)
    val normalizedEnd = end.withOffsetSameInstant(ZoneOffset.UTC)

    val uid =
      if (eventUid != null) {
        require(eventUid.isNotBlank()) { "eventUid must not be blank" }
        eventUid
      } else {
        val eventId = UUID.randomUUID().toString()
        "$eventId@terminpilot.stammtischhub.de"
      }

    val resourceHref = buildResourceHref(calendarHref, "${uid.substringBefore("@")}.ics")
    val calendarUrl = httpClient.resolveUrl(properties.baseUrl, resourceHref)
    val icsBody = buildSingleEventCalendar(uid = uid, title = title, start = normalizedStart, end = normalizedEnd)

    httpClient.putCalendarObject(
      url = calendarUrl,
      email = email,
      password = appSpecificPassword,
      body = icsBody,
      ifNoneMatch = "*",
    )

    log.info("Created Apple calendar event in Apple CalDAV")
    return CreatedEvent(
      uid = uid,
      resourceHref = resourceHref,
      startTime = normalizedStart,
      endTime = normalizedEnd,
      title = title,
    )
  }

  private fun fetchViaFreeBusyQuery(
    email: String,
    password: String,
    url: String,
    startStr: String,
    endStr: String,
    from: java.time.ZonedDateTime,
    to: java.time.ZonedDateTime,
  ): List<BusySlot> {
    val body =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <C:free-busy-query xmlns:C="urn:ietf:params:xml:ns:caldav">
        <C:time-range start="$startStr" end="$endStr"/>
      </C:free-busy-query>
      """.trimIndent()
    val response =
      try {
        httpClient.report(url = url, email = email, password = password, depth = "1", body = body)
      } catch (e: AppleCalDavUnavailableException) {
        if (e.httpStatusCode == 400) {
          throw AppleFreeBusyBadRequestException(cause = e)
        }
        throw e
      } catch (e: AppleFreeBusyNotSupportedException) {
        throw e
      }
    return iCalParser.parseFreebusy(response, from, to)
  }

  private fun fetchViaCalendarQuery(
    email: String,
    password: String,
    url: String,
    startStr: String,
    endStr: String,
    from: java.time.ZonedDateTime,
    to: java.time.ZonedDateTime,
  ): List<BusySlot> {
    val body =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <D:prop>
          <D:getetag/>
          <C:calendar-data/>
        </D:prop>
        <C:filter>
          <C:comp-filter name="VCALENDAR">
            <C:comp-filter name="VEVENT">
              <C:time-range start="$startStr" end="$endStr"/>
            </C:comp-filter>
          </C:comp-filter>
        </C:filter>
      </C:calendar-query>
      """.trimIndent()
    val response = httpClient.report(url = url, email = email, password = password, depth = "1", body = body)
    val calendarDataList = xmlParser.extractCalendarData(response)
    return iCalParser.parseCalendarData(calendarDataList, from, to)
  }

  data class DiscoveredCalendar(
    val href: String,
    val displayName: String,
  )

  data class BusySlot(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
  )

  data class CreatedEvent(
    val uid: String,
    val resourceHref: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val title: String? = null,
  )

  private fun buildResourceHref(
    calendarHref: String,
    fileName: String,
  ): String =
    if (calendarHref.endsWith("/")) {
      "$calendarHref$fileName"
    } else {
      "$calendarHref/$fileName"
    }

  private fun buildSingleEventCalendar(
    uid: String,
    title: String,
    start: OffsetDateTime,
    end: OffsetDateTime,
  ): String {
    val event = VEvent(start.toInstant(), end.toInstant(), title)

    val calendarProperties =
      PropertyList()
        .add(ProdId(PRODUCT_ID))
        .add(Version(ParameterList(), "2.0"))

    val components = ComponentList<net.fortuna.ical4j.model.component.CalendarComponent>().add(event)
    val calendar = Calendar(calendarProperties, components)

    val icsBody =
      StringWriter().use { writer ->
        CalendarOutputter().output(calendar, writer)
        writer.toString()
      }

    val lines = icsBody.lines()
    var inVEvent = false
    var uidSeen = false

    val filtered =
      lines.filter { line ->
        val trimmed = line.trim()
        when {
          trimmed.startsWith("BEGIN:VEVENT") -> {
            inVEvent = true
            uidSeen = false
            true
          }

          trimmed.startsWith("END:VEVENT") -> {
            inVEvent = false
            true
          }

          inVEvent && trimmed.startsWith("UID") -> {
            if (!uidSeen) {
              uidSeen = true
              true
            } else {
              false
            }
          }

          else -> {
            true
          }
        }
      }

    val mutable = filtered.toMutableList()
    var uidReplaced = false
    for (i in mutable.indices) {
      val line = mutable[i]
      val trimmed = line.trim()
      if (trimmed.startsWith("UID")) {
        mutable[i] = "UID:$uid"
        uidReplaced = true
        break
      }
    }

    if (!uidReplaced) {
      for (i in mutable.indices.reversed()) {
        val line = mutable[i]
        val trimmed = line.trim()
        if (trimmed.startsWith("END:VEVENT")) {
          mutable.add(i, "UID:$uid")
          break
        }
      }
    }

    return mutable.joinToString("\n")
  }

  private companion object {
    val TIME_RANGE_FMT: DateTimeFormatter =
      DateTimeFormatter
        .ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC)

    val PROPFIND_PRINCIPAL =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <D:propfind xmlns:D="DAV:">
        <D:prop>
          <D:current-user-principal/>
        </D:prop>
      </D:propfind>
      """.trimIndent()

    val PROPFIND_CALENDAR_HOME =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <D:propfind xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <D:prop>
          <C:calendar-home-set/>
        </D:prop>
      </D:propfind>
      """.trimIndent()

    val PROPFIND_CALENDARS =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <D:propfind xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <D:prop>
          <D:displayname/>
          <D:resourcetype/>
          <C:supported-calendar-component-set/>
        </D:prop>
      </D:propfind>
      """.trimIndent()

    const val PRODUCT_ID = "-//StammtischHub//TerminPilot//DE"
  }
}
