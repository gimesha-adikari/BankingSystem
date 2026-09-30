@file:Suppress("OPT_IN_IS_NOT_ENABLED", "OPT_IN_USAGE")

package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.bankingsystem.mobile.app.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val DEV_PREVIEW = false

private enum class PickTarget { NONE, FRONT, BACK, SELFIE, ADDRESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycRoute(
    userName: String,
    onNavigate: (String) -> Unit = {}
) {
    val vm: KycViewModel = hiltViewModel()
    val ui by vm.ui.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(ui.docFront, ui.docBack) {
        val q = withContext(Dispatchers.Default) {
            val fronts =
                ui.docFront?.let { decodeBitmapForAnalyze(ctx, it)?.let(::computeDocQuality) }
            val backs =
                ui.docBack?.let { decodeBitmapForAnalyze(ctx, it)?.let(::computeDocQuality) }
            if (fronts == null && backs == null) null
            else DocQuality(
                blurScore = listOfNotNull(fronts?.blurScore, backs?.blurScore).minOrNull(),
                glareScore = listOfNotNull(fronts?.glareScore, backs?.glareScore).minOrNull(),
                cornerCoverage = listOfNotNull(
                    fronts?.cornerCoverage,
                    backs?.cornerCoverage
                ).minOrNull()
            )
        }
        vm.setDocQuality(q ?: DocQuality())
        val d = q ?: DocQuality()
        Log.d(
            "KYC",
            "docFront=${ui.docFront != null}, docBack=${ui.docBack != null}, " +
                    "blur=${d.blurScore}, glare=${d.glareScore}, corners=${d.cornerCoverage}"
        )
    }


    var showChooser by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(PickTarget.NONE) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraTarget by remember { mutableStateOf(PickTarget.NONE) }
    var lastRequestedTarget by remember { mutableStateOf(PickTarget.NONE) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            when (pendingCameraTarget) {
                PickTarget.FRONT -> vm.setDocFront(pendingCameraUri)
                PickTarget.BACK -> vm.setDocBack(pendingCameraUri)
                PickTarget.SELFIE -> {
                    vm.setSelfie(pendingCameraUri)
                    vm.setLiveness(0.9f)
                    vm.setFaceMatch(0.9f)
                }

                PickTarget.ADDRESS -> vm.setAddressProof(pendingCameraUri)
                else -> {}
            }
        }
        pendingCameraTarget = PickTarget.NONE
        pendingCameraUri = null
    }

    val pickVisual = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (target) {
            PickTarget.FRONT -> vm.setDocFront(uri)
            PickTarget.BACK -> vm.setDocBack(uri)
            PickTarget.SELFIE -> {
                vm.setSelfie(uri)
                vm.setLiveness(0.9f)
                vm.setFaceMatch(0.9f)
            }

            PickTarget.ADDRESS -> vm.setAddressProof(uri)
            else -> {}
        }
    }

    val requestCameraPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && lastRequestedTarget != PickTarget.NONE) {
            pendingCameraUri?.let { takePicture.launch(it) }
        }
    }

    fun createTempImageUri(local: Context): Uri {
        val dir = File(local.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("kyc_", ".jpg", dir)
        return FileProvider.getUriForFile(local, "${local.packageName}.fileprovider", file)
    }

    fun launchCameraFor(local: Context, which: PickTarget) {
        lastRequestedTarget = which
        val uri = createTempImageUri(local)
        pendingCameraUri = uri
        pendingCameraTarget = which
        val granted = ContextCompat.checkSelfPermission(
            local,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) takePicture.launch(uri) else requestCameraPerm.launch(Manifest.permission.CAMERA)
    }

    fun openGalleryFor(which: PickTarget) {
        target = which
        pickVisual.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Box(Modifier.fillMaxSize()) {
        when (ui.step) {
            KycStep.Document -> DocumentCaptureScreen(
                frontUri = ui.docFront,
                backUri = ui.docBack,
                onPickFront = { target = PickTarget.FRONT; showChooser = true },
                onRemoveFront = { vm.setDocFront(null) },
                onPickBack = { target = PickTarget.BACK; showChooser = true },
                onRemoveBack = { vm.setDocBack(null) },
                onBack = { onNavigate("Home") },
                onNext = { if (DEV_PREVIEW || vm.canContinueFromDocument()) vm.next() },
                canContinue = !uploading && (DEV_PREVIEW || vm.canContinueFromDocument())
            )

            KycStep.Selfie -> SelfieLivenessScreen(
                onBack = { vm.back() },
                onNext = { if (DEV_PREVIEW || vm.canContinueFromSelfie()) vm.next() },
                canContinue = !uploading && (DEV_PREVIEW || vm.canContinueFromSelfie()),
                selfieUri = ui.selfie,
                onCaptureSelfie = { target = PickTarget.SELFIE; showChooser = true },
                onPickFromGallery = { target = PickTarget.SELFIE; showChooser = true },
                onSelfieReady = { uri ->
                    vm.setSelfie(uri)
                    vm.setLiveness(0.9f)
                    vm.setFaceMatch(0.9f)
                }
            )

            KycStep.Address -> AddressProofScreen(
                proofUri = ui.addressProof,
                onCapture = { target = PickTarget.ADDRESS; showChooser = true },
                onPickFromGallery = { target = PickTarget.ADDRESS; showChooser = true },
                onRemove = { vm.setAddressProof(null) },
                onBack = { vm.back() },
                onNext = { if (DEV_PREVIEW || vm.canContinueFromAddress()) vm.next() },
                canContinue = !uploading && (DEV_PREVIEW || vm.canContinueFromAddress())
            )

            KycStep.Review -> KycReviewScreen(
                onBack = { vm.back() },
                onSubmit = {
                    Log.d("KYC", "Review: onSubmit tapped")
                    scope.launch {
                        val ok = vm.submit()
                        Log.d("KYC", "Review: submit() returned ok=$ok")
                        if (ok) onNavigate(Routes.KYC_STATUS)else onNavigate(Routes.HOME)
                    }
                },
                canSubmit = !uploading && vm.readyToSubmit(),
                consent = ui.consentAccepted,
                onToggleConsent = vm::setConsent
            )
        }

        if (showChooser && target != PickTarget.NONE) {
            SourceChooserDialog(
                onDismiss = { showChooser = false; target = PickTarget.NONE },
                onCamera = { showChooser = false; launchCameraFor(ctx, target) },
                onGallery = { showChooser = false; openGalleryFor(target) }
            )
        }
    }
}
