package com.jarvis.core.domain.model

/** Generic success/failure/cancelled result, used by voice, LLM, and action layers so that
 *  no layer needs to throw across a coroutine/module boundary or invent its own ad-hoc
 *  result type. Failure always carries a stable [ErrorCategory] for logging and UI. */
sealed class OperationResult<out T> {
    data class Success<T>(val value: T) : OperationResult<T>()
    data class Failure(val message: String, val category: ErrorCategory) : OperationResult<Nothing>()
    data object Cancelled : OperationResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): OperationResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
        is Cancelled -> this
    }

    fun getOrNull(): T? = (this as? Success)?.value
}
