package com.thelightphone.sdk.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * UIText equivalent for the Light SDK
 *
 * - variant-based typography
 * - primary/secondary (lighten) color
 * - optional underline + alignment
 *
 * Underline is a drawn bar, not `TextDecoration` — the native LightOS
 * selected-option underline is a ~10 px bar under the word (measured on the
 * LP3's Ringer Mode panel, 2026-08-22: 10 px thick, word-width, at the
 * baseline; the DESIGN.md "~2dp full-width underline" is the separate
 * text-field input underline). LightText's selection underline is ~4 dp and
 * spans just the word (feedback 2026-08-22).
 */
enum class LightTextVariant {
    Title,
    Subtitle,
    Heading,
    Subheading,
    Copy,
    Button,
    Paragraph,
    ParagraphWide,
    Detail,
    Fine,
    Superfine,
    Micro,
}

@Composable
private fun variantStyle(variant: LightTextVariant): TextStyle {
    val t = LightThemeTokens.typography
    val base = when (variant) {
        LightTextVariant.Title -> t.title
        LightTextVariant.Subtitle -> t.subtitle
        LightTextVariant.Heading -> t.heading
        LightTextVariant.Subheading -> t.subheading
        LightTextVariant.Copy -> t.copy
        LightTextVariant.Button -> t.button
        LightTextVariant.Paragraph -> t.paragraph
        LightTextVariant.ParagraphWide -> t.paragraphWide
        LightTextVariant.Detail -> t.detail
        LightTextVariant.Fine -> t.fine
        LightTextVariant.Superfine -> t.superfine
        LightTextVariant.Micro -> t.micro
    }
    return base.scaledForScreenHeight()
}

@Composable
fun TextStyle.scaledForScreenHeight(): TextStyle {
    val fontSize = fontSize.scaledForScreenHeight()
    val lineHeight = lineHeight.scaledForScreenHeight()
    val letterSpacing = letterSpacing.scaledForScreenHeight()
    return copy(
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
    )
}

@Composable
internal fun TextUnit.scaledForScreenHeight(): TextUnit {
    if (this == TextUnit.Unspecified) return this
    return value.designVerticalPxToSp()
}

@Composable
fun LightText(
    text: String,
    variant: LightTextVariant,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    lighten: Boolean = false,
    underline: Boolean = false,
    monospace: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    color: Color? = null,
) {
    val colors = LightThemeTokens.colors
    val baseColor = when {
        color != null -> color
        lighten -> colors.contentSecondary
        else -> colors.content
    }

    val style = variantStyle(variant)
        .let { if (align != null) it.copy(textAlign = align) else it }
        .let { if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it }

    // The native underline is a ~2dp bar under the text, not a
    // text-decoration hairline. It spans the word(s), not the full row
    // (feedback 2026-08-22: the Networks panel's selected-row underline ran
    // the whole width; it should sit under the label only). Draw it at the
    // last line's baseline, just below it — the same spot the platform would
    // draw an underline (LP3 standard, feedback 2026-08-21).
    val underlineModifier = if (underline) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val measured = remember(text, style) {
            textMeasurer.measure(AnnotatedString(text), style)
        }
        val baselinePx = measured.getLineBaseline(measured.lineCount - 1)
        val fontSizePx = with(density) { style.fontSize.toPx() }
        val thicknessPx = with(density) { UNDERLINE_THICKNESS_DP.dp.toPx() }
        modifier.drawBehind {
            drawRect(
                color = baseColor,
                topLeft = Offset(0f, baselinePx + fontSizePx * UNDERLINE_OFFSET_OF_EM),
                size = Size(measured.size.width.toFloat(), thicknessPx),
            )
        }
    } else {
        modifier
    }

    Text(
        text = text,
        modifier = underlineModifier,
        color = baseColor.takeUnless { it == Color.Unspecified } ?: LocalContentColor.current,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
    )
}

// 4 dp ≈ the LP3's selected-row underline thickness (measured 10 px at
// 420 dpi on the Ringer Mode and USB panels, 2026-08-22).
private const val UNDERLINE_THICKNESS_DP = 4
// Air between the text baseline and the underline's top: 0.24 em ≈ the LP3's
// ~24 px gap (measured under "Media transfer", USB panel, and "On", Ringer
// Mode — the ink ends ~24 px above the bar, 2026-08-22). The 0.12 em (12 px)
// gap read as too tight.
private const val UNDERLINE_OFFSET_OF_EM = 0.24f

