package com.jarvis.providers.localllm

import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.llm.LLMAvailability
import com.jarvis.core.llm.LLMProvider
import com.jarvis.core.llm.LLMProviderId
import com.jarvis.core.llm.LLMRequest
import com.jarvis.core.llm.LLMResponse

class LocalLLMProvider(private val backend: LocalInferenceBackend) : LLMProvider {
    override val id: LLMProviderId = LLMProviderId.LOCAL

    override suspend fun checkAvailability(): LLMAvailability =
        if (backend.isModelLoaded()) LLMAvailability.Available
        else LLMAvailability.Unavailable("Model not installed")

    override suspend fun complete(request: LLMRequest): OperationResult<LLMResponse> {
        val start = System.currentTimeMillis()
        if (!backend.isModelLoaded()) {
            return OperationResult.Failure("Local model not installed", ErrorCategory.PROVIDER_UNAVAILABLE)
        }
        val prompt = buildPrompt(request)
        return when (val result = backend.infer(prompt, request.timeoutMillis)) {
            is OperationResult.Success -> OperationResult.Success(
                LLMResponse(
                    providerId = LLMProviderId.LOCAL,
                    rawJson = result.value,
                    latencyMillis = System.currentTimeMillis() - start
                )
            )
            is OperationResult.Failure -> result
            is OperationResult.Cancelled -> result
        }
    }

    private fun buildPrompt(request: LLMRequest): String = buildString {
        appendLine(request.systemPrompt)
        appendLine("Candidates:")
        request.candidates.forEach { appendLine("- ${it.id}: ${it.label}") }
        appendLine("User said: ${request.transcript}")
    }
}
