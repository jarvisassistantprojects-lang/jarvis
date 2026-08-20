package com.jarvis.providers.localllm

/** Describes a GGUF model the user has picked via Storage Access Framework, per section 11.
 *  Deliberately does not include a bundled/APK path — the model must never ship inside the
 *  APK (section 1.4). */
data class LocalModelDescriptor(
    val uri: String,
    val sha256: String,
    val sizeBytes: Long,
    val contextLength: Int,
    val threadCount: Int
)
