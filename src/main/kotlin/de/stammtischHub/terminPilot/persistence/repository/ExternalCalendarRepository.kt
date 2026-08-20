package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExternalCalendarRepository : JpaRepository<ExternalCalendar, Long> {
  fun findAllByConnection(connection: CalendarConnection): List<ExternalCalendar>

  fun findByPublicId(publicId: UUID): ExternalCalendar?

  fun deleteAllByConnection(connection: CalendarConnection)
}
