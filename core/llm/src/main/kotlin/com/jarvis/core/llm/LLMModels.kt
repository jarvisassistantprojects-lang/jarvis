package com.jarvis.core.llm

/** Identifies which concrete provider actually produced a response — this is logged verbatim
 *  (section 6: "ثبت provider واقعی استفاده‌شده" / "no silent fallback") so the UI and event
 *  log never claim LOCAL ran when REMOTE actually served the request, or vice versa. */
enum class LLMProviderId { LOCAL, REMOTE }

/** A candidate the caller has already resolved locally (e.g. an installed, launchable app)
 *  that the model is allowed to choose between. Keeping this generic (not open_app-specific)
 *  lets future action types reuse the same anti-hallucination pattern from section 13. */
data class LLMCandidate(val id: String, val label: String)

data class LLMRequest(
    val transcript: String,
    val candidates: List<LLMCandidate> = emptyList(),
    val systemPrompt: String,
    val timeoutMillis: Long
)

/** [rawJson] is the full, still-unvalidated JSON text the provider returned. It is never
 *  trusted directly — the actions module always independently parses and validates it
 *  (section 10: "ادعای JSON-only مدل به‌تنهایی کنترل امنیتی نیست"). */
data class LLMResponse(
    val providerId: LLMProviderId,
    val rawJson: String,
    val latencyMillis: Long
)

sealed class LLMAvailability {
    data object Available : LLMAvailability()
    data class Unavailable(val reason: String) : LLMAvailability()
}
