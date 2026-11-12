# HATCH Firebase Security Specification (Phase 0 TDD)

## 1. Core Data Invariants
1. **PII Isolation**: User email and phone numbers live strictly in `/users_private/{userId}` and are readable/writable ONLY by the account owner (`request.auth.uid == userId`) or a verified admin. Public profiles in `/users_public/{userId}` never contain email or phone numbers.
2. **No Self-Privilege Escalation**: A user creating or updating `/users_private/{userId}` can never set or alter `role` to `'admin'` or modify `rewardCredits` arbitrarily on update; admin status is verified strictly via `/admins/{uid}` or verified bootstrapped admin email (`batydaishab@gmail.com` with `email_verified == true`).
3. **Listing Ownership & Terminal Lock**: A product in `/products/{productId}` must have `sellerId == request.auth.uid` on creation, `createdAt == request.time`, and once `status == 'sold'`, non-admins cannot revert or alter listing data.
4. **Conversation Gatekeeping**: Access to `/conversations/{conversationId}` and `/conversations/{conversationId}/messages/{messageId}` is strictly limited to `buyerId` or `sellerId` (`request.auth.uid in resource.data.participantIds`) or verified admin. A message's `senderId` must strictly equal `request.auth.uid`.
5. **Server Timestamps & Bounded Arrays**: All `createdAt`/`updatedAt` timestamps must match `request.time`. All strings and arrays have explicit `.size()` bounds.
6. **Free-Tier Cost Protection**: Subcollections are used for messages, swipes, favorites, and notifications; `allow list` never performs `get()` or `exists()` lookups; images/videos reside in Firebase Storage, never inside Firestore documents.

## 2. "Dirty Dozen" Adversarial Payloads (Must Return `PERMISSION_DENIED`)
1. **Self-Promotion to Admin**: User creates `/users_private/{uid}` with `{ "uid": uid, "role": "admin", ... }`.
2. **Ghost Field Injection on Product**: Seller creates `/products/{id}` including `"isFeaturedBySystem": true` (not in `keys().hasOnly(...)`).
3. **Identity Spoofing on Product**: Attacker creates `/products/{id}` with `"sellerId": "other_user_uid"`.
4. **PII Scraping**: Signed-in non-admin attempts `get` or `list` on `/users_private/{otherUid}`.
5. **Fake Verified Email Spoof**: Unverified user (`email_verified: false`) claims admin email to modify `/ad_config/global`.
6. **Modifying Sold Listing**: Seller tries to edit `price` after `status == 'sold'`.
7. **Unauthorized Chat Eavesdrop**: Third-party signed-in user attempts `get` on `/conversations/{cid}/messages/{mid}`.
8. **Message Sender Impersonation**: Participant A posts message with `senderId: participantB`.
9. **Client Timestamp Forgery**: User submits `createdAt: "2020-01-01T00:00:00Z"` instead of `request.time`.
10. **ID Poisoning / Denial of Wallet**: Attacker writes document with 200-char ID or 100KB description string.
11. **Unbounded Array Bomb**: Product creation attempts 50 image URLs in `images` array (`> 8`).
12. **Sponsored Ad Tampering**: Non-admin user attempts to create or delete `/sponsored_ads/{adId}` or alter `/ad_config/global`.
