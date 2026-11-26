package com.example.game

import com.example.model.LevelSpec
import kotlin.math.PI
import kotlin.math.abs
import kotlin.random.Random

object LevelGenerator {
    const val TWO_PI = (2.0 * PI).toFloat()
    const val MIN_SAFE_ANGLE_RAD = 0.225f // ~12.9 degrees clearance between pin heads
    const val BASE_SPEED_STAGE_1 = 1.45f

    fun classicLevel(level: Int, random: Random = Random.Default): LevelSpec {
        val targetPins = when {
            level == 1 -> 6
            level == 2 -> 8
            level == 3 -> 10
            level <= 6 -> 10 + (level - 3) * 2
            else -> (16 + (level - 6)).coerceAtMost(20)
        }

        val obstacleCount = when {
            level == 1 -> 1
            level == 2 -> 2
            level == 3 -> 3
            level <= 6 -> 4
            else -> 5
        }

        val obstacles = generateSpacedObstacles(
            count = obstacleCount,
            minSpacing = 0.7f,
            random = random
        )

        val baseSpeed = if (level <= 10) {
            BASE_SPEED_STAGE_1 + (level - 1) * 0.22f
        } else {
            (BASE_SPEED_STAGE_1 + 9 * 0.22f) + (level - 10) * 0.08f
        }.coerceAtMost(4.8f)

        val initialDirection = if (level % 2 == 1) 1f else -1f

        return LevelSpec(
            levelNumber = level,
            targetPins = targetPins,
            initialPinAngles = obstacles,
            baseSpeedRadPerSec = baseSpeed,
            initialDirection = initialDirection,
            allowDirectionReversal = level >= 3,
            allowSpeedVariation = level >= 2,
            reversalIntervalSec = (4.2f - level * 0.2f).coerceIn(1.6f, 4.0f),
            variationAmplitude = (0.18f + level * 0.06f).coerceAtMost(0.65f)
        )
    }

    fun dailyLevel(dateKey: String): LevelSpec {
        val seed = dateKey.fold(1125899906842597L) { acc, ch -> acc * 31L + ch.code }
        val seededRandom = Random(seed)
        val obstacleCount = 4 + seededRandom.nextInt(2) // 4 or 5 initial obstacles
        val obstacles = generateSpacedObstacles(
            count = obstacleCount,
            minSpacing = 0.68f,
            random = seededRandom
        )

        return LevelSpec(
            levelNumber = 1,
            targetPins = 18,
            initialPinAngles = obstacles,
            baseSpeedRadPerSec = 2.15f + seededRandom.nextFloat() * 0.45f,
            initialDirection = if (seededRandom.nextBoolean()) 1f else -1f,
            allowDirectionReversal = true,
            allowSpeedVariation = true,
            reversalIntervalSec = 2.4f + seededRandom.nextFloat() * 1.1f,
            variationAmplitude = 0.45f
        )
    }

    private fun generateSpacedObstacles(
        count: Int,
        minSpacing: Float,
        random: Random
    ): List<Float> {
        val result = mutableListOf<Float>()
        val baseOffset = random.nextFloat() * TWO_PI
        val idealStep = TWO_PI / count.coerceAtLeast(1)
        for (i in 0 until count) {
            val jitter = (random.nextFloat() - 0.5f) * (idealStep * 0.35f)
            val candidate = normalizeAngle(baseOffset + i * idealStep + jitter)
            if (result.all { angularDistance(it, candidate) >= minSpacing }) {
                result.add(candidate)
            } else {
                result.add(normalizeAngle(baseOffset + i * idealStep))
            }
        }
        return result
    }

    fun normalizeAngle(angleRad: Float): Float {
        var normalized = angleRad % TWO_PI
        if (normalized < 0f) normalized += TWO_PI
        return normalized
    }

    fun angularDistance(a: Float, b: Float): Float {
        val diff = abs(normalizeAngle(a) - normalizeAngle(b))
        return if (diff > PI.toFloat()) TWO_PI - diff else diff
    }

    fun findCollidingAngle(
        candidateAngle: Float,
        existingAngles: List<Float>,
        minSafeAngle: Float = MIN_SAFE_ANGLE_RAD
    ): Float? {
        return existingAngles.firstOrNull { existing ->
            angularDistance(candidateAngle, existing) < minSafeAngle
        }
    }
}
