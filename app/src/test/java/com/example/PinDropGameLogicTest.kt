package com.example

import com.example.game.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PinDropGameLogicTest {

    @Test
    fun `angularDistance handles wraparound across 0 and 2pi`() {
        val nearZero = 0.05f
        val nearTwoPi = (2.0 * PI).toFloat() - 0.05f
        val dist = LevelGenerator.angularDistance(nearZero, nearTwoPi)
        assertEquals(0.10f, dist, 0.001f)
    }

    @Test
    fun `findCollidingAngle detects pin collision within threshold and allows safe gap`() {
        val existing = listOf(0.0f, 1.5f, 3.2f)
        assertNotNull(LevelGenerator.findCollidingAngle(0.10f, existing))
        assertNotNull(LevelGenerator.findCollidingAngle(LevelGenerator.TWO_PI - 0.10f, existing))
        assertNull(LevelGenerator.findCollidingAngle(0.60f, existing))
    }

    @Test
    fun `classic levels scale in difficulty and strictly increase rotation speed`() {
        val lvl1 = LevelGenerator.classicLevel(1)
        val lvl5 = LevelGenerator.classicLevel(5)
        assertTrue(lvl5.targetPins > lvl1.targetPins)
        assertTrue(lvl5.baseSpeedRadPerSec > lvl1.baseSpeedRadPerSec)
        assertTrue(lvl5.allowDirectionReversal)

        for (stage in 1..14) {
            val current = LevelGenerator.classicLevel(stage)
            val next = LevelGenerator.classicLevel(stage + 1)
            assertTrue(
                "Stage ${stage + 1} speed should exceed Stage $stage",
                next.baseSpeedRadPerSec > current.baseSpeedRadPerSec
            )
        }
    }

    @Test
    fun `daily challenge is deterministic for a given date`() {
        val dayA1 = LevelGenerator.dailyLevel("2026-11-26")
        val dayA2 = LevelGenerator.dailyLevel("2026-11-26")
        assertEquals(dayA1.initialPinAngles, dayA2.initialPinAngles)
        assertEquals(dayA1.baseSpeedRadPerSec, dayA2.baseSpeedRadPerSec, 0.0001f)
        assertEquals(dayA1.initialDirection, dayA2.initialDirection, 0.0001f)
    }
}
