package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GameMode
import com.example.model.PinStyle
import com.example.model.PlayerStats
import com.example.model.RunRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.pinDropDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pin_drop_prefs"
)

class PinDropPreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val BEST_SCORE = intPreferencesKey("best_score")
        val HIGHEST_LEVEL = intPreferencesKey("highest_level")
        val DAILY_BEST_SCORE = intPreferencesKey("daily_best_score")
        val DAILY_COMPLETED_DATE = stringPreferencesKey("daily_completed_date")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SELECTED_PIN_STYLE = stringPreferencesKey("selected_pin_style")
        val UNLOCKED_PIN_STYLES = stringPreferencesKey("unlocked_pin_styles")
        val TOP_RUNS = stringPreferencesKey("top_runs")
    }

    val playerStats: Flow<PlayerStats> = context.pinDropDataStore.data.map { prefs ->
        val unlockedRaw = prefs[Keys.UNLOCKED_PIN_STYLES] ?: PinStyle.CLASSIC_DOT.id
        val unlocked = unlockedRaw.split(",")
            .mapNotNull { token -> PinStyle.entries.firstOrNull { it.id == token } }
            .toMutableSet()
            .apply { add(PinStyle.CLASSIC_DOT) }

        val selected = PinStyle.fromId(prefs[Keys.SELECTED_PIN_STYLE])
        val topRuns = decodeRuns(prefs[Keys.TOP_RUNS].orEmpty())

        PlayerStats(
            bestScore = prefs[Keys.BEST_SCORE] ?: 0,
            highestLevel = prefs[Keys.HIGHEST_LEVEL] ?: 1,
            dailyBestScore = prefs[Keys.DAILY_BEST_SCORE] ?: 0,
            dailyCompletedDate = prefs[Keys.DAILY_COMPLETED_DATE] ?: "",
            dailyStreak = prefs[Keys.DAILY_STREAK] ?: 0,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedStyle = if (selected in unlocked) selected else PinStyle.CLASSIC_DOT,
            unlockedStyles = unlocked,
            topRuns = topRuns
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.pinDropDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.pinDropDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun selectPinStyle(style: PinStyle) {
        context.pinDropDataStore.edit { prefs ->
            prefs[Keys.SELECTED_PIN_STYLE] = style.id
        }
    }

    suspend fun recordRunResult(
        score: Int,
        levelReached: Int,
        mode: GameMode,
        dailyCleared: Boolean,
        todayDate: String = currentDateKey()
    ): List<PinStyle> {
        val newlyUnlocked = mutableListOf<PinStyle>()
        context.pinDropDataStore.edit { prefs ->
            val prevBest = prefs[Keys.BEST_SCORE] ?: 0
            if (score > prevBest) {
                prefs[Keys.BEST_SCORE] = score
            }

            val prevHighestLevel = prefs[Keys.HIGHEST_LEVEL] ?: 1
            if (mode == GameMode.CLASSIC && levelReached > prevHighestLevel) {
                prefs[Keys.HIGHEST_LEVEL] = levelReached
            }

            if (mode == GameMode.DAILY) {
                val prevDailyBest = prefs[Keys.DAILY_BEST_SCORE] ?: 0
                if (score > prevDailyBest) {
                    prefs[Keys.DAILY_BEST_SCORE] = score
                }
                if (dailyCleared) {
                    val lastCompleted = prefs[Keys.DAILY_COMPLETED_DATE].orEmpty()
                    val currentStreak = prefs[Keys.DAILY_STREAK] ?: 0
                    val nextStreak = when {
                        lastCompleted == todayDate -> currentStreak.coerceAtLeast(1)
                        lastCompleted == yesterdayDateKey(todayDate) -> currentStreak + 1
                        else -> 1
                    }
                    prefs[Keys.DAILY_COMPLETED_DATE] = todayDate
                    prefs[Keys.DAILY_STREAK] = nextStreak
                }
            }

            // Evaluate unlockable minimal pin styles
            val currentUnlockedRaw = prefs[Keys.UNLOCKED_PIN_STYLES] ?: PinStyle.CLASSIC_DOT.id
            val unlocked = currentUnlockedRaw.split(",")
                .mapNotNull { token -> PinStyle.entries.firstOrNull { it.id == token } }
                .toMutableSet()
                .apply { add(PinStyle.CLASSIC_DOT) }

            fun grantIfNew(style: PinStyle) {
                if (style !in unlocked) {
                    unlocked.add(style)
                    newlyUnlocked.add(style)
                }
            }

            if (score >= 10) grantIfNew(PinStyle.HOLLOW_RING)
            if (mode == GameMode.CLASSIC && levelReached >= 4) grantIfNew(PinStyle.DIAMOND)
            if (score >= 25) grantIfNew(PinStyle.SPEARHEAD)
            if (mode == GameMode.DAILY && dailyCleared) grantIfNew(PinStyle.CROSSHAIR)
            if (score >= 50) grantIfNew(PinStyle.ORBITAL)

            prefs[Keys.UNLOCKED_PIN_STYLES] = unlocked.joinToString(",") { it.id }

            if (score > 0) {
                val existingRuns = decodeRuns(prefs[Keys.TOP_RUNS].orEmpty()).toMutableList()
                existingRuns.add(
                    RunRecord(
                        score = score,
                        level = levelReached,
                        date = todayDate,
                        mode = mode
                    )
                )
                val sortedTop = existingRuns
                    .sortedWith(compareByDescending<RunRecord> { it.score }.thenByDescending { it.level })
                    .take(10)
                prefs[Keys.TOP_RUNS] = encodeRuns(sortedTop)
            }
        }
        return newlyUnlocked
    }

    suspend fun resetAllStats() {
        context.pinDropDataStore.edit { prefs ->
            val keepSound = prefs[Keys.SOUND_ENABLED] ?: true
            val keepHaptics = prefs[Keys.HAPTICS_ENABLED] ?: true
            prefs.clear()
            prefs[Keys.SOUND_ENABLED] = keepSound
            prefs[Keys.HAPTICS_ENABLED] = keepHaptics
            prefs[Keys.SELECTED_PIN_STYLE] = PinStyle.CLASSIC_DOT.id
            prefs[Keys.UNLOCKED_PIN_STYLES] = PinStyle.CLASSIC_DOT.id
        }
    }

    private fun encodeRuns(runs: List<RunRecord>): String =
        runs.joinToString(";") { "${it.score}|${it.level}|${it.date}|${it.mode.name}" }

    private fun decodeRuns(raw: String): List<RunRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 4) return@mapNotNull null
            val score = parts[0].toIntOrNull() ?: return@mapNotNull null
            val level = parts[1].toIntOrNull() ?: return@mapNotNull null
            val date = parts[2]
            val mode = runCatching { GameMode.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null
            RunRecord(score = score, level = level, date = date, mode = mode)
        }
    }

    companion object {
        fun currentDateKey(timeMillis: Long = System.currentTimeMillis()): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return fmt.format(Date(timeMillis))
        }

        fun yesterdayDateKey(todayDate: String): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = runCatching { fmt.parse(todayDate) }.getOrNull() ?: return ""
            val cal = Calendar.getInstance().apply {
                time = parsed
                add(Calendar.DAY_OF_YEAR, -1)
            }
            return fmt.format(cal.time)
        }
    }
}
