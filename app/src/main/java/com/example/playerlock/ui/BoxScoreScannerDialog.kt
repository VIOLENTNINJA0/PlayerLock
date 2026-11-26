package com.example.playerlock.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.playerlock.data.BoxScoreOcrParser
import com.example.playerlock.data.ScannedBoxScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BoxScoreScannerDialog(
    onDismiss: () -> Unit,
    onScanSuccess: (ScannedBoxScore) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    var isCapturing by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            scanError = "Camera permission is required to scan box scores."
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(previewViewRef, hasCameraPermission) {
        val view = previewViewRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            cameraProvider = provider

            val preview = Preview.Builder().build()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            imageCapture = capture
            preview.setSurfaceProvider(view.surfaceProvider)
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
        } catch (e: Exception) {
            scanError = "Camera error: ${e.localizedMessage}"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Dialog(
        onDismissRequest = {
            cameraProvider?.unbindAll()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Live CameraX Preview
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    update = { view ->
                        previewViewRef = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Custom Viewfinder Canvas Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenWidth = size.width
                val screenHeight = size.height
                val windowWidth = screenWidth * 0.92f
                val windowHeight = 76.dp.toPx()

                val windowLeft = (screenWidth - windowWidth) / 2f
                val windowTop = (screenHeight - windowHeight) / 2f
                val windowRight = windowLeft + windowWidth
                val windowBottom = windowTop + windowHeight
                val dimColor = Color.Black.copy(alpha = 0.70f)

                // Dim top area
                drawRect(dimColor, topLeft = Offset(0f, 0f), size = Size(screenWidth, windowTop))
                // Dim bottom area
                drawRect(dimColor, topLeft = Offset(0f, windowBottom), size = Size(screenWidth, screenHeight - windowBottom))
                // Dim left area
                drawRect(dimColor, topLeft = Offset(0f, windowTop), size = Size(windowLeft, windowHeight))
                // Dim right area
                drawRect(dimColor, topLeft = Offset(windowRight, windowTop), size = Size(screenWidth - windowRight, windowHeight))

                // Target window border (2.dp border) outlined with primary color
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(windowLeft, windowTop),
                    size = Size(windowWidth, windowHeight),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Subtle horizontal center alignment line
                drawLine(
                    color = primaryColor.copy(alpha = 0.45f),
                    start = Offset(windowLeft + 16.dp.toPx(), screenHeight / 2f),
                    end = Offset(windowRight - 16.dp.toPx(), screenHeight / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            // Overlay UI components
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row with Close X Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCAN BOX SCORE ROW",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = {
                            cameraProvider?.unbindAll()
                            onDismiss()
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Scanner",
                            tint = Color.White
                        )
                    }
                }

                // Helper Text centered directly above the viewfinder bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Align your player's stat row inside the gold bar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom Controls: Inline Error message + Capture Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    scanError?.let { errorText ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Text(
                                    text = errorText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (isCapturing) return@Button
                            val capture = imageCapture ?: return@Button
                            isCapturing = true
                            scanError = null

                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        scope.launch(Dispatchers.Default) {
                                            try {
                                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                                val bitmap = imageProxy.toBitmap()
                                                imageProxy.close()

                                                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                val rotated = Bitmap.createBitmap(
                                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                                )
                                                if (rotated != bitmap) bitmap.recycle()

                                                val cropWidth = (rotated.width * 0.92f).toInt()
                                                val cropHeight = (rotated.height * 0.11f).toInt()
                                                val cropLeft = ((rotated.width - cropWidth) / 2).coerceAtLeast(0)
                                                val cropTop = ((rotated.height - cropHeight) / 2).coerceAtLeast(0)

                                                val safeWidth = cropWidth.coerceAtMost(rotated.width - cropLeft)
                                                val safeHeight = cropHeight.coerceAtMost(rotated.height - cropTop)

                                                val croppedBitmap = Bitmap.createBitmap(
                                                    rotated, cropLeft, cropTop, safeWidth, safeHeight
                                                )
                                                if (croppedBitmap != rotated) rotated.recycle()

                                                val parseResult = BoxScoreOcrParser.parseCroppedRow(croppedBitmap)
                                                croppedBitmap.recycle()

                                                withContext(Dispatchers.Main) {
                                                    isCapturing = false
                                                    parseResult.onSuccess { scanned ->
                                                        val hasNumbers = scanned.pts > 0 || scanned.reb > 0 || scanned.ast > 0 ||
                                                                scanned.stl > 0 || scanned.blk > 0 || scanned.tov > 0 ||
                                                                scanned.fgm > 0 || scanned.fga > 0 || scanned.ftm > 0 || scanned.fta > 0
                                                        if (hasNumbers) {
                                                            cameraProvider?.unbindAll()
                                                            onScanSuccess(scanned)
                                                            onDismiss()
                                                        } else {
                                                            scanError = "Could not read stats — hold steady and line up the row inside the bar."
                                                        }
                                                    }.onFailure {
                                                        scanError = "Could not read stats — hold steady and line up the row inside the bar."
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                imageProxy.close()
                                                withContext(Dispatchers.Main) {
                                                    isCapturing = false
                                                    scanError = "Could not read stats — hold steady and line up the row inside the bar."
                                                }
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        scanError = "Could not read stats — hold steady and line up the row inside the bar."
                                    }
                                }
                            )
                        },
                        enabled = !isCapturing && imageCapture != null,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Processing Row...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Capture Row", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
