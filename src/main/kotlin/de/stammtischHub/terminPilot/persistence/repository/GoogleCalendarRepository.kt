package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GoogleCalendarRepository : CrudRepository<GoogleCalendar, Long>
