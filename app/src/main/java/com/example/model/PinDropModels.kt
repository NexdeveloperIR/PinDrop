package com.example.model

enum class PinStyle(
    val id: String,
    val displayName: String,
    val unlockRequirement: String
) {
    CLASSIC_DOT("classic_dot", "CLASSIC DOT", "Default minimal pin"),
    HOLLOW_RING("hollow_ring", "HOLLOW RING", "Score 10 in a single run"),
    DIAMOND("diamond", "DIAMOND", "Reach Level 4 in Classic"),
    SPEARHEAD("spearhead", "SPEARHEAD", "Score 25 in a single run"),
    CROSSHAIR("crosshair", "CROSSHAIR", "Complete any Daily Challenge"),
    ORBITAL("orbital", "ORBITAL", "Score 50 in a single run");

    companion object {
        fun fromId(id: String?): PinStyle =
            entries.firstOrNull { it.id == id } ?: CLASSIC_DOT
    }
}

enum class GameMode {
    CLASSIC,
    DAILY
}

sealed interface ScreenRoute {
    data object Splash : ScreenRoute
    data object MainMenu : ScreenRoute
    data class Gameplay(val mode: GameMode) : ScreenRoute
    data object Leaderboard : ScreenRoute
    data object Settings : ScreenRoute
    data object PinStyles : ScreenRoute
}

data class RunRecord(
    val score: Int,
    val level: Int,
    val date: String,
    val mode: GameMode
)

data class PlayerStats(
    val bestScore: Int = 0,
    val highestLevel: Int = 1,
    val dailyBestScore: Int = 0,
    val dailyCompletedDate: String = "",
    val dailyStreak: Int = 0,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val selectedStyle: PinStyle = PinStyle.CLASSIC_DOT,
    val unlockedStyles: Set<PinStyle> = setOf(PinStyle.CLASSIC_DOT),
    val topRuns: List<RunRecord> = emptyList()
) {
    fun isDailyCompletedToday(today: String): Boolean = dailyCompletedDate == today
}

data class LevelSpec(
    val levelNumber: Int,
    val targetPins: Int,
    val initialPinAngles: List<Float>,
    val baseSpeedRadPerSec: Float,
    val initialDirection: Float,
    val allowDirectionReversal: Boolean,
    val allowSpeedVariation: Boolean,
    val reversalIntervalSec: Float,
    val variationAmplitude: Float
)

data class AttachedPin(
    val angleRad: Float,
    val isObstacle: Boolean = false
)

data class FlyingPin(
    val id: Long,
    /** Normalized progress from launch line (0f) to wheel impact point (1f). */
    val progress: Float = 0f
)

data class PulseRing(
    val id: Long,
    val progress: Float = 0f
)

enum class GamePhase {
    PLAYING,
    PAUSED,
    LEVEL_TRANSITION,
    COLLISION_BEAT,
    GAME_OVER
}

data class GameplayUiState(
    val mode: GameMode = GameMode.CLASSIC,
    val phase: GamePhase = GamePhase.PLAYING,
    val level: Int = 1,
    val score: Int = 0,
    val pinsRemainingInLevel: Int = 6,
    val targetPinsInLevel: Int = 6,
    val wheelAngleRad: Float = 0f,
    val currentSpeedRadPerSec: Float = 1.45f,
    val stageSpeedMultiplier: Float = 1.0f,
    val rotationDirection: Float = 1f,
    val attachedPins: List<AttachedPin> = emptyList(),
    val flyingPins: List<FlyingPin> = emptyList(),
    val pulseRings: List<PulseRing> = emptyList(),
    val wheelRecoil: Float = 0f,
    val screenShake: Float = 0f,
    val collisionAngles: Pair<Float, Float>? = null,
    val bestScoreSnapshot: Int = 0,
    val isNewBest: Boolean = false,
    val isDailyVictory: Boolean = false,
    val newlyUnlockedStyles: List<PinStyle> = emptyList(),
    val directionFlipFlash: Float = 0f,
    val showTapHint: Boolean = true
)

sealed interface GameEffect {
    data object PlayShootSound : GameEffect
    data object PlayStickSound : GameEffect
    data object PlayLevelClearSound : GameEffect
    data object PlayCollisionSound : GameEffect
    data object TriggerTickHaptic : GameEffect
    data object TriggerCollisionHaptic : GameEffect
}
