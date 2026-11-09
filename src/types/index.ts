export type Language = 'ar' | 'en';

export type CategoryId = 
  | 'phones'
  | 'electronics'
  | 'vehicles'
  | 'furniture'
  | 'fashion'
  | 'appliances'
  | 'solar_energy'
  | 'real_estate'
  | 'books_hobbies';

export type Condition = 'new' | 'like_new' | 'good' | 'fair';

export const SUDANESE_CITIES = [
  'الخرطوم (ولاية الخرطوم)',
  'أم درمان (ولاية الخرطوم)',
  'بحري (ولاية الخرطوم)',
  'ودمدني (ولاية الجزيرة)',
  'المناقل (ولاية الجزيرة)',
  'رفاعة (ولاية الجزيرة)',
  'الحصاحيصا (ولاية الجزيرة)',
  'الكاملين (ولاية الجزيرة)',
  'عطبرة (ولاية نهر النيل)',
  'الدامر (ولاية نهر النيل)',
  'شندي (ولاية نهر النيل)',
  'أبو حمد (ولاية نهر النيل)',
  'بربر (ولاية نهر النيل)',
  'دنقلا (الولاية الشمالية)',
  'مروي (الولاية الشمالية)',
  'كريمة (الولاية الشمالية)',
  'وادي حلفا (الولاية الشمالية)',
  'بورتسودان (ولاية البحر الأحمر)',
  'سواكن (ولاية البحر الأحمر)',
  'طوكر (ولاية البحر الأحمر)',
  'حلايب (ولاية البحر الأحمر)',
  'كسلا (ولاية كسلا)',
  'حلفا الجديدة (ولاية كسلا)',
  'القضارف (ولاية القضارف)',
  'الفاو (ولاية القضارف)',
  'الحواتة (ولاية القضارف)',
  'سنجة (ولاية سنار)',
  'سنار (ولاية سنار)',
  'الدندر (ولاية سنار)',
  'كوستي (ولاية النيل الأبيض)',
  'ربك (ولاية النيل الأبيض)',
  'الدويم (ولاية النيل الأبيض)',
  'الجبلين (ولاية النيل الأبيض)',
  'الأبيض (ولاية شمال كردفان)',
  'أم روابة (ولاية شمال كردفان)',
  'الرهد (ولاية شمال كردفان)',
  'كادقلي (ولاية جنوب كردفان)',
  'الدلنج (ولاية جنوب كردفان)',
  'رشاد (ولاية جنوب كردفان)',
  'تلودي (ولاية جنوب كردفان)',
  'الفولة (ولاية غرب كردفان)',
  'النهود (ولاية غرب كردفان)',
  'بابنوسة (ولاية غرب كردفان)',
  'المجلد (ولاية غرب كردفان)',
  'الفاشر (ولاية شمال دارفور)',
  'مليط (ولاية شمال دارفور)',
  'كتم (ولاية شمال دارفور)',
  'أم كدادة (ولاية شمال دارفور)',
  'نيالا (ولاية جنوب دارفور)',
  'برام (ولاية جنوب دارفور)',
  'الجنينة (ولاية غرب دارفور)',
  'زالنجي (ولاية وسط دارفور)',
  'نيرتتي (ولاية وسط دارفور)',
  'الضعين (ولاية شرق دارفور)',
  'الدمازين (ولاية النيل الأزرق)',
  'الرصيرص (ولاية النيل الأزرق)',
] as const;

export type SudaneseCity = (typeof SUDANESE_CITIES)[number] | string;

export type PaymentMethod = 'bankak' | 'fawry' | 'cash' | 'ocash';

export interface Product {
  id: string;
  titleAr: string;
  titleEn: string;
  descriptionAr: string;
  descriptionEn: string;
  price: number; // in Sudanese Pounds (SDG)
  originalPrice?: number;
  category: CategoryId;
  condition: Condition;
  city: SudaneseCity;
  areaAr: string;
  areaEn: string;
  images: string[];
  sellerId: string;
  sellerName: string;
  sellerAvatar: string;
  sellerPhone: string;
  sellerRating: number;
  sellerDealsCount: number;
  sellerVerified: boolean;
  sellerReportsCount?: number;
  paymentMethods: PaymentMethod[];
  negotiable: boolean;
  deliveryAvailable: boolean;
  urgentSale?: boolean;
  featured?: boolean;
  viewsCount: number;
  likesCount: number;
  status: 'active' | 'sold' | 'under_review' | 'flagged';
  createdAt: string;
}

export interface User {
  id: string;
  name: string;
  avatar: string;
  phone: string;
  city: SudaneseCity;
  rating: number;
  dealsCompleted: number;
  reportsCount?: number;
  verifiedIdentity: boolean;
  joinedDate: string;
  role: 'user' | 'admin';
  status?: 'active' | 'suspended';
  suspendedUntil?: string;
  suspensionDays?: number;
  suspensionReason?: string;
  suspendedAt?: string;
}

export const isUserVerified = (dealsCompleted: number, reportsCount: number = 0): boolean => {
  return dealsCompleted >= 10 && reportsCount === 0;
};

export type OfferStatus = 'pending' | 'accepted' | 'declined' | 'countered';

export interface Offer {
  id: string;
  productId: string;
  productTitle: string;
  productImage: string;
  sellerId: string;
  buyerId: string;
  buyerName: string;
  originalPrice: number;
  offeredPrice: number;
  counterPrice?: number;
  status: OfferStatus;
  createdAt: string;
}

export type MessageStatus = 'sent' | 'delivered' | 'read';

export interface ChatMessage {
  id: string;
  senderId: string;
  text: string;
  timestamp: string;
  status?: MessageStatus;
  isOfferUpdate?: boolean;
  offerAmount?: number;
  audioUrl?: string;
  audioDuration?: number; // duration in seconds
  imageUrl?: string; // photo sent during product negotiation
  imageTag?: string; // e.g. inspection category or proof
}

export interface Conversation {
  id: string;
  productId: string;
  productTitle: string;
  productPrice: number;
  productImage: string;
  participantId: string;
  participantName: string;
  participantAvatar: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
  activeOffer?: Offer;
  messages: ChatMessage[];
}

export interface NotificationItem {
  id: string;
  type: 'offer' | 'message' | 'price_drop' | 'verification' | 'system' | 'alert' | 'update';
  titleAr: string;
  titleEn: string;
  bodyAr: string;
  bodyEn: string;
  time: string;
  read: boolean;
  relatedProductId?: string;
  relatedConversationId?: string;
}

export interface ReportTicket {
  id: string;
  reporterId: string;
  reporterName: string;
  reportedUserId?: string;
  reportedUserName?: string;
  reportedProductId?: string;
  reportedProductTitle?: string;
  reason: string;
  message: string;
  createdAt: string;
  status: 'pending' | 'reviewed' | 'resolved';
}

export type ActiveTab = 'swipe' | 'browse' | 'sell' | 'messages' | 'favorites' | 'profile' | 'admin';

export interface UserReview {
  id: string;
  sellerId: string;
  sellerName?: string;
  reviewerId: string;
  reviewerName: string;
  reviewerAvatar: string;
  rating: number; // 1 to 5
  comment: string;
  tags?: string[];
  productId?: string;
  productTitle?: string;
  createdAt: string;
}

export type AdFormat = 'banner' | 'interstitial' | 'rewarded' | 'native' | 'all';

export interface AdMobConfig {
  isEnabled: boolean;
  isTestMode: boolean;
  admobAppId: string;
  bannerUnitId: string;
  interstitialUnitId: string;
  rewardedUnitId: string;
  nativeUnitId: string;
  ecpmRate: number; // in USD per 1000 impressions (e.g. 1.85)
  cpcRate: number; // in USD per click (e.g. 0.14)
  sdgRate: number; // USD to SDG conversion rate (e.g. 2650)
  bannerRotationSeconds: number; // duration of banner appearance in seconds
  interstitialCountdownSeconds: number; // duration before skip in seconds
  rewardedDurationSeconds: number; // duration of rewarded video in seconds
}

export interface AdStats {
  impressions: number;
  clicks: number;
  rewardedCompleted: number;
  estimatedEarningsUsd: number;
  estimatedEarningsSdg: number;
  lastUpdated: string;
}

export interface SponsoredAdItem {
  id: string;
  titleAr: string;
  titleEn: string;
  advertiser: string;
  descriptionAr: string;
  descriptionEn: string;
  imageUrl: string;
  logoUrl?: string;
  ctaTextAr: string;
  ctaTextEn: string;
  targetUrl: string;
  category: string;
  badgeAr: string;
  badgeEn: string;
  format?: AdFormat;
  isActive: boolean;
  durationSeconds?: number;
  clicksCount?: number;
  impressionsCount?: number;
  createdAt?: string;
  videoUrl?: string;
  mediaType?: 'image' | 'video';
}

