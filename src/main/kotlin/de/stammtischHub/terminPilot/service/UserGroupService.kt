package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.exception.UserGroupNameTakenException
import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import de.stammtischHub.terminPilot.persistence.repository.UserGroupRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserGroupService(
  private val userGroupRepository: UserGroupRepository,
) {
  @Transactional
  fun createUserGroup(
    name: String,
    members: MutableSet<User>,
  ): UserGroup {
    val normalizedName = name.trim()

    if (userGroupRepository.findByName(normalizedName).isPresent) {
      throw UserGroupNameTakenException()
    }

    val userGroup =
      UserGroup().apply {
        this.name = normalizedName
        this.members = members
      }

    return try {
      userGroupRepository.saveAndFlush(userGroup)
    } catch (_: DataIntegrityViolationException) {
      throw UserGroupNameTakenException()
    }
  }

  @Transactional
  fun updateUserGroup(
    id: Long,
    name: String?,
    users: MutableSet<User>?,
  ): UserGroup {
    val userGroup = userGroupRepository.findById(id).get()
    name?.let { userGroup.name = it.trim() }
    users?.let { userGroup.members = it }
    return userGroupRepository.saveAndFlush(userGroup)
  }

  fun deleteUserGroup(id: Long) {
    userGroupRepository.deleteById(id)
  }
}
