package com.jarvis.data.logging

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

/** In-memory ring buffer, capped at [capacity] records. Sufficient for Milestone 1 per
 *  section 16 ("یک ring buffer محلی محدود کافی است"); Room is only warranted once persistent
 *  history across process death is actually needed. Nothing here ever receives raw audio,
 *  secrets, or full transcripts — callers are responsible for not passing them in. */
class LocalEventLogger(private val capacity: Int = 200) : EventLogger {

    private val mutex = Mutex()
    private val buffer = ArrayDeque<JarvisEventRecord>(capacity)

    override suspend fun log(record: JarvisEventRecord) = mutex.withLock {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(record)
    }

    override suspend fun recent(limit: Int): List<JarvisEventRecord> = mutex.withLock {
        buffer.toList().takeLast(limit)
    }

    override suspend fun clear() = mutex.withLock {
        buffer.clear()
    }
}
