package com.jarvis.core.actions.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Action Protocol v1.0, section 6 / 12: exactly one top-level action, version pinned to
 * "1.0", unknown fields rejected outright (not ignored), no markdown fences or surrounding
 * prose tolerated. Only "open_app" is registered in Milestone 1; everything else is decoded
 * generically into [parameters] and rejected by the registry (see ActionRegistry).
 */
@Serializable
data class ActionEnvelope(
    val version: String,
    val action: String,
    val parameters: Map<String, JsonElement> = emptyMap()
)

/** Parser configured to reject unknown top-level fields and to require strict structure.
 *  This is the ONLY place raw LLM output text is deserialized — nothing else in the app
 *  should call kotlinx.serialization directly on model output. */
object ActionProtocol {
    const val SUPPORTED_VERSION = "1.0"
    private const val MAX_RESPONSE_BYTES = 8 * 1024

    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        encodeDefaults = true
    }

    sealed class DecodeResult {
        data class Ok(val envelope: ActionEnvelope) : DecodeResult()
        data class Rejected(val reason: String) : DecodeResult()
    }

    /** Decodes [raw] strictly. Rejects: oversized payloads, non-JSON surrounding text
     *  (including markdown code fences), unknown fields, missing/mismatched version,
     *  and multiple top-level actions (there is only ever one "action" key per envelope,
     *  so this is enforced structurally by the data class shape). */
    fun decode(raw: String): DecodeResult {
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_RESPONSE_BYTES) {
            return DecodeResult.Rejected("Response exceeds max size")
        }
        val trimmed = raw.trim()
        if (trimmed.startsWith("```") || trimmed.endsWith("```")) {
            return DecodeResult.Rejected("Markdown-fenced response rejected")
        }
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return DecodeResult.Rejected("Response is not a bare JSON object")
        }
        return try {
            val envelope = json.decodeFromString(ActionEnvelope.serializer(), trimmed)
            if (envelope.version != SUPPORTED_VERSION) {
                DecodeResult.Rejected("Unsupported protocol version: ${envelope.version}")
            } else {
                DecodeResult.Ok(envelope)
            }
        } catch (e: Exception) {
            DecodeResult.Rejected("Malformed action JSON: ${e.message}")
        }
    }
}
