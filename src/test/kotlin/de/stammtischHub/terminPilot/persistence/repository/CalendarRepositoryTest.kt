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
import kotlin.test.assertNotEquals

@DataJpaTest
class CalendarRepositoryTest {
  @Autowired
  lateinit var entityManager: EntityManager

  @Autowired
  lateinit var calendarRepository: CalendarRepository

  @Autowired
  lateinit var userRepository: UserRepository

  lateinit var user: User

  fun setupUser() {
    this.user =
      userRepository.save(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )
    entityManager.flush()
    entityManager.clear()
  }

  @Test
  fun `should automatically generate an id when saving a Calendar`() {
    setupUser()

    val calendar = calendarRepository.save(Calendar(this.user))

    assertNotEquals(null, calendar.id)
  }

  @Test
  fun `should find Calendar by user id`() {
    setupUser()
    val calendar =
      calendarRepository.save(
        Calendar().apply {
          user = this.user
        },
      )
    entityManager.flush()
    entityManager.clear()

    val foundCalendar = calendarRepository.findById(this.user.id!!).get()
    assertEquals(calendar, foundCalendar)
    assertEquals(this.user, foundCalendar.user)
  }

  @Test
  fun `should throw an exception when creating a Calendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      calendarRepository.save(Calendar())
      entityManager.flush()
    }
  }
}
