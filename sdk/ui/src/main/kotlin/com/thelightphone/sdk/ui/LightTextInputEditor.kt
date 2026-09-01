package com.thelightphone.sdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thelightphone.lp3Keyboard.ui.*
import com.thelightphone.lp3Keyboard.ui.viewmodel.EnQwertyLp3KeyboardViewModel
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3KeyboardViewModel
import com.thelightphone.lp3Keyboard.ui.viewmodel.Lp3RepeatableKeyboardCallback
import com.thelightphone.lp3Keyboard.ui.viewmodel.defaultEmojis
import com.thelightphone.sdk.ui.keyboard.LightEmbeddedLp3Keyboard
import com.thelightphone.sdk.ui.keyboard.TextInputKeyboardCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val INPUT_UNDERLINE_THICKNESS_PX = 3f
private const val INPUT_UNDERLINE_GAP_GRID_UNITS = 0.5f

@Composable
fun LightTextInputEditor(
    title: String,
    state: TextFieldState,
    onSubmit: (CharSequence) -> Unit,
    onBack: () -> Unit,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
    modifier: Modifier = Modifier,
    submitLabel: String = "SUBMIT",
    submitIcon: LightIconConfiguration? = null,
    showBackButton: Boolean = true,
    singleLine: Boolean = false,
    initialCaps: Boolean = false,
    editorKey: Any = remember { Any() },
    inputTextStyle: TextStyle? = null,
    submitInTopBar: Boolean = false,
    topBarSubmitIcon: LightIconConfiguration? = null,
    topBarSubmitLabel: String? = null,
    bottomAligned: Boolean = false,
    centered: Boolean = false,
    submitOnReturn: Boolean? = null,
    submitBottomRight: Boolean = false,
    // An extra action in the bottom bar's LEFT slot (icon or text) — the
    // two-action editor grammar (LightOS calendar-form style): a leading
    // "details" action beside the centered submit (feedback 2026-08-26).
    bottomBarLeadingButton: LightBarButton? = null,
    // An extra action in the bottom bar's CENTER slot — used with
    // [submitBottomRight] to render left · center · right (Tasks' notes
    // CLEAR / X / SAVE, feedback 2026-08-26).
    bottomBarCenterButton: LightBarButton? = null,
) {
    val currentOnSubmit by rememberUpdatedState(onSubmit)
    val hapticsEnabled = LocalHapticsEnabled.current
    val context = LocalContext.current
    val currentOnHaptic by rememberUpdatedState {
        if (hapticsEnabled) LightHapticFeedback.click(context)
    }
    val keyboardCallback = remember(state, singleLine, submitOnReturn) {
        TextInputKeyboardCallback(
            state = state,
            singleLine = singleLine,
            submitOnReturn = submitOnReturn,
            onReturn = { currentOnSubmit(state.text) },
            onHaptic = { currentOnHaptic() },
        )
    }

    val keyboardViewModel: Lp3KeyboardViewModel<*> = viewModel<EnQwertyLp3KeyboardViewModel<*>>(
        key = "LightTextInputEditor-$editorKey",
        factory = factory(keyboardCallback, keyboardOptionsFlow, initialCaps),
    )

    LightTextInputEditor(
        title,
        state,
        onSubmit,
        onBack,
        keyboardViewModel,
        modifier,
        submitLabel,
        submitIcon,
        showBackButton,
        singleLine,
        inputTextStyle,
        submitInTopBar,
        topBarSubmitIcon,
        topBarSubmitLabel,
        bottomAligned,
        centered,
        submitOnReturn,
        submitBottomRight,
        bottomBarLeadingButton,
        bottomBarCenterButton,
    )
}

/**
 * Full-screen text entry matching LightOS `DisplayWithKeyboardPortrait`
 *
 * - Top bar with back button + title (with the submit action in the right slot
 *   when [submitInTopBar] — the Notes/composer style: keyboard flush at the
 *   bottom, no bottom bar, maximum input space)
 * - Remaining space shows underlined heading-style input (top-aligned), or the
 *   Notes-style input when [bottomAligned]: small text anchored at the bottom,
 *   lines growing upward as the user types
 * - Embedded LP3 keyboard, and [LightBottomBar] below it (unless
 *   [submitInTopBar])
 */
@Composable
fun LightTextInputEditor(
    title: String,
    state: TextFieldState,
    onSubmit: (CharSequence) -> Unit,
    onBack: () -> Unit,
    viewModel: Lp3KeyboardViewModel<*>,
    modifier: Modifier = Modifier,
    submitLabel: String = "SUBMIT",
    submitIcon: LightIconConfiguration? = null,
    showBackButton: Boolean = true,
    singleLine: Boolean = false,
    inputTextStyle: TextStyle? = null,
    submitInTopBar: Boolean = false,
    topBarSubmitIcon: LightIconConfiguration? = null,
    topBarSubmitLabel: String? = null,
    bottomAligned: Boolean = false,
    centered: Boolean = false,
    submitOnReturn: Boolean? = null,
    submitBottomRight: Boolean = false,
    bottomBarLeadingButton: LightBarButton? = null,
    bottomBarCenterButton: LightBarButton? = null,
) {
    val colors = LightThemeTokens.colors
    val inputStyle = inputTextStyle ?: if (bottomAligned) {
        lightNotesInputTextStyle()
    } else {
        lightInputTextStyle()
    }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Surface {
        Column(modifier = modifier.fillMaxSize()) {
            LightTopBar(
                leftButton = if (showBackButton) {
                    LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = onBack,
                    )
                } else {
                    null
                },
                center = LightTopBarCenter.Text(title),
                // Notes-style editors (composer, recovery key) move the submit
                // action into the top bar so the keyboard sits flush at the
                // bottom and the input gets the full remaining space.
                rightButton = if (submitInTopBar) {
                    when {
                        topBarSubmitIcon != null -> LightBarButton.LightIcon(
                            icon = topBarSubmitIcon,
                            onClick = { onSubmit(state.text) },
                            contentDescription = topBarSubmitLabel ?: submitLabel,
                        )
                        else -> LightBarButton.Text(
                            text = topBarSubmitLabel ?: submitLabel,
                            onClick = { onSubmit(state.text) },
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 2f.gridUnitsAsDp()),
                // Top-aligned by default; Notes-style editors anchor the text
                // at the bottom (lines grow upward); centered vertically
                // between the top bar and the keyboard when [centered].
                contentAlignment = when {
                    centered -> Alignment.CenterStart
                    else -> Alignment.TopStart
                },
            ) {
                // Notes-style input (bottomAligned): short text sits at the
                // bottom and grows upward; once the draft outgrows the panel
                // it scrolls instead of spilling past the keyboard — pinned
                // to the newest line while typing, freely scrollable to read
                // earlier lines (feedback 2026-08-21). Other modes keep the
                // plain wrap-content column positioned by the outer Box.
                val scrollState = rememberScrollState()
                LaunchedEffect(Unit) {
                    snapshotFlow { scrollState.maxValue }
                        .distinctUntilChanged()
                        .collect { max -> if (max > 0) scrollState.scrollTo(max) }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (bottomAligned) {
                                Modifier.fillMaxHeight().verticalScroll(scrollState)
                            } else {
                                Modifier
                            },
                        ),
                    verticalArrangement = if (bottomAligned) Arrangement.Bottom else Arrangement.Top,
                ) {
                    // The text + cursor live in their own Box so the cursor
                    // offset math stays text-relative (bottom-anchored text is
                    // not at the outer Box's origin), and tap-to-place reads
                    // the same coordinates.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    textLayout?.let { layout ->
                                        state.edit {
                                            selection =
                                                TextRange(layout.getOffsetForPosition(down.position))
                                        }
                                    }
                                    drag(down.id) { change ->
                                        val delta = change.position - change.previousPosition
                                        // Only mostly-horizontal drags move the
                                        // cursor; vertical drags must pass
                                        // through to the scrollable draft so a
                                        // long message can be scrolled to read
                                        // (feedback 2026-08-22: the selection
                                        // gesture consumed every drag).
                                        if (abs(delta.y) <= abs(delta.x)) {
                                            textLayout?.let { layout ->
                                                state.edit {
                                                    selection = TextRange(
                                                        layout.getOffsetForPosition(change.position),
                                                    )
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            },
                    ) {
                        BasicText(
                            text = state.text.toString(),
                            style = inputStyle,
                            onTextLayout = { textLayout = it },
                            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                            softWrap = !singleLine,
                            overflow = if (singleLine) TextOverflow.StartEllipsis else TextOverflow.Clip,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        textLayout?.let { layout ->
                            val cursorPos = state.selection.min.coerceIn(0, layout.layoutInput.text.length)
                            val rect = layout.getCursorRect(cursorPos)
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                                    .width(2.dp)
                                    .height(with(LocalDensity.current) { rect.height.toDp() })
                                    .background(colors.content),
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(
                            INPUT_UNDERLINE_GAP_GRID_UNITS.gridUnitsAsDp(),
                        ),
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(INPUT_UNDERLINE_THICKNESS_PX.designVerticalPxToDp())
                            .background(colors.content),
                    )
                }
            }

            if (submitInTopBar) {
                // Notes-style: keyboard flush at the bottom, submit in the top
                // bar, maximum input space. The 5-gu bottom-bar row is still
                // reserved below the keys (LP3-verified 2026-08-21: the
                // composer's clear-draft X overlapped the keyboard's bottom
                // row when the keys ran flush to the screen edge). The zone
                // is empty here; apps like chats' composer place their action
                // (the X) there with a BottomEnd alignment.
                LightEmbeddedLp3Keyboard(
                    viewModel = viewModel,
                    additionalBottomHeight = 5f.gridUnitsAsDp(),
                )
            } else {
                LightEmbeddedLp3Keyboard(
                    viewModel = viewModel,
                    additionalBottomHeight = 5f.gridUnitsAsDp(),
                    bottomBar = {
                        // The action sits in a bottom bar below the keys; by
                        // default centered, [submitBottomRight] moves it to the
                        // right slot (the calendar-form corner-actions grammar —
                        // Passes' field editors, feedback 2026-08-24), and
                        // [bottomBarLeadingButton] adds a left-slot action.
                        LightBottomBar(
                            topPadding = 0.dp,
                            items = when {
                                submitBottomRight -> listOf(
                                    bottomBarLeadingButton,
                                    bottomBarCenterButton,
                                    submitBarButton(state, submitLabel, submitIcon, onSubmit),
                                )
                                bottomBarLeadingButton != null -> listOf(
                                    bottomBarLeadingButton,
                                    submitBarButton(state, submitLabel, submitIcon, onSubmit),
                                )
                                else -> listOf(
                                    submitBarButton(state, submitLabel, submitIcon, onSubmit),
                                )
                            },
                        )
                    }
                )
            }
        }
    }
}

/** The submit action rendered inside the editor's bottom bar (centered, or
 *  bottom-right via LightTextInputEditor's [LightTextInputEditor.submitBottomRight]). */
private fun submitBarButton(
    state: TextFieldState,
    submitLabel: String,
    submitIcon: LightIconConfiguration?,
    onSubmit: (CharSequence) -> Unit,
) = when (submitIcon) {
    null -> LightBarButton.Text(
        text = submitLabel,
        onClick = { onSubmit(state.text) },
    )
    else -> LightBarButton.LightIcon(
        icon = submitIcon,
        onClick = { onSubmit(state.text) },
        contentDescription = submitLabel,
    )
}

private fun factory(
    callback: Lp3RepeatableKeyboardCallback,
    keyboardOptionsFlow: StateFlow<KeyboardOptions>,
    initialCaps: Boolean,
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EnQwertyLp3KeyboardViewModel<Unit>(
                callback,
                keyboardOptionsFlow = keyboardOptionsFlow,
                optionsForLayout = {
                    val showCloseButton = !it.isRootLayout
                    LayoutOptions(showCloseButton)
                },
            ).apply {
                if (initialCaps) setCapsMode(true)
            } as T
        }

    }

@Composable
private fun lightInputTextStyle(): TextStyle {
    val colors = LightThemeTokens.colors
    val t = LightThemeTokens.typography
    return t.heading
        .copy(
            color = colors.content,
        )
        .scaledForScreenHeight()
}

/**
 * The Notes-style input: small text (paragraph) so the composer reads like the
 * LP3 Notes editor, whose lines sit just above the keyboard and grow upward.
 */
@Composable
private fun lightNotesInputTextStyle(): TextStyle {
    val colors = LightThemeTokens.colors
    val t = LightThemeTokens.typography
    return t.paragraph
        .copy(
            color = colors.content,
        )
        .scaledForScreenHeight()
}

@Preview(widthDp = 1080 / 3, heightDp = 1240 / 3, showBackground = true)
@Composable
private fun PreviewLightTextInputEditorDark() {
    val state = rememberTextFieldState("hi")
    LightTheme(colors = LightThemeColors.Dark) {
        LightTextInputEditor(
            title = "Name",
            state = state,
            keyboardOptionsFlow = MutableStateFlow(defaultKeyboardOptions()),
            onSubmit = {},
            onBack = {},
        )
    }
}

fun defaultKeyboardOptions() = KeyboardOptions(
    defaultEmojis,
    displayReturn = true,
    displayVoice = true,
    enableKeyAnimation = true,
    swipeEnabled = false
)
