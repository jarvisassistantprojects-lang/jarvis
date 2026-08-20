package com.jarvis.core.actions.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Typed parameters for "open_app". [packageName] must be one of the candidates the caller
 *  gave the model (see AppCatalog) — never trusted as free text (section 13: anti-hallucination
 *  candidate matching). [displayName] is optional and cosmetic only, never used for matching. */
@Serializable
data class OpenAppParameters(
    val packageName: String,
    val displayName: String? = null
)

object OpenAppParameterCodec {
    private val json = Json { ignoreUnknownKeys = false }

    fun decode(parameters: Map<String, JsonElement>): Result<OpenAppParameters> = try {
        val obj = kotlinx.serialization.json.JsonObject(parameters)
        Result.success(json.decodeFromJsonElement(OpenAppParameters.serializer(), obj))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
