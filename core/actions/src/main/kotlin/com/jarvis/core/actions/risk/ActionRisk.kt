package com.jarvis.core.actions.risk

/** Coarse risk tier per action type. Milestone 1 only ever produces LOW (open_app), but the
 *  type exists now so ActionEngine's confirmation gate doesn't need to change shape when
 *  higher-risk actions (calls, messaging) are added later. */
enum class RiskLevel { LOW, MEDIUM, HIGH }

object ActionRisk {
    private val riskByType = mapOf(
        "open_app" to RiskLevel.LOW
    )

    fun riskOf(actionType: String): RiskLevel = riskByType[actionType] ?: RiskLevel.HIGH

    fun requiresConfirmation(actionType: String): Boolean = riskOf(actionType) != RiskLevel.LOW
}
