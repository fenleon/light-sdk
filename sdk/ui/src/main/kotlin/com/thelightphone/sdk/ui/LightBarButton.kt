package com.thelightphone.sdk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow

sealed interface LightBarButton {
    val onClick: (() -> Unit)?
    val contentDescription: String?

    data class Text(
        val text: String,
        override val contentDescription: String? = null,
        override val onClick: (() -> Unit)?,
    ) : LightBarButton

    /**).
     * used for custom icons (your own painter
     *
     * for LightOS icons, prefer [LightBarButton.LightIcon].
     */
    data class Icon(
        val painter: Painter,
        override val onClick: (() -> Unit)?,
        override val contentDescription: String? = null,
        val sizeUnits: Float = LightBarButtonDefaults.ICON_SIZE_UNITS,
    ) : LightBarButton

    /**
     * LightOS icon (from [LightIcons]).
     */
    data class LightIcon(
        val icon: LightIconConfiguration,
        override val onClick: (() -> Unit)?,
        override val contentDescription: String? = icon.name,
        val sizeUnits: Float = LightBarButtonDefaults.ICON_SIZE_UNITS,
    ) : LightBarButton
}

object LightBarButtonDefaults {
    const val ICON_SIZE_UNITS = 2f
}

/** Minimum tap-target width for bar-button icons — the 2-unit icon alone is a
 *  ~27 dp target on the LP3 (well under the ~48 dp touch guideline), which
 *  made the composer's Send button miss-prone ("needs multiple presses",
 *  feedback 2026-08-17). The icon keeps its visual size; only the clickable
 *  area grows. */
private const val MIN_BAR_BUTTON_TOUCH_WIDTH_UNITS = 3.5f

typealias LightTopBarButton = LightBarButton
typealias LightBottomBarItem = LightBarButton

@Composable
internal fun LightBarButtonView(
    button: LightBarButton?,
    heightUnits: Float,
    iconSizeUnits: Float = LightBarButtonDefaults.ICON_SIZE_UNITS,
    textVariant: LightTextVariant,
    useSpacerWhenNull: Boolean,
    // Top-bar left icons are flush-left so the chevron's apex matches the
    // native LightOS back arrow; bottom-bar items stay centered (default).
    iconAlignment: Alignment = Alignment.Center,
) {
    if (button == null) {
        if (useSpacerWhenNull) {
            LightIcon(
                icon = LightIcons.SPACER,
                size = iconSizeUnits,
                contentDescription = null,
            )
        }
        return
    }

    val baseModifier = Modifier.let { modifier ->
        if (button.onClick != null) modifier.lightClickable { button.onClick?.invoke() } else modifier
    }

    when (button) {
        is LightBarButton.Text -> {
            Box(
                modifier = baseModifier.height(heightUnits.gridUnitsAsDp()),
                contentAlignment = Alignment.Center,
            ) {
                LightText(
                    text = button.text,
                    variant = textVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        is LightBarButton.Icon -> {
            val size = button.sizeUnits.gridUnitsAsDp()
            // The icon keeps its visual size; only the clickable area grows to
            // a minimum-width tap target (a bare 2-unit icon ≈ 27 dp is a
            // miss-prone target — "send needs multiple presses", feedback
            // 2026-08-17). The icon stays centered, so it shifts at most a
            // fraction of a unit.
            Box(
                modifier = baseModifier.widthIn(
                    min = maxOf(button.sizeUnits, MIN_BAR_BUTTON_TOUCH_WIDTH_UNITS).gridUnitsAsDp(),
                ),
                contentAlignment = iconAlignment,
            ) {
                Image(
                    painter = button.painter,
                    contentDescription = button.contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size),
                )
            }
        }

        is LightBarButton.LightIcon -> {
            Box(
                modifier = baseModifier.widthIn(
                    min = maxOf(button.sizeUnits, MIN_BAR_BUTTON_TOUCH_WIDTH_UNITS).gridUnitsAsDp(),
                ),
                contentAlignment = iconAlignment,
            ) {
                LightIcon(
                    icon = button.icon,
                    size = button.sizeUnits,
                    contentDescription = button.contentDescription,
                )
            }
        }
    }
}
