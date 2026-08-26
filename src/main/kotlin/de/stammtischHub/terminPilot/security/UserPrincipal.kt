package de.stammtischHub.terminPilot.security

import de.stammtischHub.terminPilot.persistence.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable

class UserPrincipal(
  val user: User,
) : UserDetails,
  Serializable {
  val id: Long
    get() = user.id

  override fun getUsername(): String = user.username

  override fun getPassword(): String = user.password

  override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(user.userType.toString()))

  override fun isAccountNonExpired() = true

  override fun isAccountNonLocked() = true

  override fun isCredentialsNonExpired() = true

  override fun isEnabled() = true
}
