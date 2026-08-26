package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.AppleCalendar
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

@DataJpaTest
class AppleCalendarRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var appleCalendarRepository: AppleCalendarRepository

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
  fun `should automatically generate an id when saving an AppleCalendar`() {
    setupUser()

    val appleCalendar =
      appleCalendarRepository.saveAndFlush(
        AppleCalendar().apply {
          owner = user
          icloudMail = "a"
          appSpecificPassword = "b"
        },
      )

    val id = assertDoesNotThrow { appleCalendar.id }
    assertThat(id).isPositive()
  }

  @Test
  fun `should throw an exception when creating an AppleCalendar with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      appleCalendarRepository.saveAndFlush(AppleCalendar())
    }
  }

  @Test
  fun `should throw an exception when creating an AppleCalendar with blank fields`() {
    setupUser()

    assertFailsWith<ConstraintViolationException> {
      appleCalendarRepository.saveAndFlush(
        AppleCalendar().apply {
          owner = user
          icloudMail = ""
          appSpecificPassword = ""
        },
      )
    }
  }
}
