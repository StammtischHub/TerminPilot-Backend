package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import jakarta.validation.ConstraintViolationException as JakartaConstraintViolationException
import org.hibernate.exception.ConstraintViolationException as HibernateConstraintViolationException

@DataJpaTest
class UserRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

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
  fun `should automatically generate an id when saving an User`() {
    setupUser()

    assertNotEquals(null, this.user.id)
  }

  @Test
  fun `should not be able to create two users with the same username`() {
    setupUser()

    assertFailsWith<HibernateConstraintViolationException> {
      userRepository.save(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )
      entityManager.flush()
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
    assertFailsWith<JakartaConstraintViolationException> {
      userRepository.save(
        User().apply {
          username = ""
          password = ""
          userType = UserType.USER
        },
      )
      entityManager.flush()
    }
  }

  @Test
  fun `should throw an exception when creating an User with null fields`() {
    assertFailsWith<JakartaConstraintViolationException> {
      userRepository.save(User())
      entityManager.flush()
    }
  }
}
