import React from 'react';
import { 
  ShieldAlert, 
  X, 
  Check, 
  Trash2, 
  AlertTriangle, 
  Package, 
  User as UserIcon, 
  CheckCircle2, 
  CreditCard,
  Zap,
  Mic,
  Tag
} from 'lucide-react';
import { Conversation, Product, ReportTicket } from '../types';
import { useApp } from '../context/AppContext';
import { AudioMessagePlayer } from './AudioMessagePlayer';

interface AdminConversationModalProps {
  isOpen: boolean;
  onClose: () => void;
  conversation: Conversation | null;
  report?: ReportTicket | null;
}

export const AdminConversationModal: React.FC<AdminConversationModalProps> = ({
  isOpen,
  onClose,
  conversation,
  report
}) => {
  const { 
    products, 
    adminDeleteProduct, 
    adminResolveReport,
    flagProduct,
    showToast, 
    t, 
    language 
  } = useApp();

  if (!isOpen || !conversation) return null;

  const product = products.find((p) => p.id === conversation.productId);

  const handleConfirmViolation = () => {
    if (report) {
      adminResolveReport(report.id, 'resolved');
    }
    if (conversation.productId) {
      flagProduct(conversation.productId, 'تم تأكيد المخالفة بعد مراجعة سجل المحادثة');
    }
    showToast(
      language === 'ar' 
        ? 'تم تأكيد صحة البلاغ رسمياً وسحب التوثيق من الحساب المخالف 🛡️' 
        : 'Violation confirmed after chat audit, badge penalized 🛡️',
      'warning'
    );
    onClose();
  };

  const handleDeleteListing = () => {
    if (conversation.productId) {
      adminDeleteProduct(conversation.productId);
      if (report) {
        adminResolveReport(report.id, 'resolved');
      }
      onClose();
    }
  };

  const handleDismissReport = () => {
    if (report) {
      adminResolveReport(report.id, 'dismissed');
    }
    showToast(
      language === 'ar' ? 'تم حفظ البلاغ كبلاغ غير مطابق بعد التدقيق' : 'Report dismissed after audit',
      'info'
    );
    onClose();
  };

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/85 backdrop-blur-md animate-fade-in"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh] animate-scale-up"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="p-4 sm:p-5 bg-gradient-to-r from-red-950/60 via-neutral-900 to-amber-950/40 border-b border-red-900/40 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-500/20 text-red-400 border border-red-500/30 flex items-center justify-center shrink-0">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-black text-white flex items-center gap-2">
                <span>{t.conversationAuditTitle}</span>
                <span className="text-[10px] bg-red-500/20 text-red-300 border border-red-500/30 px-2 py-0.5 rounded-full font-mono">
                  Audit #{conversation.id.slice(-4)}
                </span>
              </h3>
              <p className="text-xs text-neutral-400">
                {t.conversationAuditSubtitle}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Audit Context Bar */}
        <div className="p-3.5 bg-neutral-950 border-b border-neutral-800 space-y-2">
          {report && (
            <div className="p-2.5 rounded-xl bg-red-950/30 border border-red-500/30 flex items-start gap-2.5 text-xs">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <div className="min-w-0 flex-1">
                <div className="text-red-300 font-bold">
                  {language === 'ar' ? 'سبب البلاغ قيد التدقيق:' : 'Report reason under audit:'} {report.reason}
                </div>
                <div className="text-neutral-300 text-[11px] mt-0.5 leading-relaxed">
                  "{report.message}"
                </div>
              </div>
            </div>
          )}

          {/* Product & Participants info */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2.5">
              <img
                src={conversation.productImage || (product?.images[0])}
                alt={conversation.productTitle}
                className="w-10 h-10 rounded-xl object-cover border border-neutral-800 shrink-0"
              />
              <div className="min-w-0">
                <div className="text-white font-bold truncate">
                  {conversation.productTitle}
                </div>
                <div className="text-[11px] text-amber-400 font-mono font-bold">
                  {conversation.productPrice?.toLocaleString()} {t.currency}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 text-neutral-400 text-[11px]">
              <span className="bg-neutral-900 px-2 py-1 rounded-lg border border-neutral-800">
                {language === 'ar' ? `المحاور: ${conversation.participantName}` : `User: ${conversation.participantName}`}
              </span>
              {report?.reporterName && (
                <span className="bg-neutral-900 px-2 py-1 rounded-lg border border-neutral-800">
                  {language === 'ar' ? `المُبلّغ: ${report.reporterName}` : `Reporter: ${report.reporterName}`}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Message Transcript Body */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-neutral-950/60 no-scrollbar">
          <div className="text-center my-2">
            <span className="text-[10px] bg-neutral-900 text-neutral-400 border border-neutral-800 px-3 py-1 rounded-full font-mono">
              {language === 'ar' ? 'بداية سجل المحادثة الموثق' : 'Beginning of audited chat log'}
            </span>
          </div>

          {conversation.messages.map((msg) => {
            const isReporter = report ? (msg.senderId === report.reporterId) : false;
            const containsDepositWarning = msg.text.includes('عربون') || msg.text.includes('بنكك');

            return (
              <div 
                key={msg.id}
                className={`p-3 rounded-2xl max-w-[85%] space-y-1.5 transition-all text-xs ${
                  isReporter 
                    ? 'ms-auto bg-amber-950/40 border border-amber-500/30 text-white rounded-ee-none'
                    : 'me-auto bg-neutral-900 border border-neutral-800 text-neutral-200 rounded-es-none'
                }`}
              >
                <div className="flex items-center justify-between gap-3 text-[10px] text-neutral-400 border-b border-neutral-800/60 pb-1">
                  <span className="font-bold text-neutral-300">
                    {isReporter ? (report?.reporterName || 'المُبلّغ') : conversation.participantName}
                  </span>
                  <span>{msg.timestamp}</span>
                </div>

                {/* Text body */}
                <p className="leading-relaxed whitespace-pre-line text-[11px] sm:text-xs">
                  {msg.text}
                </p>

                {/* Red Flag Warning Indicator */}
                {containsDepositWarning && (
                  <div className="text-[10px] font-bold text-red-400 bg-red-950/60 border border-red-500/40 px-2 py-0.5 rounded-md flex items-center gap-1 w-fit">
                    <AlertTriangle className="w-3 h-3" />
                    <span>{language === 'ar' ? 'تنبيه إدارة: إشارة لتحويل عربون بنكك' : 'Admin Alert: Advance deposit keyword'}</span>
                  </div>
                )}

                {/* Audio message if present */}
                {msg.audioUrl && (
                  <div className="pt-1">
                    <AudioMessagePlayer
                      audioUrl={msg.audioUrl}
                      duration={msg.audioDuration || 4}
                      isMe={isReporter}
                    />
                  </div>
                )}

                {/* Image attachment if present */}
                {msg.imageUrl && (
                  <div className="pt-1">
                    <img
                      src={msg.imageUrl}
                      alt="Inspection attachment"
                      className="max-h-40 rounded-xl object-cover border border-neutral-700"
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Administration Action Footer */}
        <div className="p-3 sm:p-4 bg-neutral-900 border-t border-neutral-800 flex flex-col sm:flex-row items-center justify-between gap-2.5">
          <div className="flex items-center gap-2 w-full sm:w-auto">
            {/* Confirm Violation */}
            <button
              type="button"
              onClick={handleConfirmViolation}
              className="flex-1 sm:flex-initial px-3.5 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-500 hover:to-rose-500 text-white font-bold text-xs flex items-center justify-center gap-1.5 shadow-md shadow-red-900/40 cursor-pointer active:scale-95"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>{language === 'ar' ? 'تأكيد صحة البلاغ وعقوبة الحساب' : 'Confirm Violation'}</span>
            </button>

            {/* Delete listing */}
            {conversation.productId && (
              <button
                type="button"
                onClick={handleDeleteListing}
                className="px-3 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-rose-400 hover:text-white font-bold text-xs flex items-center justify-center gap-1.5 border border-neutral-700 transition-colors cursor-pointer"
                title={t.deleteListing}
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">{t.deleteListing}</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
            {/* Dismiss report as invalid */}
            <button
              type="button"
              onClick={handleDismissReport}
              className="flex-1 sm:flex-initial px-3 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs transition-colors cursor-pointer"
            >
              {language === 'ar' ? 'حفظ وتجاهل البلاغ' : 'Dismiss Report'}
            </button>

            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-white font-bold text-xs transition-colors cursor-pointer"
            >
              {language === 'ar' ? 'إغلاق' : 'Close'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
