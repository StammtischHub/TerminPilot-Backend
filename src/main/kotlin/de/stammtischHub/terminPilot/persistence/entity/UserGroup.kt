package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity(name = "UserGroup")
@Table(name = "userGroups")
class UserGroup(
  @Column(nullable = false)
  var name: String,
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "userGroupMembers",
    joinColumns = [JoinColumn(name = "user_group_id")],
    inverseJoinColumns = [JoinColumn(name = "user_id")],
  )
  var members: MutableSet<User> = mutableSetOf(),
) {
  @Id
  @Column(name = "user_group_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null

  fun addMember(user: User) {
    members.add(user)
    user.userGroups.add(this)
  }

  fun removeMember(user: User) {
    members.remove(user)
    user.userGroups.remove(this)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    else if (other !is UserGroup) return false
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
