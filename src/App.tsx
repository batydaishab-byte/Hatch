/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { SwipeDeck } from './components/SwipeDeck';
import { MarketBrowse } from './components/MarketBrowse';
import { ChatView } from './components/ChatView';
import { ProfileView } from './components/ProfileView';
import { AdminView } from './components/AdminView';
import { ProductCard } from './components/ProductCard';
import { ProductDetailModal } from './components/ProductDetailModal';
import { MakeOfferModal } from './components/MakeOfferModal';
import { SellModal } from './components/SellModal';
import { SafetyGuideModal } from './components/SafetyGuideModal';
import { NotificationsModal } from './components/NotificationsModal';
import { ApkInstallModal } from './components/ApkInstallModal';
import { ReportViolationModal } from './components/ReportViolationModal';
import { CreateAccountModal } from './components/CreateAccountModal';
import { EditProfileModal } from './components/EditProfileModal';
import { UserRatingModal } from './components/UserRatingModal';
import { AdMobInterstitialModal } from './components/AdMobInterstitialModal';
import { AdMobRewardedModal } from './components/AdMobRewardedModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { SplashScreen } from './components/SplashScreen';
import { ToastContainer } from './components/ToastContainer';
import { Heart, Sparkles, PlusCircle } from 'lucide-react';

const MainContent: React.FC = () => {
  const { 
    activeTab, 
    products, 
    favorites, 
    setIsSellModalOpen, 
    isApkModalOpen, 
    setIsApkModalOpen, 
    t, 
    language 
  } = useApp();

  const renderActiveView = () => {
    switch (activeTab) {
      case 'swipe':
        return <SwipeDeck />;

      case 'browse':
        return <MarketBrowse />;

      case 'messages':
        return <ChatView />;

      case 'profile':
        return <ProfileView />;

      case 'admin':
        return <AdminView />;

      case 'favorites': {
        const savedList = products.filter((p) => favorites.includes(p.id));
        return (
          <div className="w-full max-w-7xl mx-auto px-3 sm:px-6 py-6 pb-24">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-2">
                <div className="w-9 h-9 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-500 flex items-center justify-center">
                  <Heart className="w-5 h-5 fill-current" />
                </div>
                <div>
                  <h2 className="text-lg font-black text-white">
                    {t.mySavedAds}
                  </h2>
                  <p className="text-xs text-neutral-400">
                    {savedList.length} {t.itemsFound}
                  </p>
                </div>
              </div>
            </div>

            {savedList.length > 0 ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-5">
                {savedList.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="text-center py-20 bg-neutral-900/40 rounded-3xl border border-neutral-800 p-8 max-w-md mx-auto">
                <Heart className="w-12 h-12 text-neutral-600 mx-auto mb-3" />
                <h3 className="text-base font-bold text-white mb-1">
                  {language === 'ar' ? 'قائمة المفضلة فارغة' : 'Your wishlist is empty'}
                </h3>
                <p className="text-xs text-neutral-400 mb-5 leading-relaxed">
                  {language === 'ar'
                    ? 'اسحب يميناً في واجهة هتش لحفظ المنتجات التي تعجبك وتقديم عروض فورية للبائعين.'
                    : 'Swipe right on items during card discovery to save them to your wishlist.'}
                </p>
              </div>
            )}
          </div>
        );
      }

      default:
        return <SwipeDeck />;
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 flex flex-col font-sans">
      <Header />
      
      <main className="flex-1 flex flex-col">
        {renderActiveView()}
      </main>

      <BottomNav />

      {/* Global Modals & Toasts */}
      <ProductDetailModal />
      <MakeOfferModal />
      <SellModal />
      <SafetyGuideModal />
      <NotificationsModal />
      <ApkInstallModal isOpen={isApkModalOpen} onClose={() => setIsApkModalOpen(false)} />
      <ReportViolationModal />
      <CreateAccountModal />
      <EditProfileModal />
      <UserRatingModal />
      <AdMobInterstitialModal />
      <AdMobRewardedModal />
      <OfflineIndicator />
      <SplashScreen />
      <ToastContainer />
    </div>
  );
};

export default function App() {
  return (
    <AppProvider>
      <MainContent />
    </AppProvider>
  );
}
