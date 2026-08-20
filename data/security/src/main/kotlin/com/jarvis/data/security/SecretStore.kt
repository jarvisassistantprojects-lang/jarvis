package com.jarvis.data.security

/** Encrypted-at-rest storage for secrets: remote API key, Porcupine AccessKey (section 3/9).
 *  Never backed by plain SharedPreferences. Values are never logged by any caller. */
interface SecretStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
    suspend fun remove(key: String)

    companion object {
        const val KEY_REMOTE_API_KEY = "remote_api_key"
        const val KEY_PORCUPINE_ACCESS_KEY = "porcupine_access_key"
    }
}
