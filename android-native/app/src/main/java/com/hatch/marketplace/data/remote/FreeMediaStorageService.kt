package com.hatch.marketplace.data.remote

/**
 * Contract for 100% FREE, no-credit-card media storage shared between HATCH Web & Android Native.
 * Replaces Firebase Storage (which requires Blaze billing) with free-tier object/CDN providers
 * such as Cloudinary Free Tier (25 GB/mo, images/video/audio, no card) or Supabase Storage Free Tier (1 GB, no card).
 *
 * Only resulting compressed HTTPS URLs (< 1000 chars) are saved in Cloud Firestore documents.
 */
interface FreeMediaStorageService {
    suspend fun uploadImage(
        folder: MediaFolder,
        ownerId: String,
        compressedBytes: ByteArray,
        mimeType: String = "image/webp"
    ): Result<String>

    suspend fun uploadAudio(
        conversationId: String,
        senderId: String,
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4"
    ): Result<String>

    suspend fun uploadVideo(
        adId: String,
        videoBytes: ByteArray,
        mimeType: String = "video/mp4"
    ): Result<String>
}

enum class MediaFolder(val path: String) {
    PRODUCTS("hatch/products"),
    AVATARS("hatch/avatars"),
    CHATS("hatch/chats"),
    SPONSORED_ADS("hatch/sponsored_ads")
}
