import React, { useState } from 'react';
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
  ArrowRight
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { SUDANESE_CITIES, SudaneseCity } from '../types';

const AVATAR_PRESETS = [
  { id: '1', url: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=256&q=80', label: 'افتراضي' },
  { id: '2', url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=256&q=80', label: 'شاب' },
  { id: '3', url: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=256&q=80', label: 'سيدة' },
  { id: '4', url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=256&q=80', label: 'تاجر' },
  { id: '5', url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80', label: 'عصري' },
];

export const CreateAccountModal: React.FC = () => {
  const { 
    isCreateAccountOpen, 
    setIsCreateAccountOpen, 
    createNewAccount, 
    t, 
    language 
  } = useApp();

  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState<SudaneseCity>('بورتسودان (ولاية البحر الأحمر)');
  const [selectedAvatar, setSelectedAvatar] = useState(AVATAR_PRESETS[0].url);
  const [customAvatar, setCustomAvatar] = useState('');
  const [error, setError] = useState('');

  if (!isCreateAccountOpen) return null;

  const handleClose = () => {
    localStorage.setItem('hatch_seen_create_account', 'true');
    setIsCreateAccountOpen(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError(language === 'ar' ? 'يرجى كتابة الاسم الكامل' : 'Please enter your full name');
      return;
    }
    if (!phone.trim()) {
      setError(language === 'ar' ? 'يرجى إدخال رقم الهاتف' : 'Please enter your phone number');
      return;
    }

    // Clean digits and format with Sudan code +249
    let cleanDigits = phone.trim().replace(/[^0-9]/g, '');
    if (cleanDigits.startsWith('249')) {
      cleanDigits = cleanDigits.slice(3);
    }
    if (cleanDigits.startsWith('0')) {
      cleanDigits = cleanDigits.slice(1);
    }

    if (cleanDigits.length < 8) {
      setError(language === 'ar' ? 'يرجى إدخال رقم هاتف سوداني صحيح' : 'Please enter a valid Sudanese phone number');
      return;
    }

    const fullPhoneNumber = `+249 ${cleanDigits}`;

    createNewAccount({
      name: name.trim(),
      phone: fullPhoneNumber,
      city,
      avatar: customAvatar.trim() || selectedAvatar,
    });
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setCustomAvatar(reader.result);
        }
      };
      reader.readAsDataURL(file);
    }
  };

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
              <UserPlus className="w-6 h-6 stroke-[2.2]" />
            </div>
            <div>
              <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-300 text-[10px] font-bold mb-1">
                <Sparkles className="w-3 h-3 text-amber-400" />
                <span>{language === 'ar' ? 'مرحباً بك في هتش 🇸🇩' : 'Welcome to HATCH 🇸🇩'}</span>
              </div>
              <h3 className="font-black text-lg sm:text-xl text-white">
                {t.createNewAccount}
              </h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                {t.createAccountSubtitle}
              </p>
            </div>
          </div>

          <button
            onClick={handleClose}
            className="w-9 h-9 rounded-full bg-neutral-800/80 hover:bg-neutral-700 text-neutral-400 hover:text-white flex items-center justify-center transition-colors cursor-pointer shrink-0"
            title={language === 'ar' ? 'إغلاق والتصفح' : 'Close and Browse'}
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {error && (
            <div className="p-3 rounded-2xl bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-semibold flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping" />
              {error}
            </div>
          )}

          {/* Full Name */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.fullNameLabel} <span className="text-amber-400">*</span>
            </label>
            <div className="relative">
              <div className="absolute inset-y-0 start-0 ps-3.5 flex items-center pointer-events-none text-neutral-500">
                <UserIcon className="w-4 h-4" />
              </div>
              <input
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (error) setError('');
                }}
                placeholder={t.fullNamePlaceholder}
                className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl ps-10 pe-4 py-3 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner"
                autoFocus
              />
            </div>
          </div>

          {/* Phone Number with Sudan Dial Code (+249) */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.phoneLabel} <span className="text-amber-400">*</span>
            </label>
            <div className="flex items-center gap-2">
              {/* Sudan Country Code Badge */}
              <div 
                className="flex items-center gap-1.5 px-3 py-3 rounded-2xl bg-neutral-950 border border-neutral-800 text-amber-400 font-mono font-bold text-sm shrink-0 select-none shadow-inner"
                title={language === 'ar' ? 'مفتاح جمهورية السودان (+249)' : 'Sudan dial code (+249)'}
              >
                <span className="text-base leading-none">🇸🇩</span>
                <span dir="ltr">+249</span>
              </div>

              {/* Phone Input */}
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
            <p className="text-[10px] text-neutral-500 mt-1">
              {language === 'ar' 
                ? 'أدخل رقم الهاتف (مثال: 912345678 أو 123456789)' 
                : 'Enter phone number (e.g. 912345678 or 123456789)'}
            </p>
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

          {/* Choose Avatar */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-2">
              {t.chooseAvatar}
            </label>
            <div className="flex items-center gap-3 overflow-x-auto pb-1 no-scrollbar">
              {AVATAR_PRESETS.map((preset) => {
                const isSelected = !customAvatar && selectedAvatar === preset.url;
                return (
                  <button
                    key={preset.id}
                    type="button"
                    onClick={() => {
                      setSelectedAvatar(preset.url);
                      setCustomAvatar('');
                    }}
                    className={`relative w-12 h-12 rounded-full overflow-hidden border-2 transition-all shrink-0 cursor-pointer ${
                      isSelected 
                        ? 'border-amber-400 ring-4 ring-amber-400/25 scale-105' 
                        : 'border-neutral-700 opacity-70 hover:opacity-100 hover:border-neutral-500'
                    }`}
                  >
                    <img 
                      src={preset.url} 
                      alt={preset.label}
                      className="w-full h-full object-cover" 
                    />
                    {isSelected && (
                      <div className="absolute inset-0 bg-amber-500/20 flex items-center justify-center">
                        <Check className="w-4 h-4 text-amber-300 stroke-[3]" />
                      </div>
                    )}
                  </button>
                );
              })}

              {/* Upload Custom Avatar Button */}
              <label 
                className={`relative w-12 h-12 rounded-full border-2 border-dashed flex flex-col items-center justify-center cursor-pointer transition-all shrink-0 ${
                  customAvatar 
                    ? 'border-amber-400 ring-4 ring-amber-400/25' 
                    : 'border-neutral-700 hover:border-amber-400/70 bg-neutral-950/60'
                }`}
                title={language === 'ar' ? 'رفع صورتك الخاصة' : 'Upload photo'}
              >
                {customAvatar ? (
                  <img src={customAvatar} alt="Custom" className="w-full h-full rounded-full object-cover" />
                ) : (
                  <Camera className="w-4 h-4 text-neutral-400 hover:text-amber-400" />
                )}
                <input 
                  type="file" 
                  accept="image/*" 
                  onChange={handleImageUpload} 
                  className="hidden" 
                />
              </label>
            </div>
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
              className="w-full py-3.5 px-5 bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 text-neutral-950 font-black text-sm rounded-2xl shadow-lg shadow-amber-500/25 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer flex items-center justify-center gap-2"
            >
              <UserPlus className="w-4 h-4 stroke-[2.5]" />
              <span>{t.createAccountBtn}</span>
            </button>

            <button
              type="button"
              onClick={handleClose}
              className="w-full py-2.5 px-4 text-xs font-bold text-neutral-400 hover:text-white hover:bg-neutral-800/60 rounded-xl transition-colors cursor-pointer"
            >
              {t.browseAsGuest}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
