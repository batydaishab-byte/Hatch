# HATCH Firebase Security Specification (Hardened Zero-Trust Audit)

## 1. Core Data Invariants
1. **PII Isolation**: User email and phone numbers live strictly in `/users_private/{userId}` and are readable/writable ONLY by the account owner (`request.auth.uid == userId`) or a verified admin. Public profiles in `/users_public/{userId}` and `/commercial_profiles/{profileId}` never contain email or phone numbers.
2. **Server-Trusted Admin Authorization (No Email/Client-Role Bypass)**:
   - Client-writable `users_private.role` and hardcoded email checks never confer admin authority.
   - Admin authorization in Firestore Rules requires a verified auth token with Firebase Custom Claim (`request.auth.token.admin == true && request.auth.token.email_verified == true`) or a verified `/admins/{uid}` registry document (which only admins or Firebase Console/Admin SDK can write).
   - Collection-wide admin `list` queries use `request.auth.token.admin == true` directly so `allow list` performs zero `get()`/`exists()` lookups.
3. **Admin-Only Verification (Zero Auto-Verification)**:
   - `dealsCompleted` is strictly a counter of completed sales; reaching 10 deals NEVER alters `verifiedIdentity`.
   - Non-admin users can NEVER write `verifiedIdentity` on `/users_public/{uid}` or `/commercial_profiles/{uid}`, nor forge `sellerVerified` on `/products/{productId}`.
   - `products/{productId}` creation enforces `sellerVerified == get(/users_public/$(request.auth.uid)).data.verifiedIdentity`.
   - Users submit verification applications to `/verification_requests/{requestId}` (`status == 'pending'`). Only admins can approve, reject, or revoke verification across `verification_requests`, `users_public`, `commercial_profiles`, and active seller `products`.
4. **Anti-Tampering Listing Protection**:
   - No non-admin user can set `status = 'flagged'` or modify another user's product `status`.
   - Submitting a report in `/reports/{reportId}` creates a `pending` ticket for admin moderation and NEVER disables or modifies `/products/{productId}`.
   - Sellers can only edit allowed descriptive/pricing fields of their own `active` listings, or transition their own `active` listing to `sold` (with atomic `+1` `dealsCompleted` bound to `lastSoldProductId`). Once `sold` or `flagged`/`under_review`, non-admins cannot alter listing fields.
5. **Strict Offer & Chat State Machine**:
   - Conversation `buyerId`, `sellerId`, `participantIds`, and `productId` are immutable and tied to an `active` product where `sellerId == product.sellerId`.
   - Offer transitions are actor- and state-locked:
     - Buyer (`request.auth.uid == buyerId`) ONLY: creates/submits `pending` offer with `offerAmount > 0` (never overwriting `accepted`).
     - Seller (`request.auth.uid == sellerId`) ONLY: responds to `pending` with `accepted`, `declined`, or `countered` (`counterAmount > 0`).
     - Buyer (`request.auth.uid == buyerId`) ONLY: responds to `countered` with `accepted` or `declined`.
     - Terminal lock: `accepted` is final (`acceptedBy == request.auth.uid`, `acceptedAt == request.time`, `offerUpdatedBy == request.auth.uid`), cannot be overwritten or repeated, and never alters `offerAmount`/`counterAmount` during acceptance.
     - `isOfferUpdate == true` chat messages require an atomic parent conversation offer update in the same transaction (`getAfter`).
6. **Server-Side Suspension Enforcement**:
   - `isNotSuspended()` checks `/users_public/{request.auth.uid}` (`status == 'active'` OR expired `suspendedUntilTs < request.time`).
   - Suspended users are denied `create`/`update`/`delete` across `/products`, `/conversations`, `/messages`, `/reviews`, `/reports`, `/verification_requests`, `/credit_transactions`, `/swipes`, `/favorites`, and profile updates, while guest read-only catalog browsing remains open.
   - Non-admins can NEVER modify `status`, `suspendedUntil`, `suspendedUntilTs`, `suspensionDays`, `suspensionReason`, `dealsCompleted` (except verified `+1` sale), `reportsCount`, or `verifiedIdentity`.
7. **Atomic Reward Credits & Featured Boosts + Immutable Audit Ledger**:
   - Users can NEVER increment `users_private/{uid}.rewardCredits` directly or set `products/{pid}.featured = true` directly.
   - Admin credit grants require `isAdmin()`, atomically increment `rewardCredits`, and write an `admin_grant` record to `/credit_transactions/{txId}`.
   - Ad boosting by a listing owner requires a 3-document atomic transaction/batch verified via `get()`/`getAfter()`/`existsAfter()`:
     1. `/users_private/{uid}` decrements `rewardCredits` by `-1` (requiring `rewardCredits >= 1`) and records `lastCreditTxId`.
     2. `/products/{productId}` transitions `featured` from `false` to `true` (requiring `active` + owner) and records `featuredTxId`.
     3. `/credit_transactions/{txId}` creates an immutable `ad_boost_spend` (`amount == -1`, `balanceBefore`, `balanceAfter`) linked to that `txId` and `productId`, preventing replay and double-spending.
   - `/credit_transactions/{txId}` forbids all client `update` and `delete` operations (`allow update, delete: if false;`).
8. **Server Timestamps, Bounded Arrays & Free-Tier Cost Protection**:
   - All `createdAt`/`updatedAt`/`acceptedAt`/`reviewedAt` timestamps must match `request.time`. All strings and lists (`images`, `tags`, `fcmTokens`, `offerHistory`, `participantIds`) have strict `.size()` bounds. No `get()`/`exists()` inside `allow list`.

## 2. Adversarial Payloads (Must Return `PERMISSION_DENIED`)
1. **Self-Promotion to Admin**: User creates/updates `/users_private/{uid}` with `role: "admin"`.
2. **Hardcoded Email Spoof Without Claim/Admin Doc**: Non-admin email or unverified email attempts admin writes on `/ad_config`, `/verification_requests`, or `/products`.
3. **Self-Verification & 10-Deal Auto-Verify Bypass**: User updates `/users_public/{uid}` setting `verifiedIdentity: true` when `dealsCompleted` reaches 10 (or directly).
4. **Seller Verified Flag Spoofing**: Unverified seller creates `/products/{id}` with `sellerVerified: true`.
5. **Competitor Listing Sabotage (Flagging)**: Signed-in competitor attempts to update `/products/{otherProductId}` to `status: "flagged"`.
6. **Modifying Sold or Flagged Listing**: Seller tries to edit `price` or restore `status: "active"` after `status` is `'sold'` or `'flagged'`.
7. **Buyer Accepting Own Pending Offer**: Buyer updates `/conversations/{cid}` from `offerStatus: "pending"` directly to `"accepted"` (or jumps from `"none"` to `"accepted"`).
8. **Seller Accepting Own Counter Offer / Mutating Amount**: Seller updates `offerStatus: "countered"` to `"accepted"`, or alters `offerAmount` while accepting.
9. **Suspended User Action Bypass**: User with `status: "suspended"` and active `suspendedUntilTs > request.time` attempts to create a product, send a chat/message, submit a review, submit a report, or update profile.
10. **Self-Granting Reward Credits**: Non-admin user updates `/users_private/{uid}` to increase `rewardCredits` by `+1`.
11. **Free Featured Listing & Double-Spend Replay**: Seller updates `/products/{id}` to `featured: true` without an atomic `-1` credit transaction, or replays an existing `featuredTxId` / `lastCreditTxId`.
12. **Credit Ledger Tampering**: User or admin attempts to `update` or `delete` an existing `/credit_transactions/{txId}` audit document.
13. **PII & Private Query Scraping**: Signed-in non-admin attempts `get` or `list` on `/users_private/{otherUid}` or `/verification_requests` belonging to another user.
14. **Client Timestamp / ID Poisoning / Unbounded Array Bomb**: Submitting non-`request.time` timestamps, >128-char IDs, or oversized arrays/strings.
