package com.jarvis.core.actions.registry

/** Per-action capability tier, per technical review section 2. The registry is intentionally
 *  broader than what's implemented in Milestone 1 so the UI/settings can show "coming soon"
 *  states honestly instead of pretending unsupported actions don't exist. */
enum class ActionCapability { AVAILABLE, REQUIRES_PERMISSION, REQUIRES_USER_CONFIRMATION, UNSUPPORTED }

data class RegisteredAction(
    val type: String,
    val capability: ActionCapability,
    val note: String
)

/** Single source of truth for "what can JARVIS actually do on this device right now".
 *  Only [OPEN_APP] is capability AVAILABLE and wired to an executor in Milestone 1; every
 *  other entry exists so unknown/future actions from a model response are rejected with a
 *  clear reason instead of silently no-op'ing. */
object ActionRegistry {
    const val OPEN_APP = "open_app"

    private val actions: Map<String, RegisteredAction> = listOf(
        RegisteredAction(OPEN_APP, ActionCapability.AVAILABLE, "Implemented in Milestone 1"),
        RegisteredAction("close_app", ActionCapability.UNSUPPORTED, "Force-stop is not possible on non-root Android"),
        RegisteredAction("go_home", ActionCapability.UNSUPPORTED, "Requires Accessibility, out of scope for Milestone 1"),
        RegisteredAction("go_back", ActionCapability.UNSUPPORTED, "Requires Accessibility, out of scope for Milestone 1"),
        RegisteredAction("set_volume", ActionCapability.UNSUPPORTED, "Out of scope for Milestone 1"),
        RegisteredAction("set_brightness", ActionCapability.UNSUPPORTED, "Requires WRITE_SETTINGS; out of scope for Milestone 1"),
        RegisteredAction("flashlight_on", ActionCapability.UNSUPPORTED, "Out of scope for Milestone 1"),
        RegisteredAction("flashlight_off", ActionCapability.UNSUPPORTED, "Out of scope for Milestone 1"),
        RegisteredAction("wifi_on", ActionCapability.UNSUPPORTED, "Direct toggle blocked on Android 10+"),
        RegisteredAction("wifi_off", ActionCapability.UNSUPPORTED, "Direct toggle blocked on Android 10+"),
        RegisteredAction("bluetooth_on", ActionCapability.UNSUPPORTED, "Requires user dialog on modern Android"),
        RegisteredAction("bluetooth_off", ActionCapability.UNSUPPORTED, "Requires user dialog on modern Android"),
        RegisteredAction("take_screenshot", ActionCapability.UNSUPPORTED, "Requires Accessibility or MediaProjection consent"),
        RegisteredAction("read_notifications", ActionCapability.UNSUPPORTED, "Requires Notification Listener access"),
        RegisteredAction("open_url", ActionCapability.UNSUPPORTED, "Scheme validation not implemented yet"),
    ).associateBy { it.type }

    fun lookup(type: String): RegisteredAction? = actions[type]

    fun all(): List<RegisteredAction> = actions.values.toList()
}
