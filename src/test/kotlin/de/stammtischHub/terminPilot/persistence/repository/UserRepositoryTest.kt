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

@DataJpaTest
class UserRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should find user by username`() {
    val user = userRepository.save(User().apply {
      username = "username"
      password = "password"
      userType = UserType.USER
    })
    entityManager.flush()
    entityManager.clear()

    val foundUser = userRepository.findByUsername("username")
    assertEquals(user, foundUser)
  }

  @Test
  fun `should throw exception when creating a user with blank fields`() {
    assertFailsWith<ConstraintViolationException> {
      userRepository.save(User().apply {
        username = ""
        password = ""
        userType = UserType.USER
      })
      entityManager.flush()
    }
  }

  @Test
  fun `should throw exception when creating a user with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      userRepository.save(User())
      entityManager.flush()
    }
  }
}
