package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

@DataJpaTest
class UserGroupRepositoryTest {

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userGroupRepository: UserGroupRepository

  @Test
  fun `should throw exception when creating a user group with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.save(UserGroup())
      entityManager.flush()
    }
  }

  @Test
  fun `should throw exception when creating a user group with an blank name`() {
    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.save(UserGroup().apply {
        name = ""
      })
      entityManager.flush()
    }
  }
}
