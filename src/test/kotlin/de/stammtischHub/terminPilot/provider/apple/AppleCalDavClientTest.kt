package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.config.AppleCalDavProperties
import net.fortuna.ical4j.data.CalendarBuilder
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

class AppleCalDavClientTest {
  private lateinit var server: MockWebServer
  private lateinit var client: AppleCalDavClient
  private lateinit var httpClient: CalDavHttpClient
  private lateinit var xmlParser: CalDavXmlParser
  private lateinit var iCalParser: ICalParser

  @BeforeEach
  fun setUp() {
    server = MockWebServer()
    server.start()
    val baseUrl = server.url("/").toString().trimEnd('/')

    val properties =
      AppleCalDavProperties(
        baseUrl = baseUrl,
        connectTimeout = Duration.ofSeconds(5),
        readTimeout = Duration.ofSeconds(10),
      )
    httpClient = CalDavHttpClient(properties)
    xmlParser = CalDavXmlParser()
    iCalParser = ICalParser()
    client = AppleCalDavClient(properties, httpClient, xmlParser, iCalParser)
  }

  @AfterEach
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun `discovery_resolves_principal_and_calendar_home`() {
    server.enqueue(MockResponse().setResponseCode(207).setBody(PRINCIPAL_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_HOME_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_LIST_RESPONSE))

    val calendars = client.discoverCalendars("user@icloud.com", "app-password")

    assertEquals(1, calendars.size)
    assertEquals("Home", calendars[0].displayName)
    assertEquals(3, server.requestCount)
  }

  @Test
  fun `discovery_does_not_require_hardcoded_account_id`() {
    server.enqueue(MockResponse().setResponseCode(207).setBody(PRINCIPAL_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_HOME_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_LIST_RESPONSE))

    client.discoverCalendars("user@icloud.com", "app-password")

    server.takeRequest()
    val thirdRequest = server.takeRequest()
    server.takeRequest()
    assertTrue(
      thirdRequest.path?.contains("/principals/") == true,
      "Second PROPFIND should use the href from the principal response",
    )
  }

  @Test
  fun `calendar_discovery_returns_only_vevent_calendars`() {
    server.enqueue(MockResponse().setResponseCode(207).setBody(PRINCIPAL_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_HOME_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_LIST_WITH_TASKS_RESPONSE))

    val calendars = client.discoverCalendars("user@icloud.com", "app-password")

    assertTrue(calendars.all { it.displayName != "Reminders" }, "Tasks/VTODO calendars must be excluded")
    assertEquals(1, calendars.size)
    assertEquals("Home", calendars[0].displayName)
  }

  @Test
  fun `calendar_discovery_ignores_inbox_outbox_and_tasks`() {
    server.enqueue(MockResponse().setResponseCode(207).setBody(PRINCIPAL_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_HOME_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_LIST_WITH_INBOX_RESPONSE))

    val calendars = client.discoverCalendars("user@icloud.com", "app-password")

    assertTrue(calendars.none { it.displayName == "Inbox" }, "Inbox must be excluded")
    assertEquals(1, calendars.size)
  }

  @Test
  fun `xml_parser_supports_different_namespace_prefixes`() {
    server.enqueue(MockResponse().setResponseCode(207).setBody(PRINCIPAL_RESPONSE_ALT_PREFIX))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_HOME_RESPONSE))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_LIST_RESPONSE))

    val calendars = client.discoverCalendars("user@icloud.com", "app-password")

    assertEquals(1, calendars.size, "Should parse alternative namespace prefix without error")
  }

  @Test
  fun `invalid_credentials_result_in_connection_error`() {
    server.enqueue(MockResponse().setResponseCode(401))

    val ex =
      assertThrows(AppleAuthenticationException::class.java) {
        client.discoverCalendars("user@icloud.com", "wrong-password")
      }
    assertEquals("APPLE_AUTHENTICATION_FAILED", ex.errorCode)
    assertFalse(
      "wrong-password" in (ex.message ?: ""),
      "Password must not appear in exception message",
    )
  }

  @Test
  fun `apple_caldav_unavailable_throws_correct_exception`() {
    server.enqueue(MockResponse().setResponseCode(503))

    assertThrows(AppleCalDavUnavailableException::class.java) {
      client.discoverCalendars("user@icloud.com", "app-password")
    }
  }

  @Test
  fun `freebusy_response_is_converted_to_busy_intervals`() {
    val calendarHref = "/calendars/user/home/"
    val from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val to = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(200).setBody(FREEBUSY_ICS_RESPONSE))

    val slots = client.fetchBusyIntervals("user@icloud.com", "app-password", calendarHref, from, to)

    assertEquals(1, slots.size)
    assertEquals("2024-01-15T09:00Z", slots[0].startTime.toString())
    assertEquals("2024-01-15T10:00Z", slots[0].endTime.toString())
  }

  @Test
  fun `fallback_calendar_query_is_used_when_freebusy_is_not_supported`() {
    val calendarHref = "/calendars/user/home/"
    val from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val to = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(405))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val slots = client.fetchBusyIntervals("user@icloud.com", "app-password", calendarHref, from, to)

    assertEquals(1, slots.size)
    assertEquals(2, server.requestCount, "Should have made exactly 2 REPORT requests")
  }

  @Test
  fun `selected_calendars_are_synchronized_via_client`() {
    val calendarHref = "/calendars/user/home/"
    val from = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val to = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(200).setBody(FREEBUSY_ICS_RESPONSE))

    val slots = client.fetchBusyIntervals("user@icloud.com", "app-password", calendarHref, from, to)

    assertTrue(slots.isNotEmpty())
    val request = server.takeRequest()
    assertTrue(request.path?.contains("home") == true, "Request must target the specific calendar")
  }

  @Test
  fun `credentials_are_not_in_logs_or_exceptions`() {
    val password = "my-secret-password-xyz"
    server.enqueue(MockResponse().setResponseCode(403))

    val ex =
      assertThrows(AppleAuthenticationException::class.java) {
        client.discoverCalendars("user@icloud.com", password)
      }

    assertFalse(password in (ex.message ?: ""), "Password must not appear in exception message")
    assertFalse(password in ex.errorCode, "Password must not appear in error code")
    assertFalse(password in ex.userMessage, "Password must not appear in user-facing message")
  }

  @Test
  fun `create_event_sends_valid_icalendar_put`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.ofHours(1))
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.ofHours(1))
    server.enqueue(MockResponse().setResponseCode(201))

    client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)

    val request = server.takeRequest()
    val rawBody = request.body.readUtf8()
    assertEquals("PUT", request.method)
    assertTrue(request.path?.endsWith(".ics") == true)
    assertTrue(request.getHeader("Content-Type")?.startsWith("text/calendar") == true)

    val parsedCalendar = CalendarBuilder().build(StringReader(rawBody))
    assertNotNull(parsedCalendar, "Generated iCalendar should parse successfully")

    assertTrue(rawBody.contains("BEGIN:VEVENT"))
    assertTrue(rawBody.contains("END:VEVENT"))
  }

  @Test
  fun `create_event_uses_discovered_calendar_href`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(201))

    val created =
      client.createEvent(
        "user@icloud.com",
        "app-password",
        "/calendars/testuser/home/",
        "Planning",
        start,
        end,
      )

    val request = server.takeRequest()
    assertTrue(request.path?.startsWith("/calendars/testuser/home/") == true)
    assertEquals(created.resourceHref, request.path)
  }

  @Test
  fun `create_event_sets_if_none_match`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(201))

    client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)

    val request = server.takeRequest()
    assertEquals("*", request.getHeader("If-None-Match"))
  }

  @Test
  fun `create_event_handles_201_created`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(201))

    val created =
      client.createEvent(
        "user@icloud.com",
        "app-password",
        "/calendars/testuser/home/",
        "Planning",
        start,
        end,
      )

    assertTrue(created.uid.contains("@terminpilot.stammtischhub.de"))
    assertEquals(start, created.startTime)
    assertEquals(end, created.endTime)
  }

  @Test
  fun `create_event_handles_204_no_content`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(204))

    val created =
      client.createEvent(
        "user@icloud.com",
        "app-password",
        "/calendars/testuser/home/",
        "Planning",
        start,
        end,
      )

    assertTrue(created.resourceHref.endsWith(".ics"))
  }

  @Test
  fun `create_event_rejects_invalid_time_range`() {
    val start = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)

    assertThrows(IllegalArgumentException::class.java) {
      client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)
    }
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `create_event_handles_auth_error`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(401))

    assertThrows(AppleAuthenticationException::class.java) {
      client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)
    }
  }

  @Test
  fun `create_event_handles_resource_conflict`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(412))

    assertThrows(AppleCalendarResourceConflictException::class.java) {
      client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)
    }
  }

  @Test
  fun `create_event_handles_target_conflict`() {
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(409))

    assertThrows(AppleCalendarConflictException::class.java) {
      client.createEvent("user@icloud.com", "app-password", "/calendars/testuser/home/", "Planning", start, end)
    }
  }

  @Test
  fun `create_event_does_not_log_credentials`() {
    val password = "secret-app-password"
    val start = OffsetDateTime.of(2024, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC)
    server.enqueue(MockResponse().setResponseCode(403))

    val ex =
      assertThrows(AppleAuthenticationException::class.java) {
        client.createEvent("user@icloud.com", password, "/calendars/testuser/home/", "Planning", start, end)
      }

    assertFalse(password in (ex.message ?: ""))
    assertFalse(password in ex.userMessage)
  }

  private fun parseCalendarRequest(request: RecordedRequest): net.fortuna.ical4j.model.Calendar =
    CalendarBuilder().build(StringReader(request.body.readUtf8()))

  companion object {
    val PRINCIPAL_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:">
        <response>
          <href>/</href>
          <propstat>
            <prop>
              <current-user-principal>
                <href>/principals/users/testuser/</href>
              </current-user-principal>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()

    val PRINCIPAL_RESPONSE_ALT_PREFIX =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <d:multistatus xmlns:d="DAV:">
        <d:response>
          <d:href>/</d:href>
          <d:propstat>
            <d:prop>
              <d:current-user-principal>
                <d:href>/principals/users/testuser/</d:href>
              </d:current-user-principal>
            </d:prop>
            <d:status>HTTP/1.1 200 OK</d:status>
          </d:propstat>
        </d:response>
      </d:multistatus>
      """.trimIndent()

    val CALENDAR_HOME_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
        <response>
          <href>/principals/users/testuser/</href>
          <propstat>
            <prop>
              <cal:calendar-home-set>
                <href>/calendars/testuser/</href>
              </cal:calendar-home-set>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()

    val CALENDAR_LIST_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <response>
          <href>/calendars/testuser/</href>
          <propstat>
            <prop>
              <displayname>Home</displayname>
              <resourcetype><collection/></resourcetype>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
        <response>
          <href>/calendars/testuser/home/</href>
          <propstat>
            <prop>
              <displayname>Home</displayname>
              <resourcetype>
                <collection/>
                <C:calendar/>
              </resourcetype>
              <C:supported-calendar-component-set>
                <C:comp name="VEVENT"/>
              </C:supported-calendar-component-set>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()

    val CALENDAR_LIST_WITH_TASKS_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <response>
          <href>/calendars/testuser/home/</href>
          <propstat>
            <prop>
              <displayname>Home</displayname>
              <resourcetype>
                <collection/>
                <C:calendar/>
              </resourcetype>
              <C:supported-calendar-component-set>
                <C:comp name="VEVENT"/>
              </C:supported-calendar-component-set>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
        <response>
          <href>/calendars/testuser/tasks/</href>
          <propstat>
            <prop>
              <displayname>Reminders</displayname>
              <resourcetype>
                <collection/>
                <C:calendar/>
              </resourcetype>
              <C:supported-calendar-component-set>
                <C:comp name="VTODO"/>
              </C:supported-calendar-component-set>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()

    val CALENDAR_LIST_WITH_INBOX_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <response>
          <href>/calendars/testuser/home/</href>
          <propstat>
            <prop>
              <displayname>Home</displayname>
              <resourcetype>
                <collection/>
                <C:calendar/>
              </resourcetype>
              <C:supported-calendar-component-set>
                <C:comp name="VEVENT"/>
              </C:supported-calendar-component-set>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
        <response>
          <href>/calendars/testuser/inbox/</href>
          <propstat>
            <prop>
              <displayname>Inbox</displayname>
              <resourcetype>
                <collection/>
                <C:schedule-inbox/>
              </resourcetype>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()

    val FREEBUSY_ICS_RESPONSE =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VFREEBUSY
      DTSTART:20240101T000000Z
      DTEND:20240131T235959Z
      FREEBUSY;FBTYPE=BUSY:20240115T090000Z/20240115T100000Z
      END:VFREEBUSY
      END:VCALENDAR
      """.trimIndent()

    val CALENDAR_QUERY_RESPONSE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <multistatus xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
        <response>
          <href>/calendars/testuser/home/event1.ics</href>
          <propstat>
            <prop>
              <C:calendar-data>BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      DTSTART:20240115T090000Z
      DTEND:20240115T100000Z
      END:VEVENT
      END:VCALENDAR</C:calendar-data>
            </prop>
            <status>HTTP/1.1 200 OK</status>
          </propstat>
        </response>
      </multistatus>
      """.trimIndent()
  }

  @Test
  fun `fetchBusyIntervals falls back to calendar-query when free-busy-query returns 405 Method Not Allowed`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(405))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
    assertEquals(2, server.requestCount, "Expected 2 requests: free-busy-query (failed) + calendar-query (success)")
  }

  @Test
  fun `fetchBusyIntervals falls back to calendar-query when free-busy-query returns 501 Not Implemented`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(501))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
    assertEquals(2, server.requestCount)
  }

  @Test
  fun `fetchBusyIntervals falls back to calendar-query when free-busy-query returns 415 Unsupported Media Type`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(415))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
  }

  @Test
  fun `fetchBusyIntervals falls back to calendar-query when free-busy-query returns 409 Conflict`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(409))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
  }

  @Test
  fun `fetchBusyIntervals does not fall back on 401 Unauthorized`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(401))

    assertThrows(AppleAuthenticationException::class.java) {
      client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)
    }

    assertEquals(1, server.requestCount, "Expected only one request (no fallback for auth error)")
  }

  @Test
  fun `fetchBusyIntervals preserves 403 Forbidden as auth error without fallback`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(403))

    assertThrows(AppleAuthenticationException::class.java) {
      client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)
    }

    assertEquals(1, server.requestCount, "Expected only one request (no fallback for auth error)")
  }

  @Test
  fun `fetchBusyIntervals propagates real server errors without credentials in logs`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(503))

    assertThrows(AppleCalDavUnavailableException::class.java) {
      client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)
    }

    assertEquals(1, server.requestCount)
  }

  @Test
  fun `fetchBusyIntervals falls back to calendar-query when free-busy-query returns 400`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(400))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
    assertEquals(2, server.requestCount, "Expected 2 requests: free-busy-query (400) + calendar-query (success)")
  }

  @Test
  fun `does_not_treat_400_from_calendar_query_as_free_busy_unsupported`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(207).setBody(FREEBUSY_ICS_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
    assertEquals(1, server.requestCount, "Expected 1 request (free-busy-query succeeded, no fallback)")
  }

  @Test
  fun `free_busy_400_fallback_does_not_log_response_body_or_credentials`() {
    val calendarHref = "/calendars/test/home/"
    val start = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(400).setBody("sensitive error body"))
    server.enqueue(MockResponse().setResponseCode(207).setBody(CALENDAR_QUERY_RESPONSE))

    val intervals = client.fetchBusyIntervals("test@icloud.com", "pass", calendarHref, start, end)

    assertEquals(1, intervals.size)
    assertEquals(2, server.requestCount)
  }

  @Test
  fun `createEvent_generates_exactly_one_calendar_version`() {
    val start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(201))

    val event =
      client.createEvent(
        email = "test@icloud.com",
        appSpecificPassword = "pass",
        calendarHref = "/calendars/test/home/",
        title = "Test Event",
        start = start,
        end = end,
      )

    val request = server.takeRequest()
    val icsBody = request.body.readUtf8()

    val versionCount = icsBody.split("\n").count { it.startsWith("VERSION:") }
    assertEquals(1, versionCount, "VERSION must appear exactly once in VCALENDAR")
    assertTrue(icsBody.contains("VERSION:2.0"), "VERSION must be 2.0")
    assertFalse(icsBody.contains("VERSION:2.0;2.0"), "VERSION must not be duplicated")
  }

  @Test
  fun `createEvent_generated_calendar_is_parseable_by_ical4j`() {
    val start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(201))

    val event =
      client.createEvent(
        email = "test@icloud.com",
        appSpecificPassword = "pass",
        calendarHref = "/calendars/test/home/",
        title = "Test Event",
        start = start,
        end = end,
      )

    val request = server.takeRequest()
    val icsBody = request.body.readUtf8()

    val calendar = CalendarBuilder().build(StringReader(icsBody))
    assertNotNull(calendar, "Generated ICS must be parseable by iCal4j")

    assertTrue(icsBody.contains("BEGIN:VCALENDAR"), "ICS must contain BEGIN:VCALENDAR")
    assertTrue(icsBody.contains("END:VCALENDAR"), "ICS must contain END:VCALENDAR")
    assertTrue(icsBody.contains("BEGIN:VEVENT"), "ICS must contain BEGIN:VEVENT")
    assertTrue(icsBody.contains("END:VEVENT"), "ICS must contain END:VEVENT")
  }

  @Test
  fun `put_authentication_exception_preserves_http_status`() {
    server.enqueue(MockResponse().setResponseCode(403))

    val exception =
      assertThrows(AppleAuthenticationException::class.java) {
        client.createEvent(
          email = "test@icloud.com",
          appSpecificPassword = "wrongpass",
          calendarHref = "/calendars/test/home/",
          title = "Test Event",
          start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC),
          end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC),
        )
      }

    assertEquals(403, exception.httpStatusCode, "HTTP 403 should be preserved in exception")
    assertTrue(
      exception.message?.contains("HTTP 403") ?: false,
      "Exception message should contain HTTP status code",
    )
  }

  @Test
  fun `put_401_unauthorized_preserves_http_status`() {
    server.enqueue(MockResponse().setResponseCode(401))

    val exception =
      assertThrows(AppleAuthenticationException::class.java) {
        client.createEvent(
          email = "test@icloud.com",
          appSpecificPassword = "wrongpass",
          calendarHref = "/calendars/test/home/",
          title = "Test Event",
          start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC),
          end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC),
        )
      }

    assertEquals(401, exception.httpStatusCode, "HTTP 401 should be preserved in exception")
  }

  @Test
  fun `different_event_uids_create_different_resource_paths`() {
    val start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(201))
    server.enqueue(MockResponse().setResponseCode(201))

    val uid1 = "terminpilot-test-uid-1"
    val uid2 = "terminpilot-test-uid-2"

    client.createEvent(
      email = "test@icloud.com",
      appSpecificPassword = "pass",
      calendarHref = "/calendars/test/home/",
      title = "Event 1",
      start = start,
      end = end,
      eventUid = uid1,
    )

    client.createEvent(
      email = "test@icloud.com",
      appSpecificPassword = "pass",
      calendarHref = "/calendars/test/home/",
      title = "Event 2",
      start = start,
      end = end,
      eventUid = uid2,
    )

    val request1 = server.takeRequest()
    val request2 = server.takeRequest()

    val path1 = request1.path ?: ""
    val path2 = request2.path ?: ""

    assertNotEquals(path1, path2, "Different UIDs must result in different resource paths")
    assertTrue(
      path1.contains("terminpilot-test-uid-1.ics"),
      "First request should have UID-1 in path",
    )
    assertTrue(
      path2.contains("terminpilot-test-uid-2.ics"),
      "Second request should have UID-2 in path",
    )
  }

  @Test
  fun `generated_ics_contains_exactly_one_uid_in_vevent`() {
    val start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(201))

    val testUid = "terminpilot-test-unique-uid"
    client.createEvent(
      email = "test@icloud.com",
      appSpecificPassword = "pass",
      calendarHref = "/calendars/test/home/",
      title = "Test Event",
      start = start,
      end = end,
      eventUid = testUid,
    )

    val request = server.takeRequest()
    val icsBody = request.body.readUtf8()

    val lines = icsBody.split(Regex("\\r?\\n"))
    var inVEvent = false
    var uidCount = 0

    for (line in lines) {
      val trimmed = line.trim()
      when {
        trimmed.startsWith("BEGIN:VEVENT") -> {
          inVEvent = true
        }

        trimmed.startsWith("END:VEVENT") -> {
          inVEvent = false
        }

        inVEvent && trimmed.startsWith("UID") -> {
          uidCount++
        }
      }
    }

    assertEquals(1, uidCount, "VEVENT must contain exactly one UID property. ICS:\n$icsBody")
    assertTrue(icsBody.contains("UID:$testUid"), "Generated ICS must contain the provided UID")
  }

  @Test
  fun `separate_random_event_uids_do_not_conflict_on_put`() {
    val start = OffsetDateTime.of(2024, 7, 29, 10, 0, 0, 0, ZoneOffset.UTC)
    val end = OffsetDateTime.of(2024, 7, 29, 11, 0, 0, 0, ZoneOffset.UTC)

    server.enqueue(MockResponse().setResponseCode(201))
    server.enqueue(MockResponse().setResponseCode(201))

    val uuid1 =
      java.util.UUID
        .randomUUID()
        .toString()
    val uuid2 =
      java.util.UUID
        .randomUUID()
        .toString()

    client.createEvent(
      email = "test@icloud.com",
      appSpecificPassword = "pass",
      calendarHref = "/calendars/test/home/",
      title = "Event A",
      start = start,
      end = end,
      eventUid = "test-$uuid1",
    )

    client.createEvent(
      email = "test@icloud.com",
      appSpecificPassword = "pass",
      calendarHref = "/calendars/test/home/",
      title = "Event B",
      start = start,
      end = end,
      eventUid = "test-$uuid2",
    )

    val request1 = server.takeRequest()
    val request2 = server.takeRequest()

    assertEquals("PUT", request1.method)
    assertEquals("PUT", request2.method)
  }
}
