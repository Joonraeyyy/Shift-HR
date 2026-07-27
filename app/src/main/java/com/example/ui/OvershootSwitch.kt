package com.example.ui

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Animated toggle switch with overshoot spring easing:
 * - Translates knob across track with cubic-bezier(0.34, 1.56, 0.64, 1) over ~0.4s
 * - Crossfades track and knob colors, blending smoothly with user's selected active theme color
 * - Respects system reduced motion settings (instant position snap)
 * - Triggers tactile haptic feedback on state toggle
 */
@Composable
fun OvershootSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color? = null,
    inactiveKnobColor: Color? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Detect if user has enabled Reduced Motion in OS settings
    val isReducedMotion = remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (e: Exception) {
            false
        }
    }

    val isDark = isSystemInDarkTheme() || com.example.ui.theme.AppTextColor == Color(0xFFFFFFFF)
    
    val defaultInactiveTrack = inactiveTrackColor ?: if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
    val defaultInactiveKnob = inactiveKnobColor ?: if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    
    val alpha = if (enabled) 1f else 0.45f

    // Spring overshoot cubic-bezier(0.34, 1.56, 0.64, 1) over 0.4s
    val springEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
    
    val posAnimationSpec = if (isReducedMotion) {
        snap()
    } else {
        tween<Float>(
            durationMillis = 400,
            easing = springEasing
        )
    }

    val colorAnimationSpec = if (isReducedMotion) snap() else tween<Color>(durationMillis = 400)

    // Animated knob offset fraction (0f -> 1f with overshoot bounce or instant snap)
    val knobPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = posAnimationSpec,
        label = "overshoot_knob_position"
    )

    // Track color crossfade over 0.4s blending with active theme color
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.35f) else defaultInactiveTrack,
        animationSpec = colorAnimationSpec,
        label = "overshoot_track_color"
    )

    // Border color crossfade over 0.4s
    val borderColor by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.8f) else if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000),
        animationSpec = colorAnimationSpec,
        label = "overshoot_border_color"
    )

    // Knob color crossfade over 0.4s
    val knobColor by animateColorAsState(
        targetValue = if (checked) activeColor else defaultInactiveKnob,
        animationSpec = colorAnimationSpec,
        label = "overshoot_knob_color"
    )

    // Tactile haptic feedback on state change
    LaunchedEffect(checked) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val density = LocalDensity.current
    val trackWidth = 50.dp
    val trackHeight = 28.dp
    val knobSize = 22.dp
    val padding = 3.dp

    // Travel distance
    val maxTravelPx = with(density) { (trackWidth - knobSize - (padding * 2)).toPx() }
    val knobOffsetPx = maxTravelPx * knobPosition

    Box(
        modifier = modifier
            .alpha(alpha)
            .minimumInteractiveComponentSize()
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCheckedChange(!checked)
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(trackWidth, trackHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(padding),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = knobOffsetPx.roundToInt(), y = 0) }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(knobColor)
            )
        }
    }
}

