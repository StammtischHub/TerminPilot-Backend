package de.stammtischHub.terminPilot.persistence.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val ALGORITHM = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val IV_LENGTH_BYTES = 12

/**
 * Encrypts/decrypts individual String fields transparently for JPA (AES-256-GCM).
 * A new IV is generated per value and prepended to the ciphertext.
 */
@Converter
@Component
class EncryptedStringConverter(
  @Value($$"${TOKEN_ENCRYPTION_KEY}") base64Key: String,
) : AttributeConverter<String, String> {
  private val keyBytes = Base64.getDecoder().decode(base64Key)

  init {
    require(keyBytes.size == 32) {
      "TOKEN_ENCRYPTION_KEY must decode to 32 bytes for AES-256, but was ${keyBytes.size}"
    }
  }

  private val secretKey = SecretKeySpec(keyBytes, "AES")

  private val random = SecureRandom()

  override fun convertToDatabaseColumn(attribute: String?): String? {
    if (attribute == null) return null
    val iv = ByteArray(IV_LENGTH_BYTES).also(random::nextBytes)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val ciphertext = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(iv + ciphertext)
  }

  override fun convertToEntityAttribute(dbData: String?): String? {
    if (dbData == null) return null
    val decoded = Base64.getDecoder().decode(dbData)
    val iv = decoded.copyOfRange(0, IV_LENGTH_BYTES)
    val ciphertext = decoded.copyOfRange(IV_LENGTH_BYTES, decoded.size)
    val cipher = Cipher.getInstance(ALGORITHM)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }
}
