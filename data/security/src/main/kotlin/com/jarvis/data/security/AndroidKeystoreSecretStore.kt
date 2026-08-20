package com.jarvis.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts each secret with AES-256-GCM using a key that never leaves the Android Keystore
 * (section 3: "رمزگذاری API key با کلید AES-GCM در Android Keystore"). The encrypted blob
 * (IV + ciphertext, base64) is the only thing persisted, in a dedicated SharedPreferences file
 * that holds nothing else — plaintext secrets and Authorization headers are never logged.
 */
class AndroidKeystoreSecretStore(context: Context) : SecretStore {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("jarvis_secrets", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private companion object {
        const val KEYSTORE_ALIAS = "jarvis_secret_store_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    override suspend fun get(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return try {
            val decoded = Base64.decode(stored, Base64.NO_WRAP)
            val iv = decoded.copyOfRange(0, 12)
            val ciphertext = decoded.copyOfRange(12, decoded.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null // corrupted or key invalidated (e.g. device credentials changed) — treat as absent
        }
    }

    override suspend fun put(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + ciphertext
        prefs.edit().putString(key, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
