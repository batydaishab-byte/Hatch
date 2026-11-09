import React from 'react';
import { CheckCircle2, AlertCircle, Info } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const ToastContainer: React.FC = () => {
  const { toasts } = useApp();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-20 inset-x-4 z-50 pointer-events-none flex flex-col items-center gap-2 max-w-md mx-auto">
      {toasts.map((toast) => {
        let borderClass = 'border-neutral-700 bg-neutral-900/95 text-white';
        let icon = <Info className="w-4 h-4 text-amber-400 shrink-0" />;

        if (toast.type === 'success') {
          borderClass = 'border-emerald-500/40 bg-neutral-900/95 text-white shadow-emerald-950/40';
          icon = <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />;
        } else if (toast.type === 'warning') {
          borderClass = 'border-rose-500/40 bg-neutral-900/95 text-white shadow-rose-950/40';
          icon = <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />;
        }

        return (
          <div
            key={toast.id}
            className={`pointer-events-auto px-4 py-2.5 rounded-2xl border shadow-xl backdrop-blur-md flex items-center gap-2.5 text-xs font-semibold animate-in fade-in slide-in-from-bottom-2 duration-200 max-w-full ${borderClass}`}
          >
            {icon}
            <span className="truncate">{toast.message}</span>
          </div>
        );
      })}
    </div>
  );
};
