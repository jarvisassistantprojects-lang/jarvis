package com.jarvis.platform.androidcontrol

import com.jarvis.core.actions.registry.ActionRegistry
import com.jarvis.core.actions.registry.RegisteredAction

/** Thin facade the UI/settings layer reads to show real device capability per action
 *  (section 2's table). In Milestone 1 this simply mirrors [ActionRegistry]; it exists as a
 *  seam so future device-specific checks (e.g. "is WRITE_SETTINGS granted") can be layered in
 *  without the UI depending on core/actions directly. */
class AndroidControlEngine {
    fun capabilities(): List<RegisteredAction> = ActionRegistry.all()
}
