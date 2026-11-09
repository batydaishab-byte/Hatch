import React, { useState, useEffect } from 'react';

interface SplashScreenProps {
  onFinish?: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onFinish }) => {
  const [isVisible, setIsVisible] = useState(true);
  const [isFading, setIsFading] = useState(false);
  const [progress, setProgress] = useState(15);

  const logoSrc = '/assets/aistudio/file_000000007ae48210a2463d89f629054c_b808c85d.png';

  useEffect(() => {
    // Smooth progress simulation
    const timer1 = setTimeout(() => setProgress(45), 300);
    const timer2 = setTimeout(() => setProgress(80), 750);
    const timer3 = setTimeout(() => setProgress(100), 1200);

    // Start fade out
    const fadeTimer = setTimeout(() => {
      setIsFading(true);
    }, 1500);

    // Completely unmount splash
    const removeTimer = setTimeout(() => {
      setIsVisible(false);
      if (onFinish) onFinish();
    }, 1900);

    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
      clearTimeout(fadeTimer);
      clearTimeout(removeTimer);
    };
  }, [onFinish]);

  if (!isVisible) return null;

  return (
    <div
      onClick={() => {
        setIsFading(true);
        setTimeout(() => setIsVisible(false), 300);
      }}
      className={`fixed inset-0 z-[100] flex flex-col items-center justify-between p-6 bg-neutral-950 transition-opacity duration-500 select-none cursor-pointer ${
        isFading ? 'opacity-0 pointer-events-none' : 'opacity-100'
      }`}
    >
      {/* Background Ambience */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_rgba(245,158,11,0.15)_0%,_transparent_70%)] pointer-events-none" />

      {/* Top spacing */}
      <div className="w-full pt-4 flex justify-end">
        <span className="text-[11px] font-bold text-neutral-500 hover:text-neutral-300 transition-colors">
          تخطي ✕
        </span>
      </div>

      {/* Main Center Content */}
      <div className="relative flex flex-col items-center justify-center text-center space-y-6 z-10 max-w-sm px-4">
        {/* Brand Logo Container with Glowing Ring */}
        <div className="relative group">
          <div className="absolute -inset-2 bg-gradient-to-tr from-amber-500 to-yellow-300 rounded-[36px] blur-xl opacity-60 animate-pulse" />
          
          <div className="relative w-36 h-36 sm:w-44 sm:h-44 rounded-[32px] overflow-hidden bg-neutral-900 border-2 border-amber-400/60 shadow-2xl shadow-amber-500/20 transform transition-transform hover:scale-105 duration-300">
            <img
              src={logoSrc}
              alt="HATCH — هتش"
              className="w-full h-full object-cover"
              loading="eager"
            />
          </div>
        </div>

        {/* Brand Title and Tagline */}
        <div className="space-y-2">
          <div className="flex items-center justify-center gap-2">
            <h1 className="text-3xl sm:text-4xl font-black tracking-tight bg-gradient-to-r from-amber-400 via-yellow-200 to-amber-500 bg-clip-text text-transparent">
              هَـتـْـش
            </h1>
            <span className="text-xs font-black bg-amber-500/10 text-amber-400 border border-amber-500/30 px-2 py-0.5 rounded-full">
              HATCH SUDAN 🇸🇩
            </span>
          </div>

          <p className="text-xs sm:text-sm text-neutral-300 font-medium">
            سوق الإعلانات والمنتجات في السودان • تصفح وفاوض واشترِ بذكاء
          </p>
        </div>

        {/* Progress Bar */}
        <div className="w-48 sm:w-56 space-y-2 pt-2">
          <div className="w-full h-1.5 bg-neutral-800/80 rounded-full overflow-hidden p-0.5 border border-neutral-700/60">
            <div
              className="h-full bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 rounded-full transition-all duration-300 ease-out shadow-sm shadow-amber-400/50"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-[11px] text-neutral-400 font-medium">
            جاري فتح هتش...
          </p>
        </div>
      </div>

      {/* Footer Info */}
      <div className="w-full pb-4 text-center text-[11px] text-neutral-400 z-10 flex items-center justify-center gap-2">
        <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping inline-block" />
        <span>بورتسودان • الخرطوم • عطبرة • أم درمان • كسلا • ود مدني</span>
      </div>
    </div>
  );
};
