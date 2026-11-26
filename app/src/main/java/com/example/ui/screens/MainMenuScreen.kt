package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PinDropPreferencesRepository
import com.example.model.PinStyle
import com.example.model.PlayerStats
import com.example.ui.components.MonochromeOutlinedButton
import com.example.ui.components.MonochromePrimaryButton
import com.example.ui.drawPinFromCenter
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhiteHairline
import com.example.ui.theme.WhiteMuted
import com.example.ui.theme.WhiteSurface
import kotlin.math.PI

@Composable
fun MainMenuScreen(
    stats: PlayerStats,
    onPlayClick: () -> Unit,
    onDailyChallengeClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onPinStylesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val todayKey = PinDropPreferencesRepository.currentDateKey()
    val dailyDone = stats.isDailyCompletedToday(todayKey)

    val infiniteTransition = rememberInfiniteTransition(label = "menu_wheel")
    val previewAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "menu_wheel_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("main_menu_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Branding Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.brand_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = WhiteMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live animated minimal rotating wheel emblem
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val wheelRadius = size.minDimension * 0.21f
                        val outerRadius = size.minDimension * 0.44f
                        val headRadius = size.minDimension * 0.038f
                        val stemWidth = 2.dp.toPx()

                        drawCircle(
                            color = PureWhite,
                            radius = wheelRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        val pinOffsets = listOf(0f, 1.05f, 2.2f, 3.35f, 4.7f)
                        pinOffsets.forEach { offset ->
                            drawPinFromCenter(
                                center = center,
                                worldAngleRad = previewAngle + offset,
                                innerRadius = wheelRadius,
                                outerRadius = outerRadius,
                                headRadius = headRadius,
                                stemWidth = stemWidth,
                                style = stats.selectedStyle
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.game_title),
                    style = MaterialTheme.typography.displayLarge.copy(
                        letterSpacing = 4.sp
                    ),
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.game_tagline),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        letterSpacing = 0.8.sp
                    ),
                    color = WhiteMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Primary Action Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonochromePrimaryButton(
                    text = stringResource(R.string.menu_play),
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClick,
                    testTag = "play_button"
                )

                MonochromeOutlinedButton(
                    text = if (dailyDone) {
                        stringResource(R.string.menu_daily_completed)
                    } else {
                        stringResource(R.string.menu_daily_challenge)
                    },
                    icon = if (dailyDone) Icons.Default.Check else Icons.Default.Bolt,
                    onClick = onDailyChallengeClick,
                    testTag = "daily_challenge_button"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonochromeOutlinedButton(
                        text = stringResource(R.string.menu_leaderboard),
                        icon = Icons.Default.Leaderboard,
                        onClick = onLeaderboardClick,
                        modifier = Modifier.weight(1f),
                        testTag = "leaderboard_button"
                    )
                    MonochromeOutlinedButton(
                        text = stringResource(R.string.menu_settings),
                        icon = Icons.Default.Settings,
                        onClick = onSettingsClick,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_button"
                    )
                }

                MonochromeOutlinedButton(
                    text = "${stringResource(R.string.menu_pin_styles)} (${stats.unlockedStyles.size}/${PinStyle.entries.size})",
                    icon = Icons.Default.Style,
                    onClick = onPinStylesClick,
                    testTag = "pin_styles_button"
                )
            }

            // Bottom Best Score Anchor
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WhiteSurface,
                border = BorderStroke(1.dp, WhiteHairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.menu_best_score, stats.bestScore),
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        modifier = Modifier.testTag("menu_best_score_text")
                    )
                    Text(
                        text = "LVL ${stats.highestLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhiteMuted
                    )
                }
            }
        }
    }
}
