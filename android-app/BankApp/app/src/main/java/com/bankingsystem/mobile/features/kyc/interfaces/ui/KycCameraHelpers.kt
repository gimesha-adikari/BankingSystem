package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.content.Context
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal fun ProcessCameraProvider.hasCameraSafe(selector: CameraSelector): Boolean =
    try { hasCamera(selector) } catch (_: CameraInfoUnavailableException) { false }

internal fun createTempImageFile(context: Context): File {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    return File(dir, "selfie_$ts.jpg")
}

internal fun ArrayDeque<Float>.minVal(): Float =
    this.fold(Float.POSITIVE_INFINITY) { acc, v -> min(acc, v) }

internal fun ArrayDeque<Float>.maxVal(): Float =
    this.fold(Float.NEGATIVE_INFINITY) { acc, v -> max(acc, v) }
