package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ProviderType
import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CalendarConnectionRepository : JpaRepository<CalendarConnection, Long> {
  fun findAllByUser(user: User): List<CalendarConnection>

  fun findByPublicId(publicId: UUID): CalendarConnection?

  fun findByUserAndProviderAndAccountIdentifier(
    user: User,
    provider: ProviderType,
    accountIdentifier: String,
  ): CalendarConnection?
}
