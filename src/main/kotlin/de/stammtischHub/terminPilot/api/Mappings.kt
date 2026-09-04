package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.model.generated.Weekday
import java.time.DayOfWeek
import de.stammtischHub.terminPilot.model.generated.User as UserDto
import de.stammtischHub.terminPilot.persistence.entity.User as UserEntity

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

fun UserEntity.toUserDto(): UserDto =
  UserDto(
    id = this.id,
    name = this.username,
  )

fun List<UserEntity>.toUserDtoList(): List<UserDto> = this.map { it.toUserDto() }
