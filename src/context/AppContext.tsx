import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import confetti from 'canvas-confetti';
import { 
  Product, 
  User, 
  Conversation, 
  NotificationItem, 
  Language, 
  ActiveTab, 
  SudaneseCity, 
  CategoryId, 
  Condition,
  isUserVerified,
  ReportTicket,
  UserReview,
  AdMobConfig,
  AdStats,
  SponsoredAdItem,
  AdFormat
} from '../types';
import { translations } from '../data/translations';
import { 
  initialProducts, 
  mockUsers, 
  initialConversations, 
  initialNotifications, 
  initialReports, 
  initialReviews,
  defaultAdMobConfig,
  defaultAdStats,
  initialSponsoredAds
} from '../data/mockData';

interface ToastState {
  id: string;
  message: string;
  type: 'success' | 'info' | 'warning';
}

interface AppContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: typeof translations.ar;
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab) => void;
  
  // User & Auth Persona
  currentUser: User;
  switchUser: (userId: string) => void;
  availableUsers: User[];
  createNewAccount: (userData: { name: string; phone: string; city: SudaneseCity; avatar?: string }) => void;
  updateCurrentUser: (userData: Partial<User>) => void;
  isCreateAccountOpen: boolean;
  setIsCreateAccountOpen: (open: boolean) => void;
  isEditProfileOpen: boolean;
  setIsEditProfileOpen: (open: boolean) => void;
  
  // Products & Swipe Deck
  products: Product[];
  swipedIds: string[];
  favorites: string[];
  toggleFavorite: (productId: string) => void;
  swipeRight: (productId: string, openOffer?: boolean) => void;
  swipeLeft: (productId?: string) => void;
  superLike: (productId: string) => void;
  undoSwipe: () => void;
  resetDeck: () => void;
  addProduct: (productData: Omit<Product, 'id' | 'createdAt' | 'viewsCount' | 'likesCount' | 'status'>) => void;
  updateProduct: (productId: string, updatedData: Partial<Product>) => void;
  deleteProduct: (productId: string) => void;
  markProductAsSold: (productId: string) => void;
  
  // Filters & Search
  selectedCity: SudaneseCity | 'all';
  setSelectedCity: (city: SudaneseCity | 'all') => void;
  selectedCategory: CategoryId | 'all';
  setSelectedCategory: (cat: CategoryId | 'all') => void;
  selectedCondition: Condition | 'all';
  setSelectedCondition: (cond: Condition | 'all') => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
  onlyNegotiable: boolean;
  setOnlyNegotiable: (val: boolean) => void;
  onlyUrgent: boolean;
  setOnlyUrgent: (val: boolean) => void;
  onlyBankak: boolean;
  setOnlyBankak: (val: boolean) => void;
  priceRange: [number, number];
  setPriceRange: (range: [number, number]) => void;
  isPriceUnlimited: boolean;
  setIsPriceUnlimited: (val: boolean) => void;
  resetFilters: () => void;

  // Modals & Sheets
  selectedProductForDetail: Product | null;
  setSelectedProductForDetail: (product: Product | null) => void;
  selectedProductForOffer: Product | null;
  setSelectedProductForOffer: (product: Product | null) => void;
  isSellModalOpen: boolean;
  setIsSellModalOpen: (open: boolean) => void;
  isSafetyModalOpen: boolean;
  setIsSafetyModalOpen: (open: boolean) => void;
  isNotificationsOpen: boolean;
  setIsNotificationsOpen: (open: boolean) => void;
  isApkModalOpen: boolean;
  setIsApkModalOpen: (open: boolean) => void;

  // Messaging & Offers
  conversations: Conversation[];
  activeConversationId: string | null;
  setActiveConversationId: (id: string | null) => void;
  startOrOpenConversation: (productId: string) => void;
  sendMessage: (
    conversationId: string, 
    text?: string, 
    audioData?: { audioUrl: string; audioDuration: number },
    imageData?: { imageUrl: string; imageTag?: string }
  ) => void;
  submitOffer: (productId: string, amount: number, note?: string) => void;
  respondToOffer: (conversationId: string, action: 'accept' | 'decline' | 'counter', counterAmount?: number) => void;

  // Notifications
  notifications: NotificationItem[];
  markNotificationsAsRead: () => void;
  broadcastNotification: (notifData: {
    title: string;
    body: string;
    type: 'offer' | 'alert' | 'update' | 'system';
    relatedProductId?: string;
  }) => void;
  unreadNotificationsCount: number;
  unreadMessagesCount: number;

  // Admin & Moderation & Reports
  flagProduct: (productId: string, reason: string) => void;
  adminApproveProduct: (productId: string) => void;
  adminDeleteProduct: (productId: string) => void;
  adminDeleteUser: (userId: string) => void;
  adminSuspendUser: (userId: string, days: number, reason: string) => void;
  adminUnsuspendUser: (userId: string) => void;
  reports: ReportTicket[];
  submitReport: (reportData: {
    reportedUserId?: string;
    reportedUserName?: string;
    reportedProductId?: string;
    reportedProductTitle?: string;
    reason: string;
    message: string;
  }) => void;
  reportModalData: {
    isOpen: boolean;
    productId?: string;
    productTitle?: string;
    userId?: string;
    userName?: string;
  } | null;
  openReportModal: (data: {
    productId?: string;
    productTitle?: string;
    userId?: string;
    userName?: string;
  }) => void;
  closeReportModal: () => void;
  adminResolveReport: (reportId: string, action: 'resolved' | 'dismissed') => void;

  // Seller Rating & Reviews
  reviews: UserReview[];
  selectedSellerForReview: {
    sellerId: string;
    sellerName: string;
    sellerAvatar: string;
    sellerRating: number;
    sellerDealsCount: number;
    sellerVerified: boolean;
    productId?: string;
    productTitle?: string;
  } | null;
  isReviewModalOpen: boolean;
  setIsReviewModalOpen: (open: boolean) => void;
  openReviewModal: (sellerData: {
    sellerId: string;
    sellerName: string;
    sellerAvatar: string;
    sellerRating: number;
    sellerDealsCount: number;
    sellerVerified: boolean;
    productId?: string;
    productTitle?: string;
  }) => void;
  closeReviewModal: () => void;
  submitUserReview: (reviewData: {
    sellerId: string;
    sellerName?: string;
    rating: number;
    comment: string;
    tags?: string[];
    productId?: string;
    productTitle?: string;
  }) => void;
  getSellerReviews: (sellerId: string) => UserReview[];

  // AdMob & Monetization
  adMobConfig: AdMobConfig;
  updateAdMobConfig: (newConfig: Partial<AdMobConfig>) => void;
  adStats: AdStats;
  sponsoredAds: SponsoredAdItem[];
  addSponsoredAd: (ad: Omit<SponsoredAdItem, 'id' | 'createdAt' | 'clicksCount' | 'impressionsCount'>) => void;
  updateSponsoredAd: (id: string, updates: Partial<SponsoredAdItem>) => void;
  toggleSponsoredAdStatus: (id: string) => void;
  deleteSponsoredAd: (id: string) => void;
  resetAdStats: () => void;
  recordAdImpression: (format?: AdFormat, adId?: string) => void;
  recordAdClick: (format?: AdFormat, adId?: string) => void;
  isInterstitialOpen: boolean;
  showInterstitialAd: () => void;
  closeInterstitialAd: () => void;
  isRewardedOpen: boolean;
  showRewardedAd: (onReward?: () => void) => void;
  closeRewardedAd: () => void;
  rewardCredits: number;
  useRewardCreditForListing: (productId: string) => boolean;

  // Toast
  toasts: ToastState[];
  showToast: (message: string, type?: 'success' | 'info' | 'warning') => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [language, setLanguageState] = useState<Language>(() => {
    const saved = localStorage.getItem('hatch_lang');
    return (saved === 'en' || saved === 'ar') ? saved : 'ar';
  });

  const [activeTab, setActiveTab] = useState<ActiveTab>('swipe');
  
  // Available users list (persisted all users including custom + mock users with status)
  const [availableUsers, setAvailableUsers] = useState<User[]>(() => {
    const savedAll = localStorage.getItem('hatch_all_users');
    if (savedAll) {
      try {
        const parsed = JSON.parse(savedAll);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      } catch (e) {
        console.error('Error parsing all users from localStorage', e);
      }
    }
    const saved = localStorage.getItem('hatch_custom_users');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        return [...parsed, ...mockUsers];
      } catch (e) {
        console.error('Error parsing custom users from localStorage', e);
      }
    }
    return mockUsers;
  });

  useEffect(() => {
    localStorage.setItem('hatch_all_users', JSON.stringify(availableUsers));
  }, [availableUsers]);

  const [currentUser, setCurrentUser] = useState<User>(() => {
    const saved = localStorage.getItem('hatch_current_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing currentUser from localStorage', e);
      }
    }
    return mockUsers[0];
  });
  
  // Persisted or default products
  const [products, setProducts] = useState<Product[]>(() => {
    const saved = localStorage.getItem('hatch_products');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing products from localStorage', e);
      }
    }
    return initialProducts;
  });

  // Swipe history
  const [swipedIds, setSwipedIds] = useState<string[]>(() => {
    const saved = localStorage.getItem('hatch_swiped');
    return saved ? JSON.parse(saved) : [];
  });

  const [lastSwipedId, setLastSwipedId] = useState<string | null>(null);

  // Favorites
  const [favorites, setFavorites] = useState<string[]>(() => {
    const saved = localStorage.getItem('hatch_favs');
    return saved ? JSON.parse(saved) : ['prod_1', 'prod_4'];
  });

  // Conversations & Offers
  const [conversations, setConversations] = useState<Conversation[]>(() => {
    const saved = localStorage.getItem('hatch_conversations');
    return saved ? JSON.parse(saved) : initialConversations;
  });
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);

  // Notifications
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const saved = localStorage.getItem('hatch_notifications');
    return saved ? JSON.parse(saved) : initialNotifications;
  });

  // Search & Filters
  const [selectedCity, setSelectedCity] = useState<SudaneseCity | 'all'>('all');
  const [selectedCategory, setSelectedCategory] = useState<CategoryId | 'all'>('all');
  const [selectedCondition, setSelectedCondition] = useState<Condition | 'all'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [onlyNegotiable, setOnlyNegotiable] = useState(false);
  const [onlyUrgent, setOnlyUrgent] = useState(false);
  const [onlyBankak, setOnlyBankak] = useState(false);
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 25000000]);
  const [isPriceUnlimited, setIsPriceUnlimited] = useState<boolean>(false);

  // Modals
  const [selectedProductForDetail, setSelectedProductForDetail] = useState<Product | null>(null);
  const [selectedProductForOffer, setSelectedProductForOffer] = useState<Product | null>(null);
  const [rawIsSellModalOpen, setRawIsSellModalOpen] = useState(false);
  const isSellModalOpen = rawIsSellModalOpen;
  const setIsSellModalOpen = (open: boolean) => {
    if (open && currentUser.status === 'suspended') {
      const untilDate = currentUser.suspendedUntil 
        ? new Date(currentUser.suspendedUntil).toLocaleDateString(language === 'ar' ? 'ar-SD' : 'en-US', { year: 'numeric', month: 'long', day: 'numeric' })
        : '';
      showToast(
        language === 'ar'
          ? `⚠️ حسابك موقوف مؤقتاً حتى ${untilDate}. سبب الإيقاف: ${currentUser.suspensionReason || 'مخالفة الشروط'}. لا يمكنك نشر إعلانات جديدة.`
          : `⚠️ Your account is suspended until ${untilDate}. Reason: ${currentUser.suspensionReason || 'Violation'}. Cannot post new listings.`,
        'warning'
      );
      return;
    }
    setRawIsSellModalOpen(open);
  };
  const [isSafetyModalOpen, setIsSafetyModalOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [isApkModalOpen, setIsApkModalOpen] = useState(false);
  const [isCreateAccountOpen, setIsCreateAccountOpen] = useState<boolean>(() => {
    const saved = localStorage.getItem('hatch_seen_create_account');
    return !saved;
  });
  const [isEditProfileOpen, setIsEditProfileOpen] = useState(false);

  // Seller Rating & Reviews
  const [reviews, setReviews] = useState<UserReview[]>(() => {
    const saved = localStorage.getItem('hatch_user_reviews');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing reviews from localStorage', e);
      }
    }
    return initialReviews;
  });

  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [selectedSellerForReview, setSelectedSellerForReview] = useState<{
    sellerId: string;
    sellerName: string;
    sellerAvatar: string;
    sellerRating: number;
    sellerDealsCount: number;
    sellerVerified: boolean;
    productId?: string;
    productTitle?: string;
  } | null>(null);

  // Persist reviews
  useEffect(() => {
    localStorage.setItem('hatch_user_reviews', JSON.stringify(reviews));
  }, [reviews]);

  const openReviewModal = (sellerData: {
    sellerId: string;
    sellerName: string;
    sellerAvatar: string;
    sellerRating: number;
    sellerDealsCount: number;
    sellerVerified: boolean;
    productId?: string;
    productTitle?: string;
  }) => {
    setSelectedSellerForReview(sellerData);
    setIsReviewModalOpen(true);
  };

  const closeReviewModal = () => {
    setIsReviewModalOpen(false);
    setSelectedSellerForReview(null);
  };

  const getSellerReviews = (sellerId: string) => {
    return reviews.filter((r) => r.sellerId === sellerId);
  };

  const submitUserReview = (reviewData: {
    sellerId: string;
    sellerName?: string;
    rating: number;
    comment: string;
    tags?: string[];
    productId?: string;
    productTitle?: string;
  }) => {
    const newReview: UserReview = {
      id: `rev_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      sellerId: reviewData.sellerId,
      sellerName: reviewData.sellerName,
      reviewerId: currentUser.id,
      reviewerName: currentUser.name,
      reviewerAvatar: currentUser.avatar,
      rating: Math.max(1, Math.min(5, reviewData.rating)),
      comment: reviewData.comment.trim(),
      tags: reviewData.tags || [],
      productId: reviewData.productId,
      productTitle: reviewData.productTitle,
      createdAt: language === 'ar' ? 'الآن' : 'Just now',
    };

    const updatedReviews = [newReview, ...reviews];
    setReviews(updatedReviews);
    localStorage.setItem('hatch_user_reviews', JSON.stringify(updatedReviews));

    // Calculate updated rating for the seller based on all reviews
    const allSellerReviews = updatedReviews.filter((r) => r.sellerId === reviewData.sellerId);
    const totalRatingSum = allSellerReviews.reduce((sum, r) => sum + r.rating, 0);
    const newAvgRating = Math.round((totalRatingSum / allSellerReviews.length) * 10) / 10;

    // Update available users
    setAvailableUsers((prev) =>
      prev.map((u) => (u.id === reviewData.sellerId ? { ...u, rating: newAvgRating } : u))
    );

    // Update products of this seller
    setProducts((prev) => {
      const updated = prev.map((p) =>
        p.sellerId === reviewData.sellerId ? { ...p, sellerRating: newAvgRating } : p
      );
      localStorage.setItem('hatch_products', JSON.stringify(updated));
      return updated;
    });

    // Update currentUser if current user is the seller
    if (currentUser.id === reviewData.sellerId) {
      setCurrentUser((prev) => ({ ...prev, rating: newAvgRating }));
    }

    // Update selectedProductForDetail if currently viewing
    if (selectedProductForDetail && selectedProductForDetail.sellerId === reviewData.sellerId) {
      setSelectedProductForDetail({
        ...selectedProductForDetail,
        sellerRating: newAvgRating,
      });
    }

    // Update selectedSellerForReview if modal is still open
    if (selectedSellerForReview && selectedSellerForReview.sellerId === reviewData.sellerId) {
      setSelectedSellerForReview({
        ...selectedSellerForReview,
        sellerRating: newAvgRating,
      });
    }

    // Confetti celebration
    try {
      confetti({
        particleCount: 45,
        spread: 60,
        origin: { y: 0.65 },
        colors: ['#F59E0B', '#FBBF24', '#10B981'],
      });
    } catch (_) {}

    // Add in-app notification to the seller
    const notifItem: NotificationItem = {
      id: `notif_${Date.now()}`,
      type: 'verification',
      titleAr: `تقييم جديد (${reviewData.rating} ★)`,
      titleEn: `New Rating (${reviewData.rating} ★)`,
      bodyAr: `أضاف ${currentUser.name} تقييماً لحسابك (${reviewData.rating} نجوم): "${reviewData.comment.slice(0, 35)}..."`,
      bodyEn: `${currentUser.name} added a ${reviewData.rating}-star review: "${reviewData.comment.slice(0, 35)}..."`,
      time: language === 'ar' ? 'الآن' : 'Just now',
      read: false,
      relatedProductId: reviewData.productId,
    };
    setNotifications((prev) => [notifItem, ...prev]);

    showToast(
      language === 'ar'
        ? `تم نشر تقييمك بنجاح (${newAvgRating} ★)! شكراً لمساهمتك في موثوقية هتش.`
        : `Review submitted (${newAvgRating} ★)! Thank you for supporting trust on HATCH.`,
      'success'
    );
  };

  // AdMob & Monetization Engine
  const [adMobConfig, setAdMobConfig] = useState<AdMobConfig>(() => {
    const saved = localStorage.getItem('hatch_admob_config');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing admob config', e);
      }
    }
    return defaultAdMobConfig;
  });

  const [adStats, setAdStats] = useState<AdStats>(() => {
    const saved = localStorage.getItem('hatch_ad_stats');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing ad stats', e);
      }
    }
    return defaultAdStats;
  });

  const [sponsoredAds, setSponsoredAds] = useState<SponsoredAdItem[]>(() => {
    const saved = localStorage.getItem('hatch_sponsored_ads');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error('Error parsing sponsored ads from localStorage', e);
      }
    }
    return initialSponsoredAds;
  });

  const [rewardCredits, setRewardCredits] = useState<number>(() => {
    const saved = localStorage.getItem('hatch_reward_credits');
    return saved ? parseInt(saved, 10) || 0 : 2;
  });

  const [isInterstitialOpen, setIsInterstitialOpen] = useState(false);
  const [isRewardedOpen, setIsRewardedOpen] = useState(false);
  const [onRewardCallback, setOnRewardCallback] = useState<(() => void) | null>(null);

  // Persist AdMob config, stats, and ads
  useEffect(() => {
    localStorage.setItem('hatch_admob_config', JSON.stringify(adMobConfig));
  }, [adMobConfig]);

  useEffect(() => {
    localStorage.setItem('hatch_ad_stats', JSON.stringify(adStats));
  }, [adStats]);

  useEffect(() => {
    localStorage.setItem('hatch_sponsored_ads', JSON.stringify(sponsoredAds));
  }, [sponsoredAds]);

  useEffect(() => {
    localStorage.setItem('hatch_reward_credits', rewardCredits.toString());
  }, [rewardCredits]);

  const updateAdMobConfig = (newConfig: Partial<AdMobConfig>) => {
    setAdMobConfig((prev) => ({ ...prev, ...newConfig }));
    showToast(language === 'ar' ? 'تم حفظ وتحديث إعدادات شبكة AdMob بنجاح' : 'AdMob settings updated successfully', 'success');
  };

  const addSponsoredAd = (newAdData: Omit<SponsoredAdItem, 'id' | 'createdAt' | 'clicksCount' | 'impressionsCount'>) => {
    const newAd: SponsoredAdItem = {
      ...newAdData,
      id: `ad_${Date.now()}`,
      isActive: true,
      clicksCount: 0,
      impressionsCount: 0,
      createdAt: language === 'ar' ? 'الآن' : 'Just now',
    };
    setSponsoredAds((prev) => [newAd, ...prev]);
    showToast(
      language === 'ar'
        ? `تم إضافة الإعلان بنجاح (${newAd.advertiser}) 🚀`
        : `Ad campaign added (${newAd.advertiser}) 🚀`,
      'success'
    );
  };

  const updateSponsoredAd = (id: string, updates: Partial<SponsoredAdItem>) => {
    setSponsoredAds((prev) =>
      prev.map((ad) => (ad.id === id ? { ...ad, ...updates } : ad))
    );
    showToast(
      language === 'ar' ? 'تم تحديث بيانات الإعلان بنجاح' : 'Ad updated successfully',
      'success'
    );
  };

  const toggleSponsoredAdStatus = (id: string) => {
    setSponsoredAds((prev) =>
      prev.map((ad) => {
        if (ad.id === id) {
          const newStatus = !ad.isActive;
          showToast(
            language === 'ar'
              ? (newStatus ? `تم تفعيل ظهور الإعلان (${ad.advertiser}) ✅` : `تم إيقاف/إلغاء الإعلان (${ad.advertiser}) ⏸️`)
              : (newStatus ? `Ad activated (${ad.advertiser}) ✅` : `Ad paused (${ad.advertiser}) ⏸️`),
            newStatus ? 'success' : 'info'
          );
          return { ...ad, isActive: newStatus };
        }
        return ad;
      })
    );
  };

  const deleteSponsoredAd = (id: string) => {
    setSponsoredAds((prev) => prev.filter((ad) => ad.id !== id));
    showToast(
      language === 'ar' ? 'تم حذف الإعلان نهائياً من الشبكة' : 'Ad campaign removed',
      'warning'
    );
  };

  const resetAdStats = () => {
    setAdStats({
      impressions: 0,
      clicks: 0,
      rewardedCompleted: 0,
      estimatedEarningsUsd: 0,
      estimatedEarningsSdg: 0,
      lastUpdated: language === 'ar' ? 'الآن' : 'Just now',
    });
    setSponsoredAds((prev) =>
      prev.map((ad) => ({ ...ad, clicksCount: 0, impressionsCount: 0 }))
    );
    showToast(
      language === 'ar' ? 'تم تصفير عدادات المشاهدات والنقرات والأرباح بنجاح 🔄' : 'Ad statistics reset successfully 🔄',
      'info'
    );
  };

  const recordAdImpression = (format: AdFormat = 'banner', adId?: string) => {
    if (!adMobConfig.isEnabled) return;
    setAdStats((prev) => {
      const newImpressions = prev.impressions + 1;
      const addedRevenueUsd = adMobConfig.ecpmRate / 1000;
      const newUsd = Math.round((prev.estimatedEarningsUsd + addedRevenueUsd) * 10000) / 10000;
      const newSdg = Math.round(newUsd * adMobConfig.sdgRate);
      return {
        ...prev,
        impressions: newImpressions,
        estimatedEarningsUsd: newUsd,
        estimatedEarningsSdg: newSdg,
        lastUpdated: language === 'ar' ? 'الآن' : 'Just now',
      };
    });

    if (adId) {
      setSponsoredAds((prev) =>
        prev.map((ad) => (ad.id === adId ? { ...ad, impressionsCount: (ad.impressionsCount || 0) + 1 } : ad))
      );
    }
  };

  const recordAdClick = (format: AdFormat = 'banner', adId?: string) => {
    if (!adMobConfig.isEnabled) return;
    setAdStats((prev) => {
      const newClicks = prev.clicks + 1;
      const addedRevenueUsd = adMobConfig.cpcRate;
      const newUsd = Math.round((prev.estimatedEarningsUsd + addedRevenueUsd) * 100) / 100;
      const newSdg = Math.round(newUsd * adMobConfig.sdgRate);
      return {
        ...prev,
        clicks: newClicks,
        estimatedEarningsUsd: newUsd,
        estimatedEarningsSdg: newSdg,
        lastUpdated: language === 'ar' ? 'الآن' : 'Just now',
      };
    });

    if (adId) {
      setSponsoredAds((prev) =>
        prev.map((ad) => (ad.id === adId ? { ...ad, clicksCount: (ad.clicksCount || 0) + 1 } : ad))
      );
    }
  };

  const showInterstitialAd = () => {
    if (!adMobConfig.isEnabled) return;
    setIsInterstitialOpen(true);
    recordAdImpression('interstitial');
  };

  const closeInterstitialAd = () => {
    setIsInterstitialOpen(false);
  };

  const showRewardedAd = (onReward?: () => void) => {
    if (onReward) {
      setOnRewardCallback(() => onReward);
    } else {
      setOnRewardCallback(null);
    }
    setIsRewardedOpen(true);
    recordAdImpression('rewarded');
  };

  const closeRewardedAd = () => {
    setIsRewardedOpen(false);
    setOnRewardCallback(null);
  };

  const useRewardCreditForListing = (productId: string): boolean => {
    if (rewardCredits <= 0) return false;
    setRewardCredits((prev) => Math.max(0, prev - 1));
    // Boost listing: make it urgent and featured
    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, featured: true, urgentSale: true } : p))
    );
    showToast(
      language === 'ar'
        ? 'تم استخدام رصيد المكافأة وتمييز إعلانك كعاجل ومميز بنجاح! 🚀'
        : 'Reward credit used! Your ad is now featured and urgent! 🚀',
      'success'
    );
    return true;
  };

  // Toasts
  const [toasts, setToasts] = useState<ToastState[]>([]);

  // Keep dir and lang in sync on root html element
  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = language === 'ar' ? 'rtl' : 'ltr';
    localStorage.setItem('hatch_lang', language);
  }, [language]);

  // Persist products
  useEffect(() => {
    localStorage.setItem('hatch_products', JSON.stringify(products));
  }, [products]);

  // Persist swiped
  useEffect(() => {
    localStorage.setItem('hatch_swiped', JSON.stringify(swipedIds));
  }, [swipedIds]);

  // Persist favorites
  useEffect(() => {
    localStorage.setItem('hatch_favs', JSON.stringify(favorites));
  }, [favorites]);

  // Persist conversations
  useEffect(() => {
    localStorage.setItem('hatch_conversations', JSON.stringify(conversations));
  }, [conversations]);

  // Persist notifications
  useEffect(() => {
    localStorage.setItem('hatch_notifications', JSON.stringify(notifications));
  }, [notifications]);

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
  };

  const t = translations[language];

  const showToast = (message: string, type: 'success' | 'info' | 'warning' = 'info') => {
    const id = Date.now().toString() + Math.random().toString().slice(2, 5);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3800);
  };

  const switchUser = (userId: string) => {
    const user = availableUsers.find((u) => u.id === userId);
    if (user) {
      setCurrentUser(user);
      localStorage.setItem('hatch_current_user', JSON.stringify(user));
      showToast(
        language === 'ar' 
          ? `تم التبديل إلى حساب: ${user.name}` 
          : `Switched account to: ${user.name}`,
        'info'
      );
    }
  };

  const createNewAccount = (userData: {
    name: string;
    phone: string;
    city: SudaneseCity;
    avatar?: string;
  }) => {
    const defaultAvatars = [
      'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80',
      'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=256&q=80',
      'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=256&q=80',
      'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80',
      'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80',
    ];

    const chosenAvatar = userData.avatar || defaultAvatars[Math.floor(Math.random() * defaultAvatars.length)];

    const newUser: User = {
      id: 'user_' + Date.now(),
      name: userData.name.trim(),
      avatar: chosenAvatar,
      phone: userData.phone.trim(),
      city: userData.city,
      rating: 5.0,
      dealsCompleted: 0,
      reportsCount: 0,
      verifiedIdentity: false,
      joinedDate: new Date().toISOString().split('T')[0],
      role: 'user',
    };

    setAvailableUsers((prev) => {
      const next = [newUser, ...prev];
      const customOnly = next.filter((u) => !mockUsers.some((m) => m.id === u.id));
      localStorage.setItem('hatch_custom_users', JSON.stringify(customOnly));
      return next;
    });

    setCurrentUser(newUser);
    localStorage.setItem('hatch_current_user', JSON.stringify(newUser));
    localStorage.setItem('hatch_seen_create_account', 'true');
    setIsCreateAccountOpen(false);

    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.6 }
    });

    showToast(
      language === 'ar'
        ? `أهلاً بك يا ${newUser.name}! تم إنشاء حسابك في هتش بنجاح 🎉`
        : `Welcome ${newUser.name}! Your account on HATCH has been created 🎉`,
      'success'
    );
  };

  const updateCurrentUser = (userData: Partial<User>) => {
    setCurrentUser((prev) => {
      const updated: User = {
        ...prev,
        ...userData,
      };
      localStorage.setItem('hatch_current_user', JSON.stringify(updated));
      return updated;
    });

    setAvailableUsers((prev) => {
      const next = prev.map((u) => (u.id === currentUser.id ? { ...u, ...userData } : u));
      const customOnly = next.filter((u) => !mockUsers.some((m) => m.id === u.id));
      localStorage.setItem('hatch_custom_users', JSON.stringify(customOnly));
      return next;
    });

    setIsEditProfileOpen(false);

    showToast(
      language === 'ar'
        ? 'تم تحديث بيانات الملف الشخصي بنجاح ✨'
        : 'Profile details updated successfully ✨',
      'success'
    );
  };

  const toggleFavorite = (productId: string) => {
    setFavorites((prev) => {
      const exists = prev.includes(productId);
      if (exists) {
        showToast(language === 'ar' ? 'تمت إزالة المنتج من المفضلة' : 'Removed from favorites', 'info');
        return prev.filter((id) => id !== productId);
      } else {
        showToast(
          language === 'ar' ? 'تمت إضافة المنتج إلى المفضلة ❤️' : 'Added to favorites ❤️',
          'success'
        );
        return [...prev, productId];
      }
    });
  };

  const swipeRight = (productId: string, openOffer: boolean = false) => {
    setLastSwipedId(productId);
    setSwipedIds((prev) => (prev.includes(productId) ? prev : [...prev, productId]));

    if (openOffer) {
      const prod = products.find((p) => p.id === productId);
      if (prod) setSelectedProductForOffer(prod);
    }
  };

  const swipeLeft = (_productId?: string) => {
    setSwipedIds((prev) => {
      if (prev.length === 0) {
        showToast(
          language === 'ar' ? 'أنت في بداية القائمة (لا يوجد إعلان سابق)' : 'You are at the start (no previous item)',
          'info'
        );
        return prev;
      }
      const newSwiped = prev.slice(0, prev.length - 1);
      setLastSwipedId(newSwiped.length > 0 ? newSwiped[newSwiped.length - 1] : null);
      showToast(
        language === 'ar' ? 'تم الرجوع للإعلان السابق ⬅️' : 'Returned to previous card ⬅️',
        'info'
      );
      return newSwiped;
    });
  };

  const superLike = (productId: string) => {
    confetti({
      particleCount: 50,
      spread: 60,
      origin: { y: 0.7 }
    });
    setLastSwipedId(productId);
    setSwipedIds((prev) => (prev.includes(productId) ? prev : [...prev, productId]));

    const prod = products.find((p) => p.id === productId);
    if (prod) {
      setSelectedProductForOffer(prod);
      showToast(
        language === 'ar' ? 'عرض مباشر فوري ⚡! قدم سعرك للبائع' : 'Super Like ⚡! Name your price',
        'success'
      );
    }
  };

  const undoSwipe = () => {
    swipeLeft();
  };

  const resetDeck = () => {
    setSwipedIds([]);
    setLastSwipedId(null);
    showToast(language === 'ar' ? 'تمت إعادة ضبط بطاقات التصفح' : 'Discovery deck reloaded', 'success');
  };

  const resetFilters = () => {
    setSelectedCity('all');
    setSelectedCategory('all');
    setSelectedCondition('all');
    setSearchQuery('');
    setOnlyNegotiable(false);
    setOnlyUrgent(false);
    setOnlyBankak(false);
    setPriceRange([0, 25000000]);
    setIsPriceUnlimited(false);
    showToast(t.clearFilters, 'info');
  };

  const addProduct = (
    productData: Omit<Product, 'id' | 'createdAt' | 'viewsCount' | 'likesCount' | 'status'>
  ) => {
    const newId = 'prod_' + Date.now();
    const newProduct: Product = {
      ...productData,
      id: newId,
      viewsCount: 1,
      likesCount: 0,
      status: 'active',
      createdAt: language === 'ar' ? 'الآن' : 'Just now',
    };

    setProducts((prev) => [newProduct, ...prev]);
    // Also trigger celebratory confetti
    confetti({
      particleCount: 90,
      spread: 70,
      origin: { y: 0.6 }
    });

    showToast(t.adPublishedSuccess, 'success');
    setIsSellModalOpen(false);
    setActiveTab('browse');
  };

  const updateProduct = (productId: string, updatedData: Partial<Product>) => {
    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, ...updatedData } : p))
    );
    showToast(
      language === 'ar' ? 'تم تحديث بيانات الإعلان بنجاح ✨' : 'Listing updated successfully ✨',
      'success'
    );
  };

  const markProductAsSold = (productId: string) => {
    const prod = products.find((p) => p.id === productId);
    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, status: 'sold' as const } : p))
    );

    // If the product belongs to current user, count it as a successful trusted sale
    if (prod && prod.sellerId === currentUser.id) {
      setCurrentUser((prev) => {
        const nextDeals = (prev.dealsCompleted || 0) + 1;
        const reports = prev.reportsCount || 0;
        const nowVerified = isUserVerified(nextDeals, reports);
        
        if (nowVerified && !prev.verifiedIdentity) {
          setTimeout(() => {
            showToast(
              language === 'ar' 
                ? 'لقد تم توثيق حسابك بعد عشره صفقات ناجحه' 
                : 'Your account has been verified after ten successful deals',
              'success'
            );
          }, 1200);

          setNotifications((prevNotifs) => [
            {
              id: 'notif_verified_' + Date.now(),
              type: 'verification',
              titleAr: 'توثيق الحساب',
              titleEn: 'Account Verification',
              bodyAr: 'لقد تم توثيق حسابك بعد عشره صفقات ناجحه',
              bodyEn: 'Your account has been verified after ten successful deals',
              time: 'الآن',
              read: false,
            },
            ...prevNotifs
          ]);
        }

        return {
          ...prev,
          dealsCompleted: nextDeals,
          verifiedIdentity: nowVerified,
        };
      });
    }

    confetti({
      particleCount: 80,
      spread: 60,
      origin: { y: 0.5 }
    });
    showToast(
      language === 'ar' 
        ? 'تهانينا على البيع! تم نقل الإعلان إلى قسم الإعلانات المباعة بنجاح 🎉' 
        : 'Congratulations on your sale! Ad moved to Sold Listings 🎉',
      'success'
    );
  };

  const deleteProduct = (productId: string) => {
    setProducts((prev) => prev.filter((p) => p.id !== productId));
    setFavorites((prev) => prev.filter((id) => id !== productId));
    showToast(
      language === 'ar' ? 'تم حذف الإعلان من القائمة' : 'Listing removed',
      'info'
    );
  };

  const startOrOpenConversation = (productId: string) => {
    const prod = products.find((p) => p.id === productId);
    if (!prod) return;

    // Check if conversation already exists
    const existing = conversations.find(
      (c) => c.productId === productId && (c.participantId === prod.sellerId || c.participantId === currentUser.id)
    );

    if (existing) {
      setActiveConversationId(existing.id);
      setActiveTab('messages');
      if (selectedProductForDetail) setSelectedProductForDetail(null);
      return;
    }

    // Create new conversation
    const newConvId = 'conv_' + Date.now();
    const newConv: Conversation = {
      id: newConvId,
      productId: prod.id,
      productTitle: language === 'ar' ? prod.titleAr : prod.titleEn,
      productPrice: prod.price,
      productImage: prod.images[0],
      participantId: prod.sellerId,
      participantName: prod.sellerName,
      participantAvatar: prod.sellerAvatar,
      lastMessage: language === 'ar' ? 'بدء محادثة استفسار جديدة' : 'Started inquiry conversation',
      lastMessageTime: language === 'ar' ? 'الآن' : 'Now',
      unreadCount: 0,
      messages: [
        {
          id: 'msg_' + Date.now(),
          senderId: currentUser.id,
          text: language === 'ar' ? `السلام عليكم، أنا مهتم بـ: ${prod.titleAr}` : `Hello, I'm interested in: ${prod.titleEn}`,
          timestamp: language === 'ar' ? 'الآن' : 'Now',
          status: 'delivered' as const,
        }
      ]
    };

    setConversations((prev) => [newConv, ...prev]);
    setActiveConversationId(newConvId);
    setActiveTab('messages');
    if (selectedProductForDetail) setSelectedProductForDetail(null);
  };

  const sendMessage = (
    conversationId: string, 
    text?: string, 
    audioData?: { audioUrl: string; audioDuration: number },
    imageData?: { imageUrl: string; imageTag?: string }
  ) => {
    const trimmedText = text?.trim() || '';
    if (!trimmedText && !audioData && !imageData) return;

    const defaultAudioLabel = language === 'ar' ? '🎤 تسجيل صوتي' : '🎤 Voice note';
    const tagPrefix = imageData?.imageTag ? `[${imageData.imageTag}] ` : '';
    const defaultImageLabel = language === 'ar' ? `📷 ${tagPrefix}صورة للمعاينة` : `📷 ${tagPrefix}Photo for inspection`;
    const messageContent = trimmedText || (imageData ? defaultImageLabel : defaultAudioLabel);
    const newMsgId = 'msg_' + Date.now();

    setConversations((prev) =>
      prev.map((c) => {
        if (c.id === conversationId) {
          const newMsg = {
            id: newMsgId,
            senderId: currentUser.id,
            text: messageContent,
            timestamp: new Date().toLocaleTimeString(language === 'ar' ? 'ar-SD' : 'en-US', {
              hour: '2-digit',
              minute: '2-digit',
            }),
            status: 'sent' as const,
            audioUrl: audioData?.audioUrl,
            audioDuration: audioData?.audioDuration,
            imageUrl: imageData?.imageUrl,
            imageTag: imageData?.imageTag,
          };
          return {
            ...c,
            lastMessage: audioData 
              ? defaultAudioLabel 
              : imageData 
              ? (trimmedText ? `📷 ${tagPrefix}${trimmedText}` : defaultImageLabel) 
              : messageContent,
            lastMessageTime: language === 'ar' ? 'الآن' : 'Just now',
            messages: [...c.messages, newMsg],
          };
        }
        return c;
      })
    );

    // Step 1: Simulate network delivery after 600ms (turns to double check mark)
    setTimeout(() => {
      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === conversationId) {
            return {
              ...c,
              messages: c.messages.map((m) =>
                m.id === newMsgId && m.status === 'sent'
                  ? { ...m, status: 'delivered' as const }
                  : m
              ),
            };
          }
          return c;
        })
      );
    }, 600);

    // Step 2: Simulate message read receipt after 1400ms (turns to double blue check marks)
    setTimeout(() => {
      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === conversationId) {
            return {
              ...c,
              messages: c.messages.map((m) =>
                m.id === newMsgId && (m.status === 'sent' || m.status === 'delivered')
                  ? { ...m, status: 'read' as const }
                  : m
              ),
            };
          }
          return c;
        })
      );
    }, 1400);

    // Step 3: Simulated seller response after 2.2 seconds for an ultra-alive marketplace experience!
    setTimeout(() => {
      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === conversationId) {
            const repliesAr = imageData
              ? [
                  'ما شاء الله! فحصت صورة المنتج، النظافة والحالة ممتازة ومطابقة للوصف تماماً. متى يناسبك نلتقي؟',
                  'تسلم يا غالي على إرسال الصورة الواضحة، الفحص تمام التمام، جاهز للتسليم والتحويل عبر بنكك فوراً.',
                  'تمام جداً، الصورة أثبتت جودة الجهاز، اتفقنا ومستعد للقاء والمعاينة في أي وقت اليوم.'
                ]
              : audioData
              ? [
                  'سمعت تسجيلك الصوتي يا غالي، تمام جداً وموافق بإذن الله، نلتقي للمعاينة اليوم.',
                  'تسلم يا طيب، صوتك واضح جداً، متفقين ونتواصل هاتفياً للتسليم.',
                  'أهلاً بك، سمعت الرسالة الصوتية، الجهاز متاح ونلتقي في المكان المتفق عليه اليوم.'
                ]
              : [
                  'أهلاً بك يا غالي! نعم المنتج متاح وجاهز للمعاينة الفورية.',
                  'تحياتي، نعم أقبل التحويل عبر بنكك مباشرة بعد التأكد من الجهاز.',
                  'مرحبا، أين موقعك بالضبط؟ يمكننا اللقاء في مكان عام مناسب اليوم.',
                  'تمام جداً، اتفقنا إن شاء الله، تواصل معي هاتفياً للتنسيق.'
                ];
            const repliesEn = imageData
              ? [
                  'Awesome! Inspected the product photo, the condition looks great and pristine. When can we meet?',
                  'Thanks for sending the clear photo! Everything checks out, agreed on the meetup details.',
                  'Great photos! Everything matches the description, ready to finalize the deal.'
                ]
              : audioData
              ? [
                  'I listened to your voice message! Sounds great, let us meet up today.',
                  'Thank you! Audio received crystal clear, agreed on the details.',
                  'Hello! Got your voice note, item is ready for inspection whenever you want.'
                ]
              : [
                  'Hello friend! Yes the item is fully available for inspection.',
                  'Greetings! Yes, instant Bankak transfer is accepted right after checking the item.',
                  'Where are you located? We can meet up at a secure public spot today.',
                  'Sounds good! Looking forward to closing the deal.'
                ];
            const replyList = language === 'ar' ? repliesAr : repliesEn;
            const randomReply = replyList[Math.floor(Math.random() * replyList.length)];
            
            return {
              ...c,
              lastMessage: randomReply,
              lastMessageTime: language === 'ar' ? 'الآن' : 'Just now',
              messages: [
                ...c.messages.map((m) =>
                  m.senderId === currentUser.id ? { ...m, status: 'read' as const } : m
                ),
                {
                  id: 'msg_seller_' + Date.now(),
                  senderId: c.participantId,
                  text: randomReply,
                  timestamp: new Date().toLocaleTimeString(language === 'ar' ? 'ar-SD' : 'en-US', {
                    hour: '2-digit',
                    minute: '2-digit',
                  }),
                }
              ]
            };
          }
          return c;
        })
      );
    }, 2200);
  };

  const submitOffer = (productId: string, amount: number, note?: string) => {
    if (currentUser.status === 'suspended') {
      const untilDate = currentUser.suspendedUntil 
        ? new Date(currentUser.suspendedUntil).toLocaleDateString(language === 'ar' ? 'ar-SD' : 'en-US', { year: 'numeric', month: 'long', day: 'numeric' })
        : '';
      showToast(
        language === 'ar'
          ? `⚠️ حسابك موقوف مؤقتاً حتى ${untilDate}. سبب الإيقاف: ${currentUser.suspensionReason || 'مخالفة الشروط'}. لا يمكن تقديم عروض شراء.`
          : `⚠️ Your account is suspended until ${untilDate}. Reason: ${currentUser.suspensionReason || 'Violation'}. You cannot submit offers.`,
        'warning'
      );
      setSelectedProductForOffer(null);
      return;
    }

    const prod = products.find((p) => p.id === productId);
    if (!prod) return;

    const offerId = 'offer_' + Date.now();
    const newOffer = {
      id: offerId,
      productId: prod.id,
      productTitle: language === 'ar' ? prod.titleAr : prod.titleEn,
      productImage: prod.images[0],
      sellerId: prod.sellerId,
      buyerId: currentUser.id,
      buyerName: currentUser.name,
      originalPrice: prod.price,
      offeredPrice: amount,
      status: 'pending' as const,
      createdAt: language === 'ar' ? 'الآن' : 'Now',
    };

    // Find or create conversation
    setConversations((prev) => {
      const existingIdx = prev.findIndex(
        (c) => c.productId === productId && (c.participantId === prod.sellerId || c.participantId === currentUser.id)
      );

      const offerMsg = {
        id: 'msg_offer_' + Date.now(),
        senderId: currentUser.id,
        text: note?.trim() 
          ? `${language === 'ar' ? 'تقديم عرض شراء:' : 'Offer placed:'} ${amount.toLocaleString()} ${t.currency}. (${note})`
          : `${language === 'ar' ? 'تقديم عرض شراء:' : 'Offer placed:'} ${amount.toLocaleString()} ${t.currency}`,
        timestamp: language === 'ar' ? 'الآن' : 'Now',
        status: 'delivered' as const,
        isOfferUpdate: true,
        offerAmount: amount,
      };

      if (existingIdx >= 0) {
        const updated = [...prev];
        updated[existingIdx] = {
          ...updated[existingIdx],
          activeOffer: newOffer,
          lastMessage: offerMsg.text,
          lastMessageTime: language === 'ar' ? 'الآن' : 'Now',
          messages: [...updated[existingIdx].messages, offerMsg],
        };
        return updated;
      } else {
        const newConv: Conversation = {
          id: 'conv_' + Date.now(),
          productId: prod.id,
          productTitle: language === 'ar' ? prod.titleAr : prod.titleEn,
          productPrice: prod.price,
          productImage: prod.images[0],
          participantId: prod.sellerId,
          participantName: prod.sellerName,
          participantAvatar: prod.sellerAvatar,
          lastMessage: offerMsg.text,
          lastMessageTime: language === 'ar' ? 'الآن' : 'Now',
          unreadCount: 0,
          activeOffer: newOffer,
          messages: [offerMsg],
        };
        return [newConv, ...prev];
      }
    });

    confetti({
      particleCount: 40,
      spread: 50,
    });

    showToast(t.offerSentSuccess, 'success');
    setSelectedProductForOffer(null);
  };

  const respondToOffer = (
    conversationId: string, 
    action: 'accept' | 'decline' | 'counter', 
    counterAmount?: number
  ) => {
    setConversations((prev) =>
      prev.map((c) => {
        if (c.id === conversationId && c.activeOffer) {
          let newStatus = c.activeOffer.status;
          let systemText = '';

          if (action === 'accept') {
            newStatus = 'accepted';
            systemText = language === 'ar' 
              ? `🎉 تم قبول العرض بمبلغ: ${c.activeOffer.offeredPrice.toLocaleString()} ${t.currency}!` 
              : `🎉 Offer of ${c.activeOffer.offeredPrice.toLocaleString()} ${t.currency} has been accepted!`;
            
            confetti({ particleCount: 70, spread: 60 });
            showToast(systemText, 'success');
          } else if (action === 'decline') {
            newStatus = 'declined';
            systemText = language === 'ar' ? 'تم رفض العرض من قبل البائع.' : 'The offer was declined.';
            showToast(systemText, 'warning');
          } else if (action === 'counter' && counterAmount) {
            newStatus = 'countered';
            systemText = language === 'ar'
              ? `قام البائع بتقديم عرض مضاد: ${counterAmount.toLocaleString()} ${t.currency}`
              : `Seller sent a counter offer: ${counterAmount.toLocaleString()} ${t.currency}`;
            showToast(systemText, 'info');
          }

          return {
            ...c,
            activeOffer: {
              ...c.activeOffer,
              status: newStatus,
              counterPrice: counterAmount || c.activeOffer.counterPrice,
            },
            lastMessage: systemText,
            messages: [
              ...c.messages,
              {
                id: 'msg_sys_' + Date.now(),
                senderId: currentUser.id,
                text: systemText,
                timestamp: language === 'ar' ? 'الآن' : 'Now',
                isOfferUpdate: true,
                offerAmount: counterAmount || c.activeOffer.offeredPrice,
              }
            ]
          };
        }
        return c;
      })
    );
  };

  const markNotificationsAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const broadcastNotification = (notifData: {
    title: string;
    body: string;
    type: 'offer' | 'alert' | 'update' | 'system';
    relatedProductId?: string;
  }) => {
    const newNotif: NotificationItem = {
      id: 'broadcast_' + Date.now(),
      type: notifData.type,
      titleAr: notifData.title,
      titleEn: notifData.title,
      bodyAr: notifData.body,
      bodyEn: notifData.body,
      time: language === 'ar' ? 'الآن' : 'Just now',
      read: false,
      relatedProductId: notifData.relatedProductId,
    };

    setNotifications((prev) => [newNotif, ...prev]);

    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.5 }
    });

    showToast(
      language === 'ar'
        ? `تم إرسال الإشعار لجميع المستخدمين بنجاح 📢 (${notifData.title})`
        : `Notification broadcasted to all users 📢 (${notifData.title})`,
      'success'
    );
  };

  const flagProduct = (productId: string, reason: string) => {
    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, status: 'flagged' } : p))
    );
    showToast(
      language === 'ar' ? 'شكراً لإبلاغك. سيتم مراجعة الإعلان من فريق الأمان.' : 'Thank you. Safety team will review the listing.',
      'info'
    );
  };

  const adminApproveProduct = (productId: string) => {
    setProducts((prev) =>
      prev.map((p) => (p.id === productId ? { ...p, status: 'active' } : p))
    );
    showToast(
      language === 'ar' ? 'تم اعتماد الإعلان ونشره في السوق' : 'Listing approved and restored',
      'success'
    );
  };

  const adminDeleteProduct = (productId: string) => {
    setProducts((prev) => prev.filter((p) => p.id !== productId));
    showToast(
      language === 'ar' ? 'تم حذف الإعلان نهائياً' : 'Listing permanently removed',
      'warning'
    );
  };

  // Reports state & HATCH administration handling
  const [reports, setReports] = useState<ReportTicket[]>(() => {
    const saved = localStorage.getItem('hatch_reports');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        /* ignore */
      }
    }
    return initialReports;
  });

  useEffect(() => {
    localStorage.setItem('hatch_reports', JSON.stringify(reports));
  }, [reports]);

  const [reportModalData, setReportModalData] = useState<{
    isOpen: boolean;
    productId?: string;
    productTitle?: string;
    userId?: string;
    userName?: string;
  } | null>(null);

  const openReportModal = (data: {
    productId?: string;
    productTitle?: string;
    userId?: string;
    userName?: string;
  }) => {
    setReportModalData({
      isOpen: true,
      ...data,
    });
  };

  const closeReportModal = () => {
    setReportModalData(null);
  };

  const submitReport = (reportData: {
    reportedUserId?: string;
    reportedUserName?: string;
    reportedProductId?: string;
    reportedProductTitle?: string;
    reason: string;
    message: string;
  }) => {
    const newReport: ReportTicket = {
      id: 'rep_' + Date.now(),
      reporterId: currentUser.id,
      reporterName: currentUser.name,
      reportedUserId: reportData.reportedUserId,
      reportedUserName: reportData.reportedUserName,
      reportedProductId: reportData.reportedProductId,
      reportedProductTitle: reportData.reportedProductTitle,
      reason: reportData.reason,
      message: reportData.message,
      createdAt: language === 'ar' ? 'الآن' : 'Just now',
      status: 'pending',
    };

    setReports((prev) => [newReport, ...prev]);

    // If product is reported, mark it as flagged for administration
    if (reportData.reportedProductId) {
      setProducts((prev) =>
        prev.map((p) =>
          p.id === reportData.reportedProductId
            ? { ...p, status: 'flagged' }
            : p
        )
      );
    }

    // If user is reported, increase their reportsCount so it directly impacts verification
    if (reportData.reportedUserId) {
      setProducts((prev) =>
        prev.map((p) =>
          p.sellerId === reportData.reportedUserId
            ? { ...p, sellerReportsCount: (p.sellerReportsCount || 0) + 1, sellerVerified: false }
            : p
        )
      );
    }

    // Add a system notification about report dispatched to HATCH administration
    const adminNotif: NotificationItem = {
      id: 'notif_rep_' + Date.now(),
      type: 'system',
      titleAr: 'تم إرسال البلاغ لإدارة هتش',
      titleEn: 'Report Dispatched to HATCH Admins',
      bodyAr: `تم إرسال رسالتك وتفاصيل البلاغ بخصوص "${reportData.reportedProductTitle || reportData.reportedUserName || 'المخالفة'}" إلى إدارة هتش لمراجعتها واتخاذ الإجراء اللازم.`,
      bodyEn: `Your message regarding "${reportData.reportedProductTitle || reportData.reportedUserName || 'Violation'}" was dispatched to HATCH administration for review.`,
      time: language === 'ar' ? 'الآن' : 'Just now',
      read: false,
    };
    setNotifications((prev) => [adminNotif, ...prev]);

    showToast(t.reportSentToast, 'success');
    closeReportModal();
  };

  const adminResolveReport = (reportId: string, action: 'resolved' | 'dismissed') => {
    setReports((prev) =>
      prev.map((r) =>
        r.id === reportId
          ? { ...r, status: action === 'resolved' ? 'resolved' : 'reviewed' }
          : r
      )
    );
    showToast(
      language === 'ar' ? 'تم تحديث حالة البلاغ في لوحة إدارة هتش' : 'Report status updated in HATCH admin',
      'info'
    );
  };

  const adminDeleteUser = (userId: string) => {
    const target = availableUsers.find((u) => u.id === userId);
    const targetName = target?.name || (language === 'ar' ? 'المستخدم' : 'User');

    // 1. Remove user from available users
    const updatedUsers = availableUsers.filter((u) => u.id !== userId);
    setAvailableUsers(updatedUsers);

    // 2. If deleted user was active currentUser, switch to next available user
    if (currentUser.id === userId) {
      const fallback = updatedUsers[0] || mockUsers[0];
      setCurrentUser(fallback);
      localStorage.setItem('hatch_current_user', JSON.stringify(fallback));
    }

    // 3. Remove all listings published by this user
    setProducts((prev) => prev.filter((p) => p.sellerId !== userId));

    // 4. Remove conversations involving this user
    setConversations((prev) => prev.filter((c) => c.participantId !== userId));

    showToast(
      language === 'ar'
        ? `🗑️ تم حذف حساب (${targetName}) وجميع إعلاناته ومحادثاته نهائياً من منصة هتش.`
        : `🗑️ User account (${targetName}) and all listings permanently removed from HATCH.`,
      'warning'
    );
  };

  const adminSuspendUser = (userId: string, days: number, reason: string) => {
    const target = availableUsers.find((u) => u.id === userId);
    if (!target) return;

    const suspendedAt = new Date().toISOString();
    const suspendedUntilDate = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
    const suspendedUntil = suspendedUntilDate.toISOString();

    const formattedDateAr = suspendedUntilDate.toLocaleDateString('ar-SD', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
    const formattedDateEn = suspendedUntilDate.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });

    const updatedUser: User = {
      ...target,
      status: 'suspended',
      suspensionDays: days,
      suspensionReason: reason,
      suspendedAt,
      suspendedUntil,
    };

    setAvailableUsers((prev) => prev.map((u) => (u.id === userId ? updatedUser : u)));

    if (currentUser.id === userId) {
      setCurrentUser(updatedUser);
      localStorage.setItem('hatch_current_user', JSON.stringify(updatedUser));
    }

    // Push official suspension notification to notifications queue
    const dayLabelAr = days === 1 ? 'يوم واحد' : days === 2 ? 'يومين' : days <= 10 ? `${days} أيام` : `${days} يوماً`;
    const notifItem: NotificationItem = {
      id: 'suspension_' + Date.now(),
      type: 'alert',
      titleAr: `⚠️ تنبيه من إدارة هتش: تم إيقاف حسابك لمدة ${dayLabelAr}`,
      titleEn: `⚠️ HATCH Admin Notice: Your account is suspended for ${days} days`,
      bodyAr: `تم إيقاف حسابك في منصة هتش حتى تاريخ ${formattedDateAr}.\nالسبب: ${reason}\n\nأثناء فترة الإيقاف لن تتمكن من إضافة إعلانات جديدة أو إرسال عروض شراء. لمراجعة القرار أو الاستئناف يرجى التواصل مع فريق الدعم والإدارة.`,
      bodyEn: `Your HATCH account has been suspended until ${formattedDateEn}.\nReason: ${reason}\n\nDuring suspension, posting ads and sending offers are disabled. Contact support to appeal.`,
      time: language === 'ar' ? 'الآن' : 'Just now',
      read: false,
    };

    setNotifications((prev) => [notifItem, ...prev]);

    showToast(
      language === 'ar'
        ? `⚠️ تم إيقاف حساب (${target.name}) لمدة ${days} يوم وإرسال إشعار رسمي بالسبب`
        : `⚠️ Account (${target.name}) suspended for ${days} days with official reason notification sent`,
      'info'
    );
  };

  const adminUnsuspendUser = (userId: string) => {
    const target = availableUsers.find((u) => u.id === userId);
    if (!target) return;

    const updatedUser: User = {
      ...target,
      status: 'active',
      suspensionDays: undefined,
      suspensionReason: undefined,
      suspendedAt: undefined,
      suspendedUntil: undefined,
    };

    setAvailableUsers((prev) => prev.map((u) => (u.id === userId ? updatedUser : u)));

    if (currentUser.id === userId) {
      setCurrentUser(updatedUser);
      localStorage.setItem('hatch_current_user', JSON.stringify(updatedUser));
    }

    const restoreNotif: NotificationItem = {
      id: 'restore_' + Date.now(),
      type: 'update',
      titleAr: '✅ تم رفع الإيقاف عن حسابك بالكامل',
      titleEn: '✅ Account Suspension Lifted',
      bodyAr: 'مرحباً بك مجدداً في هتش! تم رفع الإيقاف عن حسابك، ويمكنك الآن نشر إعلاناتك وتقديم عروض الشراء والتواصل بشكل طبيعي.',
      bodyEn: 'Welcome back to HATCH! Your account suspension has been lifted, and you can now post ads and make offers normally.',
      time: language === 'ar' ? 'الآن' : 'Just now',
      read: false,
    };

    setNotifications((prev) => [restoreNotif, ...prev]);

    showToast(
      language === 'ar'
        ? `✅ تم رفع الإيقاف عن حساب (${target.name}) وإعادة تفعيله بنجاح`
        : `✅ Suspension lifted for (${target.name}) and account reactivated`,
      'success'
    );
  };

  const unreadNotificationsCount = notifications.filter((n) => !n.read).length;
  const unreadMessagesCount = conversations.reduce((acc, c) => acc + c.unreadCount, 0);

  return (
    <AppContext.Provider
      value={{
        language,
        setLanguage,
        t,
        activeTab,
        setActiveTab,
        currentUser,
        switchUser,
        availableUsers,
        createNewAccount,
        updateCurrentUser,
        isCreateAccountOpen,
        setIsCreateAccountOpen,
        isEditProfileOpen,
        setIsEditProfileOpen,
        products,
        swipedIds,
        favorites,
        toggleFavorite,
        swipeRight,
        swipeLeft,
        superLike,
        undoSwipe,
        resetDeck,
        addProduct,
        updateProduct,
        deleteProduct,
        markProductAsSold,
        selectedCity,
        setSelectedCity,
        selectedCategory,
        setSelectedCategory,
        selectedCondition,
        setSelectedCondition,
        searchQuery,
        setSearchQuery,
        onlyNegotiable,
        setOnlyNegotiable,
        onlyUrgent,
        setOnlyUrgent,
        onlyBankak,
        setOnlyBankak,
        priceRange,
        setPriceRange,
        isPriceUnlimited,
        setIsPriceUnlimited,
        resetFilters,
        selectedProductForDetail,
        setSelectedProductForDetail,
        selectedProductForOffer,
        setSelectedProductForOffer,
        isSellModalOpen,
        setIsSellModalOpen,
        isSafetyModalOpen,
        setIsSafetyModalOpen,
        isNotificationsOpen,
        setIsNotificationsOpen,
        isApkModalOpen,
        setIsApkModalOpen,
        conversations,
        activeConversationId,
        setActiveConversationId,
        startOrOpenConversation,
        sendMessage,
        submitOffer,
        respondToOffer,
        notifications,
        markNotificationsAsRead,
        broadcastNotification,
        unreadNotificationsCount,
        unreadMessagesCount,
        flagProduct,
        adminApproveProduct,
        adminDeleteProduct,
        adminDeleteUser,
        adminSuspendUser,
        adminUnsuspendUser,
        reports,
        submitReport,
        reportModalData,
        openReportModal,
        closeReportModal,
        adminResolveReport,
        reviews,
        selectedSellerForReview,
        isReviewModalOpen,
        setIsReviewModalOpen,
        openReviewModal,
        closeReviewModal,
        submitUserReview,
        getSellerReviews,
        adMobConfig,
        updateAdMobConfig,
        adStats,
        sponsoredAds,
        addSponsoredAd,
        updateSponsoredAd,
        toggleSponsoredAdStatus,
        deleteSponsoredAd,
        resetAdStats,
        recordAdImpression,
        recordAdClick,
        isInterstitialOpen,
        showInterstitialAd,
        closeInterstitialAd,
        isRewardedOpen,
        showRewardedAd,
        closeRewardedAd,
        rewardCredits,
        useRewardCreditForListing,
        toasts,
        showToast,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
