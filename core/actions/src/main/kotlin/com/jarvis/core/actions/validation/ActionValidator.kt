package com.jarvis.core.actions.validation

import com.jarvis.core.actions.protocol.ActionEnvelope
import com.jarvis.core.actions.protocol.OpenAppParameterCodec
import com.jarvis.core.actions.registry.ActionCapability
import com.jarvis.core.actions.registry.ActionRegistry

/** A candidate app the caller resolved locally before ever calling the LLM. The model's
 *  chosen packageName must equal one of these exactly — see section 13. */
data class AppCandidate(val packageName: String, val displayName: String)

sealed class ValidationResult {
    data class OpenApp(val packageName: String, val displayName: String) : ValidationResult()
    data class Rejected(val reason: String) : ValidationResult()
}

/** Validates a decoded [ActionEnvelope] against the action registry and, for open_app,
 *  against the exact candidate list supplied to the model — never a free-text package name,
 *  and never an ambiguous or missing match (section 13: "اگر تطبیق مبهم باشد، execution
 *  انجام نمی‌شود"). */
object ActionValidator {

    private val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    fun validate(envelope: ActionEnvelope, candidates: List<AppCandidate>): ValidationResult {
        val registered = ActionRegistry.lookup(envelope.action)
            ?: return ValidationResult.Rejected("Unknown action: ${envelope.action}")

        if (registered.capability != ActionCapability.AVAILABLE) {
            return ValidationResult.Rejected("Action not available on this device: ${envelope.action} (${registered.note})")
        }

        return when (envelope.action) {
            ActionRegistry.OPEN_APP -> validateOpenApp(envelope, candidates)
            else -> ValidationResult.Rejected("No validator wired for registered action: ${envelope.action}")
        }
    }

    private fun validateOpenApp(envelope: ActionEnvelope, candidates: List<AppCandidate>): ValidationResult {
        val decoded = OpenAppParameterCodec.decode(envelope.parameters).getOrElse {
            return ValidationResult.Rejected("Malformed open_app parameters: ${it.message}")
        }

        if (!packageNamePattern.matches(decoded.packageName)) {
            return ValidationResult.Rejected("packageName does not look like an Android package: ${decoded.packageName}")
        }

        val matches = candidates.filter { it.packageName == decoded.packageName }
        return when {
            matches.isEmpty() -> ValidationResult.Rejected(
                "packageName '${decoded.packageName}' is not among the offered candidates — refusing to execute"
            )
            matches.size > 1 -> ValidationResult.Rejected("Ambiguous candidate match — refusing to execute")
            else -> ValidationResult.OpenApp(matches.single().packageName, matches.single().displayName)
        }
    }
}
