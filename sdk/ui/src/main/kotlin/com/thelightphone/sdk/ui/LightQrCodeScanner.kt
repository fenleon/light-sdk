package com.thelightphone.sdk.ui

import android.Manifest
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.markusfisch.android.zxingcpp.ZxingCpp
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The decoded symbol's exact module grid: width, height, and one byte per
 * module (0 = black). Captured at scan time so tools can render the original
 * symbol (e.g. an airline Aztec whose re-encode differs) instead of
 * re-encoding the payload.
 */
data class LightSymbol(
    val width: Int,
    val height: Int,
    val data: ByteArray,
)

/**
 * A barcode decoded by the scanner: the decoded text value, the format as a
 * lowercase name (e.g. "qr", "aztec", "pdf417", "code128"), the raw payload
 * bytes (present when the payload is binary rather than text, e.g. an Aztec
 * ticketing code), and the exact decoded symbol grid when the decoder
 * captured it.
 */
data class LightScannedBarcode(
    val value: String,
    val formatName: String,
    val rawBytes: ByteArray? = null,
    /** The exact decoded symbol grid, when the scanner captured it. */
    val symbol: LightSymbol? = null,
)

/**
 * Full-screen barcode scanner: live camera preview, dimmed overlay with
 * viewfinder, top-bar back, and [onScanned] with the decoded string.
 *
 * Host apps must declare [Manifest.permission.CAMERA].
 *
 * When handling [onScanned], defer navigation to a [LaunchedEffect] (or similar)
 * and pop the scanner before pushing the next screen, e.g. `goBack()` then `navigateTo(...)`.
 */
@Composable
fun LightQrCodeScanner(
    onScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Scan QR Code",
    checkCameraPermission: suspend () -> Result<Boolean>,
    launchCameraPermissionRequest: suspend () -> Unit,
) {
    LightQrCodeScanner(
        onScanned = { onScanned(it.value) },
        onBack = onBack,
        modifier = modifier,
        title = title,
        formats = FORMAT_QR_CODE,
        checkCameraPermission = checkCameraPermission,
        launchCameraPermissionRequest = launchCameraPermissionRequest,
    )
}

/**
 * Full-screen barcode scanner for any [formats] zxing-cpp supports (QR, Aztec,
 * PDF417, Data Matrix, Code 128, EAN/UPC, ... — the `formats` bitmask keeps
 * the ML Kit `Barcode.FORMAT_*` values for API compatibility (see below). [onScanned] receives the
 * decoded code with its format, raw bytes, and exact decoded symbol grid.
 *
 * Host apps must declare [Manifest.permission.CAMERA].
 *
 * When handling [onScanned], defer navigation to a [LaunchedEffect] (or similar)
 * and pop the scanner before pushing the next screen, e.g. `goBack()` then `navigateTo(...)`.
 */
@Composable
fun LightQrCodeScanner(
    onScanned: (LightScannedBarcode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Scan Code",
    formats: Int = FORMAT_ALL_FORMATS,
    checkCameraPermission: suspend () -> Result<Boolean>,
    launchCameraPermissionRequest: suspend () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = LightThemeTokens.colors
    var launchedPermissionRequest by remember { mutableStateOf(false) }
    var uiState by remember { mutableStateOf(LightQrUiState.Loading) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val permissionCheck = checkCameraPermission()
            if (permissionCheck.isFailure) {
                uiState = LightQrUiState.PermissionError
            } else if (permissionCheck.getOrNull() == false) {
                if (!launchedPermissionRequest) {
                    launchCameraPermissionRequest()
                    launchedPermissionRequest = true
                } else {
                    uiState = LightQrUiState.PermissionDenied
                }
            } else {
                uiState = LightQrUiState.Active
            }
        }
    }

    val scannedOnce = remember { AtomicBoolean(false) }
    val onScannedState = rememberUpdatedState(onScanned)
    val onBackState = rememberUpdatedState(onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (uiState == LightQrUiState.Active) {
            QrCameraPreview(
                onScanned = { decoded ->
                    if (scannedOnce.compareAndSet(false, true)) {
                        onScannedState.value(decoded)
                    }
                },
                formats = formats,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize(),
            )
            QrViewfinderOverlay(
                frameColor = colors.content,
                scrimColor = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { onBackState.value() },
                    ),
                    center = LightTopBarCenter.Text(title),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )
            }

            if (uiState != LightQrUiState.Active) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState == LightQrUiState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        val message = if (uiState == LightQrUiState.PermissionDenied) {
                            "Camera permission is required to scan QR codes."
                        } else {
                            "Error: unable to request camera permission!"
                        }
                        LightText(
                            text = message,
                            variant = LightTextVariant.Copy,
                            align = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 2f.gridUnitsAsDp()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    onScanned: (LightScannedBarcode) -> Unit,
    formats: Int,
    lifecycleOwner: LifecycleOwner,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val onScannedState = rememberUpdatedState(onScanned)

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(lifecycleOwner, cameraController) {
        // Decode off the main thread: zxing-cpp runs native code per frame and
        // the analyzer is invoked sequentially at up to 30 fps.
        val analyzerExecutor = Executors.newSingleThreadExecutor()
        val readerOptions = zxingReaderOptions(formats)
        val analyzer = ImageAnalysis.Analyzer { image ->
            try {
                val decoded = runCatching {
                    val yPlane = image.planes[0]
                    ZxingCpp.readYBuffer(
                        yPlane.buffer,
                        yPlane.rowStride,
                        image.cropRect,
                        image.imageInfo.rotationDegrees,
                        readerOptions,
                    )
                }.getOrNull()
                    ?.asSequence()
                    ?.mapNotNull { it.toLightBarcode() }
                    ?.firstOrNull()
                if (decoded != null) {
                    onScannedState.value(decoded)
                }
            } finally {
                image.close()
            }
        }
        cameraController.setImageAnalysisAnalyzer(analyzerExecutor, analyzer)

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                controller = cameraController
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            bindCamera(cameraController, lifecycleOwner)
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            cameraController.unbind()
                        }
                    },
                )
            }
        },
        update = { previewView ->
            if (previewView.isAttachedToWindow) {
                previewView.post {
                    bindCamera(cameraController, lifecycleOwner)
                }
            }
        },
    )
}

private fun bindCamera(
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
) {
    runCatching {
        cameraController.bindToLifecycle(lifecycleOwner)
    }
}

@Composable
private fun QrViewfinderOverlay(
    frameColor: Color,
    scrimColor: Color,
    modifier: Modifier = Modifier,
    frameSizeFraction: Float = 0.62f,
) {
    Canvas(modifier = modifier) {
        val frameSize = size.minDimension * frameSizeFraction
        val left = (size.width - frameSize) / 2f
        val top = (size.height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize

        drawRect(scrimColor, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(scrimColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(scrimColor, topLeft = Offset(0f, top), size = Size(left, frameSize))
        drawRect(scrimColor, topLeft = Offset(right, top), size = Size(size.width - right, frameSize))

        drawRoundRect(
            color = frameColor,
            topLeft = Offset(left, top),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 3f),
        )
    }
}

private enum class LightQrUiState {
    Loading, PermissionError, PermissionDenied, Active
}

// ML Kit's Barcode.FORMAT_* bitmask values. The scanner used to decode with ML
// Kit; the SDK dropped that dependency, the public `formats` API did not — the
// default 0 = all formats, each bit selects a family (kept stable on purpose).
private const val FORMAT_ALL_FORMATS = 0
private const val FORMAT_CODE_128 = 1
private const val FORMAT_CODE_39 = 2
private const val FORMAT_CODE_93 = 4
private const val FORMAT_CODABAR = 8
private const val FORMAT_DATA_MATRIX = 16
private const val FORMAT_EAN_13 = 32
private const val FORMAT_EAN_8 = 64
private const val FORMAT_ITF = 128
private const val FORMAT_QR_CODE = 256
private const val FORMAT_UPC_A = 512
private const val FORMAT_UPC_E = 1024
private const val FORMAT_PDF417 = 2048
private const val FORMAT_AZTEC = 4096

/** ML Kit format bitmask (kept for API compatibility) → zxing-cpp formats. */
private fun zxingReaderOptions(formats: Int): ZxingCpp.ReaderOptions =
    ZxingCpp.ReaderOptions().apply {
        // An empty set decodes all formats; only restrict when the caller
        // asked for a specific ML Kit bitmask (FORMAT_ALL_FORMATS = 0).
        this.formats = zxingFormats(formats)
    }

private fun zxingFormats(formats: Int): Set<ZxingCpp.BarcodeFormat> {
    if (formats <= 0) return emptySet() // FORMAT_ALL_FORMATS / FORMAT_UNKNOWN → all
    val result = mutableSetOf<ZxingCpp.BarcodeFormat>()
    if (formats and FORMAT_AZTEC != 0) result += ZxingCpp.BarcodeFormat.Aztec
    if (formats and FORMAT_PDF417 != 0) result += ZxingCpp.BarcodeFormat.PDF417
    if (formats and FORMAT_DATA_MATRIX != 0) result += ZxingCpp.BarcodeFormat.DataMatrix
    if (formats and FORMAT_QR_CODE != 0) result += ZxingCpp.BarcodeFormat.QRCode
    if (formats and FORMAT_CODE_128 != 0) result += ZxingCpp.BarcodeFormat.Code128
    if (formats and FORMAT_CODE_39 != 0) result += ZxingCpp.BarcodeFormat.Code39
    if (formats and FORMAT_CODE_93 != 0) result += ZxingCpp.BarcodeFormat.Code93
    if (formats and FORMAT_CODABAR != 0) result += ZxingCpp.BarcodeFormat.Codabar
    if (formats and FORMAT_EAN_13 != 0) result += ZxingCpp.BarcodeFormat.EAN13
    if (formats and FORMAT_EAN_8 != 0) result += ZxingCpp.BarcodeFormat.EAN8
    if (formats and FORMAT_UPC_A != 0) result += ZxingCpp.BarcodeFormat.UPCA
    if (formats and FORMAT_UPC_E != 0) result += ZxingCpp.BarcodeFormat.UPCE
    if (formats and FORMAT_ITF != 0) result += ZxingCpp.BarcodeFormat.ITF
    return result
}

/** zxing-cpp decode result → [LightScannedBarcode], keeping the exact symbol. */
private fun ZxingCpp.Result.toLightBarcode(): LightScannedBarcode? {
    val value = text.takeIf { it.isNotBlank() } ?: return null
    return LightScannedBarcode(
        value = value,
        formatName = format.toFormatName(),
        rawBytes = rawBytes,
        symbol = symbol?.let { LightSymbol(it.width, it.height, it.data) },
    )
}

/** zxing-cpp format → lowercase name (matches the Passes format vocabulary). */
private fun ZxingCpp.BarcodeFormat.toFormatName(): String = when (this) {
    ZxingCpp.BarcodeFormat.Aztec -> "aztec"
    ZxingCpp.BarcodeFormat.PDF417 -> "pdf417"
    ZxingCpp.BarcodeFormat.DataMatrix -> "datamatrix"
    ZxingCpp.BarcodeFormat.QRCode -> "qr"
    ZxingCpp.BarcodeFormat.Code128 -> "code128"
    ZxingCpp.BarcodeFormat.Code39 -> "code39"
    ZxingCpp.BarcodeFormat.Code93 -> "code93"
    ZxingCpp.BarcodeFormat.Codabar -> "codabar"
    ZxingCpp.BarcodeFormat.EAN13 -> "ean13"
    ZxingCpp.BarcodeFormat.EAN8 -> "ean8"
    ZxingCpp.BarcodeFormat.UPCA -> "upc_a"
    ZxingCpp.BarcodeFormat.UPCE -> "upc_e"
    ZxingCpp.BarcodeFormat.ITF -> "itf14"
    else -> name.lowercase()
}
