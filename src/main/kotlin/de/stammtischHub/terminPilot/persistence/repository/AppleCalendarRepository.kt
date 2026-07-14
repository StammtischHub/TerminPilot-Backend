package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.AppleCalendar

interface AppleCalendarRepository {
  fun save(appleCalendar: AppleCalendar)

  fun findById(id: Long): AppleCalendar?
}
