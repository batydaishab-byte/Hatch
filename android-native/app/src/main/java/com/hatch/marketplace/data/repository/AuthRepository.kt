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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSessionState(
    val firebaseUser: FirebaseUser? = null,
    val uid: String? = firebaseUser?.uid,
    val isEmailVerified: Boolean = firebaseUser?.isEmailVerified ?: false,
    val publicProfile: PublicUser? = null,
    val privateProfile: PrivateUser? = null,
    val isAdmin: Boolean = false,
    val hasAdminClaim: Boolean = false,
    val hasAdminDoc: Boolean = false,
    val needsProfileOnboarding: Boolean = false,
    val isFirebaseConnected: Boolean = false
) {
    val isSignedIn: Boolean get() = !uid.isNullOrBlank()
}

data class GoogleAuthOutcome(
    val user: FirebaseUser,
    val needsOnboarding: Boolean
)

internal data class AuthUserIdentity(
    val uid: String,
    val isEmailVerified: Boolean,
    val isAnonymous: Boolean = false,
    val firebaseUser: FirebaseUser? = null
)

internal interface AuthSessionLogger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String)
}

internal object AndroidAuthSessionLogger : AuthSessionLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }
}

internal interface AuthSessionGateway {
    val hasAuth: Boolean
    val hasFirestore: Boolean
    fun currentUser(): AuthUserIdentity?
    fun currentFirebaseUser(): FirebaseUser?
    fun registerAuthStateListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit
    fun registerIdTokenListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit
    fun fetchIdTokenAdminClaim(
        uid: String,
        forceRefresh: Boolean,
        onResult: (Result<Boolean>) -> Unit
    )
    suspend fun reloadAndGetAdminClaim(forceRefresh: Boolean): Result<Pair<String, Boolean>?>
    fun observeUserDocs(
        uid: String,
        onPublic: (loaded: Boolean, user: PublicUser?) -> Unit,
        onPrivate: (user: PrivateUser?) -> Unit,
        onAdminDoc: (exists: Boolean) -> Unit
    ): () -> Unit
    fun signOut()
}

internal class FirebaseAuthSessionGateway(
    private val clients: FirebaseClientsHolder,
    private val logger: AuthSessionLogger = AndroidAuthSessionLogger
) : AuthSessionGateway {
    override val hasAuth: Boolean get() = clients.auth != null
    override val hasFirestore: Boolean get() = clients.firestore != null

    override fun currentUser(): AuthUserIdentity? = clients.auth?.currentUser?.toIdentity()

    override fun currentFirebaseUser(): FirebaseUser? = clients.auth?.currentUser

    override fun registerAuthStateListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit {
        val auth = clients.auth ?: return {}
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onChanged(firebaseAuth.currentUser?.toIdentity())
        }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }

    override fun registerIdTokenListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit {
        val auth = clients.auth ?: return {}
        val listener = FirebaseAuth.IdTokenListener { firebaseAuth ->
            onChanged(firebaseAuth.currentUser?.toIdentity())
        }
        auth.addIdTokenListener(listener)
        return { auth.removeIdTokenListener(listener) }
    }

    override fun fetchIdTokenAdminClaim(
        uid: String,
        forceRefresh: Boolean,
        onResult: (Result<Boolean>) -> Unit
    ) {
        val user = clients.auth?.currentUser
        if (user == null || user.uid != uid) {
            onResult(Result.failure(IllegalStateException("User mismatch or signed out")))
            return
        }
        user.getIdToken(forceRefresh)
            .addOnSuccessListener { tokenResult ->
                onResult(Result.success(tokenResult.claims["admin"] == true))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    override suspend fun reloadAndGetAdminClaim(forceRefresh: Boolean): Result<Pair<String, Boolean>?> =
        runCatching {
            val auth = clients.auth ?: error("Firebase Auth unavailable")
            val user = auth.currentUser ?: return@runCatching null
            if (forceRefresh) {
                runCatching { user.reload().await() }
            }
            val refreshedUser = auth.currentUser ?: return@runCatching null
            val tokenResult = refreshedUser.getIdToken(forceRefresh).await()
            val claim = refreshedUser.isEmailVerified && (tokenResult?.claims?.get("admin") == true)
            refreshedUser.uid to claim
        }

    override fun observeUserDocs(
        uid: String,
        onPublic: (loaded: Boolean, user: PublicUser?) -> Unit,
        onPrivate: (user: PrivateUser?) -> Unit,
        onAdminDoc: (exists: Boolean) -> Unit
    ): () -> Unit {
        val db = clients.firestore ?: return {}

        val publicReg: ListenerRegistration = db.collection("users_public").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    logger.e("HatchAuthFlow", "users_public listener error: code=${err.code}")
                    return@addSnapshotListener
                }
                val parsed = snap?.takeIf { it.exists() }
                    ?.toObject(PublicUser::class.java)
                    ?.copy(uid = snap.id)
                logger.d(
                    "HatchAuthFlow",
                    "users_public snapshot: exists=${snap?.exists() == true}, fromCache=${snap?.metadata?.isFromCache == true}, hasCity=${!parsed?.city.isNullOrBlank()}"
                )
                onPublic(true, parsed)
            }

        val privateReg: ListenerRegistration = db.collection("users_private").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    logger.w("HatchAuthFlow", "users_private listener error: code=${err.code}")
                    return@addSnapshotListener
                }
                val parsed = snap?.takeIf { it.exists() }
                    ?.toObject(PrivateUser::class.java)
                    ?.copy(uid = snap.id)
                onPrivate(parsed)
            }

        val adminReg: ListenerRegistration = db.collection("admins").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    logger.w("HatchAuthFlow", "admins listener error: code=${err.code}")
                    return@addSnapshotListener
                }
                onAdminDoc(snap != null && snap.exists())
            }

        return {
            publicReg.remove()
            privateReg.remove()
            adminReg.remove()
        }
    }

    override fun signOut() {
        clients.auth?.signOut()
    }

    private fun FirebaseUser.toIdentity(): AuthUserIdentity = AuthUserIdentity(
        uid = uid,
        isEmailVerified = isEmailVerified,
        isAnonymous = isAnonymous,
        firebaseUser = this
    )
}

@Singleton
class AuthRepository internal constructor(
    private val clients: FirebaseClientsHolder,
    private val gateway: AuthSessionGateway,
    private val sessionScope: CoroutineScope,
    private val logger: AuthSessionLogger = AndroidAuthSessionLogger,
    stopTimeoutMillis: Long = 5_000L
) {
    @Inject
    constructor(clients: FirebaseClientsHolder) : this(
        clients = clients,
        gateway = FirebaseAuthSessionGateway(clients),
        sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        logger = AndroidAuthSessionLogger,
        stopTimeoutMillis = 5_000L
    )

    private data class ClaimSyncEvent(
        val uid: String,
        val isEmailVerified: Boolean,
        val hasAdminClaim: Boolean,
        val verifiedWithServer: Boolean,
        val sequence: Long
    )

    val isConfigured: Boolean get() = clients.isConfigured
    private val claimSyncSeq = AtomicLong(0L)
    private val claimSyncState = MutableStateFlow<ClaimSyncEvent?>(null)

    private val sharedAuthSessionFlow: SharedFlow<AuthSessionState> by lazy {
        createRawAuthSessionFlow()
            .distinctUntilChanged(::areAuthSessionsEquivalent)
            .shareIn(
                scope = sessionScope,
                started = SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = stopTimeoutMillis,
                    replayExpirationMillis = 0L
                ),
                replay = 1
            )
    }

    fun currentFirebaseUser(): FirebaseUser? = gateway.currentFirebaseUser()

    fun observeAuthSession(): Flow<AuthSessionState> = sharedAuthSessionFlow

    private fun createRawAuthSessionFlow(): Flow<AuthSessionState> = callbackFlow {
        if (!gateway.hasAuth) {
            logger.w("HatchAuthFlow", "observeAuthSession aborted: FirebaseAuth is null")
            trySend(AuthSessionState())
            awaitClose {}
            return@callbackFlow
        }

        var cleanupUserDocs: (() -> Unit)? = null
        var currentPublic: PublicUser? = null
        var currentPrivate: PrivateUser? = null
        var hasAdminClaim = false
        var hasAdminDoc = false
        var publicLoaded = false
        var lastObservedUid: String? = null
        var lastObservedVerified: Boolean? = null
        var lastAdminVerifyUid: String? = null
        var lastAdminPromoteRefreshUid: String? = null
        var sessionSeqCutoff = claimSyncSeq.get()

        fun emitState(user: AuthUserIdentity?) {
            val liveUid = gateway.currentUser()?.uid
            if (user == null) {
                if (lastObservedUid != null || liveUid != null) return
                trySend(AuthSessionState(isFirebaseConnected = gateway.hasFirestore))
                return
            }
            if (lastObservedUid != user.uid) return
            if (liveUid != null && liveUid != user.uid) return
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
                    firebaseUser = user.firebaseUser,
                    uid = user.uid,
                    isEmailVerified = verifiedEmail,
                    publicProfile = currentPublic,
                    privateProfile = currentPrivate,
                    isAdmin = isAdmin,
                    hasAdminClaim = effectiveClaim,
                    hasAdminDoc = effectiveDoc,
                    needsProfileOnboarding = needsOnboarding,
                    isFirebaseConnected = gateway.hasFirestore
                )
            )
        }

        fun syncClaimsFromToken(user: AuthUserIdentity, forceRefresh: Boolean) {
            gateway.fetchIdTokenAdminClaim(user.uid, forceRefresh) { result ->
                val active = gateway.currentUser()
                if (active == null || active.uid != user.uid || lastObservedUid != user.uid) {
                    return@fetchIdTokenAdminClaim
                }
                result.onSuccess { rawClaim ->
                    val claim = active.isEmailVerified && rawClaim
                    // Never grant Admin privileges from a cached token before verifying with the server.
                    if (!forceRefresh && claim && lastAdminVerifyUid != active.uid) {
                        lastAdminVerifyUid = active.uid
                        syncClaimsFromToken(active, forceRefresh = true)
                        return@fetchIdTokenAdminClaim
                    }
                    if (forceRefresh) {
                        lastAdminVerifyUid = active.uid
                    }
                    val verificationChanged = lastObservedVerified != active.isEmailVerified
                    lastObservedVerified = active.isEmailVerified
                    if (hasAdminClaim != claim || verificationChanged) {
                        hasAdminClaim = claim
                        emitState(active)
                    }
                }.onFailure { e ->
                    logger.w("HatchAuthFlow", "getIdToken(forceRefresh=$forceRefresh) failed: ${e.javaClass.simpleName}")
                    if (forceRefresh) {
                        if (lastAdminVerifyUid == active.uid) {
                            lastAdminVerifyUid = null
                        }
                        if (lastAdminPromoteRefreshUid == active.uid) {
                            lastAdminPromoteRefreshUid = null
                        }
                        // Fallback cleanly to cached token when offline
                        gateway.fetchIdTokenAdminClaim(active.uid, forceRefresh = false) { cachedResult ->
                            val stillActive = gateway.currentUser()
                            if (stillActive != null && stillActive.uid == active.uid && lastObservedUid == active.uid) {
                                cachedResult.onSuccess { rawCached ->
                                    val cachedClaim = stillActive.isEmailVerified && rawCached
                                    val verificationChanged = lastObservedVerified != stillActive.isEmailVerified
                                    lastObservedVerified = stillActive.isEmailVerified
                                    if (hasAdminClaim != cachedClaim || verificationChanged) {
                                        hasAdminClaim = cachedClaim
                                        emitState(stillActive)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val claimSyncJob = launch {
            claimSyncState
                .filterNotNull()
                .filter { it.sequence > sessionSeqCutoff }
                .collect { event ->
                    val activeUser = gateway.currentUser()
                    if (activeUser != null && activeUser.uid == event.uid && lastObservedUid == event.uid) {
                        val effectiveClaim = activeUser.isEmailVerified && event.hasAdminClaim
                        val changed = hasAdminClaim != effectiveClaim ||
                            lastObservedVerified != activeUser.isEmailVerified
                        hasAdminClaim = effectiveClaim
                        lastObservedVerified = activeUser.isEmailVerified
                        if (event.verifiedWithServer) {
                            lastAdminVerifyUid = event.uid
                        }
                        if (changed) {
                            emitState(activeUser)
                        }
                    }
                }
        }

        val cleanupIdTokenListener = gateway.registerIdTokenListener { user ->
            if (user != null && user.uid == lastObservedUid && gateway.currentUser()?.uid == user.uid) {
                syncClaimsFromToken(user, forceRefresh = false)
            }
        }

        val cleanupAuthListener = gateway.registerAuthStateListener { user ->
            if (user != null && user.uid == lastObservedUid) {
                val verificationChanged = lastObservedVerified != user.isEmailVerified
                lastObservedVerified = user.isEmailVerified
                if (verificationChanged) {
                    syncClaimsFromToken(user, forceRefresh = false)
                    emitState(user)
                }
                return@registerAuthStateListener
            }

            sessionSeqCutoff = claimSyncSeq.get()
            if (claimSyncState.value?.uid != user?.uid) {
                claimSyncState.value = null
            }
            lastObservedUid = user?.uid
            lastObservedVerified = user?.isEmailVerified
            cleanupUserDocs?.invoke()
            cleanupUserDocs = null
            currentPublic = null
            currentPrivate = null
            hasAdminClaim = false
            hasAdminDoc = false
            publicLoaded = false
            lastAdminVerifyUid = null
            lastAdminPromoteRefreshUid = null

            logger.d(
                "HatchAuthFlow",
                "AuthStateListener fired: signedIn=${user != null}, isAnonymous=${user?.isAnonymous == true}, hasFirestore=${gateway.hasFirestore}"
            )
            if (user == null) {
                emitState(null)
                return@registerAuthStateListener
            }

            emitState(user)

            // Read cached claims locally first; only force server refresh if cached token
            // claims admin (to detect revocations) or when admins/{uid} appears/changes.
            syncClaimsFromToken(user, forceRefresh = false)

            if (!gateway.hasFirestore) {
                logger.e("HatchAuthFlow", "Firestore unavailable during auth session observation")
                return@registerAuthStateListener
            }

            val observedUid = user.uid
            cleanupUserDocs = gateway.observeUserDocs(
                uid = observedUid,
                onPublic = { loaded, parsed ->
                    if (lastObservedUid != observedUid || gateway.currentUser()?.uid != observedUid) {
                        return@observeUserDocs
                    }
                    publicLoaded = loaded
                    currentPublic = parsed
                    val active = gateway.currentUser()?.takeIf { it.uid == observedUid } ?: user
                    emitState(active)
                },
                onPrivate = { parsed ->
                    if (lastObservedUid != observedUid || gateway.currentUser()?.uid != observedUid) {
                        return@observeUserDocs
                    }
                    currentPrivate = parsed
                    val active = gateway.currentUser()?.takeIf { it.uid == observedUid } ?: user
                    emitState(active)
                },
                onAdminDoc = { docExists ->
                    if (lastObservedUid != observedUid || gateway.currentUser()?.uid != observedUid) {
                        return@observeUserDocs
                    }
                    val docChanged = hasAdminDoc != docExists
                    hasAdminDoc = docExists
                    val active = gateway.currentUser()?.takeIf { it.uid == observedUid } ?: user
                    if (docChanged && docExists && !hasAdminClaim && lastAdminPromoteRefreshUid != observedUid) {
                        // Admin doc appeared without claim yet; refresh token once in case claim was just minted.
                        lastAdminPromoteRefreshUid = observedUid
                        syncClaimsFromToken(active, forceRefresh = true)
                    } else if (docChanged && !docExists && hasAdminClaim) {
                        // Admin doc removed while client still holds cached admin claim; verify revocation.
                        syncClaimsFromToken(active, forceRefresh = true)
                    }
                    emitState(active)
                }
            )
        }

        awaitClose {
            claimSyncJob.cancel()
            cleanupAuthListener()
            cleanupIdTokenListener()
            cleanupUserDocs?.invoke()
            cleanupUserDocs = null
        }
    }

    private fun areAuthSessionsEquivalent(a: AuthSessionState, b: AuthSessionState): Boolean =
        a.uid == b.uid &&
            a.isEmailVerified == b.isEmailVerified &&
            a.publicProfile == b.publicProfile &&
            a.privateProfile == b.privateProfile &&
            a.isAdmin == b.isAdmin &&
            a.hasAdminClaim == b.hasAdminClaim &&
            a.hasAdminDoc == b.hasAdminDoc &&
            a.needsProfileOnboarding == b.needsProfileOnboarding &&
            a.isFirebaseConnected == b.isFirebaseConnected

    /**
     * Forces Firebase Auth to fetch a fresh ID token with updated custom claims for the current user.
     * Note: Refreshing the token on this client updates local claims (grant or revocation), while
     * previously minted tokens on other devices remain valid until their normal JWT expiry unless
     * refresh tokens are revoked via Firebase Admin SDK.
     */
    suspend fun refreshIdTokenClaims(forceRefresh: Boolean = true): Result<Boolean> = runCatching {
        if (!gateway.hasAuth) error("Firebase Auth unavailable")
        val resolved = gateway.reloadAndGetAdminClaim(forceRefresh).getOrThrow() ?: return@runCatching false
        val (uid, claim) = resolved
        val activeUser = gateway.currentUser()
        val effectiveClaim = activeUser != null && activeUser.uid == uid && activeUser.isEmailVerified && claim
        if (activeUser != null && activeUser.uid == uid) {
            claimSyncState.value = ClaimSyncEvent(
                uid = uid,
                isEmailVerified = activeUser.isEmailVerified,
                hasAdminClaim = effectiveClaim,
                verifiedWithServer = forceRefresh,
                sequence = claimSyncSeq.incrementAndGet()
            )
        }
        effectiveClaim
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
        gateway.signOut()
    }
}
