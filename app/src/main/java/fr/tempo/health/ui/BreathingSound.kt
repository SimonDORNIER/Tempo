package fr.tempo.health.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class BreathingCueMode(val label: String) {
    SHORT("Deux sons courts"),
    CONTINUOUS("Sons continus")
}

enum class BreathingTimbre(val label: String) {
    SOFT("Doux"),
    PURE("Pur"),
    CLEAR("Clair")
}

data class BreathingSoundSettings(
    val enabled: Boolean = false,
    val mode: BreathingCueMode = BreathingCueMode.SHORT,
    val volume: Int = 55,
    val inhalePitchHz: Int = 550,
    val exhalePitchHz: Int = 350,
    val inhaleTimbre: BreathingTimbre = BreathingTimbre.SOFT,
    val exhaleTimbre: BreathingTimbre = BreathingTimbre.SOFT
)

/** Les clés sont dans les réglages déjà pris en charge par la sauvegarde complète. */
object BreathingSoundPreferences {
    private const val FILE = "tempo-health-settings"
    private const val PREFIX = "breathing-sound-"

    fun load(context: Context): BreathingSoundSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fun <T : Enum<T>> parse(raw: String?, fallback: T, values: Array<T>): T =
            values.firstOrNull { it.name == raw } ?: fallback
        return BreathingSoundSettings(
            enabled = prefs.getBoolean(PREFIX + "enabled", false),
            mode = parse(
                prefs.getString(PREFIX + "mode", null),
                BreathingCueMode.SHORT, BreathingCueMode.entries.toTypedArray()
            ),
            volume = prefs.getInt(PREFIX + "volume", 55).coerceIn(0, 100),
            inhalePitchHz = prefs.getInt(PREFIX + "inhale-hz", 550).coerceIn(200, 1000),
            exhalePitchHz = prefs.getInt(PREFIX + "exhale-hz", 350).coerceIn(200, 1000),
            inhaleTimbre = parse(
                prefs.getString(PREFIX + "inhale-timbre", null),
                BreathingTimbre.SOFT, BreathingTimbre.entries.toTypedArray()
            ),
            exhaleTimbre = parse(
                prefs.getString(PREFIX + "exhale-timbre", null),
                BreathingTimbre.SOFT, BreathingTimbre.entries.toTypedArray()
            )
        )
    }

    fun save(context: Context, settings: BreathingSoundSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(PREFIX + "enabled", settings.enabled)
            .putString(PREFIX + "mode", settings.mode.name)
            .putInt(PREFIX + "volume", settings.volume.coerceIn(0, 100))
            .putInt(PREFIX + "inhale-hz", settings.inhalePitchHz.coerceIn(200, 1000))
            .putInt(PREFIX + "exhale-hz", settings.exhalePitchHz.coerceIn(200, 1000))
            .putString(PREFIX + "inhale-timbre", settings.inhaleTimbre.name)
            .putString(PREFIX + "exhale-timbre", settings.exhaleTimbre.name)
            .apply()
    }
}

/**
 * Audio généré localement, sans fichier externe ni service. L'arrêt de la
 * coroutine (pause, sortie, changement de phase ou muet) coupe l'AudioTrack.
 */
class BreathingSoundPlayer {
    suspend fun play(
        settings: BreathingSoundSettings,
        inhale: Boolean,
        phaseSeconds: Int,
        preview: Boolean = false
    ) {
        if (settings.volume == 0) return
        val durationMs = when {
            preview && settings.mode == BreathingCueMode.CONTINUOUS -> 900
            settings.mode == BreathingCueMode.SHORT -> 240
            else -> phaseSeconds.coerceAtLeast(1) * 1000
        }
        val frequency = if (inhale) settings.inhalePitchHz else settings.exhalePitchHz
        val timbre = if (inhale) settings.inhaleTimbre else settings.exhaleTimbre
        val samples = withContext(Dispatchers.Default) {
            makeSamples(durationMs, frequency, timbre, settings.volume)
        }
        withContext(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                if (track.state == AudioTrack.STATE_INITIALIZED &&
                    track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING) > 0
                ) {
                    track.play()
                    delay(durationMs.toLong())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Audio indisponible : l'exercice reste utilisable en silence.
            } finally {
                track?.let { audio ->
                    runCatching { audio.pause() }
                    runCatching { audio.flush() }
                    runCatching { audio.release() }
                }
            }
        }
    }

    private fun makeSamples(
        durationMs: Int, pitchHz: Int, timbre: BreathingTimbre, volume: Int
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs / 1000).coerceAtLeast(1)
        val hz = pitchHz.coerceIn(200, 1000)
        val amplitude = (volume.coerceIn(0, 100) / 100.0) * 0.43
        val attack = (SAMPLE_RATE * 0.035).toInt().coerceAtLeast(1)
        val release = (SAMPLE_RATE * 0.095).toInt().coerceAtLeast(1)
        return ShortArray(count) { i ->
            val phase = 2.0 * PI * hz * i / SAMPLE_RATE
            val wave = when (timbre) {
                BreathingTimbre.SOFT -> sin(phase) * 0.91 + sin(phase * 2.0) * 0.09
                BreathingTimbre.PURE -> sin(phase)
                BreathingTimbre.CLEAR -> sin(phase) * 0.72 +
                    sin(phase * 2.0) * 0.20 + sin(phase * 3.0) * 0.08
            }
            val fadeIn = (i.toDouble() / attack).coerceIn(0.0, 1.0)
            val fadeOut = ((count - 1 - i).toDouble() / release).coerceIn(0.0, 1.0)
            (wave * fadeIn.coerceAtMost(fadeOut) * amplitude * Short.MAX_VALUE)
                .roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
