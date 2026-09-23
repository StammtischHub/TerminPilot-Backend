package de.stammtischHub.terminPilot.service

import de.stammtischHub.terminPilot.exception.UserGroupNameTakenException
import de.stammtischHub.terminPilot.model.generated.UserGroupResponse
import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import de.stammtischHub.terminPilot.persistence.repository.UserGroupRepository
import de.stammtischHub.terminPilot.service.mapping.toUserGroupResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserGroupService(
  private val userGroupRepository: UserGroupRepository,
  private val userService: UserService,
) {
  @Transactional
  fun createUserGroup(
    name: String,
    creatorId: Long,
    memberIds: List<Long>,
  ): UserGroupResponse {
    val normalizedName = name.trim()

    val creator = userService.getUserByUserId(creatorId)
    val members = memberIds.map { userService.getUserByUserId(it) }.toMutableSet()

    if (userGroupRepository.findByName(normalizedName).isPresent) {
      throw UserGroupNameTakenException()
    }

    val userGroup =
      UserGroup().apply {
        this.name = normalizedName
        this.creator = creator
        this.members = members
      }

    return try {
      val savedUserGroup = userGroupRepository.saveAndFlush(userGroup)
      savedUserGroup.toUserGroupResponse()
    } catch (_: DataIntegrityViolationException) {
      throw UserGroupNameTakenException()
    }
  }

  @Transactional
  fun updateUserGroup(
    id: Long,
    name: String?,
    memberIds: List<Long>?,
  ): UserGroupResponse {
    val userGroup = userGroupRepository.findById(id).get()
    val members =
      memberIds?.map { userService.getUserByUserId(it) }?.toMutableSet()

    name?.let { userGroup.name = it.trim() }
    members?.let { userGroup.members = it }
    return userGroupRepository.saveAndFlush(userGroup).toUserGroupResponse()
  }

  @Transactional
  fun deleteUserGroup(id: Long) {
    userGroupRepository.deleteById(id)
  }
}
