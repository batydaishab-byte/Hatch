package com.hatch.marketplace.data.repository

import android.util.Log
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.UserStatus
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSessionState(
    val firebaseUser: FirebaseUser? = null,
    val publicProfile: PublicUser? = null,
    val privateProfile: PrivateUser? = null,
    val isAdmin: Boolean = false,
    val isVerifiedAdminEmail: Boolean = false,
    val needsProfileOnboarding: Boolean = false,
    val isFirebaseConnected: Boolean = false
)

data class GoogleAuthOutcome(
    val user: FirebaseUser,
    val needsOnboarding: Boolean
)

@Singleton
class AuthRepository @Inject constructor(
    private val clients: FirebaseClientsHolder
) {
    val isConfigured: Boolean get() = clients.isConfigured

    fun currentFirebaseUser(): FirebaseUser? = clients.auth?.currentUser

    fun observeAuthSession(): Flow<AuthSessionState> = callbackFlow {
        val auth = clients.auth
        val db = clients.firestore
        if (auth == null) {
            Log.w("HatchAuthFlow", "observeAuthSession aborted: FirebaseAuth is null")
            trySend(AuthSessionState())
            close()
            return@callbackFlow
        }

        var publicReg: ListenerRegistration? = null
        var privateReg: ListenerRegistration? = null
        var adminReg: ListenerRegistration? = null

        var currentPublic: PublicUser? = null
        var currentPrivate: PrivateUser? = null
        var hasAdminDoc = false
        var publicLoaded = false

        fun emitState(user: FirebaseUser?) {
            if (user == null) {
                trySend(AuthSessionState(isFirebaseConnected = db != null))
                return
            }
            val verifiedEmail = user.isEmailVerified &&
                user.email?.trim()?.equals("batydaishab@gmail.com", ignoreCase = true) == true
            val isAdmin = hasAdminDoc || verifiedEmail ||
                MarketplaceLogic.isAdminRole(currentPrivate?.role, if (user.isEmailVerified) user.email else null)
            val needsOnboarding = publicLoaded && (
                currentPublic == null ||
                    currentPublic?.name.isNullOrBlank() ||
                    currentPublic?.city.isNullOrBlank()
                )
            trySend(
                AuthSessionState(
                    firebaseUser = user,
                    publicProfile = currentPublic,
                    privateProfile = currentPrivate,
                    isAdmin = isAdmin,
                    isVerifiedAdminEmail = verifiedEmail,
                    needsProfileOnboarding = needsOnboarding,
                    isFirebaseConnected = db != null
                )
            )
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            publicReg?.remove(); publicReg = null
            privateReg?.remove(); privateReg = null
            adminReg?.remove(); adminReg = null
            currentPublic = null
            currentPrivate = null
            hasAdminDoc = false
            publicLoaded = false

            val user = firebaseAuth.currentUser
            Log.d(
                "HatchAuthFlow",
                "AuthStateListener fired: signedIn=${user != null}, isAnonymous=${user?.isAnonymous == true}, hasFirestore=${db != null}"
            )
            if (user == null) {
                emitState(null)
                return@AuthStateListener
            }

            emitState(user)

            if (db == null) {
                Log.e("HatchAuthFlow", "Firestore unavailable during auth session observation")
                return@AuthStateListener
            }

            publicReg = db.collection("users_public").document(user.uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e("HatchAuthFlow", "users_public listener error: code=${err.code}")
                        return@addSnapshotListener
                    }
                    publicLoaded = true
                    currentPublic = snap?.takeIf { it.exists() }
                        ?.toObject(PublicUser::class.java)
                        ?.copy(uid = snap.id)
                    Log.d(
                        "HatchAuthFlow",
                        "users_public snapshot: exists=${snap?.exists() == true}, fromCache=${snap?.metadata?.isFromCache == true}, hasCity=${!currentPublic?.city.isNullOrBlank()}"
                    )
                    emitState(user)
                }

            privateReg = db.collection("users_private").document(user.uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w("HatchAuthFlow", "users_private listener error: code=${err.code}")
                        return@addSnapshotListener
                    }
                    currentPrivate = snap?.takeIf { it.exists() }
                        ?.toObject(PrivateUser::class.java)
                        ?.copy(uid = snap.id)
                    emitState(user)
                }

            adminReg = db.collection("admins").document(user.uid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.w("HatchAuthFlow", "admins listener error: code=${err.code}")
                        return@addSnapshotListener
                    }
                    hasAdminDoc = snap != null && snap.exists()
                    emitState(user)
                }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            publicReg?.remove()
            privateReg?.remove()
            adminReg?.remove()
        }
    }

    suspend fun verifyFirestoreServerConnection(): Result<Boolean> = runCatching {
        val db = clients.firestore ?: error("Firebase not configured")
        db.collection("ad_config").document("default").get(Source.SERVER).await()
        true
    }.recoverCatching {
        val db = clients.firestore ?: error("Firebase not configured")
        db.collection("products").whereEqualTo("status", "active").limit(1).get(Source.SERVER).await()
        true
    }

    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> = runCatching {
        val auth = clients.auth ?: error("Firebase Auth unavailable")
        val res = auth.signInWithCredential(credential).await()
        val user = res.user ?: auth.currentUser ?: error("Google Sign-In returned empty user")
        Log.d("HatchAuthFlow", "Firebase signInWithCredential success: isNewUser=${res.additionalUserInfo?.isNewUser}")
        user
    }.onFailure { e ->
        Log.e("HatchAuthFlow", "Firebase signInWithCredential failed: ${e.javaClass.simpleName}")
    }

    suspend fun signInWithGoogleAndCheckProfile(credential: AuthCredential): Result<GoogleAuthOutcome> = runCatching {
        val user = signInWithCredential(credential).getOrThrow()
        val auth = clients.auth ?: error("Firebase Auth unavailable")
        val activeUser = auth.currentUser ?: user
        val db = clients.firestore ?: error("Firestore unavailable")

        val publicRef = db.collection("users_public").document(activeUser.uid)
        val snap = try {
            publicRef.get(Source.SERVER).await()
        } catch (serverErr: Exception) {
            Log.w(
                "HatchAuthFlow",
                "Server profile lookup fallback to default source: ${serverErr.javaClass.simpleName}"
            )
            publicRef.get().await()
        }

        val name = snap.getString("name").orEmpty()
        val city = snap.getString("city").orEmpty()
        val needsOnboarding = !snap.exists() || name.isBlank() || city.isBlank()
        Log.d(
            "HatchAuthFlow",
            "Post-Google profile check: docExists=${snap.exists()}, hasName=${name.isNotBlank()}, hasCity=${city.isNotBlank()}, needsOnboarding=$needsOnboarding"
        )
        GoogleAuthOutcome(user = activeUser, needsOnboarding = needsOnboarding)
    }.onFailure { e ->
        Log.e("HatchAuthFlow", "signInWithGoogleAndCheckProfile failed: ${e.javaClass.simpleName}")
    }

    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> = runCatching {
        val auth = clients.auth ?: error("Firebase Auth unavailable")
        val res = auth.signInWithEmailAndPassword(email.trim(), password).await()
        res.user ?: error("Email sign-in failed")
    }

    suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        name: String,
        phone: String,
        city: String,
        avatar: String
    ): Result<FirebaseUser> = runCatching {
        val auth = clients.auth ?: error("Firebase Auth unavailable")
        val res = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = res.user ?: error("Account creation failed")
        saveOnboardingProfile(
            uid = user.uid,
            email = user.email ?: email.trim(),
            name = name,
            phone = phone,
            city = city,
            avatar = avatar
        ).getOrThrow()
        user
    }

    suspend fun saveOnboardingProfile(
        uid: String,
        email: String,
        name: String,
        phone: String,
        city: String,
        avatar: String
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Name must be at least 2 characters." }
        val cleanCity = city.trim().take(100)
        require(cleanCity.length >= 2) { "Please select a valid city." }
        val cleanPhone = phone.trim().ifBlank { "+249000000000" }.take(32)
        require(cleanPhone.length >= 6) { "Phone number must be at least 6 characters." }

        val publicRef = db.collection("users_public").document(uid)
        val privateRef = db.collection("users_private").document(uid)

        val existingPublic = publicRef.get().await()
        if (!existingPublic.exists()) {
            val publicPayload = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to cleanName,
                "avatar" to avatar,
                "city" to cleanCity,
                "rating" to 5.0,
                "dealsCompleted" to 0,
                "reportsCount" to 0,
                "verifiedIdentity" to false,
                "status" to UserStatus.ACTIVE.wireValue,
                "joinedDate" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            publicRef.set(publicPayload).await()
        } else {
            publicRef.update(
                mapOf(
                    "name" to cleanName,
                    "avatar" to avatar,
                    "city" to cleanCity,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val existingPrivate = privateRef.get().await()
        if (!existingPrivate.exists()) {
            val privatePayload = hashMapOf<String, Any>(
                "uid" to uid,
                "email" to email.take(160),
                "phone" to cleanPhone,
                "role" to "user",
                "rewardCredits" to 0,
                "fcmTokens" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            privateRef.set(privatePayload).await()
        } else {
            privateRef.update(
                mapOf(
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        city: String,
        avatar: String
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Name must be at least 2 characters." }
        val cleanCity = city.trim().take(100)
        require(cleanCity.length >= 2) { "City is required." }
        val cleanPhone = phone.trim().take(32)
        require(cleanPhone.length >= 6) { "Valid phone number is required." }

        db.collection("users_public").document(uid).update(
            mapOf(
                "name" to cleanName,
                "avatar" to avatar,
                "city" to cleanCity,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("users_private").document(uid).update(
            mapOf(
                "phone" to cleanPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun adjustRewardCredits(uid: String, delta: Int): Result<Int> = runCatching {
        require(delta == 1 || delta == -1) { "Reward credit adjustment must be +1 or -1." }
        val db = clients.firestore ?: error("Firestore unavailable")
        val privateRef = db.collection("users_private").document(uid)
        db.runTransaction { tx ->
            val snap = tx.get(privateRef)
            val current = (snap.getLong("rewardCredits") ?: 0L).toInt()
            val next = (current + delta).coerceIn(0, 1000)
            require(!(delta < 0 && current <= 0)) { "Insufficient reward credits." }
            tx.update(
                privateRef,
                mapOf(
                    "rewardCredits" to next,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            next
        }.await()
    }

    fun observePublicUsers(isSignedIn: Boolean, isVerifiedAdminEmail: Boolean): Flow<List<PublicUser>> = callbackFlow {
        val db = clients.firestore
        if (db == null || !isSignedIn) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = if (isVerifiedAdminEmail) {
            db.collection("users_public")
        } else {
            db.collection("users_public").whereEqualTo("status", UserStatus.ACTIVE.wireValue)
        }

        val reg = query.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(PublicUser::class.java)?.copy(uid = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun signOut() {
        clients.auth?.signOut()
    }
}
