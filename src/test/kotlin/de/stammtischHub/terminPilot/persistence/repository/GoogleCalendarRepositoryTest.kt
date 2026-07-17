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

@DataJpaTest
class GoogleCalendarRepositoryTest {

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var googleCalendarRepository: GoogleCalendarRepository

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should throw exception when creating a calendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      googleCalendarRepository.save(GoogleCalendar())
      entityManager.flush()
    }
  }

  @Test
  fun `should throw exception when creating a calendar with blank fields`() {
    val user = userRepository.save(User().apply {
      username = "username"
      password = "password"
      userType = UserType.USER
    })

    assertFailsWith<ConstraintViolationException> {
      googleCalendarRepository.save(GoogleCalendar(user).apply {
        calendarName = ""
        accessToken = ""
        refreshToken = ""
        tokenExpiry = 5L
      })
      entityManager.flush()
    }
  }
}
