package com.hatch.marketplace.data.repository

import com.google.firebase.auth.FirebaseUser
import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.PublicUser
import java.io.IOException

internal object NoOpAuthSessionLogger : AuthSessionLogger {
    override fun d(tag: String, message: String) = Unit
    override fun w(tag: String, message: String) = Unit
    override fun e(tag: String, message: String) = Unit
}

internal class FakeAuthSessionGateway(
    override val hasAuth: Boolean = true,
    override val hasFirestore: Boolean = true
) : AuthSessionGateway {

    private var activeUser: AuthUserIdentity? = null
    private val cachedAdminClaims = mutableMapOf<String, Boolean>()
    private val serverAdminClaims = mutableMapOf<String, Boolean>()

    var isNetworkOnline: Boolean = true

    var authListenerRegistrations: Int = 0
        private set
    var authListenerRemovals: Int = 0
        private set
    var idTokenListenerRegistrations: Int = 0
        private set
    var idTokenListenerRemovals: Int = 0
        private set
    var userDocsRegistrations: Int = 0
        private set
    var userDocsRemovals: Int = 0
        private set
    var cachedTokenFetchCalls: Int = 0
        private set
    var forcedTokenRefreshCalls: Int = 0
        private set
    var reloadAndRefreshCalls: Int = 0
        private set

    val observedDocUidsHistory = mutableListOf<String>()
    val activeAuthListenerCount: Int get() = authListenerRegistrations - authListenerRemovals
    val activeIdTokenListenerCount: Int get() = idTokenListenerRegistrations - idTokenListenerRemovals
    val activeUserDocsCount: Int get() = userDocsRegistrations - userDocsRemovals

    private val authListeners = linkedSetOf<(AuthUserIdentity?) -> Unit>()
    private val idTokenListeners = linkedSetOf<(AuthUserIdentity?) -> Unit>()

    private data class DocCallbacks(
        val uid: String,
        val onPublic: (Boolean, PublicUser?) -> Unit,
        val onPrivate: (PrivateUser?) -> Unit,
        val onAdminDoc: (Boolean) -> Unit
    )

    private val activeDocCallbacks = linkedMapOf<Int, DocCallbacks>()
    private var nextDocId = 1

    fun setUserClaims(uid: String, cachedAdmin: Boolean, serverAdmin: Boolean = cachedAdmin) {
        cachedAdminClaims[uid] = cachedAdmin
        serverAdminClaims[uid] = serverAdmin
    }

    fun emitAuthState(user: AuthUserIdentity?) {
        activeUser = user
        authListeners.toList().forEach { it(user) }
    }

    fun emitIdTokenRefresh(user: AuthUserIdentity? = activeUser) {
        idTokenListeners.toList().forEach { it(user) }
    }

    fun emitUserDocs(
        uid: String,
        publicUser: PublicUser? = null,
        emitPublic: Boolean = false,
        privateUser: PrivateUser? = null,
        emitPrivate: Boolean = false,
        adminDocExists: Boolean? = null
    ) {
        activeDocCallbacks.values.filter { it.uid == uid }.toList().forEach { cb ->
            if (emitPublic) cb.onPublic(true, publicUser)
            if (emitPrivate) cb.onPrivate(privateUser)
            if (adminDocExists != null) cb.onAdminDoc(adminDocExists)
        }
    }

    fun emitStalePublicDocToAnyListener(uid: String, staleUser: PublicUser) {
        activeDocCallbacks.values.filter { it.uid == uid }.toList().forEach { cb ->
            cb.onPublic(true, staleUser)
        }
    }

    override fun currentUser(): AuthUserIdentity? = activeUser

    override fun currentFirebaseUser(): FirebaseUser? = activeUser?.firebaseUser

    override fun registerAuthStateListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit {
        authListenerRegistrations++
        authListeners.add(onChanged)
        onChanged(activeUser)
        return {
            if (authListeners.remove(onChanged)) {
                authListenerRemovals++
            }
        }
    }

    override fun registerIdTokenListener(onChanged: (AuthUserIdentity?) -> Unit): () -> Unit {
        idTokenListenerRegistrations++
        idTokenListeners.add(onChanged)
        onChanged(activeUser)
        return {
            if (idTokenListeners.remove(onChanged)) {
                idTokenListenerRemovals++
            }
        }
    }

    override fun fetchIdTokenAdminClaim(
        uid: String,
        forceRefresh: Boolean,
        onResult: (Result<Boolean>) -> Unit
    ) {
        if (forceRefresh) {
            forcedTokenRefreshCalls++
            if (!isNetworkOnline) {
                onResult(Result.failure(IOException("Network unavailable")))
                return
            }
            val latest = serverAdminClaims[uid] ?: false
            cachedAdminClaims[uid] = latest
            onResult(Result.success(latest))
        } else {
            cachedTokenFetchCalls++
            onResult(Result.success(cachedAdminClaims[uid] ?: false))
        }
    }

    override suspend fun reloadAndGetAdminClaim(forceRefresh: Boolean): Result<Pair<String, Boolean>?> {
        if (forceRefresh) {
            reloadAndRefreshCalls++
            forcedTokenRefreshCalls++
        } else {
            cachedTokenFetchCalls++
        }
        val user = activeUser ?: return Result.success(null)
        if (forceRefresh && !isNetworkOnline) {
            return Result.failure(IOException("Network unavailable"))
        }
        val claim = if (forceRefresh) {
            val fresh = serverAdminClaims[user.uid] ?: false
            cachedAdminClaims[user.uid] = fresh
            user.isEmailVerified && fresh
        } else {
            user.isEmailVerified && (cachedAdminClaims[user.uid] ?: false)
        }
        return Result.success(user.uid to claim)
    }

    override fun observeUserDocs(
        uid: String,
        onPublic: (loaded: Boolean, user: PublicUser?) -> Unit,
        onPrivate: (user: PrivateUser?) -> Unit,
        onAdminDoc: (exists: Boolean) -> Unit
    ): () -> Unit {
        userDocsRegistrations++
        observedDocUidsHistory.add(uid)
        val id = nextDocId++
        activeDocCallbacks[id] = DocCallbacks(uid, onPublic, onPrivate, onAdminDoc)
        return {
            if (activeDocCallbacks.remove(id) != null) {
                userDocsRemovals++
            }
        }
    }

    override fun signOut() {
        emitAuthState(null)
    }
}
