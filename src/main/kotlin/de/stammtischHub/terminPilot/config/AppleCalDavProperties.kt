package de.stammtischHub.terminPilot.config

import java.time.Duration

data class AppleCalDavProperties(
  val baseUrl: String = "https://caldav.icloud.com",
  val connectTimeout: Duration = Duration.ofSeconds(10),
  val readTimeout: Duration = Duration.ofSeconds(30),
)
