package com.jarvis.platform.androidcontrol.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Resolves launcher-visible apps via PackageManager's standard launcher-intent query, which
 * is package-visibility-safe (no QUERY_ALL_PACKAGES — see section 9). Used two ways:
 *  1. To narrow a spoken app name ("Telegram") down to a small candidate list BEFORE calling
 *     the LLM, so the model is only ever asked to choose among real, installed packages
 *     (section 13) instead of being given (or hallucinating) the full app list.
 *  2. To confirm, after the model responds, that the chosen packageName is still installed
 *     and launchable right before executing.
 */
class AppCatalog(private val context: Context) {

    private fun queryLauncherApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved.map {
            LaunchableApp(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString()
            )
        }.distinctBy { it.packageName }
    }

    /** Returns a short, bounded candidate list matching [spokenName] by fuzzy label match.
     *  Never returns the full installed-app list — only apps plausibly matching what the
     *  user said get sent to the LLM as candidates (section 13: don't leak the full catalog).
     *
     *  Matches against BOTH the launcher label and the package name. This matters because
     *  [android.content.pm.PackageManager.loadLabel] returns a locale-dependent string — on a
     *  device set to a non-English locale (e.g. Persian), an app like Telegram is very often
     *  installed with a translated label ("تلگرام") even though the user spoke the Latin name
     *  ("telegram"). The label alone then never contains the spoken needle. The package name
     *  (e.g. "org.telegram.messenger") is locale-independent and still contains it, so it acts
     *  as a fallback signal without requiring a maintained alias table. */
    fun findCandidates(spokenName: String, maxResults: Int = 5): List<LaunchableApp> {
        val needle = spokenName.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        return queryLauncherApps()
            .filter {
                val label = it.label.lowercase()
                val pkg = it.packageName.lowercase()
                label.contains(needle) || needle.contains(label) || pkg.contains(needle)
            }
            .sortedBy { it.label.length }
            .take(maxResults)
    }

    /** Re-confirms a specific package is still installed and launchable, used as the final
     *  gate immediately before execution. */
    fun isInstalledAndLaunchable(packageName: String): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        return launchIntent != null
    }
}
