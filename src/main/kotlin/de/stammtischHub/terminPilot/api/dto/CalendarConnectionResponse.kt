package de.stammtischHub.terminPilot.api.dto

import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import java.time.OffsetDateTime
import java.util.UUID

data class CalendarConnectionResponse(
  val id: UUID,
  val provider: String,
  val accountIdentifier: String,
  val status: String,
  val lastSyncAt: OffsetDateTime?,
  val lastErrorCode: String?,
  val lastErrorMessage: String?,
  val createdAt: OffsetDateTime,
  val updatedAt: OffsetDateTime,
) {
  companion object {
    fun from(connection: CalendarConnection) =
      CalendarConnectionResponse(
        id = connection.publicId,
        provider = connection.provider.name,
        accountIdentifier = connection.accountIdentifier,
        status = connection.status.name,
        lastSyncAt = connection.lastSyncAt,
        lastErrorCode = connection.lastErrorCode,
        lastErrorMessage = connection.lastErrorMessage,
        createdAt = connection.createdAt,
        updatedAt = connection.updatedAt,
      )
  }
}
