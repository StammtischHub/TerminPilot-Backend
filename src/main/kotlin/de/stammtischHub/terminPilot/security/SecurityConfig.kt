package de.stammtischHub.terminPilot.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

@Configuration
@EnableWebSecurity
class SecurityConfig {
  // Todo: We temporarily need to use the Java API for the filter chain. See https://github.com/spring-projects/spring-security/issues/18332
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .authorizeHttpRequests { auth ->
        auth
          .requestMatchers(
            "/api/auth/login",
            "/api/auth/register",
            "/api/google/oauth/callback", // TODO: remove
          ).permitAll()
          .anyRequest()
          .authenticated()
      }.csrf { csrf ->
        csrf.spa()
        csrf.ignoringRequestMatchers("/api/auth/**", "/api/google/oauth/callback") // TODO: remove
      }.exceptionHandling { ex ->
        ex.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
      }
    return http.build()
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

  @Bean
  fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

  @Bean
  fun securityContextRepository(): SecurityContextRepository = HttpSessionSecurityContextRepository()
}
