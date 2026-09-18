package com.bankingsystem.mobile.features.wallet.interfaces.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun QrPayScanScreen(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onScanned: (QrPreview) -> Unit
) {
    val ctx = LocalContext.current
    val granted = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted.value = ok
    }
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    val manual = remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Scan QR", style = MaterialTheme.typography.headlineSmall)
        if (granted.value) {
            CameraPreview(modifier = Modifier.weight(1f)) { raw ->
                val preview = parseQr(raw)
                onScanned(preview)
            }
        } else {
            OutlinedTextField(
                value = manual.value,
                onValueChange = { manual.value = it },
                label = { Text("Enter QR data manually") },
                modifier = Modifier.fillMaxSize().weight(1f)
            )
            Button(onClick = {
                val p = parseQr(manual.value.ifBlank { "merchantName=Store&amount=0&memo=" })
                onScanned(p)
            }) { Text("Use this data") }
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onRaw: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analyzer = remember {
        object : ImageAnalysis.Analyzer {
            override fun analyze(image: ImageProxy) {
                image.close()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val pv = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val provider = ProcessCameraProvider.getInstance(ctx).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().build().apply { setAnalyzer(Dispatchers.Default.asExecutor(), analyzer) }
            provider.unbindAll()
            provider.bindToLifecycle(context.findLifecycleOwner(), androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            pv
        }
    )
}

private fun Context.findLifecycleOwner(): androidx.lifecycle.LifecycleOwner {
    return this as androidx.lifecycle.LifecycleOwner
}

private data class PairKV(val k: String, val v: String)

private fun parseQr(raw: String): QrPreview {
    val parts = raw.split('&', ';', '?')
    val map = buildMap {
        for (p in parts) {
            val i = p.indexOf('=')
            if (i > 0) {
                val k = p.substring(0, i).lowercase()
                val v = Uri.decode(p.substring(i + 1))
                put(k, v)
            }
        }
    }
    val merchant = map["merchantname"] ?: map["merchant"] ?: "Merchant"
    val amount = map["amount"] ?: "0"
    val memo = map["memo"] ?: map["note"] ?: ""
    return QrPreview(merchantName = merchant, amount = amount, memo = memo)
}
