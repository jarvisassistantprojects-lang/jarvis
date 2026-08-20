package com.jarvis.providers.localllm

import com.jarvis.core.domain.model.OperationResult

/** Contract for an on-device inference backend (future: llama.cpp via JNI — see section 11).
 *  Milestone 1 ships this interface and [UnavailableLocalInferenceBackend] only; there is
 *  intentionally no concrete LlamaCppBackend yet, and nothing here is allowed to fabricate a
 *  plausible-looking response (section 1.4: "پاسخ ثابت... جعلی محسوب می‌شود"). */
interface LocalInferenceBackend {
    suspend fun isModelLoaded(): Boolean
    suspend fun loadModel(descriptor: LocalModelDescriptor): OperationResult<Unit>
    suspend fun unloadModel()
    suspend fun infer(prompt: String, timeoutMillis: Long): OperationResult<String>
}

/** Always-unavailable stand-in used until a real backend (llama.cpp) is wired in. Every call
 *  fails explicitly rather than returning any fixed/stub text. */
class UnavailableLocalInferenceBackend(private val reason: String = "No local inference backend implemented yet") :
    LocalInferenceBackend {
    override suspend fun isModelLoaded(): Boolean = false
    override suspend fun loadModel(descriptor: LocalModelDescriptor): OperationResult<Unit> =
        OperationResult.Failure(reason, com.jarvis.core.domain.model.ErrorCategory.PROVIDER_UNAVAILABLE)
    override suspend fun unloadModel() {}
    override suspend fun infer(prompt: String, timeoutMillis: Long): OperationResult<String> =
        OperationResult.Failure(reason, com.jarvis.core.domain.model.ErrorCategory.PROVIDER_UNAVAILABLE)
}
