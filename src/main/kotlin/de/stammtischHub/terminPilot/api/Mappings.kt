package de.stammtischHub.terminPilot.api

import de.stammtischHub.terminPilot.model.generated.Constraints
import java.time.DayOfWeek
import de.stammtischHub.terminPilot.model.generated.User as UserDto
import de.stammtischHub.terminPilot.persistence.entity.User as UserEntity

fun Constraints.Weekdays.toDayOfWeek(): DayOfWeek =
  when (this) {
    Constraints.Weekdays.MONDAY -> DayOfWeek.MONDAY
    Constraints.Weekdays.TUESDAY -> DayOfWeek.TUESDAY
    Constraints.Weekdays.WEDNESDAY -> DayOfWeek.WEDNESDAY
    Constraints.Weekdays.THURSDAY -> DayOfWeek.THURSDAY
    Constraints.Weekdays.FRIDAY -> DayOfWeek.FRIDAY
    Constraints.Weekdays.SATURDAY -> DayOfWeek.SATURDAY
    Constraints.Weekdays.SUNDAY -> DayOfWeek.SUNDAY
  }

fun Set<Constraints.Weekdays>.toDayOfWeekSet(): Set<DayOfWeek> = this.map { it.toDayOfWeek() }.toSet()

fun UserEntity.toUserDto(): UserDto =
  UserDto(
    id = this.id!!,
    name = this.username,
  )

fun List<UserEntity>.toUserDtoList(): List<UserDto> = this.map { it.toUserDto() }
