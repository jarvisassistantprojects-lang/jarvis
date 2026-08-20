package com.jarvis.providers.remotellm

data class RemoteLLMConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val timeoutMillis: Long = 15_000,
    val streamingEnabled: Boolean = false
) {
    /** Section 10: HTTPS by default, and no redirect to a different host. Reject anything
     *  that isn't https unless it's explicitly localhost (useful for local dev servers). */
    fun isSchemeAllowed(): Boolean =
        baseUrl.startsWith("https://") || baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1")
}
