package com.hatch.marketplace.data.media

import com.hatch.marketplace.BuildConfig

/**
 * Enforces HATCH Free-Tier & Security Media Upload Policy:
 * 1. Regular users can ONLY upload compressed WebP product images (max 5/listing, <= 1.5 MB each).
 * 2. Admins ONLY can upload sponsored ad images (<= 2 MB) and videos (<= 15 MB, <= 30s).
 * 3. No API_SECRET or private credentials exist in Android or Web JS.
 * 4. Fail-closed: Admin permissions are verified server-side at upload signing time; if no free
 *    HTTPS signer endpoint is configured, video upload stays strictly DISABLED.
 */
object MediaUploadPolicy {
    // Product Image Limits (Regular Users & Admins)
    const val MAX_PRODUCT_IMAGES_PER_LISTING = 5
    const val MAX_PRODUCT_IMAGE_DIMENSION_PX = 1280
    const val PRODUCT_IMAGE_WEBP_QUALITY = 82
    const val MAX_PRODUCT_IMAGE_BYTES = 1_572_864L // 1.5 MB

    // Product Thumbnail Limits (Reduces feed bandwidth)
    const val THUMBNAIL_MAX_DIMENSION_PX = 400
    const val THUMBNAIL_WEBP_QUALITY = 75
    const val MAX_THUMBNAIL_BYTES = 153_600L // 150 KB

    // Sponsored Ad Media Limits (Admin ONLY)
    const val MAX_AD_IMAGE_BYTES = 2_097_152L // 2 MB
    const val MAX_AD_VIDEO_BYTES = 15_728_640L // 15 MB
    const val MAX_AD_VIDEO_DURATION_MS = 30_000L // 30 seconds

    // Cloudinary Free Tier Quota Warning Threshold (25 monthly credits)
    const val CLOUDINARY_FREE_CREDITS_LIMIT = 25.0
    const val QUOTA_WARNING_THRESHOLD_PERCENT = 80.0

    val ALLOWED_IMAGE_MIME_TYPES = setOf("image/webp", "image/jpeg", "image/jpg", "image/png")
    val ALLOWED_VIDEO_MIME_TYPES = setOf("video/mp4", "video/webm")

    enum class UploadAction {
        PRODUCT_IMAGE,
        PRODUCT_THUMBNAIL,
        SPONSORED_AD_IMAGE,
        SPONSORED_AD_VIDEO
    }

    /**
     * Fail-closed gate: Video upload requires BOTH:
     * - Verified Admin role on server
     * - Active HTTPS Signed-Upload endpoint (so API_SECRET never lives in client app)
     */
    fun isVideoUploadSecurelyEnabled(isAdmin: Boolean): Boolean {
        val signerConfigured = BuildConfig.MEDIA_SIGNER_URL.startsWith("https://") &&
            BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank()
        return isAdmin && signerConfigured
    }

    fun canUserPerformUpload(action: UploadAction, isAdmin: Boolean): Result<Unit> {
        return when (action) {
            UploadAction.PRODUCT_IMAGE,
            UploadAction.PRODUCT_THUMBNAIL -> Result.success(Unit)
            UploadAction.SPONSORED_AD_IMAGE -> {
                if (isAdmin) Result.success(Unit)
                else Result.failure(SecurityException("Only verified admins can upload sponsored ad images."))
            }
            UploadAction.SPONSORED_AD_VIDEO -> {
                if (!isAdmin) {
                    Result.failure(SecurityException("Only verified admins can upload sponsored ad videos."))
                } else if (!isVideoUploadSecurelyEnabled(isAdmin = true)) {
                    Result.failure(
                        IllegalStateException(
                            "Video upload is locked until a free signed-upload backend (Cloudflare Worker) is configured."
                        )
                    )
                } else {
                    Result.success(Unit)
                }
            }
        }
    }
}
