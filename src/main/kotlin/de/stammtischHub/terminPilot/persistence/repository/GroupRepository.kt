package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.UserGroup

interface GroupRepository {
  fun save(userGroup: UserGroup)

  fun findById(id: Long): UserGroup?
}
