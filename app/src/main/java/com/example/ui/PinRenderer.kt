package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.PinStyle
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared pure black-and-white Canvas drawing helpers for pins across Gameplay, Main Menu,
 * and the Pin Styles showcase.
 */
fun DrawScope.drawPinFromCenter(
    center: Offset,
    worldAngleRad: Float,
    innerRadius: Float,
    outerRadius: Float,
    headRadius: Float,
    stemWidth: Float,
    style: PinStyle,
    color: Color = PureWhite,
    alpha: Float = 1f
) {
    val cosA = cos(worldAngleRad)
    val sinA = sin(worldAngleRad)

    val start = Offset(
        x = center.x + cosA * innerRadius,
        y = center.y + sinA * innerRadius
    )
    val headCenter = Offset(
        x = center.x + cosA * outerRadius,
        y = center.y + sinA * outerRadius
    )

    drawLine(
        color = color.copy(alpha = alpha),
        start = start,
        end = headCenter,
        strokeWidth = stemWidth,
        cap = StrokeCap.Round
    )

    drawPinHead(
        center = headCenter,
        angleRad = worldAngleRad,
        headRadius = headRadius,
        style = style,
        color = color,
        alpha = alpha
    )
}

fun DrawScope.drawPinHead(
    center: Offset,
    angleRad: Float,
    headRadius: Float,
    style: PinStyle,
    color: Color = PureWhite,
    alpha: Float = 1f
) {
    val c = color.copy(alpha = alpha)
    when (style) {
        PinStyle.CLASSIC_DOT -> {
            drawCircle(
                color = c,
                radius = headRadius,
                center = center
            )
        }

        PinStyle.HOLLOW_RING -> {
            drawCircle(
                color = PureBlack,
                radius = headRadius,
                center = center
            )
            drawCircle(
                color = c,
                radius = headRadius,
                center = center,
                style = Stroke(width = (headRadius * 0.34f).coerceAtLeast(2f))
            )
        }

        PinStyle.DIAMOND -> {
            val deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 45f
            rotate(degrees = deg, pivot = center) {
                val side = headRadius * 1.55f
                drawRect(
                    color = c,
                    topLeft = Offset(center.x - side / 2f, center.y - side / 2f),
                    size = Size(side, side)
                )
            }
        }

        PinStyle.SPEARHEAD -> {
            val deg = Math.toDegrees(angleRad.toDouble()).toFloat()
            rotate(degrees = deg, pivot = center) {
                val path = Path().apply {
                    moveTo(center.x + headRadius * 1.2f, center.y)
                    lineTo(center.x - headRadius * 0.9f, center.y - headRadius * 0.95f)
                    lineTo(center.x - headRadius * 0.45f, center.y)
                    lineTo(center.x - headRadius * 0.9f, center.y + headRadius * 0.95f)
                    close()
                }
                drawPath(path = path, color = c)
            }
        }

        PinStyle.CROSSHAIR -> {
            drawCircle(
                color = PureBlack,
                radius = headRadius,
                center = center
            )
            val stroke = (headRadius * 0.28f).coerceAtLeast(1.8f)
            drawCircle(
                color = c,
                radius = headRadius,
                center = center,
                style = Stroke(width = stroke)
            )
            drawLine(
                color = c,
                start = Offset(center.x - headRadius * 0.65f, center.y),
                end = Offset(center.x + headRadius * 0.65f, center.y),
                strokeWidth = stroke
            )
            drawLine(
                color = c,
                start = Offset(center.x, center.y - headRadius * 0.65f),
                end = Offset(center.x, center.y + headRadius * 0.65f),
                strokeWidth = stroke
            )
        }

        PinStyle.ORBITAL -> {
            drawCircle(
                color = PureBlack,
                radius = headRadius,
                center = center
            )
            drawCircle(
                color = c,
                radius = headRadius,
                center = center,
                style = Stroke(width = (headRadius * 0.25f).coerceAtLeast(1.8f))
            )
            drawCircle(
                color = c,
                radius = headRadius * 0.44f,
                center = center
            )
        }
    }
}
