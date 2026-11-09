import React, { useState, useEffect } from 'react';
import { X, Zap, Send } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const MakeOfferModal: React.FC = () => {
  const { 
    selectedProductForOffer, 
    setSelectedProductForOffer, 
    submitOffer, 
    t, 
    language 
  } = useApp();

  const [offerAmount, setOfferAmount] = useState<number>(0);
  const [note, setNote] = useState('');

  // Synchronize default offer amount whenever the selected product changes
  useEffect(() => {
    if (selectedProductForOffer) {
      const defaultSuggested = Math.round((selectedProductForOffer.price * 0.9) / 500) * 500;
      setOfferAmount(defaultSuggested);
      setNote('');
    }
  }, [selectedProductForOffer]);

  if (!selectedProductForOffer) return null;

  const product = selectedProductForOffer;
  const discountPercentages = [5, 10, 15, 20];

  const handleApplyDiscount = (pct: number) => {
    const discounted = Math.round((product.price * (1 - pct / 100)) / 500) * 500;
    setOfferAmount(discounted);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (offerAmount <= 0) return;
    submitOffer(product.id, offerAmount, note);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-neutral-950/85 backdrop-blur-md">
      <div className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden p-5 sm:p-6 animate-in fade-in zoom-in-95 duration-200">
        {/* Close Button */}
        <button
          onClick={() => setSelectedProductForOffer(null)}
          className="absolute top-4 end-4 w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
            <Zap className="w-6 h-6 fill-amber-400" />
          </div>
          <div>
            <h3 className="text-lg font-black text-white">
              {t.offerTitle}
            </h3>
            <p className="text-xs text-neutral-400 truncate max-w-[280px]">
              {language === 'ar' ? product.titleAr : product.titleEn}
            </p>
          </div>
        </div>

        {/* Current Asking Price info */}
        <div className="p-3 bg-neutral-950 rounded-2xl border border-neutral-800 flex items-center justify-between mb-4 text-xs">
          <span className="text-neutral-400">{t.currentPrice}</span>
          <span className="text-base font-bold text-neutral-200">
            {product.price.toLocaleString()} <span className="text-amber-400 font-bold">{t.currency}</span>
          </span>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Quick Discount Chips */}
          <div>
            <label className="block text-xs font-semibold text-neutral-400 mb-2">
              {t.suggestedOffers}
            </label>
            <div className="grid grid-cols-4 gap-2">
              {discountPercentages.map((pct) => {
                const calculated = Math.round((product.price * (1 - pct / 100)) / 500) * 500;
                const isSelected = offerAmount === calculated;
                return (
                  <button
                    type="button"
                    key={pct}
                    onClick={() => handleApplyDiscount(pct)}
                    className={`py-2 px-1 rounded-xl text-xs font-bold border transition-all cursor-pointer text-center ${
                      isSelected
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md shadow-amber-500/20'
                        : 'bg-neutral-800/80 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
                    }`}
                  >
                    <span>-{pct}%</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Amount input */}
          <div>
            <label className="block text-xs font-semibold text-neutral-300 mb-1.5">
              {t.enterOfferAmount}
            </label>
            <div className="relative">
              <input
                type="number"
                value={offerAmount || ''}
                onChange={(e) => setOfferAmount(Number(e.target.value))}
                min="1000"
                step="500"
                required
                className="w-full bg-neutral-950 border border-neutral-700 focus:border-amber-500 rounded-xl py-3 px-4 text-xl font-black text-amber-400 focus:outline-none tracking-tight font-mono"
              />
              <span className="absolute top-1/2 -translate-y-1/2 end-4 text-sm font-bold text-neutral-400">
                {t.currency}
              </span>
            </div>
          </div>

          {/* Optional Note */}
          <div>
            <label className="block text-xs font-semibold text-neutral-400 mb-1.5">
              {language === 'ar' ? 'رسالة مرفقة مع العرض (اختياري)' : 'Message with offer (Optional)'}
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={t.offerNotePlaceholder}
              rows={2}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl p-3 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500/70"
            />
          </div>

          {/* Action buttons */}
          <div className="pt-2 flex items-center gap-3">
            <button
              type="button"
              onClick={() => setSelectedProductForOffer(null)}
              className="w-1/3 py-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-semibold text-xs border border-neutral-700 transition-colors cursor-pointer"
            >
              {language === 'ar' ? 'إلغاء' : 'Cancel'}
            </button>

            <button
              type="submit"
              className="w-2/3 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-xs sm:text-sm shadow-lg shadow-amber-500/20 flex items-center justify-center gap-1.5 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
            >
              <Send className="w-4 h-4" />
              <span>{t.sendOfferBtn}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
