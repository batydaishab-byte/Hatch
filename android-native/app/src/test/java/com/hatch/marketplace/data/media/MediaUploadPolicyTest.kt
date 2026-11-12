package com.hatch.marketplace.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUploadPolicyTest {

    @Test
    fun regularUserCanUploadProductImagesAndThumbnailsOnly() {
        assertTrue(
            MediaUploadPolicy.canUserPerformUpload(
                action = MediaUploadPolicy.UploadAction.PRODUCT_IMAGE,
                isAdmin = false
            ).isSuccess
        )
        assertTrue(
            MediaUploadPolicy.canUserPerformUpload(
                action = MediaUploadPolicy.UploadAction.PRODUCT_THUMBNAIL,
                isAdmin = false
            ).isSuccess
        )
    }

    @Test
    fun regularUserIsBlockedFromSponsoredAdImagesAndVideos() {
        assertTrue(
            MediaUploadPolicy.canUserPerformUpload(
                action = MediaUploadPolicy.UploadAction.SPONSORED_AD_IMAGE,
                isAdmin = false
            ).isFailure
        )
        assertTrue(
            MediaUploadPolicy.canUserPerformUpload(
                action = MediaUploadPolicy.UploadAction.SPONSORED_AD_VIDEO,
                isAdmin = false
            ).isFailure
        )
    }

    @Test
    fun videoUploadRemainsFailClosedWithoutHttpsSigner() {
        assertFalse(MediaUploadPolicy.isVideoUploadSecurelyEnabled(isAdmin = false))
        assertFalse(MediaUploadPolicy.isVideoUploadSecurelyEnabled(isAdmin = true))
        assertTrue(
            MediaUploadPolicy.canUserPerformUpload(
                action = MediaUploadPolicy.UploadAction.SPONSORED_AD_VIDEO,
                isAdmin = true
            ).isFailure
        )
    }
}
