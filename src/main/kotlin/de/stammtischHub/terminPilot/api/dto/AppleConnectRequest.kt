package de.stammtischHub.terminPilot.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class AppleConnectRequest(
  @field:Email
  @field:NotBlank
  val email: String,
  @field:NotBlank
  val appSpecificPassword: String,
)
