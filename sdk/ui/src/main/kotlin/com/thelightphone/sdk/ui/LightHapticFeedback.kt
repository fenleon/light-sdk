package com.thelightphone.sdk.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object LightHapticFeedback {

    // currently optimized for LP3, which has a "slow" motor
    fun click(context: Context) = vibrateForDuration(context, 45.milliseconds)

    /** Subtler tap feedback (lap ticks and similar). */
    fun tick(context: Context) = vibrateForDuration(context, 20.milliseconds)

    fun vibrateForDuration(context: Context, duration: Duration) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(duration.inWholeMilliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/**
 * Tool-safe haptics callback: wraps [LightHapticFeedback.click] so tool code
 * never needs a Context. Honors the user's global haptics preference.
 */
@Composable
fun rememberLightHapticClick(): () -> Unit {
    val context = LocalContext.current
    val enabled = LocalHapticsEnabled.current
    return remember(enabled, context) {
        { if (enabled) LightHapticFeedback.click(context) }
    }
}

/** As [rememberLightHapticClick] but with the subtler tick feedback. */
@Composable
fun rememberLightHapticTick(): () -> Unit {
    val context = LocalContext.current
    val enabled = LocalHapticsEnabled.current
    return remember(enabled, context) {
        { if (enabled) LightHapticFeedback.tick(context) }
    }
}
