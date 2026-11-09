import React from 'react';
import { CheckCircle2, Check, X } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Product } from '../types';

interface SoldConfirmModalProps {
  product: Product | { id: string; titleAr: string; titleEn?: string; price: number; images: string[] } | null;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export const SoldConfirmModal: React.FC<SoldConfirmModalProps> = ({
  product,
  isOpen,
  onClose,
  onConfirm,
}) => {
  const { t, language } = useApp();

  if (!isOpen || !product) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-950/85 backdrop-blur-md animate-in fade-in duration-150">
      <div className="relative w-full max-w-sm bg-neutral-900 border border-neutral-800 rounded-3xl p-5 sm:p-6 shadow-2xl text-center space-y-4">
        {/* Close icon button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-3.5 end-3.5 w-7 h-7 rounded-full bg-neutral-800/80 hover:bg-neutral-800 text-neutral-400 hover:text-white flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Header Icon */}
        <div className="w-14 h-14 mx-auto rounded-2xl bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shadow-inner">
          <CheckCircle2 className="w-7 h-7" />
        </div>

        {/* Title & User's Exact Prompt Message */}
        <div className="space-y-1.5">
          <h3 className="text-base font-bold text-white">
            {t.soldConfirmTitle}
          </h3>
          <p className="text-sm font-semibold text-neutral-200 leading-relaxed px-1">
            {language === 'ar' 
              ? 'هل تم بيع منتجك؟ هل تريد حذفه من الإعلانات؟'
              : 'Has your item been sold? Would you like to remove it from listings?'}
          </p>
        </div>

        {/* Product mini card preview */}
        <div className="flex items-center gap-3 p-3 rounded-2xl bg-neutral-950/80 border border-neutral-800 text-start">
          <img
            src={product.images[0] || 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&q=80'}
            alt={product.titleAr}
            className="w-12 h-12 rounded-xl object-cover shrink-0 border border-neutral-800"
          />
          <div className="min-w-0 flex-1">
            <h4 className="text-xs font-bold text-white truncate">
              {language === 'ar' ? product.titleAr : (product.titleEn || product.titleAr)}
            </h4>
            <div className="text-xs font-bold text-amber-400 font-mono mt-0.5">
              {product.price.toLocaleString()} {t.currency}
            </div>
          </div>
        </div>

        {/* Action Buttons: موافقة & إلغاء */}
        <div className="flex items-center gap-2.5 pt-1">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 px-4 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-bold transition-colors cursor-pointer"
          >
            {t.cancelBtn}
          </button>
          <button
            type="button"
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="flex-1 py-2.5 px-4 rounded-xl bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 text-white text-xs font-black transition-all shadow-lg shadow-emerald-950/40 cursor-pointer active:scale-95 flex items-center justify-center gap-1.5"
          >
            <Check className="w-4 h-4" />
            <span>{t.confirmSoldBtn}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
