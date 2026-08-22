package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import jakarta.validation.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@DataJpaTest
class UserGroupRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var userGroupRepository: UserGroupRepository

  @Autowired
  lateinit var userRepository: UserRepository

  lateinit var creatorUser: User
  lateinit var memberUser: User

  fun setupUsers() {
    this.creatorUser =
      userRepository.saveAndFlush(
        User().apply {
          username = "creator"
          password = "password"
        },
      )

    this.memberUser =
      userRepository.saveAndFlush(
        User().apply {
          username = "member"
          password = "password"
        },
      )

    entityManager.clear()
  }

  @Test
  fun `should automatically generate an id when saving an UserGroup`() {
    setupUsers()

    val userGroup =
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = "name"
          creator = creatorUser
          members = mutableSetOf(memberUser)
        },
      )

    assertNotEquals(null, userGroup.id)
  }

  @Test
  fun `should find UserGroup by name`() {
    setupUsers()

    val userGroup =
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = "name"
          creator = creatorUser
          members = mutableSetOf(memberUser)
        },
      )

    val foundUserGroup = userGroupRepository.findByName("name").get()
    assertEquals(userGroup, foundUserGroup)
  }

  @Test
  fun `should find UserGroups by creator`() {
    setupUsers()

    val userGroups =
      mutableSetOf(
        userGroupRepository.saveAndFlush(
          UserGroup().apply {
            name = "name"
            creator = creatorUser
            members = mutableSetOf(memberUser)
          },
        ),
        userGroupRepository.saveAndFlush(
          UserGroup().apply {
            name = "name2"
            creator = creatorUser
            members = mutableSetOf(memberUser)
          },
        ),
      )

    entityManager.clear()

    val foundUserGroups = userGroupRepository.findByCreatorId(creatorUser.id!!).get()
    assertContentEquals(userGroups, foundUserGroups)
  }

  @Test
  fun `should throw an exception when creating an UserGroup with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(UserGroup())
    }
  }

  @Test
  fun `should throw an exception when creating an UserGroup with an blank name`() {
    setupUsers()

    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = ""
          creator = creatorUser
          members = mutableSetOf(memberUser)
        },
      )
    }
  }

  @Test
  fun `should throw an exception when creating an UserGroup with an empty members set`() {
    setupUsers()

    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = "name"
          creator = creatorUser
          members = mutableSetOf()
        },
      )
    }
  }

  @Test
  fun `should throw an exception when creating an UserGroup with no creator`() {
    setupUsers()

    assertFailsWith<ConstraintViolationException> {
      userGroupRepository.saveAndFlush(
        UserGroup().apply {
          name = "name"
          members = mutableSetOf(memberUser)
        },
      )
    }
  }
}
