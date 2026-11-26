package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameMode
import com.example.model.PlayerStats
import com.example.ui.components.MonochromeTopBar
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhiteHairline
import com.example.ui.theme.WhiteMuted
import com.example.ui.theme.WhiteSurface

@Composable
fun LeaderboardScreen(
    stats: PlayerStats,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("leaderboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            MonochromeTopBar(
                title = stringResource(R.string.leaderboard_title),
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Summary Stat Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = stringResource(R.string.leaderboard_classic_best),
                    value = stats.bestScore.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.leaderboard_daily_best),
                    value = stats.dailyBestScore.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.leaderboard_daily_streak),
                    value = "${stats.dailyStreak}D",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.leaderboard_top_runs),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.5.sp),
                color = WhiteMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (stats.topRuns.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WhiteSurface,
                    border = BorderStroke(1.dp, WhiteHairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = WhiteMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WhiteMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(stats.topRuns) { index, run ->
                        val isTop = index == 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTop) PureWhite else WhiteSurface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isTop) PureWhite else WhiteHairline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isTop) PureBlack else WhiteMuted
                                    )
                                    Column {
                                        Text(
                                            text = if (run.mode == GameMode.DAILY) "DAILY" else "CLASSIC",
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                            color = if (isTop) PureBlack else PureWhite
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.leaderboard_run_meta,
                                                run.level,
                                                run.date
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isTop) PureBlack.copy(alpha = 0.7f) else WhiteMuted
                                        )
                                    }
                                }

                                Text(
                                    text = run.score.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isTop) PureBlack else PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = PureBlack,
        border = BorderStroke(1.2.dp, PureWhite),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = PureWhite
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = WhiteMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
