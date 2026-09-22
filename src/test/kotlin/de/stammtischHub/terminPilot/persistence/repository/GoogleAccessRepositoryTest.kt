package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.GoogleAccess
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

@DataJpaTest
class GoogleAccessRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var googleAccessRepository: GoogleAccessRepository

  @Test
  fun `should automatically generate an id when saving a GoogleAccess`() {
    val googleAccess =
      googleAccessRepository.saveAndFlush(
        GoogleAccess().apply {
          accessToken = "accessToken"
          refreshToken = "refreshToken"
          tokenExpiry = 5L
        },
      )

    val id = assertDoesNotThrow { googleAccess.id }
    assertThat(id).isPositive()
  }

  @Test
  fun `should throw an exception when creating a GoogleAccess with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      googleAccessRepository.saveAndFlush(GoogleAccess())
    }
  }

  @Test
  fun `should throw an exception when creating a GoogleAccess with blank fields`() {
    assertFailsWith<ConstraintViolationException> {
      googleAccessRepository.saveAndFlush(
        GoogleAccess().apply {
          accessToken = ""
          refreshToken = ""
          tokenExpiry = 5L
        },
      )
    }
  }
}
