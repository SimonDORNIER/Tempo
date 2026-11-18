package fr.tempo.health.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import fr.tempo.health.domain.BreathPhase
import fr.tempo.health.domain.BreathPhaseState
import fr.tempo.health.domain.BreathingPattern
import fr.tempo.health.ui.theme.TempoColors as C

/** Rayon relatif au début ou à la fin du tick courant du minuteur existant. */
internal fun breathingBubbleFraction(
    pattern: BreathingPattern,
    state: BreathPhaseState,
    nextSecond: Boolean = false
): Float {
    val duration = state.phaseDurationSeconds.coerceAtLeast(1)
    val elapsed = duration - state.remainingSeconds + if (nextSecond) 1 else 0
    val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
    return when (state.phase) {
        BreathPhase.INHALE -> progress
        BreathPhase.EXHALE -> 1f - progress
        // Une pause reste fixe, même si la phase respiratoire précédente a été omise.
        BreathPhase.HOLD_AFTER_INHALE -> if (pattern.inhaleSeconds > 0) 1f else 0f
        BreathPhase.HOLD_AFTER_EXHALE -> if (pattern.exhaleSeconds > 0) 0f else 1f
    }
}

/**
 * Indication visuelle uniquement : aucun nouveau minuteur de séance.
 * Le moteur de respiration reste la source de vérité pour les sons et les phases.
 */
@Composable
internal fun BreathingBubble(
    pattern: BreathingPattern,
    phaseState: BreathPhaseState?,
    preparing: Boolean,
    paused: Boolean
) {
    val initial = phaseState?.let { breathingBubbleFraction(pattern, it) } ?: 0f
    val expansion = remember { Animatable(initial) }
    var lastStep by remember { mutableIntStateOf(Int.MIN_VALUE) }

    LaunchedEffect(pattern, phaseState, preparing, paused) {
        val state = phaseState ?: return@LaunchedEffect
        if (paused) return@LaunchedEffect // Annule l'animation sans déplacer la bulle.
        val start = breathingBubbleFraction(pattern, state)
        if (preparing) {
            expansion.snapTo(start)
            lastStep = Int.MIN_VALUE
            return@LaunchedEffect
        }
        if (lastStep != state.stepIndex) {
            expansion.snapTo(start)
            lastStep = state.stepIndex
        }
        when (state.phase) {
            BreathPhase.HOLD_AFTER_INHALE, BreathPhase.HOLD_AFTER_EXHALE ->
                expansion.snapTo(start)
            BreathPhase.INHALE, BreathPhase.EXHALE ->
                expansion.animateTo(
                    targetValue = breathingBubbleFraction(pattern, state, nextSecond = true),
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
        }
    }

    // L'espace réservé reste fixe : aucun déplacement du texte ni du compteur.
    Canvas(Modifier.size(124.dp)) {
        val radius = size.minDimension * (0.23f + 0.21f * expansion.value.coerceIn(0f, 1f))
        val glowRadius = radius * 1.12f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(C.calm.copy(alpha = 0.17f), C.calm.copy(alpha = 0f)),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(C.calm.copy(alpha = 0.82f), C.calmDeep, C.sleepDeep),
                center = center - Offset(radius * 0.28f, radius * 0.32f),
                radius = radius * 1.45f
            ),
            radius = radius
        )
        drawCircle(
            color = C.calm.copy(alpha = 0.65f),
            radius = radius,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}
