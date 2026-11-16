import { AccountType, Product, resolveAccountType, SudaneseCity, User } from '../types';

export interface CreateAccountInput {
  name: string;
  phone: string;
  city: SudaneseCity;
  avatar?: string;
  accountType?: AccountType;
  businessActivity?: string;
  businessLogo?: string;
  uid?: string;
  email?: string;
}

/**
 * Ensures any legacy user object without `accountType` is treated cleanly as 'personal'
 * without forcing re-registration or altering deals/products/chats.
 */
export function normalizeLegacyUser(user: User): User {
  const accountType = resolveAccountType(user.accountType);
  return {
    ...user,
    accountType,
    avatar: user.businessLogo || user.avatar,
  };
}

/**
 * Builds or updates a User record during onboarding bound to the active UID
 * (without creating a second Firebase account or dropping existing stats if UID already exists).
 */
export function buildOnboardedUser(
  input: CreateAccountInput,
  existingUser?: User | null
): User {
  const accountType = resolveAccountType(input.accountType);
  const chosenLogoOrAvatar =
    (accountType !== 'personal' ? input.businessLogo?.trim() : undefined) ||
    input.avatar?.trim() ||
    existingUser?.avatar ||
    'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80';

  const trimmedName = input.name.trim();
  const trimmedActivity =
    accountType === 'personal' ? undefined : input.businessActivity?.trim() || undefined;
  const logoValue = accountType === 'personal' ? undefined : chosenLogoOrAvatar;

  return {
    id: input.uid || existingUser?.id || `user_${Date.now()}`,
    name: trimmedName,
    avatar: chosenLogoOrAvatar,
    businessLogo: logoValue,
    businessActivity: trimmedActivity,
    accountType,
    phone: input.phone.trim(),
    email: input.email ?? existingUser?.email,
    city: input.city,
    rating: existingUser?.rating ?? 5.0,
    dealsCompleted: existingUser?.dealsCompleted ?? 0,
    reportsCount: existingUser?.reportsCount ?? 0,
    verifiedIdentity: existingUser?.verifiedIdentity ?? false,
    joinedDate: existingUser?.joinedDate ?? new Date().toISOString().split('T')[0],
    role: existingUser?.role ?? 'user',
    status: existingUser?.status ?? 'active',
    profileCompleted: true,
  };
}

/**
 * Applies profile updates or account-type upgrade (e.g. 'personal' -> 'store' | 'company')
 * strictly under the same user UID, preserving deals, ratings, verification, and roles.
 */
export function applyUserProfileUpdate(
  current: User,
  updates: Partial<User>
): User {
  const nextType =
    updates.accountType !== undefined
      ? resolveAccountType(updates.accountType)
      : resolveAccountType(current.accountType);

  const nextLogo =
    nextType === 'personal'
      ? undefined
      : updates.businessLogo !== undefined
      ? updates.businessLogo.trim() || undefined
      : updates.avatar !== undefined
      ? updates.avatar.trim() || undefined
      : current.businessLogo || current.avatar;

  const nextAvatar =
    updates.avatar?.trim() ||
    (nextType !== 'personal' ? nextLogo : undefined) ||
    current.avatar;

  const nextActivity =
    nextType === 'personal'
      ? undefined
      : updates.businessActivity !== undefined
      ? updates.businessActivity.trim() || undefined
      : current.businessActivity;

  return {
    ...current,
    ...updates,
    id: current.id, // Never mutate UID on profile update or upgrade
    dealsCompleted: current.dealsCompleted,
    rating: current.rating,
    verifiedIdentity: current.verifiedIdentity,
    joinedDate: current.joinedDate,
    role: current.role,
    accountType: nextType,
    avatar: nextAvatar,
    businessLogo: nextLogo,
    businessActivity: nextActivity,
    profileCompleted: true,
  };
}

/**
 * Propagates updated user identity/commercial branding to existing products
 * owned by that UID without losing any product IDs, prices, or status.
 */
export function syncProductsForUpdatedUser(
  products: Product[],
  updatedUser: User
): Product[] {
  const accountType = resolveAccountType(updatedUser.accountType);
  return products.map((product) => {
    if (product.sellerId !== updatedUser.id) return product;
    return {
      ...product,
      sellerName: updatedUser.name,
      sellerAvatar: updatedUser.businessLogo || updatedUser.avatar,
      sellerPhone: updatedUser.phone,
      sellerAccountType: accountType,
      sellerBusinessActivity: updatedUser.businessActivity,
    };
  });
}

/**
 * Attaches current seller commercial metadata to a newly published listing
 * so stores and companies can publish multiple products without re-entering details.
 */
export function enrichProductWithSellerIdentity(
  productData: Omit<Product, 'id' | 'createdAt' | 'viewsCount' | 'likesCount' | 'status'>,
  seller: User
): Omit<Product, 'id' | 'createdAt' | 'viewsCount' | 'likesCount' | 'status'> {
  const accountType = resolveAccountType(seller.accountType);
  return {
    ...productData,
    sellerId: seller.id,
    sellerName: seller.name,
    sellerAvatar: seller.businessLogo || seller.avatar,
    sellerPhone: seller.phone,
    sellerRating: seller.rating,
    sellerDealsCount: seller.dealsCompleted,
    sellerVerified: seller.verifiedIdentity,
    sellerAccountType: accountType,
    sellerBusinessActivity:
      accountType === 'personal' ? undefined : seller.businessActivity,
  };
}

/**
 * Resolves a commercial storefront's profile & product catalog for its independent page.
 */
export function getCommercialStorefrontData(
  sellerId: string,
  users: User[],
  products: Product[]
): {
  seller: User | null;
  accountType: AccountType;
  activeProducts: Product[];
  soldProducts: Product[];
} {
  const matchedUser = users.find((u) => u.id === sellerId);
  const sellerProducts = products.filter((p) => p.sellerId === sellerId);
  const activeProducts = sellerProducts.filter((p) => p.status === 'active');
  const soldProducts = sellerProducts.filter((p) => p.status === 'sold');

  if (matchedUser) {
    const normalized = normalizeLegacyUser(matchedUser);
    return {
      seller: normalized,
      accountType: resolveAccountType(normalized.accountType),
      activeProducts,
      soldProducts,
    };
  }

  const sampleProduct = sellerProducts[0];
  if (!sampleProduct) {
    return {
      seller: null,
      accountType: 'personal',
      activeProducts: [],
      soldProducts: [],
    };
  }

  const fallbackType = resolveAccountType(sampleProduct.sellerAccountType);
  const synthesizedSeller: User = {
    id: sampleProduct.sellerId,
    name: sampleProduct.sellerName,
    avatar: sampleProduct.sellerAvatar,
    businessLogo: fallbackType !== 'personal' ? sampleProduct.sellerAvatar : undefined,
    businessActivity: sampleProduct.sellerBusinessActivity,
    accountType: fallbackType,
    phone: sampleProduct.sellerPhone,
    city: sampleProduct.city,
    rating: sampleProduct.sellerRating || 4.9,
    dealsCompleted: sampleProduct.sellerDealsCount || 0,
    verifiedIdentity: Boolean(sampleProduct.sellerVerified),
    joinedDate: sampleProduct.createdAt || '',
    role: 'user',
    status: 'active',
  };

  return {
    seller: synthesizedSeller,
    accountType: fallbackType,
    activeProducts,
    soldProducts,
  };
}

export function isCommercialAccount(
  userOrType?: Pick<User, 'accountType'> | AccountType | null
): boolean {
  const resolved = resolveAccountType(userOrType);
  return resolved === 'store' || resolved === 'company';
}
