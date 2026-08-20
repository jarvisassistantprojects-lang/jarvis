package com.jarvis.core.llm

/** User-facing provider selection. AUTO's fallback-to-remote behavior additionally requires
 *  [allowAutoRemoteFallback] to be true — the router must never fall back silently
 *  (technical review section 6). */
enum class LLMMode { AUTO, LOCAL, REMOTE }
