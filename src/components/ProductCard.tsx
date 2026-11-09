import React from 'react';
import { 
  Heart, 
  MapPin, 
  ShieldCheck, 
  CreditCard, 
  Flame, 
  Sparkles, 
  Tag,
  CheckCircle2
} from 'lucide-react';
import { Product } from '../types';
import { useApp } from '../context/AppContext';

interface ProductCardProps {
  product: Product;
  onSelect?: () => void;
  footerActions?: React.ReactNode;
  showOfferButton?: boolean;
}

export const ProductCard: React.FC<ProductCardProps> = ({ 
  product, 
  onSelect, 
  footerActions,
  showOfferButton 
}) => {
  const { 
    currentUser,
    favorites, 
    toggleFavorite, 
    setSelectedProductForDetail, 
    setSelectedProductForOffer, 
    t, 
    language 
  } = useApp();

  const isFavorited = favorites.includes(product.id);
  const isOwner = currentUser?.id === product.sellerId;
  const shouldShowOffer = showOfferButton !== undefined ? showOfferButton : (!isOwner && !footerActions);

  const handleClick = () => {
    if (onSelect) {
      onSelect();
    } else {
      setSelectedProductForDetail(product);
    }
  };

  return (
    <div 
      onClick={handleClick}
      className="group relative bg-neutral-900 rounded-2xl overflow-hidden border border-neutral-800 hover:border-amber-500/50 shadow-md hover:shadow-xl hover:shadow-amber-500/10 transition-all duration-300 flex flex-col cursor-pointer"
    >
      {/* Thumbnail Aspect Box */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-neutral-950">
        <img
          src={product.images[0]}
          alt={language === 'ar' ? product.titleAr : product.titleEn}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
        />

        {/* Sold Overlay Banner */}
        {product.status === 'sold' && (
          <div className="absolute inset-0 bg-neutral-950/70 backdrop-blur-[1px] flex items-center justify-center p-2 z-10">
            <span className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-600 text-white font-black text-xs shadow-xl border border-emerald-400/50">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{language === 'ar' ? 'تم البيع ✓' : 'Sold ✓'}</span>
            </span>
          </div>
        )}

        {/* Favorite Button - Top Right (فوق في اليمين) */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            toggleFavorite(product.id);
          }}
          className={`absolute top-2 right-2 z-10 w-8 h-8 rounded-full border backdrop-blur-md flex items-center justify-center transition-all cursor-pointer shadow-md ${
            isFavorited
              ? 'bg-rose-600 border-rose-300 text-white shadow-rose-600/30'
              : 'bg-neutral-950/70 hover:bg-neutral-950/90 border-white/20 text-white hover:scale-105'
          }`}
          title={isFavorited ? t.removedFromFavorites : t.saveFavorite}
        >
          <Heart
            className={`w-4 h-4 transition-colors ${
              isFavorited ? 'fill-current text-white' : 'text-white'
            }`}
          />
        </button>

        {/* Badges on Image */}
        <div className="absolute bottom-2 start-2 flex items-center gap-1.5 flex-wrap">
          {product.urgentSale && (
            <span className="flex items-center gap-0.5 text-[10px] font-bold bg-rose-600 text-white px-2 py-0.5 rounded-full shadow-md">
              <Flame className="w-2.5 h-2.5 fill-current" />
              {t.urgentBadge}
            </span>
          )}

          {product.paymentMethods.includes('bankak') && (
            <span className="text-[10px] font-bold bg-emerald-600/90 text-white px-1.5 py-0.5 rounded-full shadow-md backdrop-blur-sm">
              بنكك
            </span>
          )}
        </div>
      </div>

      {/* Card Body */}
      <div className="p-3.5 flex flex-col flex-1 justify-between gap-2">
        <div>
          {/* Price & Condition */}
          <div className="flex items-baseline justify-between gap-1 mb-1">
            <div className="flex items-baseline gap-1">
              <span className="text-lg font-black text-amber-400">
                {product.price.toLocaleString()}
              </span>
              <span className="text-xs font-bold text-amber-300">
                {t.currency}
              </span>
            </div>

            <span className="text-[10px] text-neutral-400 bg-neutral-800 px-1.5 py-0.5 rounded">
              {t.conditions[product.condition]}
            </span>
          </div>

          {/* Title */}
          <h4 className="text-sm font-semibold text-neutral-100 group-hover:text-amber-300 transition-colors line-clamp-2 leading-snug">
            {language === 'ar' ? product.titleAr : product.titleEn}
          </h4>

          {/* Seller Name, Static Rating Mark and Verification Badge */}
          <div className="flex items-center gap-1.5 mt-1.5 text-[11px] text-neutral-400">
            <span className="font-medium text-neutral-300 truncate max-w-[120px]">
              {product.sellerName || 'بائع موثوق'}
            </span>
            <span className="text-[10px] text-amber-400 font-bold shrink-0">
              ★ {(product.sellerRating || 4.9).toFixed(1)}
            </span>
            {product.sellerVerified && (
              <span title={t.verifiedSeller}>
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              </span>
            )}
          </div>
        </div>

        {/* Footer: Location & Quick Offer Action */}
        <div className="pt-2 border-t border-neutral-800/80 flex items-center justify-between text-xs text-neutral-400">
          <div className="flex items-center gap-1 truncate max-w-[140px]">
            <MapPin className="w-3 h-3 text-amber-400/80 shrink-0" />
            <span className="truncate text-[11px] font-medium text-neutral-300">
              {product.city}
            </span>
          </div>

          {shouldShowOffer && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                setSelectedProductForOffer(product);
              }}
              className="text-[11px] font-bold text-amber-400 hover:text-amber-300 bg-amber-500/10 hover:bg-amber-500/20 px-2 py-1 rounded-lg border border-amber-500/20 transition-all cursor-pointer"
            >
              {t.makeOffer}
            </button>
          )}
        </div>

        {footerActions && (
          <div 
            onClick={(e) => e.stopPropagation()}
            className="pt-2.5 mt-2 border-t border-neutral-800/90 flex items-center gap-2"
          >
            {footerActions}
          </div>
        )}
      </div>
    </div>
  );
};
