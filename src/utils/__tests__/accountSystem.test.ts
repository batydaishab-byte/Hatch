import assert from 'node:assert/strict';
import {
  canTransitionOffer,
  isAdminUser,
  isUserSuspendedActive,
  Product,
  resolveAccountType,
  User,
} from '../../types';
import {
  applyUserProfileUpdate,
  buildOnboardedUser,
  enrichProductWithSellerIdentity,
  getCommercialStorefrontData,
  isCommercialAccount,
  normalizeLegacyUser,
  syncProductsForUpdatedUser,
} from '../accountHelpers';

function runTests() {
  // 1. Legacy user without accountType defaults cleanly to personal
  const legacyUser: User = {
    id: 'user_legacy_1',
    name: 'ياسر بابكر',
    avatar: 'https://example.com/avatar.jpg',
    phone: '+249 91 234 5678',
    city: 'بورتسودان (ولاية البحر الأحمر)',
    rating: 4.9,
    dealsCompleted: 14,
    verifiedIdentity: true,
    joinedDate: '2023-08-15',
    role: 'user',
  };
  assert.equal(resolveAccountType(legacyUser), 'personal');
  assert.equal(isCommercialAccount(legacyUser), false);
  const normalizedLegacy = normalizeLegacyUser(legacyUser);
  assert.equal(normalizedLegacy.accountType, 'personal');
  assert.equal(normalizedLegacy.dealsCompleted, 14);
  assert.equal(normalizedLegacy.id, 'user_legacy_1');

  // 2. Onboarding for personal, store, and company bound to Google UID
  const personalUser = buildOnboardedUser({
    uid: 'google_uid_personal',
    email: 'person@example.com',
    name: 'أحمد حسن',
    phone: '+249 912345678',
    city: 'الخرطوم (ولاية الخرطوم)',
    avatar: 'https://example.com/p.jpg',
    accountType: 'personal',
  });
  assert.equal(personalUser.id, 'google_uid_personal');
  assert.equal(personalUser.accountType, 'personal');
  assert.equal(personalUser.businessActivity, undefined);
  assert.equal(personalUser.businessLogo, undefined);
  assert.equal(personalUser.profileCompleted, true);

  const storeUser = buildOnboardedUser({
    uid: 'google_uid_store',
    email: 'store@example.com',
    name: 'متجر النيل للإلكترونيات',
    phone: '+249 922345678',
    city: 'أم درمان (ولاية الخرطوم)',
    accountType: 'store',
    businessActivity: 'بيع الهواتف والأجهزة الذكية',
    businessLogo: 'https://example.com/store-logo.png',
  });
  assert.equal(storeUser.id, 'google_uid_store');
  assert.equal(storeUser.accountType, 'store');
  assert.equal(storeUser.businessActivity, 'بيع الهواتف والأجهزة الذكية');
  assert.equal(storeUser.businessLogo, 'https://example.com/store-logo.png');
  assert.equal(storeUser.avatar, 'https://example.com/store-logo.png');
  assert.equal(isCommercialAccount(storeUser), true);

  const companyUser = buildOnboardedUser({
    uid: 'google_uid_company',
    email: 'company@example.com',
    name: 'شركة الرواد للطاقة الشمسية',
    phone: '+249 932345678',
    city: 'عطبرة (ولاية نهر النيل)',
    accountType: 'company',
    businessActivity: 'حلول الطاقة المتجددة والتوريدات',
    businessLogo: 'https://example.com/company-logo.png',
  });
  assert.equal(companyUser.id, 'google_uid_company');
  assert.equal(companyUser.accountType, 'company');
  assert.equal(companyUser.businessActivity, 'حلول الطاقة المتجددة والتوريدات');
  assert.equal(isCommercialAccount(companyUser), true);

  // 3. Multiple product publishing inherits commercial info without re-entry
  const draft1 = enrichProductWithSellerIdentity(
    {
      titleAr: 'إنفرتر طاقة شمسية 5 كيلووات',
      titleEn: '5KW Solar Inverter',
      descriptionAr: 'إنفرتر أصلي بضمان سنة كاملة',
      descriptionEn: 'Original inverter with 1 year warranty',
      price: 2500000,
      category: 'solar_energy',
      condition: 'new',
      city: companyUser.city,
      areaAr: 'السوق الكبير',
      areaEn: 'Grand Market',
      images: ['https://example.com/inv1.jpg'],
      sellerId: 'placeholder',
      sellerName: 'placeholder',
      sellerAvatar: 'placeholder',
      sellerPhone: 'placeholder',
      sellerRating: 0,
      sellerDealsCount: 0,
      sellerVerified: false,
      paymentMethods: ['bankak', 'cash'],
      negotiable: true,
      deliveryAvailable: true,
    },
    companyUser
  );
  assert.equal(draft1.sellerId, 'google_uid_company');
  assert.equal(draft1.sellerName, 'شركة الرواد للطاقة الشمسية');
  assert.equal(draft1.sellerAvatar, 'https://example.com/company-logo.png');
  assert.equal(draft1.sellerAccountType, 'company');
  assert.equal(draft1.sellerBusinessActivity, 'حلول الطاقة المتجددة والتوريدات');

  const draft2 = enrichProductWithSellerIdentity(
    { ...draft1, titleAr: 'لوح شمسي 550 وات', price: 420000 },
    companyUser
  );
  assert.equal(draft2.sellerAccountType, 'company');
  assert.equal(draft2.sellerName, 'شركة الرواد للطاقة الشمسية');

  // 4. Upgrading a personal account to a store/company preserves UID, deals, rating, and products
  const existingProductForLegacy: Product = {
    id: 'prod_legacy_1',
    titleAr: 'هاتف سامسونج',
    titleEn: 'Samsung Phone',
    descriptionAr: 'نظيف جداً بحالة ممتازة',
    descriptionEn: 'Very clean excellent condition',
    price: 550000,
    category: 'phones',
    condition: 'like_new',
    city: normalizedLegacy.city,
    areaAr: 'حي الشاطئ',
    areaEn: 'Al-Shatei',
    images: ['https://example.com/sam.jpg'],
    sellerId: normalizedLegacy.id,
    sellerName: normalizedLegacy.name,
    sellerAvatar: normalizedLegacy.avatar,
    sellerPhone: normalizedLegacy.phone,
    sellerRating: normalizedLegacy.rating,
    sellerDealsCount: normalizedLegacy.dealsCompleted,
    sellerVerified: normalizedLegacy.verifiedIdentity,
    paymentMethods: ['bankak'],
    negotiable: true,
    deliveryAvailable: false,
    viewsCount: 12,
    likesCount: 3,
    status: 'active',
    createdAt: '2025-01-01',
  };

  const upgradedStore = applyUserProfileUpdate(normalizedLegacy, {
    accountType: 'store',
    name: 'متجر ياسر للهواتف',
    businessActivity: 'بيع وصيانة الهواتف الذكية',
    businessLogo: 'https://example.com/yasir-store.png',
  });
  assert.equal(upgradedStore.id, 'user_legacy_1');
  assert.equal(upgradedStore.dealsCompleted, 14);
  assert.equal(upgradedStore.verifiedIdentity, true);
  assert.equal(upgradedStore.rating, 4.9);
  assert.equal(upgradedStore.accountType, 'store');
  assert.equal(upgradedStore.businessLogo, 'https://example.com/yasir-store.png');

  const syncedProducts = syncProductsForUpdatedUser([existingProductForLegacy], upgradedStore);
  assert.equal(syncedProducts[0].id, 'prod_legacy_1');
  assert.equal(syncedProducts[0].sellerId, 'user_legacy_1');
  assert.equal(syncedProducts[0].sellerName, 'متجر ياسر للهواتف');
  assert.equal(syncedProducts[0].sellerAccountType, 'store');
  assert.equal(syncedProducts[0].sellerBusinessActivity, 'بيع وصيانة الهواتف الذكية');
  assert.equal(syncedProducts[0].sellerAvatar, 'https://example.com/yasir-store.png');

  // 5. Mixed feed & commercial storefront lookup
  const companyProduct1: Product = {
    ...draft1,
    id: 'prod_comp_1',
    viewsCount: 10,
    likesCount: 2,
    status: 'active',
    createdAt: '2025-02-01',
  };
  const companyProduct2: Product = {
    ...draft2,
    id: 'prod_comp_2',
    viewsCount: 4,
    likesCount: 1,
    status: 'sold',
    createdAt: '2025-02-02',
  };
  const allCatalog = [...syncedProducts, companyProduct1, companyProduct2];

  const storefront = getCommercialStorefrontData(
    'google_uid_company',
    [upgradedStore, companyUser],
    allCatalog
  );
  assert.equal(storefront.seller?.id, 'google_uid_company');
  assert.equal(storefront.accountType, 'company');
  assert.equal(storefront.activeProducts.length, 1);
  assert.equal(storefront.activeProducts[0].id, 'prod_comp_1');
  assert.equal(storefront.soldProducts.length, 1);
  assert.equal(storefront.soldProducts[0].id, 'prod_comp_2');

  // 6. Security Invariant: Suspension check (active vs expired)
  assert.equal(isUserSuspendedActive({ status: 'active' }), false);
  assert.equal(
    isUserSuspendedActive({ status: 'suspended', suspendedUntilMs: Date.now() + 60_000 }),
    true
  );
  assert.equal(
    isUserSuspendedActive({ status: 'suspended', suspendedUntilMs: Date.now() - 60_000 }),
    false
  );

  // 7. Security Invariant: Server-trusted admin claim check vs forged local role
  const forgedAdmin: User = { ...personalUser, role: 'admin', hasAdminClaim: false, isAdminDoc: false };
  assert.equal(isAdminUser(forgedAdmin, true), false);
  assert.equal(isAdminUser({ ...forgedAdmin, hasAdminClaim: true }, true), true);
  assert.equal(isAdminUser({ ...forgedAdmin, isAdminDoc: true }, true), true);

  // 8. Security Invariant: Offer state machine transitions
  assert.equal(canTransitionOffer('none', 'pending', 'buyer_1', 'buyer_1', 'seller_1'), true);
  assert.equal(canTransitionOffer('none', 'pending', 'seller_1', 'buyer_1', 'seller_1'), false);
  assert.equal(canTransitionOffer('pending', 'accepted', 'buyer_1', 'buyer_1', 'seller_1'), false);
  assert.equal(canTransitionOffer('pending', 'accepted', 'seller_1', 'buyer_1', 'seller_1'), true);
  assert.equal(canTransitionOffer('pending', 'countered', 'seller_1', 'buyer_1', 'seller_1'), true);
  assert.equal(canTransitionOffer('countered', 'accepted', 'buyer_1', 'buyer_1', 'seller_1'), true);
  assert.equal(canTransitionOffer('accepted', 'declined', 'seller_1', 'buyer_1', 'seller_1'), false);

  // 9. Security Invariant: Client profile update cannot spoof verifiedIdentity or role or featured
  const spoofAttempt = applyUserProfileUpdate(
    { ...personalUser, verifiedIdentity: false, role: 'user', dealsCompleted: 3 },
    { verifiedIdentity: true, role: 'admin', dealsCompleted: 99, name: 'تعديل' }
  );
  assert.equal(spoofAttempt.verifiedIdentity, false);
  assert.equal(spoofAttempt.role, 'user');
  assert.equal(spoofAttempt.dealsCompleted, 3);
  assert.equal(draft1.featured, false);

  console.log('ALL ACCOUNT & SECURITY SYSTEM TESTS PASSED (9/9 scenarios verified).');
}

runTests();
