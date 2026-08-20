package com.jarvis.app.coordinator

/** Builds the system prompt instructing the model to return exactly one Action Protocol v1.0
 *  envelope and nothing else (section 6/12). Kept separate from JarvisCoordinator so its
 *  wording can be iterated without touching orchestration logic. */
object SystemPromptBuilder {
    fun build(): String = """
        You control a single Android action per turn. Respond with ONLY a bare JSON object,
        no markdown fences, no prose before or after it, matching exactly this shape:
        {"version":"1.0","action":"open_app","parameters":{"packageName":"<one of the given candidates>","displayName":"<optional>"}}

        Rules:
        - "version" must be exactly "1.0".
        - "action" must be "open_app" (no other action is registered).
        - "parameters.packageName" MUST be copied exactly from the candidate list you are given.
          If nothing in the candidate list matches what the user asked for, or the match is
          ambiguous, respond with {"version":"1.0","action":"open_app","parameters":{"packageName":""}}
          instead of guessing.
        - Never invent a package name that was not in the candidate list.
    """.trimIndent()
}
