package de.stammtischHub.terminPilot.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class User {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  var id: Long? = null

  /** Short-lived access token for Google API calls. */
  @Column(columnDefinition = "TEXT")
  var googleAccessToken: String? = null

  /** Long-lived refresh token used to obtain new access tokens. */
  @Column(columnDefinition = "TEXT")
  var googleRefreshToken: String? = null

  /** Unix timestamp (ms) at which the access token expires. */
  var googleTokenExpiry: Long? = null

  /** The Google Calendar ID to use for this user. Defaults to the primary calendar. */
  var googleCalendarId: String = "primary"
}
