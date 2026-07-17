package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.GoogleCalendar
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@DataJpaTest
class GoogleCalendarRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var googleCalendarRepository: GoogleCalendarRepository

  @Autowired
  lateinit var userRepository: UserRepository

  lateinit var user: User

  fun setupUser() {
    this.user =
      userRepository.saveAndFlush(
        User().apply {
          username = "username"
          passwordHash = "password"
          userType = UserType.USER
        },
      )
    entityManager.clear()
  }

  @Test
  fun `should automatically generate an id when saving a GoogleCalendar`() {
    setupUser()

    val googleCalendar =
      googleCalendarRepository.saveAndFlush(
        GoogleCalendar().apply {
          owner = user
          calendarName = "calendarName"
          accessToken = "accessToken"
          refreshToken = "refreshToken"
          tokenExpiry = 5L
        },
      )

    assertNotEquals(null, googleCalendar.id)
  }

  @Test
  fun `should throw an exception when creating a GoogleCalendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      googleCalendarRepository.saveAndFlush(GoogleCalendar())
    }
  }

  @Test
  fun `should throw an exception when creating a GoogleCalendar with blank fields`() {
    setupUser()

    assertFailsWith<ConstraintViolationException> {
      googleCalendarRepository.saveAndFlush(
        GoogleCalendar().apply {
          owner = user
          calendarName = ""
          accessToken = ""
          refreshToken = ""
          tokenExpiry = 5L
        },
      )
    }
  }
}
