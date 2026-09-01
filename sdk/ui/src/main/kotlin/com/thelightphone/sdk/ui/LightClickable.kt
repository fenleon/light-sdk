package com.thelightphone.sdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

/**
 * Will be periodically set based on server preferences, see:
 * [com.thelightphone.sdk.shared.LightServiceMethod.GetUserPreferences].
 */
val LocalHapticsEnabled = compositionLocalOf { false }

/**
 * Makes content clickable without displaying a visual press indication. When
 * [onLongClick] is set the tap + long-press are disambiguated by
 * [combinedClickable] (a long-press fires [onLongClick] instead of
 * [onClick]); the LightOS haptic still fires on finger-down either way.
 */
fun Modifier.lightClickable(
    enabled: Boolean = true,
    // will be &&'ed with the user's global haptics preference
    hapticsEnabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val userHapticsEnabled = LocalHapticsEnabled.current
    val context = LocalContext.current
    val performHaptic = enabled && hapticsEnabled && userHapticsEnabled
    // Fire on finger-down like LightOS
    val hapticInput = pointerInput(performHaptic) {
        if (!performHaptic) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            LightHapticFeedback.click(context)
        }
    }
    if (onLongClick == null) {
        hapticInput.clickable(
            interactionSource = null,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    } else {
        hapticInput.combinedClickable(
            interactionSource = null,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
}
