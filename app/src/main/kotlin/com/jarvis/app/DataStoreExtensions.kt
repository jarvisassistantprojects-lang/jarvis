package com.jarvis.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.jarvisDataStore by preferencesDataStore(name = "jarvis_settings")
