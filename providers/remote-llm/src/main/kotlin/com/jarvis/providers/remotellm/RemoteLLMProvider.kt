package com.jarvis.providers.remotellm

import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.llm.LLMAvailability
import com.jarvis.core.llm.LLMProvider
import com.jarvis.core.llm.LLMProviderId
import com.jarvis.core.llm.LLMRequest
import com.jarvis.core.llm.LLMResponse

class RemoteLLMProvider(
    private val configProvider: suspend () -> RemoteLLMConfig?,
    private val adapter: RemoteLLMAdapter = OpenAICompatibleAdapter()
) : LLMProvider {

    override val id: LLMProviderId = LLMProviderId.REMOTE

    override suspend fun checkAvailability(): LLMAvailability {
        val config = configProvider() ?: return LLMAvailability.Unavailable("No remote provider configured")
        if (config.baseUrl.isBlank() || config.apiKey.isBlank() || config.model.isBlank()) {
            return LLMAvailability.Unavailable("Remote base URL, API key, or model missing")
        }
        if (!config.isSchemeAllowed()) {
            return LLMAvailability.Unavailable("Remote base URL must use HTTPS")
        }
        return LLMAvailability.Available
    }

    override suspend fun complete(request: LLMRequest): OperationResult<LLMResponse> {
        val start = System.currentTimeMillis()
        val config = configProvider()
            ?: return OperationResult.Failure("No remote provider configured", ErrorCategory.PROVIDER_UNAVAILABLE)

        return when (val result = adapter.complete(config, request)) {
            is OperationResult.Success -> OperationResult.Success(
                LLMResponse(
                    providerId = LLMProviderId.REMOTE,
                    rawJson = result.value,
                    latencyMillis = System.currentTimeMillis() - start
                )
            )
            is OperationResult.Failure -> result
            is OperationResult.Cancelled -> result
        }
    }
}
