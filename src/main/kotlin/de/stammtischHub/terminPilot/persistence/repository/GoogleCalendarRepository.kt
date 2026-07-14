package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar

interface GoogleCalendarRepository {
  fun save(googleCalendar: GoogleCalendar)

  fun findById(id: Long): GoogleCalendar?
}
