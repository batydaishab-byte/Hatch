import React from 'react';
import { 
  Sparkles, 
  Globe, 
  Bell, 
  ShieldCheck, 
  MapPin, 
  ChevronDown, 
  Flame, 
  Shield, 
  UserCheck,
  Smartphone
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { SudaneseCity, SUDANESE_CITIES } from '../types';

export const Header: React.FC = () => {
  const { 
    language, 
    setLanguage, 
    t, 
    currentUser, 
    switchUser, 
    availableUsers, 
    selectedCity, 
    setSelectedCity,
    unreadNotificationsCount,
    setIsNotificationsOpen,
    setIsSafetyModalOpen,
    setIsApkModalOpen,
    setActiveTab,
    activeTab
  } = useApp();

  const cities: (SudaneseCity | 'all')[] = [
    'all',
    ...SUDANESE_CITIES
  ];

  return (
    <header className="sticky top-0 z-40 bg-neutral-900/90 backdrop-blur-md border-b border-neutral-800 text-white">
      <div className="max-w-7xl mx-auto px-3 sm:px-6 h-16 flex items-center justify-between gap-2">
        {/* Brand Logo & Name */}
        <div 
          onClick={() => setActiveTab('swipe')}
          className="flex items-center gap-2.5 cursor-pointer group select-none"
        >
          {/* Logo Icon with the uploaded HATCH Brand Icon */}
          <div className="relative w-11 h-11 rounded-2xl bg-gradient-to-tr from-amber-600 via-amber-500 to-yellow-400 p-0.5 shadow-lg shadow-amber-500/25 group-hover:scale-105 transition-transform duration-300 shrink-0">
            <div className="w-full h-full bg-neutral-950 rounded-[14px] flex items-center justify-center overflow-hidden relative">
              <img
                src="/assets/aistudio/pwa-512x512_f8378661.png"
                alt="هتش HATCH"
                className="w-full h-full object-cover"
                loading="eager"
              />
              <div className="absolute -bottom-1 -right-1 w-3.5 h-3.5 bg-emerald-500 rounded-full border-2 border-neutral-950 shadow-sm" title="Online in Sudan"></div>
            </div>
          </div>

          <div className="flex flex-col">
            <div className="flex items-center gap-1.5">
              <span className="text-xl font-black tracking-tight bg-gradient-to-r from-amber-400 via-yellow-200 to-amber-500 bg-clip-text text-transparent">
                {language === 'ar' ? 'هَـتـْـش' : 'HATCH'}
              </span>
              <span className="text-xs px-1.5 py-0.5 rounded font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20 uppercase tracking-wider hidden sm:inline-block">
                سودان 🇸🇩
              </span>
            </div>
            <span className="text-[10px] text-neutral-400 -mt-1 font-medium hidden xs:inline-block">
              {t.logoSub}
            </span>
          </div>
        </div>

        {/* Center: City Quick Picker */}
        <div className="relative hidden md:flex items-center">
          <div className="flex items-center gap-1.5 bg-neutral-800/80 hover:bg-neutral-800 border border-neutral-700/60 rounded-full px-3 py-1.5 text-xs text-neutral-200 transition-colors">
            <MapPin className="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <select
              value={selectedCity}
              onChange={(e) => setSelectedCity(e.target.value as any)}
              className="bg-transparent text-xs text-neutral-200 focus:outline-none cursor-pointer pr-1"
            >
              {cities.map((city) => (
                <option key={city} value={city} className="bg-neutral-900 text-white">
                  {city === 'all' ? t.cities.all : city}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Right side actions */}
        <div className="flex items-center gap-1.5 sm:gap-2.5">
          {/* APK Install Button */}
          <button
            onClick={() => setIsApkModalOpen(true)}
            className="flex items-center gap-1.5 text-xs text-amber-300 hover:text-amber-200 bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/35 px-2.5 py-1.5 rounded-full transition-all cursor-pointer font-bold shadow-sm shadow-amber-500/10"
            title="تثبيت التطبيق (APK) / Install App"
          >
            <Smartphone className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden xs:inline">
              {language === 'ar' ? 'تطبيق APK' : 'APK App'}
            </span>
          </button>

          {/* Safety Shield Button */}
          <button
            onClick={() => setIsSafetyModalOpen(true)}
            title={t.safetyShield}
            className="flex items-center gap-1 text-xs text-emerald-400 hover:text-emerald-300 bg-emerald-950/40 hover:bg-emerald-900/40 border border-emerald-800/40 px-2.5 py-1.5 rounded-full transition-all cursor-pointer"
          >
            <ShieldCheck className="w-4 h-4" />
            <span className="hidden lg:inline text-xs font-medium">
              {language === 'ar' ? 'أمان بنكك' : 'Bankak Safe'}
            </span>
          </button>

          {/* Language Switch */}
          <button
            onClick={() => setLanguage(language === 'ar' ? 'en' : 'ar')}
            className="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-neutral-800/80 hover:bg-neutral-700/80 border border-neutral-700 text-xs font-semibold text-neutral-200 transition-all cursor-pointer"
            title="Switch Language / تغيير اللغة"
          >
            <Globe className="w-3.5 h-3.5 text-amber-400" />
            <span>{language === 'ar' ? 'English' : 'عربي'}</span>
          </button>

          {/* Notifications Bell */}
          <button
            onClick={() => setIsNotificationsOpen(true)}
            className="relative p-2 rounded-full bg-neutral-800/80 hover:bg-neutral-700 border border-neutral-700 text-neutral-300 hover:text-white transition-all cursor-pointer"
            title={t.notificationsTitle}
          >
            <Bell className="w-4 h-4" />
            {unreadNotificationsCount > 0 && (
              <span className="absolute -top-1 -right-1 w-4 h-4 bg-amber-500 text-neutral-950 text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse">
                {unreadNotificationsCount}
              </span>
            )}
          </button>

          {/* Switchable User Avatar Pill */}
          <div className="relative group">
            <button
              onClick={() => setActiveTab('profile')}
              className="flex items-center gap-2 pl-2 pr-1 py-1 rounded-full bg-neutral-800/90 hover:bg-neutral-700 border border-neutral-700 cursor-pointer transition-all"
            >
              <img
                src={currentUser.avatar}
                alt={currentUser.name}
                className="w-7 h-7 rounded-full object-cover ring-1 ring-amber-400/40"
              />
              <span className="text-xs font-medium max-w-[80px] sm:max-w-[100px] truncate hidden sm:inline">
                {currentUser.name}
              </span>
              {currentUser.role === 'admin' && (
                <span className="text-[10px] bg-red-500/20 text-red-400 px-1.5 py-0.5 rounded font-mono font-bold">
                  ADMIN
                </span>
              )}
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
