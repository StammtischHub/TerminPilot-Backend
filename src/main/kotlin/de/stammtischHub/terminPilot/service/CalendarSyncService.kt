package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.config.CalendarSyncProperties
import de.stammtischHub.terminPilot.persistence.entity.BusyInterval
import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ConnectionStatus
import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import de.stammtischHub.terminPilot.persistence.entity.ProviderType
import de.stammtischHub.terminPilot.persistence.repository.BusyIntervalRepository
import de.stammtischHub.terminPilot.persistence.repository.CalendarConnectionRepository
import de.stammtischHub.terminPilot.persistence.repository.ExternalCalendarRepository
import de.stammtischHub.terminPilot.provider.apple.AppleCalDavClient
import de.stammtischHub.terminPilot.provider.apple.AppleCalDavException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class CalendarSyncService(
  private val calendarConnectionRepository: CalendarConnectionRepository,
  private val externalCalendarRepository: ExternalCalendarRepository,
  private val busyIntervalRepository: BusyIntervalRepository,
  private val credentialEncryptionService: CredentialEncryptionService,
  private val appleCalDavClient: AppleCalDavClient,
  private val calendarSyncProperties: CalendarSyncProperties,
) {
  private val log = LoggerFactory.getLogger(CalendarSyncService::class.java)

  @Transactional
  fun syncConnection(connection: CalendarConnection) {
    log.info(
      "Starting sync for CalendarConnection id={}, provider={}",
      connection.id,
      connection.provider,
    )
    try {
      if (connection.provider != ProviderType.APPLE) {
        throw IllegalArgumentException("Unsupported provider for Apple sync: ${connection.provider}")
      }
      syncApple(connection)
      connection.status = ConnectionStatus.ACTIVE
      connection.lastSyncAt = OffsetDateTime.now()
      connection.lastErrorCode = null
      connection.lastErrorMessage = null
      log.info("Sync completed successfully for connection id={}", connection.id)
    } catch (ex: AppleCalDavException) {
      log.warn("Apple CalDAV error for connection id={}: code={}", connection.id, ex.errorCode)
      updateError(connection, ex.errorCode, ex.userMessage)
    } catch (ex: Exception) {
      log.error("Sync failed for connection id={}", connection.id, ex)
      updateError(connection, "SYNC_FAILED", ex.message)
    } finally {
      connection.updatedAt = OffsetDateTime.now()
      calendarConnectionRepository.save(connection)
    }
  }

  private fun syncApple(connection: CalendarConnection) {
    val email = connection.accountIdentifier
    val password = credentialEncryptionService.decrypt(connection.encryptedCredential)

    val discovered = appleCalDavClient.discoverCalendars(email, password)
    log.info("Discovered {} calendar(s) for connection id={}", discovered.size, connection.id)

    val existingByHref = mutableMapOf<String, ExternalCalendar>()
    for (calendar in externalCalendarRepository.findAllByConnection(connection)) {
      existingByHref[calendar.externalHref] = calendar
    }
    val selectedCalendars = mutableListOf<ExternalCalendar>()
    for (discoveredCalendar in discovered) {
      var calendar = existingByHref[discoveredCalendar.href]
      if (calendar == null) {
        calendar =
          ExternalCalendar(
            connection = connection,
            externalHref = discoveredCalendar.href,
            displayName = discoveredCalendar.displayName,
          )
        calendar = externalCalendarRepository.save(calendar)
      }
      if (calendar.selected) {
        selectedCalendars += calendar
      }
    }

    val from = OffsetDateTime.now()
    val to = from.plusDays(calendarSyncProperties.lookaheadDays)

    for (calendar in selectedCalendars) {
      busyIntervalRepository.deleteAllByExternalCalendar(calendar)
      val slots = appleCalDavClient.fetchBusyIntervals(email, password, calendar.externalHref, from, to)
      for (slot in slots) {
        busyIntervalRepository.save(
          BusyInterval(
            connection = connection,
            externalCalendar = calendar,
            startTime = slot.startTime,
            endTime = slot.endTime,
          ),
        )
      }
      log.info(
        "Imported {} busy interval(s) for calendar id={}, connection id={}",
        slots.size,
        calendar.id,
        connection.id,
      )
    }
  }

  private fun updateError(
    connection: CalendarConnection,
    code: String,
    message: String?,
  ) {
    connection.status = ConnectionStatus.ERROR
    connection.lastErrorCode = code
    connection.lastErrorMessage = message
  }
}
