package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserType
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@DataJpaTest
class UserRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should automatically generate an id when saving an User`() {
    val userGroup =
      userRepository.save(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )

    assertNotEquals(null, userGroup.id)
  }

  @Test
  fun `should find User by username`() {
    val user =
      userRepository.save(
        User().apply {
          username = "username"
          password = "password"
          userType = UserType.USER
        },
      )
    entityManager.flush()
    entityManager.clear()

    val foundUser = userRepository.findByUsername("username").get()
    assertEquals(user, foundUser)
  }

  @Test
  fun `should throw an exception when creating an User with blank fields`() {
    assertFailsWith<ConstraintViolationException> {
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
    assertFailsWith<ConstraintViolationException> {
      userRepository.save(User())
      entityManager.flush()
    }
  }
}
