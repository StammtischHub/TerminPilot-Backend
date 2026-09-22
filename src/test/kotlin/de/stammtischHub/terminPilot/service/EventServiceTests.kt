package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.domain.Event
import de.stammtischHub.terminPilot.domain.ScoredSlot
import de.stammtischHub.terminPilot.domain.TimeSlot
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import de.stammtischHub.terminPilot.provider.CalendarProvider
import de.stammtischHub.terminPilot.domain.SlotCoverage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Tests für [EventService], gegliedert per [Nested]-Klasse.
 */
class EventServiceTest {
  private lateinit var calendarProvider: CalendarProvider
  private lateinit var userRepository: UserRepository
  private lateinit var eventService: EventService
  private lateinit var user: User

  @BeforeEach
  fun setUp() {
    calendarProvider = mockk()
    userRepository = mockk()
    eventService = EventService(userRepository, calendarProvider)
    user = mockk()
    every { user.id } returns 1L
  }

  private fun stubBusyEvents(events: List<Event>) {
    every {
      calendarProvider.getCalendarForTimespan(any(), any(), any())
    } returns events
  }

  private fun busyEvent(
    start: LocalDateTime,
    end: LocalDateTime,
  ): Event =
    Event(
      title = "Busy",
      start = start,
      end = end,
      participants = emptyList(),
      location = null,
      description = null,
    )

  private fun invokeGetFreeSlots(
    weekdays: Set<DayOfWeek>,
    dateRange: ClosedRange<LocalDate>,
    timeRange: ClosedRange<LocalTime>,
  ): List<TimeSlot> {
    val method =
      EventService::class.java.getDeclaredMethod(
        "getFreeSlotsForParticipant",
        User::class.java,
        Set::class.java,
        ClosedRange::class.java,
        ClosedRange::class.java,
      )
    method.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    return method.invoke(eventService, user, weekdays, dateRange, timeRange) as List<TimeSlot>
  }

  @Nested
  inner class GetFreeSlotsForParticipant {
    @Test
    @DisplayName("Keine Busy-Events → das komplette Zeitfenster ist frei")
    fun noBusyEvents() {
      stubBusyEvents(emptyList())
      val monday = LocalDate.of(2026, 9, 21)

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(TimeSlot(monday.atTime(9, 0), monday.atTime(17, 0))),
        result,
      )
    }

    @Test
    @DisplayName("Busy-Event vollständig im Fenster teilt es in zwei freie Slots")
    fun busyEventInsideWindow() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(listOf(busyEvent(monday.atTime(12, 0), monday.atTime(13, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(
          TimeSlot(monday.atTime(9, 0), monday.atTime(12, 0)),
          TimeSlot(monday.atTime(13, 0), monday.atTime(17, 0)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Busy-Event deckt das komplette Fenster ab → keine freien Slots")
    fun busyEventCoversWholeWindow() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(listOf(busyEvent(monday.atTime(9, 0), monday.atTime(17, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(emptyList<TimeSlot>(), result)
    }

    @Test
    @DisplayName("Busy-Event ragt über beide Fenstergrenzen hinaus → wird auf das Fenster geclippt")
    fun busyEventOverlapsWindowBoundaries() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(listOf(busyEvent(monday.atTime(7, 0), monday.atTime(19, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(emptyList<TimeSlot>(), result)
    }

    @Test
    @DisplayName("Busy-Event komplett vor dem Fenster wird ignoriert")
    fun busyEventBeforeWindow() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(listOf(busyEvent(monday.atTime(6, 0), monday.atTime(8, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(TimeSlot(monday.atTime(9, 0), monday.atTime(17, 0))),
        result,
      )
    }

    @Test
    @DisplayName("Busy-Event komplett nach dem Fenster wird ignoriert")
    fun busyEventAfterWindow() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(listOf(busyEvent(monday.atTime(18, 0), monday.atTime(19, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(TimeSlot(monday.atTime(9, 0), monday.atTime(17, 0))),
        result,
      )
    }

    @Test
    @DisplayName("Überlappende Busy-Events werden vor der Invertierung gemerged")
    fun overlappingBusyEventsAreMerged() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(
        listOf(
          busyEvent(monday.atTime(10, 0), monday.atTime(12, 0)),
          busyEvent(monday.atTime(11, 0), monday.atTime(13, 0)),
        ),
      )

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(
          TimeSlot(monday.atTime(9, 0), monday.atTime(10, 0)),
          TimeSlot(monday.atTime(13, 0), monday.atTime(17, 0)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Direkt angrenzende (berührende) Busy-Events werden zu einem gemerged")
    fun adjacentBusyEventsAreMerged() {
      val monday = LocalDate.of(2026, 9, 21)
      stubBusyEvents(
        listOf(
          busyEvent(monday.atTime(10, 0), monday.atTime(11, 0)),
          busyEvent(monday.atTime(11, 0), monday.atTime(12, 0)),
        ),
      )

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY),
          monday..monday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(
          TimeSlot(monday.atTime(9, 0), monday.atTime(10, 0)),
          TimeSlot(monday.atTime(12, 0), monday.atTime(17, 0)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Nur die im weekdays-Set enthaltenen Tage liefern freie Slots")
    fun onlyMatchingWeekdaysAreConsidered() {
      // Mo 2026-09-21 .. So 2026-09-27
      val monday = LocalDate.of(2026, 9, 21)
      val sunday = LocalDate.of(2026, 9, 27)
      val wednesday = monday.plusDays(2)
      stubBusyEvents(emptyList())

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
          monday..sunday,
          LocalTime.of(9, 0)..LocalTime.of(10, 0),
        )

      assertEquals(
        listOf(
          TimeSlot(monday.atTime(9, 0), monday.atTime(10, 0)),
          TimeSlot(wednesday.atTime(9, 0), wednesday.atTime(10, 0)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Ein Busy-Event an einem Tag beeinflusst nicht die freien Slots anderer Tage")
    fun busyEventDoesNotLeakAcrossDays() {
      val monday = LocalDate.of(2026, 9, 21)
      val tuesday = monday.plusDays(1)
      stubBusyEvents(listOf(busyEvent(monday.atTime(9, 0), monday.atTime(17, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
          monday..tuesday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(TimeSlot(tuesday.atTime(9, 0), tuesday.atTime(17, 0))),
        result,
      )
    }

    @Test
    @DisplayName("Ein Busy-Event über Mitternacht hinweg wird pro Tag separat geclippt")
    fun busyEventSpanningMultipleDaysIsClippedPerDay() {
      val monday = LocalDate.of(2026, 9, 21)
      val tuesday = monday.plusDays(1)
      // Busy von Montag 20:00 bis Dienstag 10:00
      stubBusyEvents(listOf(busyEvent(monday.atTime(20, 0), tuesday.atTime(10, 0))))

      val result =
        invokeGetFreeSlots(
          setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
          monday..tuesday,
          LocalTime.of(9, 0)..LocalTime.of(17, 0),
        )

      assertEquals(
        listOf(
          // Montag: Busy beginnt erst 20:00, also nach dem Fensterende 17:00 → ganzer Tag frei
          TimeSlot(monday.atTime(9, 0), monday.atTime(17, 0)),
          // Dienstag: Busy endet 10:00 innerhalb des Fensters → Rest ab 10:00 frei
          TimeSlot(tuesday.atTime(10, 0), tuesday.atTime(17, 0)),
        ),
        result,
      )
    }
  }

  @Nested
  inner class IntersectFreeSlots {
    private fun invoke(freeSlotsPerParticipant: Map<Long, List<TimeSlot>>): List<SlotCoverage> {
      val method =
        EventService::class.java.getDeclaredMethod(
          "intersectFreeSlots",
          Map::class.java,
        )
      method.isAccessible = true

      @Suppress("UNCHECKED_CAST")
      return method.invoke(eventService, freeSlotsPerParticipant) as List<SlotCoverage>
    }

    @Test
    @DisplayName("Leere Map → keine Segmente")
    fun noParticipants() {
      val result = invoke(emptyMap())
      assertEquals(emptyList<SlotCoverage>(), result)
    }

    @Test
    @DisplayName("Ein Teilnehmer, ein Slot → ein Segment mit genau diesem Teilnehmer")
    fun singleParticipantSingleSlot() {
      val day = LocalDate.of(2026, 9, 21)
      val input = mapOf(1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))))

      val result = invoke(input)

      assertEquals(
        listOf(SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L))),
        result,
      )
    }

    @Test
    @DisplayName("Zwei Teilnehmer, identische Slots → ein Segment mit beiden Teilnehmern")
    fun twoParticipantsFullyOverlapping() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
          2L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
        )

      val result = invoke(input)

      assertEquals(
        listOf(SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L, 2L))),
        result,
      )
    }

    @Test
    @DisplayName("Teilweise überlappende Slots ergeben drei Segmente mit unterschiedlicher Coverage")
    fun partiallyOverlappingSlots() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(12, 0))),
          2L to listOf(TimeSlot(day.atTime(10, 0), day.atTime(13, 0))),
        )

      val result = invoke(input)

      assertEquals(
        listOf(
          SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L)),
          SlotCoverage(TimeSlot(day.atTime(10, 0), day.atTime(12, 0)), setOf(1L, 2L)),
          SlotCoverage(TimeSlot(day.atTime(12, 0), day.atTime(13, 0)), setOf(2L)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Nicht überlappende Slots erzeugen zwei separate Segmente, die Lücke dazwischen fehlt")
    fun nonOverlappingSlotsLeaveGapOut() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
          2L to listOf(TimeSlot(day.atTime(12, 0), day.atTime(13, 0))),
        )

      val result = invoke(input)

      assertEquals(
        listOf(
          SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L)),
          SlotCoverage(TimeSlot(day.atTime(12, 0), day.atTime(13, 0)), setOf(2L)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Direkt aneinandergrenzende Slots verschiedener Teilnehmer erzeugen kein leeres Zwischensegment")
    fun adjacentSlotsOfDifferentParticipantsDoNotCreateEmptySegment() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(11, 0))),
          2L to listOf(TimeSlot(day.atTime(11, 0), day.atTime(13, 0))),
        )

      val result = invoke(input)

      assertEquals(
        listOf(
          SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(11, 0)), setOf(1L)),
          SlotCoverage(TimeSlot(day.atTime(11, 0), day.atTime(13, 0)), setOf(2L)),
        ),
        result,
      )
    }

    @Test
    @DisplayName("Drei Teilnehmer exakt gleichzeitig frei → genau ein Segment mit allen drei, keine Duplikate")
    fun threeParticipantsSimultaneouslyFree() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
          2L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
          3L to listOf(TimeSlot(day.atTime(9, 0), day.atTime(10, 0))),
        )

      val result = invoke(input)

      assertEquals(
        listOf(SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L, 2L, 3L))),
        result,
      )
    }

    @Test
    @DisplayName("Mehrere getrennte Slots desselben Teilnehmers ergeben mehrere unabhängige Segmente")
    fun sameParticipantMultipleNonContiguousSlots() {
      val day = LocalDate.of(2026, 9, 21)
      val input =
        mapOf(
          1L to
            listOf(
              TimeSlot(day.atTime(9, 0), day.atTime(10, 0)),
              TimeSlot(day.atTime(14, 0), day.atTime(15, 0)),
            ),
        )

      val result = invoke(input)

      assertEquals(
        listOf(
          SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L)),
          SlotCoverage(TimeSlot(day.atTime(14, 0), day.atTime(15, 0)), setOf(1L)),
        ),
        result,
      )
    }
  }

  @Nested
  inner class ScoreFreeSlot {
    private fun invoke(
      segments: List<SlotCoverage>,
      duration: Int,
      timeRange: ClosedRange<LocalTime>,
      totalParticipants: Int,
      topN: Int,
    ): List<ScoredSlot> {
      val method =
        EventService::class.java.getDeclaredMethod(
          "scoreFreeSlot",
          List::class.java,
          Int::class.javaPrimitiveType,
          ClosedRange::class.java,
          Int::class.javaPrimitiveType,
          Int::class.javaPrimitiveType,
        )
      method.isAccessible = true

      @Suppress("UNCHECKED_CAST")
      return method.invoke(eventService, segments, duration, timeRange, totalParticipants, topN) as List<ScoredSlot>
    }

    private fun assertScoredSlot(
      expectedSlot: TimeSlot,
      expectedParticipants: Set<Long>,
      expectedScore: Double,
      actual: ScoredSlot,
    ) {
      assertEquals(expectedSlot, actual.slot)
      assertEquals(expectedParticipants, actual.freeParticipantIds)
      assertEquals(expectedScore, actual.score, 1e-9)
    }

    @Test
    @DisplayName("Leere Segment-Liste → keine Vorschläge")
    fun noSegments() {
      val result =
        invoke(
          emptyList(),
          duration = 60,
          timeRange = LocalTime.of(9, 0)..LocalTime.of(17, 0),
          totalParticipants = 2,
          topN = 5,
        )

      assertEquals(emptyList<ScoredSlot>(), result)
    }

    @Test
    @DisplayName("Segmente mit unvollständiger Coverage werden ausgeschlossen ('volle Besetzung MUSS dominieren')")
    fun partialCoverageIsExcluded() {
      val day = LocalDate.of(2026, 9, 21)
      val fullyCovered = SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(9, 30)), setOf(1L, 2L, 3L))
      val partiallyCovered = SlotCoverage(TimeSlot(day.atTime(10, 0), day.atTime(10, 30)), setOf(1L, 2L))

      val result =
        invoke(
          listOf(fullyCovered, partiallyCovered),
          duration = 30,
          timeRange = LocalTime.of(9, 0)..LocalTime.of(17, 0),
          totalParticipants = 3,
          topN = 5,
        )

      assertEquals(1, result.size)
      assertScoredSlot(TimeSlot(day.atTime(9, 0), day.atTime(9, 30)), setOf(1L, 2L, 3L), 1.0, result.single())
    }

    @Test
    @DisplayName("Segment exakt duration-lang → ein Kandidat mit perfektem Score, wenn er am Fensterstart liegt")
    fun exactDurationSegmentAtWindowStartScoresPerfectly() {
      val day = LocalDate.of(2026, 9, 21)
      val segment = SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L, 2L))

      val result =
        invoke(
          listOf(segment),
          duration = 60,
          timeRange = LocalTime.of(9, 0)..LocalTime.of(17, 0),
          totalParticipants = 2,
          topN = 5,
        )

      assertEquals(1, result.size)
      assertScoredSlot(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L, 2L), 1.0, result.single())
    }

    @Test
    @DisplayName("Segment länger als duration → früher und zentrierter Kandidat, zentriert gewinnt bei Segmentstart am Fensteranfang")
    fun longerSegmentGeneratesTwoCandidatesCenteredWins() {
      val day = LocalDate.of(2026, 9, 21)
      val segment = SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(11, 0)), setOf(1L))

      val result =
        invoke(
          listOf(segment),
          duration = 60,
          timeRange = LocalTime.of(9, 0)..LocalTime.of(17, 0),
          totalParticipants = 1,
          topN = 5,
        )

      assertEquals(2, result.size)
      assertScoredSlot(TimeSlot(day.atTime(9, 30), day.atTime(10, 30)), setOf(1L), 0.9642857142857143, result[0])
      assertScoredSlot(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L), 0.5, result[1])
    }

    @Test
    @DisplayName("topN begrenzt die Ergebnisliste auf die höchstbewerteten Kandidaten")
    fun topNLimitsToHighestScored() {
      val day = LocalDate.of(2026, 9, 21)
      val segments =
        listOf(
          SlotCoverage(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L)), // Score 1.0
          SlotCoverage(TimeSlot(day.atTime(10, 0), day.atTime(11, 0)), setOf(1L)), // Score ~0.9286
          SlotCoverage(TimeSlot(day.atTime(12, 0), day.atTime(13, 0)), setOf(1L)), // Score ~0.7857
          SlotCoverage(TimeSlot(day.atTime(15, 0), day.atTime(16, 0)), setOf(1L)), // Score ~0.5714
        )

      val result =
        invoke(
          segments,
          duration = 60,
          timeRange = LocalTime.of(9, 0)..LocalTime.of(17, 0),
          totalParticipants = 1,
          topN = 2,
        )

      assertEquals(2, result.size)
      assertScoredSlot(TimeSlot(day.atTime(9, 0), day.atTime(10, 0)), setOf(1L), 1.0, result[0])
      assertScoredSlot(TimeSlot(day.atTime(10, 0), day.atTime(11, 0)), setOf(1L), 0.9285714285714286, result[1])
    }
  }
}
