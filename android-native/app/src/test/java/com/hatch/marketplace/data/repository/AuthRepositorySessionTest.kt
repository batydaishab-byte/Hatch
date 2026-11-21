package com.hatch.marketplace.data.repository

import com.hatch.marketplace.data.model.PrivateUser
import com.hatch.marketplace.data.model.PublicUser
import com.hatch.marketplace.data.remote.FirebaseClientsHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositorySessionTest {

    private fun createRepo(
        gateway: FakeAuthSessionGateway,
        scope: kotlinx.coroutines.CoroutineScope,
        stopTimeoutMillis: Long = 0L
    ): AuthRepository = AuthRepository(
        clients = FirebaseClientsHolder(app = null, auth = null, firestore = null),
        gateway = gateway,
        sessionScope = scope,
        logger = NoOpAuthSessionLogger,
        stopTimeoutMillis = stopTimeoutMillis
    )

    @Test
    fun observeAuthSessionIsSharedAcrossMultipleCollectorsWithSingleFirebaseListenerSet() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope, stopTimeoutMillis = 0L)
        gateway.setUserClaims("user_1", cachedAdmin = false, serverAdmin = false)
        gateway.emitAuthState(AuthUserIdentity(uid = "user_1", isEmailVerified = true))

        val collectedByFourteenSubscribers = List(14) { mutableListOf<AuthSessionState>() }
        val jobs = collectedByFourteenSubscribers.map { bucket ->
            launch {
                repo.observeAuthSession().collect { bucket.add(it) }
            }
        }
        advanceUntilIdle()

        assertEquals(1, gateway.authListenerRegistrations)
        assertEquals(1, gateway.idTokenListenerRegistrations)
        assertEquals(1, gateway.userDocsRegistrations)
        assertEquals(1, gateway.activeAuthListenerCount)
        assertEquals(1, gateway.activeIdTokenListenerCount)
        assertEquals(1, gateway.activeUserDocsCount)
        assertEquals(0, gateway.forcedTokenRefreshCalls)

        gateway.emitUserDocs(
            uid = "user_1",
            publicUser = PublicUser(uid = "user_1", name = "أحمد", city = "الخرطوم"),
            emitPublic = true,
            privateUser = PrivateUser(uid = "user_1", rewardCredits = 3),
            emitPrivate = true
        )
        advanceUntilIdle()

        collectedByFourteenSubscribers.forEach { bucket ->
            assertTrue(bucket.isNotEmpty())
            assertEquals("user_1", bucket.last().uid)
            assertEquals("أحمد", bucket.last().publicProfile?.name)
            assertEquals(3, bucket.last().privateProfile?.rewardCredits)
            assertFalse(bucket.last().isAdmin)
        }

        jobs.forEach { it.cancelAndJoin() }
        advanceUntilIdle()

        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun regularUserDoesNotTriggerForcedTokenRefreshOnSignInOrTokenTick() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope)
        gateway.setUserClaims("regular_1", cachedAdmin = false, serverAdmin = false)

        val states = mutableListOf<AuthSessionState>()
        val job = launch { repo.observeAuthSession().collect { states.add(it) } }
        advanceUntilIdle()

        // Guest initially
        assertEquals(0, gateway.forcedTokenRefreshCalls)

        // Regular user signs in
        gateway.emitAuthState(AuthUserIdentity(uid = "regular_1", isEmailVerified = true))
        advanceUntilIdle()
        gateway.emitIdTokenRefresh()
        advanceUntilIdle()

        assertEquals(0, gateway.forcedTokenRefreshCalls)
        assertTrue(states.last().isSignedIn)
        assertFalse(states.last().isAdmin)

        job.cancelAndJoin()
        advanceUntilIdle()
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun adminDocAloneNeverGrantsAdminWithoutVerifiedServerClaimAndDetectsRevocation() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope)

        // User has admins/{uid} doc in Firestore, but token claim is still false
        gateway.setUserClaims("user_doc_only", cachedAdmin = false, serverAdmin = false)
        gateway.emitAuthState(AuthUserIdentity(uid = "user_doc_only", isEmailVerified = true))

        val states = mutableListOf<AuthSessionState>()
        val job = launch { repo.observeAuthSession().collect { states.add(it) } }
        advanceUntilIdle()

        gateway.emitUserDocs(uid = "user_doc_only", adminDocExists = true)
        advanceUntilIdle()

        val afterDocOnly = states.last()
        assertTrue(afterDocOnly.hasAdminDoc)
        assertFalse(afterDocOnly.hasAdminClaim)
        assertFalse(afterDocOnly.isAdmin)
        // Triggered 1 refresh because admin doc appeared while claim was false
        assertEquals(1, gateway.forcedTokenRefreshCalls)

        // Server mints admin custom claim -> refreshIdTokenClaims picks it up
        gateway.setUserClaims("user_doc_only", cachedAdmin = false, serverAdmin = true)
        val granted = repo.refreshIdTokenClaims(forceRefresh = true).getOrThrow()
        advanceUntilIdle()
        assertTrue(granted)
        assertTrue(states.last().isAdmin)
        assertTrue(states.last().hasAdminClaim)

        // Server revokes admin custom claim while admins doc remains -> refresh catches revocation
        gateway.setUserClaims("user_doc_only", cachedAdmin = true, serverAdmin = false)
        val stillAdmin = repo.refreshIdTokenClaims(forceRefresh = true).getOrThrow()
        advanceUntilIdle()
        assertFalse(stillAdmin)
        assertFalse(states.last().hasAdminClaim)
        assertFalse(states.last().isAdmin)

        // Re-grant after revocation via ClaimSyncEvent must not be dropped
        gateway.setUserClaims("user_doc_only", cachedAdmin = false, serverAdmin = true)
        val reGranted = repo.refreshIdTokenClaims(forceRefresh = true).getOrThrow()
        advanceUntilIdle()
        assertTrue(reGranted)
        assertTrue(states.last().hasAdminClaim)
        assertTrue(states.last().isAdmin)

        job.cancelAndJoin()
        advanceUntilIdle()
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun cachedAdminClaimVerifiesOnceAtSessionStartAndDetectsServerRevocation() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope)
        // Cached token still says admin=true, but server revoked it
        gateway.setUserClaims("revoked_admin", cachedAdmin = true, serverAdmin = false)
        gateway.emitAuthState(AuthUserIdentity(uid = "revoked_admin", isEmailVerified = true))

        val states = mutableListOf<AuthSessionState>()
        val job = launch { repo.observeAuthSession().collect { states.add(it) } }
        advanceUntilIdle()

        // Must never emit isAdmin=true or hasAdminClaim=true from unverified cached claim while online
        assertTrue(states.none { it.isAdmin })
        assertTrue(states.none { it.hasAdminClaim })
        assertEquals(1, gateway.forcedTokenRefreshCalls)
        assertFalse(states.last().hasAdminClaim)
        assertFalse(states.last().isAdmin)

        job.cancelAndJoin()
        advanceUntilIdle()
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun signOutAndAccountSwitchCleanUpPreviousListenersAndNeverLeakPreviousUserState() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope)
        gateway.setUserClaims("user_a", cachedAdmin = true, serverAdmin = true)
        gateway.setUserClaims("user_b", cachedAdmin = false, serverAdmin = false)

        val states = mutableListOf<AuthSessionState>()
        val job = launch { repo.observeAuthSession().collect { states.add(it) } }
        advanceUntilIdle()

        // Account A signs in as Admin with full profile
        gateway.emitAuthState(AuthUserIdentity(uid = "user_a", isEmailVerified = true))
        advanceUntilIdle()
        gateway.emitUserDocs(
            uid = "user_a",
            publicUser = PublicUser(uid = "user_a", name = "Admin A", city = "الخرطوم"),
            emitPublic = true,
            privateUser = PrivateUser(uid = "user_a", rewardCredits = 50),
            emitPrivate = true,
            adminDocExists = true
        )
        advanceUntilIdle()
        assertTrue(states.last().isAdmin)
        assertEquals("Admin A", states.last().publicProfile?.name)

        // Direct switch to Account B without intermediate null
        gateway.emitAuthState(AuthUserIdentity(uid = "user_b", isEmailVerified = true))
        advanceUntilIdle()

        val switchedState = states.last()
        assertEquals("user_b", switchedState.uid)
        assertNull(switchedState.publicProfile)
        assertNull(switchedState.privateProfile)
        assertFalse(switchedState.isAdmin)
        assertFalse(switchedState.hasAdminClaim)
        assertFalse(switchedState.hasAdminDoc)
        assertEquals(1, gateway.activeUserDocsCount)
        assertEquals(listOf("user_a", "user_b"), gateway.observedDocUidsHistory)

        // Late callback from user_a must be ignored
        gateway.emitStalePublicDocToAnyListener("user_a", PublicUser(uid = "user_a", name = "Stale A"))
        advanceUntilIdle()
        assertNull(states.last().publicProfile)

        // Sign out clears user_b completely
        repo.signOut()
        advanceUntilIdle()
        val signedOutState = states.last()
        assertFalse(signedOutState.isSignedIn)
        assertNull(signedOutState.uid)
        assertNull(signedOutState.publicProfile)
        assertNull(signedOutState.privateProfile)
        assertFalse(signedOutState.isAdmin)
        assertEquals(0, gateway.activeUserDocsCount)

        job.cancelAndJoin()
        advanceUntilIdle()
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun offlineFallbackPreservesCachedClaimsAndRecoversWhenBackOnline() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope)
        gateway.isNetworkOnline = false
        gateway.setUserClaims("admin_offline", cachedAdmin = true, serverAdmin = false)
        gateway.emitAuthState(AuthUserIdentity(uid = "admin_offline", isEmailVerified = true))

        val states = mutableListOf<AuthSessionState>()
        val job = launch { repo.observeAuthSession().collect { states.add(it) } }
        advanceUntilIdle()

        // Forced check failed offline -> fallback kept cached claim safely
        assertTrue(states.last().hasAdminClaim)
        assertTrue(states.last().isAdmin)

        // Refresh while still offline returns failure cleanly
        assertTrue(repo.refreshIdTokenClaims(forceRefresh = true).isFailure)
        advanceUntilIdle()
        assertTrue(states.last().isAdmin)

        // Internet returns -> forced refresh discovers server revocation
        gateway.isNetworkOnline = true
        val refreshed = repo.refreshIdTokenClaims(forceRefresh = true).getOrThrow()
        advanceUntilIdle()
        assertFalse(refreshed)
        assertFalse(states.last().isAdmin)
        assertFalse(states.last().hasAdminClaim)

        job.cancelAndJoin()
        advanceUntilIdle()
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }

    @Test
    fun sharedFlowRemovesAllListenersAfterStopTimeoutExpires() = runTest {
        val gateway = FakeAuthSessionGateway()
        val repo = createRepo(gateway, backgroundScope, stopTimeoutMillis = 5_000L)
        gateway.emitAuthState(AuthUserIdentity(uid = "user_timeout", isEmailVerified = true))

        val job = launch {
            repo.observeAuthSession().first { it.uid == "user_timeout" }
        }
        advanceUntilIdle()
        job.join()

        // Before 5s grace period elapses, listeners stay warm
        assertEquals(1, gateway.activeAuthListenerCount)
        advanceTimeBy(5_001L)
        advanceUntilIdle()

        // After grace period expires with zero collectors, listeners are removed cleanly
        assertEquals(0, gateway.activeAuthListenerCount)
        assertEquals(0, gateway.activeIdTokenListenerCount)
        assertEquals(0, gateway.activeUserDocsCount)
    }
}
