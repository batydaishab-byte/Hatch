import React, { useState, useEffect } from 'react';
import { X, ExternalLink, ShieldCheck, Sparkles, Volume2, Info } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const AdMobInterstitialModal: React.FC = () => {
  const { 
    isInterstitialOpen, 
    closeInterstitialAd, 
    adMobConfig, 
    sponsoredAds, 
    recordAdClick, 
    language 
  } = useApp();

  // Only show active ads matching interstitial or all
  const activeAds = sponsoredAds.filter(
    (a) => a.isActive !== false && (!a.format || a.format === 'all' || a.format === 'interstitial')
  );

  const [currentAdIdx, setCurrentAdIdx] = useState(0);
  const ad = activeAds.length > 0 ? activeAds[currentAdIdx % activeAds.length] : null;

  const targetDuration = ad?.durationSeconds || adMobConfig.interstitialCountdownSeconds || 5;
  const [countdown, setCountdown] = useState(targetDuration);
  const [canSkip, setCanSkip] = useState(false);

  useEffect(() => {
    if (isInterstitialOpen && activeAds.length > 0) {
      const randomIdx = Math.floor(Math.random() * activeAds.length);
      setCurrentAdIdx(randomIdx);
      const chosenAd = activeAds[randomIdx];
      const duration = chosenAd?.durationSeconds || adMobConfig.interstitialCountdownSeconds || 5;

      setCountdown(duration);
      setCanSkip(false);

      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            setCanSkip(true);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [isInterstitialOpen, activeAds.length, adMobConfig.interstitialCountdownSeconds]);

  if (!isInterstitialOpen || !ad) {
    return null;
  }

  const handleCtaClick = () => {
    recordAdClick('interstitial', ad.id);
    if (ad.targetUrl) {
      window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
    }
    closeInterstitialAd();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/95 backdrop-blur-lg animate-in fade-in duration-200">
      <div className="relative w-full max-w-xl h-full sm:h-auto sm:max-h-[90vh] bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden flex flex-col justify-between">
        
        {/* Top AdMob Interstitial Header Bar */}
        <div className="p-3 bg-neutral-950 border-b border-neutral-800 flex items-center justify-between z-20">
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-black bg-amber-500 text-neutral-950 px-2 py-0.5 rounded">
              Google AdMob
            </span>
            <span className="text-[11px] text-neutral-400">
              {language === 'ar' ? 'إعلان بيني (Interstitial)' : 'Interstitial Ad'}
            </span>
            {adMobConfig.isTestMode && (
              <span className="text-[9px] font-mono text-neutral-500 bg-neutral-900 px-1.5 py-0.5 rounded border border-neutral-800">
                {adMobConfig.interstitialUnitId.slice(0, 24)}...
              </span>
            )}
          </div>

          {/* Countdown & Skip Button */}
          {canSkip ? (
            <button
              onClick={closeInterstitialAd}
              className="px-3 py-1 rounded-full bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-bold flex items-center gap-1 transition-all cursor-pointer shadow-md hover:scale-105 active:scale-95"
            >
              <span>{language === 'ar' ? 'تخطي الإعلان' : 'Skip Ad'}</span>
              <X className="w-4 h-4" />
            </button>
          ) : (
            <div className="px-3 py-1 rounded-full bg-neutral-900 border border-neutral-800 text-neutral-400 text-xs font-mono font-bold flex items-center gap-1.5">
              <span>{language === 'ar' ? `يمكن التخطي خلال ${countdown} ث` : `Skip in ${countdown}s`}</span>
            </div>
          )}
        </div>

        {/* Main Ad Creative */}
        <div className="relative flex-1 flex flex-col justify-center items-center p-4 sm:p-6 overflow-hidden">
          <div className="relative w-full aspect-video max-h-72 rounded-2xl overflow-hidden mb-4 shadow-xl border border-neutral-800 bg-black">
            {ad.videoUrl ? (
              <video
                src={ad.videoUrl}
                autoPlay
                playsInline
                loop
                muted
                className="w-full h-full object-cover"
              />
            ) : (
              <img
                src={ad.imageUrl}
                alt={ad.advertiser}
                className="w-full h-full object-cover"
              />
            )}
            <div className="absolute top-3 start-3 bg-neutral-950/80 backdrop-blur-md px-2.5 py-1 rounded-full text-xs font-bold text-amber-300 border border-amber-400/30 flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>{ad.advertiser}</span>
              {ad.videoUrl && (
                <span className="text-[9px] bg-amber-500 text-neutral-950 px-1 rounded font-black">
                  VIDEO
                </span>
              )}
            </div>
          </div>

          {/* Advertiser Info & Title */}
          <div className="w-full text-center space-y-2">
            <h3 className="text-lg sm:text-xl font-black text-white leading-tight">
              {language === 'ar' ? ad.titleAr : ad.titleEn}
            </h3>
            <p className="text-xs sm:text-sm text-neutral-300 max-w-md mx-auto leading-relaxed">
              {language === 'ar' ? ad.descriptionAr : ad.descriptionEn}
            </p>
          </div>
        </div>

        {/* Bottom CTA Bar */}
        <div className="p-4 sm:p-5 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between gap-3">
          <div className="text-[11px] text-neutral-400">
            <span>{language === 'ar' ? 'رعاية رسمية عبر شبكة AdMob' : 'AdMob Sponsored Partner'}</span>
          </div>

          <button
            onClick={handleCtaClick}
            className="flex-1 sm:flex-none py-3 px-6 rounded-2xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-xs sm:text-sm shadow-xl shadow-amber-500/20 flex items-center justify-center gap-2 transition-all hover:scale-105 active:scale-95 cursor-pointer"
          >
            <span>{language === 'ar' ? ad.ctaTextAr : ad.ctaTextEn}</span>
            <ExternalLink className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
