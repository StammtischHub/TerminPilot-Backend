package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "User")
@Table(name = "users")
class User(
  @Column(nullable = false, unique = true)
  var username: String,
  @Column(nullable = false)
  var password: String,
  @ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
  var userGroups: MutableSet<UserGroup> = mutableSetOf(),
  @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var calendars: MutableSet<Calendar> = mutableSetOf(),
) {
  @Id
  @Column(name = "user_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    else if (other !is User) return false
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
