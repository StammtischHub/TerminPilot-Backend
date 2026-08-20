package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity(name = "ExternalCalendar")
@Table(name = "externalCalendars")
class ExternalCalendar(
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "connection_id", nullable = false)
  var connection: CalendarConnection,
  @Column(nullable = false, columnDefinition = "TEXT")
  var externalHref: String,
  @Column(nullable = false)
  var displayName: String,
  @Column(nullable = false)
  var selected: Boolean = false,
  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  var publicId: UUID = UUID.randomUUID(),
) {
  @Id
  @Column(name = "external_calendar_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null
}
