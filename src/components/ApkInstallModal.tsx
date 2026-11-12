import React, { useState } from 'react';
import { 
  X, 
  Smartphone, 
  Download, 
  CheckCircle2, 
  ExternalLink, 
  ShieldCheck, 
  Sparkles, 
  Layers, 
  Share2, 
  Copy, 
  Check, 
  Terminal,
  FileJson,
  AlertTriangle
} from 'lucide-react';
import { usePWAInstall } from '../hooks/usePWAInstall';
import { useApp } from '../context/AppContext';
import confetti from 'canvas-confetti';

interface ApkInstallModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ApkInstallModal: React.FC<ApkInstallModalProps> = ({ isOpen, onClose }) => {
  const { isInstallable, isInstalled, isAndroid, isIOS, install } = usePWAInstall();
  const { language, t, showToast } = useApp();
  const [activeTab, setActiveTab] = useState<'instant' | 'package'>('instant');
  const [copiedLink, setCopiedLink] = useState(false);

  if (!isOpen) return null;

  const handleInstantInstall = async () => {
    const success = await install();
    if (success) {
      confetti({ particleCount: 70, spread: 60 });
      showToast(
        language === 'ar' ? 'تم بدء تثبيت تطبيق هتش على هاتفك بنجاح!' : 'HATCH installation initiated!',
        'success'
      );
      onClose();
    }
  };

  const currentAppUrl = window.location.origin;
  const pwaBuilderUrl = `https://www.pwabuilder.com?url=${encodeURIComponent(currentAppUrl)}`;

  const handleCopyLink = () => {
    navigator.clipboard?.writeText(currentAppUrl);
    setCopiedLink(true);
    showToast(language === 'ar' ? 'تم نسخ رابط التطبيق' : 'App link copied to clipboard', 'info');
    setTimeout(() => setCopiedLink(false), 2000);
  };

  const handleDownloadManifest = () => {
    fetch('/manifest.json')
      .then((res) => res.blob())
      .then((blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'manifest.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        showToast(language === 'ar' ? 'تم تنزيل ملف manifest.json بنجاح' : 'manifest.json downloaded successfully', 'success');
      })
      .catch((err) => {
        console.error('Failed to download manifest', err);
      });
  };

  const handleCopyManifestJson = () => {
    fetch('/manifest.json')
      .then((res) => res.text())
      .then((text) => {
        navigator.clipboard?.writeText(text);
        showToast(language === 'ar' ? 'تم نسخ كود ملف manifest.json بالكامل' : 'Manifest JSON copied', 'info');
      })
      .catch((err) => {
        console.error('Failed to copy manifest', err);
      });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-5 sm:p-7 overflow-hidden my-auto max-h-[92vh] flex flex-col animate-in fade-in zoom-in-95 duration-200">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 end-4 w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 flex items-center justify-center cursor-pointer transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Modal Header */}
        <div className="flex items-center gap-3.5 mb-5">
          <div className="w-13 h-13 rounded-2xl bg-gradient-to-tr from-amber-500 via-amber-400 to-yellow-300 p-0.5 shadow-lg shadow-amber-500/20 shrink-0">
            <div className="w-full h-full bg-neutral-950 rounded-[14px] flex items-center justify-center overflow-hidden">
              <img
                src="/assets/aistudio/pwa-512x512_f8378661.png"
                alt="هتش HATCH"
                className="w-full h-full object-cover"
              />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-lg sm:text-xl font-black text-white">
                {language === 'ar' ? 'تطبيق أندرويد (APK) — هتش' : 'HATCH Android Application (APK)'}
              </h3>
              <span className="text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                Android
              </span>
            </div>
            <p className="text-xs text-neutral-400 mt-0.5">
              {language === 'ar'
                ? 'ثبّت التطبيق على هاتفك المحمول واستمتع بتجربة أسرع بدون متصفح'
                : 'Install HATCH directly on your Android phone for native speed'}
            </p>
          </div>
        </div>

        {/* Tab Buttons */}
        <div className="flex items-center gap-2 border-b border-neutral-800 pb-2 mb-4 text-xs font-bold">
          <button
            onClick={() => setActiveTab('instant')}
            className={`flex-1 py-2 px-3 rounded-xl transition-all cursor-pointer text-center ${
              activeTab === 'instant'
                ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                : 'bg-neutral-950 text-neutral-400 hover:text-white'
            }`}
          >
            {language === 'ar' ? '⚡ التثبيت الفوري على الهاتف' : '⚡ Direct Mobile Install'}
          </button>
          <button
            onClick={() => setActiveTab('package')}
            className={`flex-1 py-2 px-3 rounded-xl transition-all cursor-pointer text-center ${
              activeTab === 'package'
                ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                : 'bg-neutral-950 text-neutral-400 hover:text-white'
            }`}
          >
            {language === 'ar' ? '📦 توليد حزمة APK مستقلة' : '📦 Build Standalone APK'}
          </button>
        </div>

        {/* Tab 1: Instant Install (WebAPK) */}
        {activeTab === 'instant' && (
          <div className="space-y-4 overflow-y-auto no-scrollbar flex-1">
            {/* Installed status banner */}
            {isInstalled ? (
              <div className="p-4 rounded-2xl bg-emerald-950/40 border border-emerald-600/40 flex items-center gap-3">
                <CheckCircle2 className="w-6 h-6 text-emerald-400 shrink-0" />
                <div>
                  <h4 className="text-xs font-bold text-emerald-300">
                    {language === 'ar' ? 'تطبيق هتش مثبت بالفعل على هذا الجهاز!' : 'HATCH is already installed on this device!'}
                  </h4>
                  <p className="text-[11px] text-neutral-300 mt-0.5">
                    {language === 'ar'
                      ? 'يمكنك فتحه مباشرة من شاشة هاتفك الرئيسية كأي تطبيق رسمي.'
                      : 'You can launch it directly from your home screen launcher.'}
                  </p>
                </div>
              </div>
            ) : isInstallable ? (
              <div className="p-4 rounded-2xl bg-amber-950/30 border border-amber-500/30 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-amber-300">
                    {language === 'ar' ? 'جاهز للتثبيت الفوري كـ تطبيق أندرويد' : 'Ready for instant Android installation'}
                  </span>
                  <span className="text-[10px] text-neutral-400">WebAPK Ready</span>
                </div>
                <button
                  onClick={handleInstantInstall}
                  className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-sm shadow-xl shadow-amber-500/25 flex items-center justify-center gap-2 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
                >
                  <Download className="w-4 h-4" />
                  <span>{language === 'ar' ? 'تثبيت تطبيق هتش الآن (APK)' : 'Install HATCH APK Now'}</span>
                </button>
              </div>
            ) : (
              /* Fallback instructions for manual install via browser */
              <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-3">
                <h4 className="text-xs font-bold text-white flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-400" />
                  {language === 'ar' ? 'خطوات التثبيت السريع على أندرويد (خلال 5 ثوانٍ):' : 'Easy 5-second Android setup:'}
                </h4>
                <div className="space-y-2.5 text-xs text-neutral-300">
                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">1</span>
                    <p className="text-[11px] leading-relaxed">
                      {language === 'ar'
                        ? 'انقر على زر القائمة (ثلاث نقاط ⋮) في أعلى أو أسفل المتصفح (كروم / سامسونج).'
                        : 'Tap the browser menu (three dots ⋮) in Chrome or Samsung Internet.'}
                    </p>
                  </div>
                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">2</span>
                    <p className="text-[11px] leading-relaxed">
                      {language === 'ar'
                        ? 'اختر «تثبيت التطبيق» (Install app) أو «إضافة إلى الشاشة الرئيسية» (Add to Home screen).'
                        : 'Select "Install app" or "Add to Home screen".'}
                    </p>
                  </div>
                  <div className="flex items-start gap-2.5">
                    <span className="w-5 h-5 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-xs shrink-0 mt-0.5">3</span>
                    <p className="text-[11px] leading-relaxed">
                      {language === 'ar'
                        ? 'سيقوم نظام أندرويد تلقائياً بإنشاء حزمة WebAPK وتثبيتها بأيقونة هتش الذهبية!'
                        : 'Android will automatically generate and install the system WebAPK with full offline capabilities!'}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Android features cards */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="p-3 rounded-xl bg-neutral-950 border border-neutral-800 space-y-1">
                <div className="font-bold text-white flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                  {language === 'ar' ? 'يعمل بدون شريط عناوين' : 'Standalone Window'}
                </div>
                <p className="text-[10px] text-neutral-400">
                  {language === 'ar' ? 'واجهة ملء الشاشة مثل تطبيقات جوجل بلاي' : 'Full-screen app view with no address bar'}
                </p>
              </div>

              <div className="p-3 rounded-xl bg-neutral-950 border border-neutral-800 space-y-1">
                <div className="font-bold text-white flex items-center gap-1">
                  <Layers className="w-3.5 h-3.5 text-amber-400" />
                  {language === 'ar' ? 'توفير الإنترنت وسرعة فائقة' : 'Fast Offline Cache'}
                </div>
                <p className="text-[10px] text-neutral-400">
                  {language === 'ar' ? 'تخزين كاش محلي للصور وعروض السوق' : 'Caches marketplace data locally'}
                </p>
              </div>
            </div>

            {/* Share or copy app link */}
            <div className="p-3 rounded-xl bg-neutral-950/80 border border-neutral-800 flex items-center justify-between text-xs">
              <span className="text-neutral-400 text-[11px] truncate max-w-[240px]">
                {currentAppUrl}
              </span>
              <button
                onClick={handleCopyLink}
                className="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-amber-400 font-bold text-xs transition-colors cursor-pointer"
              >
                {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedLink ? (language === 'ar' ? 'تم النسخ' : 'Copied') : (language === 'ar' ? 'نسخ الرابط' : 'Copy')}</span>
              </button>
            </div>
          </div>
        )}

        {/* Tab 2: Standalone APK Generator (PWABuilder / Bubblewrap) */}
        {activeTab === 'package' && (
          <div className="space-y-4 overflow-y-auto no-scrollbar flex-1">
            {/* حل فوري وواضح لمشكلة (الاسم مفقود في موقع PWABuilder) */}
            <div className="p-4 rounded-2xl bg-amber-500/10 border-2 border-amber-500/50 space-y-3">
              <div className="flex items-start gap-2.5">
                <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <h4 className="text-xs sm:text-sm font-black text-amber-300">
                    {language === 'ar'
                      ? 'حل مشكلة «الاسم مفقود» (Name is missing) على موقع PWABuilder'
                      : 'Solution for "Name is missing" on PWABuilder'}
                  </h4>
                  <p className="text-[11px] text-neutral-300 leading-relaxed">
                    {language === 'ar'
                      ? 'إذا ظهرت لك هذه الرسالة في PWABuilder، فالسبب هو أن رابط المعاينة السحابية محمي بحساب مطور يمنع خوادم الموقع الخارجي من فحص الرابط تلقائياً. يمكنك حل المشكلة فوراً بإحدى الطريقتين:'
                      : 'If you see "Name is missing", PWABuilder cannot crawl the cloud preview URL directly due to authentication. Solve it instantly using either option:'}
                  </p>
                </div>
              </div>

              {/* Action Buttons to bypass the issue */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleDownloadManifest}
                  className="py-2.5 px-3 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-black text-xs flex items-center justify-center gap-2 transition-transform active:scale-95 cursor-pointer shadow-md shadow-amber-500/20"
                >
                  <FileJson className="w-4 h-4 shrink-0" />
                  <span>{language === 'ar' ? '1. تنزيل ملف manifest.json جاهز' : '1. Download manifest.json'}</span>
                </button>

                <button
                  type="button"
                  onClick={handleCopyManifestJson}
                  className="py-2.5 px-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white font-bold text-xs flex items-center justify-center gap-2 border border-neutral-700 transition-colors cursor-pointer"
                >
                  <Copy className="w-3.5 h-3.5 shrink-0 text-amber-400" />
                  <span>{language === 'ar' ? '2. نسخ كود الـ Manifest' : '2. Copy Manifest Code'}</span>
                </button>
              </div>

              {/* Pre-configured App fields for PWABuilder manual entry */}
              <div className="space-y-2 pt-2 border-t border-amber-500/20">
                <div className="text-[11px] font-bold text-neutral-300">
                  {language === 'ar' ? 'أو انسخ بيانات التطبيق وضعها في حقول الموقع مباشرة:' : 'Or copy and paste these fields into PWABuilder:'}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px]">
                  <div className="p-2.5 rounded-xl bg-neutral-900/90 border border-neutral-800 flex items-center justify-between">
                    <div>
                      <span className="text-neutral-400 block text-[10px]">{language === 'ar' ? 'اسم التطبيق (App Name):' : 'App Name:'}</span>
                      <strong className="text-white font-mono text-xs">HATCH</strong>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        navigator.clipboard?.writeText('HATCH');
                        showToast(language === 'ar' ? 'تم نسخ اسم التطبيق HATCH' : 'Copied HATCH', 'info');
                      }}
                      className="px-2.5 py-1 rounded-lg bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 text-[11px] font-bold cursor-pointer transition-colors"
                    >
                      {language === 'ar' ? 'نسخ' : 'Copy'}
                    </button>
                  </div>

                  <div className="p-2.5 rounded-xl bg-neutral-900/90 border border-neutral-800 flex items-center justify-between">
                    <div>
                      <span className="text-neutral-400 block text-[10px]">{language === 'ar' ? 'معرّف الحزمة (Package ID):' : 'Package ID:'}</span>
                      <strong className="text-amber-400 font-mono text-[10px]">com.hatch.marketplace</strong>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        navigator.clipboard?.writeText('com.hatch.marketplace');
                        showToast(language === 'ar' ? 'تم نسخ معرّف الحزمة com.hatch.marketplace' : 'Copied com.hatch.marketplace', 'info');
                      }}
                      className="px-2.5 py-1 rounded-lg bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 text-[11px] font-bold cursor-pointer transition-colors"
                    >
                      {language === 'ar' ? 'نسخ' : 'Copy'}
                    </button>
                  </div>
                </div>
              </div>

              {/* Recommended native Android install shortcut */}
              <div className="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-500/30 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span className="text-[11px] text-emerald-300 font-medium">
                    {language === 'ar' ? 'أفضل وأسرع طريقة: التثبيت المباشر على الهاتف بدون أي مواقع' : 'Best method: Direct install without external websites'}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => setActiveTab('instant')}
                  className="px-2.5 py-1 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-neutral-950 text-[10px] font-black shrink-0 transition-transform active:scale-95 cursor-pointer"
                >
                  {language === 'ar' ? 'الانتقال للتثبيت ⚡' : 'Install Directly ⚡'}
                </button>
              </div>
            </div>

            {/* PWA 100% Score Checklist */}
            <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <h4 className="text-xs font-bold text-white">
                    {language === 'ar' ? 'جاهزية تطبيق هتش لتوليد ملف APK موقّع' : 'HATCH APK Generation Readiness'}
                  </h4>
                </div>
                <span className="text-[10px] font-black bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                  100% PWA Score
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2 text-[11px]">
                <div className="p-2.5 rounded-xl bg-neutral-900 border border-neutral-800/80 flex items-center gap-2 text-neutral-300">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{language === 'ar' ? 'ملف Manifest.json كامل' : 'Complete Manifest'}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-neutral-900 border border-neutral-800/80 flex items-center gap-2 text-neutral-300">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{language === 'ar' ? 'أيقونات 192/512 + Maskable' : 'PWA & Maskable Icons'}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-neutral-900 border border-neutral-800/80 flex items-center gap-2 text-neutral-300">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{language === 'ar' ? 'Service Worker نشط ومحدث' : 'Service Worker Active'}</span>
                </div>
                <div className="p-2.5 rounded-xl bg-neutral-900 border border-neutral-800/80 flex items-center gap-2 text-neutral-300">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{language === 'ar' ? 'صفحة Offline جاهزة' : 'Offline Page Ready'}</span>
                </div>
              </div>
            </div>

            {/* Standalone APK Generator (PWABuilder) */}
            <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-3">
              <div className="flex items-center gap-2">
                <Download className="w-4 h-4 text-amber-400" />
                <h4 className="text-xs font-bold text-white">
                  {language === 'ar' ? 'فتح موقع PWABuilder (جوجل ومايكروسوفت)' : 'Open PWABuilder'}
                </h4>
              </div>
              <p className="text-[11px] text-neutral-300 leading-relaxed">
                {language === 'ar'
                  ? 'يمكنك زيارة موقع PWABuilder وتحميل ملف APK موقّع أو حزمة AAB لمتجر جوجل بلاي.'
                  : 'Visit PWABuilder to package signed APK or AAB bundle for Google Play.'}
              </p>

              <div className="flex flex-col sm:flex-row items-center gap-2 pt-1">
                <a
                  href={pwaBuilderUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:flex-1 py-3 px-4 rounded-xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 text-neutral-950 font-black text-xs flex items-center justify-center gap-2 shadow-lg shadow-amber-500/20 hover:scale-[1.01] active:scale-[0.99] transition-all cursor-pointer"
                >
                  <Download className="w-4 h-4" />
                  <span>{language === 'ar' ? 'فتح PWABuilder في نافذة جديدة' : 'Open PWABuilder'}</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>

                <a
                  href="/offline.html"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:w-auto py-3 px-3.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs flex items-center justify-center gap-1.5 border border-neutral-700 transition-colors"
                  title="معاينة صفحة وضع عدم الاتصال"
                >
                  <span>{language === 'ar' ? 'معاينة صفحة Offline' : 'Preview Offline'}</span>
                </a>
              </div>
            </div>

            {/* Developer CLI Option */}
            <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-neutral-300">
                <Terminal className="w-3.5 h-3.5 text-amber-400" />
                <span>{language === 'ar' ? 'للمطورين: بناء APK مباشر عبر Google Bubblewrap CLI' : 'For Developers: Direct APK via Google Bubblewrap CLI'}</span>
              </div>
              <pre className="p-2.5 rounded-lg bg-neutral-900 border border-neutral-800 text-[11px] text-amber-300/90 font-mono overflow-x-auto">
{`# 1. Install Google Bubblewrap
npm i -g @bubblewrap/cli

# 2. Init Android project with HATCH manifest
bubblewrap init --manifest="${currentAppUrl}/manifest.json"

# 3. Build signed APK for Android
bubblewrap build`}
              </pre>
            </div>
          </div>
        )}

        {/* Modal Footer */}
        <div className="pt-3 mt-2 border-t border-neutral-800 flex justify-end">
          <button
            onClick={onClose}
            className="w-full py-2.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold rounded-xl border border-neutral-700 transition-colors cursor-pointer"
          >
            {language === 'ar' ? 'إغلاق' : 'Close'}
          </button>
        </div>
      </div>
    </div>
  );
};
