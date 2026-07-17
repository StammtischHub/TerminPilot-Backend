package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.Calendar
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import jakarta.persistence.EntityManager
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
class CalendarRepositoryTest {

  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var calendarRepository: CalendarRepository

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should find calendar by user id`() {
    val savedUser = userRepository.save(User().apply {
      username = "username"
      password = "password"
      userType = UserType.USER
    })
    val calendar = calendarRepository.save(Calendar().apply {
      user = savedUser
    })
    entityManager.flush()
    entityManager.clear()

    val foundCalendar = calendarRepository.findById(savedUser.id!!).get()
    assertEquals(calendar, foundCalendar)
    assertEquals(savedUser, foundCalendar.user)
  }

  @Test
  fun `should throw exception when creating a calendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      calendarRepository.save(Calendar())
      entityManager.flush()
    }
  }
}
