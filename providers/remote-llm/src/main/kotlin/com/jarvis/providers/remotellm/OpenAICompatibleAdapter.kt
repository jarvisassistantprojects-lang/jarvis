package com.jarvis.providers.remotellm

import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.llm.LLMRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Section 10: standard chat-completions shape only, HTTPS by default (or localhost for dev),
 * no cross-host redirects, API key never logged, response size/time bounded by the caller's
 * request timeout. The returned raw text is NOT parsed as an action here — see
 * RemoteResponseDecoder + core/actions.
 */
class OpenAICompatibleAdapter : RemoteLLMAdapter {

    private val client = OkHttpClient.Builder()
        .followRedirects(false) // section 3: "غیرفعال بودن redirect بین hostها"
        .followSslRedirects(false)
        .build()

    override suspend fun complete(config: RemoteLLMConfig, request: LLMRequest): OperationResult<String> {
        if (!config.isSchemeAllowed()) {
            return OperationResult.Failure("Remote base URL must use HTTPS", ErrorCategory.PROVIDER_UNAVAILABLE)
        }

        val body = buildJsonObject {
            put("model", config.model)
            put("stream", false) // Milestone 1 executes only after a complete response (section 10)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", request.systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", buildUserContent(request))
                }
            }
        }

        val httpRequest = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(Json.encodeToString(JsonObject.serializer(), body).toRequestBody("application/json".toMediaType()))
            .build()

        val timeoutClient = client.newBuilder()
            .callTimeout(config.timeoutMillis, TimeUnit.MILLISECONDS)
            .build()

        return try {
            val responseBody = executeAsync(timeoutClient, httpRequest)
            RemoteResponseDecoder.extractMessageText(responseBody).fold(
                onSuccess = { OperationResult.Success(it) },
                onFailure = { OperationResult.Failure("Malformed remote response: ${it.message}", ErrorCategory.INVALID_ACTION_RESPONSE) }
            )
        } catch (e: IOException) {
            OperationResult.Failure("Remote request failed: ${e.message}", ErrorCategory.PROVIDER_UNAVAILABLE)
        }
    }

    override suspend fun testConnection(config: RemoteLLMConfig): OperationResult<Unit> {
        if (!config.isSchemeAllowed()) {
            return OperationResult.Failure("Remote base URL must use HTTPS", ErrorCategory.PROVIDER_UNAVAILABLE)
        }
        val httpRequest = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/models")
            .header("Authorization", "Bearer ${config.apiKey}")
            .get()
            .build()
        return try {
            val timeoutClient = client.newBuilder().callTimeout(config.timeoutMillis, TimeUnit.MILLISECONDS).build()
            executeAsync(timeoutClient, httpRequest)
            OperationResult.Success(Unit)
        } catch (e: IOException) {
            OperationResult.Failure("Connection test failed: ${e.message}", ErrorCategory.PROVIDER_UNAVAILABLE)
        }
    }

    private suspend fun executeAsync(client: OkHttpClient, request: Request): String =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWith(Result.failure(e))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val text = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            if (cont.isActive) cont.resumeWith(Result.failure(IOException("HTTP ${resp.code}: $text")))
                        } else {
                            if (cont.isActive) cont.resume(text)
                        }
                    }
                }
            })
        }

    private fun buildUserContent(request: LLMRequest): String = buildString {
        if (request.candidates.isNotEmpty()) {
            appendLine("Valid candidates (choose packageName from this list only):")
            request.candidates.forEach { appendLine("- ${it.id}: ${it.label}") }
        }
        append(request.transcript)
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    key: String,
    builderAction: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit
) {
    put(key, kotlinx.serialization.json.buildJsonArray(builderAction))
}

private fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(
    builderAction: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    add(buildJsonObject(builderAction))
}
