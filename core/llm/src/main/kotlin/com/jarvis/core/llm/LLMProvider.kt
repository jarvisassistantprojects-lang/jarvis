package com.jarvis.core.llm

import com.jarvis.core.domain.model.OperationResult

interface LLMProvider {
    val id: LLMProviderId

    /** Cheap, side-effect-free check of whether this provider can currently serve a request
     *  (e.g. LOCAL checks whether a model file + backend are actually loaded; REMOTE checks
     *  whether a base URL + API key are configured). Must never itself attempt inference. */
    suspend fun checkAvailability(): LLMAvailability

    /** Runs one request. Implementations must honor [LLMRequest.timeoutMillis] themselves
     *  (in addition to any caller-side timeout) and must return [OperationResult.Failure]
     *  rather than throwing on network/parse errors. */
    suspend fun complete(request: LLMRequest): OperationResult<LLMResponse>
}
