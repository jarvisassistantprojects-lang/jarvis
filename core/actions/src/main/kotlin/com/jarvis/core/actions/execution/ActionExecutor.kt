package com.jarvis.core.actions.execution

import com.jarvis.core.actions.validation.ValidationResult
import com.jarvis.core.domain.model.OperationResult

/** Implemented per-action by the platform layer (e.g. OpenAppExecutor in
 *  platform/android-control). Kept generic over [ValidationResult] so ActionEngine can dispatch
 *  without a chain of "if action == ..." checks growing unbounded as actions are added. */
interface ActionExecutor {
    val handles: String
    suspend fun execute(validated: ValidationResult): OperationResult<String>
}
