package de.stammtischHub.terminPilot.persistence.repository

import de.stammtischHub.terminPilot.persistence.entity.CalDavAccess
import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

@DataJpaTest
class CalDavAccessRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var calDavAccessRepository: CalDavAccessRepository

  @Test
  fun `should automatically generate an id when saving an CalDavAccess`() {
    val calDavAccess =
      calDavAccessRepository.saveAndFlush(
        CalDavAccess().apply {
          address = "address"
          appSpecificPassword = "password"
        },
      )

    val id = assertDoesNotThrow { calDavAccess.id }
    assertThat(id).isPositive()
  }

  @Test
  fun `should throw an exception when creating an CalDavAccess with null fields`() {
    assertFailsWith<ConstraintViolationException> {
      calDavAccessRepository.saveAndFlush(CalDavAccess())
    }
  }

  @Test
  fun `should throw an exception when creating an CalDavAccess with blank fields`() {
    assertFailsWith<ConstraintViolationException> {
      calDavAccessRepository.saveAndFlush(
        CalDavAccess().apply {
          address = ""
          appSpecificPassword = ""
        },
      )
    }
  }
}
