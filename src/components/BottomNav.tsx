import React from 'react';
import { 
  Flame, 
  Store, 
  PlusCircle, 
  MessageSquare, 
  Heart, 
  User, 
  ShieldAlert 
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { ActiveTab } from '../types';

export const BottomNav: React.FC = () => {
  const { activeTab, setActiveTab, t, unreadMessagesCount, setIsSellModalOpen, currentUser, setActiveConversationId } = useApp();

  const navItems: { tab: ActiveTab; label: string; icon: React.ReactNode; isAction?: boolean }[] = [
    {
      tab: 'swipe',
      label: t.tabSwipe,
      icon: <Flame className="w-5 h-5" />,
    },
    {
      tab: 'browse',
      label: t.tabBrowse,
      icon: <Store className="w-5 h-5" />,
    },
    {
      tab: 'sell',
      label: t.tabSell,
      icon: <PlusCircle className="w-6 h-6 text-neutral-950" />,
      isAction: true,
    },
    {
      tab: 'messages',
      label: t.tabMessages,
      icon: (
        <div className="relative">
          <MessageSquare className="w-5 h-5" />
          {unreadMessagesCount > 0 && (
            <span className="absolute -top-1 -right-2 bg-amber-500 text-neutral-950 text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
              {unreadMessagesCount}
            </span>
          )}
        </div>
      ),
    },
    {
      tab: 'favorites',
      label: t.tabFavorites,
      icon: <Heart className="w-5 h-5" />,
    },
    {
      tab: 'profile',
      label: t.tabProfile,
      icon: <User className="w-5 h-5" />,
    },
  ];

  if (currentUser.role === 'admin') {
    navItems.push({
      tab: 'admin',
      label: t.tabAdmin,
      icon: <ShieldAlert className="w-5 h-5 text-red-400" />,
    });
  }

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-neutral-900/95 backdrop-blur-lg border-t border-neutral-800 pb-safe">
      <div className="max-w-md md:max-w-xl mx-auto px-2 flex items-center justify-around h-16">
        {navItems.map((item) => {
          if (item.isAction) {
            return (
              <button
                key={item.tab}
                onClick={() => setIsSellModalOpen(true)}
                className="group relative -top-3 flex flex-col items-center cursor-pointer focus:outline-none"
                title={item.label}
              >
                <div className="w-13 h-13 rounded-full bg-gradient-to-tr from-amber-500 via-yellow-400 to-amber-300 flex items-center justify-center shadow-lg shadow-amber-500/30 group-hover:scale-105 group-active:scale-95 transition-all">
                  {item.icon}
                </div>
                <span className="text-[11px] font-bold text-amber-400 mt-0.5">
                  {item.label}
                </span>
              </button>
            );
          }

          const isActive = activeTab === item.tab;

          return (
            <button
              key={item.tab}
              onClick={() => {
                if (item.tab === 'messages') {
                  setActiveConversationId(null);
                }
                setActiveTab(item.tab);
              }}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all cursor-pointer ${
                isActive ? 'text-amber-400 scale-105 font-bold' : 'text-neutral-400 hover:text-neutral-200'
              }`}
            >
              <div className="relative">
                {item.icon}
                {isActive && (
                  <span className="absolute -bottom-1 left-1/2 transform -translate-x-1/2 w-1 h-1 bg-amber-400 rounded-full"></span>
                )}
              </div>
              <span className="text-[10px] mt-1 tracking-tight truncate max-w-[64px]">
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
