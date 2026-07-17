package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class BaseLongId {
  @Column(nullable = false, updatable = false, unique = true)
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  val id: Long? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other !is BaseLongId) return false

    return id != null && id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
