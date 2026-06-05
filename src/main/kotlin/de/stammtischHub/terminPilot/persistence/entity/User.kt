package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class User {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null
}
