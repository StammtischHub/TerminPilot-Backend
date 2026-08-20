package de.stammtischHub.terminPilot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class CredentialEncryptionService(
  @Value("\${terminpilot.encryption.key}") base64Key: String,
) {
  private val secretKey: SecretKey = SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES")

  fun encrypt(plaintext: String): String {
    val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(iv + ciphertext)
  }

  fun decrypt(encoded: String): String {
    val data = Base64.getDecoder().decode(encoded)
    check(data.size > GCM_IV_LENGTH) { "Encoded credential is too short to be valid" }
    val iv = data.sliceArray(0 until GCM_IV_LENGTH)
    val ciphertext = data.sliceArray(GCM_IV_LENGTH until data.size)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
  }

  private companion object {
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 128
  }
}
