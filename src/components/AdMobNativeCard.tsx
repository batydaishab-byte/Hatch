import React, { useEffect } from 'react';
import { ExternalLink, Sparkles, Megaphone } from 'lucide-react';
import { SponsoredAdItem } from '../types';
import { useApp } from '../context/AppContext';

interface AdMobNativeCardProps {
  ad: SponsoredAdItem;
}

export const AdMobNativeCard: React.FC<AdMobNativeCardProps> = ({ ad }) => {
  const { 
    adMobConfig, 
    recordAdImpression, 
    recordAdClick, 
    language 
  } = useApp();

  useEffect(() => {
    if (adMobConfig.isEnabled && ad.isActive !== false) {
      recordAdImpression('native', ad.id);
    }
  }, [ad.id, adMobConfig.isEnabled, ad.isActive]);

  const handleClick = () => {
    recordAdClick('native', ad.id);
    if (ad.targetUrl) {
      window.open(ad.targetUrl, '_blank', 'noopener,noreferrer');
    }
  };

  if (!adMobConfig.isEnabled || ad.isActive === false) {
    return null;
  }

  return (
    <div 
      onClick={handleClick}
      className="group relative bg-gradient-to-b from-neutral-900 to-neutral-950 rounded-2xl overflow-hidden border border-amber-500/30 hover:border-amber-400 shadow-md hover:shadow-xl hover:shadow-amber-500/10 transition-all duration-300 flex flex-col cursor-pointer"
    >
      {/* Thumbnail Aspect Box */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-neutral-950">
        <img
          src={ad.imageUrl}
          alt={ad.advertiser}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
        />

        {/* AdMob Sponsored Badge */}
        <div className="absolute top-2 start-2 flex items-center gap-1.5 z-10">
          <span className="flex items-center gap-1 text-[10px] font-black bg-amber-500 text-neutral-950 px-2 py-0.5 rounded-full shadow-md">
            <Megaphone className="w-2.5 h-2.5 fill-neutral-950" />
            <span>AdMob</span>
          </span>
          <span className="text-[10px] font-bold bg-neutral-950/80 text-amber-300 border border-amber-400/30 px-2 py-0.5 rounded-full backdrop-blur-sm shadow-md">
            {language === 'ar' ? 'إعلان مدعوم' : 'Sponsored'}
          </span>
        </div>
      </div>

      {/* Card Body */}
      <div className="p-3.5 flex flex-col flex-1 justify-between gap-2">
        <div>
          {/* Advertiser info */}
          <div className="flex items-center justify-between text-xs mb-1">
            <span className="font-bold text-amber-400 truncate">
              {ad.advertiser}
            </span>
            <span className="text-[10px] text-neutral-400 bg-neutral-800 px-1.5 py-0.5 rounded">
              Ad
            </span>
          </div>

          {/* Title */}
          <h4 className="text-sm font-semibold text-white group-hover:text-amber-300 transition-colors line-clamp-2 leading-snug">
            {language === 'ar' ? ad.titleAr : ad.titleEn}
          </h4>

          {/* Description */}
          <p className="text-xs text-neutral-400 line-clamp-2 mt-1 leading-relaxed">
            {language === 'ar' ? ad.descriptionAr : ad.descriptionEn}
          </p>
        </div>

        {/* Footer CTA */}
        <div className="pt-2 border-t border-neutral-800 flex items-center justify-between text-xs">
          <span className="text-[10px] text-neutral-500">
            {language === 'ar' ? 'شركاء هتش المعتمدين' : 'HATCH Partner'}
          </span>

          <button
            type="button"
            className="text-[11px] font-black text-neutral-950 bg-amber-400 hover:bg-amber-300 px-3 py-1 rounded-lg flex items-center gap-1 transition-all group-hover:scale-105"
          >
            <span>{language === 'ar' ? ad.ctaTextAr : ad.ctaTextEn}</span>
            <ExternalLink className="w-3 h-3" />
          </button>
        </div>
      </div>
    </div>
  );
};
