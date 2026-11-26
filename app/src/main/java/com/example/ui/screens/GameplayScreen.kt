package com.example.ui.screens

import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.GameMode
import com.example.model.GamePhase
import com.example.model.GameplayUiState
import com.example.model.PinStyle
import com.example.ui.components.MonochromeOutlinedButton
import com.example.ui.components.MonochromePrimaryButton
import com.example.ui.drawPinFromCenter
import com.example.ui.drawPinHead
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhiteHairline
import com.example.ui.theme.WhiteMuted
import com.example.ui.theme.WhiteSubtle
import com.example.ui.theme.WhiteSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameplayScreen(
    state: GameplayUiState,
    pinStyle: PinStyle,
    onFrame: (Float) -> Unit,
    onShootTap: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onPauseClick()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        when (state.phase) {
            GamePhase.PLAYING -> onPauseClick()
            GamePhase.PAUSED -> onResumeClick()
            GamePhase.GAME_OVER -> onHomeClick()
            GamePhase.LEVEL_TRANSITION, GamePhase.COLLISION_BEAT -> Unit
        }
    }

    // 60 FPS frame-driven game loop
    val isLoopActive = state.phase == GamePhase.PLAYING ||
        state.phase == GamePhase.LEVEL_TRANSITION ||
        state.phase == GamePhase.COLLISION_BEAT

    LaunchedEffect(isLoopActive) {
        if (!isLoopActive) return@LaunchedEffect
        var lastFrameNs = 0L
        while (true) {
            withFrameNanos { frameTimeNs ->
                if (lastFrameNs != 0L) {
                    val deltaSec = (frameTimeNs - lastFrameNs) / 1_000_000_000f
                    onFrame(deltaSec)
                }
                lastFrameNs = frameTimeNs
            }
        }
    }

    val context = LocalContext.current
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("gameplay_screen")
    ) {
        // Playfield Canvas with instant touch-down firing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.phase) {
                    if (state.phase != GamePhase.PLAYING) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onShootTap()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minDim = min(size.width, size.height)
                val shakeMagnitude = state.screenShake * 14f
                val shakeOffset = if (shakeMagnitude > 0.2f) {
                    Offset(
                        x = sin(state.screenShake * 47f) * shakeMagnitude,
                        y = cos(state.screenShake * 63f) * shakeMagnitude
                    )
                } else {
                    Offset.Zero
                }

                val baseCenter = Offset(
                    x = size.width / 2f + shakeOffset.x,
                    y = size.height * 0.41f + shakeOffset.y
                )

                val recoilScale = 1f + state.wheelRecoil * 0.045f
                val wheelRadius = (minDim * 0.142f).coerceIn(48f, 145f) * recoilScale
                val pinLength = (minDim * 0.165f).coerceIn(58f, 175f)
                val outerRadius = wheelRadius + pinLength
                val headRadius = (minDim * 0.026f).coerceIn(8.5f, 22f)
                val stemWidth = (minDim * 0.0055f).coerceIn(2.5f, 5.5f)

                val launchY = (size.height * 0.83f)
                    .coerceAtLeast(baseCenter.y + outerRadius + headRadius * 3.8f)
                    .coerceAtMost(size.height - 72f)
                val impactHeadY = baseCenter.y + outerRadius

                // Subtle expanding pulse rings on successful stick
                state.pulseRings.forEach { ring ->
                    val ringRadius = wheelRadius + pinLength * (0.2f + ring.progress * 1.15f)
                    val ringAlpha = (1f - ring.progress) * 0.42f
                    drawCircle(
                        color = PureWhite.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = baseCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Subtle direction flip indicator ring
                if (state.directionFlipFlash > 0.02f) {
                    drawCircle(
                        color = PureWhite.copy(alpha = state.directionFlipFlash * 0.35f),
                        radius = wheelRadius * (1.28f + (1f - state.directionFlipFlash) * 0.3f),
                        center = baseCenter,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Attached pins rotating with the wheel
                state.attachedPins.forEach { pin ->
                    val worldAngle = state.wheelAngleRad + pin.angleRad
                    drawPinFromCenter(
                        center = baseCenter,
                        worldAngleRad = worldAngle,
                        innerRadius = wheelRadius,
                        outerRadius = outerRadius,
                        headRadius = headRadius,
                        stemWidth = stemWidth,
                        style = if (pin.isObstacle) PinStyle.CLASSIC_DOT else pinStyle
                    )
                }

                // Main rotating wheel outline + inner dot
                drawCircle(
                    color = PureWhite,
                    radius = wheelRadius,
                    center = baseCenter,
                    style = Stroke(width = 3.5.dp.toPx())
                )
                drawCircle(
                    color = PureWhite.copy(alpha = 0.22f),
                    radius = wheelRadius * 0.78f,
                    center = baseCenter,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Collision highlight rings during collision beat
                state.collisionAngles?.let { (angleA, angleB) ->
                    listOf(angleA, angleB).forEach { localAngle ->
                        val worldA = state.wheelAngleRad + localAngle
                        val hitCenter = Offset(
                            x = baseCenter.x + cos(worldA) * outerRadius,
                            y = baseCenter.y + sin(worldA) * outerRadius
                        )
                        drawCircle(
                            color = PureWhite,
                            radius = headRadius * 2.1f,
                            center = hitCenter,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }

                // Flying pins moving upward toward the wheel
                val upAngle = (-PI / 2.0).toFloat()
                state.flyingPins.forEach { flying ->
                    val headY = launchY + (impactHeadY - launchY) * flying.progress
                    val tipY = headY - pinLength
                    drawLine(
                        color = PureWhite,
                        start = Offset(baseCenter.x, tipY),
                        end = Offset(baseCenter.x, headY),
                        strokeWidth = stemWidth,
                        cap = StrokeCap.Round
                    )
                    drawPinHead(
                        center = Offset(baseCenter.x, headY),
                        angleRad = upAngle,
                        headRadius = headRadius,
                        style = pinStyle
                    )
                }

                // Ready pin at bottom launch position + queue preview dots
                val unlaunchedRemaining = (state.pinsRemainingInLevel - state.flyingPins.size)
                    .coerceAtLeast(0)
                if (unlaunchedRemaining > 0 && state.phase == GamePhase.PLAYING) {
                    val readyTipY = launchY - pinLength * 0.72f
                    drawLine(
                        color = PureWhite.copy(alpha = 0.85f),
                        start = Offset(baseCenter.x, readyTipY),
                        end = Offset(baseCenter.x, launchY),
                        strokeWidth = stemWidth,
                        cap = StrokeCap.Round
                    )
                    drawPinHead(
                        center = Offset(baseCenter.x, launchY),
                        angleRad = upAngle,
                        headRadius = headRadius,
                        style = pinStyle
                    )

                    val queuedDots = (unlaunchedRemaining - 1).coerceAtMost(4)
                    for (i in 1..queuedDots) {
                        val dotY = launchY + headRadius * 2.4f + i * (headRadius * 1.9f)
                        if (dotY < size.height - 16f) {
                            drawCircle(
                                color = PureWhite.copy(alpha = (0.75f - i * 0.14f).coerceAtLeast(0.2f)),
                                radius = headRadius * 0.42f,
                                center = Offset(baseCenter.x, dotY)
                            )
                        }
                    }
                }
            }

            // HUD + Center Stage Number Overlay
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val wheelCenterY = maxHeight * 0.41f
                val stageCenterLabel = if (state.mode == GameMode.DAILY) "D" else state.level.toString()

                // Stage number centered inside the rotating ring
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = wheelCenterY - 22.dp)
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stageCenterLabel,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PureWhite,
                        modifier = Modifier.testTag("wheel_stage_number")
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Stage / Speed / Progress badge
                        val stageProgress = if (state.targetPinsInLevel > 0) {
                            ((state.targetPinsInLevel - state.pinsRemainingInLevel).toFloat() /
                                state.targetPinsInLevel.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WhiteSurface,
                            border = BorderStroke(1.dp, WhiteHairline),
                            modifier = Modifier.testTag("gameplay_stage_badge")
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(146.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.mode == GameMode.DAILY) {
                                            stringResource(R.string.gameplay_daily_label)
                                        } else {
                                            stringResource(R.string.gameplay_level_label, state.level)
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PureWhite,
                                        modifier = Modifier.testTag("gameplay_stage_text")
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%.1fx",
                                            state.stageSpeedMultiplier
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WhiteMuted,
                                        modifier = Modifier.testTag("gameplay_speed_text")
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(WhiteHairline, RoundedCornerShape(2.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(stageProgress)
                                            .fillMaxHeight()
                                            .background(PureWhite, RoundedCornerShape(2.dp))
                                    )
                                }

                                Text(
                                    text = "${state.pinsRemainingInLevel} / ${state.targetPinsInLevel} LEFT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WhiteMuted
                                )
                            }
                        }

                        // Right: Pause button
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onPauseClick()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = stringResource(R.string.cd_pause_game),
                                tint = PureWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Top Center Score
                    Text(
                        text = state.score.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("gameplay_score_text")
                    )
                }

                // First-shot tap instruction hint
                AnimatedVisibility(
                    visible = state.showTapHint && state.phase == GamePhase.PLAYING,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.gameplay_tap_hint),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.5.sp),
                        color = WhiteMuted,
                        textAlign = TextAlign.Center
                    )
                }

                // Level Clear minimal banner
                AnimatedVisibility(
                    visible = state.phase == GamePhase.LEVEL_TRANSITION,
                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.gameplay_level_clear, state.level),
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.5.sp),
                            color = PureBlack,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // Pause Overlay
        AnimatedVisibility(
            visible = state.phase == GamePhase.PAUSED,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = 0.90f))
                    .testTag("pause_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureBlack,
                    border = BorderStroke(1.5.dp, PureWhite),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.gameplay_pause_title),
                            style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 4.sp),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MonochromePrimaryButton(
                            text = stringResource(R.string.gameplay_resume),
                            icon = Icons.Default.PlayArrow,
                            onClick = onResumeClick,
                            testTag = "resume_button"
                        )
                        MonochromeOutlinedButton(
                            text = stringResource(R.string.gameplay_restart),
                            icon = Icons.Default.Refresh,
                            onClick = onRestartClick,
                            testTag = "pause_restart_button"
                        )
                        MonochromeOutlinedButton(
                            text = stringResource(R.string.gameplay_quit),
                            icon = Icons.Default.Home,
                            onClick = onHomeClick,
                            testTag = "pause_home_button"
                        )
                    }
                }
            }
        }

        // Game Over / Daily Complete Screen Overlay
        AnimatedVisibility(
            visible = state.phase == GamePhase.GAME_OVER,
            enter = fadeIn(tween(260)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260)),
            exit = fadeOut(tween(150))
        ) {
            GameOverOverlay(
                state = state,
                onTryAgain = onRestartClick,
                onHome = onHomeClick,
                onShareScore = {
                    val shareText = context.getString(R.string.share_score_message, state.score)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val chooser = Intent.createChooser(
                        sendIntent,
                        context.getString(R.string.share_score_chooser_title)
                    )
                    context.startActivity(chooser)
                }
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    state: GameplayUiState,
    onTryAgain: () -> Unit,
    onHome: () -> Unit,
    onShareScore: () -> Unit
) {
    val animatedScore = remember(state.score, state.phase) { Animatable(0f) }
    LaunchedEffect(state.score, state.phase) {
        if (state.phase == GamePhase.GAME_OVER) {
            animatedScore.snapTo(0f)
            animatedScore.animateTo(
                targetValue = state.score.toFloat(),
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.94f))
            .testTag("game_over_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (state.isDailyVictory) {
                    stringResource(R.string.daily_victory_title)
                } else {
                    stringResource(R.string.game_over_title)
                },
                style = MaterialTheme.typography.displayMedium.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = PureWhite,
                textAlign = TextAlign.Center
            )

            if (state.isNewBest) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PureWhite
                ) {
                    Text(
                        text = stringResource(R.string.game_over_new_best),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = PureBlack,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Score Summary Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureBlack,
                border = BorderStroke(1.5.dp, PureWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.game_over_your_score,
                            animatedScore.value.roundToInt()
                        ),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = PureWhite,
                        modifier = Modifier.testTag("game_over_your_score")
                    )
                    Text(
                        text = stringResource(
                            R.string.game_over_best_score,
                            state.bestScoreSnapshot
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = WhiteMuted,
                        modifier = Modifier.testTag("game_over_best_score")
                    )
                    if (state.mode == GameMode.CLASSIC) {
                        Text(
                            text = stringResource(R.string.game_over_stage_reached, state.level),
                            style = MaterialTheme.typography.labelLarge,
                            color = WhiteMuted,
                            modifier = Modifier.testTag("game_over_stage_reached")
                        )
                    }
                }
            }

            // Newly Unlocked Pin Style Banner
            state.newlyUnlockedStyles.forEach { unlocked ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhiteSurface,
                    border = BorderStroke(1.dp, WhiteSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.unlocked_style_banner, unlocked.displayName),
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            MonochromePrimaryButton(
                text = stringResource(R.string.game_over_try_again),
                icon = Icons.Default.Refresh,
                onClick = onTryAgain,
                testTag = "try_again_button"
            )

            MonochromeOutlinedButton(
                text = stringResource(R.string.game_over_share_score),
                icon = Icons.Default.Share,
                onClick = onShareScore,
                testTag = "share_score_button"
            )

            MonochromeOutlinedButton(
                text = stringResource(R.string.game_over_home),
                icon = Icons.Default.Home,
                onClick = onHome,
                testTag = "home_button"
            )
        }
    }
}
