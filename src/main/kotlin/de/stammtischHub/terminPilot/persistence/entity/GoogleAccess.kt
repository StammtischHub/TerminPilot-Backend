package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

@Entity(name = "google_access")
@Table(name = "google_accesses")
@AttributeOverride(name = "_id", column = Column(name = "calendar_access_id"))
class GoogleAccess : BaseLongId() {
  @Column(columnDefinition = "TEXT")
  @NotBlank
  lateinit var accessToken: String

  @Column(columnDefinition = "TEXT")
  @NotBlank
  lateinit var refreshToken: String

  @Column(name = "token_expiry")
  @PositiveOrZero
  @NotNull
  private var _tokenExpiry: Long? = null

  var tokenExpiry: Long
    get() = _tokenExpiry ?: error("tokenExpiry wurde noch nicht initialisiert")
    set(value) {
      _tokenExpiry = value
    }
}
