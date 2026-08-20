# JARVIS — Milestone 1 (per JARVIS Technical Review v1.0)

This is the Milestone 1 source tree, structured exactly per the review's section 7 module
layout. It targets minSdk 26 / compileSdk & targetSdk 36, Kotlin 2.0, Compose, manual DI
(`JarvisAppContainer`, no Hilt yet per the review's rationale).

## IMPORTANT — build status

This was generated without access to the Android SDK, Gradle, or a compiler/emulator, so
**it has not been compiled or run**. It's written carefully against real Android/Kotlin APIs
(SpeechRecognizer, TextToSpeech, AudioManager, PackageManager, Android Keystore, DataStore,
OkHttp, kotlinx.serialization, Vosk), but treat it as a strong first draft, not a
verified build. Expect to fix:
- Gradle Kotlin DSL / version-catalog wiring issues (plugin aliasing, `kotlin("jvm")`
  shorthand resolution) once you actually sync in Android Studio.
- Compose API drift (Material3 API surface moves between versions faster than most).
  Settings, stored via `AndroidKeystoreSecretStore`).
- The launcher icon is a placeholder vector (see `app/ICON_NOTE.txt`).

## What's real vs. stubbed

Fully implemented against real Android APIs:
- Wake word (Vosk, offline, no account), TTS, SpeechRecognizer, AudioSessionCoordinator (audio focus)
- Action Protocol v1.0 strict decode/validate/execute pipeline, `open_app` end-to-end
- AppCatalog (launcher-visible query only, no QUERY_ALL_PACKAGES)
- Remote OpenAI-compatible LLM provider (HTTPS-only, no redirects, secrets never logged)
- AES-GCM/Keystore secret storage, DataStore settings, in-memory ring-buffer event log
- Foreground microphone service with persistent notification + Stop action
- Compose UI: main screen (orb, start/stop, settings link) + settings screen

Deliberately stubbed, per the review's own instructions (section 1.4 / 11):
- **Local LLM inference** — `UnavailableLocalInferenceBackend` always reports
  "Model not installed"; there is no llama.cpp/JNI backend yet. This is intentional: the
  review explicitly forbids shipping a model in the APK or faking local responses.
- The main screen's orb is currently wired to a static `JarvisState.Idle` rather than a live
  stream from `JarvisCoordinator` (which runs inside `JarvisVoiceService`, a different
  component). Wiring live state across the service boundary (e.g. a bound service + a shared
  `StateFlow`, or broadcasting via a small IPC channel) is the next thing to build — right now
  the persistent notification is the actual source of truth for "is JARVIS listening".

## Build

Requires Android Studio (Ladybird/Koala or newer) with SDK 36, JDK 17.

```
./gradlew assembleDebug
```

## Layout

Matches technical review section 7 exactly: `app/`, `core/{domain,voice,llm,actions}`,
`platform/{voice-android,android-control}`, `providers/{local-llm,remote-llm}`,
`data/{settings,security,logging}`.
