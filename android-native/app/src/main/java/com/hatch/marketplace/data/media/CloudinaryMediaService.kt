package com.hatch.marketplace.data.media

import com.hatch.marketplace.BuildConfig
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SignedUploadTicket(
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String,
    val folder: String,
    val resourceType: String,
    val allowedFormats: String
)

@Serializable
data class CloudinaryQuotaStatus(
    val creditsUsed: Double = 0.0,
    val creditsLimit: Double = MediaUploadPolicy.CLOUDINARY_FREE_CREDITS_LIMIT,
    val usagePercent: Double = 0.0,
    val isNearLimit: Boolean = false,
    val videoUploadsEnabled: Boolean = false
)

@Serializable
data class UploadedProductImageResult(
    val fullImageUrl: String,
    val thumbnailUrl: String
)

/**
 * Signed Cloudinary uploader.
 * Security Guarantee:
 * - NO Cloudinary API_SECRET exists anywhere in this app.
 * - Every upload requests a short-lived signature from MEDIA_SIGNER_URL using the user's
 *   verified Firebase ID Token. The signer enforces Admin permissions for ads/videos at upload time.
 * - If MEDIA_SIGNER_URL is not configured, video uploads remain strictly disabled (fail-closed).
 */
@Singleton
class CloudinaryMediaService @Inject constructor(
    private val firebaseClients: FirebaseClientsHolder
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uploadProductImageWithThumbnail(
        media: CompressedProductMedia
    ): Result<UploadedProductImageResult> = withContext(Dispatchers.IO) {
        runCatching {
            val fullTicket = requestSignedTicket(
                action = MediaUploadPolicy.UploadAction.PRODUCT_IMAGE,
                byteSize = media.fullWebpBytes.size.toLong(),
                durationMs = 0L
            ).getOrThrow()

            val fullUrl = performCloudinaryMultipartUpload(
                ticket = fullTicket,
                bytes = media.fullWebpBytes,
                fileName = "prod_${UUID.randomUUID()}.webp",
                mimeType = "image/webp"
            ).getOrThrow()

            val thumbTicket = requestSignedTicket(
                action = MediaUploadPolicy.UploadAction.PRODUCT_THUMBNAIL,
                byteSize = media.thumbnailWebpBytes.size.toLong(),
                durationMs = 0L
            ).getOrThrow()

            val thumbUrl = performCloudinaryMultipartUpload(
                ticket = thumbTicket,
                bytes = media.thumbnailWebpBytes,
                fileName = "thumb_${UUID.randomUUID()}.webp",
                mimeType = "image/webp"
            ).getOrThrow()

            UploadedProductImageResult(
                fullImageUrl = fullUrl,
                thumbnailUrl = thumbUrl
            )
        }
    }

    suspend fun uploadAdminAdVideo(
        validatedVideo: ValidatedAdVideo,
        isAdmin: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            MediaUploadPolicy.canUserPerformUpload(
                MediaUploadPolicy.UploadAction.SPONSORED_AD_VIDEO,
                isAdmin = isAdmin
            ).getOrThrow()

            val ticket = requestSignedTicket(
                action = MediaUploadPolicy.UploadAction.SPONSORED_AD_VIDEO,
                byteSize = validatedVideo.videoBytes.size.toLong(),
                durationMs = validatedVideo.durationMs
            ).getOrThrow()

            performCloudinaryMultipartUpload(
                ticket = ticket,
                bytes = validatedVideo.videoBytes,
                fileName = "ad_${UUID.randomUUID()}.mp4",
                mimeType = validatedVideo.mimeType
            ).getOrThrow()
        }
    }

    suspend fun fetchAdminQuotaStatus(isAdmin: Boolean): Result<CloudinaryQuotaStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!isAdmin || !BuildConfig.MEDIA_SIGNER_URL.startsWith("https://")) {
                    return@runCatching CloudinaryQuotaStatus(
                        videoUploadsEnabled = MediaUploadPolicy.isVideoUploadSecurelyEnabled(isAdmin)
                    )
                }
                val idToken = requireFirebaseIdToken()
                val conn = (URL("${BuildConfig.MEDIA_SIGNER_URL.trimEnd('/')}/admin/media-usage")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $idToken")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText().orEmpty()
                require(code in 200..299) { "Quota check failed ($code): $body" }
                json.decodeFromString<CloudinaryQuotaStatus>(body)
            }
        }

    private suspend fun requestSignedTicket(
        action: MediaUploadPolicy.UploadAction,
        byteSize: Long,
        durationMs: Long
    ): Result<SignedUploadTicket> = runCatching {
        val signerBase = BuildConfig.MEDIA_SIGNER_URL.trimEnd('/')
        require(signerBase.startsWith("https://")) {
            "Signed upload service (MEDIA_SIGNER_URL) is not configured yet."
        }
        val idToken = requireFirebaseIdToken()
        val payload = JSONObject().apply {
            put("action", action.name)
            put("byteSize", byteSize)
            put("durationMs", durationMs)
        }.toString()

        val conn = (URL("$signerBase/sign-upload").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText().orEmpty()
        require(code in 200..299) { "Upload authorization denied ($code): $body" }
        json.decodeFromString<SignedUploadTicket>(body)
    }

    private fun performCloudinaryMultipartUpload(
        ticket: SignedUploadTicket,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> = runCatching {
        val boundary = "----HatchBoundary${UUID.randomUUID()}"
        val endpoint = "https://api.cloudinary.com/v1_1/${ticket.cloudName}/${ticket.resourceType}/upload"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connectTimeout = 20_000
            readTimeout = 30_000
        }

        DataOutputStream(conn.outputStream).use { out ->
            fun writeField(name: String, value: String) {
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                out.writeBytes("$value\r\n")
            }
            writeField("api_key", ticket.apiKey)
            writeField("timestamp", ticket.timestamp.toString())
            writeField("signature", ticket.signature)
            writeField("folder", ticket.folder)
            writeField("allowed_formats", ticket.allowedFormats)

            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
            out.writeBytes("Content-Type: $mimeType\r\n\r\n")
            out.write(bytes)
            out.writeBytes("\r\n--$boundary--\r\n")
        }

        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.readText().orEmpty()
        require(code in 200..299) { "Cloudinary upload failed ($code): $body" }
        JSONObject(body).getString("secure_url")
    }

    private suspend fun requireFirebaseIdToken(): String {
        val user = firebaseClients.auth?.currentUser
            ?: error("User must be signed in to upload media.")
        return user.getIdToken(false).await()?.token ?: error("Missing Firebase ID token.")
    }
}
