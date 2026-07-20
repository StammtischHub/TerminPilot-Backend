package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull

@Entity(name = "Calendar")
@Table(name = "calendars")
@Inheritance(strategy = InheritanceType.JOINED)
@AttributeOverride(name = "id", column = Column(name = "calendar_id"))
class Calendar : BaseLongId() {
  @ManyToOne
  @JoinColumn(name = "user_id")
  @NotNull
  var owner: User? = null
}
