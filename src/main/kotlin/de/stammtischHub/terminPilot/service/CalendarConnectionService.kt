package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ConnectionStatus
import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import de.stammtischHub.terminPilot.persistence.entity.ProviderType
import de.stammtischHub.terminPilot.persistence.repository.BusyIntervalRepository
import de.stammtischHub.terminPilot.persistence.repository.CalendarConnectionRepository
import de.stammtischHub.terminPilot.persistence.repository.ExternalCalendarRepository
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CalendarConnectionService(
  private val calendarConnectionRepository: CalendarConnectionRepository,
  private val externalCalendarRepository: ExternalCalendarRepository,
  private val busyIntervalRepository: BusyIntervalRepository,
  private val userRepository: UserRepository,
  private val credentialEncryptionService: CredentialEncryptionService,
  private val calendarSyncService: CalendarSyncService,
) {
  private val log = LoggerFactory.getLogger(CalendarConnectionService::class.java)

  @Transactional
  fun connectAppleCalendar(
    userId: Long,
    email: String,
    appSpecificPassword: String,
  ): CalendarConnection {
    val user =
      userRepository
        .findById(userId)
        .orElseThrow { NoSuchElementException("User $userId not found") }

    calendarConnectionRepository
      .findByUserAndProviderAndAccountIdentifier(user, ProviderType.APPLE, email)
      ?.let { throw IllegalStateException("Apple Calendar already connected for account: $email") }

    val encrypted = credentialEncryptionService.encrypt(appSpecificPassword)
    val connection =
      CalendarConnection(
        user = user,
        provider = ProviderType.APPLE,
        accountIdentifier = email,
        encryptedCredential = encrypted,
      )
    calendarConnectionRepository.save(connection)
    log.info("Created CalendarConnection id={} for userId={}, provider=APPLE", connection.id, userId)

    try {
      calendarSyncService.syncConnection(connection)
    } catch (ex: Exception) {
      log.warn(
        "Initial sync failed for connection id={} (status will reflect the error): {}",
        connection.id,
        ex.message,
      )
    }

    return connection
  }

  fun listConnections(userId: Long): List<CalendarConnection> {
    val user =
      userRepository
        .findById(userId)
        .orElseThrow { NoSuchElementException("User $userId not found") }
    return calendarConnectionRepository.findAllByUser(user)
  }

  @Transactional
  fun disconnect(
    userId: Long,
    connectionId: UUID,
  ) {
    val connection = resolveOwned(userId, connectionId)

    val calendars = externalCalendarRepository.findAllByConnection(connection)
    calendars.forEach { busyIntervalRepository.deleteAllByExternalCalendar(it) }
    externalCalendarRepository.deleteAllByConnection(connection)

    connection.encryptedCredential = ""
    connection.status = ConnectionStatus.DISCONNECTED
    calendarConnectionRepository.delete(connection)

    log.info("Disconnected CalendarConnection id={} for userId={}", connectionId, userId)
  }

  @Transactional
  fun triggerSync(
    userId: Long,
    connectionId: UUID,
  ): CalendarConnection {
    val connection = resolveOwned(userId, connectionId)
    calendarSyncService.syncConnection(connection)
    return connection
  }

  fun listCalendars(
    userId: Long,
    connectionId: UUID,
  ): List<ExternalCalendar> {
    val connection = resolveOwned(userId, connectionId)
    return externalCalendarRepository.findAllByConnection(connection)
  }

  @Transactional
  fun updateCalendarSelection(
    userId: Long,
    connectionId: UUID,
    calendarId: UUID,
    selected: Boolean,
  ): ExternalCalendar {
    val connection = resolveOwned(userId, connectionId)

    val calendar =
      externalCalendarRepository
        .findByPublicId(calendarId)
        ?: throw NoSuchElementException("ExternalCalendar $calendarId not found")

    if (calendar.connection.publicId != connectionId) {
      throw AccessDeniedException("ExternalCalendar $calendarId does not belong to connection $connectionId")
    }

    calendar.selected = selected
    if (!selected) {
      busyIntervalRepository.deleteAllByExternalCalendar(calendar)
    }
    return externalCalendarRepository.save(calendar)
  }

  private fun resolveOwned(
    userId: Long,
    connectionId: UUID,
  ): CalendarConnection {
    val user =
      userRepository
        .findById(userId)
        .orElseThrow { NoSuchElementException("User $userId not found") }
    val connection =
      calendarConnectionRepository
        .findByPublicId(connectionId)
        ?: throw NoSuchElementException("CalendarConnection $connectionId not found")
    if (connection.user.id != user.id) {
      throw AccessDeniedException("CalendarConnection $connectionId does not belong to user $userId")
    }
    return connection
  }
}
