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
    val volume: Int = 65,
    val inhalePitchHz: Int = 460,
    val exhalePitchHz: Int = 330,
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
            volume = prefs.getInt(PREFIX + "volume", 65).coerceIn(0, 100),
            inhalePitchHz = prefs.getInt(PREFIX + "inhale-hz", 460).coerceIn(200, 1000),
            exhalePitchHz = prefs.getInt(PREFIX + "exhale-hz", 330).coerceIn(200, 1000),
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
        val durationMs = BreathingSoundEnvelope.durationMs(
            settings.mode, hold, remainingSeconds, preview
        )
        val inspiration = phase == BreathPhase.INHALE
        val pitch = when {
            hold -> 290
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
                fillSamples(
                    buffer, count, offset, totalSamples, durationMs,
                    pitch, timbre, volume, phase, settings.mode
                )
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
            // Laisser le temps au périphérique de jouer la queue feutrée.
            val remainingFrames = (totalSamples - audio.playbackHeadPosition).coerceAtLeast(0)
            delay((remainingFrames * 1000L / SAMPLE_RATE + 35L).coerceIn(35L, 300L))
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
        durationMs: Int,
        pitch: Int,
        timbre: BreathingTimbre,
        volume: Int,
        phase: BreathPhase,
        mode: BreathingCueMode
    ) {
        val hold = phase == BreathPhase.HOLD_AFTER_INHALE ||
            phase == BreathPhase.HOLD_AFTER_EXHALE
        val hz = pitch.coerceIn(200, 1000).toDouble()
        val amplitude = volume.coerceIn(0, 100) / 100.0 *
            BreathingSoundEnvelope.peak(mode, hold)
        // Une très légère montée accompagne l'inspiration, une descente l'expiration.
        // L'intégrale de fréquence évite les ruptures de phase durant le glissement.
        val glide = when (phase) {
            BreathPhase.INHALE -> 0.025
            BreathPhase.EXHALE -> -0.025
            else -> 0.0
        }
        val startHz = hz * (1.0 - glide / 2.0)
        val difference = hz * glide

        for (i in 0 until count) {
            val frame = offset + i
            val t = frame.toDouble()
            val progressMs = t * 1000.0 / SAMPLE_RATE
            val cycles = (startHz * t + difference * t * t / (2.0 * total)) /
                SAMPLE_RATE
            val angle = 2.0 * PI * cycles
            val wave = when (timbre) {
                BreathingTimbre.SOFT ->
                    sin(angle) * 0.82 + sin(angle * 0.5) * 0.18
                BreathingTimbre.PURE ->
                    sin(angle) * 0.94 + sin(angle * 0.5) * 0.06
                BreathingTimbre.CLEAR ->
                    sin(angle) * 0.80 + sin(angle * 2.0) * 0.13 +
                        sin(angle * 0.5) * 0.07
            }
            val gain = BreathingSoundEnvelope.gain(progressMs, durationMs, mode, hold)
            samples[i] = (wave * gain * amplitude * Short.MAX_VALUE)
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
