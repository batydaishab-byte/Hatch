import React, { useState, useEffect } from 'react';
import {
  X,
  UserPlus,
  MapPin,
  Phone,
  User as UserIcon,
  Sparkles,
  ShieldCheck,
  Camera,
  Check,
  Store,
  Building2,
  ArrowLeft,
  ArrowRight,
  Link2,
  Loader2,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { AccountType, SUDANESE_CITIES, SudaneseCity } from '../types';

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

export const CreateAccountModal: React.FC = () => {
  const {
    isCreateAccountOpen,
    setIsCreateAccountOpen,
    createNewAccount,
    firebaseUser,
    isAuthLoading,
    authOnboardingRequired,
    signInWithGoogle,
    t,
    language,
  } = useApp();

  const [step, setStep] = useState<'choose_type' | 'complete_profile'>('choose_type');
  const [accountType, setAccountType] = useState<AccountType>('personal');
  const [name, setName] = useState('');
  const [businessActivity, setBusinessActivity] = useState('');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState<SudaneseCity>('بورتسودان (ولاية البحر الأحمر)');
  const [selectedAvatar, setSelectedAvatar] = useState(AVATAR_PRESETS[0].url);
  const [customAvatar, setCustomAvatar] = useState('');
  const [logoUrlInput, setLogoUrlInput] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Reset modal state cleanly whenever opened; prefill Google name/photo when signed in
  useEffect(() => {
    if (isCreateAccountOpen) {
      setStep('choose_type');
      setError('');
      setIsSubmitting(false);
      if (firebaseUser) {
        setName((prev) => prev || firebaseUser.displayName || '');
        if (firebaseUser.photoURL) {
          setSelectedAvatar(firebaseUser.photoURL);
        }
      }
    }
  }, [isCreateAccountOpen, firebaseUser]);

  // Update default logo preset when toggling between personal and commercial types
  const handleSelectType = (type: AccountType) => {
    setAccountType(type);
    setError('');
    if (type !== 'personal') {
      const isCurrentlyPersonalPreset = AVATAR_PRESETS.some((p) => p.url === selectedAvatar);
      if (isCurrentlyPersonalPreset) {
        setSelectedAvatar(type === 'company' ? COMMERCIAL_LOGO_PRESETS[1].url : COMMERCIAL_LOGO_PRESETS[0].url);
      }
    } else {
      const isCurrentlyCommercialPreset = COMMERCIAL_LOGO_PRESETS.some((p) => p.url === selectedAvatar);
      if (isCurrentlyCommercialPreset) {
        setSelectedAvatar(firebaseUser?.photoURL || AVATAR_PRESETS[0].url);
      }
    }
  };

  if (!isCreateAccountOpen) return null;

  const handleClose = () => {
    localStorage.setItem('hatch_seen_create_account', 'true');
    setIsCreateAccountOpen(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!name.trim() || name.trim().length < 2) {
      setError(
        accountType === 'store'
          ? language === 'ar'
            ? 'يرجى كتابة اسم المتجر (حرفان على الأقل)'
            : 'Please enter the store name (at least 2 chars)'
          : accountType === 'company'
          ? language === 'ar'
            ? 'يرجى كتابة اسم الشركة (حرفان على الأقل)'
            : 'Please enter the company name (at least 2 chars)'
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
            ? 'يرجى تحديد نوع نشاط المتجر'
            : 'Please specify the store activity type'
          : language === 'ar'
          ? 'يرجى تحديد مجال عمل الشركة'
          : 'Please specify the company business field'
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
          : 'Please enter a valid Sudanese phone number'
      );
      return;
    }

    const fullPhoneNumber = `+249 ${cleanDigits}`;
    const chosenImage = logoUrlInput.trim() || customAvatar.trim() || selectedAvatar;

    setIsSubmitting(true);
    try {
      await createNewAccount({
        uid: firebaseUser?.uid,
        email: firebaseUser?.email || undefined,
        name: name.trim(),
        phone: fullPhoneNumber,
        city,
        avatar: chosenImage,
        accountType,
        businessActivity: accountType === 'personal' ? undefined : businessActivity.trim(),
        businessLogo: accountType === 'personal' ? undefined : chosenImage,
      });
    } finally {
      setIsSubmitting(false);
    }
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

  const accountOptions: {
    id: AccountType;
    emoji: string;
    title: string;
    desc: string;
    icon: React.ReactNode;
    accent: string;
  }[] = [
    {
      id: 'personal',
      emoji: '👤',
      title: t.accountTypePersonal,
      desc: t.accountTypePersonalDesc,
      icon: <UserIcon className="w-5 h-5" />,
      accent: 'from-amber-500/20 to-yellow-500/5 border-amber-500/40',
    },
    {
      id: 'store',
      emoji: '🏪',
      title: t.accountTypeStore,
      desc: t.accountTypeStoreDesc,
      icon: <Store className="w-5 h-5" />,
      accent: 'from-emerald-500/20 to-amber-500/5 border-emerald-500/40',
    },
    {
      id: 'company',
      emoji: '🏢',
      title: t.accountTypeCompany,
      desc: t.accountTypeCompanyDesc,
      icon: <Building2 className="w-5 h-5" />,
      accent: 'from-sky-500/20 to-amber-500/5 border-sky-500/40',
    },
  ];

  const isRtl = language === 'ar';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-5 sm:p-7 overflow-hidden my-auto animate-in fade-in zoom-in-95 duration-200">
        {/* Glow ambient accent */}
        <div className="absolute top-0 end-0 w-44 h-44 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 start-0 w-44 h-44 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

        {/* Header */}
        <div className="relative flex items-start justify-between gap-4 pb-4 border-b border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0 shadow-inner">
              {step === 'choose_type' ? (
                <Sparkles className="w-6 h-6 stroke-[2.2]" />
              ) : accountType === 'store' ? (
                <Store className="w-6 h-6 stroke-[2.2]" />
              ) : accountType === 'company' ? (
                <Building2 className="w-6 h-6 stroke-[2.2]" />
              ) : (
                <UserPlus className="w-6 h-6 stroke-[2.2]" />
              )}
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-300 text-[10px] font-bold mb-1">
                <Sparkles className="w-3 h-3 text-amber-400" />
                <span>
                  {firebaseUser?.email
                    ? `Google: ${firebaseUser.email}`
                    : language === 'ar'
                    ? 'منصة هتش السودان 🇸🇩'
                    : 'HATCH Sudan 🇸🇩'}
                </span>
              </div>
              <h3 className="font-black text-base sm:text-lg text-white leading-snug">
                {step === 'choose_type'
                  ? t.chooseAccountTypeTitle
                  : accountType === 'store'
                  ? t.accountTypeStore
                  : accountType === 'company'
                  ? t.accountTypeCompany
                  : t.accountTypePersonal}
              </h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                {step === 'choose_type'
                  ? t.chooseAccountTypeSubtitle
                  : t.createAccountSubtitle}
              </p>
            </div>
          </div>

          {!authOnboardingRequired && (
            <button
              onClick={handleClose}
              className="w-9 h-9 rounded-full bg-neutral-800/80 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition-colors cursor-pointer shrink-0"
              title={language === 'ar' ? 'إغلاق والتصفح' : 'Close and Browse'}
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Step 1: Account Type Selection */}
        {step === 'choose_type' ? (
          <div className="mt-5 space-y-4">
            {/* Google Sign-In CTA when not yet signed in */}
            {!firebaseUser && (
              <button
                type="button"
                onClick={signInWithGoogle}
                disabled={isAuthLoading}
                className="w-full py-3 px-4 rounded-2xl bg-white hover:bg-neutral-100 text-neutral-900 font-bold text-sm shadow-lg flex items-center justify-center gap-2.5 transition-all cursor-pointer disabled:opacity-60"
              >
                {isAuthLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin text-amber-600" />
                    <span>{t.signingInGoogle}</span>
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24" aria-hidden="true">
                      <path
                        fill="#4285F4"
                        d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"
                      />
                      <path
                        fill="#34A853"
                        d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24Z"
                      />
                      <path
                        fill="#FBBC05"
                        d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14Z"
                      />
                      <path
                        fill="#EA4335"
                        d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96Z"
                      />
                    </svg>
                    <span>{t.continueWithGoogle}</span>
                  </>
                )}
              </button>
            )}

            {/* Three Account Type Cards */}
            <div className="space-y-2.5">
              {accountOptions.map((option) => {
                const isSelected = accountType === option.id;
                return (
                  <button
                    key={option.id}
                    type="button"
                    onClick={() => handleSelectType(option.id)}
                    className={`w-full p-4 rounded-2xl border-2 text-start transition-all cursor-pointer flex items-center justify-between gap-3 ${
                      isSelected
                        ? 'bg-gradient-to-br from-amber-500/15 via-neutral-900 to-neutral-950 border-amber-400 shadow-lg shadow-amber-500/15 scale-[1.01]'
                        : 'bg-neutral-950/70 border-neutral-800 hover:border-neutral-600 hover:bg-neutral-900'
                    }`}
                  >
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div
                        className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl shrink-0 border ${
                          isSelected
                            ? 'bg-amber-500 text-neutral-950 border-amber-300 shadow-md'
                            : 'bg-neutral-900 text-amber-400 border-neutral-800'
                        }`}
                      >
                        <span aria-hidden="true">{option.emoji}</span>
                      </div>

                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-black text-sm sm:text-base text-white">
                            {option.title}
                          </span>
                        </div>
                        <p className="text-xs text-neutral-400 mt-0.5 leading-relaxed">
                          {option.desc}
                        </p>
                      </div>
                    </div>

                    <div
                      className={`w-6 h-6 rounded-full border-2 flex items-center justify-center shrink-0 transition-all ${
                        isSelected
                          ? 'bg-amber-400 border-amber-300 text-neutral-950'
                          : 'border-neutral-700 bg-neutral-900'
                      }`}
                    >
                      {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Continue / Guest Actions */}
            <div className="pt-2 flex flex-col gap-2">
              <button
                type="button"
                onClick={() => setStep('complete_profile')}
                className="w-full py-3.5 px-5 bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-neutral-950 font-black text-sm rounded-2xl shadow-lg shadow-amber-500/25 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer flex items-center justify-center gap-2"
              >
                <span>{t.continueNextBtn}</span>
                {isRtl ? (
                  <ArrowLeft className="w-4 h-4 stroke-[2.5]" />
                ) : (
                  <ArrowRight className="w-4 h-4 stroke-[2.5]" />
                )}
              </button>

              {!authOnboardingRequired && (
                <button
                  type="button"
                  onClick={handleClose}
                  className="w-full py-2.5 px-4 text-xs font-bold text-neutral-400 hover:text-white hover:bg-neutral-800/60 rounded-xl transition-colors cursor-pointer"
                >
                  {t.browseAsGuest}
                </button>
              )}
            </div>
          </div>
        ) : (
          /* Step 2: Complete Account Data according to chosen AccountType */
          <form onSubmit={handleSubmit} className="mt-5 space-y-4">
            {/* Selected Type Pill & Back Button */}
            <div className="flex items-center justify-between gap-2 p-2.5 rounded-2xl bg-neutral-950/80 border border-neutral-800">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-300">
                <span>
                  {accountType === 'personal' ? '👤' : accountType === 'store' ? '🏪' : '🏢'}
                </span>
                <span>
                  {accountType === 'personal'
                    ? t.accountTypePersonal
                    : accountType === 'store'
                    ? t.accountTypeStore
                    : t.accountTypeCompany}
                </span>
              </div>

              <button
                type="button"
                onClick={() => {
                  setError('');
                  setStep('choose_type');
                }}
                className="text-[11px] font-bold text-neutral-300 hover:text-amber-300 px-2.5 py-1 rounded-xl bg-neutral-900 border border-neutral-700 hover:border-amber-500/50 transition-colors cursor-pointer"
              >
                {t.backToAccountTypes}
              </button>
            </div>

            {error && (
              <div className="p-3 rounded-2xl bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-semibold flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping" />
                {error}
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
                  autoFocus
                />
              </div>
            </div>

            {/* Store Activity Type or Company Business Field */}
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

            {/* Phone Number with Sudan Dial Code (+249) */}
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

            {/* Verification Badge Note */}
            <div className="p-3 rounded-2xl bg-neutral-950/60 border border-neutral-800 text-[11px] text-neutral-400 flex items-center gap-2.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
              <span>
                {language === 'ar'
                  ? 'شارة التوثيق الذهبية تظهر تلقائياً بعد إتمام 10 صفقات بيع ناجحة وموثوقة.'
                  : 'The verified gold badge unlocks automatically after 10 trusted successful deals.'}
              </span>
            </div>

            {/* Action Buttons */}
            <div className="pt-2 flex flex-col gap-2">
              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-3.5 px-5 bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-neutral-950 font-black text-sm rounded-2xl shadow-lg shadow-amber-500/25 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer flex items-center justify-center gap-2 disabled:opacity-60"
              >
                {isSubmitting ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <UserPlus className="w-4 h-4 stroke-[2.5]" />
                )}
                <span>{t.createAccountBtn}</span>
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
