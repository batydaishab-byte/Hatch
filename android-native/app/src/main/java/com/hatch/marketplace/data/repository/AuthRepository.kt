package com.hatch.marketplace.data.repository

import android.util.Log
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.hatch.marketplace.data.model.AccountType
import com.hatch.marketplace.data.model.HatchFormatters
import com.hatch.marketplace.data.model.MarketplaceLogic
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.ProductStatus
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.model.UserStatus
import com.hatch.marketplace.data.model.VerificationRequest
import com.hatch.marketplace.data.model.resolvedAccountType
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSessionState(
    val firebaseUser: FirebaseUser? = null,
    val publicProfile: PublicUser? = null,
    val privateProfile: PrivateUser? = null,
    val isAdmin: Boolean = false,
    val hasAdminClaim: Boolean = false,
    val hasAdminDoc: Boolean = false,
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
    private val forcedClaimEvents = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 8)

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
        var hasAdminClaim = false
        var hasAdminDoc = false
        var publicLoaded = false
        var lastObservedUid: String? = null
        var lastForceRefreshedUid: String? = null

        fun emitState(user: FirebaseUser?) {
            if (user == null) {
                trySend(AuthSessionState(isFirebaseConnected = db != null))
                return
            }
            val verifiedEmail = user.isEmailVerified
            val effectiveClaim = verifiedEmail && hasAdminClaim
            val effectiveDoc = verifiedEmail && hasAdminDoc
            val isAdmin = MarketplaceLogic.isAdminRole(hasAdminClaim = effectiveClaim, hasAdminDoc = effectiveDoc)
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
                    hasAdminClaim = effectiveClaim,
                    hasAdminDoc = effectiveDoc,
                    needsProfileOnboarding = needsOnboarding,
                    isFirebaseConnected = db != null
                )
            )
        }

        fun syncClaimsFromToken(user: FirebaseUser, forceRefresh: Boolean) {
            user.getIdToken(forceRefresh).addOnSuccessListener { tokenResult ->
                if (auth.currentUser?.uid == user.uid) {
                    val claim = user.isEmailVerified && (tokenResult.claims["admin"] == true)
                    if (hasAdminClaim != claim || !forceRefresh) {
                        hasAdminClaim = claim
                        emitState(user)
                    }
                }
            }.addOnFailureListener { e ->
                Log.w("HatchAuthFlow", "getIdToken(forceRefresh=$forceRefresh) failed: ${e.javaClass.simpleName}")
                if (forceRefresh && auth.currentUser?.uid == user.uid) {
                    // Fallback cleanly to cached token when offline
                    user.getIdToken(false).addOnSuccessListener { cached ->
                        if (auth.currentUser?.uid == user.uid) {
                            hasAdminClaim = user.isEmailVerified && (cached.claims["admin"] == true)
                            emitState(user)
                        }
                    }
                }
            }
        }

        val forcedJob = launch {
            forcedClaimEvents.collectLatest { (uid, claim) ->
                val activeUser = auth.currentUser
                if (activeUser != null && activeUser.uid == uid) {
                    hasAdminClaim = activeUser.isEmailVerified && claim
                    emitState(activeUser)
                }
            }
        }

        val idTokenListener = FirebaseAuth.IdTokenListener { firebaseAuth ->
            val user = firebaseAuth.currentUser ?: return@IdTokenListener
            syncClaimsFromToken(user, forceRefresh = false)
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user?.uid == lastObservedUid && user != null) {
                emitState(user)
                return@AuthStateListener
            }
            lastObservedUid = user?.uid
            publicReg?.remove(); publicReg = null
            privateReg?.remove(); privateReg = null
            adminReg?.remove(); adminReg = null
            currentPublic = null
            currentPrivate = null
            hasAdminClaim = false
            hasAdminDoc = false
            publicLoaded = false

            Log.d(
                "HatchAuthFlow",
                "AuthStateListener fired: signedIn=${user != null}, isAnonymous=${user?.isAnonymous == true}, hasFirestore=${db != null}"
            )
            if (user == null) {
                lastForceRefreshedUid = null
                emitState(null)
                return@AuthStateListener
            }

            emitState(user)

            // Read cached claims immediately, then force one server refresh per session start
            // so newly granted or recently revoked admin claims are detected without waiting ~1h.
            syncClaimsFromToken(user, forceRefresh = false)
            if (lastForceRefreshedUid != user.uid) {
                lastForceRefreshedUid = user.uid
                syncClaimsFromToken(user, forceRefresh = true)
            }

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
                    val docExists = snap != null && snap.exists()
                    val docChanged = hasAdminDoc != docExists
                    hasAdminDoc = docExists
                    if (docChanged && docExists && !hasAdminClaim) {
                        // Admin doc appeared without claim yet; refresh token in case claim was just minted.
                        syncClaimsFromToken(user, forceRefresh = true)
                    }
                    emitState(user)
                }
        }

        auth.addAuthStateListener(authListener)
        auth.addIdTokenListener(idTokenListener)
        awaitClose {
            forcedJob.cancel()
            auth.removeAuthStateListener(authListener)
            auth.removeIdTokenListener(idTokenListener)
            publicReg?.remove()
            privateReg?.remove()
            adminReg?.remove()
        }
    }

    /**
     * Forces Firebase Auth to fetch a fresh ID token with updated custom claims for the current user.
     * Note: Refreshing the token on this client updates local claims (grant or revocation), while
     * previously minted tokens on other devices remain valid until their normal JWT expiry unless
     * refresh tokens are revoked via Firebase Admin SDK.
     */
    suspend fun refreshIdTokenClaims(forceRefresh: Boolean = true): Result<Boolean> = runCatching {
        val auth = clients.auth ?: error("Firebase Auth unavailable")
        val user = auth.currentUser ?: return@runCatching false
        if (forceRefresh) {
            runCatching { user.reload().await() }
        }
        val tokenResult = user.getIdToken(forceRefresh).await()
        val claim = user.isEmailVerified && (tokenResult?.claims?.get("admin") == true)
        forcedClaimEvents.tryEmit(user.uid to claim)
        claim
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
        avatar: String,
        accountType: String = AccountType.PERSONAL.wireValue,
        businessActivity: String = "",
        businessLogo: String = ""
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val resolvedType = AccountType.fromWire(accountType)
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Name must be at least 2 characters." }
        val cleanCity = city.trim().take(100)
        require(cleanCity.length >= 2) { "Please select a valid city." }
        val cleanPhone = phone.trim().ifBlank { "+249000000000" }.take(32)
        require(cleanPhone.length >= 6) { "Phone number must be at least 6 characters." }

        val cleanActivity = if (resolvedType.isCommercial()) businessActivity.trim().take(120) else ""
        if (resolvedType.isCommercial()) {
            require(cleanActivity.length >= 2) { "Business activity / field is required." }
        }
        val cleanLogo = if (resolvedType.isCommercial()) {
            businessLogo.trim().ifBlank { avatar }.take(1000)
        } else {
            ""
        }
        val effectiveAvatar = if (resolvedType.isCommercial() && cleanLogo.isNotBlank()) {
            cleanLogo
        } else {
            avatar.trim().take(1000)
        }

        val publicRef = db.collection("users_public").document(uid)
        val privateRef = db.collection("users_private").document(uid)

        val existingPublic = publicRef.get().await()
        if (!existingPublic.exists()) {
            val publicPayload = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to cleanName,
                "avatar" to effectiveAvatar,
                "city" to cleanCity,
                "rating" to 5.0,
                "dealsCompleted" to 0,
                "reportsCount" to 0,
                "verifiedIdentity" to false,
                "status" to UserStatus.ACTIVE.wireValue,
                "accountType" to resolvedType.wireValue,
                "joinedDate" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanActivity.isNotEmpty()) {
                publicPayload["businessActivity"] = cleanActivity
            }
            if (cleanLogo.isNotEmpty()) {
                publicPayload["businessLogo"] = cleanLogo
            }
            publicRef.set(publicPayload).await()
        } else {
            publicRef.update(
                mapOf(
                    "name" to cleanName,
                    "avatar" to effectiveAvatar,
                    "city" to cleanCity,
                    "accountType" to resolvedType.wireValue,
                    "businessActivity" to cleanActivity,
                    "businessLogo" to cleanLogo,
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

        syncCommercialProfileDoc(
            uid = uid,
            resolvedType = resolvedType,
            cleanName = cleanName,
            effectiveAvatar = effectiveAvatar,
            cleanCity = cleanCity,
            cleanActivity = cleanActivity,
            cleanLogo = cleanLogo
        )
    }

    suspend fun updateProfile(
        uid: String,
        name: String,
        phone: String,
        city: String,
        avatar: String,
        accountType: String = AccountType.PERSONAL.wireValue,
        businessActivity: String = "",
        businessLogo: String = ""
    ): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val resolvedType = AccountType.fromWire(accountType)
        val cleanName = name.trim().take(80)
        require(cleanName.length >= 2) { "Name must be at least 2 characters." }
        val cleanCity = city.trim().take(100)
        require(cleanCity.length >= 2) { "City is required." }
        val cleanPhone = phone.trim().take(32)
        require(cleanPhone.length >= 6) { "Valid phone number is required." }

        val cleanActivity = if (resolvedType.isCommercial()) businessActivity.trim().take(120) else ""
        if (resolvedType.isCommercial()) {
            require(cleanActivity.length >= 2) { "Business activity / field is required." }
        }
        val cleanLogo = if (resolvedType.isCommercial()) {
            businessLogo.trim().ifBlank { avatar }.take(1000)
        } else {
            ""
        }
        val brand = MarketplaceLogic.buildSellerBrandSnapshot(
            name = cleanName,
            avatar = avatar,
            businessLogo = cleanLogo,
            accountType = resolvedType,
            businessActivity = cleanActivity
        )

        db.collection("users_public").document(uid).update(
            mapOf(
                "name" to cleanName,
                "avatar" to brand.sellerAvatar,
                "city" to cleanCity,
                "accountType" to resolvedType.wireValue,
                "businessActivity" to cleanActivity,
                "businessLogo" to cleanLogo,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        db.collection("users_private").document(uid).update(
            mapOf(
                "phone" to cleanPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Propagate updated store/company identity onto active products under the same UID
        runCatching {
            val sellerProducts = db.collection("products")
                .whereEqualTo("sellerId", uid)
                .whereEqualTo("status", ProductStatus.ACTIVE.wireValue)
                .get()
                .await()
                .documents

            sellerProducts.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "sellerName" to brand.sellerName,
                            "sellerAvatar" to brand.sellerAvatar,
                            "sellerAccountType" to brand.sellerAccountType,
                            "sellerBusinessActivity" to brand.sellerBusinessActivity,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        }

        syncCommercialProfileDoc(
            uid = uid,
            resolvedType = resolvedType,
            cleanName = cleanName,
            effectiveAvatar = brand.sellerAvatar,
            cleanCity = cleanCity,
            cleanActivity = cleanActivity,
            cleanLogo = cleanLogo
        )
    }

    suspend fun ensureCommercialProfileSynced(user: PublicUser) {
        val authUid = clients.auth?.currentUser?.uid ?: return
        if (authUid != user.uid || !user.resolvedAccountType().isCommercial()) return
        val db = clients.firestore ?: return
        runCatching {
            val commercialRef = db.collection("commercial_profiles").document(user.uid)
            val snap = commercialRef.get().await()
            if (!snap.exists()) {
                syncCommercialProfileDoc(
                    uid = user.uid,
                    resolvedType = user.resolvedAccountType(),
                    cleanName = user.name.trim().take(80),
                    effectiveAvatar = user.avatar.trim().take(1000),
                    cleanCity = user.city.trim().take(100),
                    cleanActivity = user.businessActivity.orEmpty().trim().take(120),
                    cleanLogo = user.businessLogo.orEmpty().trim().take(1000)
                )
            }
        }
    }

    private suspend fun syncCommercialProfileDoc(
        uid: String,
        resolvedType: AccountType,
        cleanName: String,
        effectiveAvatar: String,
        cleanCity: String,
        cleanActivity: String,
        cleanLogo: String
    ) {
        val db = clients.firestore ?: return
        val commercialRef = db.collection("commercial_profiles").document(uid)
        if (!resolvedType.isCommercial()) {
            runCatching { commercialRef.delete().await() }
            return
        }

        val existingCommercial = commercialRef.get().await()
        if (!existingCommercial.exists()) {
            val publicSnap = db.collection("users_public").document(uid).get().await()
            val joinedDate = publicSnap.getTimestamp("joinedDate") ?: FieldValue.serverTimestamp()
            val rating = publicSnap.getDouble("rating") ?: 5.0
            val dealsCompleted = (publicSnap.getLong("dealsCompleted") ?: 0L).toInt()
            val reportsCount = (publicSnap.getLong("reportsCount") ?: 0L).toInt()
            val verifiedIdentity = publicSnap.getBoolean("verifiedIdentity") ?: false
            val status = publicSnap.getString("status") ?: UserStatus.ACTIVE.wireValue

            val payload = hashMapOf<String, Any>(
                "uid" to uid,
                "name" to cleanName,
                "avatar" to effectiveAvatar,
                "city" to cleanCity,
                "rating" to rating,
                "dealsCompleted" to dealsCompleted,
                "reportsCount" to reportsCount,
                "verifiedIdentity" to verifiedIdentity,
                "status" to status,
                "accountType" to resolvedType.wireValue,
                "joinedDate" to joinedDate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanActivity.isNotEmpty()) {
                payload["businessActivity"] = cleanActivity
            }
            if (cleanLogo.isNotEmpty()) {
                payload["businessLogo"] = cleanLogo
            }
            commercialRef.set(payload).await()
        } else {
            commercialRef.update(
                mapOf(
                    "name" to cleanName,
                    "avatar" to effectiveAvatar,
                    "city" to cleanCity,
                    "accountType" to resolvedType.wireValue,
                    "businessActivity" to cleanActivity,
                    "businessLogo" to cleanLogo,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun grantRewardCredit(uid: String, amount: Int, reason: String): Result<Int> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val adminUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        require(amount in 1..100) { "Grant amount must be between 1 and 100." }
        val cleanReason = reason.trim().ifBlank { "منح رصيد تمييز من إدارة هتش" }.take(240)
        val privateRef = db.collection("users_private").document(uid)
        val txId = HatchFormatters.sanitizeFirestoreId("ctx_${UUID.randomUUID().toString().replace("-", "").take(16)}")
        val txRef = db.collection("credit_transactions").document(txId)

        val nextBalance = db.runTransaction { tx ->
            val snap = tx.get(privateRef)
            require(snap.exists()) { "User private profile not found." }
            val current = (snap.getLong("rewardCredits") ?: 0L).toInt()
            val next = current + amount
            require(next <= 1000) { "Reward credit balance cannot exceed 1000." }
            tx.update(
                privateRef,
                mapOf(
                    "rewardCredits" to next,
                    "lastCreditTxId" to txId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.set(
                txRef,
                hashMapOf<String, Any>(
                    "id" to txId,
                    "uid" to uid,
                    "type" to "admin_grant",
                    "amount" to amount,
                    "balanceBefore" to current,
                    "balanceAfter" to next,
                    "grantedBy" to adminUid,
                    "reason" to cleanReason,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            next
        }.await()

        runCatching {
            val notifId = HatchFormatters.sanitizeFirestoreId("cred_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            db.collection("users_private").document(uid)
                .collection("notifications").document(notifId)
                .set(
                    hashMapOf<String, Any>(
                        "id" to notifId,
                        "type" to "system",
                        "titleAr" to "تم منحك +$amount رصيد تمييز من الإدارة ✨",
                        "titleEn" to "Admin granted you +$amount boost credits ✨",
                        "bodyAr" to cleanReason,
                        "bodyEn" to cleanReason,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
        nextBalance
    }

    suspend fun approveVerificationRequest(request: VerificationRequest): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val adminUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        db.collection("verification_requests").document(request.id).update(
            mapOf(
                "status" to "approved",
                "reviewedBy" to adminUid,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        setUserVerificationStatus(uid = request.uid, verified = true, reason = "").getOrThrow()
    }

    suspend fun rejectVerificationRequest(request: VerificationRequest, rejectionReason: String): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val adminUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        val cleanReason = rejectionReason.trim().ifBlank { "لم تستوفِ البيانات شروط التوثيق الحالية" }.take(400)
        db.collection("verification_requests").document(request.id).update(
            mapOf(
                "status" to "rejected",
                "reviewedBy" to adminUid,
                "rejectionReason" to cleanReason,
                "reviewedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        runCatching {
            val notifId = HatchFormatters.sanitizeFirestoreId("ver_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            db.collection("users_private").document(request.uid)
                .collection("notifications").document(notifId)
                .set(
                    hashMapOf<String, Any>(
                        "id" to notifId,
                        "type" to "verification",
                        "titleAr" to "تحديث حول طلب شارة التوثيق الذهبية",
                        "titleEn" to "Gold Verification Request Update",
                        "bodyAr" to "تعذر اعتماد الطلب حالياً: $cleanReason",
                        "bodyEn" to "Request not approved yet: $cleanReason",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    suspend fun setUserVerificationStatus(uid: String, verified: Boolean, reason: String = ""): Result<Unit> = runCatching {
        val db = clients.firestore ?: error("Firestore unavailable")
        val adminUid = clients.auth?.currentUser?.uid ?: error("Authentication required")
        db.collection("users_public").document(uid).update(
            mapOf(
                "verifiedIdentity" to verified,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        runCatching {
            val commercialRef = db.collection("commercial_profiles").document(uid)
            if (commercialRef.get().await().exists()) {
                commercialRef.update(
                    mapOf(
                        "verifiedIdentity" to verified,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        runCatching {
            val sellerProducts = db.collection("products")
                .whereEqualTo("sellerId", uid)
                .whereEqualTo("status", ProductStatus.ACTIVE.wireValue)
                .get()
                .await()
                .documents
            sellerProducts.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "sellerVerified" to verified,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        }

        if (!verified) {
            runCatching {
                val approvedReqs = db.collection("verification_requests")
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("status", "approved")
                    .get()
                    .await()
                    .documents
                approvedReqs.forEach { doc ->
                    val revokeUpdates = hashMapOf<String, Any>(
                        "status" to "revoked",
                        "reviewedBy" to adminUid,
                        "reviewedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (reason.isNotBlank()) {
                        revokeUpdates["rejectionReason"] = reason.trim().take(400)
                    }
                    doc.reference.update(revokeUpdates).await()
                }
            }
        }

        runCatching {
            val notifId = HatchFormatters.sanitizeFirestoreId("ver_${UUID.randomUUID().toString().replace("-", "").take(12)}")
            val notifPayload = if (verified) {
                hashMapOf<String, Any>(
                    "id" to notifId,
                    "type" to "verification",
                    "titleAr" to "تهانينا! تم منح حسابك شارة التوثيق الذهبية ⭐",
                    "titleEn" to "Congratulations! Gold Verified Badge Granted ⭐",
                    "bodyAr" to "تمت مراجعة حسابك واعتماد التوثيق الرسمي من إدارة هتش.",
                    "bodyEn" to "Your account has been verified with the official Gold Badge by HATCH Administration.",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            } else {
                val cleanReason = reason.trim().ifBlank { "مراجعة إدارية" }.take(300)
                hashMapOf<String, Any>(
                    "id" to notifId,
                    "type" to "verification",
                    "titleAr" to "تم سحب شارة التوثيق الذهبية",
                    "titleEn" to "Gold Verification Badge Revoked",
                    "bodyAr" to "قامت الإدارة بسحب شارة التوثيق: $cleanReason",
                    "bodyEn" to "HATCH Administration revoked your verified badge: $cleanReason",
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            }
            db.collection("users_private").document(uid)
                .collection("notifications").document(notifId)
                .set(notifPayload).await()
        }
    }

    fun observePublicUsers(isSignedIn: Boolean, hasAdminClaim: Boolean): Flow<List<PublicUser>> = callbackFlow {
        val db = clients.firestore
        if (db == null || !isSignedIn) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = if (hasAdminClaim) {
            db.collection("users_public")
        } else {
            db.collection("users_public").whereEqualTo("status", UserStatus.ACTIVE.wireValue)
        }

        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w("HatchAuthFlow", "users_public list listener error: code=${err.code}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                doc.toObject(PublicUser::class.java)?.copy(uid = doc.id)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeCommercialProfiles(hasAdminClaim: Boolean): Flow<List<PublicUser>> = callbackFlow {
        val db = clients.firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = if (hasAdminClaim) {
            db.collection("commercial_profiles")
        } else {
            db.collection("commercial_profiles")
                .whereEqualTo("status", UserStatus.ACTIVE.wireValue)
                .whereIn("accountType", listOf(AccountType.STORE.wireValue, AccountType.COMPANY.wireValue))
        }

        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w("HatchCommercial", "commercial_profiles listener error: code=${err.code}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val raw = doc.toObject(PublicUser::class.java)?.copy(uid = doc.id) ?: return@mapNotNull null
                if (hasAdminClaim) raw else MarketplaceLogic.sanitizeCommercialProfileForGuest(raw)
            }.orEmpty()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun signOut() {
        clients.auth?.signOut()
    }
}
