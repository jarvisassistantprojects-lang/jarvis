package com.jarvis.core.actions.execution

import com.jarvis.core.actions.protocol.ActionProtocol
import com.jarvis.core.actions.risk.ActionRisk
import com.jarvis.core.actions.validation.ActionValidator
import com.jarvis.core.actions.validation.AppCandidate
import com.jarvis.core.actions.validation.ValidationResult
import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult

/**
 * Full pipeline for turning raw LLM output into an executed action, enforcing the ordering
 * from section 13: strict decode -> registry lookup -> typed validation against real
 * candidates -> risk-based confirmation gate -> dispatch to the matching executor.
 * No step is skippable; a failure at any stage returns immediately without executing.
 */
class ActionEngine(
    private val executors: List<ActionExecutor>,
    private val requestConfirmation: suspend (actionType: String, summary: String) -> Boolean = { _, _ -> true }
) {
    suspend fun run(
        rawModelOutput: String,
        candidates: List<AppCandidate>
    ): OperationResult<String> {
        val decoded = when (val result = ActionProtocol.decode(rawModelOutput)) {
            is ActionProtocol.DecodeResult.Rejected ->
                return OperationResult.Failure(result.reason, ErrorCategory.INVALID_ACTION_RESPONSE)
            is ActionProtocol.DecodeResult.Ok -> result.envelope
        }

        val validated = ActionValidator.validate(decoded, candidates)
        if (validated is ValidationResult.Rejected) {
            return OperationResult.Failure(validated.reason, ErrorCategory.ACTION_VALIDATION_FAILED)
        }

        if (ActionRisk.requiresConfirmation(decoded.action)) {
            val confirmed = requestConfirmation(decoded.action, describe(validated))
            if (!confirmed) return OperationResult.Cancelled
        }

        val executor = executors.firstOrNull { it.handles == decoded.action }
            ?: return OperationResult.Failure(
                "No executor registered for action: ${decoded.action}",
                ErrorCategory.ACTION_NOT_REGISTERED
            )

        return executor.execute(validated)
    }

    private fun describe(validated: ValidationResult): String = when (validated) {
        is ValidationResult.OpenApp -> "Open ${validated.displayName}"
        is ValidationResult.Rejected -> "" // unreachable here, kept exhaustive
    }
}
