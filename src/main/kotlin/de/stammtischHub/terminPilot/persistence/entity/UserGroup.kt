package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Entity(name = "UserGroup")
@Table(name = "userGroups")
@AttributeOverride(name = "_id", column = Column(name = "user_group_id"))
class UserGroup : BaseLongId() {
  @NotBlank
  lateinit var name: String

  @ManyToOne
  @JoinColumn(name = "creator_user_id")
  @NotNull
  var creator: User? = null

  @ManyToMany
  @JoinTable(
    name = "userGroupMembers",
    joinColumns = [JoinColumn(name = "user_group_id")],
    inverseJoinColumns = [JoinColumn(name = "member_user_id")],
  )
  @NotEmpty
  var members: MutableSet<User> = mutableSetOf()
}
