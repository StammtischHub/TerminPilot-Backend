package de.stammtischHub.terminPilot.provider.apple

import de.stammtischHub.terminPilot.config.AppleCalDavProperties
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Tag("apple-real")
class AppleCalDavRealIntegrationTest {
  @Test
  fun `real_icloud_integration_supports_discovery_read_and_create`() {
    assumeTrue(realTestEnabled(), "Real Apple integration test is disabled")

    val email = requiredEnv("APPLE_TEST_EMAIL")
    val password = requiredEnv("APPLE_TEST_APP_PASSWORD")
    val calendarName = requiredEnv("APPLE_TEST_CALENDAR_NAME")

    val properties =
      AppleCalDavProperties(
        baseUrl = "https://caldav.icloud.com",
        connectTimeout = Duration.ofSeconds(30),
        readTimeout = Duration.ofSeconds(60),
      )
    val client =
      AppleCalDavClient(
        properties,
        CalDavHttpClient(properties),
        CalDavXmlParser(),
        ICalParser(),
      )

    val discoveredCalendars =
      retryOnUnavailable {
        client.discoverCalendars(email, password)
      }
    assertTrue(discoveredCalendars.isNotEmpty(), "Expected at least one Apple calendar")

    val targetCalendar = discoveredCalendars.firstOrNull { it.displayName == calendarName }
    assertNotNull(targetCalendar, "Configured Apple test calendar was not found")
    val selectedCalendar = requireNotNull(targetCalendar)

    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val start =
      now
        .plusDays(2)
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    val end = start.plusMinutes(15)

    val runId = UUID.randomUUID().toString()
    val eventUid = "terminpilot-integration-$runId"
    val title = "TerminPilot Integration Test $runId"

    val beforeCreateBusy =
      retryOnUnavailable {
        client.fetchBusyIntervals(
          email,
          password,
          selectedCalendar.href,
          start.minusHours(12),
          end.plusHours(12),
        )
      }
    assertNotNull(beforeCreateBusy)

    val created =
      client.createEvent(
        email = email,
        appSpecificPassword = password,
        calendarHref = selectedCalendar.href,
        title = title,
        start = start,
        end = end,
        eventUid = eventUid,
      )
    assertTrue(created.resourceHref.endsWith(".ics"), "Expected a calendar object resource ending in .ics")
    assertTrue(created.uid == eventUid, "Expected UID to match the provided eventUid")

    val eventVisibleAsBusy =
      waitUntilBusyIntervalVisible(
        client = client,
        email = email,
        password = password,
        calendarHref = selectedCalendar.href,
        expectedStart = start,
        expectedEnd = end,
      )

    assertTrue(eventVisibleAsBusy, "Created Apple event was not visible as busy time after re-query")
  }

  private fun waitUntilBusyIntervalVisible(
    client: AppleCalDavClient,
    email: String,
    password: String,
    calendarHref: String,
    expectedStart: OffsetDateTime,
    expectedEnd: OffsetDateTime,
  ): Boolean {
    repeat(15) {
      val busyIntervals =
        retryOnUnavailable {
          client.fetchBusyIntervals(
            email = email,
            appSpecificPassword = password,
            calendarHref = calendarHref,
            from = expectedStart.minusHours(12),
            to = expectedEnd.plusHours(12),
          )
        }
      val found =
        busyIntervals.any { interval ->
          interval.startTime < expectedEnd && interval.endTime > expectedStart
        }
      if (found) {
        return true
      }
      Thread.sleep(4_000)
    }
    return false
  }

  private fun <T> retryOnUnavailable(block: () -> T): T {
    var last: AppleCalDavUnavailableException? = null
    repeat(3) { attempt ->
      try {
        return block()
      } catch (ex: AppleCalDavUnavailableException) {
        last = ex
        if (attempt < 2) {
          Thread.sleep(2_000L * (attempt + 1))
        }
      }
    }
    throw requireNotNull(last)
  }

  private fun realTestEnabled(): Boolean =
    readConfig("APPLE_REAL_INTEGRATION_TEST_ENABLED").equals("true", ignoreCase = true)

  private fun requiredEnv(name: String): String {
    val value = readConfig(name)
    assumeTrue(value.isNotEmpty(), "Configuration $name is missing (env var or system property)")
    return value
  }

  private fun readConfig(name: String): String =
    System.getenv(name)?.trim().orEmpty().ifEmpty {
      System.getProperty(name)?.trim().orEmpty()
    }
}
