package com.bankingsystem.mobile.features.kyc.interfaces.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal fun createImageUri(ctx: Context, name: String): Uri {
    val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "${name}_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}

fun decodeBitmapForAnalyze(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? {
    val resolver = context.contentResolver
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    val w = opts.outWidth
    val h = opts.outHeight
    if (w <= 0 || h <= 0) return null
    var sample = 1
    var largest = max(w, h)
    while (largest / sample > maxDim) sample *= 2
    val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
    val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts2) } ?: return null
    val rotated = try {
        val orientation = resolver.openInputStream(uri)?.use { ins ->
            ExifInterface(ins).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        if (!matrix.isIdentity) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true) else bmp
    } catch (_: Exception) {
        bmp
    }
    return rotated
}

fun computeDocQuality(bmp: Bitmap): DocQuality {
    val w = bmp.width
    val h = bmp.height
    val px = IntArray(w * h)
    bmp.getPixels(px, 0, w, 0, 0, w, h)
    val g = IntArray(px.size)
    for (i in px.indices) {
        val p = px[i]
        val r = (p shr 16) and 0xFF
        val gch = (p shr 8) and 0xFF
        val b = p and 0xFF
        g[i] = (0.299 * r + 0.587 * gch + 0.114 * b).toInt()
    }
    val blurScore = laplacianVarianceScore(g, w, h)
    val glareScore = 1f - brightFraction(g, threshold = 240)
    val corners = cornerCoverageCount(g, w, h)
    return DocQuality(
        blurScore = blurScore,
        glareScore = glareScore.coerceIn(0f, 1f),
        cornerCoverage = corners
    )
}

private fun laplacianVarianceScore(gray: IntArray, w: Int, h: Int): Float {
    var sum = 0.0
    var sumSq = 0.0
    var n = 0
    for (y in 1 until h - 1) {
        val yi = y * w
        for (x in 1 until w - 1) {
            val i = yi + x
            val v = (-4 * gray[i] +
                    gray[i - 1] + gray[i + 1] +
                    gray[i - w] + gray[i + w]).toDouble()
            sum += v
            sumSq += v * v
            n++
        }
    }
    if (n == 0) return 0f
    val mean = sum / n
    val variance = (sumSq / n) - mean * mean
    val norm = (variance / 2500.0).coerceIn(0.0, 1.0)
    return norm.toFloat()
}

private fun brightFraction(gray: IntArray, threshold: Int): Float {
    var bright = 0
    for (v in gray) if (v >= threshold) bright++
    return bright.toFloat() / gray.size.toFloat()
}

private fun cornerCoverageCount(gray: IntArray, w: Int, h: Int): Int {
    val pw = max(4, (w * 0.15f).toInt())
    val ph = max(4, (h * 0.15f).toInt())
    fun stddev(x0: Int, y0: Int): Double {
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in y0 until min(h, y0 + ph)) {
            var i = y * w + x0
            for (x in x0 until min(w, x0 + pw)) {
                val v = gray[i].toDouble()
                sum += v
                sumSq += v * v
                n++
                i++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        val varc = (sumSq / n) - mean * mean
        return sqrt(max(0.0, varc))
    }
    var count = 0
    if (stddev(0, 0) > 12) count++
    if (stddev(w - pw, 0) > 12) count++
    if (stddev(0, h - ph) > 12) count++
    if (stddev(w - pw, h - ph) > 12) count++
    return count
}
