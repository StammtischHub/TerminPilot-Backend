package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity(name = "Calendar")
@Table(name = "calendars")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Calendar(
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  var user: User,
) {
  @Id
  @Column(name = "calendar_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    } else if (other !is Calendar) {
      return false
    }
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
