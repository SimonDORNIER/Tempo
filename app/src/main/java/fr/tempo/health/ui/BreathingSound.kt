package fr.tempo.health.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import fr.tempo.health.domain.BreathPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
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
    val enabled: Boolean = true,
    val mode: BreathingCueMode = BreathingCueMode.SHORT,
    val volume: Int = 70,
    val inhalePitchHz: Int = 550,
    val exhalePitchHz: Int = 350,
    val inhaleTimbre: BreathingTimbre = BreathingTimbre.SOFT,
    val exhaleTimbre: BreathingTimbre = BreathingTimbre.SOFT
)

/** Tous les réglages sont inclus dans l'export des préférences de Tempo. */
object BreathingSoundPreferences {
    private const val FILE = "tempo-health-settings"
    private const val PREFIX = "breathing-sound-"

    fun load(context: Context): BreathingSoundSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        fun <T : Enum<T>> parse(raw: String?, fallback: T, values: Array<T>): T =
            values.firstOrNull { it.name == raw } ?: fallback
        return BreathingSoundSettings(
            enabled = prefs.getBoolean(PREFIX + "enabled", true),
            mode = parse(prefs.getString(PREFIX + "mode", null),
                BreathingCueMode.SHORT, BreathingCueMode.entries.toTypedArray()),
            volume = prefs.getInt(PREFIX + "volume", 70).coerceIn(0, 100),
            inhalePitchHz = prefs.getInt(PREFIX + "inhale-hz", 550).coerceIn(200, 1000),
            exhalePitchHz = prefs.getInt(PREFIX + "exhale-hz", 350).coerceIn(200, 1000),
            inhaleTimbre = parse(prefs.getString(PREFIX + "inhale-timbre", null),
                BreathingTimbre.SOFT, BreathingTimbre.entries.toTypedArray()),
            exhaleTimbre = parse(prefs.getString(PREFIX + "exhale-timbre", null),
                BreathingTimbre.SOFT, BreathingTimbre.entries.toTypedArray())
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
 * PCM 44,1 kHz en flux continu vers la sortie MULTIMÉDIA.
 * Contrairement à MODE_STATIC, la taille du buffer ne dépend jamais de la
 * longueur de la phase. Chaque bloc est annulable en pause/arrêt.
 */
class BreathingSoundPlayer {
    private val mutex = Mutex()

    /** false signale un échec de la sortie audio ou un volume de 0. */
    suspend fun play(
        settings: BreathingSoundSettings,
        phase: BreathPhase,
        remainingSeconds: Int,
        preview: Boolean = false
    ): Boolean {
        if (settings.volume <= 0 && !preview) return false
        mutex.lock()
        try {
            return playStream(settings, phase, remainingSeconds, preview)
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun playStream(
        settings: BreathingSoundSettings,
        phase: BreathPhase,
        remainingSeconds: Int,
        preview: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val hold = phase == BreathPhase.HOLD_AFTER_INHALE ||
            phase == BreathPhase.HOLD_AFTER_EXHALE
        val durationMs = when {
            hold -> 320
            settings.mode == BreathingCueMode.SHORT -> 260
            preview -> 1050
            else -> remainingSeconds.coerceAtLeast(1) * 1000
        }
        val inspiration = phase == BreathPhase.INHALE
        val pitch = when {
            hold -> 280
            inspiration -> settings.inhalePitchHz
            else -> settings.exhalePitchHz
        }
        val timbre = if (inspiration) settings.inhaleTimbre else settings.exhaleTimbre
        val volume = if (preview) settings.volume.coerceAtLeast(30) else settings.volume
        val totalSamples = SAMPLE_RATE * durationMs / 1000
        val minBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        var track: AudioTrack? = null
        try {
            require(minBytes > 0) { "Format PCM non accepté" }
            val audio = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBytes, CHUNK_SAMPLES * 8))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track = audio
            check(audio.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack non initialisé" }
            audio.play() // Le flux doit démarrer avant le remplissage progressif.
            var offset = 0
            val buffer = ShortArray(CHUNK_SAMPLES)
            while (offset < totalSamples) {
                currentCoroutineContext().ensureActive()
                val count = min(CHUNK_SAMPLES, totalSamples - offset)
                fillSamples(buffer, count, offset, totalSamples, pitch, timbre, volume, hold)
                var written = 0
                while (written < count) {
                    currentCoroutineContext().ensureActive()
                    val n = audio.write(
                        buffer, written, count - written, AudioTrack.WRITE_BLOCKING
                    )
                    check(n > 0) { "Écriture audio refusée : " + n }
                    written += n
                }
                offset += count
            }
            // Le dernier bloc peut encore être dans le buffer du périphérique.
            delay(90)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (problem: Exception) {
            Log.w("TempoBreathingAudio", "Impossible de lire la phase", problem)
            false
        } finally {
            track?.let { audio ->
                runCatching { audio.pause() }
                runCatching { audio.flush() }
                runCatching { audio.release() }
            }
        }
    }

    private fun fillSamples(
        samples: ShortArray,
        count: Int,
        offset: Int,
        total: Int,
        pitch: Int,
        timbre: BreathingTimbre,
        volume: Int,
        hold: Boolean
    ) {
        val hz = pitch.coerceIn(200, 1000)
        val amplitude = volume.coerceIn(0, 100) / 100.0 * if (hold) 0.40 else 0.72
        val attack = SAMPLE_RATE * 0.025
        val release = SAMPLE_RATE * 0.060
        for (i in 0 until count) {
            val t = offset + i
            val angle = 2.0 * PI * hz * t / SAMPLE_RATE
            val wave = when (timbre) {
                BreathingTimbre.SOFT -> sin(angle) * 0.92 + sin(angle * 2) * 0.08
                BreathingTimbre.PURE -> sin(angle)
                BreathingTimbre.CLEAR -> sin(angle) * 0.72 +
                    sin(angle * 2) * 0.20 + sin(angle * 3) * 0.08
            }
            val fade = min(
                (t.toDouble() / attack).coerceIn(0.0, 1.0),
                ((total - t).toDouble() / release).coerceIn(0.0, 1.0)
            )
            // Les pauses se distinguent par deux petits sons graves, puis le silence.
            val pulse = if (!hold) 1.0 else {
                val positionMs = t * 1000 / SAMPLE_RATE
                if (positionMs in 0..105 || positionMs in 170..275) 1.0 else 0.0
            }
            samples[i] = (wave * fade * pulse * amplitude * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHUNK_SAMPLES = 1024
    }
}
