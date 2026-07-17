package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity(name = "UserGroup")
@Table(name = "userGroups")
@AttributeOverride(name = "id", column = Column(name = "user_group_id"))
class UserGroup : BaseLongId() {
  @NotNull
  @NotBlank
  var name: String? = null

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "userGroupMembers",
    joinColumns = [JoinColumn(name = "user_group_id")],
    inverseJoinColumns = [JoinColumn(name = "user_id")],
  )
  var members: MutableSet<User> = mutableSetOf()

  fun addMember(user: User) {
    members.add(user)
    user.userGroups.add(this)
  }

  fun removeMember(user: User) {
    members.remove(user)
    user.userGroups.remove(this)
  }
}
