import React, { useState, useEffect } from 'react';
import { 
  ShieldAlert, 
  X, 
  Send, 
  AlertTriangle, 
  Package, 
  User as UserIcon, 
  CheckCircle2, 
  Info 
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export const ReportViolationModal: React.FC = () => {
  const { 
    reportModalData, 
    closeReportModal, 
    submitReport, 
    t, 
    language 
  } = useApp();

  const [selectedReason, setSelectedReason] = useState<string>('طلب تحويل عربون مسبق عبر بنكك');
  const [reportMessage, setReportMessage] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Reset form when modal opens or target changes
  useEffect(() => {
    if (reportModalData?.isOpen) {
      setSelectedReason(
        language === 'ar' ? 'طلب تحويل عربون مسبق عبر بنكك' : 'Advance deposit requested via Bankak'
      );
      setReportMessage('');
      setIsSubmitting(false);
    }
  }, [reportModalData, language]);

  if (!reportModalData?.isOpen) {
    return null;
  }

  const commonReasons = language === 'ar' ? [
    'طلب تحويل عربون مسبق عبر بنكك',
    'احتيال أو سعر وهمي / مضلل',
    'سلعة مقلدة أو تالفة مخالفة للصور',
    'رفض المعاينة واللقاء في مكان عام',
    'سوء سلوك أو رسائل غير لائقة',
    'أخرى (يرجى التوضيح في الرسالة أدناه)'
  ] : [
    'Advance deposit requested via Bankak',
    'Fraudulent or misleading price',
    'Counterfeit or damaged item',
    'Refusal of in-person public inspection',
    'Inappropriate behavior or harassment',
    'Other (Please describe in message bar below)'
  ];

  const quickPhrases = language === 'ar' ? [
    'طلب تحويل عربون بنكك قبل المعاينة',
    'السعر في الإعلان غير مطابق للواقع',
    'السلعة مقلدة وليست أصلية',
    'رفض التسليم باليد في مكان عام',
    'حساب مشبوه ومكرر'
  ] : [
    'Asked for advance deposit before inspection',
    'Price is different from listed amount',
    'Item appears counterfeit or non-genuine',
    'Refuses in-person handover in public place',
    'Suspicious or duplicate account'
  ];

  const handleAppendPhrase = (phrase: string) => {
    setReportMessage((prev) => {
      const trimmed = prev.trim();
      if (!trimmed) return phrase;
      return `${trimmed} - ${phrase}`;
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reportMessage.trim()) return;

    setIsSubmitting(true);
    submitReport({
      reportedProductId: reportModalData.productId,
      reportedProductTitle: reportModalData.productTitle,
      reportedUserId: reportModalData.userId,
      reportedUserName: reportModalData.userName,
      reason: selectedReason,
      message: reportMessage.trim(),
    });
  };

  const targetTitle = reportModalData.productTitle;
  const targetUser = reportModalData.userName;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-md animate-fade-in"
      onClick={closeReportModal}
    >
      <div 
        className="w-full max-w-lg bg-neutral-900 border border-red-500/30 rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh] animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="p-4 sm:p-5 bg-gradient-to-r from-red-950/60 via-neutral-900 to-amber-950/40 border-b border-red-900/40 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-500/20 text-red-400 border border-red-500/30 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-black text-white flex items-center gap-1.5">
                <span>{t.reportModalTitle}</span>
              </h3>
              <p className="text-xs text-neutral-400">
                {t.reportModalSubtitle}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={closeReportModal}
            className="p-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white transition-colors cursor-pointer"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <form onSubmit={handleSubmit} className="p-4 sm:p-6 overflow-y-auto space-y-4 no-scrollbar flex-1">
          {/* Target Info Pill */}
          <div className="p-3 rounded-2xl bg-neutral-950/80 border border-neutral-800 flex items-center gap-3 text-xs">
            {targetTitle ? (
              <>
                <div className="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center shrink-0">
                  <Package className="w-4 h-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-[10px] text-neutral-400">
                    {language === 'ar' ? 'الإعلان المُبلّغ عنه:' : 'Reported Listing:'}
                  </div>
                  <div className="font-bold text-white truncate">
                    {targetTitle}
                  </div>
                </div>
              </>
            ) : targetUser ? (
              <>
                <div className="w-8 h-8 rounded-xl bg-red-500/10 text-red-400 flex items-center justify-center shrink-0">
                  <UserIcon className="w-4 h-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="text-[10px] text-neutral-400">
                    {language === 'ar' ? 'المستخدم المُبلّغ عنه:' : 'Reported User:'}
                  </div>
                  <div className="font-bold text-white truncate">
                    {targetUser}
                  </div>
                </div>
              </>
            ) : (
              <div className="text-neutral-400">
                {language === 'ar' ? 'بلاغ عام عن مخالفة أمان' : 'General marketplace safety report'}
              </div>
            )}
            {targetUser && targetTitle && (
              <span className="text-[10px] text-neutral-400 bg-neutral-800 px-2 py-1 rounded-lg shrink-0">
                {language === 'ar' ? `المعلن: ${targetUser}` : `Seller: ${targetUser}`}
              </span>
            )}
          </div>

          {/* Reason Selection */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-neutral-300 flex items-center gap-1.5">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
              <span>{t.reportCategoryLabel}</span>
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {commonReasons.map((reason) => {
                const isSelected = selectedReason === reason;
                return (
                  <button
                    type="button"
                    key={reason}
                    onClick={() => setSelectedReason(reason)}
                    className={`p-2.5 rounded-xl text-start text-xs font-semibold transition-all cursor-pointer border ${
                      isSelected
                        ? 'bg-red-500/15 border-red-500 text-red-300 shadow-sm'
                        : 'bg-neutral-950/60 border-neutral-800 text-neutral-300 hover:border-neutral-700 hover:text-white'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center shrink-0 ${
                        isSelected ? 'border-red-400 bg-red-500' : 'border-neutral-600'
                      }`}>
                        {isSelected && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </span>
                      <span className="leading-snug">{reason}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* THE MESSAGE BAR / INPUT BOX */}
          <div className="space-y-2 pt-1">
            <div className="flex items-center justify-between">
              <label 
                htmlFor="report-message-input" 
                className="text-xs font-bold text-amber-400 flex items-center gap-1.5"
              >
                <span>{t.reportMessageBarLabel}</span>
                <span className="text-red-400">*</span>
              </label>
              <span className="text-[10px] text-neutral-400">
                {language === 'ar' ? 'يُرسل مباشرة لإدارة هتش' : 'Sent directly to HATCH Admin'}
              </span>
            </div>

            {/* Quick Helper Chips for one-click writing */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
              <span className="text-[10px] text-neutral-400 shrink-0">
                {language === 'ar' ? 'إضافة سريعة:' : 'Quick insert:'}
              </span>
              {quickPhrases.map((phrase) => (
                <button
                  type="button"
                  key={phrase}
                  onClick={() => handleAppendPhrase(phrase)}
                  className="text-[11px] bg-neutral-800 hover:bg-neutral-700 text-neutral-300 px-2.5 py-1 rounded-lg shrink-0 border border-neutral-700/80 transition-colors cursor-pointer hover:border-amber-400/40"
                >
                  + {phrase}
                </button>
              ))}
            </div>

            {/* The Textarea / Message Bar */}
            <div className="relative">
              <textarea
                id="report-message-input"
                rows={4}
                value={reportMessage}
                onChange={(e) => setReportMessage(e.target.value)}
                placeholder={t.reportMessageBarPlaceholder}
                className="w-full bg-neutral-950 border border-neutral-700 focus:border-red-500 focus:ring-1 focus:ring-red-500 rounded-2xl p-3.5 text-xs text-white placeholder-neutral-500 outline-none transition-all leading-relaxed"
                autoFocus
                required
              />
              <div className="text-[10px] text-neutral-500 text-end mt-1">
                {reportMessage.length} {language === 'ar' ? 'حرف' : 'characters'}
              </div>
            </div>
          </div>

          {/* Advisory & Assurance Callout */}
          <div className="p-3 rounded-2xl bg-neutral-950/60 border border-neutral-800/80 flex items-start gap-2.5 text-xs">
            <Info className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <p className="text-[11px] text-neutral-400 leading-relaxed">
              {language === 'ar'
                ? 'تتولى إدارة هتش فحص البلاغات الواردة وتدقيق سجلات المحادثات وحسابات البائعين بشكل فوري لحماية المجتمع والحد من عمليات النصب في السودان.'
                : 'HATCH administration urgently investigates incoming reports and fraud alerts to keep our marketplace safe.'}
            </p>
          </div>

          {/* Action Buttons */}
          <div className="pt-2 flex flex-col sm:flex-row items-center gap-2.5">
            <button
              type="submit"
              disabled={!reportMessage.trim() || isSubmitting}
              className={`w-full sm:flex-1 py-3 px-4 rounded-xl font-bold text-xs sm:text-sm flex items-center justify-center gap-2 transition-all cursor-pointer shadow-lg ${
                !reportMessage.trim() || isSubmitting
                  ? 'bg-neutral-800 text-neutral-500 cursor-not-allowed border border-neutral-700'
                  : 'bg-gradient-to-r from-red-600 via-rose-600 to-red-700 hover:from-red-500 hover:to-rose-500 text-white shadow-red-600/30 active:scale-95'
              }`}
            >
              <Send className="w-4 h-4" />
              <span>{t.reportSendBtn}</span>
            </button>

            <button
              type="button"
              onClick={closeReportModal}
              className="w-full sm:w-auto px-4 py-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs transition-colors cursor-pointer"
            >
              {language === 'ar' ? 'إلغاء' : 'Cancel'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
