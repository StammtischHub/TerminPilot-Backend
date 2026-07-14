package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertEquals

@DataJpaTest
class UserRepositoryTest {

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userRepository: UserRepository

  @Test
  fun `should save user`() {
    val user = User(username = "test", password = "password")
    entityManager.persist(user)
    entityManager.flush()

    val foundUser = userRepository.findById(user.id!!).get()
    assertEquals(user, foundUser)
  }

}
