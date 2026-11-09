import React from 'react';
import { 
  X, 
  ShieldCheck, 
  AlertTriangle, 
  CreditCard, 
  MapPin, 
  Eye, 
  CheckCircle2, 
  Lock 
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export const SafetyGuideModal: React.FC = () => {
  const { isSafetyModalOpen, setIsSafetyModalOpen, t, language } = useApp();

  if (!isSafetyModalOpen) return null;

  const safetySteps = [
    {
      titleAr: 'التحقق المباشر من تطبيق بنكك (Bankak)',
      titleEn: 'Direct Verification on Official Bankak App',
      descAr: 'لا تقبل إطلاقاً لقطات الشاشة (الاسكرين شوت) أو رسائل الـ SMS التي يدعي المشتري أنها من بنكك، حيث يمكن تزييفها. افتح تطبيق بنكك بنفسك وتأكد من تحديث كشف الحساب ورؤية المبلغ والرصيد الفعلي.',
      descEn: 'Never rely solely on screenshot receipts or SMS alerts. Open your Bankak app directly, refresh your transaction statement, and verify the funds before handing over any item.',
      icon: <CreditCard className="w-5 h-5 text-emerald-400" />
    },
    {
      titleAr: 'المقابلة والمعاينة في أماكن عامة مزدحمة',
      titleEn: 'Meet in Bustling Public Places',
      descAr: 'اتفق دائماً على اللقاء في أماكن عامة وآمنة نهاراً مثل: مول عفراء أو سيتي سنتر، كورنيش بورتسودان، شارع النيل، أو مقاهي ومطاعم رئيسية معروفة داخل مدينتك.',
      descEn: 'Always schedule handovers in well-lit public spots such as Afra Mall, Port Sudan Corniche, Nile Street, or familiar city market centers during daylight hours.',
      icon: <MapPin className="w-5 h-5 text-amber-400" />
    },
    {
      titleAr: 'فحص السلعة بدقة قبل إتمام الدفع',
      titleEn: 'Thorough Inspection Before Payment',
      descAr: 'قم بفحص السلعة شخصياً: تجربة تشغيل الأجهزة الإلكترونية والهواتف، فحص كفاءة منظومة الطاقة الشمسية أو سلامة بطارية الليثيوم، والتأكد من مطابقة المواصفات المذكورة في إعلان هتش.',
      descEn: 'Test electronics, verify battery health, inspect solar inverters, and verify the cosmetic condition matches the photos listed on HATCH.',
      icon: <Eye className="w-5 h-5 text-blue-400" />
    },
    {
      titleAr: 'احذر طلب تحويل العربون المسبق',
      titleEn: 'Beware of Advance Deposit Requests',
      descAr: 'تطبيق هتش يحذر من تحويل أي مبالغ مقدمة أو رسوم شحن مجهولة قبل رؤية البائع والسلعة وجهاً لوجه وفحصها بالكامل.',
      descEn: 'HATCH strictly advises against paying advance deposits or blind delivery fees before inspecting the physical merchandise in person.',
      icon: <AlertTriangle className="w-5 h-5 text-rose-400" />
    }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-neutral-950/85 backdrop-blur-md">
      <div className="relative w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-5 sm:p-7 overflow-y-auto max-h-[90vh] no-scrollbar">
        {/* Close Button */}
        <button
          onClick={() => setIsSafetyModalOpen(false)}
          className="absolute top-4 end-4 w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 flex items-center justify-center transition-colors cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3 mb-5">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <div>
            <h3 className="text-lg font-black text-white">
              {t.safetyNoticeTitle}
            </h3>
            <p className="text-xs text-neutral-400">
              {language === 'ar' ? 'إرشادات هتش لمعاملات تجارية موثوقة وخالية من الاحتيال' : 'HATCH Trust & Verification Standards in Sudan'}
            </p>
          </div>
        </div>

        {/* Steps */}
        <div className="space-y-3.5 mb-6">
          {safetySteps.map((step, idx) => (
            <div key={idx} className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800/80 flex items-start gap-3">
              <div className="p-2 rounded-xl bg-neutral-900 border border-neutral-800 shrink-0 mt-0.5">
                {step.icon}
              </div>
              <div className="space-y-1">
                <h4 className="text-xs font-bold text-white">
                  {language === 'ar' ? step.titleAr : step.titleEn}
                </h4>
                <p className="text-[11px] text-neutral-400 leading-relaxed">
                  {language === 'ar' ? step.descAr : step.descEn}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Action Button */}
        <button
          onClick={() => setIsSafetyModalOpen(false)}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-xs sm:text-sm shadow-lg shadow-amber-500/20 transition-all cursor-pointer"
        >
          {language === 'ar' ? 'فهمت، شكراً' : 'Understood, Thank you'}
        </button>
      </div>
    </div>
  );
};
