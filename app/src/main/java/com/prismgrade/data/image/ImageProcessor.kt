package com.prismgrade.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/** A photo that is ready to send: JPEG bytes, base64, and a copy kept on disk. */
data class PreparedImage(
    val base64: String,
    val localFile: File,
    val width: Int,
    val height: Int,
)

/**
 * Decodes a captured or picked photo, applies its EXIF rotation, scales it down
 * and re-encodes it as JPEG.
 *
 * Scaling matters twice over: the API rejects oversized images, and a
 * straight-from-camera 12MP file is mostly upload time for detail the model
 * doesn't use. [MAX_EDGE] keeps enough resolution to read corners and edges.
 */
class ImageProcessor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    class UnreadableImageException(cause: Throwable? = null) :
        IOException("That photo couldn't be read", cause)

    suspend fun prepare(uri: Uri, slot: String): PreparedImage = withContext(ioDispatcher) {
        val bitmap = decodeScaled(uri) ?: throw UnreadableImageException()
        val oriented = try {
            applyExifRotation(uri, bitmap)
        } catch (e: IOException) {
            bitmap // an unreadable EXIF header is not worth failing the whole capture
        }

        val jpeg = ByteArrayOutputStream().use { out ->
            oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }

        val file = File(inspectionDir(), "$slot-${System.currentTimeMillis()}.jpg")
        file.writeBytes(jpeg)

        PreparedImage(
            base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP),
            localFile = file,
            width = oriented.width,
            height = oriented.height,
        ).also {
            if (oriented !== bitmap) bitmap.recycle()
            oriented.recycle()
        }
    }

    /** Decode with a sample size, so a huge photo never lands in memory whole. */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = openStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= MAX_EDGE) return decoded

        val scale = MAX_EDGE.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= MAX_EDGE) sample *= 2
        return sample
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = openStream(uri).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun openStream(uri: Uri) =
        context.contentResolver.openInputStream(uri) ?: throw UnreadableImageException()

    /** Where processed photos live so history can show them again. */
    fun inspectionDir(): File =
        File(context.filesDir, "inspections").apply { mkdirs() }

    /** Temp file the camera app writes into, exposed through the FileProvider. */
    fun newCaptureFile(): File =
        File(File(context.cacheDir, "captures").apply { mkdirs() }, "capture-${System.currentTimeMillis()}.jpg")

    private companion object {
        const val MAX_EDGE = 1600
        const val JPEG_QUALITY = 92
    }
}
