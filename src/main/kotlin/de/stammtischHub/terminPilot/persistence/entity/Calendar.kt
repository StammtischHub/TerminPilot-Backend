package de.stammtischHub.terminPilot.persistence.entity

import de.stammtischHub.terminPilot.persistence.entity.constraints.ExactlyOneNotNull
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity(name = "calendar")
@Table(name = "calendars")
@Inheritance(strategy = InheritanceType.JOINED)
@ExactlyOneNotNull("googleAccess", "calDavAccess")
@AttributeOverride(name = "_id", column = Column(name = "calendar_id"))
class Calendar : BaseLongId() {
  @ManyToOne
  @JoinColumn(name = "owner")
  @NotNull
  lateinit var owner: User

  @NotBlank
  lateinit var externalCalendarName: String

  @ManyToOne
  @JoinColumn(name = "google_access")
  var googleAccess: GoogleAccess? = null

  @ManyToOne
  @JoinColumn(name = "cal_dav_access")
  var calDavAccess: CalDavAccess? = null
}
