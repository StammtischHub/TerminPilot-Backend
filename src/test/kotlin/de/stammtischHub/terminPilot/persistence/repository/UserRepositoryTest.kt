package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.boot.jpa.test.autoconfigure.find
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
    val user = userRepository.save(User(username = "test", password = "password"))
    entityManager.flush()
    entityManager.clear()

    val foundUser = entityManager.find<User>(user.id!!)!!

    assertEquals(user, foundUser)
  }

  @Test
  fun `should update existing user`() {
    val user = entityManager.persist(User(username = "test", password = "password"))
    entityManager.flush()
    entityManager.clear()

    val persistedUser = entityManager.find<User>(user.id!!)!!
    persistedUser.password = "newPassword"
    userRepository.save(persistedUser)
    entityManager.flush()
    entityManager.clear()

    val updatedUser = entityManager.find<User>(user.id!!)!!
    assertEquals("newPassword", updatedUser.password)
  }

  @Test
  fun `should find user by id`() {
    val user = entityManager.persist(User(username = "test", password = "password"))
    entityManager.flush()
    entityManager.clear()

    val foundUser = userRepository.findById(user.id!!).get()
    assertEquals(user, foundUser)
  }
}
