package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.persistence.entity.CalendarConnection
import de.stammtischHub.terminPilot.persistence.entity.ConnectionStatus
import de.stammtischHub.terminPilot.persistence.entity.ExternalCalendar
import de.stammtischHub.terminPilot.persistence.entity.ProviderType
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.repository.BusyIntervalRepository
import de.stammtischHub.terminPilot.persistence.repository.CalendarConnectionRepository
import de.stammtischHub.terminPilot.persistence.repository.ExternalCalendarRepository
import de.stammtischHub.terminPilot.persistence.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CalendarConnectionServiceTest {
  @Mock
  lateinit var calendarConnectionRepository: CalendarConnectionRepository

  @Mock
  lateinit var externalCalendarRepository: ExternalCalendarRepository

  @Mock
  lateinit var busyIntervalRepository: BusyIntervalRepository

  @Mock
  lateinit var userRepository: UserRepository

  @Mock
  lateinit var credentialEncryptionService: CredentialEncryptionService

  @Mock
  lateinit var calendarSyncService: CalendarSyncService

  @InjectMocks
  lateinit var service: CalendarConnectionService

  private lateinit var testUser: User
  private lateinit var testConnection: CalendarConnection
  private lateinit var testCalendar: ExternalCalendar
  private val testConnectionPublicId = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val testCalendarPublicId = UUID.fromString("22222222-2222-2222-2222-222222222222")

  @BeforeEach
  fun setup() {
    testUser =
      User().apply {
        username = "test@icloud.com"
        passwordHash = "hashed"
      }
    ReflectionTestUtils.setField(testUser, "id", 1L)
    testConnection =
      CalendarConnection(
        user = testUser,
        provider = ProviderType.APPLE,
        accountIdentifier = "test@icloud.com",
        encryptedCredential = "enc-cred",
        publicId = testConnectionPublicId,
      ).also { it.id = 42L }
    testCalendar =
      ExternalCalendar(
        testConnection,
        "/cal/home/",
        "Home",
        selected = true,
        publicId = testCalendarPublicId,
      ).also { it.id = 7L }
  }

  @Test
  fun `connectAppleCalendar creates connection with encrypted credential`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(
      calendarConnectionRepository.findByUserAndProviderAndAccountIdentifier(any(), any(), any()),
    ).willReturn(null)
    given(credentialEncryptionService.encrypt("app-password")).willReturn("enc-cred")
    given(calendarConnectionRepository.save(any())).willReturn(testConnection)
    willThrow(RuntimeException("sync not available")).given(calendarSyncService).syncConnection(any())

    val result = service.connectAppleCalendar(1L, "test@icloud.com", "app-password")

    assertEquals(ProviderType.APPLE, result.provider)
    assertEquals("test@icloud.com", result.accountIdentifier)
    then(credentialEncryptionService).should().encrypt("app-password")
    then(calendarConnectionRepository).should().save(any())
  }

  @Test
  fun `connectAppleCalendar throws IllegalStateException if connection already exists`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(
      calendarConnectionRepository.findByUserAndProviderAndAccountIdentifier(any(), any(), any()),
    ).willReturn(testConnection)

    assertThrows(IllegalStateException::class.java) {
      service.connectAppleCalendar(1L, "test@icloud.com", "password")
    }
    then(calendarConnectionRepository).shouldHaveNoMoreInteractions()
  }

  @Test
  fun `connectAppleCalendar throws NoSuchElementException when user not found`() {
    given(userRepository.findById(99L)).willReturn(Optional.empty())

    assertThrows(NoSuchElementException::class.java) {
      service.connectAppleCalendar(99L, "nobody@icloud.com", "password")
    }
  }

  @Test
  fun `listConnections returns connections for user`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(calendarConnectionRepository.findAllByUser(testUser)).willReturn(listOf(testConnection))

    val result = service.listConnections(1L)

    assertEquals(1, result.size)
    assertEquals(42L, result.first().id)
  }

  @Test
  fun `disconnect deletes calendars, busy intervals and clears credentials`() {
    val calendar = ExternalCalendar(testConnection, "/cal/home", "Home")
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(calendarConnectionRepository.findByPublicId(testConnectionPublicId)).willReturn(testConnection)
    given(externalCalendarRepository.findAllByConnection(testConnection)).willReturn(listOf(calendar))
    willDoNothing().given(busyIntervalRepository).deleteAllByExternalCalendar(calendar)
    willDoNothing().given(externalCalendarRepository).deleteAllByConnection(testConnection)

    service.disconnect(1L, testConnectionPublicId)

    then(busyIntervalRepository).should().deleteAllByExternalCalendar(calendar)
    then(externalCalendarRepository).should().deleteAllByConnection(testConnection)
    then(calendarConnectionRepository).should().delete(testConnection)
    assertEquals("", testConnection.encryptedCredential)
    assertEquals(ConnectionStatus.DISCONNECTED, testConnection.status)
  }

  @Test
  fun `disconnect throws NoSuchElementException when connection not found`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    val missingConnectionId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    given(calendarConnectionRepository.findByPublicId(missingConnectionId)).willReturn(null)

    assertThrows(NoSuchElementException::class.java) {
      service.disconnect(1L, missingConnectionId)
    }
  }

  @Test
  fun `disconnect throws AccessDeniedException when connection belongs to another user`() {
    val otherUser =
      User().apply {
        username = "other@icloud.com"
        passwordHash = "hashed"
      }
    ReflectionTestUtils.setField(otherUser, "id", 2L)
    given(userRepository.findById(2L)).willReturn(Optional.of(otherUser))
    given(calendarConnectionRepository.findByPublicId(testConnectionPublicId)).willReturn(testConnection)

    assertThrows(AccessDeniedException::class.java) {
      service.disconnect(2L, testConnectionPublicId)
    }
  }

  @Test
  fun `updateCalendarSelection sets selected flag`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(calendarConnectionRepository.findByPublicId(testConnectionPublicId)).willReturn(testConnection)
    given(externalCalendarRepository.findByPublicId(testCalendarPublicId)).willReturn(testCalendar)
    given(externalCalendarRepository.save(testCalendar)).willReturn(testCalendar)

    val result = service.updateCalendarSelection(1L, testConnectionPublicId, testCalendarPublicId, true)

    assertEquals(true, result.selected)
    then(externalCalendarRepository).should().save(testCalendar)
  }

  @Test
  fun `updateCalendarSelection removes busy intervals when calendar is deselected`() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser))
    given(calendarConnectionRepository.findByPublicId(testConnectionPublicId)).willReturn(testConnection)
    given(externalCalendarRepository.findByPublicId(testCalendarPublicId)).willReturn(testCalendar)
    given(externalCalendarRepository.save(testCalendar)).willReturn(testCalendar)

    val result = service.updateCalendarSelection(1L, testConnectionPublicId, testCalendarPublicId, false)

    assertEquals(false, result.selected)
    then(busyIntervalRepository).should().deleteAllByExternalCalendar(testCalendar)
  }
}
