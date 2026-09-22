package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Entity(name = "user_group")
@Table(
  name = "user_groups",
  uniqueConstraints = [
    UniqueConstraint(name = "unique_name_per_creator", columnNames = ["name", "creator"])
  ]
)
@AttributeOverride(name = "_id", column = Column(name = "user_group_id"))
class UserGroup : BaseLongId() {
  @NotBlank
  lateinit var name: String

  @ManyToOne
  @JoinColumn(name = "creator")
  @NotNull
  var creator: User? = null

  @ManyToMany
  @JoinTable(
    name = "members",
    joinColumns = [JoinColumn(name = "user_group")],
    inverseJoinColumns = [JoinColumn(name = "member")],
  )
  @NotEmpty
  var members: MutableSet<User> = mutableSetOf()
}
