package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundManager
import com.example.model.GameEffect
import com.example.model.ScreenRoute
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.PinStylesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.PinDropTheme
import com.example.viewmodel.PinDropViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        setContent {
            PinDropTheme {
                PinDropApp()
            }
        }
    }
}

@Composable
fun PinDropApp(
    viewModel: PinDropViewModel = viewModel()
) {
    val route by viewModel.currentRoute.collectAsStateWithLifecycle()
    val stats by viewModel.playerStats.collectAsStateWithLifecycle()
    val gameplayState by viewModel.gameplayState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val soundManager = remember(context) { SoundManager(context.applicationContext) }

    LaunchedEffect(stats.soundEnabled) {
        soundManager.isSoundEnabled = stats.soundEnabled
    }

    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    LaunchedEffect(viewModel, soundManager, view) {
        viewModel.effects.collect { effect ->
            val currentStats = viewModel.playerStats.value
            when (effect) {
                GameEffect.PlayShootSound -> {
                    soundManager.playClick()
                }
                GameEffect.PlayStickSound -> {
                    soundManager.playStick()
                }
                GameEffect.PlayLevelClearSound -> {
                    soundManager.playLevelClear()
                }
                GameEffect.PlayCollisionSound -> {
                    soundManager.playCollision()
                }
                GameEffect.TriggerTickHaptic -> {
                    if (currentStats.hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
                GameEffect.TriggerCollisionHaptic -> {
                    if (currentStats.hapticsEnabled) {
                        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.REJECT
                        } else {
                            HapticFeedbackConstants.LONG_PRESS
                        }
                        view.performHapticFeedback(constant)
                    }
                }
            }
        }
    }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "screen_transition"
    ) { currentScreen ->
        when (currentScreen) {
            is ScreenRoute.Splash -> {
                SplashScreen(onFinished = viewModel::onSplashComplete)
            }

            is ScreenRoute.MainMenu -> {
                MainMenuScreen(
                    stats = stats,
                    onPlayClick = viewModel::startClassicGame,
                    onDailyChallengeClick = viewModel::startDailyChallenge,
                    onLeaderboardClick = { viewModel.navigateTo(ScreenRoute.Leaderboard) },
                    onPinStylesClick = { viewModel.navigateTo(ScreenRoute.PinStyles) },
                    onSettingsClick = { viewModel.navigateTo(ScreenRoute.Settings) }
                )
            }

            is ScreenRoute.Gameplay -> {
                GameplayScreen(
                    state = gameplayState,
                    pinStyle = stats.selectedStyle,
                    onFrame = viewModel::onFrame,
                    onShootTap = viewModel::onShootTap,
                    onPauseClick = viewModel::pauseGame,
                    onResumeClick = viewModel::resumeGame,
                    onRestartClick = viewModel::restartCurrentGame,
                    onHomeClick = viewModel::navigateHome
                )
            }

            is ScreenRoute.Leaderboard -> {
                LeaderboardScreen(
                    stats = stats,
                    onBack = viewModel::navigateHome
                )
            }

            is ScreenRoute.PinStyles -> {
                PinStylesScreen(
                    stats = stats,
                    onSelectStyle = viewModel::selectPinStyle,
                    onBack = viewModel::navigateHome
                )
            }

            is ScreenRoute.Settings -> {
                SettingsScreen(
                    stats = stats,
                    onToggleSound = { enabled ->
                        viewModel.setSoundEnabled(enabled)
                        soundManager.isSoundEnabled = enabled
                        if (enabled) soundManager.playClick()
                    },
                    onToggleHaptics = viewModel::setHapticsEnabled,
                    onResetStats = viewModel::resetAllStats,
                    onBack = viewModel::navigateHome
                )
            }
        }
    }
}
