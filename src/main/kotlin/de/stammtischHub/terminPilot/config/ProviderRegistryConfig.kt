package de.stammtischHub.terminPilot.config

import de.stammtischHub.terminPilot.provider.CalendarProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProviderRegistryConfig {

    @Bean
    fun providerRegistry(providers: List<CalendarProvider>): Map<String, CalendarProvider> =
        providers.associateBy { it.javaClass.simpleName }
}