package com.hatch.marketplace.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.hatch.marketplace.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Non-null wrapper holding optional Firebase instances so Dagger/Hilt never fails on
 * nullable @Provides bindings even when google-services.json is absent in local builds.
 */
class FirebaseClientsHolder(
    val app: FirebaseApp?,
    val auth: FirebaseAuth?,
    val firestore: FirebaseFirestore?
) {
    val isConfigured: Boolean
        get() = app != null && auth != null && firestore != null
}

/**
 * Provides Firebase services pointing to the shared HATCH Cloud Firestore database.
 * - Uses ONLY 100% Free Spark Tier services (Firebase Auth + Cloud Firestore).
 * - Does NOT use Firebase Storage (which requires Blaze billing); media uploads use
 *   a free CDN provider (Cloudinary Free Tier) via signed uploads.
 * - Enables 40 MB local persistent cache to stay well below Spark daily read limits.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseServiceModule {

    @Provides
    @Singleton
    fun provideFirebaseClientsHolder(
        @ApplicationContext context: Context
    ): FirebaseClientsHolder {
        val app = FirebaseApp.getApps(context).firstOrNull() ?: runCatching {
            FirebaseApp.initializeApp(context)
        }.getOrNull()

        val auth = app?.let { runCatching { FirebaseAuth.getInstance(it) }.getOrNull() }

        val firestore = app?.let { firebaseApp ->
            runCatching {
                val db = FirebaseFirestore.getInstance(firebaseApp, BuildConfig.FIRESTORE_DATABASE_ID)
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(40L * 1024L * 1024L) // 40 MB offline cache
                            .build()
                    )
                    .build()
                db.firestoreSettings = settings
                db
            }.getOrNull()
        }

        return FirebaseClientsHolder(app = app, auth = auth, firestore = firestore)
    }
}
