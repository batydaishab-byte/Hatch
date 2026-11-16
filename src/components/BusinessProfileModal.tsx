import React from 'react';
import {
  X,
  Store,
  Building2,
  MapPin,
  Phone,
  Star,
  ShieldCheck,
  Package,
  CheckCircle2,
  Briefcase,
  MessageSquare,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { getCommercialStorefrontData } from '../utils/accountHelpers';
import { ProductCard } from './ProductCard';

export const BusinessProfileModal: React.FC = () => {
  const {
    selectedBusinessId,
    setSelectedBusinessId,
    availableUsers,
    products,
    currentUser,
    setSelectedProductForDetail,
    startOrOpenConversation,
    openReviewModal,
    t,
    language,
  } = useApp();

  if (!selectedBusinessId) return null;

  const { seller, accountType, activeProducts, soldProducts } =
    getCommercialStorefrontData(selectedBusinessId, availableUsers, products);

  if (!seller) return null;

  const isCompany = accountType === 'company';
  const logo = seller.businessLogo || seller.avatar;
  const isOwnPage = currentUser.id === seller.id;
  const isVerified =
    seller.verifiedIdentity ||
    (seller.dealsCompleted >= 10 && (!seller.reportsCount || seller.reportsCount === 0));

  const handleCall = () => {
    window.location.href = `tel:${seller.phone}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/90 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-4xl bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[100vh] sm:max-h-[92vh] flex flex-col animate-in fade-in zoom-in-95 duration-200">
        {/* Top Cover & Header */}
        <div className="relative bg-gradient-to-br from-neutral-950 via-neutral-900 to-amber-950/30 border-b border-neutral-800 p-5 sm:p-7 shrink-0">
          <div className="absolute top-0 end-0 w-56 h-56 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative flex items-start justify-between gap-4">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 text-center sm:text-start flex-1">
              <div className="relative shrink-0">
                <img
                  src={logo}
                  alt={seller.name}
                  className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl object-cover ring-3 ring-amber-400 shadow-2xl bg-neutral-950"
                />
                {isVerified && (
                  <div
                    className="absolute -bottom-1.5 -end-1.5 bg-emerald-500 text-white p-1 rounded-full border-2 border-neutral-900 shadow-md"
                    title={t.verifiedSeller}
                  >
                    <ShieldCheck className="w-4 h-4 stroke-[2.5]" />
                  </div>
                )}
              </div>

              <div className="flex-1 min-w-0 space-y-2">
                <div className="flex items-center justify-center sm:justify-start gap-2 flex-wrap">
                  <span
                    className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-black border ${
                      isCompany
                        ? 'bg-sky-500/15 text-sky-300 border-sky-500/40'
                        : 'bg-emerald-500/15 text-emerald-300 border-emerald-500/40'
                    }`}
                  >
                    {isCompany ? (
                      <>
                        <Building2 className="w-3.5 h-3.5" />
                        <span>{t.commercialBadgeCompany}</span>
                      </>
                    ) : (
                      <>
                        <Store className="w-3.5 h-3.5" />
                        <span>{t.commercialBadgeStore}</span>
                      </>
                    )}
                  </span>

                  {isVerified && (
                    <span className="inline-flex items-center gap-1 text-[11px] font-bold bg-emerald-950/80 text-emerald-400 border border-emerald-700/60 px-2.5 py-0.5 rounded-full">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      <span>
                        {language === 'ar' ? 'حساب موثق ✓' : 'Verified ✓'}
                      </span>
                    </span>
                  )}
                </div>

                <h2 className="text-xl sm:text-2xl font-black text-white leading-tight">
                  {seller.name}
                </h2>

                {seller.businessActivity && (
                  <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-amber-300 bg-amber-500/10 border border-amber-500/20 px-3 py-1 rounded-xl">
                    <Briefcase className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                    <span>{seller.businessActivity}</span>
                  </div>
                )}

                <div className="flex items-center justify-center sm:justify-start gap-3 text-xs text-neutral-300 flex-wrap pt-1">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-amber-400" />
                    {seller.city}
                  </span>
                  <span>•</span>
                  <button
                    type="button"
                    onClick={() =>
                      openReviewModal({
                        sellerId: seller.id,
                        sellerName: seller.name,
                        sellerAvatar: logo,
                        sellerRating: seller.rating,
                        sellerDealsCount: seller.dealsCompleted,
                        sellerVerified: isVerified,
                      })
                    }
                    className="inline-flex items-center gap-1 text-amber-400 font-bold hover:underline cursor-pointer"
                  >
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    <span>{(seller.rating || 5.0).toFixed(1)}</span>
                  </button>
                  <span>•</span>
                  <span className="flex items-center gap-1 text-neutral-300">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    {seller.dealsCompleted} {t.dealsDone}
                  </span>
                </div>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setSelectedBusinessId(null)}
              className="w-10 h-10 rounded-full bg-neutral-800/90 hover:bg-neutral-700 text-neutral-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer shrink-0 border border-neutral-700"
              aria-label={t.cancelBtn}
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Commercial Quick Contact Actions */}
          {!isOwnPage && (
            <div className="mt-4 pt-4 border-t border-neutral-800/80 flex items-center gap-2.5 flex-wrap">
              <button
                type="button"
                onClick={handleCall}
                className="px-4 py-2 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-300 border border-amber-500/40 text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <Phone className="w-3.5 h-3.5" />
                <span>{t.callSeller}</span>
              </button>

              {activeProducts[0] && (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedBusinessId(null);
                    startOrOpenConversation(activeProducts[0].id);
                  }}
                  className="px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white border border-neutral-700 text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer"
                >
                  <MessageSquare className="w-3.5 h-3.5 text-amber-400" />
                  <span>{t.chatSeller}</span>
                </button>
              )}
            </div>
          )}
        </div>

        {/* Scrollable Products Catalog */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-5 no-scrollbar">
          <div className="flex items-center justify-between">
            <h3 className="text-sm sm:text-base font-black text-white flex items-center gap-2">
              <Package className="w-4 h-4 text-amber-400" />
              <span>
                {t.commercialProductsTitle} {seller.name} ({activeProducts.length})
              </span>
            </h3>

            {soldProducts.length > 0 && (
              <span className="text-xs font-bold text-emerald-400 bg-emerald-950/60 border border-emerald-700/40 px-2.5 py-1 rounded-full">
                {soldProducts.length} {language === 'ar' ? 'صفقات مكتملة' : 'sold'}
              </span>
            )}
          </div>

          {activeProducts.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 sm:gap-4">
              {activeProducts.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  onSelect={() => {
                    setSelectedBusinessId(null);
                    setSelectedProductForDetail(product);
                  }}
                />
              ))}
            </div>
          ) : (
            <div className="text-center py-14 bg-neutral-950/60 rounded-2xl border border-neutral-800 text-neutral-400 text-xs">
              {t.noCommercialProducts}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
