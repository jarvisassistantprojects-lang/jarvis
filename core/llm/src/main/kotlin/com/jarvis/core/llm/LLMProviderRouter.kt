package com.jarvis.core.llm

import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult

/**
 * Implements the routing rules from section 6:
 *  - LOCAL: only ever calls the local provider. If unavailable, fails with a clear reason
 *    ("Model not installed") — no fabricated response, no silent remote fallback.
 *  - REMOTE: only ever calls the remote provider.
 *  - AUTO: checks local availability first; uses local if available; falls back to remote
 *    ONLY if [allowAutoRemoteFallback] is true; otherwise fails explicitly.
 * The provider that actually served the request is always recorded in the returned
 * [LLMResponse.providerId] so callers/loggers never have to guess.
 */
class LLMProviderRouter(
    private val localProvider: LLMProvider,
    private val remoteProvider: LLMProvider
) {
    suspend fun complete(
        request: LLMRequest,
        mode: LLMMode,
        allowAutoRemoteFallback: Boolean
    ): OperationResult<LLMResponse> = when (mode) {
        LLMMode.LOCAL -> runOrUnavailable(localProvider, request, "Local model not installed")
        LLMMode.REMOTE -> runOrUnavailable(remoteProvider, request, "Remote provider not configured")
        LLMMode.AUTO -> {
            when (localProvider.checkAvailability()) {
                is LLMAvailability.Available -> localProvider.complete(request)
                is LLMAvailability.Unavailable -> {
                    if (!allowAutoRemoteFallback) {
                        OperationResult.Failure(
                            "Local model unavailable and remote fallback is disabled in settings",
                            ErrorCategory.PROVIDER_UNAVAILABLE
                        )
                    } else {
                        runOrUnavailable(remoteProvider, request, "Remote provider not configured")
                    }
                }
            }
        }
    }

    private suspend fun runOrUnavailable(
        provider: LLMProvider,
        request: LLMRequest,
        unavailableMessage: String
    ): OperationResult<LLMResponse> = when (val availability = provider.checkAvailability()) {
        is LLMAvailability.Available -> provider.complete(request)
        is LLMAvailability.Unavailable ->
            OperationResult.Failure("$unavailableMessage: ${availability.reason}", ErrorCategory.PROVIDER_UNAVAILABLE)
    }
}
