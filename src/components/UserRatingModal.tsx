import React, { useState } from 'react';
import { 
  X, 
  Star, 
  ShieldCheck, 
  CheckCircle2, 
  MessageSquare, 
  ThumbsUp, 
  Sparkles, 
  UserCheck, 
  Send,
  Calendar,
  AlertCircle
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export const UserRatingModal: React.FC = () => {
  const { 
    isReviewModalOpen, 
    closeReviewModal, 
    selectedSellerForReview, 
    submitUserReview, 
    getSellerReviews, 
    currentUser, 
    language, 
    t 
  } = useApp();

  const [rating, setRating] = useState<number>(5);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [comment, setComment] = useState<string>('');
  const [selectedTags, setSelectedTags] = useState<string[]>([
    'صادق وأمين 🤝',
    'استجابة سريعة ⚡'
  ]);
  const [activeTab, setActiveTab] = useState<'rate' | 'reviews'>('rate');

  if (!isReviewModalOpen || !selectedSellerForReview) {
    return null;
  }

  const seller = selectedSellerForReview;
  const sellerReviews = getSellerReviews(seller.sellerId);
  const isSelfRating = currentUser.id === seller.sellerId;

  const availableTags = language === 'ar' ? [
    'صادق وأمين 🤝',
    'استجابة سريعة ⚡',
    'بضاعة مطابقة للوصف ✨',
    'سعر عادل وتفاوض مرن 💰',
    'التزام بمواعيد التسليم ⏱️',
    'سلاسة في الدفع عبر بنكك 🏦',
    'تعامل راقي وأخلاق عالية 🇸🇩',
    'سلعة ممتازة ونظيفة 💎'
  ] : [
    'Honest & Trustworthy 🤝',
    'Fast Response ⚡',
    'Matches Description ✨',
    'Fair Price & Flexible 💰',
    'Punctual Delivery ⏱️',
    'Smooth Bankak Payment 🏦',
    'Polite & Respectful 🇸🇩',
    'Clean Condition 💎'
  ];

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter((t) => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const getRatingFeedback = (stars: number) => {
    switch (stars) {
      case 5:
        return language === 'ar' ? 'ممتاز وموثوق جداً 🌟 (أنصح بشدة بالتعامل معه)' : 'Excellent & Highly Trusted 🌟';
      case 4:
        return language === 'ar' ? 'جيد جداً وتعامل طيب 👍 (تجربة إيجابية)' : 'Very Good & Pleasant Deal 👍';
      case 3:
        return language === 'ar' ? 'جيد ومقبول 👌 (تمت المعاملة بنجاح)' : 'Good & Acceptable 👌';
      case 2:
        return language === 'ar' ? 'أقل من المتوقع ⚠️ (يحتاج لتحسين في الوصف أو الموعد)' : 'Below Expectations ⚠️';
      case 1:
        return language === 'ar' ? 'سيء ولا أنصح به ❌ (واجهت مشكلة في المعاملة)' : 'Poor Experience ❌';
      default:
        return '';
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isSelfRating) return;

    submitUserReview({
      sellerId: seller.sellerId,
      sellerName: seller.sellerName,
      rating,
      comment: comment || (language === 'ar' ? 'بائع موثوق وتعامل محترم وأنصح بالتعامل معه.' : 'Trusted seller with great communication.'),
      tags: selectedTags,
      productId: seller.productId,
      productTitle: seller.productTitle,
    });

    closeReviewModal();
    setComment('');
  };

  const currentDisplayRating = hoverRating !== null ? hoverRating : rating;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto animate-in fade-in duration-200">
      <div 
        onClick={(e) => e.stopPropagation()}
        className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden my-auto flex flex-col max-h-[92vh]"
      >
        {/* Header Bar */}
        <div className="p-4 sm:p-5 bg-gradient-to-b from-neutral-800/80 to-neutral-900 border-b border-neutral-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="relative">
              <img
                src={seller.sellerAvatar}
                alt={seller.sellerName}
                className="w-12 h-12 rounded-full object-cover ring-2 ring-amber-400/80 shadow-md"
              />
              {seller.sellerVerified && (
                <div 
                  className="absolute -bottom-1 -right-1 bg-neutral-950 rounded-full p-0.5 text-emerald-400"
                  title={t.verifiedSeller}
                >
                  <ShieldCheck className="w-4 h-4 fill-emerald-500/20" />
                </div>
              )}
            </div>

            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base sm:text-lg font-black text-white">
                  {seller.sellerName}
                </h3>
                {seller.sellerVerified && (
                  <span className="text-[10px] font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                    {language === 'ar' ? 'بائع موثق ✓' : 'Verified ✓'}
                  </span>
                )}
              </div>

              <div className="flex items-center gap-2 text-xs text-neutral-400 mt-0.5">
                <span className="text-amber-400 font-extrabold flex items-center gap-1">
                  <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                  {seller.sellerRating.toFixed(1)}
                </span>
                <span>•</span>
                <span>{seller.sellerDealsCount} {t.dealsDone}</span>
                <span>•</span>
                <span>{sellerReviews.length} {language === 'ar' ? 'تقييم مسجل' : 'reviews'}</span>
              </div>
            </div>
          </div>

          <button
            onClick={closeReviewModal}
            className="w-9 h-9 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white flex items-center justify-center transition-transform hover:scale-105 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Product context banner if rating originated from a product page */}
        {seller.productTitle && (
          <div className="px-4 py-2.5 bg-amber-500/10 border-b border-amber-500/20 flex items-center justify-between text-xs text-amber-300">
            <span className="truncate">
              {language === 'ar' ? 'بشأن الإعلان المعروض: ' : 'Regarding listing: '}
              <span className="font-bold text-white">{seller.productTitle}</span>
            </span>
            <span className="shrink-0 text-[10px] text-amber-400 bg-amber-500/20 px-2 py-0.5 rounded-full font-bold">
              {language === 'ar' ? 'صفقة هتش' : 'HATCH Deal'}
            </span>
          </div>
        )}

        {/* Tab Switcher: Add Rating vs View Reviews */}
        <div className="flex border-b border-neutral-800 bg-neutral-950/60 p-1">
          <button
            onClick={() => setActiveTab('rate')}
            className={`flex-1 py-2 text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              activeTab === 'rate'
                ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <Star className={`w-3.5 h-3.5 ${activeTab === 'rate' ? 'fill-neutral-950' : 'text-neutral-400'}`} />
            <span>{t.addYourRating}</span>
          </button>

          <button
            onClick={() => setActiveTab('reviews')}
            className={`flex-1 py-2 text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
              activeTab === 'reviews'
                ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>{t.previousReviews} ({sellerReviews.length})</span>
          </button>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-4 flex-1">
          {activeTab === 'rate' ? (
            isSelfRating ? (
              <div className="p-5 text-center bg-neutral-950/50 rounded-2xl border border-neutral-800 space-y-3">
                <div className="w-12 h-12 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 flex items-center justify-center mx-auto">
                  <UserCheck className="w-6 h-6" />
                </div>
                <h4 className="text-sm font-bold text-white">
                  {language === 'ar' ? 'هذا هو حسابك الشخصي' : 'This is your own account'}
                </h4>
                <p className="text-xs text-neutral-400 leading-relaxed max-w-xs mx-auto">
                  {language === 'ar' 
                    ? 'لا يمكنك تقييم حسابك الشخصي، ولكن يمكنك متابعة تقييمات المشترين الآخرين من تبويب "آراء المشترين".'
                    : 'You cannot rate your own profile. You can view feedback from other buyers in the reviews tab.'}
                </p>
                <button
                  type="button"
                  onClick={() => setActiveTab('reviews')}
                  className="px-4 py-2 rounded-xl bg-amber-500 text-neutral-950 text-xs font-bold transition-all cursor-pointer"
                >
                  {language === 'ar' ? 'عرض تقييمات حسابي' : 'View My Reviews'}
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                {/* Star Selector Box */}
                <div className="p-4 rounded-2xl bg-neutral-950/70 border border-neutral-800 flex flex-col items-center text-center space-y-2">
                  <span className="text-xs font-bold text-neutral-300">
                    {t.rateExperience}
                  </span>

                  {/* 5 Big Interactive Golden Stars */}
                  <div className="flex items-center gap-2 py-1">
                    {[1, 2, 3, 4, 5].map((starValue) => {
                      const isFilled = starValue <= currentDisplayRating;
                      return (
                        <button
                          key={starValue}
                          type="button"
                          onClick={() => setRating(starValue)}
                          onMouseEnter={() => setHoverRating(starValue)}
                          onMouseLeave={() => setHoverRating(null)}
                          className="p-1 rounded-xl hover:scale-125 active:scale-95 transition-all cursor-pointer focus:outline-none"
                          title={`${starValue} ${language === 'ar' ? 'نجوم' : 'Stars'}`}
                        >
                          <Star 
                            className={`w-8 h-8 sm:w-9 sm:h-9 transition-colors ${
                              isFilled 
                                ? 'fill-amber-400 text-amber-400 drop-shadow-[0_0_8px_rgba(251,191,36,0.6)]' 
                                : 'text-neutral-700 hover:text-neutral-500'
                            }`}
                          />
                        </button>
                      );
                    })}
                  </div>

                  {/* Dynamic Reaction Text */}
                  <div className="text-xs font-bold text-amber-400 min-h-[20px] animate-in fade-in duration-150">
                    {getRatingFeedback(currentDisplayRating)}
                  </div>
                </div>

                {/* Compliment Badges / Tags */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-2">
                    {t.complimentsTitle}
                  </label>
                  <div className="flex flex-wrap gap-1.5">
                    {availableTags.map((tag) => {
                      const isSelected = selectedTags.includes(tag);
                      return (
                        <button
                          key={tag}
                          type="button"
                          onClick={() => toggleTag(tag)}
                          className={`text-xs px-2.5 py-1.5 rounded-xl border transition-all cursor-pointer ${
                            isSelected
                              ? 'bg-amber-500/20 border-amber-500 text-amber-300 font-bold shadow-sm'
                              : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-neutral-700 hover:text-neutral-200'
                          }`}
                        >
                          {tag}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Review Text Field */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                    {language === 'ar' ? 'تعليقك وتفاصيل التجربة:' : 'Your comment & feedback:'}
                  </label>
                  <textarea
                    rows={3}
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder={t.reviewPlaceholder}
                    className="w-full bg-neutral-950 border border-neutral-800 rounded-2xl p-3 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500 transition-colors resize-none"
                  />
                  <div className="flex items-center justify-between text-[11px] text-neutral-500 mt-1">
                    <span>
                      {language === 'ar' 
                        ? `سيتم نشر التقييم باسم: ${currentUser.name}` 
                        : `Posting as: ${currentUser.name}`}
                    </span>
                    <span className="flex items-center gap-1 text-emerald-400">
                      <CheckCircle2 className="w-3 h-3" />
                      {language === 'ar' ? 'تقييم فوري ومعتمد' : 'Instant & Verified'}
                    </span>
                  </div>
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-xs sm:text-sm shadow-lg shadow-amber-500/20 flex items-center justify-center gap-2 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
                >
                  <Star className="w-4 h-4 fill-neutral-950 text-neutral-950" />
                  <span>{t.submitReviewBtn}</span>
                </button>
              </form>
            )
          ) : (
            /* Previous Reviews Tab */
            <div className="space-y-3">
              {sellerReviews.length > 0 ? (
                sellerReviews.map((rev) => (
                  <div 
                    key={rev.id}
                    className="p-3.5 rounded-2xl bg-neutral-950/80 border border-neutral-800/80 space-y-2"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2.5">
                        <img
                          src={rev.reviewerAvatar}
                          alt={rev.reviewerName}
                          className="w-7 h-7 rounded-full object-cover ring-1 ring-amber-400/50"
                        />
                        <div>
                          <div className="text-xs font-bold text-white">
                            {rev.reviewerName}
                          </div>
                          <div className="text-[10px] text-neutral-400 flex items-center gap-1">
                            <Calendar className="w-2.5 h-2.5" />
                            <span>{rev.createdAt}</span>
                            {rev.productTitle && (
                              <>
                                <span>•</span>
                                <span className="truncate max-w-[120px] text-neutral-300">
                                  {rev.productTitle}
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Stars */}
                      <div className="flex items-center gap-0.5 text-amber-400">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star 
                            key={i}
                            className={`w-3.5 h-3.5 ${
                              i < Math.round(rev.rating) 
                                ? 'fill-amber-400 text-amber-400' 
                                : 'text-neutral-700'
                            }`}
                          />
                        ))}
                      </div>
                    </div>

                    {/* Comment */}
                    <p className="text-xs text-neutral-300 leading-relaxed font-normal">
                      {rev.comment}
                    </p>

                    {/* Tags */}
                    {rev.tags && rev.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1 pt-1">
                        {rev.tags.map((tg, idx) => (
                          <span
                            key={idx}
                            className="text-[10px] font-medium bg-neutral-900 border border-neutral-800 text-neutral-300 px-2 py-0.5 rounded-lg"
                          >
                            {tg}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))
              ) : (
                <div className="text-center py-12 px-4 space-y-2 bg-neutral-950/40 rounded-2xl border border-neutral-800/60">
                  <Star className="w-8 h-8 text-neutral-600 mx-auto" />
                  <h4 className="text-xs font-bold text-neutral-300">
                    {language === 'ar' ? 'لا توجد تقييمات سابقة بعد' : 'No reviews yet'}
                  </h4>
                  <p className="text-[11px] text-neutral-400 max-w-xs mx-auto">
                    {t.noReviewsYet}
                  </p>
                  <button
                    type="button"
                    onClick={() => setActiveTab('rate')}
                    className="mt-2 text-xs font-bold text-amber-400 hover:text-amber-300 underline cursor-pointer"
                  >
                    {language === 'ar' ? 'كن أول من يقيّم البائع الآن ⭐' : 'Be the first to rate seller ⭐'}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer info */}
        <div className="p-3 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between text-[11px] text-neutral-400 px-5">
          <span className="flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>{language === 'ar' ? 'نظام هتش لتقييم الثقة والمصداقية' : 'HATCH Trust & Safety Protocol'}</span>
          </span>
          <button
            type="button"
            onClick={closeReviewModal}
            className="text-neutral-400 hover:text-white transition-colors cursor-pointer"
          >
            {t.cancelBtn}
          </button>
        </div>
      </div>
    </div>
  );
};
