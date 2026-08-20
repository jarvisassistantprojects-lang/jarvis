package com.jarvis.platform.androidcontrol.apps

import android.content.Context
import android.content.Intent
import com.jarvis.core.actions.execution.ActionExecutor
import com.jarvis.core.actions.registry.ActionRegistry
import com.jarvis.core.actions.validation.ValidationResult
import com.jarvis.core.domain.model.ErrorCategory
import com.jarvis.core.domain.model.OperationResult

/**
 * Executes "open_app". Per section 1.2, this is only reliable while JarvisActivity is in the
 * foreground (Milestone 1's acceptance bar) — background-activity-launch restrictions on
 * modern Android can silently drop startActivity calls from a bare foreground service.
 * [isCallerForeground] must reflect the coordinator's actual foreground/visible state.
 */
class OpenAppExecutor(
    private val context: Context,
    private val appCatalog: AppCatalog,
    private val isCallerForeground: () -> Boolean
) : ActionExecutor {

    override val handles: String = ActionRegistry.OPEN_APP

    override suspend fun execute(validated: ValidationResult): OperationResult<String> {
        val openApp = validated as? ValidationResult.OpenApp
            ?: return OperationResult.Failure("OpenAppExecutor received a non-open_app result", ErrorCategory.ACTION_EXECUTION_FAILED)

        if (!isCallerForeground()) {
            return OperationResult.Failure(
                "Cannot reliably start ${openApp.displayName}: JARVIS is not in the foreground " +
                    "(background activity launch is restricted on modern Android)",
                ErrorCategory.ACTION_EXECUTION_FAILED
            )
        }

        // Final re-check immediately before launch — the app could have been uninstalled
        // between validation and execution.
        if (!appCatalog.isInstalledAndLaunchable(openApp.packageName)) {
            return OperationResult.Failure(
                "${openApp.displayName} is no longer installed or launchable",
                ErrorCategory.ACTION_EXECUTION_FAILED
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(openApp.packageName)
            ?: return OperationResult.Failure("No launch intent for ${openApp.packageName}", ErrorCategory.ACTION_EXECUTION_FAILED)

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            OperationResult.Success("Opened ${openApp.displayName}")
        } catch (e: Exception) {
            OperationResult.Failure("Failed to launch ${openApp.displayName}: ${e.message}", ErrorCategory.ACTION_EXECUTION_FAILED)
        }
    }
}
