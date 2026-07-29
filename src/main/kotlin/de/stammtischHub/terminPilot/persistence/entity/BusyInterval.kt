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
import java.time.OffsetDateTime

@Entity(name = "BusyInterval")
@Table(name = "busyIntervals")
class BusyInterval(
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "connection_id", nullable = false)
  var connection: CalendarConnection,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "external_calendar_id", nullable = false)
  var externalCalendar: ExternalCalendar,
  @Column(nullable = false)
  var startTime: OffsetDateTime,
  @Column(nullable = false)
  var endTime: OffsetDateTime,
) {
  @Id
  @Column(name = "busy_interval_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null
}
