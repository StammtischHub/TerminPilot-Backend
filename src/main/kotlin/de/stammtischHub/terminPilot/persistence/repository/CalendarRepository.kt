package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.Calendar
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CalendarRepository : CrudRepository<Calendar, Long> {
  fun findByUserId(userId: Long): Optional<Iterable<Calendar>>
}
