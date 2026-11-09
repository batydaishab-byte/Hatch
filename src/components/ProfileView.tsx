import React, { useState } from 'react';
import { 
  User, 
  ShieldCheck, 
  MapPin, 
  Phone, 
  Star, 
  Package, 
  Heart, 
  Shield, 
  CheckCircle, 
  Users, 
  ExternalLink, 
  Lock, 
  AlertTriangle,
  Smartphone,
  Download,
  Pencil,
  CheckCircle2,
  Check,
  UserPlus,
  Clock
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Product } from '../types';
import { ProductCard } from './ProductCard';
import { EditListingModal } from './EditListingModal';
import { SoldConfirmModal } from './SoldConfirmModal';

export const ProfileView: React.FC = () => {
  const { 
    currentUser, 
    availableUsers, 
    switchUser, 
    products, 
    favorites, 
    deleteProduct,
    markProductAsSold,
    setIsSafetyModalOpen,
    setIsApkModalOpen,
    setIsCreateAccountOpen,
    setIsEditProfileOpen,
    openReviewModal,
    t, 
    language 
  } = useApp();

  const [activeSubTab, setActiveSubTab] = useState<'my_ads' | 'sold_ads' | 'saved' | 'safety'>('my_ads');
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [soldConfirmProduct, setSoldConfirmProduct] = useState<Product | null>(null);

  const myActiveAds = products.filter((p) => p.sellerId === currentUser.id && p.status !== 'sold');
  const mySoldAds = products.filter((p) => p.sellerId === currentUser.id && p.status === 'sold');
  const favoritedProducts = products.filter((p) => favorites.includes(p.id));
  const isVerified = currentUser.dealsCompleted >= 10 && (!currentUser.reportsCount || currentUser.reportsCount === 0);

  return (
    <div className="w-full max-w-5xl mx-auto px-3 sm:px-6 py-4 pb-24 space-y-6">
      {/* Persona Switcher Banner (Crucial for testing all marketplace roles) */}
      <div className="p-3.5 bg-neutral-900 border border-neutral-800 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-3 shadow-md">
        <div className="flex items-center gap-2 text-xs text-neutral-300">
          <Users className="w-4 h-4 text-amber-400" />
          <span className="font-semibold">{t.switchPersona}:</span>
        </div>

        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar w-full sm:w-auto">
          {availableUsers.map((user) => (
            <button
              key={user.id}
              onClick={() => switchUser(user.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all cursor-pointer whitespace-nowrap ${
                currentUser.id === user.id
                  ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md font-bold'
                  : 'bg-neutral-800/80 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
              }`}
            >
              <img
                src={user.avatar}
                alt={user.name}
                className="w-4 h-4 rounded-full object-cover"
              />
              <span>{user.name.split(' ')[0]}</span>
              {user.status === 'suspended' && (
                <span className="text-[10px] bg-red-600/90 text-white font-bold px-1.5 py-0.2 rounded-full animate-pulse">
                  {language === 'ar' ? 'موقوف' : 'Suspended'}
                </span>
              )}
              {user.role === 'admin' && (
                <span className="text-[10px] bg-red-900/60 text-red-300 px-1 rounded">Admin</span>
              )}
            </button>
          ))}

          {/* Create Account Button in Switcher */}
          <button
            type="button"
            onClick={() => setIsCreateAccountOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold border border-dashed border-amber-500/60 bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 transition-all cursor-pointer whitespace-nowrap"
            title={language === 'ar' ? 'إنشاء حساب جديد' : 'Create New Account'}
          >
            <UserPlus className="w-3.5 h-3.5 text-amber-400" />
            <span>{t.nearAccountBadge} +</span>
          </button>
        </div>
      </div>

      {/* شريط وتنبيه إيقاف الحساب من قبل الإدارة مع السبب والمدة */}
      {currentUser.status === 'suspended' && (
        <div className="p-5 sm:p-6 rounded-3xl bg-gradient-to-r from-red-950/80 via-rose-950/60 to-neutral-900 border-2 border-red-500/80 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 end-0 w-64 h-64 bg-red-500/10 rounded-full blur-3xl pointer-events-none" />
          <div className="flex flex-col sm:flex-row items-start gap-4 relative z-10">
            <div className="w-12 h-12 rounded-2xl bg-red-500/20 text-red-400 border border-red-500/40 flex items-center justify-center shrink-0 shadow-lg">
              <AlertTriangle className="w-7 h-7 text-red-400" />
            </div>

            <div className="flex-1 space-y-2.5 text-start">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="px-3 py-1 rounded-full text-xs font-black bg-red-600 text-white shadow-md">
                  {language === 'ar' ? '⚠️ تنبيه رسمي: تم إيقاف حسابك' : '⚠️ Official: Account Suspended'}
                </span>
                {currentUser.suspensionDays && (
                  <span className="text-xs font-bold text-red-200 bg-red-950/90 px-2.5 py-0.5 rounded-full border border-red-700">
                    {language === 'ar'
                      ? `المدة: ${currentUser.suspensionDays} ${currentUser.suspensionDays === 1 ? 'يوم' : currentUser.suspensionDays === 2 ? 'يومين' : currentUser.suspensionDays <= 10 ? 'أيام' : 'يوماً'}`
                      : `Duration: ${currentUser.suspensionDays} days`}
                  </span>
                )}
                {currentUser.suspendedUntil && (
                  <span className="text-xs text-neutral-300 flex items-center gap-1 font-semibold">
                    <Clock className="w-3.5 h-3.5 text-red-400" />
                    <span>
                      {language === 'ar' ? 'تاريخ انتهاء الإيقاف:' : 'Suspended until:'}{' '}
                      <strong className="text-white underline decoration-red-500/50">
                        {new Date(currentUser.suspendedUntil).toLocaleDateString(
                          language === 'ar' ? 'ar-SD' : 'en-US',
                          { year: 'numeric', month: 'long', day: 'numeric' }
                        )}
                      </strong>
                    </span>
                  </span>
                )}
              </div>

              <div className="p-3.5 rounded-2xl bg-neutral-950/80 border border-red-500/40 text-xs sm:text-sm text-neutral-200 space-y-1">
                <div className="font-bold text-red-400 flex items-center gap-1.5">
                  <span>{language === 'ar' ? 'سبب الإيقاف الصادر من الإدارة:' : 'Suspension Reason from Admin:'}</span>
                </div>
                <p className="font-semibold text-white leading-relaxed text-sm">
                  "{currentUser.suspensionReason || (language === 'ar' ? 'مخالفة معايير الأمان والشروط في منصة هتش' : 'Community safety violation')}"
                </p>
              </div>

              <p className="text-xs text-neutral-300 leading-relaxed">
                {language === 'ar'
                  ? 'تم تقييد صلاحيات حسابك مؤقتاً (لا يمكن نشر إعلانات جديدة أو تقديم عروض شراء عبر بنكك). بعد انتهاء المدة سيتم استعادة كامل الميزات تلقائياً، أو يمكنك التواصل مع الإدارة للاستئناف.'
                  : 'Account capabilities are restricted (cannot post new ads or send purchase offers). Restrictions will lift automatically after the duration ends.'}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* User Card Profile Header */}
      <div className="bg-gradient-to-br from-neutral-900 via-neutral-900 to-neutral-950 border border-neutral-800 rounded-3xl p-5 sm:p-7 shadow-xl">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-5 text-center sm:text-start">
          <div className="relative">
            <img
              src={currentUser.avatar}
              alt={currentUser.name}
              className="w-24 h-24 rounded-full object-cover ring-4 ring-amber-400 shadow-2xl"
            />
            {isVerified && (
              <div 
                className="absolute bottom-0 end-0 bg-emerald-500 text-white p-1 rounded-full border-2 border-neutral-900 shadow-md"
                title={t.verifiedSeller}
              >
                <ShieldCheck className="w-4 h-4 stroke-[2.5]" />
              </div>
            )}
            {/* Quick avatar badge for create account */}
            <button
              type="button"
              onClick={() => setIsCreateAccountOpen(true)}
              className="absolute -top-1.5 -start-1.5 bg-neutral-900 border border-amber-400 text-amber-300 hover:text-white p-1 rounded-full shadow-lg hover:scale-110 transition-transform cursor-pointer"
              title={language === 'ar' ? 'إنشاء حساب جديد' : 'Create New Account'}
            >
              <UserPlus className="w-3.5 h-3.5 text-amber-400" />
            </button>

            {/* Quick avatar badge for edit profile */}
            <button
              type="button"
              onClick={() => setIsEditProfileOpen(true)}
              className="absolute -bottom-1 -start-1 bg-neutral-900 border border-neutral-700 hover:border-amber-400 text-neutral-300 hover:text-amber-400 p-1.5 rounded-full shadow-lg hover:scale-110 transition-all cursor-pointer"
              title={t.editProfile}
            >
              <Pencil className="w-3 h-3 text-amber-400" />
            </button>
          </div>

          <div className="flex-1 space-y-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <div className="flex items-center justify-center sm:justify-start gap-2 flex-wrap">
                  <h2 className="text-xl sm:text-2xl font-black text-white flex items-center gap-2 flex-wrap">
                    {currentUser.name}
                    {isVerified ? (
                      <span className="text-xs bg-emerald-950/80 text-emerald-400 border border-emerald-800 px-2.5 py-0.5 rounded-full font-bold flex items-center gap-1 shadow-sm">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        <span>{language === 'ar' ? 'موثق (10+ صفقات)' : 'Verified (10+ Deals)'}</span>
                      </span>
                    ) : (
                      <span className="text-xs bg-amber-500/10 text-amber-300 border border-amber-500/30 px-2 py-0.5 rounded-full font-bold flex items-center gap-1">
                        <Shield className="w-3.5 h-3.5 text-amber-400" />
                        <span>{language === 'ar' ? 'قيد التوثيق' : 'In Progress'}</span>
                      </span>
                    )}
                  </h2>

                  {/* علامة تعديل الملف الشخصي لتعديل بيانات الحساب */}
                  <button
                    type="button"
                    onClick={() => setIsEditProfileOpen(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-neutral-800 hover:bg-neutral-700 text-neutral-200 hover:text-white border border-neutral-700 hover:border-amber-400/60 shadow-sm transition-all hover:scale-105 active:scale-95 cursor-pointer"
                    title={t.editProfile}
                  >
                    <Pencil className="w-3.5 h-3.5 text-amber-400" />
                    <span>{t.editProfile}</span>
                  </button>

                  {/* علامة إنشاء حساب بالقرب من حساب المستخدم */}
                  <button
                    type="button"
                    onClick={() => setIsCreateAccountOpen(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-neutral-950 shadow-md shadow-amber-500/25 border border-amber-400 transition-all hover:scale-105 active:scale-95 cursor-pointer"
                    title={language === 'ar' ? 'إنشاء حساب جديد' : 'Create New Account'}
                  >
                    <UserPlus className="w-3.5 h-3.5 stroke-[2.5]" />
                    <span>{t.nearAccountBadge}</span>
                  </button>
                </div>
                <div className="flex items-center justify-center sm:justify-start gap-3 text-xs text-neutral-400 mt-1">
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-amber-400" />
                    {currentUser.city}
                  </span>
                  <span>•</span>
                  <span className="flex items-center gap-1">
                    <Phone className="w-3.5 h-3.5 text-amber-400" />
                    {currentUser.phone}
                  </span>
                </div>
              </div>

              {/* Trust & Deals Counter */}
              <div className="flex items-center justify-center gap-3 bg-neutral-950/80 p-2.5 rounded-2xl border border-neutral-800">
                <button
                  type="button"
                  onClick={() => openReviewModal({
                    sellerId: currentUser.id,
                    sellerName: currentUser.name,
                    sellerAvatar: currentUser.avatar,
                    sellerRating: currentUser.rating,
                    sellerDealsCount: currentUser.dealsCompleted,
                    sellerVerified: isVerified,
                  })}
                  className="text-center px-2 hover:bg-neutral-900 rounded-xl transition-all cursor-pointer group"
                  title={language === 'ar' ? 'عرض تقييمات الحساب' : 'View account reviews'}
                >
                  <div className="text-sm font-extrabold text-amber-400 flex items-center justify-center gap-1 group-hover:scale-105 transition-transform">
                    <Star className="w-3.5 h-3.5 fill-amber-400" />
                    {(currentUser.rating || 5.0).toFixed(1)}
                  </div>
                  <div className="text-[10px] text-neutral-400 group-hover:text-amber-300 underline decoration-amber-500/30">{t.sellerScore}</div>
                </button>

                <div className="h-6 w-px bg-neutral-800" />

                <div className="text-center px-2">
                  <div className="text-sm font-extrabold text-white">
                    {currentUser.dealsCompleted}
                  </div>
                  <div className="text-[10px] text-neutral-400">{t.dealsDone}</div>
                </div>
              </div>
            </div>

            {/* Verification Status Banner based on 10 trusted sales with zero reports */}
            {isVerified ? (
              <div className="text-xs bg-emerald-950/40 p-3 rounded-2xl border border-emerald-500/30 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-start">
                <div className="flex items-center gap-2 text-emerald-300 min-w-0">
                  <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span className="font-semibold leading-relaxed">
                    {language === 'ar'
                      ? 'حساب موثق وموثوق: أنجز أكثر من 10 صفقات ناجحة بسجل ناصع وبدون أي بلاغ أو مخالفة.'
                      : 'Verified Account: Completed 10+ trusted sales with a clean record and zero violations.'}
                  </span>
                </div>
                <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/80 border border-emerald-600/40 px-2 py-0.5 rounded-lg shrink-0 flex items-center gap-1 self-start sm:self-auto">
                  <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                  <span>{language === 'ar' ? 'سجل ناصع (0 بلاغات)' : '0 Reports'}</span>
                </span>
              </div>
            ) : (
              <div className="text-xs bg-neutral-950/90 p-3 rounded-2xl border border-neutral-800 space-y-2 text-start">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
                  <div className="flex items-center gap-2 text-neutral-200">
                    <Shield className="w-4 h-4 text-amber-400 shrink-0" />
                    <span className="font-bold text-white">
                      {language === 'ar' ? 'شروط التوثيق في هتش (10 صفقات موثوقة بدون مخالفات)' : 'HATCH Verification: 10 Trusted Deals & Zero Violations'}
                    </span>
                  </div>
                  <span className="text-[11px] font-mono text-amber-400 font-bold bg-amber-500/10 px-2 py-0.5 rounded-lg border border-amber-500/20 self-start sm:self-auto">
                    {currentUser.dealsCompleted} / 10 {language === 'ar' ? 'صفقات مكتملة' : 'completed'}
                  </span>
                </div>
                <p className="text-[11px] text-neutral-400 leading-relaxed">
                  {language === 'ar'
                    ? 'التوثيق ليس بالهوية، بل يظهر تلقائياً بعد إتمام 10 عمليات بيع موثوقة بدون أي إبلاغ أو مخالفة للبائع أو المشتري.'
                    : 'Verification does not require ID; it is automatically unlocked after completing 10 trusted sales with zero reports or violations for seller or buyer.'}
                </p>
                {/* Visual Progress Bar */}
                <div className="w-full h-1.5 bg-neutral-800 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-gradient-to-r from-amber-500 to-emerald-400 transition-all duration-500 rounded-full"
                    style={{ width: `${Math.min(100, (currentUser.dealsCompleted / 10) * 100)}%` }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Android APK Banner in Profile */}
      <div className="p-4 rounded-2xl bg-gradient-to-r from-amber-950/40 via-neutral-900 to-neutral-900 border border-amber-500/30 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-lg">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 flex items-center justify-center shrink-0">
            <Smartphone className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-white flex items-center gap-2">
              {language === 'ar' ? 'نسخة تطبيق أندرويد (APK)' : 'Android Application (APK)'}
              <span className="text-[10px] bg-emerald-950 text-emerald-400 border border-emerald-800 px-2 py-0.5 rounded-full font-mono">
                WebAPK
              </span>
            </h4>
            <p className="text-xs text-neutral-400">
              {language === 'ar'
                ? 'ثبّت التطبيق مباشرة على شاشة هاتفك المحمول أو قم بتصدير حزمة APK مستقلة'
                : 'Install to Android home screen or export standalone APK'}
            </p>
          </div>
        </div>
        <button
          onClick={() => setIsApkModalOpen(true)}
          className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 font-black text-xs flex items-center justify-center gap-1.5 shadow-md shadow-amber-500/20 hover:scale-[1.02] cursor-pointer transition-all shrink-0"
        >
          <Download className="w-4 h-4" />
          <span>{language === 'ar' ? 'تثبيت / تحميل APK' : 'Install / Download APK'}</span>
        </button>
      </div>

      {/* Sub Tabs Bar */}
      <div className="flex items-center gap-2 border-b border-neutral-800 pb-2 overflow-x-auto no-scrollbar">
        <button
          onClick={() => setActiveSubTab('my_ads')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
            activeSubTab === 'my_ads'
              ? 'bg-amber-500 text-neutral-950 shadow-md'
              : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
          }`}
        >
          <Package className="w-4 h-4" />
          <span>{t.myActiveAds} ({myActiveAds.length})</span>
        </button>

        {/* زر يعرض الإعلانات التي تم بيعها */}
        <button
          onClick={() => setActiveSubTab('sold_ads')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
            activeSubTab === 'sold_ads'
              ? 'bg-emerald-500 text-neutral-950 shadow-md'
              : 'text-neutral-400 hover:text-emerald-400 hover:bg-neutral-800'
          }`}
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>{t.soldAdsTitle} ({mySoldAds.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('saved')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
            activeSubTab === 'saved'
              ? 'bg-amber-500 text-neutral-950 shadow-md'
              : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
          }`}
        >
          <Heart className="w-4 h-4" />
          <span>{t.mySavedAds} ({favoritedProducts.length})</span>
        </button>

        <button
          onClick={() => setActiveSubTab('safety')}
          className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap ${
            activeSubTab === 'safety'
              ? 'bg-amber-500 text-neutral-950 shadow-md'
              : 'text-neutral-400 hover:text-white hover:bg-neutral-800'
          }`}
        >
          <Shield className="w-4 h-4" />
          <span>{t.safetyShield}</span>
        </button>
      </div>

      {/* Sub Tab Content */}
      {activeSubTab === 'my_ads' && (
        <div>
          {myActiveAds.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
              {myActiveAds.map((product) => (
                <ProductCard 
                  key={product.id} 
                  product={product} 
                  showOfferButton={false}
                  footerActions={
                    <div className="flex items-center gap-1.5 w-full">
                      {/* زر تعديل الإعلان */}
                      <button
                        type="button"
                        onClick={() => setEditingProduct(product)}
                        className="flex-1 py-1.5 px-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 hover:text-amber-300 border border-neutral-700/80 text-[11px] font-bold transition-all flex items-center justify-center gap-1 cursor-pointer active:scale-95 shadow-sm"
                        title={t.editAd}
                      >
                        <Pencil className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                        <span>{t.editAd}</span>
                      </button>

                      {/* زر تم البيع */}
                      <button
                        type="button"
                        onClick={() => setSoldConfirmProduct(product)}
                        className="flex-1 py-1.5 px-2 rounded-xl bg-emerald-950/70 hover:bg-emerald-600 text-emerald-400 hover:text-white border border-emerald-800/80 text-[11px] font-bold transition-all flex items-center justify-center gap-1 cursor-pointer active:scale-95 shadow-sm"
                        title={t.markAsSold}
                      >
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                        <span>{t.markAsSold}</span>
                      </button>
                    </div>
                  }
                />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 bg-neutral-900/50 rounded-2xl border border-neutral-800 text-neutral-400 text-xs">
              {language === 'ar' ? 'ليس لديك إعلانات نشطة حالياً بهذا الحساب' : 'No active listings for this persona yet'}
            </div>
          )}
        </div>
      )}

      {/* عرض الإعلانات التي تم بيعها */}
      {activeSubTab === 'sold_ads' && (
        <div className="space-y-4">
          <div className="p-3.5 rounded-2xl bg-emerald-950/30 border border-emerald-500/30 flex items-center justify-between text-xs text-emerald-300">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>
                {language === 'ar' 
                  ? 'سجل الإعلانات التي تم بيعها بنجاح وحسابها ضمن الصفقات المكتملة الموثوقة' 
                  : 'Archive of successfully completed sales contributing to your verified status'}
              </span>
            </div>
            <span className="font-mono font-bold text-white bg-emerald-900/60 px-2 py-0.5 rounded-lg border border-emerald-700/60">
              {mySoldAds.length} {language === 'ar' ? 'مبيعات' : 'deals'}
            </span>
          </div>

          {mySoldAds.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
              {mySoldAds.map((product) => (
                <ProductCard 
                  key={product.id} 
                  product={product} 
                  showOfferButton={false}
                  footerActions={
                    <div className="w-full py-1.5 px-2 rounded-xl bg-emerald-950/70 border border-emerald-800/60 text-emerald-300 text-xs font-bold flex items-center justify-center gap-1.5">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                      <span>{t.soldBadge}</span>
                    </div>
                  }
                />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 bg-neutral-900/50 rounded-2xl border border-neutral-800 text-neutral-400 text-xs">
              {language === 'ar' ? 'لم تقم بتسجيل أي إعلانات مباعة حتى الآن' : 'No sold listings archived yet'}
            </div>
          )}
        </div>
      )}

      {activeSubTab === 'saved' && (
        <div>
          {favoritedProducts.length > 0 ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-4">
              {favoritedProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 bg-neutral-900/50 rounded-2xl border border-neutral-800 text-neutral-400 text-xs">
              {language === 'ar' ? 'لم تقم بحفظ أي منتجات في قائمتك بعد' : 'No items saved to your wishlist yet'}
            </div>
          )}
        </div>
      )}

      {activeSubTab === 'safety' && (
        <div className="space-y-4">
          <div className="p-5 rounded-2xl bg-neutral-900 border border-neutral-800 space-y-4">
            <h3 className="font-bold text-sm text-amber-400 flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              {t.safetyNoticeTitle}
            </h3>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div className="p-3.5 rounded-xl bg-neutral-950 border border-neutral-800 space-y-2">
                <div className="font-bold text-white flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-xs">1</span>
                  {language === 'ar' ? 'تأكيد إشعار بنكك الرسمي' : 'Verify Bankak in App'}
                </div>
                <p className="text-neutral-400 leading-relaxed text-[11px]">
                  {t.safetyNoticeP1}
                </p>
              </div>

              <div className="p-3.5 rounded-xl bg-neutral-950 border border-neutral-800 space-y-2">
                <div className="font-bold text-white flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center text-xs">2</span>
                  {language === 'ar' ? 'المعاينة في أماكن عامة' : 'Public Venue Meetups'}
                </div>
                <p className="text-neutral-400 leading-relaxed text-[11px]">
                  {t.safetyNoticeP2}
                </p>
              </div>

              <div className="p-3.5 rounded-xl bg-neutral-950 border border-neutral-800 space-y-2">
                <div className="font-bold text-white flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-rose-500/20 text-rose-400 flex items-center justify-center text-xs">3</span>
                  {language === 'ar' ? 'عدم إرسال عربون مسبق' : 'Never Send Pre-deposits'}
                </div>
                <p className="text-neutral-400 leading-relaxed text-[11px]">
                  {t.safetyNoticeP3}
                </p>
              </div>
            </div>

            <button
              onClick={() => setIsSafetyModalOpen(true)}
              className="text-xs text-amber-400 hover:text-amber-300 font-semibold flex items-center gap-1 cursor-pointer"
            >
              <span>{t.learnMoreSafety}</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Edit Listing Modal */}
      <EditListingModal
        product={editingProduct}
        isOpen={Boolean(editingProduct)}
        onClose={() => setEditingProduct(null)}
      />

      {/* Sold Confirmation Modal */}
      <SoldConfirmModal
        product={soldConfirmProduct}
        isOpen={Boolean(soldConfirmProduct)}
        onClose={() => setSoldConfirmProduct(null)}
        onConfirm={() => {
          if (soldConfirmProduct) {
            markProductAsSold(soldConfirmProduct.id);
            setSoldConfirmProduct(null);
          }
        }}
      />
    </div>
  );
};
