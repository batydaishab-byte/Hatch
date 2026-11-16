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
  collection,
  query,
  where,
  getDocs,
  onSnapshot,
  serverTimestamp,
  writeBatch,
  Timestamp,
} from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';
import { AccountType, Product, resolveAccountType, User } from '../types';

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
      role: priv.role === 'admin' ? 'admin' : 'user',
      status: pub.status === 'suspended' ? 'suspended' : 'active',
      suspendedUntil: pub.suspendedUntil,
      suspensionDays: pub.suspensionDays,
      suspensionReason: pub.suspensionReason,
      profileCompleted: isComplete,
    };

    return { user, isComplete };
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, `${publicPath} / ${privatePath}`);
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

  try {
    const publicSnap = await getDoc(publicRef);
    if (publicSnap.exists()) {
      await updateDoc(publicRef, publicUpdate);
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, `users_public/${uid}`);
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

  // Propagate updated identity to seller's active listings in Firestore so cards/detail views stay in sync
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
        where('sellerId', '==', uid)
      );
      const snap = await getDocs(sellerProductsQuery);
      if (!snap.empty) {
        const batch = writeBatch(db);
        let hasUpdates = false;
        snap.docs.forEach((docSnap) => {
          const data = docSnap.data();
          if (data.status !== 'sold') {
            const prodPatch: Record<string, any> = { updatedAt: serverTimestamp() };
            if (publicUpdate.name) prodPatch.sellerName = publicUpdate.name;
            if (publicUpdate.avatar) prodPatch.sellerAvatar = publicUpdate.avatar;
            if (publicUpdate.accountType) prodPatch.sellerAccountType = publicUpdate.accountType;
            if (publicUpdate.businessActivity !== undefined) {
              prodPatch.sellerBusinessActivity = publicUpdate.businessActivity;
            }
            batch.update(docSnap.ref, prodPatch);
            hasUpdates = true;
          }
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
    featured: Boolean(product.featured),
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

export function subscribeToFirestoreCatalog(
  isSignedIn: boolean,
  onUsers: (users: User[]) => void,
  onProducts: (products: Product[]) => void
): () => void {
  const unsubProducts = onSnapshot(
    query(collection(db, 'products'), where('status', 'in', ['active', 'sold'])),
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
    unsubUsers = onSnapshot(
      query(collection(db, 'users_public'), where('status', '==', 'active')),
      (snapshot) => {
        const loaded: User[] = snapshot.docs.map((docSnap) => {
          const d = docSnap.data();
          const accountType = resolveAccountType(d.accountType);
          const avatar = d.businessLogo || d.avatar || DEFAULT_FALLBACK_AVATAR;
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
            status: d.status === 'suspended' ? 'suspended' : 'active',
          };
        });
        onUsers(loaded);
      },
      (error) => {
        handleFirestoreError(error, OperationType.LIST, 'users_public');
      }
    );
  }

  return () => {
    unsubProducts();
    unsubUsers();
  };
}
