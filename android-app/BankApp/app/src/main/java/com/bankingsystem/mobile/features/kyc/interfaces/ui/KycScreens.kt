package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.net.Uri
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.bankingsystem.mobile.core.modules.common.designsystem.FadingAppBackground
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KycScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(Modifier) {
        FadingAppBackground()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Surface(color = Color.Transparent, tonalElevation = 0.dp, shadowElevation = 0.dp) {
                    bottomBar()
                }
            },
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCaptureScreen(
    frontUri: Uri?,
    backUri: Uri?,
    onPickFront: () -> Unit,
    onRemoveFront: () -> Unit,
    onPickBack: () -> Unit,
    onRemoveBack: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean = false
) {
    KycScaffold(
        title = "Verify your ID",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = onNext,
                enabled = canContinue,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) { Text("Continue") }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepHeaderRow(current = KycStep.Document)
            KycPanel {
                Text("Capture both sides of your national ID.", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DocSlot("Front side", frontUri, onPickFront, onRemoveFront, Modifier.weight(1f))
                    DocSlot("Back side", backUri, onPickBack, onRemoveBack, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) { Text("Back") }
                Spacer(Modifier.height(10.dp))
                if (canContinue) Text("Looks good • tap Continue", color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
private fun DocSlot(
    title: String,
    uri: Uri?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val frame = RoundedCornerShape(18.dp)
    val grad = Brush.linearGradient(
        listOf(
            Color(0xFF5B67F3).copy(alpha = 0.22f),
            Color(0xFF5B67F3).copy(alpha = 0.08f)
        )
    )
    Surface(
        modifier = modifier,
        shape = frame,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(grad, frame)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            if (uri == null) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(0.15f))
                ) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        OutlinedButton(onClick = onPick) {
                            Icon(Icons.Filled.AddAPhoto, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Add photo", color = Color.White)
                        }
                    }
                }
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Remove", color = Color.White)
                    }
                }
            }
        }
    }
}

@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGetImage::class)
@Composable
fun SelfieLivenessScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean = true,
    selfieUri: Uri? = null,
    onCaptureSelfie: () -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onSelfieReady: (Uri) -> Unit = {}
) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current

    // --- NEW: keep refs so we can unbind on dispose ---
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var boundPreview by remember { mutableStateOf<Preview?>(null) }
    var boundImageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }

    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var capturing by remember { mutableStateOf(false) }
    val needCenteredFrames = 8
    val centerTolX = 0.25f
    val centerTolY = 0.28f
    val blinkCloseThr = 0.45f
    val blinkOpenThr = 0.55f
    val yawWindowSize = 18
    val yawMinRange = 3f
    val yawMaxRange = 35f
    var centeredFrames by remember { mutableIntStateOf(0) }
    var lastLeftOpen by remember { mutableStateOf<Boolean?>(null) }
    var lastRightOpen by remember { mutableStateOf<Boolean?>(null) }
    var blinked by remember { mutableStateOf(false) }
    val yawWindow = remember { ArrayDeque<Float>() }

    val previewView = remember(ctx) { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
    }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    LaunchedEffect(owner) {
        try {
            val provider = withContext(Dispatchers.Default) { ProcessCameraProvider.getInstance(ctx).get() }
            cameraProvider = provider

            val front = CameraSelector.DEFAULT_FRONT_CAMERA
            val back  = CameraSelector.DEFAULT_BACK_CAMERA
            val selector = if (provider.hasCameraSafe(front)) front else back
            if (!provider.hasCameraSafe(selector)) {
                error = "No available camera can be found"
                return@LaunchedEffect
            }

            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val mainExec = ContextCompat.getMainExecutor(ctx)

            val analyzer = ImageAnalysis.Analyzer { proxy ->
                val media = proxy.image ?: run { proxy.close(); return@Analyzer }
                val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                detector.process(image)
                    .addOnSuccessListener(mainExec) { faces: List<Face> ->
                        if (faces.size == 1) {
                            val f = faces[0]
                            val w = image.width.toFloat()
                            val h = image.height.toFloat()
                            val cx = f.boundingBox.centerX() / w
                            val cy = f.boundingBox.centerY() / h
                            val centered = abs(cx - 0.5f) < centerTolX && abs(cy - 0.5f) < centerTolY
                            centeredFrames = if (centered) (centeredFrames + 1).coerceAtMost(60) else (centeredFrames - 2).coerceAtLeast(0)
                            val centerOk = centeredFrames >= needCenteredFrames

                            val le = f.leftEyeOpenProbability
                            val re = f.rightEyeOpenProbability
                            val openNow = (le != null && le > blinkOpenThr) || (re != null && re > blinkOpenThr)
                            val closedNow = (le != null && le < blinkCloseThr) || (re != null && re < blinkCloseThr)
                            val wasOpen = (lastLeftOpen == true) || (lastRightOpen == true)
                            if (!blinked && wasOpen && closedNow) blinked = true
                            lastLeftOpen = le?.let { it > blinkOpenThr }
                            lastRightOpen = re?.let { it > blinkOpenThr }
                            val blinkOk = blinked && openNow

                            val yaw = f.headEulerAngleY
                            yawWindow.addLast(yaw)
                            if (yawWindow.size > yawWindowSize) yawWindow.removeFirst()
                            val minYaw = yawWindow.minOrNull() ?: 0f
                            val maxYaw = yawWindow.maxOrNull() ?: 0f
                            val yawRange = maxYaw - minYaw
                            val moveOk = yawRange in yawMinRange..yawMaxRange

                            val p1 = if (centerOk) 1f else centeredFrames / needCenteredFrames.toFloat()
                            val p2 = if (blinkOk) 1f else 0f
                            val p3 = if (moveOk) 1f else (yawRange / yawMinRange).coerceIn(0f, 1f)
                            progress = (p1 + p2 + p3) / 3f

                            val satisfied = listOf(centerOk, blinkOk, moveOk).count { it } >= 2
                            if (satisfied && !capturing) {
                                capturing = true
                                val out = createTempImageFile(ctx)
                                val opts = ImageCapture.OutputFileOptions.Builder(out).build()
                                imageCapture.takePicture(
                                    opts,
                                    mainExec,
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            onSelfieReady(Uri.fromFile(out))
                                        }
                                        override fun onError(exc: ImageCaptureException) {
                                            error = exc.message
                                            capturing = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                    .addOnFailureListener(mainExec) { e -> error = e.message }
                    .addOnCompleteListener(mainExec) { proxy.close() }
            }

            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(mainExec, analyzer) }

            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, preview, imageCapture, analysis)

            // remember bound use cases for cleanup
            boundPreview = preview
            boundImageCapture = imageCapture
            boundAnalysis = analysis

            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to start camera"
        }
    }

    // --- NEW: clean up when leaving composition / owner changes / app goes background ---
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                cameraProvider?.let { prov ->
                    boundAnalysis?.clearAnalyzer()
                    runCatching { prov.unbindAll() }
                }
            }
        }
        owner.lifecycle.addObserver(obs)

        onDispose {
            owner.lifecycle.removeObserver(obs)
            boundAnalysis?.clearAnalyzer()

            detector.close()

            cameraProvider?.let { prov ->
                val useCases = listOfNotNull(boundPreview, boundImageCapture, boundAnalysis).toTypedArray()
                runCatching {
                    if (useCases.isNotEmpty()) prov.unbind(*useCases) else prov.unbindAll()
                }
            }

            boundPreview = null
            boundImageCapture = null
            boundAnalysis = null
            cameraProvider = null
        }
    }


    KycScaffold(
        title = "Face verification",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = onNext,
                enabled = canContinue,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) { Text("Continue") }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepHeaderRow(current = KycStep.Selfie)
            KycPanel {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
                Text("Center your face in the frame", color = Color.White)
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPickFromGallery) { Text("Pick from gallery") }
                    OutlinedButton(onClick = onCaptureSelfie, enabled = error == null) { Text("Capture") }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressProofScreen(
    proofUri: Uri? = null,
    onCapture: () -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onRemove: () -> Unit = {},
    onBack: () -> Unit,
    onNext: () -> Unit,
    canContinue: Boolean = true
) {
    KycScaffold(
        title = "Proof of address",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = onNext,
                enabled = canContinue,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) { Text("Continue") }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepHeaderRow(current = KycStep.Address)
            KycPanel {
                val grad = Brush.linearGradient(
                    listOf(
                        Color(0xFF5B67F3).copy(alpha = 0.22f),
                        Color(0xFF5B67F3).copy(alpha = 0.08f)
                    )
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    color = Color.Transparent
                ) {
                    Box(
                        Modifier
                            .background(grad, RoundedCornerShape(18.dp))
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (proofUri == null) {
                            Text("Upload / capture utility bill, lease, etc.", color = Color.White)
                        } else {
                            AsyncImage(
                                model = proofUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPickFromGallery) { Text("Pick from gallery") }
                    OutlinedButton(onClick = onCapture) { Text("Capture") }
                    if (proofUri != null) OutlinedButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycReviewScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    consent: Boolean,
    onToggleConsent: (Boolean) -> Unit
) {
    KycScaffold(
        title = "Review & submit",
        onBack = onBack,
        bottomBar = {
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) { Text("Submit") }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StepHeaderRow(current = KycStep.Review)
            KycPanel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("KYC summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF34D399))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = onToggleConsent)
                    Text("I confirm the information is accurate and I consent to verification.", color = Color.White)
                }
            }
        }
    }
}
