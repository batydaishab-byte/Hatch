import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User as FirebaseUser,
} from 'firebase/auth';
import {
  getFirestore,
  doc,
  getDoc,
  getDocFromServer,
  setDoc,
  updateDoc,
  deleteDoc,
  deleteField,
  collection,
  query,
  where,
  getDocs,
  onSnapshot,
  serverTimestamp,
  writeBatch,
  runTransaction,
  Timestamp,
} from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';
import {
  AccountType,
  CreditTransaction,
  isUserSuspendedActive,
  Product,
  ReportTicket,
  resolveAccountType,
  User,
  VerificationRequest,
} from '../types';

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId: string | undefined;
    email: string | null | undefined;
    emailVerified: boolean | undefined;
    isAnonymous: boolean | undefined;
    tenantId: string | null | undefined;
    providerInfo: {
      providerId: string;
      displayName: string | null;
      email: string | null;
      photoUrl: string | null;
    }[];
  };
}

const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
export const db = getFirestore(app, firebaseConfig.firestoreDatabaseId);
export const auth = getAuth(app);

export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export function handleFirestoreError(
  error: unknown,
  operationType: OperationType,
  path: string | null
): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData.map((provider) => ({
          providerId: provider.providerId,
          displayName: provider.displayName,
          email: provider.email,
          photoUrl: provider.photoURL,
        })) || [],
    },
    operationType,
    path,
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

const DEFAULT_FALLBACK_AVATAR =
  'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80';
const DEFAULT_FALLBACK_PRODUCT_IMG =
  'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=800&q=80';

/**
 * Ensures image/logo URLs stored in Firestore respect the <= 1000 char security rule limit
 * while keeping HTTPS URLs intact. Data URLs or oversized strings are swapped for a valid HTTPS fallback
 * when persisting to Firestore (local UI state still keeps the chosen preview).
 */
export function sanitizeUrlForFirestore(
  url: string | undefined,
  fallback: string = DEFAULT_FALLBACK_AVATAR
): string {
  if (!url) return fallback;
  const trimmed = url.trim();
  if (!trimmed) return fallback;
  if (trimmed.startsWith('data:') || trimmed.length > 1000) {
    return fallback;
  }
  return trimmed;
}

export function normalizeSudanPhoneForFirestore(phone: string | undefined): string {
  const cleaned = (phone || '').trim();
  if (cleaned.length >= 6 && cleaned.length <= 32) {
    return cleaned;
  }
  return '+249 910000000';
}

export function normalizeNameForFirestore(name: string | undefined): string {
  const cleaned = (name || '').trim();
  if (cleaned.length >= 2 && cleaned.length <= 80) {
    return cleaned;
  }
  return cleaned.length === 1 ? `${cleaned}_` : 'مستخدم هتش';
}

let connectionTested = false;
export async function testFirestoreConnection(): Promise<void> {
  if (connectionTested) return;
  connectionTested = true;
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.error('Please check your Firebase configuration. The client is offline.');
    }
  }
}

export async function signInWithGooglePopup(): Promise<FirebaseUser> {
  const result = await signInWithPopup(auth, googleProvider);
  return result.user;
}

export async function signOutFirebase(): Promise<void> {
  await signOut(auth);
}

export function subscribeToAuthState(
  callback: (user: FirebaseUser | null) => void
): () => void {
  return onAuthStateChanged(auth, callback);
}

function formatJoinedDate(val: unknown): string {
  if (val instanceof Timestamp) {
    return val.toDate().toISOString().split('T')[0];
  }
  if (typeof val === 'string' && val.length > 0) {
    return val.split('T')[0];
  }
  return new Date().toISOString().split('T')[0];
}

export interface LoadedFirestoreProfile {
  user: User;
  isComplete: boolean;
  rewardCredits: number;
}

function parseSuspendedFields(pub: Record<string, any>): {
  status: 'active' | 'suspended';
  suspendedUntil?: string;
  suspendedUntilMs?: number;
} {
  let suspendedUntilMs: number | undefined;
  let suspendedUntil: string | undefined =
    typeof pub.suspendedUntil === 'string' ? pub.suspendedUntil : undefined;

  if (pub.suspendedUntilTs instanceof Timestamp) {
    suspendedUntilMs = pub.suspendedUntilTs.toMillis();
    suspendedUntil = pub.suspendedUntilTs.toDate().toISOString();
  } else if (suspendedUntil) {
    const parsed = Date.parse(suspendedUntil);
    if (!Number.isNaN(parsed)) {
      suspendedUntilMs = parsed;
    }
  }

  const rawStatus = pub.status === 'suspended' ? 'suspended' : 'active';
  const activeSuspended = isUserSuspendedActive({
    status: rawStatus,
    suspendedUntil,
    suspendedUntilMs,
  });

  return {
    status: activeSuspended ? 'suspended' : 'active',
    suspendedUntil: activeSuspended ? suspendedUntil : undefined,
    suspendedUntilMs: activeSuspended ? suspendedUntilMs : undefined,
  };
}

export async function fetchUserProfileFromFirestore(
  uid: string,
  fallbackEmail?: string | null
): Promise<LoadedFirestoreProfile | null> {
  const publicPath = `users_public/${uid}`;
  const privatePath = `users_private/${uid}`;
  try {
    const publicSnap = await getDoc(doc(db, 'users_public', uid));
    if (!publicSnap.exists()) {
      return null;
    }
    const pub = publicSnap.data();

    let priv: Record<string, any> = {};
    try {
      const privateSnap = await getDoc(doc(db, 'users_private', uid));
      if (privateSnap.exists()) {
        priv = privateSnap.data();
      }
    } catch {
      // If private doc doesn't exist yet or isn't readable, continue gracefully with public doc
    }

    let hasAdminClaim = false;
    try {
      if (auth.currentUser && auth.currentUser.uid === uid) {
        const tokenResult = await auth.currentUser.getIdTokenResult();
        hasAdminClaim = Boolean(
          tokenResult?.claims?.admin === true && auth.currentUser.emailVerified === true
        );
      }
    } catch {
      hasAdminClaim = false;
    }

    let isAdminDoc = false;
    try {
      const adminSnap = await getDoc(doc(db, 'admins', uid));
      isAdminDoc = adminSnap.exists();
    } catch {
      isAdminDoc = false;
    }

    const accountType = resolveAccountType(pub.accountType as AccountType | undefined);
    const name = typeof pub.name === 'string' ? pub.name.trim() : '';
    const city = typeof pub.city === 'string' ? pub.city.trim() : '';
    const phone = typeof priv.phone === 'string' ? priv.phone.trim() : '';
    const logoOrAvatar =
      (typeof pub.businessLogo === 'string' && pub.businessLogo) ||
      (typeof pub.avatar === 'string' && pub.avatar) ||
      DEFAULT_FALLBACK_AVATAR;

    const hasBusinessRequirement =
      accountType === 'personal' ||
      (typeof pub.businessActivity === 'string' && pub.businessActivity.trim().length > 0);

    const isComplete =
      name.length >= 2 && city.length >= 2 && phone.length >= 6 && hasBusinessRequirement;

    const susp = parseSuspendedFields(pub);
    const rewardCredits =
      typeof priv.rewardCredits === 'number' && priv.rewardCredits >= 0
        ? Math.floor(priv.rewardCredits)
        : 0;

    const user: User = {
      id: uid,
      name: name || 'مستخدم هتش',
      avatar: logoOrAvatar,
      businessLogo:
        typeof pub.businessLogo === 'string' && pub.businessLogo
          ? pub.businessLogo
          : undefined,
      businessActivity:
        typeof pub.businessActivity === 'string' && pub.businessActivity
          ? pub.businessActivity
          : undefined,
      accountType,
      phone: phone || '+249 910000000',
      email: typeof priv.email === 'string' ? priv.email : fallbackEmail || undefined,
      city: city || 'بورتسودان (ولاية البحر الأحمر)',
      rating: typeof pub.rating === 'number' ? pub.rating : 5.0,
      dealsCompleted: typeof pub.dealsCompleted === 'number' ? pub.dealsCompleted : 0,
      reportsCount: typeof pub.reportsCount === 'number' ? pub.reportsCount : 0,
      verifiedIdentity: Boolean(pub.verifiedIdentity),
      joinedDate: formatJoinedDate(pub.joinedDate),
      role: hasAdminClaim || isAdminDoc || priv.role === 'admin' ? 'admin' : 'user',
      hasAdminClaim,
      isAdminDoc,
      status: susp.status,
      suspendedUntil: susp.suspendedUntil,
      suspendedUntilMs: susp.suspendedUntilMs,
      suspensionDays: susp.status === 'suspended' ? pub.suspensionDays : undefined,
      suspensionReason: susp.status === 'suspended' ? pub.suspensionReason : undefined,
      lastSoldProductId: typeof pub.lastSoldProductId === 'string' ? pub.lastSoldProductId : undefined,
      profileCompleted: isComplete,
    };

    return { user, isComplete, rewardCredits };
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, `${publicPath} / ${privatePath}`);
  }
}

async function syncCommercialProfileInFirestore(
  uid: string,
  accountType: AccountType,
  fields: {
    name: string;
    avatar: string;
    city: string;
    businessActivity?: string;
    businessLogo?: string;
  }
): Promise<void> {
  const commercialRef = doc(db, 'commercial_profiles', uid);
  const publicRef = doc(db, 'users_public', uid);

  try {
    const commercialSnap = await getDoc(commercialRef);
    if (accountType === 'store' || accountType === 'company') {
      const pubSnap = await getDoc(publicRef);
      if (!pubSnap.exists()) return;
      const pubData = pubSnap.data();
      if (commercialSnap.exists()) {
        await updateDoc(commercialRef, {
          name: fields.name,
          avatar: fields.avatar,
          city: fields.city,
          accountType,
          businessActivity: (fields.businessActivity || '').trim().slice(0, 120),
          businessLogo: fields.businessLogo || fields.avatar,
          updatedAt: serverTimestamp(),
        });
      } else {
        await setDoc(commercialRef, {
          uid,
          name: fields.name,
          avatar: fields.avatar,
          city: fields.city,
          rating: typeof pubData.rating === 'number' ? pubData.rating : 5.0,
          dealsCompleted: typeof pubData.dealsCompleted === 'number' ? pubData.dealsCompleted : 0,
          reportsCount: typeof pubData.reportsCount === 'number' ? pubData.reportsCount : 0,
          verifiedIdentity: Boolean(pubData.verifiedIdentity),
          status: pubData.status === 'suspended' ? 'suspended' : 'active',
          accountType,
          businessActivity: (fields.businessActivity || '').trim().slice(0, 120),
          businessLogo: fields.businessLogo || fields.avatar,
          joinedDate: pubData.joinedDate || serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    } else if (commercialSnap.exists()) {
      await deleteDoc(commercialRef);
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `commercial_profiles/${uid}`);
  }
}

export async function upsertOnboardingUserToFirestore(
  uid: string,
  data: {
    name: string;
    phone: string;
    city: string;
    avatar: string;
    accountType: AccountType;
    businessActivity?: string;
    businessLogo?: string;
    email?: string | null;
  }
): Promise<void> {
  const publicRef = doc(db, 'users_public', uid);
  const privateRef = doc(db, 'users_private', uid);

  const safeName = normalizeNameForFirestore(data.name);
  const safeCity = data.city.trim().slice(0, 100) || 'بورتسودان (ولاية البحر الأحمر)';
  const safeAvatar = sanitizeUrlForFirestore(data.avatar, DEFAULT_FALLBACK_AVATAR);
  const safePhone = normalizeSudanPhoneForFirestore(data.phone);
  const accountType = resolveAccountType(data.accountType);
  const safeActivity =
    accountType === 'personal' ? '' : (data.businessActivity || '').trim().slice(0, 120);
  const safeLogo =
    accountType === 'personal'
      ? ''
      : sanitizeUrlForFirestore(data.businessLogo || data.avatar, safeAvatar);

  try {
    const publicSnap = await getDoc(publicRef);
    if (!publicSnap.exists()) {
      const publicPayload: Record<string, any> = {
        uid,
        name: safeName,
        avatar: accountType === 'personal' ? safeAvatar : safeLogo,
        city: safeCity,
        rating: 5.0,
        dealsCompleted: 0,
        reportsCount: 0,
        verifiedIdentity: false,
        status: 'active',
        accountType,
        joinedDate: serverTimestamp(),
        updatedAt: serverTimestamp(),
      };
      if (accountType !== 'personal') {
        publicPayload.businessActivity = safeActivity;
        publicPayload.businessLogo = safeLogo;
      }
      await setDoc(publicRef, publicPayload);
    } else {
      const updatePayload: Record<string, any> = {
        name: safeName,
        avatar: accountType === 'personal' ? safeAvatar : safeLogo,
        city: safeCity,
        accountType,
        updatedAt: serverTimestamp(),
      };
      if (accountType !== 'personal') {
        updatePayload.businessActivity = safeActivity;
        updatePayload.businessLogo = safeLogo;
      }
      await updateDoc(publicRef, updatePayload);
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `users_public/${uid}`);
  }

  await syncCommercialProfileInFirestore(uid, accountType, {
    name: safeName,
    avatar: accountType === 'personal' ? safeAvatar : safeLogo,
    city: safeCity,
    businessActivity: safeActivity,
    businessLogo: safeLogo,
  });

  try {
    const privateSnap = await getDoc(privateRef);
    if (!privateSnap.exists()) {
      const privatePayload: Record<string, any> = {
        uid,
        phone: safePhone,
        role: 'user',
        rewardCredits: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      };
      if (data.email) {
        privatePayload.email = data.email.slice(0, 160);
      }
      await setDoc(privateRef, privatePayload);
    } else {
      await updateDoc(privateRef, {
        phone: safePhone,
        updatedAt: serverTimestamp(),
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `users_private/${uid}`);
  }
}

export async function updateUserProfileInFirestore(
  uid: string,
  updates: {
    name?: string;
    phone?: string;
    city?: string;
    avatar?: string;
    accountType?: AccountType;
    businessActivity?: string;
    businessLogo?: string;
  }
): Promise<void> {
  const publicRef = doc(db, 'users_public', uid);
  const privateRef = doc(db, 'users_private', uid);

  const publicUpdate: Record<string, any> = {
    updatedAt: serverTimestamp(),
  };

  if (updates.name !== undefined) {
    publicUpdate.name = normalizeNameForFirestore(updates.name);
  }
  if (updates.city !== undefined) {
    publicUpdate.city = updates.city.trim().slice(0, 100);
  }
  if (updates.accountType !== undefined) {
    publicUpdate.accountType = resolveAccountType(updates.accountType);
  }
  if (updates.businessActivity !== undefined) {
    publicUpdate.businessActivity = updates.businessActivity.trim().slice(0, 120);
  }
  if (updates.businessLogo !== undefined) {
    publicUpdate.businessLogo = sanitizeUrlForFirestore(
      updates.businessLogo,
      DEFAULT_FALLBACK_AVATAR
    );
  }
  if (updates.avatar !== undefined) {
    publicUpdate.avatar = sanitizeUrlForFirestore(updates.avatar, DEFAULT_FALLBACK_AVATAR);
  } else if (updates.businessLogo !== undefined) {
    publicUpdate.avatar = publicUpdate.businessLogo;
  }

  let latestPublicData: Record<string, any> | null = null;
  try {
    const publicSnap = await getDoc(publicRef);
    if (publicSnap.exists()) {
      await updateDoc(publicRef, publicUpdate);
      latestPublicData = { ...publicSnap.data(), ...publicUpdate };
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `users_public/${uid}`);
  }

  if (latestPublicData) {
    const resolvedType = resolveAccountType(
      (publicUpdate.accountType ?? latestPublicData.accountType) as AccountType | undefined
    );
    await syncCommercialProfileInFirestore(uid, resolvedType, {
      name: normalizeNameForFirestore(publicUpdate.name ?? latestPublicData.name),
      avatar: sanitizeUrlForFirestore(
        publicUpdate.avatar ?? latestPublicData.avatar,
        DEFAULT_FALLBACK_AVATAR
      ),
      city: (publicUpdate.city ?? latestPublicData.city ?? 'الخرطوم').toString().slice(0, 100),
      businessActivity:
        publicUpdate.businessActivity ?? latestPublicData.businessActivity ?? '',
      businessLogo: publicUpdate.businessLogo ?? latestPublicData.businessLogo,
    });
  }

  if (updates.phone !== undefined) {
    try {
      const privateSnap = await getDoc(privateRef);
      if (privateSnap.exists()) {
        await updateDoc(privateRef, {
          phone: normalizeSudanPhoneForFirestore(updates.phone),
          updatedAt: serverTimestamp(),
        });
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users_private/${uid}`);
    }
  }

  // Propagate updated identity strictly to seller's ACTIVE listings (per security rules)
  if (
    updates.name !== undefined ||
    updates.avatar !== undefined ||
    updates.businessLogo !== undefined ||
    updates.accountType !== undefined ||
    updates.businessActivity !== undefined
  ) {
    try {
      const sellerProductsQuery = query(
        collection(db, 'products'),
        where('sellerId', '==', uid),
        where('status', '==', 'active')
      );
      const snap = await getDocs(sellerProductsQuery);
      if (!snap.empty) {
        const batch = writeBatch(db);
        let hasUpdates = false;
        snap.docs.forEach((docSnap) => {
          const prodPatch: Record<string, any> = { updatedAt: serverTimestamp() };
          if (publicUpdate.name) prodPatch.sellerName = publicUpdate.name;
          if (publicUpdate.avatar) prodPatch.sellerAvatar = publicUpdate.avatar;
          if (publicUpdate.accountType) prodPatch.sellerAccountType = publicUpdate.accountType;
          if (publicUpdate.businessActivity !== undefined) {
            prodPatch.sellerBusinessActivity = publicUpdate.businessActivity;
          }
          batch.update(docSnap.ref, prodPatch);
          hasUpdates = true;
        });
        if (hasUpdates) {
          await batch.commit();
        }
      }
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `products (sellerId=${uid})`);
    }
  }
}

export async function publishProductToFirestore(product: Product): Promise<void> {
  const productRef = doc(db, 'products', product.id);
  const safeImages = (product.images || [])
    .slice(0, 5)
    .map((img) => sanitizeUrlForFirestore(img, DEFAULT_FALLBACK_PRODUCT_IMG));
  if (safeImages.length === 0) {
    safeImages.push(DEFAULT_FALLBACK_PRODUCT_IMG);
  }

  const safeDescAr =
    (product.descriptionAr || '').trim().length >= 5
      ? product.descriptionAr.trim().slice(0, 2000)
      : 'منتج متاح للبيع عبر منصة هتش السودان.';

  const safeTitleAr = normalizeNameForFirestore(product.titleAr);
  const safeSellerName = normalizeNameForFirestore(product.sellerName);
  const safeSellerAvatar = sanitizeUrlForFirestore(
    product.sellerAvatar,
    DEFAULT_FALLBACK_AVATAR
  );

  // Never trust client-supplied featured=true at product creation; boosting requires atomic credit spend
  const payload: Record<string, any> = {
    id: product.id,
    titleAr: safeTitleAr,
    titleEn: (product.titleEn || safeTitleAr).slice(0, 120),
    descriptionAr: safeDescAr,
    descriptionEn: (product.descriptionEn || safeDescAr).slice(0, 2000),
    price: Math.max(1, Number(product.price) || 1),
    category: product.category,
    condition: product.condition,
    city: product.city.slice(0, 100),
    areaAr: (product.areaAr || 'وسط المدينة').slice(0, 100),
    areaEn: (product.areaEn || 'City Center').slice(0, 100),
    images: safeImages,
    thumbnailUrl: safeImages[0],
    sellerId: product.sellerId,
    sellerName: safeSellerName,
    sellerAvatar: safeSellerAvatar,
    sellerRating: typeof product.sellerRating === 'number' ? product.sellerRating : 5.0,
    sellerDealsCount: Math.max(0, Math.floor(product.sellerDealsCount || 0)),
    sellerVerified: Boolean(product.sellerVerified),
    sellerAccountType: resolveAccountType(product.sellerAccountType),
    paymentMethods:
      product.paymentMethods && product.paymentMethods.length > 0
        ? product.paymentMethods.slice(0, 4)
        : ['bankak', 'cash'],
    negotiable: Boolean(product.negotiable),
    deliveryAvailable: Boolean(product.deliveryAvailable),
    urgentSale: Boolean(product.urgentSale),
    featured: false,
    viewsCount: 0,
    likesCount: 0,
    status: 'active',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };

  if (product.originalPrice && product.originalPrice > 0) {
    payload.originalPrice = Number(product.originalPrice);
  }
  if (product.sellerBusinessActivity) {
    payload.sellerBusinessActivity = product.sellerBusinessActivity.trim().slice(0, 120);
  }

  try {
    await setDoc(productRef, payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, `products/${product.id}`);
  }
}

export async function markProductAsSoldInFirestore(
  productId: string,
  sellerId: string
): Promise<void> {
  const productRef = doc(db, 'products', productId);
  const publicRef = doc(db, 'users_public', sellerId);
  const commercialRef = doc(db, 'commercial_profiles', sellerId);

  try {
    await runTransaction(db, async (tx) => {
      const [prodSnap, pubSnap, commSnap] = await Promise.all([
        tx.get(productRef),
        tx.get(publicRef),
        tx.get(commercialRef),
      ]);
      if (!prodSnap.exists()) {
        throw new Error('Product does not exist');
      }
      const prodData = prodSnap.data();
      if (prodData.sellerId !== sellerId || prodData.status !== 'active') {
        throw new Error('Cannot mark this listing as sold');
      }
      if (!pubSnap.exists()) {
        throw new Error('Seller public profile does not exist');
      }
      const pubData = pubSnap.data();
      const nextDeals = (typeof pubData.dealsCompleted === 'number' ? pubData.dealsCompleted : 0) + 1;

      tx.update(productRef, {
        status: 'sold',
        updatedAt: serverTimestamp(),
      });
      tx.update(publicRef, {
        dealsCompleted: nextDeals,
        lastSoldProductId: productId,
        updatedAt: serverTimestamp(),
      });
      if (commSnap.exists()) {
        const commData = commSnap.data();
        const commNextDeals =
          (typeof commData.dealsCompleted === 'number' ? commData.dealsCompleted : 0) + 1;
        tx.update(commercialRef, {
          dealsCompleted: commNextDeals,
          lastSoldProductId: productId,
          updatedAt: serverTimestamp(),
        });
      }
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `products/${productId}`);
  }
}

export async function featureProductWithCreditInFirestore(
  productId: string,
  uid: string
): Promise<string> {
  const txId = `tx_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const privateRef = doc(db, 'users_private', uid);
  const productRef = doc(db, 'products', productId);
  const txRef = doc(db, 'credit_transactions', txId);

  try {
    await runTransaction(db, async (tx) => {
      const [privSnap, prodSnap] = await Promise.all([
        tx.get(privateRef),
        tx.get(productRef),
      ]);
      if (!privSnap.exists()) {
        throw new Error('Private profile not found');
      }
      if (!prodSnap.exists()) {
        throw new Error('Product not found');
      }
      const privData = privSnap.data();
      const prodData = prodSnap.data();
      const balanceBefore =
        typeof privData.rewardCredits === 'number' ? privData.rewardCredits : 0;

      if (balanceBefore < 1) {
        throw new Error('Insufficient boost credits');
      }
      if (prodData.sellerId !== uid || prodData.status !== 'active') {
        throw new Error('Only seller active listings can be boosted');
      }
      if (prodData.featured === true) {
        throw new Error('Listing is already featured');
      }

      const balanceAfter = balanceBefore - 1;

      tx.set(txRef, {
        id: txId,
        uid,
        type: 'ad_boost_spend',
        amount: -1,
        balanceBefore,
        balanceAfter,
        productId,
        reason: 'تمييز إعلان عبر رصيد التمييز المعتمد',
        createdAt: serverTimestamp(),
      });
      tx.update(privateRef, {
        rewardCredits: balanceAfter,
        lastCreditTxId: txId,
        updatedAt: serverTimestamp(),
      });
      tx.update(productRef, {
        featured: true,
        featuredTxId: txId,
        updatedAt: serverTimestamp(),
      });
    });
    return txId;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `credit_transactions/${txId}`);
  }
}

export async function grantRewardCreditInFirestore(
  targetUid: string,
  amount: number,
  reason: string,
  adminUid: string
): Promise<string> {
  const safeAmount = Math.max(1, Math.min(100, Math.floor(amount)));
  const txId = `tx_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const privateRef = doc(db, 'users_private', targetUid);
  const txRef = doc(db, 'credit_transactions', txId);

  try {
    await runTransaction(db, async (tx) => {
      const privSnap = await tx.get(privateRef);
      if (!privSnap.exists()) {
        throw new Error('Target users_private document does not exist');
      }
      const privData = privSnap.data();
      const balanceBefore =
        typeof privData.rewardCredits === 'number' ? privData.rewardCredits : 0;
      const balanceAfter = balanceBefore + safeAmount;

      tx.set(txRef, {
        id: txId,
        uid: targetUid,
        type: 'admin_grant',
        amount: safeAmount,
        balanceBefore,
        balanceAfter,
        grantedBy: adminUid,
        reason: reason.trim().slice(0, 240) || 'منحة رصيد تمييز من إدارة هتش',
        createdAt: serverTimestamp(),
      });
      tx.update(privateRef, {
        rewardCredits: balanceAfter,
        lastCreditTxId: txId,
        updatedAt: serverTimestamp(),
      });
    });
    return txId;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `credit_transactions/${txId}`);
  }
}

export async function submitVerificationRequestToFirestore(
  uid: string,
  user: User,
  note: string,
  documentUrl?: string
): Promise<VerificationRequest> {
  const reqId = `vreq_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
  const reqRef = doc(db, 'verification_requests', reqId);
  const safeNote = note.trim().slice(0, 600);
  if (safeNote.length < 5) {
    throw new Error('Verification note must be at least 5 characters');
  }

  const payload: Record<string, any> = {
    id: reqId,
    uid,
    userName: normalizeNameForFirestore(user.name),
    userAvatar: sanitizeUrlForFirestore(user.businessLogo || user.avatar, DEFAULT_FALLBACK_AVATAR),
    accountType: resolveAccountType(user.accountType),
    city: (user.city || 'الخرطوم').toString().slice(0, 100),
    note: safeNote,
    status: 'pending',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };

  if (documentUrl && documentUrl.trim() && !documentUrl.trim().startsWith('data:')) {
    payload.documentUrl = documentUrl.trim().slice(0, 1000);
  }

  try {
    await setDoc(reqRef, payload);
    return {
      id: reqId,
      uid,
      userName: payload.userName,
      userAvatar: payload.userAvatar,
      accountType: payload.accountType,
      city: payload.city,
      note: safeNote,
      documentUrl: payload.documentUrl,
      status: 'pending',
      createdAt: new Date().toISOString().split('T')[0],
      updatedAt: new Date().toISOString().split('T')[0],
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, `verification_requests/${reqId}`);
  }
}

export async function reviewVerificationRequestInFirestore(
  reqId: string,
  targetUid: string,
  decision: 'approved' | 'rejected' | 'revoked',
  adminUid: string,
  rejectionReason?: string
): Promise<void> {
  const reqRef = doc(db, 'verification_requests', reqId);
  const publicRef = doc(db, 'users_public', targetUid);
  const commercialRef = doc(db, 'commercial_profiles', targetUid);
  const isVerified = decision === 'approved';

  try {
    const reqUpdate: Record<string, any> = {
      status: decision,
      reviewedBy: adminUid,
      reviewedAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (rejectionReason && rejectionReason.trim()) {
      reqUpdate.rejectionReason = rejectionReason.trim().slice(0, 400);
    }

    const batch = writeBatch(db);
    batch.update(reqRef, reqUpdate);
    batch.update(publicRef, {
      verifiedIdentity: isVerified,
      updatedAt: serverTimestamp(),
    });

    const commSnap = await getDoc(commercialRef);
    if (commSnap.exists()) {
      batch.update(commercialRef, {
        verifiedIdentity: isVerified,
        updatedAt: serverTimestamp(),
      });
    }

    const prodSnap = await getDocs(
      query(collection(db, 'products'), where('sellerId', '==', targetUid))
    );
    prodSnap.docs.forEach((docSnap) => {
      batch.update(docSnap.ref, {
        sellerVerified: isVerified,
        updatedAt: serverTimestamp(),
      });
    });

    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `verification_requests/${reqId}`);
  }
}

export async function setUserVerifiedInFirestore(
  targetUid: string,
  verified: boolean
): Promise<void> {
  const publicRef = doc(db, 'users_public', targetUid);
  const commercialRef = doc(db, 'commercial_profiles', targetUid);
  try {
    const batch = writeBatch(db);
    batch.update(publicRef, {
      verifiedIdentity: verified,
      updatedAt: serverTimestamp(),
    });
    const commSnap = await getDoc(commercialRef);
    if (commSnap.exists()) {
      batch.update(commercialRef, {
        verifiedIdentity: verified,
        updatedAt: serverTimestamp(),
      });
    }
    const prodSnap = await getDocs(
      query(collection(db, 'products'), where('sellerId', '==', targetUid))
    );
    prodSnap.docs.forEach((docSnap) => {
      batch.update(docSnap.ref, {
        sellerVerified: verified,
        updatedAt: serverTimestamp(),
      });
    });
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `users_public/${targetUid}`);
  }
}

export async function submitReportToFirestore(report: ReportTicket): Promise<void> {
  const reportRef = doc(db, 'reports', report.id);
  const payload: Record<string, any> = {
    id: report.id,
    reporterId: report.reporterId,
    reporterName: normalizeNameForFirestore(report.reporterName),
    reason: report.reason.trim().slice(0, 120) || 'مخالفة',
    message:
      report.message.trim().length >= 5
        ? report.message.trim().slice(0, 1500)
        : 'بلاغ مخالفة للمراجعة من إدارة هتش',
    status: 'pending',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
  if (report.reportedUserId) payload.reportedUserId = report.reportedUserId;
  if (report.reportedUserName) payload.reportedUserName = report.reportedUserName.slice(0, 80);
  if (report.reportedProductId) payload.reportedProductId = report.reportedProductId;
  if (report.reportedProductTitle) payload.reportedProductTitle = report.reportedProductTitle.slice(0, 140);

  try {
    await setDoc(reportRef, payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, `reports/${report.id}`);
  }
}

export async function adminApproveProductInFirestore(productId: string): Promise<void> {
  try {
    await updateDoc(doc(db, 'products', productId), {
      status: 'active',
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `products/${productId}`);
  }
}

export async function adminFlagProductInFirestore(productId: string): Promise<void> {
  try {
    await updateDoc(doc(db, 'products', productId), {
      status: 'flagged',
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `products/${productId}`);
  }
}

export async function adminDeleteProductInFirestore(productId: string): Promise<void> {
  try {
    await deleteDoc(doc(db, 'products', productId));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, `products/${productId}`);
  }
}

export async function adminSuspendUserInFirestore(
  userId: string,
  days: number,
  reason: string
): Promise<void> {
  const safeDays = Math.max(1, Math.min(3650, Math.floor(days)));
  const untilDate = new Date(Date.now() + safeDays * 24 * 60 * 60 * 1000);
  const publicRef = doc(db, 'users_public', userId);
  const commercialRef = doc(db, 'commercial_profiles', userId);

  try {
    const batch = writeBatch(db);
    batch.update(publicRef, {
      status: 'suspended',
      suspendedUntil: untilDate.toISOString(),
      suspendedUntilTs: Timestamp.fromDate(untilDate),
      suspensionDays: safeDays,
      suspensionReason: reason.trim().slice(0, 500) || 'مخالفة شروط الاستخدام',
      updatedAt: serverTimestamp(),
    });
    const commSnap = await getDoc(commercialRef);
    if (commSnap.exists()) {
      batch.update(commercialRef, {
        status: 'suspended',
        updatedAt: serverTimestamp(),
      });
    }
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `users_public/${userId}`);
  }
}

export async function adminUnsuspendUserInFirestore(userId: string): Promise<void> {
  const publicRef = doc(db, 'users_public', userId);
  const commercialRef = doc(db, 'commercial_profiles', userId);
  try {
    const batch = writeBatch(db);
    batch.update(publicRef, {
      status: 'active',
      suspendedUntil: deleteField(),
      suspendedUntilTs: deleteField(),
      suspensionDays: deleteField(),
      suspensionReason: deleteField(),
      updatedAt: serverTimestamp(),
    });
    const commSnap = await getDoc(commercialRef);
    if (commSnap.exists()) {
      batch.update(commercialRef, {
        status: 'active',
        updatedAt: serverTimestamp(),
      });
    }
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `users_public/${userId}`);
  }
}

export async function adminResolveReportInFirestore(
  reportId: string,
  status: 'resolved' | 'dismissed'
): Promise<void> {
  try {
    await updateDoc(doc(db, 'reports', reportId), {
      status,
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `reports/${reportId}`);
  }
}

export function subscribeToPrivateCredits(
  uid: string,
  onCredits: (credits: number) => void
): () => void {
  return onSnapshot(
    doc(db, 'users_private', uid),
    (snap) => {
      if (snap.exists()) {
        const data = snap.data();
        onCredits(typeof data.rewardCredits === 'number' ? Math.max(0, Math.floor(data.rewardCredits)) : 0);
      }
    },
    () => {
      // Gracefully ignore before private doc onboarding completes
    }
  );
}

export function subscribeToVerificationRequests(
  uid: string,
  hasAdminClaim: boolean,
  onRequests: (requests: VerificationRequest[]) => void
): () => void {
  const q = hasAdminClaim
    ? query(collection(db, 'verification_requests'))
    : query(collection(db, 'verification_requests'), where('uid', '==', uid));

  return onSnapshot(
    q,
    (snapshot) => {
      const loaded: VerificationRequest[] = snapshot.docs.map((docSnap) => {
        const d = docSnap.data();
        return {
          id: d.id || docSnap.id,
          uid: d.uid || '',
          userName: d.userName || 'مستخدم هتش',
          userAvatar: d.userAvatar || undefined,
          accountType: resolveAccountType(d.accountType),
          city: d.city || 'الخرطوم',
          note: d.note || '',
          documentUrl: d.documentUrl || undefined,
          status: (d.status || 'pending') as VerificationRequest['status'],
          reviewedBy: d.reviewedBy || undefined,
          rejectionReason: d.rejectionReason || undefined,
          createdAt: formatJoinedDate(d.createdAt),
          updatedAt: formatJoinedDate(d.updatedAt),
          reviewedAt: d.reviewedAt ? formatJoinedDate(d.reviewedAt) : undefined,
        };
      });
      onRequests(loaded);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, 'verification_requests');
    }
  );
}

export function subscribeToCreditTransactions(
  uid: string,
  hasAdminClaim: boolean,
  onTransactions: (txs: CreditTransaction[]) => void
): () => void {
  const q = hasAdminClaim
    ? query(collection(db, 'credit_transactions'))
    : query(collection(db, 'credit_transactions'), where('uid', '==', uid));

  return onSnapshot(
    q,
    (snapshot) => {
      const loaded: CreditTransaction[] = snapshot.docs.map((docSnap) => {
        const d = docSnap.data();
        return {
          id: d.id || docSnap.id,
          uid: d.uid || '',
          type: (d.type || 'admin_grant') as CreditTransaction['type'],
          amount: Number(d.amount) || 0,
          balanceBefore: Number(d.balanceBefore) || 0,
          balanceAfter: Number(d.balanceAfter) || 0,
          productId: d.productId || undefined,
          grantedBy: d.grantedBy || undefined,
          reason: d.reason || '',
          createdAt: formatJoinedDate(d.createdAt),
        };
      });
      onTransactions(loaded);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, 'credit_transactions');
    }
  );
}

export function subscribeToReports(
  uid: string,
  hasAdminClaim: boolean,
  onReports: (reports: ReportTicket[]) => void
): () => void {
  const q = hasAdminClaim
    ? query(collection(db, 'reports'))
    : query(collection(db, 'reports'), where('reporterId', '==', uid));

  return onSnapshot(
    q,
    (snapshot) => {
      const loaded: ReportTicket[] = snapshot.docs.map((docSnap) => {
        const d = docSnap.data();
        return {
          id: d.id || docSnap.id,
          reporterId: d.reporterId || '',
          reporterName: d.reporterName || '',
          reportedUserId: d.reportedUserId || undefined,
          reportedUserName: d.reportedUserName || undefined,
          reportedProductId: d.reportedProductId || undefined,
          reportedProductTitle: d.reportedProductTitle || undefined,
          reason: d.reason || '',
          message: d.message || '',
          createdAt: formatJoinedDate(d.createdAt),
          status: (d.status || 'pending') as ReportTicket['status'],
        };
      });
      onReports(loaded);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, 'reports');
    }
  );
}

export function subscribeToFirestoreCatalog(
  isSignedIn: boolean,
  hasAdminClaim: boolean,
  onUsers: (users: User[]) => void,
  onProducts: (products: Product[]) => void
): () => void {
  const productsQuery = hasAdminClaim
    ? query(collection(db, 'products'))
    : query(collection(db, 'products'), where('status', 'in', ['active', 'sold']));

  const unsubProducts = onSnapshot(
    productsQuery,
    (snapshot) => {
      const loaded: Product[] = snapshot.docs.map((docSnap) => {
        const d = docSnap.data();
        return {
          id: d.id || docSnap.id,
          titleAr: d.titleAr || '',
          titleEn: d.titleEn || d.titleAr || '',
          descriptionAr: d.descriptionAr || '',
          descriptionEn: d.descriptionEn || d.descriptionAr || '',
          price: Number(d.price) || 0,
          originalPrice: d.originalPrice ? Number(d.originalPrice) : undefined,
          category: d.category,
          condition: d.condition,
          city: d.city || 'الخرطوم',
          areaAr: d.areaAr || '',
          areaEn: d.areaEn || '',
          images: Array.isArray(d.images) && d.images.length > 0 ? d.images : [DEFAULT_FALLBACK_PRODUCT_IMG],
          sellerId: d.sellerId || '',
          sellerName: d.sellerName || '',
          sellerAvatar: d.sellerAvatar || DEFAULT_FALLBACK_AVATAR,
          sellerPhone: '+249 910000000',
          sellerRating: typeof d.sellerRating === 'number' ? d.sellerRating : 5.0,
          sellerDealsCount: typeof d.sellerDealsCount === 'number' ? d.sellerDealsCount : 0,
          sellerVerified: Boolean(d.sellerVerified),
          sellerAccountType: resolveAccountType(d.sellerAccountType),
          sellerBusinessActivity: d.sellerBusinessActivity || undefined,
          paymentMethods: Array.isArray(d.paymentMethods) ? d.paymentMethods : ['cash'],
          negotiable: Boolean(d.negotiable),
          deliveryAvailable: Boolean(d.deliveryAvailable),
          urgentSale: Boolean(d.urgentSale),
          featured: Boolean(d.featured),
          viewsCount: Number(d.viewsCount) || 0,
          likesCount: Number(d.likesCount) || 0,
          status: d.status || 'active',
          createdAt: formatJoinedDate(d.createdAt),
        };
      });
      onProducts(loaded);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, 'products');
    }
  );

  let unsubUsers = () => {};
  if (isSignedIn) {
    const usersQuery = hasAdminClaim
      ? query(collection(db, 'users_public'))
      : query(collection(db, 'users_public'), where('status', '==', 'active'));

    unsubUsers = onSnapshot(
      usersQuery,
      (snapshot) => {
        const loaded: User[] = snapshot.docs.map((docSnap) => {
          const d = docSnap.data();
          const accountType = resolveAccountType(d.accountType);
          const avatar = d.businessLogo || d.avatar || DEFAULT_FALLBACK_AVATAR;
          const susp = parseSuspendedFields(d);
          return {
            id: d.uid || docSnap.id,
            name: d.name || 'مستخدم هتش',
            avatar,
            businessLogo: d.businessLogo || undefined,
            businessActivity: d.businessActivity || undefined,
            accountType,
            phone: '+249 910000000',
            city: d.city || 'الخرطوم',
            rating: typeof d.rating === 'number' ? d.rating : 5.0,
            dealsCompleted: typeof d.dealsCompleted === 'number' ? d.dealsCompleted : 0,
            reportsCount: typeof d.reportsCount === 'number' ? d.reportsCount : 0,
            verifiedIdentity: Boolean(d.verifiedIdentity),
            joinedDate: formatJoinedDate(d.joinedDate),
            role: 'user',
            status: susp.status,
            suspendedUntil: susp.suspendedUntil,
            suspendedUntilMs: susp.suspendedUntilMs,
            suspensionDays: susp.status === 'suspended' ? d.suspensionDays : undefined,
            suspensionReason: susp.status === 'suspended' ? d.suspensionReason : undefined,
          };
        });
        onUsers(loaded);
      },
      (error) => {
        handleFirestoreError(error, OperationType.LIST, 'users_public');
      }
    );
  } else {
    // Guest view: subscribe to public active commercial profiles (stores & companies)
    const commQuery = query(
      collection(db, 'commercial_profiles'),
      where('status', '==', 'active'),
      where('accountType', 'in', ['store', 'company'])
    );
    unsubUsers = onSnapshot(
      commQuery,
      (snapshot) => {
        const loaded: User[] = snapshot.docs.map((docSnap) => {
          const d = docSnap.data();
          const accountType = resolveAccountType(d.accountType);
          const avatar = d.businessLogo || d.avatar || DEFAULT_FALLBACK_AVATAR;
          return {
            id: d.uid || docSnap.id,
            name: d.name || 'متجر هتش',
            avatar,
            businessLogo: d.businessLogo || undefined,
            businessActivity: d.businessActivity || undefined,
            accountType,
            phone: '+249 910000000',
            city: d.city || 'الخرطوم',
            rating: typeof d.rating === 'number' ? d.rating : 5.0,
            dealsCompleted: typeof d.dealsCompleted === 'number' ? d.dealsCompleted : 0,
            reportsCount: typeof d.reportsCount === 'number' ? d.reportsCount : 0,
            verifiedIdentity: Boolean(d.verifiedIdentity),
            joinedDate: formatJoinedDate(d.joinedDate),
            role: 'user',
            status: 'active',
          };
        });
        onUsers(loaded);
      },
      () => {
        // Gracefully allow local catalog for offline/preview
      }
    );
  }

  return () => {
    unsubProducts();
    unsubUsers();
  };
}
