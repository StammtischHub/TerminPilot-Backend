package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.stereotype.Repository

@Repository
interface UserRepository {
  fun findById(id: Long): List<User>
}
