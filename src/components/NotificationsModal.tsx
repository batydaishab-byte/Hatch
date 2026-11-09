import { 
  X, 
  Bell, 
  Check, 
  CheckCheck, 
  Zap, 
  MessageSquare, 
  TrendingDown, 
  ShieldCheck,
  AlertTriangle,
  Sparkles,
  Megaphone,
  Tag
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export const NotificationsModal: React.FC = () => {
  const { 
    isNotificationsOpen, 
    setIsNotificationsOpen, 
    notifications, 
    markNotificationsAsRead, 
    setActiveConversationId, 
    setActiveTab, 
    t, 
    language 
  } = useApp();

  if (!isNotificationsOpen) return null;

  const handleNotificationClick = (convId?: string) => {
    if (convId) {
      setActiveConversationId(convId);
      setActiveTab('messages');
      setIsNotificationsOpen(false);
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'offer':
        return <Tag className="w-4 h-4 text-amber-400" />;
      case 'alert':
        return <AlertTriangle className="w-4 h-4 text-rose-400" />;
      case 'update':
        return <Sparkles className="w-4 h-4 text-cyan-400" />;
      case 'message':
        return <MessageSquare className="w-4 h-4 text-blue-400" />;
      case 'price_drop':
        return <TrendingDown className="w-4 h-4 text-emerald-400" />;
      case 'verification':
        return <ShieldCheck className="w-4 h-4 text-emerald-400" />;
      case 'system':
      default:
        return <Megaphone className="w-4 h-4 text-amber-400" />;
    }
  };

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'offer':
        return <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-md bg-amber-500/15 text-amber-300 border border-amber-500/30">{language === 'ar' ? 'عرض جديد' : 'New Offer'}</span>;
      case 'alert':
        return <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-md bg-rose-500/15 text-rose-300 border border-rose-500/30">{language === 'ar' ? 'تنبيه إداري' : 'Alert'}</span>;
      case 'update':
        return <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-md bg-cyan-500/15 text-cyan-300 border border-cyan-500/30">{language === 'ar' ? 'مستجدات' : 'Update'}</span>;
      case 'verification':
        return <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-md bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">{language === 'ar' ? 'توثيق' : 'Verified'}</span>;
      case 'price_drop':
        return <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-md bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">{language === 'ar' ? 'تخفيض' : 'Price Drop'}</span>;
      default:
        return null;
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-neutral-950/85 backdrop-blur-md">
      <div className="relative w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-5 overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
          <div className="flex items-center gap-2">
            <Bell className="w-5 h-5 text-amber-400" />
            <h3 className="font-bold text-base text-white">
              {t.notificationsTitle}
            </h3>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={markNotificationsAsRead}
              className="text-xs text-neutral-400 hover:text-amber-300 flex items-center gap-1 cursor-pointer"
              title={t.markAllRead}
            >
              <CheckCheck className="w-4 h-4" />
              <span className="hidden sm:inline">{t.markAllRead}</span>
            </button>

            <button
              onClick={() => setIsNotificationsOpen(false)}
              className="w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 flex items-center justify-center cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* List */}
        <div className="overflow-y-auto divide-y divide-neutral-800/80 flex-1 my-2 no-scrollbar">
          {notifications.length > 0 ? (
            notifications.map((notif) => (
              <div
                key={notif.id}
                onClick={() => handleNotificationClick(notif.relatedConversationId)}
                className={`p-3 flex items-start gap-3 transition-colors cursor-pointer ${
                  !notif.read ? 'bg-amber-500/5' : 'hover:bg-neutral-850'
                }`}
              >
                <div className="p-2 rounded-xl bg-neutral-950 border border-neutral-800 shrink-0 mt-0.5">
                  {getIcon(notif.type)}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-1 mb-1">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <h4 className="text-xs font-bold text-white truncate">
                        {language === 'ar' ? notif.titleAr : notif.titleEn}
                      </h4>
                      {getTypeBadge(notif.type)}
                    </div>
                    <span className="text-[10px] text-neutral-500 shrink-0">
                      {notif.time}
                    </span>
                  </div>

                  <p className="text-xs text-neutral-300 line-clamp-3 leading-relaxed">
                    {language === 'ar' ? notif.bodyAr : notif.bodyEn}
                  </p>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12 text-neutral-400 text-xs">
              {t.noNotifications}
            </div>
          )}
        </div>

        {/* Footer */}
        <button
          onClick={() => setIsNotificationsOpen(false)}
          className="w-full py-2.5 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold rounded-xl border border-neutral-700 transition-colors cursor-pointer mt-2"
        >
          {language === 'ar' ? 'إغلاق' : 'Close'}
        </button>
      </div>
    </div>
  );
};
