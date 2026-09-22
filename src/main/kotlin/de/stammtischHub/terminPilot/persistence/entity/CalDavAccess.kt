package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity(name = "cal_dav_access")
@Table(name = "cal_dav_accesses")
@AttributeOverride(name = "_id", column = Column(name = "calendar_access_id"))
class CalDavAccess: BaseLongId() {
  @NotBlank
  lateinit var address: String

  @NotBlank
  lateinit var appSpecificPassword: String
}
