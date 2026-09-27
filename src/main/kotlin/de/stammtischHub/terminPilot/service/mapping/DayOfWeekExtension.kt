package de.stammtischHub.terminPilot.service.mapping

import de.stammtischHub.terminPilot.model.generated.Weekday
import java.time.DayOfWeek

fun Weekday.toDayOfWeek(): DayOfWeek =
  when (this) {
    Weekday.MONDAY -> DayOfWeek.MONDAY
    Weekday.TUESDAY -> DayOfWeek.TUESDAY
    Weekday.WEDNESDAY -> DayOfWeek.WEDNESDAY
    Weekday.THURSDAY -> DayOfWeek.THURSDAY
    Weekday.FRIDAY -> DayOfWeek.FRIDAY
    Weekday.SATURDAY -> DayOfWeek.SATURDAY
    Weekday.SUNDAY -> DayOfWeek.SUNDAY
  }

fun List<Weekday>.toDayOfWeekSet(): Set<DayOfWeek> = this.map { it.toDayOfWeek() }.toSet()
