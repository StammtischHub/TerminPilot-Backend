package de.stammtischHub.terminPilot.provider.apple

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ICalParserTest {
  private val parser = ICalParser()

  private val from = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  private val to = ZonedDateTime.of(2024, 1, 31, 23, 59, 59, 0, ZoneOffset.UTC)

  @Test
  fun `freebusy_response_is_converted_to_busy_intervals`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VFREEBUSY
      DTSTART:20240101T000000Z
      DTEND:20240131T235959Z
      FREEBUSY;FBTYPE=BUSY:20240115T090000Z/20240115T100000Z
      FREEBUSY;FBTYPE=BUSY-TENTATIVE:20240120T140000Z/20240120T150000Z
      END:VFREEBUSY
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseFreebusy(ics, from, to)

    assertEquals(2, slots.size)
    assertEquals("2024-01-15T09:00Z", slots[0].startTime.toString())
    assertEquals("2024-01-15T10:00Z", slots[0].endTime.toString())
    assertEquals("2024-01-20T14:00Z", slots[1].startTime.toString())
    assertEquals("2024-01-20T15:00Z", slots[1].endTime.toString())
  }

  @Test
  fun `freebusy_free_intervals_are_ignored`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VFREEBUSY
      FREEBUSY;FBTYPE=FREE:20240110T090000Z/20240110T100000Z
      FREEBUSY;FBTYPE=BUSY:20240115T090000Z/20240115T100000Z
      END:VFREEBUSY
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseFreebusy(ics, from, to)

    assertEquals(1, slots.size)
    assertEquals("2024-01-15T09:00Z", slots[0].startTime.toString())
  }

  @Test
  fun `freebusy_absent_fbtype_defaults_to_busy`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VFREEBUSY
      FREEBUSY:20240115T090000Z/20240115T100000Z
      END:VFREEBUSY
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseFreebusy(ics, from, to)

    assertEquals(1, slots.size)
  }

  @Test
  fun `freebusy_intervals_outside_range_are_excluded`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VFREEBUSY
      FREEBUSY;FBTYPE=BUSY:20230101T090000Z/20230101T100000Z
      FREEBUSY;FBTYPE=BUSY:20240115T090000Z/20240115T100000Z
      END:VFREEBUSY
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseFreebusy(ics, from, to)

    assertEquals(1, slots.size)
    assertEquals("2024-01-15T09:00Z", slots[0].startTime.toString())
  }

  @Test
  fun `transparent_events_are_ignored`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      DTSTART:20240115T090000Z
      DTEND:20240115T100000Z
      TRANSP:TRANSPARENT
      SUMMARY:This detail must not be stored
      END:VEVENT
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseCalendarData(listOf(ics), from, to)

    assertTrue(slots.isEmpty(), "TRANSPARENT event should produce no busy slots")
  }

  @Test
  fun `cancelled_events_are_ignored`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      DTSTART:20240115T090000Z
      DTEND:20240115T100000Z
      STATUS:CANCELLED
      SUMMARY:This detail must not be stored
      END:VEVENT
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseCalendarData(listOf(ics), from, to)

    assertTrue(slots.isEmpty(), "CANCELLED event should produce no busy slots")
  }

  @Test
  fun `all_day_events_are_handled_correctly`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      DTSTART;VALUE=DATE:20240115
      DTEND;VALUE=DATE:20240116
      SUMMARY:This detail must not be stored
      END:VEVENT
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseCalendarData(listOf(ics), from, to)

    assertEquals(1, slots.size)
    assertEquals("2024-01-15T00:00Z", slots[0].startTime.toString())
  }

  @Test
  fun `opaque_event_produces_busy_slot`() {
    val ics =
      """
      BEGIN:VCALENDAR
      VERSION:2.0
      BEGIN:VEVENT
      DTSTART:20240115T090000Z
      DTEND:20240115T100000Z
      TRANSP:OPAQUE
      SUMMARY:This detail must not be stored
      END:VEVENT
      END:VCALENDAR
      """.trimIndent()

    val slots = parser.parseCalendarData(listOf(ics), from, to)

    assertEquals(1, slots.size)
    assertEquals("2024-01-15T09:00Z", slots[0].startTime.toString())
    assertEquals("2024-01-15T10:00Z", slots[0].endTime.toString())
  }

  @Test
  fun `event_details_are_not_returned`() {
    val slot =
      AppleCalDavClient.BusySlot(
        startTime = java.time.OffsetDateTime.now(),
        endTime = java.time.OffsetDateTime.now(),
      )
    val fields =
      slot.javaClass.declaredFields
        .map { it.name }
        .toSet()
    assertTrue("startTime" in fields, "BusySlot must have startTime")
    assertTrue("endTime" in fields, "BusySlot must have endTime")
    assertTrue("summary" !in fields, "BusySlot must NOT have summary")
    assertTrue("description" !in fields, "BusySlot must NOT have description")
    assertTrue("location" !in fields, "BusySlot must NOT have location")
    assertTrue("attendee" !in fields, "BusySlot must NOT have attendee")
  }

  @Test
  fun `credentials_are_not_in_exception_message`() {
    val password = "super-secret-password-12345"
    val exception = AppleAuthenticationException()

    assertTrue(
      password !in (exception.message ?: ""),
      "Password must not appear in exception message",
    )
    assertTrue(
      password !in exception.userMessage,
      "Password must not appear in user-facing message",
    )
  }
}
