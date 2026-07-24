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
      calendarRepository.save(Calendar(this.user))
    entityManager.flush()
    entityManager.clear()

    val foundCalendars = calendarRepository.findByUserId(this.user.id!!).get()
    assertEquals(listOf(calendar), foundCalendars)
    assert(foundCalendars.all { it.user == this.user })
  }

  @Test
  fun `should find multiple Calendar entities by user id`() {
    setupUser()
    val calendar1 =
      calendarRepository.save(Calendar(this.user))
    val calendar2 =
      calendarRepository.save(Calendar(this.user))
    entityManager.flush()
    entityManager.clear()

    val foundCalendars = calendarRepository.findByUserId(this.user.id!!).get()
    assertEquals(listOf(calendar1, calendar2).sortedBy { it.id }, foundCalendars.sortedBy { it.id })
    assert(foundCalendars.all { it.user == this.user })
  }

  @Test
  fun `should throw an exception when creating a Calendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      calendarRepository.save(Calendar())
      entityManager.flush()
    }
  }
}
