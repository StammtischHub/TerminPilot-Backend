package de.stammtischHub.terminPilot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
class CalDavConfiguration {
  @Bean
  @ConfigurationProperties(prefix = "terminpilot.apple-caldav")
  fun appleCalDavProperties(): AppleCalDavProperties = AppleCalDavProperties()

  @Bean
  @ConfigurationProperties(prefix = "terminpilot.calendar-sync")
  fun calendarSyncProperties(): CalendarSyncProperties = CalendarSyncProperties()
}
