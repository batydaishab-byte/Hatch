import React, { useState, useMemo, useRef } from 'react';
import { motion, useMotionValue, useTransform, AnimatePresence } from 'motion/react';
import { 
  Heart, 
  X, 
  Zap, 
  RotateCcw, 
  Info, 
  MapPin, 
  ShieldCheck, 
  CreditCard, 
  Sparkles, 
  Filter, 
  CheckCircle2, 
  Flame, 
  Tag, 
  ChevronRight, 
  ChevronLeft,
  RefreshCw,
  ArrowDown
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Product } from '../types';

export const SwipeDeck: React.FC = () => {
  const { 
    products, 
    swipedIds, 
    swipeRight, 
    swipeLeft, 
    superLike, 
    undoSwipe, 
    resetDeck,
    setSelectedProductForDetail,
    selectedCity,
    setSelectedCity,
    selectedCategory,
    setSelectedCategory,
    favorites,
    toggleFavorite,
    t,
    language 
  } = useApp();

  // Filter deck based on active city and category, excluding swiped items
  const activeCards = useMemo(() => {
    return products.filter((prod) => {
      if (prod.status !== 'active') return false;
      if (swipedIds.includes(prod.id)) return false;
      if (selectedCity !== 'all') {
        const selCityBase = selectedCity.split('(')[0].trim();
        const prodCityBase = prod.city.split('(')[0].trim();
        if (
          prod.city !== selectedCity && 
          selCityBase !== prodCityBase && 
          !prod.city.includes(selCityBase) && 
          !selectedCity.includes(prodCityBase)
        ) {
          return false;
        }
      }
      if (selectedCategory !== 'all' && prod.category !== selectedCategory) return false;
      return true;
    });
  }, [products, swipedIds, selectedCity, selectedCategory]);

  const topCard = activeCards[0] || null;
  const nextCard = activeCards[1] || null;
  const isTopCardFavorited = topCard ? favorites.includes(topCard.id) : false;

  // Track active image index on top card
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  // Pull-to-refresh gesture state
  const [pullDistance, setPullDistance] = useState(0);
  const [isPulling, setIsPulling] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const touchStartRef = useRef<{ y: number; active: boolean }>({ y: 0, active: false });

  const PULL_THRESHOLD = 70;

  const triggerPullRefresh = () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    setPullDistance(56);
    
    // Smooth reload of the product stream
    setTimeout(() => {
      resetDeck();
      setIsRefreshing(false);
      setPullDistance(0);
      setIsPulling(false);
    }, 850);
  };

  const handleContainerTouchStart = (e: React.TouchEvent) => {
    if (window.scrollY <= 20 && e.touches.length === 1 && !isRefreshing) {
      touchStartRef.current = { y: e.touches[0].clientY, active: true };
    }
  };

  const handleContainerTouchMove = (e: React.TouchEvent) => {
    if (!touchStartRef.current.active || isRefreshing) return;
    const currentY = e.touches[0].clientY;
    const deltaY = currentY - touchStartRef.current.y;
    if (deltaY > 0 && window.scrollY <= 20) {
      const distance = Math.min(deltaY * 0.45, 110);
      setPullDistance(distance);
      setIsPulling(true);
    } else if (deltaY < 0 && isPulling) {
      setPullDistance(0);
      setIsPulling(false);
    }
  };

  const handleContainerTouchEnd = () => {
    if (!touchStartRef.current.active) return;
    touchStartRef.current.active = false;
    if (pullDistance >= PULL_THRESHOLD) {
      triggerPullRefresh();
    } else {
      setPullDistance(0);
      setIsPulling(false);
    }
  };

  // Motion values for swipe drag
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const rotate = useTransform(x, [-260, 260], [-18, 18]);
  const opacity = useTransform(x, [-300, -200, 0, 200, 300], [0, 1, 1, 1, 0]);

  // Stamps opacity
  const nextOpacity = useTransform(x, [20, 130], [0, 1]);
  const prevOpacity = useTransform(x, [-20, -130], [0, 1]);
  const superLikeOpacity = useTransform(y, [-20, -120], [0, 1]);
  const refreshCardOpacity = useTransform(y, [30, 115], [0, 1]);

  const handleDragEnd = (_: any, info: any) => {
    const thresholdX = 110;
    const thresholdY = -120;
    const thresholdPullDown = 95;

    if (info.offset.y < thresholdY && Math.abs(info.offset.x) < 90) {
      if (topCard) {
        superLike(topCard.id);
        setActiveImageIndex(0);
      }
    } else if (info.offset.y > thresholdPullDown && Math.abs(info.offset.x) < 85) {
      // Pull down gesture on the card itself triggers feed refresh
      triggerPullRefresh();
    } else if (info.offset.x > thresholdX) {
      // Swipe Right: Browse Next (تصفح التالي)
      if (topCard) {
        swipeRight(topCard.id);
        setActiveImageIndex(0);
      }
    } else if (info.offset.x < -thresholdX) {
      // Swipe Left: Browse Previous (تصفح السابق)
      swipeLeft();
      setActiveImageIndex(0);
    }
    x.set(0);
    y.set(0);
  };

  const handleNextPhoto = (e: React.MouseEvent, totalPhotos: number) => {
    e.stopPropagation();
    setActiveImageIndex((prev) => (prev + 1) % totalPhotos);
  };

  const handlePrevPhoto = (e: React.MouseEvent, totalPhotos: number) => {
    e.stopPropagation();
    setActiveImageIndex((prev) => (prev - 1 + totalPhotos) % totalPhotos);
  };

  return (
    <div 
      className="w-full flex flex-col items-center justify-start py-2 sm:py-4 px-3 max-w-lg mx-auto min-h-[calc(100vh-8.5rem)] relative select-none"
      onTouchStart={handleContainerTouchStart}
      onTouchMove={handleContainerTouchMove}
      onTouchEnd={handleContainerTouchEnd}
      onTouchCancel={handleContainerTouchEnd}
    >
      {/* Pull-To-Refresh Visual Indicator Bar */}
      <div 
        className="w-full overflow-hidden transition-all duration-200 ease-out flex flex-col items-center justify-center shrink-0"
        style={{ 
          height: isRefreshing ? 52 : Math.max(0, pullDistance),
          opacity: isRefreshing || pullDistance > 8 ? 1 : 0,
          marginBottom: isRefreshing || pullDistance > 8 ? 8 : 0
        }}
      >
        <div className={`w-10 h-10 rounded-full flex items-center justify-center border shadow-xl transition-all duration-150 ${
          isRefreshing || pullDistance >= PULL_THRESHOLD
            ? 'bg-amber-500/20 text-amber-300 border-amber-400 shadow-amber-500/25 scale-110'
            : 'bg-neutral-900/90 text-neutral-300 border-neutral-700'
        }`}>
          <RefreshCw 
            className={`w-5 h-5 text-amber-400 transition-transform ${isRefreshing ? 'animate-spin' : ''}`}
            style={{ 
              transform: !isRefreshing ? `rotate(${pullDistance * 4.5}deg)` : undefined 
            }}
          />
        </div>
      </div>

      {/* Mini Filter Pills Bar + Quick Reload Action */}
      <div className="w-full flex items-center justify-between gap-2 mb-3 text-xs">
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
          <button
            onClick={() => setSelectedCity('all')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity === 'all'
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            {t.cities.all}
          </button>
          <button
            onClick={() => setSelectedCity(selectedCity.includes('بورتسودان') ? 'all' : 'بورتسودان (ولاية البحر الأحمر)')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity.includes('بورتسودان')
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            🌊 بورتسودان
          </button>
          <button
            onClick={() => setSelectedCity(selectedCity.includes('الخرطوم') ? 'all' : 'الخرطوم (ولاية الخرطوم)')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity.includes('الخرطوم')
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            🏙️ الخرطوم
          </button>
          <button
            onClick={() => setSelectedCity(selectedCity.includes('أم درمان') ? 'all' : 'أم درمان (ولاية الخرطوم)')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity.includes('أم درمان')
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            🕌 أم درمان
          </button>
          <button
            onClick={() => setSelectedCity(selectedCity.includes('ودمدني') || selectedCity.includes('ود مدني') ? 'all' : 'ودمدني (ولاية الجزيرة)')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity.includes('ودمدني') || selectedCity.includes('ود مدني')
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            🌾 ودمدني
          </button>
          <button
            onClick={() => setSelectedCity(selectedCity.includes('عطبرة') ? 'all' : 'عطبرة (ولاية نهر النيل)')}
            className={`px-3 py-1 rounded-full whitespace-nowrap border transition-all cursor-pointer ${
              selectedCity.includes('عطبرة')
                ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400'
                : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
            }`}
          >
            🚂 عطبرة
          </button>
        </div>

        <span className="text-[11px] text-neutral-400 font-mono shrink-0">
          {activeCards.length} {language === 'ar' ? 'عرض' : 'left'}
        </span>
      </div>

      {/* Main Card Viewport */}
      <div 
        className="relative w-full aspect-[3/4.4] sm:aspect-[3/4.2] max-h-[580px] rounded-3xl select-none"
        style={{ 
          transform: pullDistance > 0 && isPulling ? `translateY(${pullDistance * 0.35}px)` : undefined,
          transition: isPulling ? 'none' : 'transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1)'
        }}
      >
        <AnimatePresence>
          {topCard ? (
            <>
              {/* Background Next Card Preview for tactile 3D deck feeling */}
              {nextCard && (
                <div 
                  key={nextCard.id}
                  className="absolute inset-0 rounded-3xl overflow-hidden bg-neutral-900 border border-neutral-800 scale-[0.94] translate-y-3 opacity-60 pointer-events-none shadow-2xl transition-all"
                >
                  <img
                    src={nextCard.images[0]}
                    alt={nextCard.titleAr}
                    className="w-full h-full object-cover brightness-75"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-neutral-950 via-neutral-950/40 to-transparent" />
                </div>
              )}

              {/* Top Interactive Card */}
              <motion.div
                key={topCard.id}
                style={{ x, y, rotate, opacity }}
                drag
                dragConstraints={{ left: 0, right: 0, top: 0, bottom: 0 }}
                dragElastic={0.8}
                onDragEnd={handleDragEnd}
                className="absolute inset-0 rounded-3xl overflow-hidden bg-neutral-900 border border-neutral-700/80 shadow-2xl cursor-grab active:cursor-grabbing touch-none"
              >
                {/* Photo Gallery Viewer */}
                <img
                  src={topCard.images[activeImageIndex] || topCard.images[0]}
                  alt={language === 'ar' ? topCard.titleAr : topCard.titleEn}
                  className="w-full h-full object-cover transition-opacity duration-200"
                  draggable={false}
                />

                {/* Top Overlay Bar: Story-like image indicators & quick badges */}
                <div className="absolute top-0 inset-x-0 p-3 bg-gradient-to-b from-neutral-950/80 via-neutral-950/30 to-transparent flex flex-col gap-2 z-20">
                  {/* Photo Progress Segments */}
                  {topCard.images.length > 1 && (
                    <div className="flex items-center gap-1.5 w-full">
                      {topCard.images.map((_, idx) => (
                        <div
                          key={idx}
                          className={`h-1 rounded-full flex-1 transition-all ${
                            idx === activeImageIndex ? 'bg-amber-400' : 'bg-white/30'
                          }`}
                        />
                      ))}
                    </div>
                  )}

                  {/* Badges Row + Top-Right Heart Favorite Button */}
                  <div className="flex items-center justify-between gap-2 mt-1">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {topCard.urgentSale && (
                        <span className="flex items-center gap-1 text-[11px] font-black bg-rose-600/90 text-white px-2 py-0.5 rounded-full shadow-md backdrop-blur-sm animate-pulse">
                          <Flame className="w-3 h-3 fill-current" />
                          {t.urgentBadge}
                        </span>
                      )}

                      {topCard.paymentMethods.includes('bankak') && (
                        <span className="flex items-center gap-1 text-[11px] font-bold bg-emerald-600/90 text-white px-2 py-0.5 rounded-full shadow-md backdrop-blur-sm">
                          <CreditCard className="w-3 h-3" />
                          {language === 'ar' ? 'بنكك فوري' : 'Bankak Ready'}
                        </span>
                      )}

                      <span className="text-[11px] font-semibold bg-neutral-900/80 text-amber-300 border border-amber-500/30 px-2 py-0.5 rounded-full backdrop-blur-sm">
                        {t.conditions[topCard.condition]}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedProductForDetail(topCard);
                        }}
                        className="w-9 h-9 rounded-full bg-neutral-900/80 text-white hover:bg-neutral-800 border border-white/20 flex items-center justify-center backdrop-blur-md cursor-pointer transition-transform hover:scale-110"
                        title={t.viewAllPhotos}
                      >
                        <Info className="w-4 h-4 text-amber-400" />
                      </button>

                      {/* TOP RIGHT FAVORITE HEART BUTTON - Clicking toggles favorite */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleFavorite(topCard.id);
                        }}
                        className={`w-11 h-11 rounded-full flex items-center justify-center transition-all cursor-pointer backdrop-blur-md shadow-xl ${
                          isTopCardFavorited
                            ? 'bg-rose-600 text-white border-2 border-rose-300 shadow-rose-600/50 scale-105 animate-in zoom-in-75 duration-150'
                            : 'bg-neutral-950/80 hover:bg-neutral-900 text-white border border-white/30 hover:scale-110 active:scale-95'
                        }`}
                        title={isTopCardFavorited ? (language === 'ar' ? 'إزالة من المفضلة' : 'Remove from Favorites') : (language === 'ar' ? 'إضافة إلى المفضلة' : 'Add to Favorites')}
                      >
                        <Heart
                          className={`w-5 h-5 transition-transform duration-200 ${
                            isTopCardFavorited ? 'fill-current text-white scale-110' : 'text-white'
                          }`}
                        />
                      </button>
                    </div>
                  </div>
                </div>

                {/* Left/Right Photo Tap Zones */}
                {topCard.images.length > 1 && (
                  <div className="absolute inset-0 flex z-10 pointer-events-auto">
                    <div
                      onClick={(e) => handlePrevPhoto(e, topCard.images.length)}
                      className="w-1/2 h-2/3 cursor-pointer"
                      title="Previous photo"
                    />
                    <div
                      onClick={(e) => handleNextPhoto(e, topCard.images.length)}
                      className="w-1/2 h-2/3 cursor-pointer"
                      title="Next photo"
                    />
                  </div>
                )}

                {/* BROWSE NEXT STAMP (When dragging right) */}
                <motion.div
                  style={{ opacity: nextOpacity }}
                  className="absolute top-16 left-6 z-30 pointer-events-none transform -rotate-12 border-4 border-amber-400 rounded-2xl px-4 py-1.5 bg-neutral-950/90 backdrop-blur-sm shadow-xl"
                >
                  <span className="text-xl font-black text-amber-400 tracking-wider flex items-center gap-1.5">
                    <span>{language === 'ar' ? 'التالي ➡️' : 'NEXT ➡️'}</span>
                  </span>
                </motion.div>

                {/* BROWSE PREVIOUS STAMP (When dragging left) */}
                <motion.div
                  style={{ opacity: prevOpacity }}
                  className="absolute top-16 right-6 z-30 pointer-events-none transform rotate-12 border-4 border-emerald-400 rounded-2xl px-4 py-1.5 bg-neutral-950/90 backdrop-blur-sm shadow-xl"
                >
                  <span className="text-xl font-black text-emerald-400 tracking-wider flex items-center gap-1.5">
                    <RotateCcw className="w-5 h-5 text-emerald-400 stroke-[2.5]" />
                    <span>{language === 'ar' ? 'السابق ⬅️' : 'PREVIOUS ⬅️'}</span>
                  </span>
                </motion.div>

                {/* SUPER LIKE STAMP (When dragging up) */}
                <motion.div
                  style={{ opacity: superLikeOpacity }}
                  className="absolute bottom-36 left-1/2 -translate-x-1/2 z-30 pointer-events-none border-4 border-amber-400 rounded-2xl px-4 py-1.5 bg-amber-950/90 backdrop-blur-sm shadow-2xl"
                >
                  <span className="text-xl font-black text-amber-300 tracking-wider flex items-center gap-1 whitespace-nowrap">
                    <Zap className="w-5 h-5 fill-amber-400 text-amber-400" />
                    {language === 'ar' ? 'عرض فوري ⚡' : 'SUPER OFFER'}
                  </span>
                </motion.div>

                {/* PULL DOWN TO RELOAD STAMP (When dragging card down) */}
                <motion.div
                  style={{ opacity: refreshCardOpacity }}
                  className="absolute top-14 left-1/2 -translate-x-1/2 z-30 pointer-events-none border-2 border-amber-400 rounded-full w-12 h-12 flex items-center justify-center bg-neutral-950/95 backdrop-blur-md shadow-2xl"
                >
                  <RefreshCw className="w-6 h-6 text-amber-400 animate-spin" />
                </motion.div>

                {/* Bottom Card Information Details */}
                <div 
                  onClick={() => setSelectedProductForDetail(topCard)}
                  className="absolute bottom-0 inset-x-0 p-4 pt-16 bg-gradient-to-t from-neutral-950 via-neutral-950/90 to-transparent z-20 flex flex-col gap-2 cursor-pointer"
                >
                  {/* Price and Negotiation Badge */}
                  <div className="flex items-baseline justify-between gap-2">
                    <div className="flex items-baseline gap-2">
                      <span className="text-2xl sm:text-3xl font-black text-amber-400 tracking-tight">
                        {topCard.price.toLocaleString()}
                      </span>
                      <span className="text-sm font-bold text-amber-300">
                        {t.currency}
                      </span>
                      {topCard.originalPrice && topCard.originalPrice > topCard.price && (
                        <span className="text-xs text-neutral-400 line-through">
                          {topCard.originalPrice.toLocaleString()} {t.currency}
                        </span>
                      )}
                    </div>

                    {topCard.negotiable && (
                      <span className="text-[11px] font-semibold text-emerald-300 bg-emerald-950/70 border border-emerald-800 px-2 py-0.5 rounded-full">
                        {t.negotiable}
                      </span>
                    )}
                  </div>

                  {/* Title */}
                  <h3 className="text-base sm:text-lg font-bold text-white line-clamp-2 leading-snug drop-shadow-md">
                    {language === 'ar' ? topCard.titleAr : topCard.titleEn}
                  </h3>

                  {/* Location & Seller Mini Pill */}
                  <div className="flex items-center justify-between text-xs text-neutral-300 pt-1 border-t border-neutral-800/80">
                    <div className="flex items-center gap-1 truncate">
                      <MapPin className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                      <span className="font-medium truncate">
                        {topCard.city} • {language === 'ar' ? topCard.areaAr : topCard.areaEn}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0 bg-neutral-900/60 px-2.5 py-0.5 rounded-full border border-neutral-800">
                      <img
                        src={topCard.sellerAvatar}
                        alt={topCard.sellerName}
                        className="w-4 h-4 rounded-full object-cover ring-1 ring-amber-400/50"
                      />
                      <span className="text-[11px] font-medium text-neutral-200 truncate max-w-[80px]">
                        {topCard.sellerName}
                      </span>
                      <span className="text-[10px] text-amber-400 font-bold">
                        ★ {(topCard.sellerRating || 4.9).toFixed(1)}
                      </span>
                      {topCard.sellerVerified && (
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                      )}
                    </div>
                  </div>
                </div>
              </motion.div>
            </>
          ) : (
            /* Empty State when cards run out */
            <div className="w-full h-full rounded-3xl bg-neutral-900/80 border border-neutral-800 p-6 flex flex-col items-center justify-center text-center shadow-inner">
              <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 mb-4 animate-bounce">
                <Sparkles className="w-8 h-8" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                {t.outOfCardsTitle}
              </h3>
              <p className="text-xs text-neutral-400 max-w-xs mb-6 leading-relaxed">
                {t.outOfCardsSubtitle}
              </p>
              <div className="flex flex-col sm:flex-row items-center gap-3 w-full max-w-xs">
                <button
                  onClick={resetDeck}
                  className="w-full py-2.5 px-4 bg-gradient-to-r from-amber-500 to-yellow-500 text-neutral-950 font-bold text-xs rounded-xl shadow-lg shadow-amber-500/20 hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer"
                >
                  {t.resetCards}
                </button>
                <button
                  onClick={() => setSelectedCity('all')}
                  className="w-full py-2.5 px-4 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold rounded-xl border border-neutral-700 transition-all cursor-pointer"
                >
                  {t.cities.all}
                </button>
              </div>
            </div>
          )}
        </AnimatePresence>
      </div>

      {/* Swipe Controls Button Bar */}
      <div className="w-full flex items-center justify-center gap-4 sm:gap-6 mt-4 z-20">
        {/* Previous Button (السابق - السحب لليسار) */}
        <button
          onClick={() => swipeLeft()}
          disabled={!swipedIds.length}
          className="w-13 h-13 rounded-full bg-neutral-900 border-2 border-neutral-700 hover:border-emerald-400 text-neutral-300 hover:text-emerald-400 disabled:opacity-30 disabled:pointer-events-none flex items-center justify-center shadow-xl transition-all hover:scale-110 active:scale-95 cursor-pointer group"
          title={language === 'ar' ? 'السابق (الرجوع للإعلان السابق ⬅️)' : 'Previous Card ⬅️'}
        >
          <RotateCcw className="w-5 h-5 group-hover:-rotate-45 transition-transform" />
        </button>

        {/* Super Like / Instant Offer Button (Swipe Up) */}
        <button
          onClick={() => topCard && superLike(topCard.id)}
          disabled={!topCard}
          className="w-12 h-12 rounded-full bg-gradient-to-tr from-amber-500 to-yellow-400 text-neutral-950 hover:from-amber-400 hover:to-yellow-300 disabled:opacity-40 shadow-xl shadow-amber-500/30 flex items-center justify-center transition-all hover:scale-110 active:scale-95 cursor-pointer"
          title={t.superLikeAction}
        >
          <Zap className="w-5 h-5 fill-neutral-950" />
        </button>

        {/* Favorite Heart Button (Toggles favorite directly without discarding card) */}
        <button
          onClick={() => topCard && toggleFavorite(topCard.id)}
          disabled={!topCard}
          className={`w-14 h-14 rounded-full border-2 transition-all hover:scale-110 active:scale-95 disabled:opacity-40 shadow-xl flex items-center justify-center cursor-pointer ${
            isTopCardFavorited
              ? 'bg-rose-600 border-rose-400 text-white shadow-rose-600/40'
              : 'bg-neutral-900 border-rose-500/40 text-rose-400 hover:bg-rose-600 hover:text-white hover:border-rose-500'
          }`}
          title={isTopCardFavorited ? (language === 'ar' ? 'إزالة من المفضلة' : 'Remove from Favorites') : (language === 'ar' ? 'إضافة إلى المفضلة' : 'Add to Favorites')}
        >
          <Heart className={`w-6 h-6 ${isTopCardFavorited ? 'fill-current' : ''}`} />
        </button>

        {/* Next / Browse Card Button (التالي - السحب لليمين) */}
        <button
          onClick={() => topCard && swipeRight(topCard.id)}
          disabled={!topCard}
          className="w-13 h-13 rounded-full bg-neutral-900 border-2 border-amber-500/50 text-amber-400 hover:bg-amber-500 hover:text-neutral-950 disabled:opacity-40 shadow-xl shadow-amber-500/20 flex items-center justify-center transition-all hover:scale-110 active:scale-95 cursor-pointer group"
          title={language === 'ar' ? 'التالي (تصفح الإعلان التالي ➡️)' : 'Next Product ➡️'}
        >
          {language === 'ar' ? (
            <ChevronLeft className="w-6 h-6 group-hover:-translate-x-0.5 transition-transform" />
          ) : (
            <ChevronRight className="w-6 h-6 group-hover:translate-x-0.5 transition-transform" />
          )}
        </button>
      </div>

      {/* Subtle Hint Text */}
      <p className="text-[11px] text-neutral-400 text-center mt-3 hidden sm:block">
        {language === 'ar' 
          ? 'اسحب لليمين ➡️ للتالي • اسحب لليسار ⬅️ للسابق • اسحب للأسفل 🔄 للتحديث' 
          : 'Swipe right ➡️ for Next • Swipe left ⬅️ for Previous • Pull down 🔄 to refresh'}
      </p>
    </div>
  );
};
