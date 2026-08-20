package de.stammtischHub.terminPilot.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import kotlin.test.Test

class CredentialEncryptionServiceTest {
  private val testKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
  private val service = CredentialEncryptionService(testKey)

  @Test
  fun `encrypt and decrypt roundtrip returns original plaintext`() {
    val plaintext = "super-secret-app-password-123"
    val encrypted = service.encrypt(plaintext)
    val decrypted = service.decrypt(encrypted)
    assertEquals(plaintext, decrypted)
  }

  @Test
  fun `encrypted value differs from plaintext`() {
    val plaintext = "my-apple-app-password"
    val encrypted = service.encrypt(plaintext)
    assertNotEquals(plaintext, encrypted)
  }

  @Test
  fun `two encryptions of the same plaintext produce different ciphertexts due to random IV`() {
    val plaintext = "same-password"
    val encrypted1 = service.encrypt(plaintext)
    val encrypted2 = service.encrypt(plaintext)
    assertNotEquals(encrypted1, encrypted2)
  }

  @Test
  fun `decrypt throws on tampered ciphertext`() {
    val encrypted = service.encrypt("some-password")
    val tampered = encrypted.dropLast(1) + if (encrypted.last() == 'A') "B" else "A"
    assertThrows(Exception::class.java) { service.decrypt(tampered) }
  }

  @Test
  fun `decrypt throws on input shorter than IV`() {
    assertThrows(IllegalStateException::class.java) {
      service.decrypt("c2hvcnQ=")
    }
  }

  @Test
  fun `encrypt handles empty string`() {
    val encrypted = service.encrypt("")
    val decrypted = service.decrypt(encrypted)
    assertEquals("", decrypted)
  }

  @Test
  fun `encrypt handles unicode characters`() {
    val plaintext = "Passwort-äöü-🔐"
    val encrypted = service.encrypt(plaintext)
    val decrypted = service.decrypt(encrypted)
    assertEquals(plaintext, decrypted)
  }
}
