import React, { useState, useMemo, useEffect, useRef } from 'react';
import { 
  Search, 
  SlidersHorizontal, 
  X, 
  MapPin, 
  CreditCard, 
  Flame, 
  RotateCcw, 
  Check, 
  Smartphone, 
  Laptop, 
  Car, 
  SunMedium, 
  Shirt, 
  Sofa, 
  Wrench, 
  Gamepad2, 
  Building, 
  Tag,
  Clock,
  History,
  Sparkles,
  ArrowUpRight,
  Infinity,
  RefreshCw
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { ProductCard } from './ProductCard';
import { AdMobNativeCard } from './AdMobNativeCard';
import { AdMobBanner } from './AdMobBanner';
import { CategoryId, SudaneseCity, Condition, SUDANESE_CITIES } from '../types';

export const MarketBrowse: React.FC = () => {
  const {
    products,
    sponsoredAds,
    selectedCategory,
    setSelectedCategory,
    selectedCity,
    setSelectedCity,
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
    setSelectedProductForDetail,
    t,
    language
  } = useApp();

  const [isFilterDrawerOpen, setIsFilterDrawerOpen] = useState(false);

  // Pull-to-refresh gesture state for MarketBrowse
  const [pullDistance, setPullDistance] = useState(0);
  const [isPulling, setIsPulling] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const touchStartRef = useRef<{ y: number; active: boolean }>({ y: 0, active: false });

  const PULL_THRESHOLD = 70;

  const triggerPullRefresh = () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    setPullDistance(52);

    setTimeout(() => {
      setIsRefreshing(false);
      setPullDistance(0);
      setIsPulling(false);
    }, 800);
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (window.scrollY <= 15 && e.touches.length === 1 && !isRefreshing) {
      touchStartRef.current = { y: e.touches[0].clientY, active: true };
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!touchStartRef.current.active || isRefreshing) return;
    const currentY = e.touches[0].clientY;
    const deltaY = currentY - touchStartRef.current.y;
    if (deltaY > 0 && window.scrollY <= 15) {
      const distance = Math.min(deltaY * 0.45, 110);
      setPullDistance(distance);
      setIsPulling(true);
    } else if (deltaY < 0 && isPulling) {
      setPullDistance(0);
      setIsPulling(false);
    }
  };

  const handleTouchEnd = () => {
    if (!touchStartRef.current.active) return;
    touchStartRef.current.active = false;
    if (pullDistance >= PULL_THRESHOLD) {
      triggerPullRefresh();
    } else {
      setPullDistance(0);
      setIsPulling(false);
    }
  };

  // Search history state (persisted, max 5 items)
  const [searchHistory, setSearchHistory] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('hatch_search_history');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed.slice(0, 5);
      }
    } catch (e) {
      console.error('Error loading search history', e);
    }
    return ['آيفون', 'طاقة شمسية', 'تويوتا', 'توب حرير', 'ماك بوك'];
  });

  const saveToHistory = (query: string) => {
    const trimmed = query.trim();
    if (!trimmed) return;
    setSearchHistory((prev) => {
      const filtered = prev.filter((item) => item.toLowerCase() !== trimmed.toLowerCase());
      const updated = [trimmed, ...filtered].slice(0, 5);
      try {
        localStorage.setItem('hatch_search_history', JSON.stringify(updated));
      } catch (e) {
        console.error('Error saving search history', e);
      }
      return updated;
    });
  };

  const removeFromHistory = (e: React.MouseEvent, itemToRemove: string) => {
    e.stopPropagation();
    setSearchHistory((prev) => {
      const updated = prev.filter((item) => item !== itemToRemove);
      try {
        localStorage.setItem('hatch_search_history', JSON.stringify(updated));
      } catch (e) {
        console.error('Error updating search history', e);
      }
      return updated;
    });
  };

  const clearAllHistory = () => {
    setSearchHistory([]);
    try {
      localStorage.removeItem('hatch_search_history');
    } catch (e) {
      console.error('Error clearing search history', e);
    }
  };

  const handleSelectHistoryItem = (query: string) => {
    setSearchQuery(query);
    saveToHistory(query);
  };

  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const searchContainerRef = useRef<HTMLDivElement>(null);

  // Close search suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        searchContainerRef.current &&
        !searchContainerRef.current.contains(event.target as Node)
      ) {
        setIsSearchFocused(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      saveToHistory(searchQuery);
      setIsSearchFocused(false);
    } else if (e.key === 'Escape') {
      setIsSearchFocused(false);
    }
  };

  // Debounced auto-save when user types and browses results
  useEffect(() => {
    const q = searchQuery.trim();
    if (q.length >= 2) {
      const timer = setTimeout(() => {
        saveToHistory(q);
      }, 1400);
      return () => clearTimeout(timer);
    }
  }, [searchQuery]);

  // Category items with tailored icons
  const categoryList: { id: CategoryId | 'all'; label: string; icon: React.ReactNode }[] = [
    { id: 'all', label: t.categories.all, icon: <Tag className="w-4 h-4" /> },
    { id: 'phones', label: t.categories.phones, icon: <Smartphone className="w-4 h-4" /> },
    { id: 'solar_energy', label: t.categories.solar_energy, icon: <SunMedium className="w-4 h-4" /> },
    { id: 'electronics', label: t.categories.electronics, icon: <Laptop className="w-4 h-4" /> },
    { id: 'vehicles', label: t.categories.vehicles, icon: <Car className="w-4 h-4" /> },
    { id: 'fashion', label: t.categories.fashion, icon: <Shirt className="w-4 h-4" /> },
    { id: 'furniture', label: t.categories.furniture, icon: <Sofa className="w-4 h-4" /> },
    { id: 'appliances', label: t.categories.appliances, icon: <Wrench className="w-4 h-4" /> },
    { id: 'books_hobbies', label: t.categories.books_hobbies, icon: <Gamepad2 className="w-4 h-4" /> },
  ];

  // Real-time matching against existing product categories
  const matchedCategories = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    return categoryList.filter(
      (c) => c.id !== 'all' && c.label.toLowerCase().includes(q)
    ).slice(0, 3);
  }, [searchQuery, categoryList]);

  // Real-time matching against existing product titles
  const matchedProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    return products.filter((p) => {
      if (p.status !== 'active') return false;
      const titleAr = p.titleAr.toLowerCase();
      const titleEn = p.titleEn.toLowerCase();
      return titleAr.includes(q) || titleEn.includes(q);
    }).slice(0, 5);
  }, [searchQuery, products]);

  const hasSuggestions = isSearchFocused && searchQuery.trim().length > 0 && (matchedCategories.length > 0 || matchedProducts.length > 0);

  const cities: (SudaneseCity | 'all')[] = [
    'all',
    ...SUDANESE_CITIES
  ];

  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      if (p.status !== 'active') return false;
      if (selectedCategory !== 'all' && p.category !== selectedCategory) return false;
      if (selectedCity !== 'all') {
        const selCityBase = selectedCity.split('(')[0].trim();
        const prodCityBase = p.city.split('(')[0].trim();
        if (
          p.city !== selectedCity && 
          selCityBase !== prodCityBase && 
          !p.city.includes(selCityBase) && 
          !selectedCity.includes(prodCityBase)
        ) {
          return false;
        }
      }
      if (selectedCondition !== 'all' && p.condition !== selectedCondition) return false;
      if (onlyNegotiable && !p.negotiable) return false;
      if (onlyUrgent && !p.urgentSale) return false;
      if (onlyBankak && !p.paymentMethods.includes('bankak')) return false;
      if (p.price < priceRange[0]) return false;
      if (!isPriceUnlimited && p.price > priceRange[1]) return false;

      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchTitleAr = p.titleAr.toLowerCase().includes(q);
        const matchTitleEn = p.titleEn.toLowerCase().includes(q);
        const matchDescAr = p.descriptionAr.toLowerCase().includes(q);
        const matchCity = p.city.toLowerCase().includes(q);
        const matchArea = p.areaAr.toLowerCase().includes(q);
        if (!matchTitleAr && !matchTitleEn && !matchDescAr && !matchCity && !matchArea) {
          return false;
        }
      }

      return true;
    });
  }, [
    products,
    selectedCategory,
    selectedCity,
    selectedCondition,
    onlyNegotiable,
    onlyUrgent,
    onlyBankak,
    priceRange,
    isPriceUnlimited,
    searchQuery
  ]);

  return (
    <div 
      className="w-full max-w-7xl mx-auto px-3 sm:px-6 py-4 pb-24 relative select-none"
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      onTouchCancel={handleTouchEnd}
    >
      {/* Pull-To-Refresh Visual Indicator Bar */}
      <div 
        className="w-full overflow-hidden transition-all duration-200 ease-out flex flex-col items-center justify-center shrink-0"
        style={{ 
          height: isRefreshing ? 52 : Math.max(0, pullDistance),
          opacity: isRefreshing || pullDistance > 8 ? 1 : 0,
          marginBottom: isRefreshing || pullDistance > 8 ? 12 : 0
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

      {/* Search Header Bar with Dropdown Container */}
      <div ref={searchContainerRef} className="relative mb-4 z-30">
        <div className="flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="absolute top-1/2 -translate-y-1/2 start-3.5 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={() => setIsSearchFocused(true)}
              onKeyDown={handleSearchKeyDown}
              placeholder={t.searchPlaceholder}
              className="w-full bg-neutral-900 border border-neutral-800 rounded-2xl py-3 ps-10 pe-9 text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500/80 transition-all shadow-inner"
            />
            {searchQuery && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setIsSearchFocused(false);
                }}
                className="absolute top-1/2 -translate-y-1/2 end-3 text-neutral-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Filter Toggle Button */}
          <button
            onClick={() => setIsFilterDrawerOpen(!isFilterDrawerOpen)}
            className={`flex items-center gap-1.5 px-3.5 py-3 rounded-2xl border text-sm font-semibold transition-all cursor-pointer shrink-0 ${
              isFilterDrawerOpen || onlyNegotiable || onlyUrgent || onlyBankak || selectedCondition !== 'all' || isPriceUnlimited
                ? 'bg-amber-500 text-neutral-950 border-amber-400 font-bold'
                : 'bg-neutral-900 text-neutral-200 border-neutral-800 hover:bg-neutral-800'
            }`}
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span className="hidden sm:inline">{t.filterTitle}</span>
          </button>
        </div>

        {/* Real-time Search Suggestions Dropdown */}
        {hasSuggestions && (
          <div className="absolute top-full start-0 end-0 mt-2 bg-neutral-900/98 backdrop-blur-xl border border-neutral-700/80 rounded-2xl shadow-2xl overflow-hidden divide-y divide-neutral-800 z-40 animate-in fade-in slide-in-from-top-1 duration-150">
            {/* Matched Categories section */}
            {matchedCategories.length > 0 && (
              <div className="p-2">
                <div className="px-2.5 py-1 text-[11px] font-bold text-neutral-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Tag className="w-3.5 h-3.5 text-amber-400" />
                  <span>{t.matchingCategories}</span>
                </div>
                <div className="space-y-0.5 mt-1">
                  {matchedCategories.map((cat) => (
                    <button
                      key={cat.id}
                      onClick={() => {
                        setSelectedCategory(cat.id);
                        setSearchQuery('');
                        saveToHistory(cat.label);
                        setIsSearchFocused(false);
                      }}
                      className="w-full flex items-center justify-between p-2 rounded-xl hover:bg-neutral-800/80 text-start transition-colors cursor-pointer group"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center shrink-0">
                          {cat.icon}
                        </div>
                        <span className="text-xs font-semibold text-white group-hover:text-amber-300">
                          {cat.label}
                        </span>
                      </div>
                      <span className="text-[10px] text-neutral-400 bg-neutral-950 px-2 py-0.5 rounded-full border border-neutral-800 flex items-center gap-1">
                        <span>{language === 'ar' ? 'تصفح القسم' : 'Browse'}</span>
                        <ArrowUpRight className="w-3 h-3 text-amber-400" />
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Matched Products section */}
            {matchedProducts.length > 0 && (
              <div className="p-2">
                <div className="px-2.5 py-1 text-[11px] font-bold text-neutral-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                  <span>{t.matchingProducts}</span>
                </div>
                <div className="space-y-1 mt-1">
                  {matchedProducts.map((prod) => {
                    const title = language === 'ar' ? prod.titleAr : prod.titleEn;
                    return (
                      <div
                        key={prod.id}
                        onClick={() => {
                          setSearchQuery(title);
                          saveToHistory(title);
                          setIsSearchFocused(false);
                        }}
                        className="flex items-center justify-between p-2 rounded-xl hover:bg-neutral-800/80 transition-colors cursor-pointer group"
                      >
                        <div className="flex items-center gap-2.5 min-w-0">
                          <img
                            src={prod.images[0]}
                            alt={title}
                            className="w-9 h-9 rounded-lg object-cover shrink-0 border border-neutral-800"
                          />
                          <div className="min-w-0">
                            <h5 className="text-xs font-semibold text-neutral-100 group-hover:text-amber-300 truncate">
                              {title}
                            </h5>
                            <div className="flex items-center gap-1.5 text-[10px] text-neutral-400">
                              <span className="text-amber-400 font-mono font-bold">
                                {prod.price.toLocaleString()} {t.currency}
                              </span>
                              <span>•</span>
                              <span>{prod.city}</span>
                            </div>
                          </div>
                        </div>

                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedProductForDetail(prod);
                            saveToHistory(title);
                            setIsSearchFocused(false);
                          }}
                          className="text-[10px] text-neutral-300 hover:text-white bg-neutral-950 hover:bg-neutral-700 px-2.5 py-1 rounded-lg border border-neutral-800 transition-colors shrink-0 ms-2 font-medium"
                        >
                          {language === 'ar' ? 'معاينة' : 'Inspect'}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Footer option: Search explicitly for query */}
            <div className="p-2 bg-neutral-950/70">
              <button
                onClick={() => {
                  saveToHistory(searchQuery);
                  setIsSearchFocused(false);
                }}
                className="w-full flex items-center justify-between p-2 rounded-xl hover:bg-amber-500/10 text-amber-400 text-xs font-semibold transition-colors cursor-pointer"
              >
                <div className="flex items-center gap-1.5 truncate">
                  <Search className="w-3.5 h-3.5" />
                  <span className="truncate">
                    {t.viewAllResults} «{searchQuery}»
                  </span>
                </div>
                <span className="text-[10px] text-neutral-500 font-mono bg-neutral-900 px-2 py-0.5 rounded border border-neutral-800">Enter ↵</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Previous 5 Search Queries for Quick Access */}
      {searchHistory.length > 0 && (
        <div className="flex items-center gap-2 mb-3 px-1 overflow-x-auto no-scrollbar">
          <div className="flex items-center gap-1 text-[11px] font-semibold text-neutral-400 shrink-0">
            <History className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden sm:inline">{t.searchHistory}:</span>
          </div>

          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
            {searchHistory.map((queryItem, idx) => {
              const isCurrent = searchQuery.trim().toLowerCase() === queryItem.toLowerCase();
              return (
                <div
                  key={idx}
                  onClick={() => handleSelectHistoryItem(queryItem)}
                  className={`group flex items-center gap-1.5 px-3 py-1 rounded-full text-xs transition-all cursor-pointer border shrink-0 ${
                    isCurrent
                      ? 'bg-amber-500/20 text-amber-300 border-amber-500/40 font-bold'
                      : 'bg-neutral-900/90 hover:bg-neutral-800 text-neutral-300 border-neutral-800 hover:border-neutral-700'
                  }`}
                  title={language === 'ar' ? `بحث عن: ${queryItem}` : `Search for: ${queryItem}`}
                >
                  <Clock className="w-3 h-3 text-neutral-500 group-hover:text-amber-400 transition-colors" />
                  <span className="truncate max-w-[130px]">{queryItem}</span>
                  <button
                    onClick={(e) => removeFromHistory(e, queryItem)}
                    className="p-0.5 rounded-full hover:bg-neutral-700 text-neutral-500 hover:text-rose-400 transition-colors"
                    title={language === 'ar' ? 'حذف من السجل' : 'Remove'}
                  >
                    <X className="w-2.5 h-2.5" />
                  </button>
                </div>
              );
            })}
          </div>

          <button
            onClick={clearAllHistory}
            className="text-[10px] text-neutral-500 hover:text-rose-400 transition-colors shrink-0 ms-auto whitespace-nowrap cursor-pointer px-1 py-0.5"
            title={t.clearSearchHistory}
          >
            {t.clearSearchHistory}
          </button>
        </div>
      )}

      {/* Category Pills Slider */}
      <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-3 mb-4">
        {categoryList.map((cat) => {
          const isActive = selectedCategory === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs whitespace-nowrap transition-all border cursor-pointer ${
                isActive
                  ? 'bg-amber-500 text-neutral-950 font-bold border-amber-400 shadow-md shadow-amber-500/20 scale-[1.02]'
                  : 'bg-neutral-900 text-neutral-300 border-neutral-800 hover:bg-neutral-800 hover:border-neutral-700'
              }`}
            >
              <span className={isActive ? 'text-neutral-950' : 'text-amber-400'}>
                {cat.icon}
              </span>
              <span>{cat.label}</span>
            </button>
          );
        })}
      </div>

      {/* Filter Drawer / Expanded Box */}
      {isFilterDrawerOpen && (
        <div className="mb-6 p-4 rounded-2xl bg-neutral-900/95 border border-neutral-800 shadow-xl space-y-4 animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="flex items-center justify-between border-b border-neutral-800 pb-2">
            <h4 className="font-bold text-sm text-amber-400 flex items-center gap-1.5">
              <SlidersHorizontal className="w-4 h-4" />
              {t.filterTitle}
            </h4>
            <button
              onClick={resetFilters}
              className="flex items-center gap-1 text-xs text-neutral-400 hover:text-amber-300 cursor-pointer"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              {t.clearFilters}
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* City Selector */}
            <div>
              <label className="block text-xs font-medium text-neutral-400 mb-1.5">
                {t.cityLabel}
              </label>
              <select
                value={selectedCity}
                onChange={(e) => setSelectedCity(e.target.value as any)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                {cities.map((city) => (
                  <option key={city} value={city}>
                    {city === 'all' ? t.cities.all : city}
                  </option>
                ))}
              </select>
            </div>

            {/* Condition Selector */}
            <div>
              <label className="block text-xs font-medium text-neutral-400 mb-1.5">
                {t.conditionLabel}
              </label>
              <select
                value={selectedCondition}
                onChange={(e) => setSelectedCondition(e.target.value as any)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                <option value="all">{t.conditions.all}</option>
                <option value="new">{t.conditions.new}</option>
                <option value="like_new">{t.conditions.like_new}</option>
                <option value="good">{t.conditions.good}</option>
                <option value="fair">{t.conditions.fair}</option>
              </select>
            </div>

            {/* Price Max slider & Infinite Option */}
            <div className="flex flex-col justify-between">
              <div className="flex items-center justify-between gap-1 text-xs text-neutral-400 mb-1.5">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="font-semibold text-neutral-300">{t.priceRange}</span>
                  {/* Small option button near price range for infinite / any price */}
                  <button
                    type="button"
                    onClick={() => setIsPriceUnlimited(!isPriceUnlimited)}
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold transition-all cursor-pointer border ${
                      isPriceUnlimited
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-sm shadow-amber-500/30'
                        : 'bg-neutral-800/90 hover:bg-neutral-700 text-neutral-300 border-neutral-700 hover:text-white'
                    }`}
                    title={language === 'ar' ? 'عرض المنتجات بأي سعر كان دون حد أقصى' : 'Show products at any price without limit'}
                  >
                    <Infinity className="w-3 h-3" />
                    <span>{t.anyPrice}</span>
                  </button>
                </div>

                {/* Value or Unlimited Badge */}
                <div className="shrink-0">
                  {isPriceUnlimited ? (
                    <span className="inline-flex items-center gap-1 text-amber-400 font-mono font-bold text-[11px] bg-amber-500/10 px-2 py-0.5 rounded-md border border-amber-500/20">
                      <Infinity className="w-3 h-3" />
                      <span>{t.unlimitedPriceBadge}</span>
                    </span>
                  ) : (
                    <span className="text-amber-400 font-mono font-bold text-xs">
                      {priceRange[1].toLocaleString()} {t.currency}
                    </span>
                  )}
                </div>
              </div>

              <input
                type="range"
                min="50000"
                max="25000000"
                step="50000"
                value={priceRange[1]}
                onChange={(e) => {
                  setIsPriceUnlimited(false);
                  setPriceRange([priceRange[0], Number(e.target.value)]);
                }}
                className={`w-full accent-amber-500 cursor-pointer transition-opacity ${
                  isPriceUnlimited ? 'opacity-35 hover:opacity-70' : 'opacity-100'
                }`}
              />

              <div className="flex justify-between items-center text-[10px] text-neutral-500 mt-1 font-mono">
                <span>50,000 {t.currency}</span>
                <span>25,000,000+ {t.currency}</span>
              </div>
            </div>
          </div>

          {/* Quick Filter Checkboxes */}
          <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-neutral-800/80">
            <label className="flex items-center gap-2 text-xs text-neutral-300 cursor-pointer">
              <input
                type="checkbox"
                checked={onlyBankak}
                onChange={(e) => setOnlyBankak(e.target.checked)}
                className="rounded accent-emerald-500"
              />
              <span className="flex items-center gap-1 font-medium text-emerald-400">
                <CreditCard className="w-3.5 h-3.5" />
                {t.onlyBankak}
              </span>
            </label>

            <label className="flex items-center gap-2 text-xs text-neutral-300 cursor-pointer">
              <input
                type="checkbox"
                checked={onlyUrgent}
                onChange={(e) => setOnlyUrgent(e.target.checked)}
                className="rounded accent-rose-500"
              />
              <span className="flex items-center gap-1 font-medium text-rose-400">
                <Flame className="w-3.5 h-3.5" />
                {t.onlyUrgent}
              </span>
            </label>

            <label className="flex items-center gap-2 text-xs text-neutral-300 cursor-pointer">
              <input
                type="checkbox"
                checked={onlyNegotiable}
                onChange={(e) => setOnlyNegotiable(e.target.checked)}
                className="rounded accent-amber-500"
              />
              <span>{t.onlyNegotiable}</span>
            </label>
          </div>
        </div>
      )}

      {/* Results Header Count */}
      <div className="flex items-center justify-between mb-4 text-xs text-neutral-400">
        <span>
          {filteredProducts.length} {t.itemsFound}
        </span>
        {selectedCity !== 'all' && (
          <span className="flex items-center gap-1 text-amber-400 font-medium">
            <MapPin className="w-3 h-3" />
            {selectedCity}
          </span>
        )}
      </div>

      {/* Product Grid with Native AdMob Cards */}
      {filteredProducts.length > 0 ? (
        <>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-5">
            {filteredProducts.map((product, idx) => (
              <React.Fragment key={product.id}>
                <ProductCard product={product} />
                {/* Insert Sponsored Native AdMob Card */}
                {(idx === 2 || idx === 7) && sponsoredAds[(idx === 2 ? 0 : 1) % sponsoredAds.length] && (
                  <AdMobNativeCard ad={sponsoredAds[(idx === 2 ? 0 : 1) % sponsoredAds.length]} />
                )}
              </React.Fragment>
            ))}
          </div>

          {/* In-feed AdMob Banner */}
          <AdMobBanner position="inline" className="mt-8" />
        </>
      ) : (
        <div className="text-center py-16 bg-neutral-900/40 rounded-3xl border border-neutral-800/80 p-8">
          <p className="text-neutral-400 text-sm mb-4">
            {t.noProductsFound}
          </p>
          <button
            onClick={resetFilters}
            className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-amber-400 font-semibold text-xs rounded-xl border border-neutral-700 cursor-pointer"
          >
            {t.clearFilters}
          </button>
        </div>
      )}
    </div>
  );
};
