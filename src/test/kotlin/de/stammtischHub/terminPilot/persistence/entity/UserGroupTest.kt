package de.stammtischHub.terminPilot.persistence.entity

import kotlin.test.Test

class UserGroupTest {
  @Test
  fun `should add user to group`() {
    val userGroup = UserGroup()
    val user = User()

    userGroup.addMember(user)

    assert(userGroup.members.contains(user))
    assert(user.userGroups.contains(userGroup))
  }

  @Test
  fun `should remove user to group`() {
    val userGroup = UserGroup()
    val user = User()

    userGroup.addMember(user)

    assert(userGroup.members.isNotEmpty())
    assert(user.userGroups.isNotEmpty())

    userGroup.removeMember(user)

    assert(userGroup.members.isEmpty())
    assert(user.userGroups.isEmpty())
  }
}
