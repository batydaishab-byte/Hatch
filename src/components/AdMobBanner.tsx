import React, { useState, useEffect } from 'react';
import { ExternalLink, X, Info, Sparkles } from 'lucide-react';
import { useApp } from '../context/AppContext';

interface AdMobBannerProps {
  position?: 'bottom' | 'inline';
  className?: string;
}

export const AdMobBanner: React.FC<AdMobBannerProps> = ({ position = 'bottom', className = '' }) => {
  const { 
    adMobConfig, 
    sponsoredAds, 
    recordAdImpression, 
    recordAdClick, 
    language 
  } = useApp();

  const [currentAdIdx, setCurrentAdIdx] = useState(0);
  const [isDismissed, setIsDismissed] = useState(false);
  const [showAdInfo, setShowAdInfo] = useState(false);

  // Only show active ads matching banner or all
  const activeAds = sponsoredAds.filter(
    (a) => a.isActive !== false && (!a.format || a.format === 'all' || a.format === 'banner')
  );

  const ad = activeAds.length > 0 ? activeAds[currentAdIdx % activeAds.length] : null;

  // Record impression on mount and when ad changes
  useEffect(() => {
    if (adMobConfig.isEnabled && !isDismissed && ad) {
      recordAdImpression('banner', ad.id);
    }
  }, [currentAdIdx, isDismissed, adMobConfig.isEnabled, ad?.id]);

  // Auto-rotate ads respecting custom or global banner duration
  useEffect(() => {
    if (!adMobConfig.isEnabled || isDismissed || activeAds.length <= 1 || !ad) return;
    const durationMs = (ad.durationSeconds || adMobConfig.bannerRotationSeconds || 10) * 1000;
    const timer = setTimeout(() => {
      setCurrentAdIdx((prev) => (prev + 1) % activeAds.length);
    }, durationMs);
    return () => clearTimeout(timer);
  }, [currentAdIdx, adMobConfig.isEnabled, isDismissed, activeAds.length, adMobConfig.bannerRotationSeconds, ad?.durationSeconds]);

  if (!adMobConfig.isEnabled || isDismissed || !ad) {
    return null;
  }

  const handleAdClick = () => {
    recordAdClick('banner', ad.id);
    if (ad.targetUrl) {
      window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
    }
  };

  if (position === 'bottom') {
    return (
      <div className={`w-full bg-neutral-950/95 border-t border-neutral-800 shadow-2xl py-2 px-3 sm:px-4 backdrop-blur-md z-30 transition-all ${className}`}>
        <div className="max-w-4xl mx-auto flex items-center justify-between gap-3">
          {/* AdMob Identity Badge */}
          <div className="flex items-center gap-1.5 shrink-0">
            <span className="text-[9px] font-black tracking-wider uppercase bg-amber-500/20 text-amber-300 border border-amber-500/30 px-1.5 py-0.5 rounded">
              AdMob
            </span>
            <button
              onClick={() => setShowAdInfo(!showAdInfo)}
              className="text-neutral-500 hover:text-neutral-300 transition-colors"
              title="AdChoices / Google AdMob Network"
            >
              <Info className="w-3 h-3" />
            </button>
          </div>

          {/* Ad Content & Link */}
          <div 
            onClick={handleAdClick}
            className="flex-1 flex items-center gap-2.5 sm:gap-3 cursor-pointer group min-w-0"
          >
            <img
              src={ad.logoUrl || ad.imageUrl}
              alt={ad.advertiser}
              className="w-8 h-8 sm:w-9 sm:h-9 rounded-lg object-cover ring-1 ring-neutral-700 shrink-0 group-hover:scale-105 transition-transform"
            />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-[10px] text-amber-400 font-bold truncate">
                  {ad.advertiser}
                </span>
                {adMobConfig.isTestMode && (
                  <span className="text-[9px] bg-neutral-800 text-neutral-400 px-1 rounded">
                    Test Unit
                  </span>
                )}
              </div>
              <p className="text-xs font-semibold text-white group-hover:text-amber-300 transition-colors truncate">
                {language === 'ar' ? ad.titleAr : ad.titleEn}
              </p>
            </div>

            {/* CTA Button */}
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                handleAdClick();
              }}
              className="hidden sm:inline-flex items-center gap-1 text-[11px] font-bold bg-amber-500 hover:bg-amber-400 text-neutral-950 px-3 py-1.5 rounded-lg shadow-sm transition-all hover:scale-105 active:scale-95 shrink-0 cursor-pointer"
            >
              <span>{language === 'ar' ? ad.ctaTextAr : ad.ctaTextEn}</span>
              <ExternalLink className="w-3 h-3" />
            </button>
          </div>

          {/* Dismiss button */}
          <button
            onClick={() => setIsDismissed(true)}
            className="w-6 h-6 rounded-full text-neutral-500 hover:text-neutral-300 hover:bg-neutral-800 flex items-center justify-center transition-colors shrink-0 cursor-pointer"
            title={language === 'ar' ? 'إغلاق الإعلان' : 'Close ad'}
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>

        {/* AdChoices popup information */}
        {showAdInfo && (
          <div className="max-w-4xl mx-auto mt-2 p-2 rounded-xl bg-neutral-900 border border-neutral-800 text-[11px] text-neutral-400 flex items-center justify-between">
            <span>
              {language === 'ar'
                ? 'إعلان مدعوم عبر شبكة Google AdMob و AdSense لتحقيق أرباح المشاهدات والنقرات في هتش 🇸🇩'
                : 'Sponsored unit powered by Google AdMob network generating impression and click revenue.'}
            </span>
            <button
              onClick={() => setShowAdInfo(false)}
              className="text-amber-400 hover:underline ms-2"
            >
              {language === 'ar' ? 'فهمت' : 'Got it'}
            </button>
          </div>
        )}
      </div>
    );
  }

  // Inline Banner (e.g. inside market feed or product details)
  return (
    <div className={`relative w-full rounded-2xl bg-neutral-900 border border-neutral-800 overflow-hidden shadow-md p-3 sm:p-4 my-4 ${className}`}>
      {/* Top micro bar */}
      <div className="flex items-center justify-between text-[10px] text-neutral-500 mb-2">
        <div className="flex items-center gap-1.5">
          <span className="font-bold uppercase tracking-wider text-amber-400/90 bg-amber-500/10 border border-amber-500/20 px-1.5 py-0.5 rounded">
            Google AdMob
          </span>
          <span>•</span>
          <span className="text-neutral-400">{language === 'ar' ? ad.badgeAr : ad.badgeEn}</span>
        </div>
        <button
          onClick={() => setIsDismissed(true)}
          className="text-neutral-500 hover:text-neutral-300 transition-colors"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>

      <div 
        onClick={handleAdClick}
        className="flex flex-col sm:flex-row items-center gap-3.5 cursor-pointer group"
      >
        <img
          src={ad.imageUrl}
          alt={ad.advertiser}
          className="w-full sm:w-44 h-28 object-cover rounded-xl ring-1 ring-neutral-800 group-hover:opacity-95 transition-opacity"
        />

        <div className="flex-1 space-y-1.5 text-center sm:text-start">
          <div className="text-xs font-bold text-amber-400">
            {ad.advertiser}
          </div>
          <h4 className="text-sm font-extrabold text-white group-hover:text-amber-300 transition-colors line-clamp-1">
            {language === 'ar' ? ad.titleAr : ad.titleEn}
          </h4>
          <p className="text-xs text-neutral-400 line-clamp-2 leading-relaxed">
            {language === 'ar' ? ad.descriptionAr : ad.descriptionEn}
          </p>

          <div className="pt-1 flex items-center justify-center sm:justify-start gap-2">
            <button
              type="button"
              className="inline-flex items-center gap-1.5 text-xs font-black bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 px-3.5 py-1.5 rounded-xl shadow-md transition-all hover:scale-105 active:scale-95"
            >
              <span>{language === 'ar' ? ad.ctaTextAr : ad.ctaTextEn}</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
            <span className="text-[10px] text-neutral-500">
              {language === 'ar' ? 'يدعم تطبيق هتش' : 'Supports HATCH'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
