package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.Calendar

interface CalendarRepository {
  fun save(calendar: Calendar)

  fun findById(id: Long): Calendar?

  fun findByUserId(userId: Long): Calendar?
}
