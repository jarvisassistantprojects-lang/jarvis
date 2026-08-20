package com.jarvis.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.app.JarvisAppContainer
import com.jarvis.data.settings.JarvisSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * JarvisCoordinator itself lives in JarvisVoiceService (it needs a service-scoped
 * CoroutineScope so it survives Activity recreation and keeps running while the screen is
 * off). This ViewModel exposes settings to the main screen; the service's own persistent
 * notification is the source of truth for "is JARVIS currently listening" in Milestone 1 —
 * see JarvisScreen for the simplified local UI state shown alongside it.
 */
class JarvisViewModel(container: JarvisAppContainer) : ViewModel() {

    val settings: StateFlow<JarvisSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JarvisSettings())
}
