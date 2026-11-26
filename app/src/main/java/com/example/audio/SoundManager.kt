package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * SoundPool-backed audio manager for minimalist monochrome click, lock, level-clear,
 * and collision sound effects.
 */
class SoundManager(context: Context) {
    private val sampleRate = 22050
    private val loadedIds = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    var isSoundEnabled: Boolean = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .apply {
            setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedIds.add(sampleId)
                }
            }
        }

    private val clickSoundId: Int
    private val stickSoundId: Int
    private val levelClearSoundId: Int
    private val collisionSoundId: Int

    init {
        val cacheDir = context.cacheDir
        clickSoundId = loadSynthesizedWav(
            file = File(cacheDir, "pindrop_click.wav"),
            samples = synthesizeTone(
                durationMs = 26,
                startFreqHz = 920.0,
                endFreqHz = 1380.0,
                amplitude = 0.28,
                decayRate = 18.0
            )
        )
        stickSoundId = loadSynthesizedWav(
            file = File(cacheDir, "pindrop_stick.wav"),
            samples = synthesizeTone(
                durationMs = 46,
                startFreqHz = 440.0,
                endFreqHz = 215.0,
                amplitude = 0.36,
                decayRate = 14.0
            )
        )
        levelClearSoundId = loadSynthesizedWav(
            file = File(cacheDir, "pindrop_clear.wav"),
            samples = synthesizeTone(
                durationMs = 80,
                startFreqHz = 523.25,
                endFreqHz = 523.25,
                amplitude = 0.28,
                decayRate = 6.0
            ) + synthesizeTone(
                durationMs = 130,
                startFreqHz = 783.99,
                endFreqHz = 783.99,
                amplitude = 0.30,
                decayRate = 5.0
            )
        )
        collisionSoundId = loadSynthesizedWav(
            file = File(cacheDir, "pindrop_collision.wav"),
            samples = synthesizeTone(
                durationMs = 190,
                startFreqHz = 150.0,
                endFreqHz = 56.0,
                amplitude = 0.48,
                decayRate = 7.5
            )
        )
    }

    fun playClick() = playIfReady(clickSoundId, volume = 0.75f)

    fun playStick() = playIfReady(stickSoundId, volume = 0.85f)

    fun playLevelClear() = playIfReady(levelClearSoundId, volume = 0.85f)

    fun playCollision() = playIfReady(collisionSoundId, volume = 1.0f)

    fun release() {
        loadedIds.clear()
        soundPool.release()
    }

    private fun playIfReady(soundId: Int, volume: Float) {
        if (!isSoundEnabled || soundId <= 0) return
        if (soundId in loadedIds) {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    private fun loadSynthesizedWav(file: File, samples: ShortArray): Int {
        return runCatching {
            writeWavFile(file, samples)
            soundPool.load(file.absolutePath, 1)
        }.getOrDefault(0)
    }

    private fun writeWavFile(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val totalDataLen = dataSize + 36
        val byteRate = sampleRate * 2

        val out = ByteArrayOutputStream(44 + dataSize)
        // RIFF header
        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        writeIntLE(out, totalDataLen)
        out.write("WAVE".toByteArray(Charsets.US_ASCII))
        // fmt subchunk
        out.write("fmt ".toByteArray(Charsets.US_ASCII))
        writeIntLE(out, 16) // PCM chunk size
        writeShortLE(out, 1) // AudioFormat = 1 (PCM)
        writeShortLE(out, 1) // NumChannels = 1 (Mono)
        writeIntLE(out, sampleRate)
        writeIntLE(out, byteRate)
        writeShortLE(out, 2) // BlockAlign = 2 bytes
        writeShortLE(out, 16) // BitsPerSample = 16
        // data subchunk
        out.write("data".toByteArray(Charsets.US_ASCII))
        writeIntLE(out, dataSize)

        for (sample in samples) {
            writeShortLE(out, sample.toInt())
        }

        FileOutputStream(file).use { fos ->
            fos.write(out.toByteArray())
        }
    }

    private fun writeIntLE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
        out.write((value shr 16) and 0xFF)
        out.write((value shr 24) and 0xFF)
    }

    private fun writeShortLE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun synthesizeTone(
        durationMs: Int,
        startFreqHz: Double,
        endFreqHz: Double,
        amplitude: Double,
        decayRate: Double
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count.coerceAtLeast(1)
            val freq = startFreqHz + (endFreqHz - startFreqHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-decayRate * t) * (1.0 - t * 0.15)
            val sample = (sin(phase) * amplitude * env * Short.MAX_VALUE).toInt()
            out[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
