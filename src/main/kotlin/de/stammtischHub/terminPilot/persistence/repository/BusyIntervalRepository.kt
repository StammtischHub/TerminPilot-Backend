package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.BusyInterval
import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface BusyIntervalRepository : JpaRepository<BusyInterval, Long> {
  fun findAllByConnectionAndStartTimeBetween(
    connection: CalendarConnection,
    from: OffsetDateTime,
    to: OffsetDateTime,
  ): List<BusyInterval>

  fun deleteAllByExternalCalendar(externalCalendar: ExternalCalendar)

  fun deleteAllByConnection(connection: CalendarConnection)
}
