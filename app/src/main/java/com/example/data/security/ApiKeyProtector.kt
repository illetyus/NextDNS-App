package com.example.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ApiKeyProtector {
  fun encrypt(plainText: String): String
  fun decrypt(envelope: String): String
}

class AndroidKeystoreApiKeyProtector(
  private val keyAlias: String = KEY_ALIAS
) : ApiKeyProtector {

  override fun encrypt(plainText: String): String {
    require(plainText.isNotEmpty()) { "API key cannot be empty." }

    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    cipher.updateAAD(AAD)

    val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    val payload = Base64.encodeToString(cipherText, Base64.NO_WRAP)

    return "$VERSION:$iv:$payload"
  }

  override fun decrypt(envelope: String): String {
    val parts = envelope.split(':', limit = 3)
    require(parts.size == 3 && parts[0] == VERSION) {
      "Unsupported encrypted API key format."
    }

    val iv = Base64.decode(parts[1], Base64.NO_WRAP)
    val cipherText = Base64.decode(parts[2], Base64.NO_WRAP)

    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(
      Cipher.DECRYPT_MODE,
      getOrCreateKey(),
      GCMParameterSpec(GCM_TAG_BITS, iv)
    )
    cipher.updateAAD(AAD)

    return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
  }

  @Synchronized
  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
      load(null)
    }

    (keyStore.getKey(keyAlias, null) as? SecretKey)?.let {
      return it
    }

    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      ANDROID_KEYSTORE
    )

    val spec = KeyGenParameterSpec.Builder(
      keyAlias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(KEY_SIZE_BITS)
      .build()

    keyGenerator.init(spec)
    return keyGenerator.generateKey()
  }

  companion object {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "nextdns_api_key_aes_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val VERSION = "v1"
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_BITS = 128
    private val AAD = "nextdns-api-key-v1".toByteArray(Charsets.UTF_8)
  }
}
