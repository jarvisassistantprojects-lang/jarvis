package com.jarvis.providers.remotellm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Only unwraps the OpenAI-compatible chat-completion envelope to get at the assistant's raw
 *  message text. It deliberately does NOT interpret that text as an action — that is the
 *  actions module's job (ActionProtocol.decode), kept as a second, independent validation
 *  pass per section 10: "ادعای JSON-only مدل به‌تنهایی کنترل امنیتی نیست". */
object RemoteResponseDecoder {

    @Serializable
    private data class ChatCompletionResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message? = null)

    @Serializable
    private data class Message(val content: String? = null)

    private val json = Json { ignoreUnknownKeys = true }

    fun extractMessageText(rawResponseBody: String): Result<String> = try {
        val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), rawResponseBody)
        val content = parsed.choices.firstOrNull()?.message?.content
        if (content.isNullOrBlank()) Result.failure(IllegalStateException("Empty choices[0].message.content"))
        else Result.success(content)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
