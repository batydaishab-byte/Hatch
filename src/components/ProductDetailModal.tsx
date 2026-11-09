import React, { useState, useEffect } from 'react';
import { 
  X, 
  Heart, 
  Share2, 
  ShieldCheck, 
  MapPin, 
  Phone, 
  MessageSquare, 
  CreditCard, 
  Truck, 
  AlertTriangle, 
  Flag, 
  CheckCircle, 
  Clock, 
  Eye, 
  Flame, 
  Zap, 
  Sparkles,
  Copy,
  Check,
  MessageCircle,
  Send,
  CheckCircle2,
  Star
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { AdMobBanner } from './AdMobBanner';

export const ProductDetailModal: React.FC = () => {
  const { 
    selectedProductForDetail, 
    setSelectedProductForDetail, 
    setSelectedProductForOffer, 
    startOrOpenConversation, 
    favorites, 
    toggleFavorite, 
    flagProduct, 
    openReportModal,
    openReviewModal,
    getSellerReviews,
    showToast, 
    availableUsers,
    t, 
    language 
  } = useApp();

  const [activePhotoIdx, setActivePhotoIdx] = useState(0);
  const [showFlagConfirm, setShowFlagConfirm] = useState(false);
  const [isShareSheetOpen, setIsShareSheetOpen] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  useEffect(() => {
    if (selectedProductForDetail) {
      setActivePhotoIdx(0);
      setShowFlagConfirm(false);
      setIsShareSheetOpen(false);
    }
  }, [selectedProductForDetail?.id]);

  if (!selectedProductForDetail) return null;

  const product = selectedProductForDetail;
  const isFavorited = favorites.includes(product.id);

  // Resolve seller data with full fallback safety so seller name and verification badge always render
  const seller = availableUsers?.find((u) => u.id === product.sellerId) || {
    id: product.sellerId,
    name: product.sellerName || (language === 'ar' ? 'ياسر بابكر' : 'Yasir Babiker'),
    avatar: product.sellerAvatar || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80',
    phone: product.sellerPhone || '+249 91 234 5678',
    city: product.city || 'بورتسودان',
    rating: product.sellerRating || 4.9,
    dealsCompleted: product.sellerDealsCount || 14,
    verifiedIdentity: product.sellerVerified !== undefined ? product.sellerVerified : true,
  };

  const sellerName = product.sellerName || seller.name || (language === 'ar' ? 'ياسر بابكر' : 'Verified Seller');
  const sellerAvatar = product.sellerAvatar || seller.avatar || 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80';
  const isSellerVerified = product.sellerVerified !== undefined ? product.sellerVerified : (seller.verifiedIdentity ?? true);
  const sellerPhone = product.sellerPhone || seller.phone || '+249 91 234 5678';
  const sellerRating = product.sellerRating || seller.rating || 4.9;
  const sellerDeals = product.sellerDealsCount || seller.dealsCompleted || 14;
  const sellerReviews = getSellerReviews(product.sellerId);

  const handleOpenRating = () => {
    openReviewModal({
      sellerId: product.sellerId,
      sellerName,
      sellerAvatar,
      sellerRating,
      sellerDealsCount: sellerDeals,
      sellerVerified: isSellerVerified,
      productId: product.id,
      productTitle: language === 'ar' ? product.titleAr : product.titleEn,
    });
  };

  const productTitle = language === 'ar' ? product.titleAr : product.titleEn;
  const shareUrl = window.location.origin + '?product=' + product.id;
  const shareText = language === 'ar'
    ? `عَـرْض مميز على تطبيق هتش HATCH 🇸🇩:\n🏷️ ${product.titleAr}\n💰 السعر: ${product.price.toLocaleString()} ${t.currency}\n📍 الموقع: ${product.city} - ${product.areaAr}\n✅ الدفع: ${product.paymentMethods.map(p => t.payments[p]).join('، ')}`
    : `Special deal on HATCH Sudan 🇸🇩:\n🏷️ ${product.titleEn}\n💰 Price: ${product.price.toLocaleString()} ${t.currency}\n📍 Location: ${product.city}\nCheck it out on HATCH:`;

  const handleShare = async () => {
    if (typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({
          title: `هتش | ${productTitle}`,
          text: shareText,
          url: shareUrl,
        });
        showToast(language === 'ar' ? 'تمت مشاركة الإعلان بنجاح!' : 'Listing shared successfully!', 'success');
        return;
      } catch (err: any) {
        if (err.name === 'AbortError') return;
      }
    }
    // If Web Share is not supported or was cancelled, open direct share channels
    setIsShareSheetOpen(true);
  };

  const handleCopyLink = () => {
    navigator.clipboard?.writeText(shareUrl);
    setCopiedLink(true);
    showToast(language === 'ar' ? 'تم نسخ رابط الإعلان إلى الحافظة' : 'Listing link copied to clipboard', 'success');
    setTimeout(() => setCopiedLink(false), 2000);
  };

  const shareViaWhatsApp = () => {
    const waUrl = `https://api.whatsapp.com/send?text=${encodeURIComponent(shareText + '\n' + shareUrl)}`;
    window.open(waUrl, '_blank');
    setIsShareSheetOpen(false);
  };

  const shareViaSMS = () => {
    const smsUrl = `sms:?body=${encodeURIComponent(shareText + '\n' + shareUrl)}`;
    window.location.href = smsUrl;
    setIsShareSheetOpen(false);
  };

  const handleCall = () => {
    window.location.href = `tel:${product.sellerPhone}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[100vh] sm:max-h-[92vh] flex flex-col">
        {/* Floating Top Controls */}
        <div className="absolute top-3 inset-x-3 z-30 flex items-center justify-between pointer-events-none">
          <button
            onClick={() => setSelectedProductForDetail(null)}
            className="w-10 h-10 rounded-full bg-neutral-950/80 hover:bg-neutral-950 text-white border border-white/20 backdrop-blur-md flex items-center justify-center pointer-events-auto transition-transform hover:scale-105 cursor-pointer shadow-lg"
          >
            <X className="w-5 h-5" />
          </button>

          {/* Top Seller Quick Chip */}
          <button
            type="button"
            onClick={handleOpenRating}
            className="flex items-center gap-2 bg-neutral-950/85 hover:bg-neutral-900 border border-white/20 backdrop-blur-md py-1 px-3 rounded-full pointer-events-auto shadow-lg max-w-[200px] sm:max-w-[280px] transition-all cursor-pointer active:scale-95"
            title={language === 'ar' ? 'اضغط لتقييم البائع وعرض التقييمات' : 'Click to rate seller and view ratings'}
          >
            <img
              src={sellerAvatar}
              alt={sellerName}
              className="w-6 h-6 rounded-full object-cover ring-1 ring-amber-400 shrink-0"
            />
            <span className="text-xs font-bold text-white truncate">
              {sellerName}
            </span>
            <span className="text-[10px] text-amber-400 font-bold flex items-center gap-0.5 shrink-0">
              <Star className="w-2.5 h-2.5 fill-amber-400" />
              {sellerRating.toFixed(1)}
            </span>
            {isSellerVerified && (
              <span title={t.verifiedSeller}>
                <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
              </span>
            )}
          </button>

          <div className="flex items-center gap-2 pointer-events-auto">
            <button
              onClick={handleShare}
              className="w-10 h-10 rounded-full bg-neutral-950/80 hover:bg-neutral-950 text-white border border-white/20 backdrop-blur-md flex items-center justify-center transition-transform hover:scale-105 cursor-pointer shadow-lg"
              title={t.shareListing}
            >
              <Share2 className="w-4 h-4" />
            </button>

            <button
              onClick={() => toggleFavorite(product.id)}
              className="w-10 h-10 rounded-full bg-neutral-950/80 hover:bg-neutral-950 text-white border border-white/20 backdrop-blur-md flex items-center justify-center transition-transform hover:scale-105 cursor-pointer shadow-lg"
              title={t.saveFavorite}
            >
              <Heart
                className={`w-5 h-5 ${
                  isFavorited ? 'fill-rose-500 text-rose-500' : 'text-white'
                }`}
              />
            </button>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="overflow-y-auto flex-1 no-scrollbar">
          {/* Main Photo Gallery */}
          <div className="relative aspect-[4/3] sm:aspect-[16/10] w-full bg-neutral-950">
            <img
              src={product.images[activePhotoIdx] || product.images[0]}
              alt={language === 'ar' ? product.titleAr : product.titleEn}
              className="w-full h-full object-cover transition-opacity duration-300"
            />

            {/* Badges Overlay */}
            <div className="absolute bottom-3 start-3 flex items-center gap-2 flex-wrap">
              {product.urgentSale && (
                <span className="flex items-center gap-1 text-xs font-bold bg-rose-600 text-white px-2.5 py-1 rounded-full shadow-lg">
                  <Flame className="w-3.5 h-3.5 fill-current" />
                  {t.urgentBadge}
                </span>
              )}
              {product.paymentMethods.includes('bankak') && (
                <span className="flex items-center gap-1 text-xs font-bold bg-emerald-600 text-white px-2.5 py-1 rounded-full shadow-lg backdrop-blur-sm">
                  <CreditCard className="w-3.5 h-3.5" />
                  {language === 'ar' ? 'بنكك فوري' : 'Bankak Ready'}
                </span>
              )}
            </div>
          </div>

          {/* Thumbnails Row if multiple photos */}
          {product.images.length > 1 && (
            <div className="flex items-center gap-2 p-3 bg-neutral-950/60 overflow-x-auto no-scrollbar border-b border-neutral-800">
              {product.images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setActivePhotoIdx(idx)}
                  className={`w-16 h-14 rounded-xl overflow-hidden border-2 transition-all shrink-0 cursor-pointer ${
                    activePhotoIdx === idx
                      ? 'border-amber-400 scale-105'
                      : 'border-transparent opacity-60 hover:opacity-100'
                  }`}
                >
                  <img src={img} alt="thumb" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}

          {/* Product Header & Pricing */}
          <div className="p-4 sm:p-6 space-y-4">
            {/* PROMINENT SELLER IDENTITY & VERIFICATION CARD (Visible immediately upon opening) */}
            <div className="p-4 rounded-2xl bg-neutral-950/90 border-2 border-emerald-500/30 shadow-xl flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center gap-3.5">
                <div className="relative shrink-0">
                  <img
                    src={sellerAvatar}
                    alt={sellerName}
                    className="w-13 h-13 rounded-2xl object-cover ring-2 ring-amber-400 shadow-md"
                  />
                  <span className="absolute -bottom-1 -right-1 w-3.5 h-3.5 rounded-full bg-emerald-500 border-2 border-neutral-950" title="نشط الآن" />
                </div>

                <div>
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-black text-base sm:text-lg text-white">
                      {sellerName}
                    </span>
                    {isSellerVerified ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-black bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 px-2.5 py-0.5 rounded-full shadow-sm">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        <span>{language === 'ar' ? 'بائع موثق (10+ صفقات بدون مخالفات) ✓' : 'Verified Seller (10+ Clean Sales) ✓'}</span>
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold bg-neutral-800 text-neutral-400 px-2 py-0.5 rounded-full">
                        <span>{language === 'ar' ? 'عضو في هتش' : 'Member'}</span>
                      </span>
                    )}
                  </div>

                  <div className="text-xs text-neutral-400 flex items-center gap-2 mt-1.5 flex-wrap">
                    {/* Interactive Star Rating Button */}
                    <button
                      type="button"
                      onClick={handleOpenRating}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 active:scale-95 text-amber-400 border border-amber-500/30 transition-all cursor-pointer group shadow-sm"
                      title={language === 'ar' ? 'اضغط لتقييم البائع وعرض التقييمات' : 'Click to rate seller and view ratings'}
                    >
                      <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400 group-hover:scale-125 transition-transform" />
                      <span className="font-black text-xs">{sellerRating.toFixed(1)}</span>
                      <span className="text-[10px] text-neutral-300 font-medium">
                        ({sellerReviews.length} {language === 'ar' ? 'تقييم' : 'reviews'})
                      </span>
                      <span className="text-[10px] text-amber-400 font-bold underline ms-0.5 group-hover:text-amber-300">
                        {language === 'ar' ? 'قيّم البائع ⭐' : 'Rate ⭐'}
                      </span>
                    </button>
                    <span>•</span>
                    <span className="text-neutral-300 font-medium">
                      {sellerDeals} {t.dealsDone}
                    </span>
                    <span>•</span>
                    <span className="text-neutral-400">
                      {product.city}
                    </span>
                  </div>
                </div>
              </div>

              {/* Quick Contact & Action Buttons */}
              <div className="flex items-center gap-2 pt-2 sm:pt-0 border-t sm:border-t-0 border-neutral-800 shrink-0">
                <button
                  onClick={handleCall}
                  className="flex-1 sm:flex-none px-3.5 py-2.5 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-400 border border-amber-500/30 text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                  title={t.callSeller}
                >
                  <Phone className="w-3.5 h-3.5" />
                  <span>{language === 'ar' ? 'اتصال' : 'Call'}</span>
                </button>

                <button
                  onClick={() => startOrOpenConversation(product.id)}
                  className="flex-1 sm:flex-none px-3.5 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white border border-neutral-700 text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                  title={t.chatSeller}
                >
                  <MessageSquare className="w-3.5 h-3.5 text-amber-400" />
                  <span>{language === 'ar' ? 'مراسلة' : 'Chat'}</span>
                </button>
              </div>
            </div>
            <div>
              {/* Category & Condition tags */}
              <div className="flex items-center gap-2 mb-2 text-xs">
                <span className="text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2.5 py-1 rounded-lg font-semibold">
                  {t.categories[product.category]}
                </span>
                <span className="text-neutral-300 bg-neutral-800 px-2.5 py-1 rounded-lg font-medium">
                  {t.conditions[product.condition]}
                </span>
                <span className="text-neutral-400 flex items-center gap-1 text-[11px] ms-auto">
                  <Clock className="w-3 h-3" />
                  {product.createdAt}
                </span>
              </div>

              {/* Title */}
              <h2 className="text-xl sm:text-2xl font-extrabold text-white leading-snug">
                {language === 'ar' ? product.titleAr : product.titleEn}
              </h2>

              {/* Price Banner */}
              <div className="mt-3 flex items-baseline justify-between p-3.5 rounded-2xl bg-neutral-950/80 border border-neutral-800">
                <div className="flex items-baseline gap-2">
                  <span className="text-2xl sm:text-3xl font-black text-amber-400">
                    {product.price.toLocaleString()}
                  </span>
                  <span className="text-sm font-bold text-amber-300">
                    {t.currency}
                  </span>
                  {product.originalPrice && product.originalPrice > product.price && (
                    <span className="text-sm text-neutral-400 line-through">
                      {product.originalPrice.toLocaleString()} {t.currency}
                    </span>
                  )}
                </div>

                {product.negotiable ? (
                  <span className="text-xs font-bold text-emerald-400 bg-emerald-950/80 border border-emerald-800/80 px-2.5 py-1 rounded-full">
                    {t.negotiable}
                  </span>
                ) : (
                  <span className="text-xs font-medium text-neutral-400 bg-neutral-800 px-2.5 py-1 rounded-full">
                    {t.fixedPrice}
                  </span>
                )}
              </div>
            </div>

            {/* Quick Specs / Location & Delivery */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="flex items-center gap-2 p-3 rounded-xl bg-neutral-800/50 border border-neutral-800">
                <MapPin className="w-4 h-4 text-amber-400 shrink-0" />
                <div>
                  <div className="text-[10px] text-neutral-400">{t.cityLabel}</div>
                  <div className="font-semibold text-white">
                    {product.city} - {language === 'ar' ? product.areaAr : product.areaEn}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 p-3 rounded-xl bg-neutral-800/50 border border-neutral-800">
                <Truck className="w-4 h-4 text-amber-400 shrink-0" />
                <div>
                  <div className="text-[10px] text-neutral-400">{language === 'ar' ? 'التسليم' : 'Delivery'}</div>
                  <div className="font-semibold text-white">
                    {product.deliveryAvailable ? t.deliveryAvailable : t.pickupOnly}
                  </div>
                </div>
              </div>
            </div>

            {/* Accepted Payment Methods in Sudan */}
            <div>
              <h4 className="text-xs font-bold text-neutral-400 mb-2 uppercase tracking-wider">
                {t.acceptedPaymentsLabel}
              </h4>
              <div className="flex items-center gap-2 flex-wrap">
                {product.paymentMethods.map((pm) => (
                  <span
                    key={pm}
                    className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-xl bg-neutral-800 border border-neutral-700 text-neutral-200"
                  >
                    <CreditCard className="w-3.5 h-3.5 text-amber-400" />
                    {t.payments[pm]}
                  </span>
                ))}
              </div>
            </div>

            {/* Description Body */}
            <div>
              <h4 className="text-xs font-bold text-neutral-400 mb-2 uppercase tracking-wider">
                {t.descriptionLabel}
              </h4>
              <p className="text-sm text-neutral-200 leading-relaxed whitespace-pre-line bg-neutral-950/40 p-3.5 rounded-2xl border border-neutral-800/80">
                {language === 'ar' ? product.descriptionAr : product.descriptionEn}
              </p>
            </div>

            {/* Trust and Verification Footer Badge */}
            <div className="p-3.5 rounded-2xl bg-neutral-950/60 border border-neutral-800 flex items-center justify-between text-xs">
              <div className="flex items-center gap-2.5">
                <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0" />
                <div>
                  <div className="font-bold text-white">
                    {language === 'ar' ? 'معاملة آمنة عبر منصة هتش' : 'Safe & Verified Listing'}
                  </div>
                  <div className="text-[11px] text-neutral-400">
                    {language === 'ar' 
                      ? `المعلن: ${sellerName} • ${isSellerVerified ? 'بائع موثق (10+ صفقات موثوقة بدون مخالفات)' : 'حساب نشط'}` 
                      : `Seller: ${sellerName} • ${isSellerVerified ? 'Verified Seller (10+ Clean Sales)' : 'Active Member'}`}
                  </div>
                </div>
              </div>
              <button
                onClick={handleCall}
                className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-amber-400 border border-neutral-700 font-bold text-xs flex items-center gap-1 transition-colors cursor-pointer"
              >
                <Phone className="w-3.5 h-3.5" />
                <span>{language === 'ar' ? 'اتصال' : 'Call'}</span>
              </button>
            </div>

            {/* Seller Reviews & Trust Rating Card */}
            <div className="p-3.5 rounded-2xl bg-neutral-950/80 border border-neutral-800 space-y-2.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white">
                      {t.sellerRatingTitle}
                    </h4>
                    <p className="text-[10px] text-neutral-400">
                      {language === 'ar' ? 'تقييمات المشترين السابقة والمصداقية' : 'Verified buyer ratings and trust score'}
                    </p>
                  </div>
                </div>

                {/* Rating button */}
                <button
                  type="button"
                  onClick={handleOpenRating}
                  className="px-3 py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer shadow-sm active:scale-95"
                >
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                  <span>{language === 'ar' ? 'أضف تقييمك ⭐' : 'Rate Seller ⭐'}</span>
                </button>
              </div>

              {/* Stars summary bar */}
              <div 
                onClick={handleOpenRating}
                className="flex items-center justify-between p-2.5 rounded-xl bg-neutral-900/60 border border-neutral-800/80 cursor-pointer hover:border-amber-500/30 transition-colors"
              >
                <div className="flex items-center gap-2">
                  <div className="flex items-center text-amber-400">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star 
                        key={i} 
                        className={`w-4 h-4 ${
                          i < Math.round(sellerRating) 
                            ? 'fill-amber-400 text-amber-400' 
                            : 'text-neutral-700'
                        }`} 
                      />
                    ))}
                  </div>
                  <span className="text-sm font-black text-amber-400">
                    {sellerRating.toFixed(1)}
                  </span>
                  <span className="text-xs text-neutral-400">
                    / 5.0
                  </span>
                </div>

                <span className="text-xs text-neutral-300 font-medium">
                  {sellerReviews.length} {language === 'ar' ? 'تقييم من المشترين' : 'buyer reviews'}
                </span>
              </div>

              {/* Excerpt of latest review if available */}
              {sellerReviews.length > 0 && (
                <div 
                  onClick={handleOpenRating}
                  className="text-[11px] text-neutral-300 bg-neutral-900/40 p-2.5 rounded-xl border border-neutral-800/50 cursor-pointer hover:border-amber-500/20 transition-colors"
                >
                  <div className="flex items-center gap-1.5 text-neutral-400 mb-1">
                    <span className="font-bold text-white">{sellerReviews[0].reviewerName}</span>
                    <span>•</span>
                    <span className="text-amber-400 font-bold">★ {sellerReviews[0].rating}</span>
                    <span>•</span>
                    <span>{sellerReviews[0].createdAt}</span>
                  </div>
                  <p className="line-clamp-2 italic text-neutral-300">
                    "{sellerReviews[0].comment}"
                  </p>
                </div>
              )}
            </div>

            {/* Quick Share to WhatsApp / SMS / Apps */}
            <div className="p-3.5 rounded-2xl bg-neutral-950/80 border border-neutral-800 space-y-2.5">
              <div className="flex items-center justify-between text-xs font-bold text-neutral-300">
                <div className="flex items-center gap-1.5 text-amber-400">
                  <Share2 className="w-4 h-4" />
                  <span>{language === 'ar' ? 'مشاركة الإعلان مع الأصدقاء' : 'Share this deal'}</span>
                </div>
                <span className="text-[10px] text-neutral-400 font-normal">
                  {language === 'ar' ? 'واتساب، رسائل، أو تطبيقاتك' : 'WhatsApp, SMS & Social'}
                </span>
              </div>
              <div className="grid grid-cols-3 gap-2 text-xs">
                <button
                  type="button"
                  onClick={shareViaWhatsApp}
                  className="py-2.5 px-2 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/40 text-emerald-400 font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                  title="مشاركة عبر واتساب"
                >
                  <MessageCircle className="w-4 h-4 text-emerald-400" />
                  <span>{language === 'ar' ? 'واتساب' : 'WhatsApp'}</span>
                </button>
                <button
                  type="button"
                  onClick={shareViaSMS}
                  className="py-2.5 px-2 rounded-xl bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/40 text-blue-400 font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                  title="مشاركة عبر رسالة SMS"
                >
                  <MessageSquare className="w-4 h-4 text-blue-400" />
                  <span>{language === 'ar' ? 'رسالة SMS' : 'SMS'}</span>
                </button>
                <button
                  type="button"
                  onClick={handleShare}
                  className="py-2.5 px-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 text-neutral-200 font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                  title="مشاركة عبر تطبيقات أخرى"
                >
                  <Share2 className="w-4 h-4 text-amber-400" />
                  <span>{language === 'ar' ? 'المزيد' : 'More'}</span>
                </button>
              </div>
            </div>

            {/* In-product AdMob Smart Banner */}
            <AdMobBanner position="inline" />

            {/* Sudan Safety Advisory Box */}
            <div className="p-3.5 rounded-2xl bg-amber-950/20 border border-amber-500/30 text-xs space-y-1">
              <div className="flex items-center gap-1.5 text-amber-400 font-bold">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{t.safetyNoticeTitle}</span>
              </div>
              <p className="text-neutral-300 leading-normal text-[11px]">
                {t.safetyNoticeP1}
              </p>
            </div>

            {/* Report violation option */}
            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => {
                  openReportModal({
                    productId: product.id,
                    productTitle: language === 'ar' ? product.titleAr : product.titleEn,
                    userId: product.sellerId,
                    userName: sellerName,
                  });
                }}
                className="flex items-center gap-1.5 text-xs text-neutral-400 hover:text-red-400 bg-neutral-900/60 hover:bg-red-500/10 border border-neutral-800 hover:border-red-500/30 px-3 py-1.5 rounded-xl transition-all cursor-pointer"
              >
                <Flag className="w-3.5 h-3.5 text-red-400" />
                <span>{t.reportListing}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Sticky Bottom Action Buttons */}
        <div className="p-3 sm:p-4 bg-neutral-950/95 border-t border-neutral-800 flex items-center gap-2.5">
          {/* Share Button in bottom action bar */}
          <button
            onClick={handleShare}
            className="p-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-amber-400 border border-neutral-700 flex items-center justify-center transition-all cursor-pointer shrink-0 hover:scale-105 active:scale-95 shadow-md"
            title={t.shareListing}
          >
            <Share2 className="w-5 h-5" />
          </button>

          {product.status === 'sold' ? (
            <div className="flex-1 py-3 px-3 rounded-xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-300 font-bold text-xs sm:text-sm flex items-center justify-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
              <span>{t.soldBadge}</span>
            </div>
          ) : (
            <>
              <button
                onClick={() => {
                  setSelectedProductForDetail(null);
                  setSelectedProductForOffer(product);
                }}
                className="flex-1 py-3 px-3 rounded-xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-xs sm:text-sm shadow-lg shadow-amber-500/20 flex items-center justify-center gap-1.5 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer truncate"
              >
                <Zap className="w-4 h-4 fill-neutral-950 shrink-0" />
                <span className="truncate">{t.makeOffer}</span>
              </button>

              <button
                onClick={() => {
                  startOrOpenConversation(product.id);
                }}
                className="flex-1 py-3 px-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white font-bold text-xs sm:text-sm border border-neutral-700 flex items-center justify-center gap-1.5 transition-all cursor-pointer truncate"
              >
                <MessageSquare className="w-4 h-4 text-amber-400 shrink-0" />
                <span className="truncate">{t.chatSeller}</span>
              </button>
            </>
          )}
        </div>

        {/* Dedicated Share Sheet Modal / Fallback Dialog */}
        {isShareSheetOpen && (
          <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-neutral-950/80 backdrop-blur-sm animate-in fade-in duration-150">
            <div className="w-full max-w-sm bg-neutral-900 border border-neutral-700/80 rounded-3xl p-5 shadow-2xl space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
                    <Share2 className="w-4 h-4" />
                  </div>
                  <h4 className="font-bold text-sm text-white">
                    {language === 'ar' ? 'مشاركة الإعلان' : 'Share Deal'}
                  </h4>
                </div>
                <button
                  onClick={() => setIsShareSheetOpen(false)}
                  className="w-7 h-7 rounded-full bg-neutral-800 text-neutral-400 hover:text-white flex items-center justify-center"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Channels Grid */}
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  onClick={shareViaWhatsApp}
                  className="p-3 rounded-2xl bg-emerald-950/40 hover:bg-emerald-900/40 border border-emerald-600/40 text-emerald-400 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer"
                >
                  <MessageCircle className="w-6 h-6 text-emerald-400" />
                  <span className="text-xs font-bold">{language === 'ar' ? 'واتساب' : 'WhatsApp'}</span>
                </button>

                <button
                  onClick={shareViaSMS}
                  className="p-3 rounded-2xl bg-blue-950/40 hover:bg-blue-900/40 border border-blue-600/40 text-blue-400 flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer"
                >
                  <MessageSquare className="w-6 h-6 text-blue-400" />
                  <span className="text-xs font-bold">{language === 'ar' ? 'رسالة SMS' : 'SMS Message'}</span>
                </button>
              </div>

              {/* Copy link row */}
              <div className="p-2.5 rounded-xl bg-neutral-950 border border-neutral-800 flex items-center justify-between text-xs">
                <span className="text-neutral-400 truncate max-w-[200px] text-[11px]">
                  {shareUrl}
                </span>
                <button
                  onClick={handleCopyLink}
                  className="px-2.5 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-amber-400 font-bold flex items-center gap-1 transition-colors cursor-pointer shrink-0"
                >
                  {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedLink ? (language === 'ar' ? 'تم النسخ' : 'Copied') : (language === 'ar' ? 'نسخ' : 'Copy')}</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
