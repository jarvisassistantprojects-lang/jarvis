package com.jarvis.providers.remotellm

import com.jarvis.core.domain.model.OperationResult
import com.jarvis.core.llm.LLMRequest

/** One adapter per remote API shape. Milestone 1 ships only [OpenAICompatibleAdapter]
 *  (section 10: "فقط adapter استاندارد chat-completions"); a bespoke API needs its own
 *  adapter implementing this interface — there is no generic "custom API" support without a
 *  defined contract. */
interface RemoteLLMAdapter {
    suspend fun complete(config: RemoteLLMConfig, request: LLMRequest): OperationResult<String>
    suspend fun testConnection(config: RemoteLLMConfig): OperationResult<Unit>
}
