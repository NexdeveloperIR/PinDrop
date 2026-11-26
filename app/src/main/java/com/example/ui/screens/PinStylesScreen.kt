package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PinStyle
import com.example.model.PlayerStats
import com.example.ui.components.MonochromeTopBar
import com.example.ui.drawPinHead
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WhiteHairline
import com.example.ui.theme.WhiteMuted
import com.example.ui.theme.WhiteSubtle
import com.example.ui.theme.WhiteSurface
import kotlin.math.PI

@Composable
fun PinStylesScreen(
    stats: PlayerStats,
    onSelectStyle: (PinStyle) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("pin_styles_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            MonochromeTopBar(
                title = stringResource(R.string.styles_title),
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.styles_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(PinStyle.entries) { style ->
                    val isUnlocked = style in stats.unlockedStyles
                    val isSelected = style == stats.selectedStyle

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PureWhite else WhiteSurface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> PureWhite
                                isUnlocked -> WhiteSubtle
                                else -> WhiteHairline
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isUnlocked) { onSelectStyle(style) }
                            .testTag("pin_style_${style.id}")
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
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Live Canvas Preview of the Pin Style
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = PureBlack,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cx = size.width / 2f
                                        val topY = size.height * 0.2f
                                        val headY = size.height * 0.68f
                                        drawLine(
                                            color = if (isUnlocked) PureWhite else WhiteSubtle,
                                            start = Offset(cx, topY),
                                            end = Offset(cx, headY),
                                            strokeWidth = 2.2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawPinHead(
                                            center = Offset(cx, headY),
                                            angleRad = (-PI / 2.0).toFloat(),
                                            headRadius = 7.dp.toPx(),
                                            style = style,
                                            color = if (isUnlocked) PureWhite else WhiteSubtle
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = if (isSelected) PureBlack else PureWhite
                                    )
                                    Text(
                                        text = style.unlockRequirement,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = if (isSelected) {
                                            PureBlack.copy(alpha = 0.72f)
                                        } else {
                                            WhiteMuted
                                        }
                                    )
                                }
                            }

                            when {
                                isSelected -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PureBlack,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.styles_equipped),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PureBlack
                                        )
                                    }
                                }
                                isUnlocked -> {
                                    Text(
                                        text = stringResource(R.string.styles_select),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = stringResource(R.string.styles_locked),
                                        tint = WhiteSubtle,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
