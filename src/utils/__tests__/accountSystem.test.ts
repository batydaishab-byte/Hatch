import { describe, expect, it } from 'vitest';
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

describe('HATCH Account & Security System', () => {
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

  const personalUser = buildOnboardedUser({
    uid: 'google_uid_personal',
    email: 'person@example.com',
    name: 'أحمد حسن',
    phone: '+249 912345678',
    city: 'الخرطوم (ولاية الخرطوم)',
    avatar: 'https://example.com/p.jpg',
    accountType: 'personal',
  });

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

  const baseDraft = enrichProductWithSellerIdentity(
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

  it('1. treats legacy user without accountType cleanly as personal while preserving deals and UID', () => {
    expect(resolveAccountType(legacyUser)).toBe('personal');
    expect(isCommercialAccount(legacyUser)).toBe(false);

    const normalizedLegacy = normalizeLegacyUser(legacyUser);
    expect(normalizedLegacy.accountType).toBe('personal');
    expect(normalizedLegacy.dealsCompleted).toBe(14);
    expect(normalizedLegacy.id).toBe('user_legacy_1');
  });

  it('2. supports onboarding for personal, store, and company bound to Google UID', () => {
    expect(personalUser.id).toBe('google_uid_personal');
    expect(personalUser.accountType).toBe('personal');
    expect(personalUser.businessActivity).toBeUndefined();
    expect(personalUser.businessLogo).toBeUndefined();
    expect(personalUser.profileCompleted).toBe(true);

    expect(storeUser.id).toBe('google_uid_store');
    expect(storeUser.accountType).toBe('store');
    expect(storeUser.businessActivity).toBe('بيع الهواتف والأجهزة الذكية');
    expect(storeUser.businessLogo).toBe('https://example.com/store-logo.png');
    expect(storeUser.avatar).toBe('https://example.com/store-logo.png');
    expect(isCommercialAccount(storeUser)).toBe(true);

    expect(companyUser.id).toBe('google_uid_company');
    expect(companyUser.accountType).toBe('company');
    expect(companyUser.businessActivity).toBe('حلول الطاقة المتجددة والتوريدات');
    expect(isCommercialAccount(companyUser)).toBe(true);
  });

  it('3. inherits commercial identity across multiple published products without re-entry', () => {
    expect(baseDraft.sellerId).toBe('google_uid_company');
    expect(baseDraft.sellerName).toBe('شركة الرواد للطاقة الشمسية');
    expect(baseDraft.sellerAvatar).toBe('https://example.com/company-logo.png');
    expect(baseDraft.sellerAccountType).toBe('company');
    expect(baseDraft.sellerBusinessActivity).toBe('حلول الطاقة المتجددة والتوريدات');

    const draft2 = enrichProductWithSellerIdentity(
      { ...baseDraft, titleAr: 'لوح شمسي 550 وات', price: 420000 },
      companyUser
    );
    expect(draft2.sellerAccountType).toBe('company');
    expect(draft2.sellerName).toBe('شركة الرواد للطاقة الشمسية');
  });

  it('4. preserves UID, deals, rating, verification, and products when upgrading personal account to store', () => {
    const normalizedLegacy = normalizeLegacyUser(legacyUser);
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
    expect(upgradedStore.id).toBe('user_legacy_1');
    expect(upgradedStore.dealsCompleted).toBe(14);
    expect(upgradedStore.verifiedIdentity).toBe(true);
    expect(upgradedStore.rating).toBe(4.9);
    expect(upgradedStore.accountType).toBe('store');
    expect(upgradedStore.businessLogo).toBe('https://example.com/yasir-store.png');

    const syncedProducts = syncProductsForUpdatedUser([existingProductForLegacy], upgradedStore);
    expect(syncedProducts[0].id).toBe('prod_legacy_1');
    expect(syncedProducts[0].sellerId).toBe('user_legacy_1');
    expect(syncedProducts[0].sellerName).toBe('متجر ياسر للهواتف');
    expect(syncedProducts[0].sellerAccountType).toBe('store');
    expect(syncedProducts[0].sellerBusinessActivity).toBe('بيع وصيانة الهواتف الذكية');
    expect(syncedProducts[0].sellerAvatar).toBe('https://example.com/yasir-store.png');
  });

  it('5. resolves mixed feed and commercial storefront active/sold catalog accurately', () => {
    const normalizedLegacy = normalizeLegacyUser(legacyUser);
    const upgradedStore = applyUserProfileUpdate(normalizedLegacy, {
      accountType: 'store',
      name: 'متجر ياسر للهواتف',
      businessActivity: 'بيع وصيانة الهواتف الذكية',
      businessLogo: 'https://example.com/yasir-store.png',
    });

    const draft2 = enrichProductWithSellerIdentity(
      { ...baseDraft, titleAr: 'لوح شمسي 550 وات', price: 420000 },
      companyUser
    );

    const companyProduct1: Product = {
      ...baseDraft,
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

    const storefront = getCommercialStorefrontData(
      'google_uid_company',
      [upgradedStore, companyUser],
      [companyProduct1, companyProduct2]
    );
    expect(storefront.seller?.id).toBe('google_uid_company');
    expect(storefront.accountType).toBe('company');
    expect(storefront.activeProducts).toHaveLength(1);
    expect(storefront.activeProducts[0].id).toBe('prod_comp_1');
    expect(storefront.soldProducts).toHaveLength(1);
    expect(storefront.soldProducts[0].id).toBe('prod_comp_2');
  });

  it('6. enforces suspension check for active vs expired suspensions', () => {
    expect(isUserSuspendedActive({ status: 'active' })).toBe(false);
    expect(
      isUserSuspendedActive({ status: 'suspended', suspendedUntilMs: Date.now() + 60_000 })
    ).toBe(true);
    expect(
      isUserSuspendedActive({ status: 'suspended', suspendedUntilMs: Date.now() - 60_000 })
    ).toBe(false);
  });

  it('7. enforces server-trusted admin claim/doc checks over forged local role', () => {
    const forgedAdmin: User = {
      ...personalUser,
      role: 'admin',
      hasAdminClaim: false,
      isAdminDoc: false,
    };
    expect(isAdminUser(forgedAdmin, true)).toBe(false);
    expect(isAdminUser({ ...forgedAdmin, hasAdminClaim: true }, true)).toBe(true);
    expect(isAdminUser({ ...forgedAdmin, isAdminDoc: true }, true)).toBe(true);
  });

  it('8. enforces strict offer state machine transitions by participant role', () => {
    expect(canTransitionOffer('none', 'pending', 'buyer_1', 'buyer_1', 'seller_1')).toBe(true);
    expect(canTransitionOffer('none', 'pending', 'seller_1', 'buyer_1', 'seller_1')).toBe(false);
    expect(canTransitionOffer('pending', 'accepted', 'buyer_1', 'buyer_1', 'seller_1')).toBe(false);
    expect(canTransitionOffer('pending', 'accepted', 'seller_1', 'buyer_1', 'seller_1')).toBe(true);
    expect(canTransitionOffer('pending', 'countered', 'seller_1', 'buyer_1', 'seller_1')).toBe(true);
    expect(canTransitionOffer('countered', 'accepted', 'buyer_1', 'buyer_1', 'seller_1')).toBe(true);
    expect(canTransitionOffer('accepted', 'declined', 'seller_1', 'buyer_1', 'seller_1')).toBe(false);
  });

  it('9. prevents client profile updates from spoofing verifiedIdentity, role, dealsCompleted, or featured', () => {
    const spoofAttempt = applyUserProfileUpdate(
      { ...personalUser, verifiedIdentity: false, role: 'user', dealsCompleted: 3 },
      { verifiedIdentity: true, role: 'admin', dealsCompleted: 99, name: 'تعديل' }
    );
    expect(spoofAttempt.verifiedIdentity).toBe(false);
    expect(spoofAttempt.role).toBe('user');
    expect(spoofAttempt.dealsCompleted).toBe(3);
    expect(baseDraft.featured).toBe(false);
  });
});

