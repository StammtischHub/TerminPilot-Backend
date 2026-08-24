package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity(name = "User")
@Table(name = "users")
@AttributeOverride(name = "id", column = Column(name = "user_id"))
class User : BaseLongId() {
  @Column(unique = true)
  @Size(min = 3, max = 30)
  @NotBlank
  var username: String = ""

  @NotBlank
  var password: String = ""

  @Enumerated(EnumType.STRING)
  @NotNull
  var userType: UserType = UserType.USER

  @OneToMany(mappedBy = "creator", cascade = [CascadeType.ALL], orphanRemoval = true)
  var userGroups: MutableSet<UserGroup> = mutableSetOf()

  @OneToMany(mappedBy = "owner", cascade = [CascadeType.ALL], orphanRemoval = true)
  var calendars: MutableSet<Calendar> = mutableSetOf()
}
