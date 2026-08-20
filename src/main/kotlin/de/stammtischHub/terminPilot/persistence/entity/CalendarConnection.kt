package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity(name = "CalendarConnection")
@Table(name = "calendarConnections")
class CalendarConnection(
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  var user: User,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var provider: ProviderType,
  @Column(nullable = false)
  var accountIdentifier: String,
  @Column(nullable = false, columnDefinition = "TEXT")
  var encryptedCredential: String,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  var status: ConnectionStatus = ConnectionStatus.PENDING,
  var lastSyncAt: OffsetDateTime? = null,
  @Column(length = 100)
  var lastErrorCode: String? = null,
  @Column(columnDefinition = "TEXT")
  var lastErrorMessage: String? = null,
  @Column(nullable = false, updatable = false)
  var createdAt: OffsetDateTime = OffsetDateTime.now(),
  @Column(nullable = false)
  var updatedAt: OffsetDateTime = OffsetDateTime.now(),
  @Column(name = "public_id", nullable = false, updatable = false, unique = true)
  var publicId: UUID = UUID.randomUUID(),
) {
  @Id
  @Column(name = "connection_id", nullable = false, updatable = false, unique = true)
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null
}
