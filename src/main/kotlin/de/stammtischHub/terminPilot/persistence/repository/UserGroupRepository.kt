package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import de.stammtischHub.terminPilot.persistence.entity.UserGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserGroupRepository : JpaRepository<UserGroup, Long> {
  fun findByName(name: String): Optional<UserGroup>
  fun findByCreatorId(creatorId: Long): Optional<Iterable<UserGroup>>
}
