package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
class UserRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

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
  fun `should automatically generate an id when saving an User`() {
    setupUser()

    val id = assertDoesNotThrow { user.id }
    assertThat(id).isPositive()
  }

  @Test
  fun `should not be able to create two users with the same username`() {
    setupUser()

    assertFailsWith<DataIntegrityViolationException> {
      userRepository.saveAndFlush(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )
    }
  }

  @Test
  fun `should find User by username`() {
    setupUser()

    val foundUser = userRepository.findByUsername("username").get()
    assertEquals(this.user, foundUser)
  }

  @Test
  fun `should throw an exception when creating an User with blank fields`() {
    assertFailsWith<ConstraintViolationException> {
      userRepository.saveAndFlush(
        User().apply {
          username = ""
          password = ""
          userType = UserType.USER
        },
      )
    }
  }

  @Test
  fun `should throw an exception when creating an User with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      userRepository.saveAndFlush(User())
    }
  }
}
