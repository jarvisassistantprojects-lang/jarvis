package com.jarvis.core.domain.cancellation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Enforces the "single in-flight operation" rule from the technical review (section 3 / 14):
 * cancel the previous coroutine before starting the next one, and never let a stale job's
 * result (e.g. a late HTTP response or a late action) apply after a newer one has started
 * or after the user cancelled. Every launch bumps a generation counter; callers that want to
 * apply a result must check they are still the current generation.
 */
class OperationController(private val scope: CoroutineScope) {

    private var currentJob: Job? = null
    private var generation: Long = 0L

    /** Cancels any in-flight operation and launches a new one, tagged with a generation id.
     *  [block] receives a lambda it must call before applying any side effect / state
     *  transition; the lambda returns false if this generation has been superseded. */
    fun launchExclusive(
        context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
        block: suspend CoroutineScope.(isCurrent: () -> Boolean) -> Unit
    ) {
        currentJob?.cancel()
        val myGeneration = ++generation
        currentJob = scope.launch(context) {
            block { myGeneration == generation }
        }
    }

    /** Cancels the in-flight operation without starting a new one (e.g. user said "cancel"). */
    fun cancelCurrent() {
        currentJob?.cancel()
        currentJob = null
        generation++
    }
}
