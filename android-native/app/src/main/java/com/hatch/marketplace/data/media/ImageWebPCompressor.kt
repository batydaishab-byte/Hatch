package com.hatch.marketplace.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CompressedProductMedia(
    val fullWebpBytes: ByteArray,
    val thumbnailWebpBytes: ByteArray,
    val fullWidth: Int,
    val fullHeight: Int
)

data class ValidatedAdVideo(
    val videoBytes: ByteArray,
    val durationMs: Long,
    val mimeType: String
)

/**
 * Native Android compressor & media validator:
 * - Converts user-selected images to WebP without external paid APIs.
 * - Generates a lightweight 400px WebP thumbnail for feed/browse bandwidth savings.
 * - Validates video duration (<= 30s) and size (<= 15 MB) before upload.
 */
object ImageWebPCompressor {

    fun compressProductImageAndThumbnail(
        context: Context,
        uri: Uri
    ): Result<CompressedProductMedia> = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase() ?: "image/jpeg"
        require(mime in MediaUploadPolicy.ALLOWED_IMAGE_MIME_TYPES) {
            "Unsupported image type: $mime. Only JPG, PNG, and WebP images are allowed."
        }

        val originalBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Unable to decode selected image.")

        try {
            val fullBitmap = scaleDownKeepingAspect(
                originalBitmap,
                MediaUploadPolicy.MAX_PRODUCT_IMAGE_DIMENSION_PX
            )
            val fullBytes = compressToWebPBudget(
                bitmap = fullBitmap,
                initialQuality = MediaUploadPolicy.PRODUCT_IMAGE_WEBP_QUALITY,
                maxBytes = MediaUploadPolicy.MAX_PRODUCT_IMAGE_BYTES
            )

            val thumbBitmap = scaleDownKeepingAspect(
                fullBitmap,
                MediaUploadPolicy.THUMBNAIL_MAX_DIMENSION_PX
            )
            val thumbBytes = compressToWebPBudget(
                bitmap = thumbBitmap,
                initialQuality = MediaUploadPolicy.THUMBNAIL_WEBP_QUALITY,
                maxBytes = MediaUploadPolicy.MAX_THUMBNAIL_BYTES
            )

            val width = fullBitmap.width
            val height = fullBitmap.height
            if (thumbBitmap !== fullBitmap) thumbBitmap.recycle()
            if (fullBitmap !== originalBitmap) fullBitmap.recycle()

            CompressedProductMedia(
                fullWebpBytes = fullBytes,
                thumbnailWebpBytes = thumbBytes,
                fullWidth = width,
                fullHeight = height
            )
        } finally {
            originalBitmap.recycle()
        }
    }

    fun validateAdminVideo(
        context: Context,
        uri: Uri
    ): Result<ValidatedAdVideo> = runCatching {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.lowercase() ?: "video/mp4"
        require(mime in MediaUploadPolicy.ALLOWED_VIDEO_MIME_TYPES) {
            "Unsupported video format: $mime. Only MP4 or WebM allowed."
        }

        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            runCatching { retriever.release() }
        }

        require(durationMs in 1..MediaUploadPolicy.MAX_AD_VIDEO_DURATION_MS) {
            "Video duration exceeds 30-second limit (${durationMs / 1000}s)."
        }

        val bytes = resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = stream.read(chunk)
                if (read == -1) break
                total += read
                require(total <= MediaUploadPolicy.MAX_AD_VIDEO_BYTES) {
                    "Video file exceeds 15 MB free-tier safety limit."
                }
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: error("Unable to read video stream.")

        ValidatedAdVideo(
            videoBytes = bytes,
            durationMs = durationMs,
            mimeType = mime
        )
    }

    private fun scaleDownKeepingAspect(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest.toFloat()
        val targetW = max(1, (bitmap.width * ratio).roundToInt())
        val targetH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun compressToWebPBudget(
        bitmap: Bitmap,
        initialQuality: Int,
        maxBytes: Long
    ): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        var quality = initialQuality
        var output: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            bitmap.compress(format, quality, baos)
            output = baos.toByteArray()
            quality -= 8
        } while (output.size > maxBytes && quality >= 40)

        require(output.size <= maxBytes) {
            "Compressed WebP image (${output.size} bytes) still exceeds limit ($maxBytes bytes)."
        }
        return output
    }
}
