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
      userRepository.saveAndFlush(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )
    entityManager.clear()
  }

  @Test
  fun `should automatically generate an id when saving a Calendar`() {
    setupUser()

    val calendar =
      calendarRepository.saveAndFlush(
        Calendar().apply {
          owner = user
        },
      )

    assertNotEquals(null, calendar.id)
  }

  @Test
  fun `should find Calendar by user id`() {
    setupUser()

    val calendar =
      calendarRepository.saveAndFlush(
        Calendar().apply {
          owner = user
        },
      )
    entityManager.clear()

    val foundCalendars = calendarRepository.findByOwnerId(this.user.id!!).get()
    assertEquals(listOf(calendar), foundCalendars)
    assert(foundCalendars.all { it.owner == this.user })
  }

  @Test
  fun `should find multiple Calendar entities by user id`() {
    setupUser()

    val calendar1 =
      calendarRepository.saveAndFlush(
        Calendar().apply {
          owner = user
        },
      )
    val calendar2 =
      calendarRepository.saveAndFlush(
        Calendar().apply {
          owner = user
        },
      )
    entityManager.clear()

    val foundCalendars = calendarRepository.findByOwnerId(this.user.id!!).get()
    assertEquals(listOf(calendar1, calendar2).sortedBy { it.id }, foundCalendars.sortedBy { it.id })
    assert(foundCalendars.all { it.owner == this.user })
  }

  @Test
  fun `should throw an exception when creating a Calendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      calendarRepository.saveAndFlush(Calendar())
    }
  }
}
