package com.dyremark.dailywallpaper.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Decodes an image (downsampled to the wallpaper size) and applies it via [WallpaperManager]. */
class WallpaperSetter(private val context: Context) {

    suspend fun setWallpaper(imageUri: Uri, target: WallpaperTarget): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val wm = WallpaperManager.getInstance(context)
                val targetW = wm.desiredMinimumWidth.takeIf { it > 0 }
                    ?: context.resources.displayMetrics.widthPixels
                val targetH = wm.desiredMinimumHeight.takeIf { it > 0 }
                    ?: context.resources.displayMetrics.heightPixels

                val bitmap = decodeDownsampled(imageUri, targetW, targetH)
                    ?: error("Could not decode image: $imageUri")
                val oriented = applyExifOrientation(imageUri, bitmap)
                // Reduce to exactly the wallpaper size so the system isn't handed a huge bitmap.
                val finalBitmap = scaleAndCenterCrop(oriented, targetW, targetH)

                wm.setBitmap(finalBitmap, null, true, target.toFlags())
                finalBitmap.recycle()
            }
        }

    /** Decodes the image scaled down so its smaller dimension still covers the target. */
    private fun decodeDownsampled(uri: Uri, targetW: Int, targetH: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetW, targetH)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var sample = 1
        var halfW = srcW / 2
        var halfH = srcH / 2
        // Largest power of two keeping both dimensions >= the requested size.
        while (halfW >= reqW && halfH >= reqH) {
            sample *= 2
            halfW /= 2
            halfH /= 2
        }
        return max(1, sample)
    }

    /** Scales [src] to cover [targetW]x[targetH] and center-crops it to exactly that size. */
    private fun scaleAndCenterCrop(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val scale = max(targetW.toFloat() / src.width, targetH.toFloat() / src.height)
        val scaledW = max(1, (src.width * scale).roundToInt())
        val scaledH = max(1, (src.height * scale).roundToInt())

        val scaled = if (scaledW == src.width && scaledH == src.height) {
            src
        } else {
            Bitmap.createScaledBitmap(src, scaledW, scaledH, true)
        }
        if (scaled !== src) src.recycle()

        val cropW = min(targetW, scaledW)
        val cropH = min(targetH, scaledH)
        val x = ((scaledW - cropW) / 2).coerceAtLeast(0)
        val y = ((scaledH - cropH) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(scaled, x, y, cropW, cropH)
        if (cropped !== scaled) scaled.recycle()
        return cropped
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun WallpaperTarget.toFlags(): Int = when (this) {
        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
        WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
        WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    }
}
