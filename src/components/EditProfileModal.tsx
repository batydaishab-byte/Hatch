import React, { useState, useEffect } from 'react';
import {
  X,
  Pencil,
  MapPin,
  Phone,
  User as UserIcon,
  Camera,
  Check,
  ShieldCheck,
  Sparkles,
  Store,
  Building2,
  ArrowUpCircle,
  Link2,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { AccountType, resolveAccountType, SUDANESE_CITIES, SudaneseCity } from '../types';

const AVATAR_PRESETS = [
  { id: '1', url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80', label: 'افتراضي' },
  { id: '2', url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80', label: 'شاب' },
  { id: '3', url: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=256&q=80', label: 'سيدة' },
  { id: '4', url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=256&q=80', label: 'تاجر' },
  { id: '5', url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80', label: 'عصري' },
];

const COMMERCIAL_LOGO_PRESETS = [
  { id: 'c1', url: 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=256&q=80', label: 'متجر' },
  { id: 'c2', url: 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=256&q=80', label: 'شركة' },
  { id: 'c3', url: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=256&q=80', label: 'تقنية' },
  { id: 'c4', url: 'https://images.unsplash.com/photo-1509391365360-2e959784a276?auto=format&fit=crop&w=256&q=80', label: 'طاقة' },
  { id: 'c5', url: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=256&q=80', label: 'أزياء' },
];

export const EditProfileModal: React.FC = () => {
  const {
    currentUser,
    isEditProfileOpen,
    setIsEditProfileOpen,
    updateCurrentUser,
    t,
    language,
  } = useApp();

  const initialType = resolveAccountType(currentUser.accountType);

  const [accountType, setAccountType] = useState<AccountType>(initialType);
  const [name, setName] = useState(currentUser.name);
  const [businessActivity, setBusinessActivity] = useState(currentUser.businessActivity || '');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState<SudaneseCity>(currentUser.city);
  const [selectedAvatar, setSelectedAvatar] = useState(currentUser.businessLogo || currentUser.avatar);
  const [customAvatar, setCustomAvatar] = useState('');
  const [logoUrlInput, setLogoUrlInput] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (currentUser) {
      const resolved = resolveAccountType(currentUser.accountType);
      setAccountType(resolved);
      setName(currentUser.name);
      setBusinessActivity(currentUser.businessActivity || '');
      setCity(currentUser.city);
      setSelectedAvatar(currentUser.businessLogo || currentUser.avatar);

      let clean = currentUser.phone.replace(/[^0-9]/g, '');
      if (clean.startsWith('249')) {
        clean = clean.slice(3);
      }
      if (clean.startsWith('0')) {
        clean = clean.slice(1);
      }
      setPhone(clean);
      setCustomAvatar('');
      setLogoUrlInput('');
      setError('');
    }
  }, [currentUser, isEditProfileOpen]);

  if (!isEditProfileOpen) return null;

  const currentType = resolveAccountType(currentUser.accountType);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || name.trim().length < 2) {
      setError(
        accountType === 'store'
          ? language === 'ar'
            ? 'يرجى كتابة اسم المتجر'
            : 'Please enter store name'
          : accountType === 'company'
          ? language === 'ar'
            ? 'يرجى كتابة اسم الشركة'
            : 'Please enter company name'
          : language === 'ar'
          ? 'يرجى كتابة الاسم الكامل'
          : 'Please enter your full name'
      );
      return;
    }

    if ((accountType === 'store' || accountType === 'company') && !businessActivity.trim()) {
      setError(
        accountType === 'store'
          ? language === 'ar'
            ? 'يرجى إدخال نوع نشاط المتجر'
            : 'Please enter store activity type'
          : language === 'ar'
          ? 'يرجى إدخال مجال عمل الشركة'
          : 'Please enter company business field'
      );
      return;
    }

    if (!phone.trim()) {
      setError(language === 'ar' ? 'يرجى إدخال رقم الهاتف' : 'Please enter your phone number');
      return;
    }

    let cleanDigits = phone.trim().replace(/[^0-9]/g, '');
    if (cleanDigits.startsWith('249')) {
      cleanDigits = cleanDigits.slice(3);
    }
    if (cleanDigits.startsWith('0')) {
      cleanDigits = cleanDigits.slice(1);
    }

    if (cleanDigits.length < 8) {
      setError(
        language === 'ar'
          ? 'يرجى إدخال رقم هاتف سوداني صحيح'
          : 'Please enter a valid phone number'
      );
      return;
    }

    const fullPhoneNumber = `+249 ${cleanDigits}`;
    const chosenImage = logoUrlInput.trim() || customAvatar.trim() || selectedAvatar;

    await updateCurrentUser({
      name: name.trim(),
      phone: fullPhoneNumber,
      city,
      avatar: chosenImage,
      accountType,
      businessActivity: accountType === 'personal' ? undefined : businessActivity.trim(),
      businessLogo: accountType === 'personal' ? undefined : chosenImage,
    });
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setCustomAvatar(reader.result);
          setLogoUrlInput('');
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const activePresets = accountType === 'personal' ? AVATAR_PRESETS : COMMERCIAL_LOGO_PRESETS;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-5 sm:p-7 overflow-hidden my-auto animate-in fade-in zoom-in-95 duration-200 max-h-[94vh] overflow-y-auto no-scrollbar">
        <div className="absolute top-0 end-0 w-44 h-44 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 start-0 w-44 h-44 bg-blue-500/5 rounded-full blur-3xl pointer-events-none" />

        {/* Header */}
        <div className="relative flex items-start justify-between gap-4 pb-4 border-b border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0 shadow-inner">
              <Pencil className="w-6 h-6 stroke-[2.2]" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-300 text-[10px] font-bold mb-1">
                <Sparkles className="w-3 h-3 text-amber-400" />
                <span>
                  {accountType === 'store'
                    ? `🏪 ${t.accountTypeStore}`
                    : accountType === 'company'
                    ? `🏢 ${t.accountTypeCompany}`
                    : `👤 ${t.accountTypePersonal}`}
                </span>
              </div>
              <h3 className="font-black text-lg sm:text-xl text-white">
                {t.editProfile}
              </h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                {t.editProfileSubtitle}
              </p>
            </div>
          </div>

          <button
            onClick={() => setIsEditProfileOpen(false)}
            className="w-9 h-9 rounded-full bg-neutral-800/80 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition-colors cursor-pointer shrink-0"
            title={t.cancelBtn}
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {error && (
            <div className="p-3 rounded-2xl bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-semibold flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping" />
              {error}
            </div>
          )}

          {/* Account Type Upgrade Selector for personal accounts (or type management) */}
          {currentType === 'personal' && (
            <div className="p-3.5 rounded-2xl bg-gradient-to-br from-amber-500/10 via-neutral-950 to-neutral-950 border border-amber-500/30 space-y-2.5">
              <div className="flex items-center gap-2 text-xs font-black text-amber-300">
                <ArrowUpCircle className="w-4 h-4 text-amber-400 shrink-0" />
                <span>{t.upgradeAccountTitle}</span>
              </div>
              <p className="text-[11px] text-neutral-400 leading-relaxed">
                {t.upgradeAccountDesc}
              </p>
              <div className="grid grid-cols-3 gap-2">
                {(
                  [
                    { id: 'personal' as AccountType, label: t.commercialBadgePersonal, emoji: '👤' },
                    { id: 'store' as AccountType, label: t.commercialBadgeStore, emoji: '🏪' },
                    { id: 'company' as AccountType, label: t.commercialBadgeCompany, emoji: '🏢' },
                  ]
                ).map((opt) => {
                  const selected = accountType === opt.id;
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => {
                        setAccountType(opt.id);
                        setError('');
                      }}
                      className={`py-2 px-2 rounded-xl border text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                        selected
                          ? 'bg-amber-500 text-neutral-950 border-amber-300 shadow-md'
                          : 'bg-neutral-900 text-neutral-300 border-neutral-800 hover:border-neutral-600'
                      }`}
                    >
                      <span>{opt.emoji}</span>
                      <span>{opt.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Name / Store Name / Company Name */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {accountType === 'store'
                ? t.storeNameLabel
                : accountType === 'company'
                ? t.companyNameLabel
                : t.fullNameLabel}{' '}
              <span className="text-amber-400">*</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-neutral-500">
                {accountType === 'store' ? (
                  <Store className="w-4 h-4" />
                ) : accountType === 'company' ? (
                  <Building2 className="w-4 h-4" />
                ) : (
                  <UserIcon className="w-4 h-4" />
                )}
              </div>
              <input
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (error) setError('');
                }}
                placeholder={
                  accountType === 'store'
                    ? t.storeNamePlaceholder
                    : accountType === 'company'
                    ? t.companyNamePlaceholder
                    : t.fullNamePlaceholder
                }
                className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl ps-10 pe-4 py-3 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner"
              />
            </div>
          </div>

          {/* Commercial Activity / Field */}
          {(accountType === 'store' || accountType === 'company') && (
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {accountType === 'store' ? t.storeActivityLabel : t.companyFieldLabel}{' '}
                <span className="text-amber-400">*</span>
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-neutral-500">
                  <Sparkles className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  value={businessActivity}
                  onChange={(e) => {
                    setBusinessActivity(e.target.value);
                    if (error) setError('');
                  }}
                  placeholder={
                    accountType === 'store'
                      ? t.storeActivityPlaceholder
                      : t.companyFieldPlaceholder
                  }
                  maxLength={120}
                  className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl ps-10 pe-4 py-3 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner"
                />
              </div>
            </div>
          )}

          {/* Phone Number */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.phoneLabel} <span className="text-amber-400">*</span>
            </label>
            <div className="flex items-center gap-2">
              <div
                className="flex items-center gap-1.5 px-3 py-3 rounded-2xl bg-neutral-950 border border-neutral-800 text-amber-400 font-mono font-bold text-sm shrink-0 select-none shadow-inner"
                title={language === 'ar' ? 'مفتاح جمهورية السودان (+249)' : 'Sudan dial code (+249)'}
              >
                <span className="text-base leading-none">🇸🇩</span>
                <span dir="ltr">+249</span>
              </div>

              <div className="relative flex-1">
                <div className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-neutral-500">
                  <Phone className="w-4 h-4" />
                </div>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => {
                    const val = e.target.value.replace(/[^0-9]/g, '');
                    setPhone(val);
                    if (error) setError('');
                  }}
                  placeholder={t.phonePlaceholder}
                  maxLength={10}
                  dir="ltr"
                  className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl ps-10 pe-4 py-3 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner text-left font-mono tracking-wider"
                />
              </div>
            </div>
          </div>

          {/* City / State */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.citySelectLabel} <span className="text-amber-400">*</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-neutral-500">
                <MapPin className="w-4 h-4" />
              </div>
              <select
                value={city}
                onChange={(e) => setCity(e.target.value as SudaneseCity)}
                className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl ps-10 pe-8 py-3 text-sm text-white focus:outline-none transition-all cursor-pointer shadow-inner appearance-none"
              >
                {SUDANESE_CITIES.map((c) => (
                  <option key={c} value={c} className="bg-neutral-900 text-white">
                    {c}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Choose Avatar or Store/Company Logo */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-2">
              {accountType === 'store'
                ? t.storeLogoLabel
                : accountType === 'company'
                ? t.companyLogoLabel
                : t.chooseAvatar}
            </label>
            <div className="flex items-center gap-3 overflow-x-auto pb-1 no-scrollbar">
              {activePresets.map((preset) => {
                const isSelected = !customAvatar && !logoUrlInput && selectedAvatar === preset.url;
                return (
                  <button
                    key={preset.id}
                    type="button"
                    onClick={() => {
                      setSelectedAvatar(preset.url);
                      setCustomAvatar('');
                      setLogoUrlInput('');
                    }}
                    className={`relative w-12 h-12 ${
                      accountType === 'personal' ? 'rounded-full' : 'rounded-2xl'
                    } overflow-hidden border-2 transition-all shrink-0 cursor-pointer ${
                      isSelected
                        ? 'border-amber-400 ring-4 ring-amber-400/25 scale-105'
                        : 'border-neutral-700 opacity-70 hover:opacity-100 hover:border-neutral-500'
                    }`}
                  >
                    <img src={preset.url} alt={preset.label} className="w-full h-full object-cover" />
                    {isSelected && (
                      <div className="absolute inset-0 bg-amber-500/20 flex items-center justify-center">
                        <Check className="w-4 h-4 text-amber-300 stroke-[3]" />
                      </div>
                    )}
                  </button>
                );
              })}

              <label
                className={`relative w-12 h-12 ${
                  accountType === 'personal' ? 'rounded-full' : 'rounded-2xl'
                } border-2 border-dashed flex flex-col items-center justify-center cursor-pointer transition-all shrink-0 ${
                  customAvatar
                    ? 'border-amber-400 ring-4 ring-amber-400/25'
                    : 'border-neutral-700 hover:border-amber-400/70 bg-neutral-950/60'
                }`}
                title={language === 'ar' ? 'رفع صورة من جهازك' : 'Upload photo'}
              >
                {customAvatar ? (
                  <img
                    src={customAvatar}
                    alt="Custom"
                    className={`w-full h-full ${
                      accountType === 'personal' ? 'rounded-full' : 'rounded-2xl'
                    } object-cover`}
                  />
                ) : (
                  <Camera className="w-4 h-4 text-neutral-400 hover:text-amber-400" />
                )}
                <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
              </label>
            </div>

            {(accountType === 'store' || accountType === 'company') && (
              <div className="relative mt-2">
                <div className="absolute inset-y-0 start-0 ps-3 flex items-center pointer-events-none text-neutral-500">
                  <Link2 className="w-3.5 h-3.5" />
                </div>
                <input
                  type="url"
                  value={logoUrlInput}
                  onChange={(e) => {
                    setLogoUrlInput(e.target.value);
                    if (e.target.value) setCustomAvatar('');
                  }}
                  placeholder={
                    language === 'ar'
                      ? 'أو أدخل رابط الشعار (HTTPS) اختياري'
                      : 'Or paste logo HTTPS URL (optional)'
                  }
                  dir="ltr"
                  className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl ps-9 pe-3 py-2 text-xs text-white placeholder-neutral-500 focus:outline-none text-left"
                />
              </div>
            )}
          </div>

          {/* Account Status Safeguard Notice */}
          <div className="p-3 rounded-2xl bg-neutral-950/60 border border-neutral-800 text-[11px] text-neutral-400 flex items-center gap-2.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>
              {language === 'ar'
                ? 'يتم الاحتفاظ بجميع منتجاتك وصفقاتك ومحادثاتك وتقييم ثقتك على نفس الحساب دون أي فقدان.'
                : 'All your products, deals, conversations, and trust rating are preserved under your same account.'}
            </span>
          </div>

          {/* Action Buttons */}
          <div className="pt-2 flex flex-col sm:flex-row gap-2.5">
            <button
              type="submit"
              className="flex-1 py-3 px-5 bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-neutral-950 font-black text-sm rounded-2xl shadow-lg shadow-amber-500/25 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer flex items-center justify-center gap-2"
            >
              <Check className="w-4 h-4 stroke-[2.5]" />
              <span>{t.saveChangesBtn}</span>
            </button>

            <button
              type="button"
              onClick={() => setIsEditProfileOpen(false)}
              className="py-3 px-5 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs rounded-2xl transition-colors cursor-pointer border border-neutral-700 text-center"
            >
              {t.cancelBtn}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
