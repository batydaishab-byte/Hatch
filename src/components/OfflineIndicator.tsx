import React from 'react';
import { WifiOff, RotateCcw } from 'lucide-react';
import { useOnlineStatus } from '../hooks/useOnlineStatus';
import { useApp } from '../context/AppContext';

export const OfflineIndicator: React.FC = () => {
  const isOnline = useOnlineStatus();
  const { language } = useApp();

  if (isOnline) return null;

  return (
    <div className="fixed bottom-20 start-3 end-3 sm:start-auto sm:end-6 z-40 max-w-sm flex items-center justify-between gap-3 p-3 rounded-2xl bg-amber-500/95 text-neutral-950 font-bold shadow-2xl backdrop-blur-md animate-in slide-in-from-bottom-5">
      <div className="flex items-center gap-2.5 text-xs">
        <div className="w-8 h-8 rounded-xl bg-neutral-950/20 flex items-center justify-center shrink-0">
          <WifiOff className="w-4 h-4 text-neutral-950" />
        </div>
        <div>
          <p className="text-xs font-black">
            {language === 'ar' ? 'أنت غير متصل بالإنترنت' : 'You are currently offline'}
          </p>
          <p className="text-[10px] text-neutral-900/80 font-medium">
            {language === 'ar' ? 'يتم استخدام البيانات المخزنة مؤقتاً' : 'Serving from local cache'}
          </p>
        </div>
      </div>

      <button
        onClick={() => window.location.reload()}
        className="px-2.5 py-1.5 rounded-xl bg-neutral-950 text-amber-400 text-xs font-black flex items-center gap-1 hover:bg-neutral-900 transition-colors cursor-pointer shrink-0"
      >
        <RotateCcw className="w-3.5 h-3.5" />
        <span>{language === 'ar' ? 'تحديث' : 'Retry'}</span>
      </button>
    </div>
  );
};
