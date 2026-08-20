package com.jarvis.data.logging

/** Minimal log record per section 16. Deliberately excludes: raw audio, API keys,
 *  Authorization headers, full raw server responses, and full transcripts (transcript is
 *  opt-in only and, when enabled, stored separately with limited retention). */
data class JarvisEventRecord(
    val timestampMillis: Long,
    val stateTransition: String,
    val providerUsed: String?,
    val actionType: String?,
    val result: String,
    val latencyMillis: Long?,
    val errorCategory: String?
)

interface EventLogger {
    suspend fun log(record: JarvisEventRecord)
    suspend fun recent(limit: Int = 50): List<JarvisEventRecord>
    suspend fun clear()
}
