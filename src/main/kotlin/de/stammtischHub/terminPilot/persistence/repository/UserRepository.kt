package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.stereotype.Repository

@Repository
interface UserRepository {
  fun save(user: User)

  fun findById(id: Long): User?

  fun findByUsername(username: String): User?
}
