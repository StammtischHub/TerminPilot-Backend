package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@DataJpaTest
class UserGroupRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userGroupRepository: UserGroupRepository

  @Test
  fun `should automatically generate an id when saving an UserGroup`() {
    val userGroup =
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = "name"
        },
      )

    assertNotEquals(null, userGroup.id)
  }

  @Test
  fun `should throw an exception when creating an UserGroup with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(UserGroup())
    }
  }

  @Test
  fun `should throw an exception when creating an UserGroup with an blank name`() {
    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = ""
        },
      )
    }
  }
}
