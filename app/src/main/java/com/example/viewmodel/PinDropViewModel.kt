package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PinDropPreferencesRepository
import com.example.game.LevelGenerator
import com.example.model.AttachedPin
import com.example.model.FlyingPin
import com.example.model.GameEffect
import com.example.model.GameMode
import com.example.model.GamePhase
import com.example.model.GameplayUiState
import com.example.model.LevelSpec
import com.example.model.PinStyle
import com.example.model.PlayerStats
import com.example.model.PulseRing
import com.example.model.ScreenRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class PinDropViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PinDropPreferencesRepository(application.applicationContext)

    val playerStats: StateFlow<PlayerStats> = repository.playerStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerStats()
    )

    private val _currentRoute = MutableStateFlow<ScreenRoute>(ScreenRoute.Splash)
    val currentRoute: StateFlow<ScreenRoute> = _currentRoute.asStateFlow()

    private val _gameplayState = MutableStateFlow(GameplayUiState())
    val gameplayState: StateFlow<GameplayUiState> = _gameplayState.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    private var currentLevelSpec: LevelSpec = LevelGenerator.classicLevel(1)
    private var elapsedLevelSec: Float = 0f
    private var reversalCountdownSec: Float = 3f
    private var targetDirection: Float = 1f
    private var currentAngularVelocity: Float = 1.5f
    private var speedBoostMultiplier: Float = 1f
    private var fireCooldownSec: Float = 0f
    private var phaseTimerSec: Float = 0f
    private var nextPinId: Long = 1L
    private var nextRingId: Long = 1L
    private var hasRecordedCurrentRun: Boolean = false

    fun onSplashComplete() {
        if (_currentRoute.value is ScreenRoute.Splash) {
            _currentRoute.value = ScreenRoute.MainMenu
        }
    }

    fun navigateTo(route: ScreenRoute) {
        _currentRoute.value = route
    }

    fun navigateHome() {
        _currentRoute.value = ScreenRoute.MainMenu
    }

    fun startClassicGame() {
        startNewRun(mode = GameMode.CLASSIC)
    }

    fun startDailyChallenge() {
        startNewRun(mode = GameMode.DAILY)
    }

    fun restartCurrentGame() {
        startNewRun(mode = _gameplayState.value.mode)
    }

    private fun startNewRun(mode: GameMode) {
        val stats = playerStats.value
        val spec = when (mode) {
            GameMode.CLASSIC -> LevelGenerator.classicLevel(1)
            GameMode.DAILY -> LevelGenerator.dailyLevel(PinDropPreferencesRepository.currentDateKey())
        }
        hasRecordedCurrentRun = false
        setupLevel(
            spec = spec,
            mode = mode,
            score = 0,
            bestSnapshot = if (mode == GameMode.DAILY) stats.dailyBestScore else stats.bestScore,
            showTapHint = true
        )
        _currentRoute.value = ScreenRoute.Gameplay(mode)
    }

    private fun setupLevel(
        spec: LevelSpec,
        mode: GameMode,
        score: Int,
        bestSnapshot: Int,
        showTapHint: Boolean
    ) {
        currentLevelSpec = spec
        elapsedLevelSec = 0f
        reversalCountdownSec = spec.reversalIntervalSec
        targetDirection = spec.initialDirection
        speedBoostMultiplier = 1f
        currentAngularVelocity = spec.baseSpeedRadPerSec * spec.initialDirection
        fireCooldownSec = 0f
        phaseTimerSec = 0f

        _gameplayState.value = GameplayUiState(
            mode = mode,
            phase = GamePhase.PLAYING,
            level = spec.levelNumber,
            score = score,
            pinsRemainingInLevel = spec.targetPins,
            targetPinsInLevel = spec.targetPins,
            wheelAngleRad = 0f,
            currentSpeedRadPerSec = abs(currentAngularVelocity),
            stageSpeedMultiplier = spec.baseSpeedRadPerSec / LevelGenerator.BASE_SPEED_STAGE_1,
            rotationDirection = targetDirection,
            attachedPins = spec.initialPinAngles.map { AttachedPin(angleRad = it, isObstacle = true) },
            flyingPins = emptyList(),
            pulseRings = emptyList(),
            wheelRecoil = 0f,
            screenShake = 0f,
            collisionAngles = null,
            bestScoreSnapshot = bestSnapshot,
            isNewBest = false,
            isDailyVictory = false,
            newlyUnlockedStyles = emptyList(),
            directionFlipFlash = 0f,
            showTapHint = showTapHint
        )
    }

    fun onShootTap() {
        val state = _gameplayState.value
        if (state.phase != GamePhase.PLAYING) return
        if (fireCooldownSec > 0f) return
        if (state.flyingPins.size >= state.pinsRemainingInLevel) return

        fireCooldownSec = SHOT_COOLDOWN_SEC
        val pin = FlyingPin(id = nextPinId++, progress = 0f)
        _gameplayState.update {
            it.copy(
                flyingPins = it.flyingPins + pin,
                showTapHint = false
            )
        }
        emitEffect(GameEffect.PlayShootSound)
    }

    fun pauseGame() {
        _gameplayState.update { state ->
            if (state.phase == GamePhase.PLAYING) state.copy(phase = GamePhase.PAUSED) else state
        }
    }

    fun resumeGame() {
        _gameplayState.update { state ->
            if (state.phase == GamePhase.PAUSED) state.copy(phase = GamePhase.PLAYING) else state
        }
    }

    fun onFrame(rawDeltaSec: Float) {
        val dt = rawDeltaSec.coerceIn(0f, MAX_DELTA_SEC)
        if (dt <= 0f) return

        val state = _gameplayState.value
        when (state.phase) {
            GamePhase.PLAYING -> tickPlaying(state, dt)
            GamePhase.LEVEL_TRANSITION -> tickLevelTransition(state, dt)
            GamePhase.COLLISION_BEAT -> tickCollisionBeat(state, dt)
            GamePhase.PAUSED, GamePhase.GAME_OVER -> Unit
        }
    }

    private fun tickPlaying(state: GameplayUiState, dt: Float) {
        elapsedLevelSec += dt
        if (fireCooldownSec > 0f) {
            fireCooldownSec = (fireCooldownSec - dt).coerceAtLeast(0f)
        }

        var flipFlash = (state.directionFlipFlash - dt * 2.8f).coerceAtLeast(0f)

        // Random direction reversal & speed variation on higher levels
        if (currentLevelSpec.allowDirectionReversal) {
            reversalCountdownSec -= dt
            if (reversalCountdownSec <= 0f) {
                if (Random.nextFloat() < 0.78f) {
                    targetDirection = -targetDirection
                    flipFlash = 1f
                }
                speedBoostMultiplier = if (currentLevelSpec.levelNumber >= 4) {
                    0.85f + Random.nextFloat() * 0.45f
                } else {
                    1f
                }
                reversalCountdownSec = currentLevelSpec.reversalIntervalSec *
                    (0.65f + Random.nextFloat() * 0.7f)
            }
        }

        val waveMod = if (currentLevelSpec.allowSpeedVariation) {
            1f + currentLevelSpec.variationAmplitude * sin(elapsedLevelSec * 1.9f)
        } else {
            1f
        }

        val desiredVelocity =
            currentLevelSpec.baseSpeedRadPerSec * waveMod * speedBoostMultiplier * targetDirection
        // Smooth velocity interpolation so reversals feel fluid & physical
        val lerpAlpha = (dt * 9.5f).coerceAtMost(1f)
        currentAngularVelocity += (desiredVelocity - currentAngularVelocity) * lerpAlpha

        val nextWheelAngle = LevelGenerator.normalizeAngle(
            state.wheelAngleRad + currentAngularVelocity * dt
        )

        // Advance flying pins & check impacts
        val updatedAttached = state.attachedPins.toMutableList()
        val remainingFlying = mutableListOf<FlyingPin>()
        val updatedRings = state.pulseRings.mapNotNull { ring ->
            val nextProgress = ring.progress + dt * 2.4f
            if (nextProgress >= 1f) null else ring.copy(progress = nextProgress)
        }.toMutableList()

        var nextScore = state.score
        var nextPinsRemaining = state.pinsRemainingInLevel
        var nextRecoil = (state.wheelRecoil - dt * 6.5f).coerceAtLeast(0f)
        var collidedPair: Pair<Float, Float>? = null

        for (pin in state.flyingPins) {
            val nextProgress = pin.progress + PIN_TRAVEL_SPEED * dt
            if (nextProgress >= 1f) {
                val impactAngle = LevelGenerator.normalizeAngle(HALF_PI - nextWheelAngle)
                val existingCollision = LevelGenerator.findCollidingAngle(
                    candidateAngle = impactAngle,
                    existingAngles = updatedAttached.map { it.angleRad }
                )

                if (existingCollision != null) {
                    collidedPair = impactAngle to existingCollision
                    break
                } else {
                    updatedAttached.add(AttachedPin(angleRad = impactAngle, isObstacle = false))
                    nextScore += 1
                    nextPinsRemaining = (nextPinsRemaining - 1).coerceAtLeast(0)
                    nextRecoil = 1f
                    updatedRings.add(PulseRing(id = nextRingId++, progress = 0f))
                    emitEffect(GameEffect.PlayStickSound)
                    emitEffect(GameEffect.TriggerTickHaptic)
                }
            } else {
                remainingFlying.add(pin.copy(progress = nextProgress))
            }
        }

        if (collidedPair != null) {
            emitEffect(GameEffect.PlayCollisionSound)
            emitEffect(GameEffect.TriggerCollisionHaptic)
            phaseTimerSec = COLLISION_BEAT_DURATION_SEC
            _gameplayState.update {
                it.copy(
                    phase = GamePhase.COLLISION_BEAT,
                    wheelAngleRad = nextWheelAngle,
                    attachedPins = updatedAttached,
                    flyingPins = emptyList(),
                    pulseRings = updatedRings,
                    screenShake = 1f,
                    collisionAngles = collidedPair,
                    directionFlipFlash = flipFlash
                )
            }
            return
        }

        // Check level completion
        if (nextPinsRemaining <= 0) {
            emitEffect(GameEffect.PlayLevelClearSound)
            emitEffect(GameEffect.TriggerTickHaptic)
            if (state.mode == GameMode.DAILY) {
                _gameplayState.update {
                    it.copy(
                        score = nextScore,
                        pinsRemainingInLevel = 0,
                        wheelAngleRad = nextWheelAngle,
                        attachedPins = updatedAttached,
                        flyingPins = emptyList(),
                        pulseRings = updatedRings,
                        wheelRecoil = 1f,
                        isDailyVictory = true
                    )
                }
                finalizeGameOver(
                    finalScore = nextScore,
                    levelReached = state.level,
                    mode = GameMode.DAILY,
                    dailyCleared = true
                )
                return
            } else {
                phaseTimerSec = LEVEL_TRANSITION_DURATION_SEC
                _gameplayState.update {
                    it.copy(
                        phase = GamePhase.LEVEL_TRANSITION,
                        score = nextScore,
                        pinsRemainingInLevel = 0,
                        wheelAngleRad = nextWheelAngle,
                        attachedPins = updatedAttached,
                        flyingPins = emptyList(),
                        pulseRings = updatedRings,
                        wheelRecoil = 1f
                    )
                }
                return
            }
        }

        _gameplayState.update {
            it.copy(
                score = nextScore,
                pinsRemainingInLevel = nextPinsRemaining,
                wheelAngleRad = nextWheelAngle,
                currentSpeedRadPerSec = abs(currentAngularVelocity),
                rotationDirection = targetDirection,
                attachedPins = updatedAttached,
                flyingPins = remainingFlying,
                pulseRings = updatedRings,
                wheelRecoil = nextRecoil,
                directionFlipFlash = flipFlash
            )
        }
    }

    private fun tickLevelTransition(state: GameplayUiState, dt: Float) {
        phaseTimerSec -= dt
        val nextWheelAngle = LevelGenerator.normalizeAngle(
            state.wheelAngleRad + currentAngularVelocity * 0.45f * dt
        )
        val updatedRings = state.pulseRings.mapNotNull { ring ->
            val nextProgress = ring.progress + dt * 2.0f
            if (nextProgress >= 1f) null else ring.copy(progress = nextProgress)
        }
        val nextRecoil = (state.wheelRecoil - dt * 4f).coerceAtLeast(0f)

        if (phaseTimerSec <= 0f) {
            val nextLevelNum = state.level + 1
            val nextSpec = LevelGenerator.classicLevel(nextLevelNum)
            setupLevel(
                spec = nextSpec,
                mode = state.mode,
                score = state.score,
                bestSnapshot = state.bestScoreSnapshot,
                showTapHint = false
            )
        } else {
            _gameplayState.update {
                it.copy(
                    wheelAngleRad = nextWheelAngle,
                    pulseRings = updatedRings,
                    wheelRecoil = nextRecoil
                )
            }
        }
    }

    private fun tickCollisionBeat(state: GameplayUiState, dt: Float) {
        phaseTimerSec -= dt
        val nextShake = (state.screenShake - dt * 2.6f).coerceAtLeast(0f)
        if (phaseTimerSec <= 0f) {
            finalizeGameOver(
                finalScore = state.score,
                levelReached = state.level,
                mode = state.mode,
                dailyCleared = false
            )
        } else {
            _gameplayState.update { it.copy(screenShake = nextShake) }
        }
    }

    private fun finalizeGameOver(
        finalScore: Int,
        levelReached: Int,
        mode: GameMode,
        dailyCleared: Boolean
    ) {
        if (hasRecordedCurrentRun) return
        hasRecordedCurrentRun = true

        val prevBest = playerStats.value.bestScore
        val isNewClassicBest = mode == GameMode.CLASSIC && finalScore > prevBest && finalScore > 0
        val displayBest = if (mode == GameMode.DAILY) {
            maxOf(playerStats.value.dailyBestScore, finalScore)
        } else {
            maxOf(prevBest, finalScore)
        }

        _gameplayState.update {
            it.copy(
                phase = GamePhase.GAME_OVER,
                screenShake = 0f,
                bestScoreSnapshot = displayBest,
                isNewBest = isNewClassicBest,
                isDailyVictory = dailyCleared
            )
        }

        viewModelScope.launch {
            val unlocked = repository.recordRunResult(
                score = finalScore,
                levelReached = levelReached,
                mode = mode,
                dailyCleared = dailyCleared
            )
            if (unlocked.isNotEmpty()) {
                _gameplayState.update { it.copy(newlyUnlockedStyles = unlocked) }
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun selectPinStyle(style: PinStyle) {
        if (style in playerStats.value.unlockedStyles) {
            viewModelScope.launch { repository.selectPinStyle(style) }
        }
    }

    fun resetAllStats() {
        viewModelScope.launch { repository.resetAllStats() }
    }

    private fun emitEffect(effect: GameEffect) {
        _effects.tryEmit(effect)
    }

    companion object {
        private const val HALF_PI = (PI / 2.0).toFloat()
        private const val MAX_DELTA_SEC = 0.05f
        private const val PIN_TRAVEL_SPEED = 5.8f
        private const val SHOT_COOLDOWN_SEC = 0.12f
        private const val COLLISION_BEAT_DURATION_SEC = 0.48f
        private const val LEVEL_TRANSITION_DURATION_SEC = 0.78f
    }
}
