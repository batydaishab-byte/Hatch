import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
  type Firestore,
} from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const PROJECT_ID = 'rare-lambda-l7cdj';
let testEnv: RulesTestEnvironment;

function dbFor(uid: string, token?: Record<string, unknown>): Firestore {
  return testEnv
    .authenticatedContext(uid, { email_verified: true, ...token })
    .firestore() as unknown as Firestore;
}

function adminClaimDb(uid = 'admin_1'): Firestore {
  return testEnv
    .authenticatedContext(uid, {
      email: 'admin-ops@hatch.market',
      email_verified: true,
      admin: true,
    })
    .firestore() as unknown as Firestore;
}

function guestDb(): Firestore {
  return testEnv.unauthenticatedContext().firestore() as unknown as Firestore;
}

async function seedBaselineData() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore() as unknown as Firestore;
    const now = Timestamp.now();
    const futureTs = Timestamp.fromMillis(Date.now() + 7 * 86_400_000);
    const pastTs = Timestamp.fromMillis(Date.now() - 86_400_000);

    await setDoc(doc(db, 'admins', 'admin_1'), {
      uid: 'admin_1',
      grantedAt: now,
    });

    await setDoc(doc(db, 'users_public', 'seller_1'), {
      uid: 'seller_1',
      name: 'بائع موثوق',
      city: 'الخرطوم',
      rating: 4.9,
      dealsCompleted: 9,
      reportsCount: 0,
      verifiedIdentity: false,
      status: 'active',
      accountType: 'personal',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_private', 'seller_1'), {
      uid: 'seller_1',
      email: 'seller1@example.com',
      phone: '+249912345678',
      role: 'user',
      rewardCredits: 1,
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_public', 'buyer_1'), {
      uid: 'buyer_1',
      name: 'مشتري جاد',
      city: 'أم درمان',
      rating: 5.0,
      dealsCompleted: 2,
      reportsCount: 0,
      verifiedIdentity: false,
      status: 'active',
      accountType: 'personal',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_private', 'buyer_1'), {
      uid: 'buyer_1',
      email: 'buyer1@example.com',
      phone: '+249922222222',
      role: 'user',
      rewardCredits: 0,
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_public', 'suspended_1'), {
      uid: 'suspended_1',
      name: 'حساب موقوف',
      city: 'عطبرة',
      rating: 3.5,
      dealsCompleted: 1,
      reportsCount: 3,
      verifiedIdentity: false,
      status: 'suspended',
      suspensionDays: 7,
      suspendedUntil: futureTs.toDate().toISOString(),
      suspendedUntilTs: futureTs,
      suspensionReason: 'طلب عربون مسبق',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_public', 'expired_susp_1'), {
      uid: 'expired_susp_1',
      name: 'حساب انتهى إيقافه',
      city: 'بورتسودان',
      rating: 4.5,
      dealsCompleted: 1,
      reportsCount: 1,
      verifiedIdentity: false,
      status: 'suspended',
      suspensionDays: 1,
      suspendedUntil: pastTs.toDate().toISOString(),
      suspendedUntilTs: pastTs,
      suspensionReason: 'إيقاف منتهي',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_public', 'store_1'), {
      uid: 'store_1',
      name: 'متجر النيل للهواتف',
      city: 'الخرطوم',
      rating: 4.8,
      dealsCompleted: 4,
      verifiedIdentity: true,
      status: 'active',
      accountType: 'store',
      businessActivity: 'هواتف ذكية',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'commercial_profiles', 'store_1'), {
      uid: 'store_1',
      name: 'متجر النيل للهواتف',
      city: 'الخرطوم',
      rating: 4.8,
      dealsCompleted: 4,
      verifiedIdentity: true,
      status: 'active',
      accountType: 'store',
      businessActivity: 'هواتف ذكية',
      joinedDate: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'products', 'prod_active_1'), {
      id: 'prod_active_1',
      titleAr: 'آيفون 15 برو بحالة ممتازة',
      descriptionAr: 'جهاز نظيف جداً مع الكرتونة الأصلية والمعاينة يداً بيد.',
      price: 4500000,
      category: 'phones',
      condition: 'like_new',
      city: 'الخرطوم',
      images: ['https://example.com/iphone.webp'],
      sellerId: 'seller_1',
      sellerName: 'بائع موثوق',
      sellerVerified: false,
      sellerAccountType: 'personal',
      paymentMethods: ['bankak', 'cash'],
      negotiable: true,
      deliveryAvailable: false,
      urgentSale: false,
      featured: false,
      viewsCount: 10,
      likesCount: 2,
      status: 'active',
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'products', 'prod_sold_1'), {
      id: 'prod_sold_1',
      titleAr: 'لابتوب تم بيعه',
      descriptionAr: 'لابتوب ديل مستعمل بحالة ممتازة وتم البيع.',
      price: 1200000,
      category: 'electronics',
      condition: 'good',
      city: 'الخرطوم',
      images: ['https://example.com/laptop.webp'],
      sellerId: 'seller_1',
      sellerName: 'بائع موثوق',
      sellerVerified: false,
      paymentMethods: ['cash'],
      negotiable: false,
      deliveryAvailable: false,
      featured: false,
      viewsCount: 25,
      likesCount: 5,
      status: 'sold',
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'products', 'prod_flagged_1'), {
      id: 'prod_flagged_1',
      titleAr: 'إعلان موقوف للمراجعة',
      descriptionAr: 'إعلان تم تعليقه من الإدارة لمراجعة المخالفة.',
      price: 500000,
      category: 'phones',
      condition: 'good',
      city: 'الخرطوم',
      images: ['https://example.com/flagged.webp'],
      sellerId: 'seller_1',
      sellerName: 'بائع موثوق',
      sellerVerified: false,
      paymentMethods: ['cash'],
      negotiable: false,
      deliveryAvailable: false,
      featured: false,
      viewsCount: 3,
      likesCount: 0,
      status: 'flagged',
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'products', 'prod_under_review_1'), {
      id: 'prod_under_review_1',
      titleAr: 'إعلان قيد التدقيق',
      descriptionAr: 'إعلان قيد المراجعة الإدارية حالياً.',
      price: 400000,
      category: 'phones',
      condition: 'good',
      city: 'الخرطوم',
      images: ['https://example.com/review.webp'],
      sellerId: 'seller_1',
      sellerName: 'بائع موثوق',
      sellerVerified: false,
      paymentMethods: ['cash'],
      negotiable: false,
      deliveryAvailable: false,
      featured: false,
      viewsCount: 2,
      likesCount: 0,
      status: 'under_review',
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'conversations', 'conv_1'), {
      id: 'conv_1',
      productId: 'prod_active_1',
      buyerId: 'buyer_1',
      sellerId: 'seller_1',
      participantIds: ['buyer_1', 'seller_1'],
      lastMessage: 'عرض شراء: 4200000 ج.س',
      unreadByBuyer: 0,
      unreadBySeller: 1,
      offerStatus: 'pending',
      offerAmount: 4200000,
      offerUpdatedBy: 'buyer_1',
      offerHistory: [
        {
          actorId: 'buyer_1',
          status: 'pending',
          amount: 4200000,
          atMillis: 1700000000000,
        },
      ],
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'conversations', 'conv_sold_1'), {
      id: 'conv_sold_1',
      productId: 'prod_sold_1',
      buyerId: 'buyer_1',
      sellerId: 'seller_1',
      participantIds: ['buyer_1', 'seller_1'],
      lastMessage: 'تم قبول العرض (1200000 ج.س)',
      unreadByBuyer: 0,
      unreadBySeller: 0,
      offerStatus: 'accepted',
      offerAmount: 1200000,
      offerUpdatedBy: 'seller_1',
      acceptedBy: 'seller_1',
      acceptedAt: now,
      offerHistory: [
        {
          actorId: 'buyer_1',
          status: 'pending',
          amount: 1200000,
          atMillis: 1700000000000,
        },
        {
          actorId: 'seller_1',
          status: 'accepted',
          amount: 1200000,
          atMillis: 1700000005000,
        },
      ],
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'sponsored_ads', 'ad_active_1'), {
      id: 'ad_active_1',
      titleAr: 'إعلان نشط',
      advertiser: 'متجر النيل',
      format: 'banner',
      isActive: true,
      durationSeconds: 10,
      clicksCount: 0,
      impressionsCount: 0,
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'sponsored_ads', 'ad_inactive_1'), {
      id: 'ad_inactive_1',
      titleAr: 'إعلان غير نشط',
      advertiser: 'شركة س',
      format: 'interstitial',
      isActive: false,
      durationSeconds: 15,
      clicksCount: 0,
      impressionsCount: 0,
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_private', 'buyer_1', 'notifications', 'notif_buyer_1'), {
      id: 'notif_buyer_1',
      type: 'system',
      titleAr: 'إشعار ترحيبي',
      bodyAr: 'مرحباً بك في هتش',
      read: false,
      createdAt: now,
    });

    await setDoc(doc(db, 'users_private', 'suspended_1'), {
      uid: 'suspended_1',
      email: 'susp@example.com',
      phone: '+249933333333',
      role: 'user',
      rewardCredits: 0,
      createdAt: now,
      updatedAt: now,
    });

    await setDoc(doc(db, 'users_private', 'suspended_1', 'notifications', 'notif_susp_1'), {
      id: 'notif_susp_1',
      type: 'alert',
      titleAr: 'إشعار إيقاف',
      bodyAr: 'تم إيقاف الحساب مؤقتاً',
      read: false,
      createdAt: now,
    });

    await setDoc(doc(db, 'credit_transactions', 'ctx_seed_1'), {
      id: 'ctx_seed_1',
      uid: 'seller_1',
      type: 'admin_grant',
      amount: 1,
      balanceBefore: 0,
      balanceAfter: 1,
      grantedBy: 'admin_1',
      reason: 'منحة ترحيبية',
      createdAt: now,
    });
  });
}

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync('firestore.rules', 'utf8'),
    },
  });
});

afterAll(async () => {
  await testEnv?.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await seedBaselineData();
});

describe('HATCH Zero-Trust Firestore Security Rules', () => {
  it('blocks PII access for guests and other users while keeping commercial directory readable', async () => {
    await assertSucceeds(
      getDocs(
        query(
          collection(guestDb(), 'commercial_profiles'),
          where('status', '==', 'active'),
          where('accountType', 'in', ['store', 'company'])
        )
      )
    );
    await assertFails(getDoc(doc(guestDb(), 'users_private', 'seller_1')));
    await assertFails(getDoc(doc(dbFor('buyer_1'), 'users_private', 'seller_1')));
    await assertSucceeds(getDoc(doc(dbFor('seller_1'), 'users_private', 'seller_1')));
  });

  it('prevents self-promotion to admin and email spoofing without Custom Claim / /admins doc', async () => {
    const attacker = dbFor('attacker_1', {
      email: 'batydaishab@gmail.com',
      email_verified: false,
    });
    await assertFails(
      setDoc(doc(attacker, 'users_private', 'attacker_1'), {
        uid: 'attacker_1',
        email: 'batydaishab@gmail.com',
        phone: '+249911111111',
        role: 'admin',
        rewardCredits: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );
    await assertFails(
      setDoc(doc(attacker, 'admins', 'attacker_1'), {
        uid: 'attacker_1',
      })
    );
    await assertFails(
      updateDoc(doc(dbFor('seller_1'), 'users_private', 'seller_1'), {
        role: 'admin',
        updatedAt: serverTimestamp(),
      })
    );
  });

  it('blocks self-verification & 10-deal auto-verify bypass while allowing admin-only Gold badge', async () => {
    const sellerDb = dbFor('seller_1');

    // Seller cannot flip verifiedIdentity directly even after dealsCompleted
    await assertFails(
      updateDoc(doc(sellerDb, 'users_public', 'seller_1'), {
        verifiedIdentity: true,
        updatedAt: serverTimestamp(),
      })
    );

    // Unverified seller cannot spoof sellerVerified: true on new listing
    await assertFails(
      setDoc(doc(sellerDb, 'products', 'prod_spoof'), {
        id: 'prod_spoof',
        titleAr: 'هاتف موثق مزيف',
        descriptionAr: 'محاولة تزوير شارة التوثيق الذهبية',
        price: 100000,
        category: 'phones',
        condition: 'good',
        city: 'الخرطوم',
        images: ['https://example.com/p.webp'],
        sellerId: 'seller_1',
        sellerName: 'بائع موثوق',
        sellerVerified: true,
        paymentMethods: ['cash'],
        negotiable: true,
        deliveryAvailable: false,
        viewsCount: 0,
        likesCount: 0,
        status: 'active',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );

    // Seller can submit verification_request and Admin can approve it
    await assertSucceeds(
      setDoc(doc(sellerDb, 'verification_requests', 'vreq_1'), {
        id: 'vreq_1',
        uid: 'seller_1',
        userName: 'بائع موثوق',
        accountType: 'personal',
        city: 'الخرطوم',
        note: 'أرجو مراجعة حسابي لمنح شارة التوثيق الذهبية.',
        status: 'pending',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );

    const admin = adminClaimDb();
    await assertSucceeds(
      updateDoc(doc(admin, 'verification_requests', 'vreq_1'), {
        status: 'approved',
        reviewedBy: 'admin_1',
        reviewedAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );
    await assertSucceeds(
      updateDoc(doc(admin, 'users_public', 'seller_1'), {
        verifiedIdentity: true,
        updatedAt: serverTimestamp(),
      })
    );
  });

  it('prevents competitors from flagging or altering another seller product status', async () => {
    const competitor = dbFor('buyer_1');
    await assertFails(
      updateDoc(doc(competitor, 'products', 'prod_active_1'), {
        status: 'flagged',
        updatedAt: serverTimestamp(),
      })
    );

    // Seller also cannot alter already-sold listing
    await assertFails(
      updateDoc(doc(dbFor('seller_1'), 'products', 'prod_sold_1'), {
        price: 9999999,
        status: 'active',
        updatedAt: serverTimestamp(),
      })
    );

    // Competitor CAN file a pending report without touching product status
    await assertSucceeds(
      setDoc(doc(competitor, 'reports', 'rep_1'), {
        id: 'rep_1',
        reporterId: 'buyer_1',
        reporterName: 'مشتري جاد',
        reportedProductId: 'prod_active_1',
        reportedUserId: 'seller_1',
        reason: 'counterfeit',
        message: 'إبلاغ للمراجعة الإدارية فقط.',
        status: 'pending',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );
  });

  it('enforces strict Offer & Chat state machine and append-only offerHistory', async () => {
    const buyer = dbFor('buyer_1');
    const seller = dbFor('seller_1');

    // Buyer CANNOT accept their own pending offer
    await assertFails(
      updateDoc(doc(buyer, 'conversations', 'conv_1'), {
        offerStatus: 'accepted',
        acceptedBy: 'buyer_1',
        acceptedAt: serverTimestamp(),
        offerUpdatedBy: 'buyer_1',
        offerHistory: [
          { actorId: 'buyer_1', status: 'pending', amount: 4200000, atMillis: 1700000000000 },
          { actorId: 'buyer_1', status: 'accepted', amount: 4200000, atMillis: 1700000001000 },
        ],
        updatedAt: serverTimestamp(),
      })
    );

    // Regular chat update CANNOT mutate offerHistory
    await assertFails(
      updateDoc(doc(buyer, 'conversations', 'conv_1'), {
        lastMessage: 'رسالة عادية',
        offerHistory: [],
        updatedAt: serverTimestamp(),
      })
    );

    // Seller CANNOT shrink, delete, or rewrite previous offerHistory entries
    await assertFails(
      updateDoc(doc(seller, 'conversations', 'conv_1'), {
        offerStatus: 'accepted',
        acceptedBy: 'seller_1',
        acceptedAt: serverTimestamp(),
        offerUpdatedBy: 'seller_1',
        offerHistory: [
          { actorId: 'buyer_1', status: 'pending', amount: 9999999, atMillis: 1700000000000 },
          { actorId: 'seller_1', status: 'accepted', amount: 9999999, atMillis: 1700000001000 },
        ],
        lastMessage: 'تم قبول العرض',
        unreadByBuyer: 1,
        unreadBySeller: 0,
        updatedAt: serverTimestamp(),
      })
    );

    // Seller CANNOT append an entry with a spoofed actorId or wrong status
    await assertFails(
      updateDoc(doc(seller, 'conversations', 'conv_1'), {
        offerStatus: 'accepted',
        acceptedBy: 'seller_1',
        acceptedAt: serverTimestamp(),
        offerUpdatedBy: 'seller_1',
        offerHistory: [
          { actorId: 'buyer_1', status: 'pending', amount: 4200000, atMillis: 1700000000000 },
          { actorId: 'buyer_1', status: 'accepted', amount: 4200000, atMillis: 1700000001000 },
        ],
        lastMessage: 'تم قبول العرض',
        unreadByBuyer: 1,
        unreadBySeller: 0,
        updatedAt: serverTimestamp(),
      })
    );

    // Seller accepts pending offer with valid append-only offerHistory -> succeeds
    await assertSucceeds(
      updateDoc(doc(seller, 'conversations', 'conv_1'), {
        offerStatus: 'accepted',
        acceptedBy: 'seller_1',
        acceptedAt: serverTimestamp(),
        offerUpdatedBy: 'seller_1',
        offerHistory: [
          { actorId: 'buyer_1', status: 'pending', amount: 4200000, atMillis: 1700000000000 },
          { actorId: 'seller_1', status: 'accepted', amount: 4200000, atMillis: 1700000001000 },
        ],
        lastMessage: 'تم قبول العرض',
        unreadByBuyer: 1,
        unreadBySeller: 0,
        updatedAt: serverTimestamp(),
      })
    );

    // Terminal lock: neither buyer nor seller can overwrite an accepted offer
    await assertFails(
      updateDoc(doc(buyer, 'conversations', 'conv_1'), {
        offerStatus: 'pending',
        offerAmount: 1000,
        offerUpdatedBy: 'buyer_1',
        offerHistory: [
          { actorId: 'buyer_1', status: 'pending', amount: 4200000, atMillis: 1700000000000 },
          { actorId: 'seller_1', status: 'accepted', amount: 4200000, atMillis: 1700000001000 },
          { actorId: 'buyer_1', status: 'pending', amount: 1000, atMillis: 1700000002000 },
        ],
        updatedAt: serverTimestamp(),
      })
    );
  });

  it('blocks active suspended users server-side and allows writes once suspension expires', async () => {
    const suspended = dbFor('suspended_1');
    await assertFails(
      setDoc(doc(suspended, 'products', 'prod_susp_blocked'), {
        id: 'prod_susp_blocked',
        titleAr: 'إعلان من حساب موقوف',
        descriptionAr: 'يجب منع الحساب الموقوف من النشر.',
        price: 50000,
        category: 'phones',
        condition: 'good',
        city: 'عطبرة',
        images: ['https://example.com/s.webp'],
        sellerId: 'suspended_1',
        sellerName: 'حساب موقوف',
        sellerVerified: false,
        paymentMethods: ['cash'],
        negotiable: true,
        deliveryAvailable: false,
        viewsCount: 0,
        likesCount: 0,
        status: 'active',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );

    // Expired suspension (suspendedUntilTs < request.time) IS permitted
    const expired = dbFor('expired_susp_1');
    await assertSucceeds(
      setDoc(doc(expired, 'products', 'prod_expired_ok'), {
        id: 'prod_expired_ok',
        titleAr: 'إعلان بعد انتهاء الإيقاف',
        descriptionAr: 'انتهت مدة الإيقاف المؤقت لهذا الحساب.',
        price: 75000,
        category: 'phones',
        condition: 'good',
        city: 'بورتسودان',
        images: ['https://example.com/ok.webp'],
        sellerId: 'expired_susp_1',
        sellerName: 'حساب انتهى إيقافه',
        sellerVerified: false,
        paymentMethods: ['cash'],
        negotiable: true,
        deliveryAvailable: false,
        viewsCount: 0,
        likesCount: 0,
        status: 'active',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );
  });

  it('protects rewardCredits, featured boosts, and immutable credit_transactions ledger', async () => {
    const seller = dbFor('seller_1');

    // Cannot self-grant credits or freely set featured: true
    await assertFails(
      updateDoc(doc(seller, 'users_private', 'seller_1'), {
        rewardCredits: 10,
        updatedAt: serverTimestamp(),
      })
    );
    await assertFails(
      updateDoc(doc(seller, 'products', 'prod_active_1'), {
        featured: true,
        updatedAt: serverTimestamp(),
      })
    );

    // Cannot mutate or delete immutable credit_transactions
    await assertFails(
      updateDoc(doc(adminClaimDb(), 'credit_transactions', 'ctx_seed_1'), {
        amount: 99,
      })
    );
    await assertFails(deleteDoc(doc(adminClaimDb(), 'credit_transactions', 'ctx_seed_1')));

    // Valid atomic 3-document boost spend (-1 credit + featured: true + credit_transactions) succeeds
    await assertSucceeds(
      runTransaction(seller, async (tx) => {
        const txId = 'ctx_boost_1';
        const privRef = doc(seller, 'users_private', 'seller_1');
        const prodRef = doc(seller, 'products', 'prod_active_1');
        const ledgerRef = doc(seller, 'credit_transactions', txId);

        tx.update(privRef, {
          rewardCredits: 0,
          lastCreditTxId: txId,
          updatedAt: serverTimestamp(),
        });
        tx.update(prodRef, {
          featured: true,
          featuredTxId: txId,
          updatedAt: serverTimestamp(),
        });
        tx.set(ledgerRef, {
          id: txId,
          uid: 'seller_1',
          type: 'ad_boost_spend',
          amount: -1,
          balanceBefore: 1,
          balanceAfter: 0,
          productId: 'prod_active_1',
          reason: 'Featured listing boost',
          createdAt: serverTimestamp(),
        });
      })
    );

    // Replay with the same txId or zero balance fails
    const batch = writeBatch(seller);
    batch.update(doc(seller, 'products', 'prod_active_1'), {
      featured: true,
      featuredTxId: 'ctx_boost_1',
      updatedAt: serverTimestamp(),
    });
    await assertFails(batch.commit());
  });

  it('enforces unique per-user viewsCount and atomic favorite/likesCount', async () => {
    const buyer = dbFor('buyer_1');
    const seller = dbFor('seller_1');

    // Unbound direct increment on viewsCount or likesCount fails
    await assertFails(
      updateDoc(doc(buyer, 'products', 'prod_active_1'), {
        viewsCount: 11,
        updatedAt: serverTimestamp(),
      })
    );
    await assertFails(
      updateDoc(doc(buyer, 'products', 'prod_active_1'), {
        likesCount: 3,
        updatedAt: serverTimestamp(),
      })
    );

    // Seller cannot inflate views on their own listing
    await assertFails(
      runTransaction(seller, async (tx) => {
        tx.set(doc(seller, 'users_private', 'seller_1', 'viewed_products', 'prod_active_1'), {
          productId: 'prod_active_1',
          createdAt: serverTimestamp(),
        });
        tx.update(doc(seller, 'products', 'prod_active_1'), {
          viewsCount: 11,
          updatedAt: serverTimestamp(),
        });
      })
    );

    // Buyer can record 1 atomic view per product
    await assertSucceeds(
      runTransaction(buyer, async (tx) => {
        tx.set(doc(buyer, 'users_private', 'buyer_1', 'viewed_products', 'prod_active_1'), {
          productId: 'prod_active_1',
          createdAt: serverTimestamp(),
        });
        tx.update(doc(buyer, 'products', 'prod_active_1'), {
          viewsCount: 11,
          updatedAt: serverTimestamp(),
        });
      })
    );

    // Duplicate view or deleting view receipt fails
    await assertFails(
      deleteDoc(doc(buyer, 'users_private', 'buyer_1', 'viewed_products', 'prod_active_1'))
    );
    await assertFails(
      updateDoc(doc(buyer, 'products', 'prod_active_1'), {
        viewsCount: 12,
        updatedAt: serverTimestamp(),
      })
    );

    // Buyer atomic favorite (+1 like) succeeds once, duplicate fails, then atomic unlike (-1 like) succeeds
    await assertSucceeds(
      runTransaction(buyer, async (tx) => {
        tx.set(doc(buyer, 'users_private', 'buyer_1', 'favorites', 'prod_active_1'), {
          productId: 'prod_active_1',
          createdAt: serverTimestamp(),
        });
        tx.update(doc(buyer, 'products', 'prod_active_1'), {
          likesCount: 3,
          updatedAt: serverTimestamp(),
        });
      })
    );
    await assertFails(
      setDoc(doc(buyer, 'users_private', 'buyer_1', 'favorites', 'prod_active_1'), {
        productId: 'prod_active_1',
        createdAt: serverTimestamp(),
      })
    );
    await assertSucceeds(
      runTransaction(buyer, async (tx) => {
        tx.delete(doc(buyer, 'users_private', 'buyer_1', 'favorites', 'prod_active_1'));
        tx.update(doc(buyer, 'products', 'prod_active_1'), {
          likesCount: 2,
          updatedAt: serverTimestamp(),
        });
      })
    );
  });

  it('requires an accepted & sold deal for 1-per-deal reviews and protects rating', async () => {
    const buyer = dbFor('buyer_1');
    const seller = dbFor('seller_1');

    // Direct client update to rating is blocked
    await assertFails(
      updateDoc(doc(seller, 'users_public', 'seller_1'), {
        rating: 5.0,
        updatedAt: serverTimestamp(),
      })
    );

    // Review on still-active product (not sold) fails
    await assertFails(
      setDoc(doc(buyer, 'reviews', 'prod_active_1_buyer_1'), {
        id: 'prod_active_1_buyer_1',
        sellerId: 'seller_1',
        reviewerId: 'buyer_1',
        productId: 'prod_active_1',
        conversationId: 'conv_1',
        rating: 5,
        comment: 'لم يكتمل البيع بعد',
        createdAt: serverTimestamp(),
      })
    );

    // Random review ID bypassing 1-per-deal deterministic key fails
    await assertFails(
      setDoc(doc(buyer, 'reviews', 'rev_random_fake'), {
        id: 'rev_random_fake',
        sellerId: 'seller_1',
        reviewerId: 'buyer_1',
        productId: 'prod_sold_1',
        conversationId: 'conv_sold_1',
        rating: 5,
        comment: 'محاولة تقييم بمعرف عشوائي',
        createdAt: serverTimestamp(),
      })
    );

    // Self-review fails
    await assertFails(
      setDoc(doc(seller, 'reviews', 'prod_sold_1_seller_1'), {
        id: 'prod_sold_1_seller_1',
        sellerId: 'seller_1',
        reviewerId: 'seller_1',
        productId: 'prod_sold_1',
        conversationId: 'conv_sold_1',
        rating: 5,
        comment: 'تقييم ذاتي مرفوض',
        createdAt: serverTimestamp(),
      })
    );

    // Valid completed-deal review succeeds once
    await assertSucceeds(
      setDoc(doc(buyer, 'reviews', 'prod_sold_1_buyer_1'), {
        id: 'prod_sold_1_buyer_1',
        sellerId: 'seller_1',
        reviewerId: 'buyer_1',
        reviewerName: 'مشتري جاد',
        productId: 'prod_sold_1',
        conversationId: 'conv_sold_1',
        rating: 5,
        comment: 'تعامل صادق واستلام سريع يداً بيد.',
        tags: ['سريع الاستجابة', 'سلعة مطابقة'],
        createdAt: serverTimestamp(),
      })
    );

    // Duplicate/overwrite review on the same deal fails
    await assertFails(
      updateDoc(doc(buyer, 'reviews', 'prod_sold_1_buyer_1'), {
        comment: 'تعديل مرفوض',
      })
    );
  });

  it('blocks direct get on inactive sponsored_ads while allowing active ads and admin access', async () => {
    await assertSucceeds(getDoc(doc(guestDb(), 'sponsored_ads', 'ad_active_1')));
    await assertFails(getDoc(doc(guestDb(), 'sponsored_ads', 'ad_inactive_1')));
    await assertFails(getDoc(doc(dbFor('buyer_1'), 'sponsored_ads', 'ad_inactive_1')));
    await assertSucceeds(getDoc(doc(adminClaimDb(), 'sponsored_ads', 'ad_inactive_1')));
  });

  it('blocks sellers from deleting flagged or under_review listings while allowing active delete and admin delete', async () => {
    const seller = dbFor('seller_1');
    await assertFails(deleteDoc(doc(seller, 'products', 'prod_flagged_1')));
    await assertFails(deleteDoc(doc(seller, 'products', 'prod_under_review_1')));
    await assertSucceeds(deleteDoc(doc(seller, 'products', 'prod_active_1')));
    await assertSucceeds(deleteDoc(doc(adminClaimDb(), 'products', 'prod_flagged_1')));
  });

  it('enforces isNotSuspended on personal notification updates and deletes', async () => {
    const suspended = dbFor('suspended_1');
    const buyer = dbFor('buyer_1');

    // Suspended user can still read own notification, but CANNOT update read or delete
    await assertSucceeds(
      getDoc(doc(suspended, 'users_private', 'suspended_1', 'notifications', 'notif_susp_1'))
    );
    await assertFails(
      updateDoc(doc(suspended, 'users_private', 'suspended_1', 'notifications', 'notif_susp_1'), {
        read: true,
      })
    );
    await assertFails(
      deleteDoc(doc(suspended, 'users_private', 'suspended_1', 'notifications', 'notif_susp_1'))
    );

    // Active owner can mark read and delete own notification
    await assertSucceeds(
      updateDoc(doc(buyer, 'users_private', 'buyer_1', 'notifications', 'notif_buyer_1'), {
        read: true,
      })
    );
    await assertSucceeds(
      deleteDoc(doc(buyer, 'users_private', 'buyer_1', 'notifications', 'notif_buyer_1'))
    );
  });

  it('validates all elements in images, fcmTokens, and review tags lists beyond index 0', async () => {
    const seller = dbFor('seller_1');
    const buyer = dbFor('buyer_1');

    // Invalid second image URL (not http/https) fails even when index 0 is valid
    await assertFails(
      setDoc(doc(seller, 'products', 'prod_bad_img_1'), {
        id: 'prod_bad_img_1',
        titleAr: 'منتج بصورة ثانية غير صالحة',
        descriptionAr: 'اختبار فحص كامل عناصر مصفوفة الصور.',
        price: 100000,
        category: 'phones',
        condition: 'good',
        city: 'الخرطوم',
        images: ['https://example.com/valid.webp', 'javascript:alert(1)'],
        sellerId: 'seller_1',
        sellerName: 'بائع موثوق',
        paymentMethods: ['cash'],
        negotiable: true,
        deliveryAvailable: false,
        viewsCount: 0,
        likesCount: 0,
        status: 'active',
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      })
    );

    // Invalid second FCM token (empty/oversized) fails even when index 0 is valid
    await assertFails(
      updateDoc(doc(seller, 'users_private', 'seller_1'), {
        fcmTokens: ['valid_token_0', ''],
        updatedAt: serverTimestamp(),
      })
    );

    // Invalid second review tag (oversized > 50 chars) fails even when index 0 is valid
    await assertFails(
      setDoc(doc(buyer, 'reviews', 'prod_sold_1_buyer_1'), {
        id: 'prod_sold_1_buyer_1',
        sellerId: 'seller_1',
        reviewerId: 'buyer_1',
        productId: 'prod_sold_1',
        conversationId: 'conv_sold_1',
        rating: 5,
        comment: 'اختبار فحص كامل وسوم التقييم.',
        tags: ['وسم صالح', 'x'.repeat(55)],
        createdAt: serverTimestamp(),
      })
    );
  });

  it('blocks spoofed isOfferUpdate chat messages without atomic conversation update', async () => {
    const buyer = dbFor('buyer_1');
    await assertFails(
      setDoc(doc(buyer, 'conversations', 'conv_1', 'messages', 'msg_spoof_offer'), {
        id: 'msg_spoof_offer',
        senderId: 'buyer_1',
        text: 'تم قبول العرض مزيف',
        status: 'sent',
        isOfferUpdate: true,
        offerAmount: 1000,
        createdAt: serverTimestamp(),
      })
    );
  });

  it('requires atomic product active->sold transition when incrementing dealsCompleted', async () => {
    const seller = dbFor('seller_1');

    // Direct dealsCompleted increment without marking lastSoldProductId sold fails
    await assertFails(
      updateDoc(doc(seller, 'users_public', 'seller_1'), {
        dealsCompleted: 10,
        lastSoldProductId: 'prod_active_1',
        updatedAt: serverTimestamp(),
      })
    );

    // Atomic sale transition (product active->sold + seller dealsCompleted +1) succeeds
    await assertSucceeds(
      runTransaction(seller, async (tx) => {
        tx.update(doc(seller, 'products', 'prod_active_1'), {
          status: 'sold',
          updatedAt: serverTimestamp(),
        });
        tx.update(doc(seller, 'users_public', 'seller_1'), {
          dealsCompleted: 10,
          lastSoldProductId: 'prod_active_1',
          updatedAt: serverTimestamp(),
        });
      })
    );
  });

  it('blocks review on sold product if conversation offerStatus is not accepted', async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore() as unknown as Firestore;
      const now = Timestamp.now();
      await setDoc(doc(db, 'conversations', 'conv_sold_pending'), {
        id: 'conv_sold_pending',
        productId: 'prod_sold_1',
        buyerId: 'buyer_1',
        sellerId: 'seller_1',
        participantIds: ['buyer_1', 'seller_1'],
        lastMessage: 'عرض غير مقبول',
        unreadByBuyer: 0,
        unreadBySeller: 0,
        offerStatus: 'pending',
        offerAmount: 900000,
        createdAt: now,
        updatedAt: now,
      });
    });

    await assertFails(
      setDoc(doc(dbFor('buyer_1'), 'reviews', 'prod_sold_1_buyer_1'), {
        id: 'prod_sold_1_buyer_1',
        sellerId: 'seller_1',
        reviewerId: 'buyer_1',
        productId: 'prod_sold_1',
        conversationId: 'conv_sold_pending',
        rating: 5,
        comment: 'تقييم مرفوض بدون قبول العرض',
        createdAt: serverTimestamp(),
      })
    );
  });

  it('enforces guest/non-admin get and list restrictions across products, users, reports, and verifications', async () => {
    const guest = guestDb();
    const buyer = dbFor('buyer_1');
    const admin = adminClaimDb();

    // Guest cannot read flagged or under_review products
    await assertFails(getDoc(doc(guest, 'products', 'prod_flagged_1')));
    await assertFails(getDoc(doc(guest, 'products', 'prod_under_review_1')));

    // Guest cannot list users_public; signed-in non-admin cannot list unfiltered users_public/users_private/reports/verification_requests
    await assertFails(getDocs(collection(guest, 'users_public')));
    await assertFails(getDocs(collection(buyer, 'users_public')));
    await assertFails(getDocs(collection(buyer, 'users_private')));
    await assertFails(getDocs(collection(buyer, 'reports')));
    await assertFails(getDocs(collection(buyer, 'verification_requests')));

    // Admin Custom Claim CAN list moderated/admin collections
    await assertSucceeds(getDocs(collection(admin, 'users_public')));
    await assertSucceeds(getDocs(collection(admin, 'users_private')));
    await assertSucceeds(getDocs(collection(admin, 'reports')));
    await assertSucceeds(getDocs(collection(admin, 'verification_requests')));
  });
});
