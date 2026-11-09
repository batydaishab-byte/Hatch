import React, { useState, useEffect } from 'react';
import confetti from 'canvas-confetti';
import { Gift, X, ExternalLink, Play, CheckCircle2, AlertTriangle, Sparkles } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const AdMobRewardedModal: React.FC = () => {
  const { 
    isRewardedOpen, 
    closeRewardedAd, 
    adMobConfig, 
    sponsoredAds, 
    recordAdClick,
    rewardCredits,
    showToast,
    language,
    t 
  } = useApp();

  // Only show active ads matching rewarded or all
  const activeAds = sponsoredAds.filter(
    (a) => a.isActive !== false && (!a.format || a.format === 'all' || a.format === 'rewarded')
  );

  const [currentAdIdx, setCurrentAdIdx] = useState(0);
  const ad = activeAds.length > 0 ? activeAds[currentAdIdx % activeAds.length] : null;

  const totalDuration = ad?.durationSeconds || adMobConfig.rewardedDurationSeconds || 15;
  const [timeLeft, setTimeLeft] = useState(totalDuration);
  const [isCompleted, setIsCompleted] = useState(false);
  const [showExitConfirm, setShowExitConfirm] = useState(false);

  useEffect(() => {
    if (isRewardedOpen && activeAds.length > 0) {
      const randomIdx = Math.floor(Math.random() * activeAds.length);
      setCurrentAdIdx(randomIdx);
      const chosenAd = activeAds[randomIdx];
      const duration = chosenAd?.durationSeconds || adMobConfig.rewardedDurationSeconds || 15;

      setTimeLeft(duration);
      setIsCompleted(false);
      setShowExitConfirm(false);

      const interval = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(interval);
            handleRewardCompleted();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(interval);
    }
  }, [isRewardedOpen, activeAds.length, adMobConfig.rewardedDurationSeconds]);

  const handleRewardCompleted = () => {
    setIsCompleted(true);
    try {
      confetti({
        particleCount: 60,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#F59E0B', '#10B981', '#3B82F6'],
      });
    } catch (_) {}

    showToast(
      language === 'ar'
        ? 'تهانينا! حصلت على رصيد تمييز إعلان مجاني بعد إكمال مشاهدة إعلان AdMob 🎁'
        : 'Congratulations! You earned a free ad boost credit after watching AdMob ad 🎁',
      'success'
    );
  };

  if (!isRewardedOpen || !ad) {
    return null;
  }

  const progressPercent = Math.min(100, Math.max(0, ((totalDuration - timeLeft) / totalDuration) * 100));

  const handleEarlyExit = () => {
    if (isCompleted) {
      closeRewardedAd();
    } else {
      setShowExitConfirm(true);
    }
  };

  const handleCtaClick = () => {
    recordAdClick('rewarded', ad.id);
    if (ad.targetUrl) {
      window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/95 backdrop-blur-xl animate-in fade-in duration-200">
      <div className="relative w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[100vh] sm:max-h-[92vh]">
        
        {/* Top Header Bar */}
        <div className="p-3 bg-neutral-950 border-b border-neutral-800 flex items-center justify-between z-20">
          <div className="flex items-center gap-2">
            <span className="flex items-center gap-1 text-[10px] font-black bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 px-2 py-0.5 rounded">
              <Gift className="w-3 h-3 fill-neutral-950" />
              <span>AdMob Rewarded</span>
            </span>
            <span className="text-xs font-bold text-white">
              {language === 'ar' ? 'فيديو بمكافأة' : 'Rewarded Video'}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {!isCompleted ? (
              <span className="text-xs font-mono font-bold text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2.5 py-0.5 rounded-full">
                {timeLeft} {language === 'ar' ? 'ثانية' : 's'}
              </span>
            ) : (
              <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-0.5 rounded-full flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>{language === 'ar' ? 'تمت المكافأة ✓' : 'Reward Granted ✓'}</span>
              </span>
            )}

            <button
              onClick={handleEarlyExit}
              className="w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Video Progress Bar */}
        <div className="w-full bg-neutral-800 h-1.5">
          <div 
            className="h-full bg-gradient-to-r from-amber-500 to-emerald-400 transition-all duration-1000 ease-linear"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        {/* Early Exit Confirmation Overlay */}
        {showExitConfirm && (
          <div className="absolute inset-0 bg-neutral-950/90 z-30 flex items-center justify-center p-6 text-center animate-in zoom-in-95 duration-150">
            <div className="bg-neutral-900 border border-neutral-800 p-6 rounded-2xl max-w-sm space-y-4">
              <div className="w-12 h-12 rounded-full bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center mx-auto">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h4 className="text-base font-black text-white">
                {language === 'ar' ? 'هل تريد الخروج وتفويت المكافأة؟' : 'Leave and lose reward?'}
              </h4>
              <p className="text-xs text-neutral-400 leading-relaxed">
                {language === 'ar'
                  ? `تبقى فقط ${timeLeft} ثوانٍ لتحصل على رصيد تمييز إعلانك مجاناً عبر شبكة AdMob.`
                  : `Only ${timeLeft}s left to claim your free listing boost credit.`}
              </p>
              <div className="flex gap-2 pt-2">
                <button
                  onClick={() => setShowExitConfirm(false)}
                  className="flex-1 py-2 px-3 rounded-xl bg-amber-500 text-neutral-950 text-xs font-black cursor-pointer"
                >
                  {language === 'ar' ? 'متابعة المشاهدة' : 'Resume Video'}
                </button>
                <button
                  onClick={closeRewardedAd}
                  className="py-2 px-3 rounded-xl bg-neutral-800 text-neutral-400 text-xs font-semibold hover:text-white cursor-pointer"
                >
                  {language === 'ar' ? 'تخطي وخروج' : 'Exit Anyway'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Main Video / Sponsor Creative */}
        <div className="relative flex-1 flex flex-col justify-center items-center p-5 overflow-y-auto space-y-4">
          <div className="relative w-full aspect-video rounded-2xl overflow-hidden shadow-2xl border border-neutral-800 group bg-black">
            {ad.videoUrl ? (
              <video
                src={ad.videoUrl}
                autoPlay
                playsInline
                controls={false}
                className="w-full h-full object-cover"
              />
            ) : (
              <img
                src={ad.imageUrl}
                alt={ad.advertiser}
                className="w-full h-full object-cover brightness-90 group-hover:brightness-100 transition-all"
              />
            )}
            
            {/* Live streaming / Play indicator */}
            <div className="absolute top-3 start-3 flex items-center gap-2 bg-neutral-950/80 backdrop-blur-md px-3 py-1 rounded-full text-xs font-bold text-amber-300 border border-amber-400/30">
              <Sparkles className="w-3.5 h-3.5 text-amber-400 animate-spin" />
              <span>{ad.advertiser}</span>
              {ad.videoUrl && (
                <span className="text-[9px] bg-amber-500 text-neutral-950 px-1.5 py-0.2 rounded font-black">
                  VIDEO
                </span>
              )}
            </div>

            {/* Video overlay badge */}
            <div className="absolute bottom-3 end-3 bg-neutral-950/85 px-2.5 py-1 rounded-lg text-[10px] font-mono text-neutral-300 border border-neutral-800">
              {ad.videoUrl ? 'HD Video • AdMob Stream 🇸🇩' : 'HD Video Stream 🇸🇩'}
            </div>
          </div>

          <div className="text-center space-y-2 max-w-md">
            <h3 className="text-lg sm:text-xl font-black text-white">
              {language === 'ar' ? ad.titleAr : ad.titleEn}
            </h3>
            <p className="text-xs text-neutral-300 leading-relaxed">
              {language === 'ar' ? ad.descriptionAr : ad.descriptionEn}
            </p>
          </div>

          {/* Reward Status Card */}
          <div className="w-full max-w-md p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-between text-xs text-amber-300">
            <div className="flex items-center gap-2">
              <Gift className="w-4 h-4 text-amber-400 shrink-0" />
              <span>
                {isCompleted
                  ? (language === 'ar' ? 'تمت إضافة رصيد التمييز لحسابك بنجاح!' : 'Free boost credit added to your account!')
                  : (language === 'ar' ? 'مكافأة المشاهدة: تمييز إعلانك مجاناً' : 'Reward: Free ad boost badge')}
              </span>
            </div>
            <span className="font-mono font-bold text-white bg-amber-500/20 px-2 py-0.5 rounded">
              +1 Credit
            </span>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between gap-3">
          <button
            onClick={handleCtaClick}
            className="flex-1 py-3 px-5 rounded-2xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 text-neutral-950 font-black text-xs sm:text-sm shadow-xl flex items-center justify-center gap-2 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
          >
            <span>{language === 'ar' ? ad.ctaTextAr : ad.ctaTextEn}</span>
            <ExternalLink className="w-4 h-4" />
          </button>

          {isCompleted && (
            <button
              onClick={closeRewardedAd}
              className="py-3 px-5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs sm:text-sm transition-all cursor-pointer"
            >
              {language === 'ar' ? 'استلام وإغلاق' : 'Claim & Close'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
