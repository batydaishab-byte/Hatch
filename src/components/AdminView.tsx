import React, { useState } from 'react';
import { 
  ShieldAlert, 
  Check, 
  Trash2, 
  TrendingUp, 
  Package, 
  Users, 
  AlertTriangle, 
  ExternalLink,
  MessageSquareText,
  Clock,
  User as UserIcon,
  CheckCircle2,
  XCircle,
  FileText,
  ShoppingBag,
  Eye,
  MessageSquare,
  ShieldCheck,
  Megaphone,
  Send,
  Sparkles,
  Tag,
  Radio,
  Bell,
  ChevronDown,
  ChevronUp,
  Zap,
  DollarSign,
  Coins,
  BarChart3,
  PlayCircle,
  Layers,
  Settings2,
  Gift,
  RefreshCw,
  Plus,
  MousePointerClick,
  RotateCcw,
  Edit2,
  X,
  ToggleLeft,
  ToggleRight,
  Timer,
  Video,
  UploadCloud,
  Film,
  FileVideo,
  Play,
  UserX,
  UserCheck,
  Search,
  Filter,
  Shield,
  AlertOctagon,
  Calendar
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Conversation, ReportTicket, AdFormat, SponsoredAdItem, User } from '../types';
import { AdminConversationModal } from './AdminConversationModal';

export const AdminView: React.FC = () => {
  const { 
    products, 
    conversations,
    availableUsers,
    currentUser,
    adminDeleteUser,
    adminSuspendUser,
    adminUnsuspendUser,
    switchUser,
    adminApproveProduct, 
    adminDeleteProduct, 
    setSelectedProductForDetail, 
    reports,
    adminResolveReport,
    broadcastNotification,
    notifications,
    adMobConfig,
    updateAdMobConfig,
    adStats,
    sponsoredAds,
    addSponsoredAd,
    updateSponsoredAd,
    toggleSponsoredAdStatus,
    deleteSponsoredAd,
    resetAdStats,
    recordAdClick,
    recordAdImpression,
    showInterstitialAd,
    showRewardedAd,
    t, 
    language 
  } = useApp();

  const [activeAdminTab, setActiveAdminTab] = useState<'reports' | 'broadcast' | 'sold' | 'conversations' | 'flagged' | 'monetization' | 'users'>('reports');
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [selectedReport, setSelectedReport] = useState<ReportTicket | null>(null);
  const [isChatAuditModalOpen, setIsChatAuditModalOpen] = useState(false);

  // User Management states
  const [userSearchQuery, setUserSearchQuery] = useState('');
  const [userStatusFilter, setUserStatusFilter] = useState<'all' | 'active' | 'suspended' | 'verified'>('all');
  const [selectedUserForSuspend, setSelectedUserForSuspend] = useState<User | null>(null);
  const [isSuspendModalOpen, setIsSuspendModalOpen] = useState(false);
  const [suspensionDays, setSuspensionDays] = useState<number>(7);
  const [customDaysInput, setCustomDaysInput] = useState('');
  const [isCustomDays, setIsCustomDays] = useState(false);
  const [suspensionReason, setSuspensionReason] = useState('');
  const [selectedUserForDelete, setSelectedUserForDelete] = useState<User | null>(null);
  const [isDeleteConfirmModalOpen, setIsDeleteConfirmModalOpen] = useState(false);

  // Broadcast bar states
  const [broadcastType, setBroadcastType] = useState<'offer' | 'alert' | 'update' | 'system'>('offer');
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastBody, setBroadcastBody] = useState('');
  const [broadcastProductId, setBroadcastProductId] = useState('');
  const [isBroadcastExpanded, setIsBroadcastExpanded] = useState(true);

  // AdMob configuration form states
  const [configAppId, setConfigAppId] = useState(adMobConfig.admobAppId);
  const [configBannerId, setConfigBannerId] = useState(adMobConfig.bannerUnitId);
  const [configInterstitialId, setConfigInterstitialId] = useState(adMobConfig.interstitialUnitId);
  const [configRewardedId, setConfigRewardedId] = useState(adMobConfig.rewardedUnitId);
  const [configNativeId, setConfigNativeId] = useState(adMobConfig.nativeUnitId);
  const [configEcpm, setConfigEcpm] = useState(adMobConfig.ecpmRate.toString());
  const [configCpc, setConfigCpc] = useState(adMobConfig.cpcRate.toString());
  const [configBannerDuration, setConfigBannerDuration] = useState((adMobConfig.bannerRotationSeconds || 10).toString());
  const [configInterstitialDuration, setConfigInterstitialDuration] = useState((adMobConfig.interstitialCountdownSeconds || 5).toString());
  const [configRewardedDuration, setConfigRewardedDuration] = useState((adMobConfig.rewardedDurationSeconds || 15).toString());

  // Ad Management (Add / Edit / Delete) states
  const [isAddAdModalOpen, setIsAddAdModalOpen] = useState(false);
  const [editingDurationAdId, setEditingDurationAdId] = useState<string | null>(null);
  const [editingDurationValue, setEditingDurationValue] = useState<number>(10);

  // Form states for new ad
  const [newAdvertiser, setNewAdvertiser] = useState('');
  const [newTitleAr, setNewTitleAr] = useState('');
  const [newTitleEn, setNewTitleEn] = useState('');
  const [newDescAr, setNewDescAr] = useState('');
  const [newDescEn, setNewDescEn] = useState('');
  const [newFormat, setNewFormat] = useState<AdFormat>('all');
  const [newDuration, setNewDuration] = useState('12');
  const [newTargetUrl, setNewTargetUrl] = useState('');
  const [newCtaAr, setNewCtaAr] = useState('تواصل مع المعلن');
  const [newCtaEn, setNewCtaEn] = useState('Contact Advertiser');
  const [newImageUrl, setNewImageUrl] = useState('https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=800&q=80');
  const [newCategory, setNewCategory] = useState('general');

  // Video attachment states for AdMob campaigns
  const [newMediaType, setNewMediaType] = useState<'image' | 'video'>('image');
  const [newVideoUrl, setNewVideoUrl] = useState<string>('');
  const [newVideoFileName, setNewVideoFileName] = useState<string>('');
  const [newVideoFileSize, setNewVideoFileSize] = useState<string>('');
  const [isDraggingVideo, setIsDraggingVideo] = useState(false);
  const [previewingVideoAd, setPreviewingVideoAd] = useState<SponsoredAdItem | null>(null);
  const videoFileInputRef = React.useRef<HTMLInputElement | null>(null);

  const sampleAdVideos = [
    {
      title: 'فيديو شبكات وتطبيقات 4G',
      name: 'zain_4g_sudan.mp4',
      size: '4.2 MB',
      duration: 15,
      defaultAdvertiser: 'زين السودان (Zain)',
      defaultTitle: 'زين السودان - باقات إنترنت 4G غير محدودة',
      url: 'https://assets.mixkit.co/videos/preview/mixkit-software-developer-working-on-code-screen-close-up-1728-large.mp4',
    },
    {
      title: 'فيديو معاملات تطبيق بنكك',
      name: 'bankak_payment_sudan.mp4',
      size: '3.8 MB',
      duration: 15,
      defaultAdvertiser: 'بنك الخرطوم (BOK)',
      defaultTitle: 'تطبيق بنكك - معاملات مصرفية فورية وآمنة',
      url: 'https://assets.mixkit.co/videos/preview/mixkit-hands-holding-a-smartphone-with-a-green-screen-40344-large.mp4',
    },
    {
      title: 'فيديو الطاقة الشمسية',
      name: 'nile_solar_power.mp4',
      size: '5.1 MB',
      duration: 15,
      defaultAdvertiser: 'النيل للطاقة المتجددة ☀️',
      defaultTitle: 'منظومات طاقة شمسية هجينة بضمان 5 سنوات',
      url: 'https://assets.mixkit.co/videos/preview/mixkit-solar-panels-in-a-large-plant-42352-large.mp4',
    },
  ];

  const handleVideoFile = (file: File) => {
    if (!file) return;
    if (!file.type.startsWith('video/')) {
      alert(language === 'ar' ? 'الرجاء اختيار ملف فيديو مدعوم (MP4, WebM, MOV...)' : 'Please select a valid video file (MP4, WebM, MOV)');
      return;
    }

    const sizeMB = (file.size / (1024 * 1024)).toFixed(1);
    setNewVideoFileName(file.name);
    setNewVideoFileSize(`${sizeMB} MB`);
    setNewMediaType('video');

    const objectUrl = URL.createObjectURL(file);
    setNewVideoUrl(objectUrl);

    if (file.size < 12 * 1024 * 1024) {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target?.result) {
          setNewVideoUrl(e.target.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handleVideoInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleVideoFile(file);
  };

  const handleVideoDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDraggingVideo(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleVideoFile(file);
  };

  const handleRemoveVideo = () => {
    setNewVideoUrl('');
    setNewVideoFileName('');
    setNewVideoFileSize('');
    if (videoFileInputRef.current) {
      videoFileInputRef.current.value = '';
    }
  };

  const handleVideoMetadataLoaded = (e: React.SyntheticEvent<HTMLVideoElement>) => {
    const duration = Math.round(e.currentTarget.duration);
    if (duration && duration > 0) {
      setNewDuration(Math.min(60, Math.max(5, duration)).toString());
    }
  };

  const adPresets = [
    {
      label: 'زين السودان 🇸🇩',
      advertiser: 'زين السودان (Zain)',
      titleAr: 'زين السودان - باقات إنترنت 4G غير محدودة',
      titleEn: 'Zain Sudan - Unlimited 4G Internet Bundles',
      descAr: 'تصفح سوق هتش بدون توقف مع باقات 4G فائقة السرعة وشحن فوري.',
      descEn: 'High-speed 4G internet bundles across Sudan.',
      ctaAr: 'اشترك الآن',
      ctaEn: 'Subscribe Now',
      targetUrl: 'https://www.sd.zain.com',
      imageUrl: 'https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=800&q=80',
      category: 'telecom',
    },
    {
      label: 'تطبيق بنكك 💳',
      advertiser: 'بنك الخرطوم (BOK)',
      titleAr: 'تطبيق بنكك - معاملات مصرفية فورية وآمنة',
      titleEn: 'Bankak - Instant Secure Banking',
      descAr: 'سدد قيمة صفقاتك في هتش فوراً وبأمان تام مع إشعار تحويل لحظي.',
      descEn: 'Pay for your marketplace deals securely with instant confirmation.',
      ctaAr: 'فتح حساب فوري',
      ctaEn: 'Open Account',
      targetUrl: 'https://bankofkhartoum.com',
      imageUrl: 'https://images.unsplash.com/photo-1559526324-4b87b5e36e44?auto=format&fit=crop&w=800&q=80',
      category: 'banking',
    },
    {
      label: 'طاقة شمسية ☀️',
      advertiser: 'النيل للطاقة المتجددة',
      titleAr: 'منظومات طاقة شمسية هجينة بضمان 5 سنوات',
      titleEn: 'Hybrid Solar Power Systems 5Y Warranty',
      descAr: 'وفر كهرباء متواصلة لمنزلك أو متجرك في بورتسودان وعطبرة مع ألواح كندية.',
      descEn: 'Reliable solar energy solutions across Sudan.',
      ctaAr: 'طلب معاينة مجانية',
      ctaEn: 'Free Quote',
      targetUrl: 'https://example.com/solar-sudan',
      imageUrl: 'https://images.unsplash.com/photo-1509391365360-2e959784a276?auto=format&fit=crop&w=800&q=80',
      category: 'energy',
    },
    {
      label: 'شحن وتوصيل 🚚',
      advertiser: 'السودان إكسبريس للشحن',
      titleAr: 'توصيل سريع للطرود والمشتريات بين الولايات',
      titleEn: 'Sudan Express Courier - Fast & Insured',
      descAr: 'شحن آمن للطرود والمشتريات بين بورتسودان، شندي، القضارف وكوستي.',
      descEn: 'Fast intercity package shipping across Sudan.',
      ctaAr: 'حجز شحنة فوراً',
      ctaEn: 'Book Courier',
      targetUrl: 'https://example.com/express-sudan',
      imageUrl: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80',
      category: 'delivery',
    },
  ];

  const handleApplyPreset = (preset: typeof adPresets[0]) => {
    setNewAdvertiser(preset.advertiser);
    setNewTitleAr(preset.titleAr);
    setNewTitleEn(preset.titleEn);
    setNewDescAr(preset.descAr);
    setNewDescEn(preset.descEn);
    setNewCtaAr(preset.ctaAr);
    setNewCtaEn(preset.ctaEn);
    setNewTargetUrl(preset.targetUrl);
    setNewImageUrl(preset.imageUrl);
    setNewCategory(preset.category);
  };

  const handleCreateAd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newAdvertiser.trim() || !newTitleAr.trim()) return;

    addSponsoredAd({
      titleAr: newTitleAr.trim(),
      titleEn: newTitleEn.trim() || newTitleAr.trim(),
      advertiser: newAdvertiser.trim(),
      descriptionAr: newDescAr.trim() || 'عرض ترويجي مدعوم من شبكة AdMob',
      descriptionEn: newDescEn.trim() || 'Sponsored AdMob Campaign',
      imageUrl: newImageUrl.trim(),
      videoUrl: newMediaType === 'video' && newVideoUrl ? newVideoUrl : undefined,
      mediaType: newMediaType === 'video' && newVideoUrl ? 'video' : 'image',
      ctaTextAr: newCtaAr.trim() || 'معرفة المزيد',
      ctaTextEn: newCtaEn.trim() || 'Learn More',
      targetUrl: newTargetUrl.trim() || 'https://google.com',
      category: newCategory,
      badgeAr: newMediaType === 'video' ? 'إعلان فيديو • AdMob' : 'إعلان AdMob • مدعوم',
      badgeEn: newMediaType === 'video' ? 'AdMob Video Ad' : 'AdMob Ad • Sponsored',
      format: newFormat,
      isActive: true,
      durationSeconds: parseInt(newDuration, 10) || 12,
    });

    setIsAddAdModalOpen(false);
    // Reset form
    setNewAdvertiser('');
    setNewTitleAr('');
    setNewTitleEn('');
    setNewDescAr('');
    setNewDescEn('');
    setNewTargetUrl('');
    setNewVideoUrl('');
    setNewVideoFileName('');
    setNewVideoFileSize('');
    setNewMediaType('image');
  };

  const broadcastTemplates = [
    {
      label: '🏷️ عرض خاص على الطاقة الشمسية',
      type: 'offer' as const,
      title: 'عرض ترويجي: تخفيضات على منظومات الطاقة الشمسية ☀️',
      body: 'تتوفر الآن عروض وأسعار تفاوضية خاصة على ألواح الطاقة الشمسية ومحولات الطاقة في بورتسودان وبحري، تصفح العروض وتفاوض مع البائعين فوراً!',
    },
    {
      label: '⚠️ تنبيه أمني لحماية الحساب وبنكك',
      type: 'alert' as const,
      title: 'تنبيه أمان هام: لا تحول عربون مسبق قبل المعاينة الفعلية 🛡️',
      body: 'تذكر إدارة هتش جميع المتعاملين بعدم تحويل أي أموال عبر بنكك إلا بعد مقابلة البائع وفحص السلعة واستلامها يداً بيد.',
    },
    {
      label: '✨ مستجدات وتحديثات تطبيق هتش',
      type: 'update' as const,
      title: 'مستجدات هتش: إطلاق ميزة تعديل الملف الشخصي والتوثيق 🚀',
      body: 'يسر إدارة هتش الإعلان عن إمكانية تعديل بيانات حسابك الشخصي ومتابعة صفقاتك الناجحة للوصول لشارة التوثيق الذهبية.',
    },
    {
      label: '📢 إشعار عام من الإدارة',
      type: 'system' as const,
      title: 'إشعار من إدارة هتش: نرحب بجميع المستخدمين الجدد 🇸🇩',
      body: 'هتش هو سوقكم الحر في كل ولايات ومدن السودان، نلتزم بتوفير بيئة تجارية آمنة ونزيهة خالية من الاحتيال.',
    },
  ];

  const handleSendBroadcast = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!broadcastTitle.trim() || !broadcastBody.trim()) return;

    broadcastNotification({
      title: broadcastTitle.trim(),
      body: broadcastBody.trim(),
      type: broadcastType,
      relatedProductId: broadcastProductId || undefined,
    });

    setBroadcastTitle('');
    setBroadcastBody('');
    setBroadcastProductId('');
  };

  const totalListings = products.filter((p) => p.status === 'active').length;
  const totalVolumeSDG = products.reduce((acc, p) => acc + p.price, 0);
  const flaggedProducts = products.filter((p) => p.status === 'flagged');
  const soldProducts = products.filter((p) => p.status === 'sold');
  const activeSellersCount = new Set(products.map((p) => p.sellerId)).size;

  const handleReviewConversationForReport = (rep: ReportTicket) => {
    // Find matching conversation by product ID or by participant/reporter
    const match = conversations.find(
      (c) => c.productId === rep.reportedProductId || 
             c.participantId === rep.reportedUserId || 
             c.participantId === rep.reporterId
    ) || conversations[0]; // graceful fallback if none match exactly

    setSelectedConversation(match || null);
    setSelectedReport(rep);
    setIsChatAuditModalOpen(true);
  };

  const handleOpenConversationDirect = (conv: Conversation) => {
    setSelectedConversation(conv);
    setSelectedReport(null);
    setIsChatAuditModalOpen(true);
  };

  return (
    <div className="w-full max-w-6xl mx-auto px-3 sm:px-6 py-4 pb-24 space-y-6">
      {/* Admin Header */}
      <div className="p-6 rounded-3xl bg-gradient-to-r from-red-950/40 via-neutral-900 to-amber-950/40 border border-red-900/40 shadow-xl">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-2xl bg-red-500/20 text-red-400 flex items-center justify-center">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-black text-white">
              {t.adminTitle}
            </h2>
            <p className="text-xs text-neutral-400">
              {t.adminSubtitle}
            </p>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="p-4 rounded-2xl bg-neutral-900 border border-neutral-800">
          <div className="text-xs text-neutral-400 mb-1 flex items-center gap-1.5">
            <Package className="w-3.5 h-3.5 text-amber-400" />
            <span>{t.statTotalListings}</span>
          </div>
          <div className="text-2xl font-black text-white">
            {totalListings}
          </div>
        </div>

        {/* زر ومقياس الإعلانات التي تم بيعها */}
        <div 
          onClick={() => setActiveAdminTab('sold')}
          className="p-4 rounded-2xl bg-neutral-900 border border-neutral-800 hover:border-emerald-500/40 transition-all cursor-pointer group"
          title="عرض الإعلانات التي تم بيعها"
        >
          <div className="text-xs text-neutral-400 mb-1 flex items-center gap-1.5 group-hover:text-emerald-400 transition-colors">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>{t.statSoldListings}</span>
          </div>
          <div className="text-2xl font-black text-emerald-400">
            {soldProducts.length}
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-neutral-900 border border-neutral-800">
          <div className="text-xs text-neutral-400 mb-1 flex items-center gap-1.5">
            <MessageSquareText className="w-3.5 h-3.5 text-rose-400" />
            <span>{language === 'ar' ? 'بلاغات ورسائل المستخدمين' : 'User Reports'}</span>
          </div>
          <div className="text-2xl font-black text-rose-400">
            {reports.length}
          </div>
        </div>

        <div className="p-4 rounded-2xl bg-neutral-900 border border-neutral-800">
          <div className="text-xs text-neutral-400 mb-1 flex items-center gap-1.5">
            <TrendingUp className="w-3.5 h-3.5 text-emerald-400" />
            <span>{t.statTotalVolume}</span>
          </div>
          <div className="text-base sm:text-lg font-black text-amber-400 font-mono truncate">
            {totalVolumeSDG.toLocaleString()} <span className="text-xs">{t.currency}</span>
          </div>
        </div>
      </div>

      {/* شريط إدارة هتش لبث الإشعارات لجميع المستخدمين */}
      <div className="bg-gradient-to-br from-neutral-900 via-neutral-900 to-amber-950/30 border border-amber-500/30 rounded-3xl p-5 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 end-0 w-60 h-60 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
        
        {/* Bar Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-neutral-800">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-amber-500/20 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0 shadow-inner">
              <Megaphone className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h3 className="font-black text-base text-white flex items-center gap-2">
                  <span>{t.broadcastTitle}</span>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 animate-pulse">
                    مباشر لجميع المستخدمين ⚡
                  </span>
                </h3>
              </div>
              <p className="text-xs text-neutral-400 mt-0.5">
                {t.broadcastSubtitle}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              type="button"
              onClick={() => setIsBroadcastExpanded(!isBroadcastExpanded)}
              className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white text-xs font-bold border border-neutral-700 flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <span>{isBroadcastExpanded ? 'تصغير الشريط' : 'فتح شريط البث'}</span>
              {isBroadcastExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {isBroadcastExpanded && (
          <form onSubmit={handleSendBroadcast} className="mt-4 space-y-4">
            {/* Quick Templates Selection */}
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                <span>{t.broadcastQuickTemplates}</span>
              </label>
              <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
                {broadcastTemplates.map((tmpl, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setBroadcastType(tmpl.type);
                      setBroadcastTitle(tmpl.title);
                      setBroadcastBody(tmpl.body);
                    }}
                    className="px-3 py-1.5 rounded-xl text-xs font-semibold bg-neutral-950/70 hover:bg-neutral-800 border border-neutral-800 hover:border-amber-500/40 text-neutral-300 hover:text-amber-300 whitespace-nowrap shrink-0 transition-colors cursor-pointer"
                  >
                    {tmpl.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Notification Type Selector */}
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-2">
                {t.broadcastTypeLabel}:
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {/* 1. Offer */}
                <button
                  type="button"
                  onClick={() => setBroadcastType('offer')}
                  className={`p-3 rounded-2xl border text-start transition-all cursor-pointer flex flex-col gap-1 ${
                    broadcastType === 'offer'
                      ? 'bg-amber-500/20 border-amber-400 text-white shadow-md shadow-amber-500/10'
                      : 'bg-neutral-950/80 border-neutral-800 text-neutral-400 hover:border-neutral-700 hover:text-neutral-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="p-1.5 rounded-xl bg-amber-500/20 text-amber-400">
                      <Tag className="w-4 h-4" />
                    </span>
                    {broadcastType === 'offer' && <CheckCircle2 className="w-4 h-4 text-amber-400" />}
                  </div>
                  <div className="text-xs font-bold mt-1 text-amber-300">
                    {t.broadcastTypeOffer}
                  </div>
                  <div className="text-[10px] text-neutral-400">
                    تخفيضات وعروض أسعار
                  </div>
                </button>

                {/* 2. Alert */}
                <button
                  type="button"
                  onClick={() => setBroadcastType('alert')}
                  className={`p-3 rounded-2xl border text-start transition-all cursor-pointer flex flex-col gap-1 ${
                    broadcastType === 'alert'
                      ? 'bg-rose-500/20 border-rose-400 text-white shadow-md shadow-rose-500/10'
                      : 'bg-neutral-950/80 border-neutral-800 text-neutral-400 hover:border-neutral-700 hover:text-neutral-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="p-1.5 rounded-xl bg-rose-500/20 text-rose-400">
                      <AlertTriangle className="w-4 h-4" />
                    </span>
                    {broadcastType === 'alert' && <CheckCircle2 className="w-4 h-4 text-rose-400" />}
                  </div>
                  <div className="text-xs font-bold mt-1 text-rose-300">
                    {t.broadcastTypeAlert}
                  </div>
                  <div className="text-[10px] text-neutral-400">
                    تحذير أمني وإرشادات
                  </div>
                </button>

                {/* 3. Update */}
                <button
                  type="button"
                  onClick={() => setBroadcastType('update')}
                  className={`p-3 rounded-2xl border text-start transition-all cursor-pointer flex flex-col gap-1 ${
                    broadcastType === 'update'
                      ? 'bg-cyan-500/20 border-cyan-400 text-white shadow-md shadow-cyan-500/10'
                      : 'bg-neutral-950/80 border-neutral-800 text-neutral-400 hover:border-neutral-700 hover:text-neutral-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="p-1.5 rounded-xl bg-cyan-500/20 text-cyan-400">
                      <Sparkles className="w-4 h-4" />
                    </span>
                    {broadcastType === 'update' && <CheckCircle2 className="w-4 h-4 text-cyan-400" />}
                  </div>
                  <div className="text-xs font-bold mt-1 text-cyan-300">
                    {t.broadcastTypeUpdate}
                  </div>
                  <div className="text-[10px] text-neutral-400">
                    مزايا جديدة وتحديثات
                  </div>
                </button>

                {/* 4. System / General */}
                <button
                  type="button"
                  onClick={() => setBroadcastType('system')}
                  className={`p-3 rounded-2xl border text-start transition-all cursor-pointer flex flex-col gap-1 ${
                    broadcastType === 'system'
                      ? 'bg-purple-500/20 border-purple-400 text-white shadow-md shadow-purple-500/10'
                      : 'bg-neutral-950/80 border-neutral-800 text-neutral-400 hover:border-neutral-700 hover:text-neutral-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="p-1.5 rounded-xl bg-purple-500/20 text-purple-400">
                      <Megaphone className="w-4 h-4" />
                    </span>
                    {broadcastType === 'system' && <CheckCircle2 className="w-4 h-4 text-purple-400" />}
                  </div>
                  <div className="text-xs font-bold mt-1 text-purple-300">
                    {t.broadcastTypeSystem}
                  </div>
                  <div className="text-[10px] text-neutral-400">
                    إعلانات رسمية عامة
                  </div>
                </button>
              </div>
            </div>

            {/* Inputs Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Title input */}
              <div className="md:col-span-2">
                <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                  عنوان الإشعار <span className="text-amber-400">*</span>
                </label>
                <input
                  type="text"
                  value={broadcastTitle}
                  onChange={(e) => setBroadcastTitle(e.target.value)}
                  placeholder="مثال: عروض كبرى اليوم في بورتسودان وعطبرة"
                  className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl px-4 py-2.5 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner"
                  required
                />
              </div>

              {/* Optional Linked Product */}
              <div>
                <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                  ربط بإعلان محدد (اختياري)
                </label>
                <select
                  value={broadcastProductId}
                  onChange={(e) => setBroadcastProductId(e.target.value)}
                  className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl px-3 py-2.5 text-xs text-white focus:outline-none transition-all cursor-pointer shadow-inner appearance-none"
                >
                  <option value="">بدون ربط بإعلان (إشعار عام)</option>
                  {products.filter(p => p.status === 'active').slice(0, 15).map(p => (
                    <option key={p.id} value={p.id}>
                      {p.titleAr} ({p.price.toLocaleString()} ج.س)
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Message Body */}
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                نص وتفاصيل الإشعار الذي سيصل للمستخدمين <span className="text-amber-400">*</span>
              </label>
              <textarea
                value={broadcastBody}
                onChange={(e) => setBroadcastBody(e.target.value)}
                placeholder="اكتب هنا الرسالة والتفاصيل التي ستظهر في صندوق إشعارات كل مستخدم..."
                rows={2}
                className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-2xl p-3 text-sm text-white placeholder-neutral-500 focus:outline-none transition-all shadow-inner resize-none leading-relaxed"
                required
              />
            </div>

            {/* Preview Box */}
            {(broadcastTitle || broadcastBody) && (
              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800/80">
                <div className="text-[11px] font-bold text-neutral-400 mb-2 flex items-center gap-1.5">
                  <Eye className="w-3.5 h-3.5 text-amber-400" />
                  <span>{t.broadcastPreviewTitle}</span>
                </div>
                <div className="p-3 rounded-xl bg-neutral-900 border border-neutral-800 flex items-start gap-3">
                  <div className="p-2 rounded-xl bg-neutral-950 border border-neutral-800 shrink-0 mt-0.5">
                    {broadcastType === 'offer' && <Tag className="w-4 h-4 text-amber-400" />}
                    {broadcastType === 'alert' && <AlertTriangle className="w-4 h-4 text-rose-400" />}
                    {broadcastType === 'update' && <Sparkles className="w-4 h-4 text-cyan-400" />}
                    {broadcastType === 'system' && <Megaphone className="w-4 h-4 text-purple-400" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h4 className="text-xs font-bold text-white truncate">
                          {broadcastTitle || 'عنوان الإشعار...'}
                        </h4>
                        <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded-md ${
                          broadcastType === 'offer' ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30' :
                          broadcastType === 'alert' ? 'bg-rose-500/15 text-rose-300 border border-rose-500/30' :
                          broadcastType === 'update' ? 'bg-cyan-500/15 text-cyan-300 border border-cyan-500/30' :
                          'bg-purple-500/15 text-purple-300 border border-purple-500/30'
                        }`}>
                          {broadcastType === 'offer' ? 'عرض جديد' :
                           broadcastType === 'alert' ? 'تنبيه أمني' :
                           broadcastType === 'update' ? 'مستجدات' : 'إشعار عام'}
                        </span>
                      </div>
                      <span className="text-[10px] text-neutral-500 shrink-0">الآن</span>
                    </div>
                    <p className="text-xs text-neutral-300 leading-relaxed">
                      {broadcastBody || 'تفاصيل الإشعار...'}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Submit Button */}
            <div className="flex items-center justify-between gap-3 pt-1">
              <span className="text-[11px] text-neutral-400 hidden sm:inline">
                💡 سيظهر الإشعار فوراً في جرس التنبيهات لجميع المستخدمين.
              </span>

              <button
                type="submit"
                disabled={!broadcastTitle.trim() || !broadcastBody.trim()}
                className="w-full sm:w-auto px-6 py-3 bg-gradient-to-r from-amber-500 via-amber-400 to-amber-500 hover:from-amber-400 hover:to-amber-300 disabled:opacity-50 disabled:pointer-events-none text-neutral-950 font-black text-sm rounded-2xl shadow-lg shadow-amber-500/20 transition-all hover:scale-105 active:scale-95 cursor-pointer flex items-center justify-center gap-2"
              >
                <Send className="w-4 h-4 stroke-[2.5]" />
                <span>{t.broadcastSendBtn}</span>
              </button>
            </div>
          </form>
        )}
      </div>

      {/* Admin Tab Switcher */}
      <div className="flex items-center gap-2 p-1.5 bg-neutral-900 border border-neutral-800 rounded-2xl overflow-x-auto no-scrollbar">
        {/* تبويب شريط إرسال وبث الإشعارات */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('broadcast')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'broadcast'
              ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-white'
          }`}
        >
          <Megaphone className="w-4 h-4" />
          <span>{language === 'ar' ? 'سجل بث الإشعارات' : 'Broadcast Logs'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-950 text-amber-300 border border-amber-800">
            {notifications.length}
          </span>
        </button>

        {/* تبويب رسائل وبلاغات المستخدمين */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('reports')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'reports'
              ? 'bg-red-500/20 text-red-400 border border-red-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-white'
          }`}
        >
          <MessageSquareText className="w-4 h-4" />
          <span>{language === 'ar' ? 'رسائل وبلاغات المستخدمين' : 'User Reports'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-red-950 text-red-300 border border-red-800">
            {reports.length}
          </span>
        </button>

        {/* زر يعرض الإعلانات التي تم بيعها */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('sold')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'sold'
              ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-emerald-400'
          }`}
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>{t.soldAdsTitle}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-emerald-950 text-emerald-300 border border-emerald-800">
            {soldProducts.length}
          </span>
        </button>

        {/* تبويب مراجعة المحادثات والتدقيق */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('conversations')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'conversations'
              ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-white'
          }`}
        >
          <MessageSquare className="w-4 h-4" />
          <span>{language === 'ar' ? 'مراجعة وتدقيق المحادثات' : 'Chat Audit'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-950 text-amber-300 border border-amber-800">
            {conversations.length}
          </span>
        </button>

        {/* تبويب إعلانات مخالفة معلقة */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('flagged')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'flagged'
              ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-white'
          }`}
        >
          <AlertTriangle className="w-4 h-4" />
          <span>{language === 'ar' ? 'إعلانات معلقة' : 'Flagged Ads'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-amber-950 text-amber-300 border border-amber-800">
            {flaggedProducts.length}
          </span>
        </button>

        {/* تبويب شبكة إعلانات AdMob والأرباح */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('monetization')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'monetization'
              ? 'bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 font-black shadow-md'
              : 'text-amber-400/90 hover:text-amber-300 hover:bg-neutral-800/80'
          }`}
        >
          <DollarSign className="w-4 h-4 stroke-[2.5]" />
          <span>{language === 'ar' ? 'إعلانات AdMob والأرباح' : 'AdMob Monetization'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-neutral-950/80 text-amber-300 border border-amber-400/30 font-mono">
            ${adStats.estimatedEarningsUsd.toFixed(2)}
          </span>
        </button>

        {/* تبويب إدارة المستخدمين وحذف وإيقاف الحسابات */}
        <button
          type="button"
          onClick={() => setActiveAdminTab('users')}
          className={`py-2 px-3.5 rounded-xl text-xs font-bold transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap shrink-0 ${
            activeAdminTab === 'users'
              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 shadow-sm'
              : 'text-neutral-400 hover:text-white'
          }`}
        >
          <Users className="w-4 h-4 text-rose-400" />
          <span>{language === 'ar' ? 'إدارة المستخدمين والحسابات' : 'Users Management'}</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-rose-950 text-rose-300 border border-rose-800">
            {availableUsers.length}
          </span>
        </button>
      </div>

      {/* Main Tab Content */}
      {activeAdminTab === 'broadcast' ? (
        /* Broadcast Log & Notification History */
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
            <h3 className="font-bold text-sm text-white flex items-center gap-2">
              <Megaphone className="w-4 h-4 text-amber-400" />
              <span>{language === 'ar' ? 'سجل بث الإشعارات لجميع المستخدمين' : 'Broadcast Notifications Log'}</span>
              <span className="text-xs text-neutral-400 font-normal">({notifications.length})</span>
            </h3>
            <span className="text-[11px] text-neutral-400">
              {language === 'ar' ? 'يتم إرسال كل إشعار إلى صندوق تنبيهات المستخدمين فوراً' : 'Delivered directly to every user'}
            </span>
          </div>

          {notifications.length > 0 ? (
            <div className="divide-y divide-neutral-800/80">
              {notifications.map((notif) => (
                <div key={notif.id} className="py-3.5 flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <div className="p-2.5 rounded-2xl bg-neutral-950 border border-neutral-800 shrink-0 mt-0.5">
                      {notif.type === 'offer' && <Tag className="w-4 h-4 text-amber-400" />}
                      {notif.type === 'alert' && <AlertTriangle className="w-4 h-4 text-rose-400" />}
                      {notif.type === 'update' && <Sparkles className="w-4 h-4 text-cyan-400" />}
                      {notif.type === 'verification' && <ShieldCheck className="w-4 h-4 text-emerald-400" />}
                      {(notif.type === 'system' || notif.type === 'message' || notif.type === 'price_drop') && <Megaphone className="w-4 h-4 text-purple-400" />}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap mb-1">
                        <h4 className="text-sm font-bold text-white">
                          {language === 'ar' ? notif.titleAr : notif.titleEn}
                        </h4>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          notif.type === 'offer' ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30' :
                          notif.type === 'alert' ? 'bg-rose-500/15 text-rose-300 border border-rose-500/30' :
                          notif.type === 'update' ? 'bg-cyan-500/15 text-cyan-300 border border-cyan-500/30' :
                          notif.type === 'verification' ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' :
                          'bg-purple-500/15 text-purple-300 border border-purple-500/30'
                        }`}>
                          {notif.type === 'offer' ? '🏷️ عرض ترويجي' :
                           notif.type === 'alert' ? '⚠️ تنبيه أمني' :
                           notif.type === 'update' ? '✨ مستجدات هتش' :
                           notif.type === 'verification' ? '🛡️ توثيق' : '📢 إشعار عام'}
                        </span>
                        <span className="text-[10px] text-neutral-500">
                          {notif.time}
                        </span>
                      </div>
                      <p className="text-xs text-neutral-300 leading-relaxed">
                        {language === 'ar' ? notif.bodyAr : notif.bodyEn}
                      </p>
                    </div>
                  </div>

                  <span className="px-2.5 py-1 rounded-xl bg-emerald-950/60 border border-emerald-800 text-emerald-400 text-[10px] font-bold shrink-0 flex items-center gap-1 self-start sm:self-auto">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>تم البث لجميع المستخدمين</span>
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-10 text-neutral-400 text-xs">
              {language === 'ar' ? 'لا توجد إشعارات مسجلة حالياً' : 'No notifications in history'}
            </div>
          )}
        </div>
      ) : activeAdminTab === 'reports' ? (
        /* User Reports Queue & Dispatched Messages */
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm text-white flex items-center gap-2">
              <MessageSquareText className="w-4 h-4 text-red-400" />
              <span>{t.reportAdminQueueTitle}</span>
              <span className="text-xs text-neutral-400 font-normal">({reports.length})</span>
            </h3>
            <span className="text-[11px] text-neutral-400">
              {language === 'ar' ? 'يمكن للإدارة مراجعة المحادثات للتأكد من صحة البلاغ' : 'Admin can review chat logs to verify reports'}
            </span>
          </div>

          {reports.length > 0 ? (
            <div className="space-y-3.5">
              {reports.map((rep) => {
                const targetProduct = rep.reportedProductId 
                  ? products.find((p) => p.id === rep.reportedProductId) 
                  : null;

                return (
                  <div 
                    key={rep.id} 
                    className="p-4 rounded-2xl bg-neutral-950/70 border border-neutral-800/80 hover:border-red-500/40 transition-all space-y-3"
                  >
                    {/* Header line: Reporter, Time, Reason */}
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-neutral-800/60 pb-3">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-red-500/20 text-red-400 border border-red-500/30 flex items-center gap-1">
                          <AlertTriangle className="w-3 h-3" />
                          <span>{rep.reason}</span>
                        </span>
                        <span className="text-xs text-neutral-400 flex items-center gap-1">
                          <UserIcon className="w-3.5 h-3.5 text-neutral-500" />
                          <span>{t.reportReporter}: <strong className="text-white">{rep.reporterName}</strong></span>
                        </span>
                        {rep.status === 'resolved' ? (
                          <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/80 border border-emerald-800 px-2 py-0.5 rounded-full flex items-center gap-1">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>{language === 'ar' ? 'تمت المراجعة والتدقيق' : 'Reviewed & Resolved'}</span>
                          </span>
                        ) : (
                          <span className="text-[10px] font-bold text-amber-400 bg-amber-950/80 border border-amber-800 px-2 py-0.5 rounded-full flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            <span>{language === 'ar' ? 'قيد المراجعة والتحقيق' : 'Pending Review'}</span>
                          </span>
                        )}
                      </div>

                      <div className="text-[11px] text-neutral-400 flex items-center gap-1 shrink-0">
                        <Clock className="w-3 h-3 text-neutral-500" />
                        <span>{rep.createdAt}</span>
                      </div>
                    </div>

                    {/* Reported Target Summary */}
                    {(rep.reportedProductTitle || rep.reportedUserName) && (
                      <div className="flex items-center gap-2 text-xs text-neutral-300 bg-neutral-900/60 p-2.5 rounded-xl border border-neutral-800">
                        <Package className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                        <span className="text-neutral-400">{t.reportTarget}:</span>
                        <span className="font-bold text-white truncate">
                          {rep.reportedProductTitle || rep.reportedUserName}
                        </span>
                        {rep.reportedUserName && rep.reportedProductTitle && (
                          <span className="text-neutral-400 text-[11px]">
                            ({language === 'ar' ? `المعلن: ${rep.reportedUserName}` : `Seller: ${rep.reportedUserName}`})
                          </span>
                        )}
                      </div>
                    )}

                    {/* THE WRITTEN REPORT MESSAGE IN THE MESSAGE BAR */}
                    <div className="space-y-1">
                      <div className="text-[11px] font-bold text-amber-400 flex items-center gap-1">
                        <MessageSquareText className="w-3.5 h-3.5" />
                        <span>{t.reportDetailMessage}:</span>
                      </div>
                      <div className="p-3 rounded-xl bg-neutral-900 border border-neutral-800 text-xs text-neutral-200 leading-relaxed font-sans shadow-inner">
                        "{rep.message}"
                      </div>
                    </div>

                    {/* Admin Actions for this Report */}
                    <div className="pt-1 flex items-center justify-end gap-2 flex-wrap">
                      {/* زر مراجعة المحادثة للتأكد من البلاغ */}
                      <button
                        type="button"
                        onClick={() => handleReviewConversationForReport(rep)}
                        className="px-3 py-1.5 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/40 text-amber-300 hover:text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer transition-all shadow-sm active:scale-95"
                        title={t.reviewConversationBtn}
                      >
                        <MessageSquareText className="w-3.5 h-3.5 text-amber-400" />
                        <span>{t.reviewConversationBtn}</span>
                      </button>

                      {targetProduct && (
                        <button
                          type="button"
                          onClick={() => setSelectedProductForDetail(targetProduct)}
                          className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-semibold flex items-center gap-1 cursor-pointer"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                          <span>{language === 'ar' ? 'معاينة الإعلان' : 'Inspect Item'}</span>
                        </button>
                      )}

                      {rep.status !== 'resolved' && (
                        <button
                          type="button"
                          onClick={() => adminResolveReport(rep.id, 'resolved')}
                          className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                        >
                          <Check className="w-3.5 h-3.5 stroke-[3]" />
                          <span>{t.reportMarkReviewed}</span>
                        </button>
                      )}

                      {rep.status === 'resolved' ? (
                        <button
                          type="button"
                          onClick={() => adminResolveReport(rep.id, 'dismissed')}
                          className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-400 text-xs font-semibold flex items-center gap-1 cursor-pointer"
                        >
                          <Clock className="w-3.5 h-3.5" />
                          <span>{language === 'ar' ? 'إعادة فتح للمتابعة' : 'Reopen'}</span>
                        </button>
                      ) : (
                        <button
                          type="button"
                          onClick={() => adminResolveReport(rep.id, 'dismissed')}
                          className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-semibold flex items-center gap-1 cursor-pointer"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          <span>{t.reportDismiss}</span>
                        </button>
                      )}

                      {rep.reportedProductId && (
                        <button
                          type="button"
                          onClick={() => adminDeleteProduct(rep.reportedProductId!)}
                          className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>{t.deleteListing}</span>
                        </button>
                      )}

                      {rep.reportedUserId && (
                        <>
                          <button
                            type="button"
                            onClick={() => {
                              const target = availableUsers.find((u) => u.id === rep.reportedUserId);
                              if (target) {
                                setSelectedUserForSuspend(target);
                                setSuspensionReason(rep.reason || rep.message || (language === 'ar' ? 'بلاغ مخالفة معايير الأمان والاحتيال في هتش' : 'Marketplace safety violation'));
                                setIsSuspendModalOpen(true);
                              }
                            }}
                            className="px-3 py-1.5 rounded-xl bg-amber-600/30 hover:bg-amber-600 text-amber-200 hover:text-white border border-amber-500/40 text-xs font-bold flex items-center gap-1 cursor-pointer transition-colors"
                            title={language === 'ar' ? 'إيقاف حساب المستخدم لعدة أيام مع إرسال إشعار رسمي بالسبب' : 'Suspend user account'}
                          >
                            <UserX className="w-3.5 h-3.5" />
                            <span>{language === 'ar' ? 'إيقاف حسابه' : 'Suspend User'}</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => {
                              const target = availableUsers.find((u) => u.id === rep.reportedUserId);
                              if (target) {
                                setSelectedUserForDelete(target);
                                setIsDeleteConfirmModalOpen(true);
                              }
                            }}
                            className="px-3 py-1.5 rounded-xl bg-red-950/80 hover:bg-red-800 text-red-300 hover:text-white border border-red-800 text-xs font-bold flex items-center gap-1 cursor-pointer transition-colors"
                            title={language === 'ar' ? 'حذف حساب المستخدم نهائياً' : 'Delete user account'}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            <span>{language === 'ar' ? 'حذف الحساب' : 'Delete Account'}</span>
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-10 text-neutral-400 text-xs">
              {language === 'ar' ? 'لا توجد بلاغات أو رسائل واردة حالياً من المستخدمين. السوق آمن ومستقر 🛡️' : 'No user report messages pending in the inbox.'}
            </div>
          )}
        </div>
      ) : activeAdminTab === 'sold' ? (
        /* عرض الإعلانات التي تم بيعها */
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm text-white flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>{t.soldAdsTitle}</span>
              <span className="text-xs text-neutral-400 font-normal">({soldProducts.length})</span>
            </h3>
            <span className="text-[11px] text-emerald-400 font-semibold bg-emerald-950/60 border border-emerald-800/60 px-2.5 py-1 rounded-full">
              {language === 'ar' ? 'صفقات مكتملة وموثقة في سجلات هتش' : 'Verified closed deals in HATCH logs'}
            </span>
          </div>

          {soldProducts.length > 0 ? (
            <div className="divide-y divide-neutral-800">
              {soldProducts.map((prod) => (
                <div key={prod.id} className="py-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="relative shrink-0">
                      <img
                        src={prod.images[0]}
                        alt={prod.titleAr}
                        className="w-16 h-16 rounded-2xl object-cover"
                      />
                      <span className="absolute -bottom-1 -end-1 bg-emerald-600 text-white p-0.5 rounded-full border-2 border-neutral-900">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </span>
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-2 mb-1 flex-wrap">
                        <h4 className="text-sm font-bold text-white truncate">
                          {language === 'ar' ? prod.titleAr : prod.titleEn}
                        </h4>
                        <span className="text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 px-2 py-0.5 rounded-full flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          <span>{t.soldBadge}</span>
                        </span>
                      </div>

                      <div className="text-xs text-neutral-400 flex items-center gap-2 mt-0.5 flex-wrap">
                        <span className="text-amber-400 font-mono font-bold">
                          {prod.price.toLocaleString()} {t.currency}
                        </span>
                        <span>•</span>
                        <span>{prod.city}</span>
                        <span>•</span>
                        <span>{language === 'ar' ? `البائع: ${prod.sellerName}` : `Seller: ${prod.sellerName}`}</span>
                        <span>•</span>
                        <span className="text-[11px] text-neutral-500">{prod.createdAt}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 w-full sm:w-auto justify-end shrink-0">
                    <button
                      onClick={() => setSelectedProductForDetail(prod)}
                      className="px-3.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
                    >
                      <ExternalLink className="w-3.5 h-3.5 text-amber-400" />
                      <span>{language === 'ar' ? 'معاينة الإعلان' : 'Inspect Deal'}</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-10 text-neutral-400 text-xs">
              {language === 'ar' ? 'لا توجد إعلانات مباعة مسجلة حالياً' : 'No sold listings recorded yet'}
            </div>
          )}
        </div>
      ) : activeAdminTab === 'conversations' ? (
        /* تبويب مراجعة وتدقيق المحادثات في المنصة */
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm text-white flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-amber-400" />
              <span>{t.adminConversationsTitle}</span>
              <span className="text-xs text-neutral-400 font-normal">({conversations.length})</span>
            </h3>
            <span className="text-[11px] text-neutral-400">
              {language === 'ar' ? 'مراقبة المحادثات للتأكد من خلوها من طلبات العربون الخارجية' : 'Audit logs for deposit fraud detection'}
            </span>
          </div>

          {conversations.length > 0 ? (
            <div className="divide-y divide-neutral-800">
              {conversations.map((conv) => {
                const hasDepositWarning = conv.messages.some(
                  (m) => m.text.includes('عربون') || m.text.includes('بنكك')
                );

                return (
                  <div key={conv.id} className="py-3.5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="relative shrink-0">
                        <img
                          src={conv.participantAvatar}
                          alt={conv.participantName}
                          className="w-11 h-11 rounded-full object-cover ring-1 ring-amber-400"
                        />
                        <img
                          src={conv.productImage}
                          alt={conv.productTitle}
                          className="absolute -bottom-1 -end-1 w-5 h-5 rounded-md object-cover border border-neutral-900"
                        />
                      </div>

                      <div className="min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h4 className="text-xs font-bold text-white truncate">
                            {conv.participantName}
                          </h4>
                          <span className="text-[10px] text-amber-400 font-semibold truncate">
                            ({conv.productTitle})
                          </span>
                          {hasDepositWarning && (
                            <span className="text-[9px] bg-red-950 text-red-300 border border-red-800 px-1.5 py-0.2 rounded-full font-bold">
                              {language === 'ar' ? 'يحتوي إشارة لعربون' : 'Deposit Mentioned'}
                            </span>
                          )}
                        </div>

                        <p className="text-xs text-neutral-400 truncate mt-0.5">
                          {conv.lastMessage}
                        </p>
                        <span className="text-[10px] text-neutral-500">
                          {conv.messages.length} {language === 'ar' ? 'رسائل مسجلة' : 'messages'} • {conv.lastMessageTime}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        type="button"
                        onClick={() => handleOpenConversationDirect(conv)}
                        className="px-3 py-1.5 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/40 text-amber-300 hover:text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer transition-all"
                      >
                        <MessageSquareText className="w-3.5 h-3.5 text-amber-400" />
                        <span>{language === 'ar' ? 'مراجعة وتدقيق المحادثة' : 'Audit Conversation'}</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-10 text-neutral-400 text-xs">
              {language === 'ar' ? 'لا توجد محادثات نشطة مسجلة حالياً' : 'No active conversations found'}
            </div>
          )}
        </div>
      ) : (
        /* Flagged Items Queue */
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl">
          <h3 className="font-bold text-sm text-white mb-4 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            {t.reportedItemsTitle} ({flaggedProducts.length})
          </h3>

          {flaggedProducts.length > 0 ? (
            <div className="divide-y divide-neutral-800">
              {flaggedProducts.map((prod) => (
                <div key={prod.id} className="py-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <img
                      src={prod.images[0]}
                      alt={prod.titleAr}
                      className="w-14 h-14 rounded-xl object-cover shrink-0"
                    />
                    <div>
                      <h4 className="text-sm font-bold text-white">
                        {language === 'ar' ? prod.titleAr : prod.titleEn}
                      </h4>
                      <div className="text-xs text-neutral-400 flex items-center gap-2 mt-0.5">
                        <span className="text-amber-400 font-mono font-bold">
                          {prod.price.toLocaleString()} {t.currency}
                        </span>
                        <span>•</span>
                        <span>{prod.city}</span>
                        <span>•</span>
                        <span>{language === 'ar' ? `بواسطة: ${prod.sellerName}` : `Seller: ${prod.sellerName}`}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                    <button
                      onClick={() => setSelectedProductForDetail(prod)}
                      className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-semibold flex items-center gap-1 cursor-pointer"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>{language === 'ar' ? 'معاينة' : 'Inspect'}</span>
                    </button>

                    <button
                      onClick={() => adminApproveProduct(prod.id)}
                      className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                    >
                      <Check className="w-3.5 h-3.5 stroke-[3]" />
                      <span>{t.approveListing}</span>
                    </button>

                    <button
                      onClick={() => adminDeleteProduct(prod.id)}
                      className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold flex items-center gap-1 cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>{t.deleteListing}</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-neutral-400 text-xs">
              {language === 'ar' ? 'لا توجد إعلانات مخالفة أو بلاغات معلقة حالياً. السوق نظيف وآمن!' : 'No pending reports in the moderation queue.'}
            </div>
          )}
        </div>
      )}

      {/* Monetization & Google AdMob Management Tab */}
      {activeAdminTab === 'monetization' && (
        <div className="space-y-6">
          {/* ========================================================================= */}
          {/* 1. DEDICATED LIVE CLICKS & REVENUE BAR / شريط خاص بحساب النقرات والأرباح */}
          {/* ========================================================================= */}
          <div className="bg-gradient-to-r from-neutral-950 via-neutral-900 to-neutral-950 border-2 border-amber-500/40 rounded-3xl p-4 sm:p-6 shadow-2xl relative overflow-hidden">
            <div className="absolute -top-12 -end-12 w-48 h-48 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute -bottom-12 -start-12 w-48 h-48 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

            {/* Top Bar Header with Live Pulse and Action Controls */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-neutral-800">
              <div className="flex items-center gap-3">
                <div className="relative flex items-center justify-center">
                  <span className="w-3.5 h-3.5 rounded-full bg-emerald-400 animate-ping absolute opacity-75" />
                  <span className="w-3 h-3 rounded-full bg-emerald-500 relative" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-base sm:text-lg font-black text-white tracking-wide flex items-center gap-2">
                      <BarChart3 className="w-5 h-5 text-amber-400" />
                      <span>{language === 'ar' ? 'شريط حساب النقرات والأرباح اللحظي' : 'Live Clicks & Revenue Ticker'}</span>
                    </h3>
                    <span className="text-[10px] font-mono bg-amber-500/15 text-amber-300 border border-amber-500/30 px-2 py-0.5 rounded-full">
                      {adMobConfig.isTestMode ? 'Sandbox Test Units' : 'Live Production'}
                    </span>
                  </div>
                  <p className="text-[11px] text-neutral-400 mt-0.5">
                    {language === 'ar'
                      ? 'احتساب تلقائي وفوري للأرباح مع كل نقرة ومشاهدة من المستخدمين في السودان'
                      : 'Real-time revenue calculation from user impressions and clicks across Sudan'}
                  </p>
                </div>
              </div>

              {/* Quick Action Toolbar on the Bar */}
              <div className="flex items-center gap-2 flex-wrap self-start sm:self-auto">
                {/* Simulate Click Button */}
                <button
                  type="button"
                  onClick={() => recordAdClick('banner')}
                  title={language === 'ar' ? 'محاكاة نقرة مستخدم لاختبار احتساب الأرباح اللحظية' : 'Simulate user click'}
                  className="px-3 py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer shadow-sm"
                >
                  <MousePointerClick className="w-3.5 h-3.5" />
                  <span>{language === 'ar' ? '+1 نقرة تجريبية' : '+1 Click'}</span>
                </button>

                {/* Simulate Impression Button */}
                <button
                  type="button"
                  onClick={() => recordAdImpression('banner')}
                  title={language === 'ar' ? 'محاكاة ظهور إعلاني لاختبار احتساب الـ eCPM' : 'Simulate impression'}
                  className="px-3 py-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 border border-neutral-700 text-xs font-bold flex items-center gap-1.5 transition-all active:scale-95 cursor-pointer"
                >
                  <Eye className="w-3.5 h-3.5 text-blue-400" />
                  <span>{language === 'ar' ? '+1 ظهور تجريبي' : '+1 View'}</span>
                </button>

                {/* Reset Stats Button */}
                <button
                  type="button"
                  onClick={() => {
                    if (window.confirm(language === 'ar' ? 'هل أنت متأكد من تصفير جميع عدادات النقرات والأرباح؟' : 'Reset all clicks & revenue metrics?')) {
                      resetAdStats();
                    }
                  }}
                  title={language === 'ar' ? 'تصفير عدادات الإحصائيات' : 'Reset Ad Statistics'}
                  className="p-2 rounded-xl bg-neutral-900 hover:bg-rose-500/20 text-neutral-400 hover:text-rose-400 border border-neutral-800 transition-colors cursor-pointer"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                </button>

                {/* Add New Ad Button */}
                <button
                  type="button"
                  onClick={() => setIsAddAdModalOpen(true)}
                  className="px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-neutral-950 font-black text-xs flex items-center gap-1.5 transition-all shadow-md active:scale-95 cursor-pointer hover:scale-105"
                >
                  <Plus className="w-4 h-4 stroke-[3]" />
                  <span>{language === 'ar' ? 'إضافة إعلان جديد (+)' : 'Add New Ad (+)'}</span>
                </button>
              </div>
            </div>

            {/* 4 Financial & Performance Counters in Ticker */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 pt-4">
              {/* 1. Total Clicks */}
              <div className="bg-neutral-950/80 border border-neutral-800/80 p-4 rounded-2xl space-y-1 shadow-md hover:border-amber-500/30 transition-colors">
                <div className="flex items-center justify-between text-neutral-400 text-xs">
                  <span>{language === 'ar' ? 'إجمالي النقرات الفعالة' : 'Total Active Clicks'}</span>
                  <MousePointerClick className="w-4 h-4 text-amber-400" />
                </div>
                <div className="text-2xl sm:text-3xl font-black text-white">
                  {adStats.clicks.toLocaleString()}
                </div>
                <div className="text-[11px] text-neutral-400 flex items-center justify-between pt-1">
                  <span>معدل CTR</span>
                  <span className="text-amber-400 font-bold font-mono">
                    {((adStats.clicks / Math.max(1, adStats.impressions)) * 100).toFixed(2)}%
                  </span>
                </div>
              </div>

              {/* 2. Total Impressions */}
              <div className="bg-neutral-950/80 border border-neutral-800/80 p-4 rounded-2xl space-y-1 shadow-md hover:border-blue-500/30 transition-colors">
                <div className="flex items-center justify-between text-neutral-400 text-xs">
                  <span>{language === 'ar' ? 'مرات الظهور (المشاهدات)' : 'Total Impressions'}</span>
                  <Eye className="w-4 h-4 text-blue-400" />
                </div>
                <div className="text-2xl sm:text-3xl font-black text-white">
                  {adStats.impressions.toLocaleString()}
                </div>
                <div className="text-[11px] text-neutral-400 flex items-center justify-between pt-1">
                  <span>eCPM</span>
                  <span className="text-blue-400 font-bold font-mono">
                    ${adMobConfig.ecpmRate.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* 3. Estimated Earnings USD */}
              <div className="bg-gradient-to-br from-neutral-900 to-amber-950/40 border border-amber-500/40 p-4 rounded-2xl space-y-1 shadow-md">
                <div className="flex items-center justify-between text-amber-300 text-xs font-bold">
                  <span>{language === 'ar' ? 'الأرباح بالدولار ($ USD)' : 'Earnings (USD)'}</span>
                  <Coins className="w-4 h-4 text-amber-400" />
                </div>
                <div className="text-2xl sm:text-3xl font-black text-amber-400">
                  ${adStats.estimatedEarningsUsd.toFixed(2)}
                </div>
                <div className="text-[11px] text-neutral-400 flex items-center justify-between pt-1">
                  <span>سعر النقرة (CPC)</span>
                  <span className="text-neutral-300 font-mono">${adMobConfig.cpcRate.toFixed(2)}</span>
                </div>
              </div>

              {/* 4. Estimated Earnings SDG */}
              <div className="bg-gradient-to-br from-neutral-900 to-emerald-950/40 border border-emerald-500/40 p-4 rounded-2xl space-y-1 shadow-md">
                <div className="flex items-center justify-between text-emerald-300 text-xs font-bold">
                  <span>{language === 'ar' ? 'الأرباح بالجنيه السوداني' : 'Earnings (SDG)'}</span>
                  <DollarSign className="w-4 h-4 text-emerald-400" />
                </div>
                <div className="text-xl sm:text-2xl font-black text-emerald-400 truncate">
                  {adStats.estimatedEarningsSdg.toLocaleString()} <span className="text-xs font-normal">ج.س</span>
                </div>
                <div className="text-[11px] text-neutral-400 flex items-center justify-between pt-1">
                  <span>سعر الصرف المعتمد</span>
                  <span className="text-neutral-300 font-mono">1$ = {adMobConfig.sdgRate} ج.س</span>
                </div>
              </div>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* 2. AD MANAGEMENT: ACTIVE & SPONSORED CAMPAIGNS (إلغاء، تعديل مدة، حذف)   */}
          {/* ========================================================================= */}
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 sm:p-6 shadow-xl space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-neutral-800">
              <div>
                <div className="flex items-center gap-2">
                  <Megaphone className="w-5 h-5 text-amber-400" />
                  <h4 className="text-base font-black text-white">
                    {language === 'ar' ? 'إدارة الإعلانات والحملات التجارية' : 'Ad Campaigns Management'}
                  </h4>
                  <span className="text-xs font-mono font-bold bg-neutral-800 text-amber-400 px-2 py-0.5 rounded-full border border-neutral-700">
                    {sponsoredAds.length} {language === 'ar' ? 'إعلانات' : 'ads'}
                  </span>
                </div>
                <p className="text-xs text-neutral-400 mt-1">
                  {language === 'ar'
                    ? 'إمكانية إضافة إعلان جديد، إلغاء أو تفعيل أي إعلان، وتحديد مدة ظهوره بالثواني عبر كل الصيغ'
                    : 'Add, pause, cancel, customize display duration in seconds, or remove ad campaigns'}
                </p>
              </div>

              <button
                type="button"
                onClick={() => setIsAddAdModalOpen(true)}
                className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-black text-xs flex items-center gap-1.5 transition-all shadow-md cursor-pointer self-start sm:self-auto hover:scale-105 active:scale-95"
              >
                <Plus className="w-4 h-4 stroke-[3]" />
                <span>{language === 'ar' ? 'إضافة إعلان جديد (+)' : 'Add Campaign (+)'}</span>
              </button>
            </div>

            {/* Campaign Cards Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {sponsoredAds.map((sAd) => {
                const isEditingDuration = editingDurationAdId === sAd.id;
                const adDuration = sAd.durationSeconds || adMobConfig.bannerRotationSeconds || 10;

                return (
                  <div
                    key={sAd.id}
                    className={`rounded-2xl p-4 border transition-all flex flex-col justify-between gap-4 ${
                      sAd.isActive !== false
                        ? 'bg-neutral-950 border-neutral-800 hover:border-amber-500/40 shadow-md'
                        : 'bg-neutral-950/50 border-rose-900/40 opacity-75'
                    }`}
                  >
                    {/* Top row: advertiser, format badge, status */}
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <img
                          src={sAd.imageUrl || sAd.logoUrl}
                          alt={sAd.advertiser}
                          className="w-12 h-12 rounded-xl object-cover ring-1 ring-neutral-700 shrink-0"
                        />
                        <div className="min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="text-xs font-black text-white truncate">
                              {sAd.advertiser}
                            </span>
                            <span className="text-[10px] font-bold bg-amber-500/15 text-amber-300 border border-amber-500/30 px-1.5 py-0.5 rounded">
                              {sAd.format === 'banner'
                                ? 'بانر'
                                : sAd.format === 'interstitial'
                                ? 'بيني'
                                : sAd.format === 'rewarded'
                                ? 'فيديو مكافأة'
                                : sAd.format === 'native'
                                ? 'مدمج Native'
                                : 'جميع الصيغ'}
                            </span>
                            {sAd.videoUrl && (
                              <button
                                type="button"
                                onClick={() => setPreviewingVideoAd(sAd)}
                                className="text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/40 px-1.5 py-0.5 rounded flex items-center gap-1 hover:bg-purple-500/30 transition-colors cursor-pointer"
                                title={language === 'ar' ? 'تشغيل ومعاينة الفيديو' : 'Preview Video'}
                              >
                                <Play className="w-2.5 h-2.5 fill-purple-300" />
                                <span>{language === 'ar' ? 'فيديو مرفق' : 'Video Attached'}</span>
                              </button>
                            )}
                          </div>
                          <p className="text-xs text-neutral-300 font-bold truncate mt-0.5">
                            {language === 'ar' ? sAd.titleAr : sAd.titleEn}
                          </p>
                          <p className="text-[11px] text-neutral-400 line-clamp-1 mt-0.5">
                            {language === 'ar' ? sAd.descriptionAr : sAd.descriptionEn}
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* Performance & Duration Row */}
                    <div className="grid grid-cols-2 gap-2 text-[11px] bg-neutral-900/80 p-2.5 rounded-xl border border-neutral-800">
                      {/* Clicks & Views */}
                      <div className="space-y-0.5">
                        <span className="text-neutral-400 text-[10px] block">
                          {language === 'ar' ? 'تفاعل الإعلان' : 'Performance'}
                        </span>
                        <div className="font-mono text-white flex items-center gap-2">
                          <span className="text-amber-400 font-bold">🖱️ {sAd.clicksCount || 0} نقرة</span>
                          <span className="text-neutral-500">•</span>
                          <span className="text-neutral-300">👁️ {sAd.impressionsCount || 0} ظهور</span>
                        </div>
                      </div>

                      {/* Display Duration Control */}
                      <div className="space-y-0.5">
                        <span className="text-neutral-400 text-[10px] block">
                          {language === 'ar' ? 'مدة ظهور الإعلان' : 'Display Duration'}
                        </span>

                        {isEditingDuration ? (
                          <div className="flex items-center gap-1.5">
                            <input
                              type="number"
                              min="3"
                              max="60"
                              value={editingDurationValue}
                              onChange={(e) => setEditingDurationValue(parseInt(e.target.value, 10) || 5)}
                              className="w-16 bg-neutral-950 border border-amber-500 text-white rounded px-1.5 py-0.5 text-xs font-mono focus:outline-none"
                            />
                            <span className="text-[10px] text-neutral-400">ثانية</span>
                            <button
                              type="button"
                              onClick={() => {
                                updateSponsoredAd(sAd.id, { durationSeconds: editingDurationValue });
                                setEditingDurationAdId(null);
                              }}
                              className="px-2 py-0.5 rounded bg-emerald-600 text-white text-[10px] font-bold cursor-pointer"
                            >
                              حفظ
                            </button>
                            <button
                              type="button"
                              onClick={() => setEditingDurationAdId(null)}
                              className="px-1.5 py-0.5 rounded bg-neutral-800 text-neutral-400 text-[10px] cursor-pointer"
                            >
                              إلغاء
                            </button>
                          </div>
                        ) : (
                          <div className="flex items-center gap-1.5">
                            <span className="font-mono text-amber-300 font-bold flex items-center gap-1">
                              <Timer className="w-3 h-3 text-amber-400" />
                              <span>{adDuration} {language === 'ar' ? 'ثوانٍ' : 'sec'}</span>
                            </span>
                            <button
                              type="button"
                              onClick={() => {
                                setEditingDurationAdId(sAd.id);
                                setEditingDurationValue(adDuration);
                              }}
                              className="text-[10px] text-neutral-400 hover:text-amber-400 underline cursor-pointer"
                            >
                              {language === 'ar' ? 'تعديل' : 'Edit'}
                            </button>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Bottom Action Controls: Toggle Status (Activate / Cancel), External Link, Delete */}
                    <div className="flex items-center justify-between gap-2 pt-2 border-t border-neutral-900">
                      {/* Toggle status: Cancel vs Activate */}
                      <button
                        type="button"
                        onClick={() => toggleSponsoredAdStatus(sAd.id)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                          sAd.isActive !== false
                            ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/25'
                            : 'bg-rose-500/15 text-rose-300 border border-rose-500/30 hover:bg-rose-500/25'
                        }`}
                      >
                        {sAd.isActive !== false ? (
                          <>
                            <span className="w-2 h-2 rounded-full bg-emerald-400" />
                            <span>{language === 'ar' ? 'نشط (يعرض حالياً)' : 'Active (Displaying)'}</span>
                            <span className="text-[10px] text-neutral-400 font-normal">| {language === 'ar' ? 'إلغاء الإعلان' : 'Pause'}</span>
                          </>
                        ) : (
                          <>
                            <span className="w-2 h-2 rounded-full bg-rose-400" />
                            <span>{language === 'ar' ? 'ملغي / معطل (متوقف)' : 'Cancelled / Paused'}</span>
                            <span className="text-[10px] text-emerald-400 font-normal">| {language === 'ar' ? 'تفعيل الإعلان' : 'Activate'}</span>
                          </>
                        )}
                      </button>

                      <div className="flex items-center gap-1.5">
                        {/* Target Link */}
                        <a
                          href={sAd.targetUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="p-2 rounded-xl bg-neutral-900 hover:bg-neutral-800 text-amber-400 border border-neutral-800 transition-colors"
                          title={sAd.targetUrl}
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </a>

                        {/* Delete Ad Button */}
                        <button
                          type="button"
                          onClick={() => {
                            if (window.confirm(language === 'ar' ? `هل أنت متأكد من حذف إعلان (${sAd.advertiser}) نهائياً؟` : 'Delete this ad campaign permanently?')) {
                              deleteSponsoredAd(sAd.id);
                            }
                          }}
                          className="p-2 rounded-xl bg-neutral-900 hover:bg-rose-500/20 text-neutral-400 hover:text-rose-400 border border-neutral-800 transition-colors cursor-pointer"
                          title={language === 'ar' ? 'حذف الإعلان نهائياً' : 'Delete Ad'}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* ========================================================================= */}
          {/* 3. AD TESTING SANDBOX & PREVIEW BUTTONS                                   */}
          {/* ========================================================================= */}
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
              <div className="flex items-center gap-2">
                <PlayCircle className="w-5 h-5 text-amber-400" />
                <h4 className="text-sm font-black text-white">
                  {language === 'ar' ? 'اختبار ومعاينة الوحدات الإعلانية في التطبيق' : 'Ad Units Testing Sandbox'}
                </h4>
              </div>
              <span className="text-xs text-neutral-400">
                {language === 'ar' ? 'فحص جاهزية الإعلانات البينية والفيديو' : 'Verify interstitial & video ads'}
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* Test Interstitial Button */}
              <button
                type="button"
                onClick={() => showInterstitialAd()}
                className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 hover:border-amber-500/50 text-start space-y-2 transition-all cursor-pointer group hover:scale-[1.02] active:scale-[0.98]"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white group-hover:text-amber-400 transition-colors">
                    {t.testInterstitialBtn}
                  </span>
                  <ExternalLink className="w-4 h-4 text-neutral-500 group-hover:text-amber-400 transition-colors" />
                </div>
                <p className="text-[11px] text-neutral-400 leading-relaxed">
                  {language === 'ar'
                    ? 'إعلان ملء الشاشة يظهر مع عداد انتظار للتخطي يحسب الأرباح والظهور.'
                    : 'Fullscreen ad displayed with countdown timer before skip.'}
                </p>
                <div className="text-[10px] font-mono text-amber-400">
                  Countdown: {adMobConfig.interstitialCountdownSeconds || 5}s
                </div>
              </button>

              {/* Test Rewarded Video Button */}
              <button
                type="button"
                onClick={() => showRewardedAd()}
                className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 hover:border-emerald-500/50 text-start space-y-2 transition-all cursor-pointer group hover:scale-[1.02] active:scale-[0.98]"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white group-hover:text-emerald-400 transition-colors">
                    {t.testRewardedBtn}
                  </span>
                  <Gift className="w-4 h-4 text-emerald-400" />
                </div>
                <p className="text-[11px] text-neutral-400 leading-relaxed">
                  {language === 'ar'
                    ? 'فيديو بمكافأة يمنح المستخدم ميزة تمييز إعلانه مجاناً بعد المشاهدة.'
                    : 'Rewarded video granting the user a free listing boost credit.'}
                </p>
                <div className="text-[10px] font-mono text-emerald-400">
                  Duration: {adMobConfig.rewardedDurationSeconds || 15}s
                </div>
              </button>

              {/* Banner Info */}
              <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 text-start space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white">
                    {language === 'ar' ? 'إعلانات البانر والـ Native' : 'Banner & Native Cards'}
                  </span>
                  <Layers className="w-4 h-4 text-amber-400" />
                </div>
                <p className="text-[11px] text-neutral-400 leading-relaxed">
                  {language === 'ar'
                    ? 'تدور تلقائياً كل عدة ثوانٍ وتولد أرباحاً لحظية مع كل مشاهدة ونقرة.'
                    : 'Auto-rotating embedded banners and high-converting native cards.'}
                </p>
                <div className="text-[10px] font-mono text-neutral-400">
                  Rotation: {adMobConfig.bannerRotationSeconds || 10}s • eCPM: ${adMobConfig.ecpmRate}
                </div>
              </div>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* 4. ADMOB CONFIGURATION & DURATION TIMERS FORM (التحكم في مدة الظهور والصيغ) */}
          {/* ========================================================================= */}
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-5 sm:p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
              <div className="flex items-center gap-2">
                <Settings2 className="w-5 h-5 text-amber-400" />
                <h4 className="text-sm font-black text-white">
                  {language === 'ar' ? 'إعدادات شبكة AdMob ومدد ظهور الإعلانات' : 'AdMob Configuration & Duration Timers'}
                </h4>
              </div>
              <span className="text-[11px] text-neutral-400 font-mono">
                SDK: Google Mobile Ads v23.0
              </span>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                updateAdMobConfig({
                  admobAppId: configAppId,
                  bannerUnitId: configBannerId,
                  interstitialUnitId: configInterstitialId,
                  rewardedUnitId: configRewardedId,
                  nativeUnitId: configNativeId,
                  ecpmRate: parseFloat(configEcpm) || 1.85,
                  cpcRate: parseFloat(configCpc) || 0.14,
                  bannerRotationSeconds: parseInt(configBannerDuration, 10) || 10,
                  interstitialCountdownSeconds: parseInt(configInterstitialDuration, 10) || 5,
                  rewardedDurationSeconds: parseInt(configRewardedDuration, 10) || 15,
                });
              }}
              className="space-y-4"
            >
              {/* Unit IDs Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* AdMob App ID */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    Google AdMob App ID
                  </label>
                  <input
                    type="text"
                    value={configAppId}
                    onChange={(e) => setConfigAppId(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    placeholder="ca-app-pub-3940256099942544~3347511713"
                  />
                </div>

                {/* Banner Unit ID */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {t.bannerUnitLabel}
                  </label>
                  <input
                    type="text"
                    value={configBannerId}
                    onChange={(e) => setConfigBannerId(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    placeholder="ca-app-pub-3940256099942544/6300978111"
                  />
                </div>

                {/* Interstitial Unit ID */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {t.interstitialUnitLabel}
                  </label>
                  <input
                    type="text"
                    value={configInterstitialId}
                    onChange={(e) => setConfigInterstitialId(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    placeholder="ca-app-pub-3940256099942544/1033173712"
                  />
                </div>

                {/* Rewarded Video Unit ID */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {t.rewardedUnitLabel}
                  </label>
                  <input
                    type="text"
                    value={configRewardedId}
                    onChange={(e) => setConfigRewardedId(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    placeholder="ca-app-pub-3940256099942544/5224354917"
                  />
                </div>

                {/* Native Unit ID */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {t.nativeUnitLabel}
                  </label>
                  <input
                    type="text"
                    value={configNativeId}
                    onChange={(e) => setConfigNativeId(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    placeholder="ca-app-pub-3940256099942544/2247696110"
                  />
                </div>

                {/* Rates Grid */}
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      eCPM ($ / 1000)
                    </label>
                    <input
                      type="number"
                      step="0.05"
                      value={configEcpm}
                      onChange={(e) => setConfigEcpm(e.target.value)}
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      CPC ($ / Click)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      value={configCpc}
                      onChange={(e) => setConfigCpc(e.target.value)}
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              {/* Format Duration Timers (التحكم في مدة ظهور الإعلانات وصيغها) */}
              <div className="p-4 rounded-2xl bg-neutral-950 border border-amber-500/20 space-y-3">
                <div className="flex items-center gap-2">
                  <Timer className="w-4 h-4 text-amber-400" />
                  <span className="text-xs font-bold text-amber-300">
                    {language === 'ar' ? 'التحكم في مدد ظهور الإعلانات وصيغها (بالثواني)' : 'Ad Display Duration Timers (Seconds)'}
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  {/* Banner Duration */}
                  <div>
                    <label className="block text-[11px] font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'مدة دوران البانر (ثوانٍ)' : 'Banner Display Time (s)'}
                    </label>
                    <input
                      type="number"
                      min="5"
                      max="60"
                      value={configBannerDuration}
                      onChange={(e) => setConfigBannerDuration(e.target.value)}
                      className="w-full bg-neutral-900 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                    <span className="text-[10px] text-neutral-500 mt-0.5 block">
                      {language === 'ar' ? 'الوقت قبل تبديل إعلان البانر' : 'Rotation interval in feed/bottom'}
                    </span>
                  </div>

                  {/* Interstitial Skip Countdown */}
                  <div>
                    <label className="block text-[11px] font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'انتظار تخطي الإعلان البيني (ثوانٍ)' : 'Interstitial Skip Timer (s)'}
                    </label>
                    <input
                      type="number"
                      min="3"
                      max="20"
                      value={configInterstitialDuration}
                      onChange={(e) => setConfigInterstitialDuration(e.target.value)}
                      className="w-full bg-neutral-900 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                    <span className="text-[10px] text-neutral-500 mt-0.5 block">
                      {language === 'ar' ? 'العداد التنازلي قبل إمكانية التخطي' : 'Seconds before user can skip'}
                    </span>
                  </div>

                  {/* Rewarded Video Duration */}
                  <div>
                    <label className="block text-[11px] font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'مدة فيديو المكافأة (ثوانٍ)' : 'Rewarded Video Watch Time (s)'}
                    </label>
                    <input
                      type="number"
                      min="10"
                      max="45"
                      value={configRewardedDuration}
                      onChange={(e) => setConfigRewardedDuration(e.target.value)}
                      className="w-full bg-neutral-900 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                    <span className="text-[10px] text-neutral-500 mt-0.5 block">
                      {language === 'ar' ? 'مدة المشاهدة المطلوبة للحصول على مكافأة' : 'Watch time to earn boost credit'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-black text-xs transition-all shadow-md cursor-pointer hover:scale-105 active:scale-95"
                >
                  {language === 'ar' ? 'حفظ إعدادات ومدد AdMob' : 'Save AdMob & Duration Settings'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: ADD NEW AD CAMPAIGN (إضافة إعلان تجاري جديد للشبكة)                 */}
      {/* ========================================================================= */}
      {isAddAdModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-neutral-950/90 backdrop-blur-md animate-in fade-in duration-200">
          <div className="relative w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
            {/* Modal Header */}
            <div className="p-4 sm:p-5 bg-neutral-950 border-b border-neutral-800 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-amber-500 text-neutral-950 flex items-center justify-center font-black">
                  <Megaphone className="w-5 h-5 fill-neutral-950" />
                </div>
                <div>
                  <h3 className="text-sm sm:text-base font-black text-white">
                    {language === 'ar' ? 'إضافة إعلان تجاري جديد لشبكة AdMob' : 'Create New Ad Campaign'}
                  </h3>
                  <p className="text-[11px] text-neutral-400">
                    {language === 'ar' ? 'حدد تفاصيل الإعلان والصيغة ومدة العرض بالثواني' : 'Configure ad creative, format, and display duration'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsAddAdModalOpen(false)}
                className="p-2 rounded-xl text-neutral-400 hover:text-white bg-neutral-900 hover:bg-neutral-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-4 sm:p-6 overflow-y-auto space-y-4">
              {/* Quick Preset Buttons */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold text-amber-400">
                  {language === 'ar' ? '⚡ قوالب جاهزة لشركات وعروض سودانية (بضغطة زر):' : '⚡ Quick Sudan Brand Presets:'}
                </label>
                <div className="flex items-center gap-2 flex-wrap">
                  {adPresets.map((preset, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handleApplyPreset(preset)}
                      className="px-3 py-1.5 rounded-xl bg-neutral-950 hover:bg-neutral-800 text-white text-xs font-bold border border-neutral-800 hover:border-amber-500/50 transition-all cursor-pointer"
                    >
                      {preset.label}
                    </button>
                  ))}
                </div>
              </div>

              <form id="create-ad-form" onSubmit={handleCreateAd} className="space-y-4 pt-2">
                {/* Advertiser Name & Format */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'اسم المعلن / الشركة *' : 'Advertiser Name *'}
                    </label>
                    <input
                      type="text"
                      required
                      value={newAdvertiser}
                      onChange={(e) => setNewAdvertiser(e.target.value)}
                      placeholder="مثال: زين السودان، بنك الخرطوم، النيل للطاقة"
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'صيغة الإعلان (Ad Format) *' : 'Ad Format *'}
                    </label>
                    <select
                      value={newFormat}
                      onChange={(e) => setNewFormat(e.target.value as AdFormat)}
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                    >
                      <option value="all">{language === 'ar' ? 'جميع الصيغ (بانر + بيني + فيديو + نايتف)' : 'All Formats'}</option>
                      <option value="banner">{language === 'ar' ? 'بانر ذكي فقط (Smart Banner)' : 'Banner Ad'}</option>
                      <option value="interstitial">{language === 'ar' ? 'إعلان بيني شاشة كاملة (Interstitial)' : 'Interstitial Ad'}</option>
                      <option value="rewarded">{language === 'ar' ? 'فيديو بمكافأة (Rewarded Video)' : 'Rewarded Video'}</option>
                      <option value="native">{language === 'ar' ? 'بطاقة مدمجة بالمنتجات (Native Feed)' : 'Native Card'}</option>
                    </select>
                  </div>
                </div>

                {/* Duration & Target URL */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1 flex items-center justify-between">
                      <span>{language === 'ar' ? 'مدة ظهور الإعلان (بالثواني) ⏱️' : 'Display Duration (Seconds)'}</span>
                      <span className="text-[10px] text-amber-400 font-normal">مخصص لهذا الإعلان</span>
                    </label>
                    <input
                      type="number"
                      min="3"
                      max="60"
                      value={newDuration}
                      onChange={(e) => setNewDuration(e.target.value)}
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'رابط الموقع عند النقر (Target URL)' : 'Target Link (URL)'}
                    </label>
                    <input
                      type="url"
                      value={newTargetUrl}
                      onChange={(e) => setNewTargetUrl(e.target.value)}
                      placeholder="https://example.com"
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                    />
                  </div>
                </div>

                {/* Title in Arabic & English */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'عنوان الإعلان بالعربية *' : 'Title in Arabic *'}
                    </label>
                    <input
                      type="text"
                      required
                      value={newTitleAr}
                      onChange={(e) => setNewTitleAr(e.target.value)}
                      placeholder="عنوان جذاب وموجز"
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-neutral-300 mb-1">
                      {language === 'ar' ? 'عنوان الإعلان بالإنجليزية (اختياري)' : 'Title in English'}
                    </label>
                    <input
                      type="text"
                      value={newTitleEn}
                      onChange={(e) => setNewTitleEn(e.target.value)}
                      placeholder="Short catchy title"
                      className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                    />
                  </div>
                </div>

                {/* Description */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {language === 'ar' ? 'نص الإعلان والوصف الترويجي' : 'Ad Description'}
                  </label>
                  <textarea
                    rows={2}
                    value={newDescAr}
                    onChange={(e) => setNewDescAr(e.target.value)}
                    placeholder="تفاصيل العرض الترويجي ومزاياه للمستخدمين..."
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl p-2.5 text-xs text-white focus:outline-none"
                  />
                </div>

                {/* CTA Button Text */}
                <div>
                  <label className="block text-xs font-bold text-neutral-300 mb-1">
                    {language === 'ar' ? 'نص زر الإعلان (CTA Text)' : 'Button CTA Text'}
                  </label>
                  <input
                    type="text"
                    value={newCtaAr}
                    onChange={(e) => setNewCtaAr(e.target.value)}
                    placeholder="اشترك الآن، تواصل، فتح حساب"
                    className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs text-white focus:outline-none"
                  />
                </div>

                {/* ========================================================= */}
                {/* MEDIA TYPE: IMAGE OR VIDEO FROM PHONE (إرفاق فيديو من الهاتف) */}
                {/* ========================================================= */}
                <div className="space-y-3 pt-3 border-t border-neutral-800">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-black text-white flex items-center gap-1.5">
                      <Film className="w-4 h-4 text-amber-400" />
                      <span>{language === 'ar' ? 'نوع الوسائط الإعلانية: صورة أو فيديو' : 'Ad Media: Image or Video'}</span>
                    </label>
                    <span className="text-[10px] text-amber-400/90 font-mono">
                      {newMediaType === 'video' ? '🎬 خيار الفيديو نشط' : '🖼️ خيار الصورة نشط'}
                    </span>
                  </div>

                  {/* Switcher: Image Banner vs Video from Phone */}
                  <div className="grid grid-cols-2 gap-2 p-1 bg-neutral-950 rounded-2xl border border-neutral-800">
                    <button
                      type="button"
                      onClick={() => setNewMediaType('image')}
                      className={`py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                        newMediaType === 'image'
                          ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                          : 'text-neutral-400 hover:text-white'
                      }`}
                    >
                      <Layers className="w-3.5 h-3.5" />
                      <span>{language === 'ar' ? 'صورة إعلانية' : 'Image Banner'}</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => {
                        setNewMediaType('video');
                        if (newFormat === 'banner') setNewFormat('all');
                      }}
                      className={`py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                        newMediaType === 'video'
                          ? 'bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 shadow-md font-black'
                          : 'text-neutral-400 hover:text-white'
                      }`}
                    >
                      <Video className="w-3.5 h-3.5" />
                      <span>{language === 'ar' ? '🎥 فيديو إعلاني من الهاتف' : '🎥 Video from Phone'}</span>
                    </button>
                  </div>

                  {/* OPTION 1: Video Attachment from Phone (إرفاق أو سحب فيديو من الهاتف) */}
                  {newMediaType === 'video' && (
                    <div className="space-y-3 p-4 rounded-2xl bg-gradient-to-b from-neutral-950 to-neutral-900 border-2 border-amber-500/40 animate-in fade-in duration-200">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-black text-amber-300 flex items-center gap-1.5">
                          <UploadCloud className="w-4 h-4 text-amber-400" />
                          <span>{language === 'ar' ? 'إرفاق أو سحب فيديو من الهاتف' : 'Attach or Drag Video from Phone'}</span>
                        </span>
                        <span className="text-[10px] text-neutral-400 font-mono">
                          MP4, WebM, MOV
                        </span>
                      </div>

                      {/* Hidden File Input for Phone Gallery / Video Files */}
                      <input
                        type="file"
                        ref={videoFileInputRef}
                        accept="video/mp4,video/webm,video/ogg,video/quicktime,video/*"
                        onChange={handleVideoInputChange}
                        className="hidden"
                      />

                      {/* Drag & Drop Upload Zone */}
                      {!newVideoUrl ? (
                        <div
                          onDragOver={(e) => {
                            e.preventDefault();
                            setIsDraggingVideo(true);
                          }}
                          onDragLeave={() => setIsDraggingVideo(false)}
                          onDrop={handleVideoDrop}
                          onClick={() => videoFileInputRef.current?.click()}
                          className={`border-2 border-dashed rounded-2xl p-6 flex flex-col items-center justify-center text-center cursor-pointer transition-all ${
                            isDraggingVideo
                              ? 'border-amber-400 bg-amber-500/10 scale-[1.01]'
                              : 'border-neutral-700 hover:border-amber-500/60 bg-neutral-950/70 hover:bg-neutral-950'
                          }`}
                        >
                          <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/30 text-amber-400 flex items-center justify-center mb-2 shadow-lg">
                            <UploadCloud className="w-6 h-6 stroke-[2.2]" />
                          </div>
                          <span className="text-xs font-black text-white block">
                            {language === 'ar' ? 'اضغط لإرفاق فيديو من ذاكرة الهاتف أو اسحب ملف الفيديو هنا' : 'Click to attach video from phone or drag & drop here'}
                          </span>
                          <span className="text-[11px] text-neutral-400 mt-1 block">
                            {language === 'ar' ? 'يدعم مقاطع الفيديو الترويجية والإعلانات المصورة حتى 25 ميجابايت' : 'Supports promotional MP4 & WebM clips up to 25MB'}
                          </span>
                          <div className="mt-3 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/30 text-xs font-bold">
                            <Video className="w-3.5 h-3.5" />
                            <span>{language === 'ar' ? 'تصفح ملفات الفيديو بالهاتف 📱' : 'Browse Phone Videos 📱'}</span>
                          </div>
                        </div>
                      ) : (
                        /* Attached Video Player Preview & Details */
                        <div className="space-y-3">
                          <div className="relative rounded-2xl overflow-hidden bg-black border border-neutral-800 shadow-xl aspect-video max-h-56 w-full flex items-center justify-center">
                            <video
                              src={newVideoUrl}
                              controls
                              playsInline
                              onLoadedMetadata={handleVideoMetadataLoaded}
                              className="w-full h-full object-contain"
                            />
                            <div className="absolute top-2 start-2 bg-neutral-950/80 backdrop-blur-md px-2.5 py-0.5 rounded-full text-[10px] font-bold text-amber-400 border border-amber-500/30 flex items-center gap-1">
                              <Film className="w-3 h-3" />
                              <span>{language === 'ar' ? 'معاينة الفيديو المرفق' : 'Attached Video Preview'}</span>
                            </div>
                          </div>

                          {/* Video Metadata bar */}
                          <div className="flex items-center justify-between bg-neutral-950 p-2.5 rounded-xl border border-neutral-800 text-xs">
                            <div className="flex items-center gap-2 min-w-0">
                              <FileVideo className="w-4 h-4 text-emerald-400 shrink-0" />
                              <div className="min-w-0 truncate">
                                <span className="text-white font-bold block truncate">
                                  {newVideoFileName || 'attached_video.mp4'}
                                </span>
                                {newVideoFileSize && (
                                  <span className="text-[10px] text-neutral-400 font-mono">
                                    {language === 'ar' ? `الحجم: ${newVideoFileSize}` : `Size: ${newVideoFileSize}`}
                                  </span>
                                )}
                              </div>
                            </div>

                            <div className="flex items-center gap-1.5 shrink-0">
                              <button
                                type="button"
                                onClick={() => videoFileInputRef.current?.click()}
                                className="px-2.5 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-[11px] font-bold cursor-pointer transition-colors"
                              >
                                {language === 'ar' ? 'تغيير' : 'Change'}
                              </button>
                              <button
                                type="button"
                                onClick={handleRemoveVideo}
                                className="px-2.5 py-1 rounded-lg bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 text-[11px] font-bold cursor-pointer transition-colors"
                              >
                                {language === 'ar' ? 'إزالة' : 'Remove'}
                              </button>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Ready Sample Promotional Videos */}
                      <div className="pt-1">
                        <span className="text-[11px] font-bold text-neutral-400 block mb-1.5">
                          {language === 'ar' ? 'أو اختر فيديو تجريبي جاهز للعرض:' : 'Or choose a ready sample promotional video:'}
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                          {sampleAdVideos.map((sVid, idx) => (
                            <button
                              key={idx}
                              type="button"
                              onClick={() => {
                                setNewVideoUrl(sVid.url);
                                setNewVideoFileName(sVid.name);
                                setNewVideoFileSize(sVid.size);
                                setNewDuration(sVid.duration.toString());
                                if (!newTitleAr) setNewTitleAr(sVid.defaultTitle);
                                if (!newAdvertiser) setNewAdvertiser(sVid.defaultAdvertiser);
                              }}
                              className="p-2 rounded-xl bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-amber-400/50 text-start transition-all cursor-pointer group"
                            >
                              <div className="flex items-center gap-1.5 text-xs font-bold text-white group-hover:text-amber-400">
                                <Play className="w-3 h-3 fill-amber-400 text-amber-400" />
                                <span className="truncate">{sVid.title}</span>
                              </div>
                              <span className="text-[10px] text-neutral-500 font-mono mt-0.5 block">
                                {sVid.duration} ثوانٍ • {sVid.size}
                              </span>
                            </button>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* OPTION 2: Image URL (if Image is selected) */}
                  {newMediaType === 'image' && (
                    <div>
                      <label className="block text-xs font-bold text-neutral-300 mb-1">
                        {language === 'ar' ? 'رابط صورة الإعلان (Image URL)' : 'Image URL'}
                      </label>
                      <input
                        type="url"
                        value={newImageUrl}
                        onChange={(e) => setNewImageUrl(e.target.value)}
                        className="w-full bg-neutral-950 border border-neutral-800 focus:border-amber-400 rounded-xl px-3 py-2 text-xs font-mono text-white focus:outline-none"
                      />
                    </div>
                  )}
                </div>
              </form>
            </div>

            {/* Modal Footer */}
            <div className="p-4 bg-neutral-950 border-t border-neutral-800 flex items-center justify-end gap-2">
              <button
                type="button"
                onClick={() => setIsAddAdModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-bold transition-colors cursor-pointer"
              >
                {language === 'ar' ? 'إلغاء' : 'Cancel'}
              </button>

              <button
                type="submit"
                form="create-ad-form"
                className="px-6 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 font-black text-xs transition-all shadow-md cursor-pointer hover:scale-105 active:scale-95 flex items-center gap-1.5"
              >
                <Plus className="w-4 h-4 stroke-[3]" />
                <span>{language === 'ar' ? 'نشر الإعلان في الشبكة' : 'Publish Ad Campaign'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================= */}
      {/* MODAL: PREVIEW ATTACHED VIDEO AD (معاينة فيديو الإعلان)    */}
      {/* ========================================================= */}
      {previewingVideoAd && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-neutral-950/90 backdrop-blur-md animate-in fade-in duration-200">
          <div className="relative w-full max-w-xl bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col">
            <div className="p-3 bg-neutral-950 border-b border-neutral-800 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs font-black text-white flex items-center gap-1.5">
                  <Film className="w-4 h-4 text-purple-400" />
                  <span>{previewingVideoAd.advertiser}</span>
                </span>
                <span className="text-[10px] bg-purple-500/20 text-purple-300 border border-purple-500/40 px-2 py-0.5 rounded-full font-bold">
                  {language === 'ar' ? 'فيديو إعلاني مرفق' : 'Attached Video'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setPreviewingVideoAd(null)}
                className="p-1.5 rounded-xl text-neutral-400 hover:text-white bg-neutral-900 hover:bg-neutral-800"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-4 bg-black aspect-video flex items-center justify-center">
              <video
                src={previewingVideoAd.videoUrl}
                controls
                autoPlay
                playsInline
                className="w-full h-full object-contain"
              />
            </div>

            <div className="p-4 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between">
              <div>
                <h4 className="text-xs font-bold text-white">
                  {language === 'ar' ? previewingVideoAd.titleAr : previewingVideoAd.titleEn}
                </h4>
                <p className="text-[11px] text-neutral-400">
                  {language === 'ar' ? `مدة العرض: ${previewingVideoAd.durationSeconds || 15} ثانية` : `Duration: ${previewingVideoAd.durationSeconds || 15}s`}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setPreviewingVideoAd(null)}
                className="px-4 py-1.5 rounded-xl bg-amber-500 text-neutral-950 text-xs font-bold cursor-pointer"
              >
                {language === 'ar' ? 'إغلاق' : 'Close'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* USERS MANAGEMENT TAB (حذف وإيقاف الحسابات لعدة أيام مع الإشعار والسبب) */}
      {/* ========================================================================= */}
      {activeAdminTab === 'users' && (
        <div className="space-y-6">
          {/* Header & Controls Bar */}
          <div className="bg-gradient-to-r from-neutral-950 via-neutral-900 to-neutral-950 border border-neutral-800 rounded-3xl p-5 shadow-xl">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-neutral-800">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center justify-center shrink-0 shadow-inner">
                  <Users className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-white flex items-center gap-2">
                    <span>{language === 'ar' ? 'إدارة المستخدمين وضبط الحسابات' : 'Users & Accounts Moderation'}</span>
                    <span className="text-xs px-2.5 py-0.5 rounded-full bg-rose-950 text-rose-300 border border-rose-800 font-bold">
                      {availableUsers.length} {language === 'ar' ? 'مستخدم' : 'users'}
                    </span>
                  </h3>
                  <p className="text-xs text-neutral-400 mt-0.5">
                    {language === 'ar' 
                      ? 'يمكنك حذف حساب أي مستخدم نهائياً أو إيقاف حسابه لعدة أيام مع إرسال إشعار رسمي بالسبب.'
                      : 'Permanently delete or suspend any user account for several days with official notification and reason.'}
                  </p>
                </div>
              </div>

              {/* Search input */}
              <div className="relative min-w-[260px]">
                <Search className="w-4 h-4 text-neutral-400 absolute top-1/2 -translate-y-1/2 start-3" />
                <input
                  type="text"
                  value={userSearchQuery}
                  onChange={(e) => setUserSearchQuery(e.target.value)}
                  placeholder={language === 'ar' ? 'بحث بالاسم، رقم الهاتف، أو المدينة...' : 'Search by name, phone, or city...'}
                  className="w-full ps-9 pe-3 py-2 rounded-xl bg-neutral-900 border border-neutral-700 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-rose-500"
                />
                {userSearchQuery && (
                  <button
                    type="button"
                    onClick={() => setUserSearchQuery('')}
                    className="absolute top-1/2 -translate-y-1/2 end-2.5 text-neutral-400 hover:text-white"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>

            {/* Status Filter Chips & KPIs */}
            <div className="mt-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
                <span className="text-xs text-neutral-400 font-semibold flex items-center gap-1 shrink-0">
                  <Filter className="w-3.5 h-3.5" />
                  <span>{language === 'ar' ? 'التصفية:' : 'Filter:'}</span>
                </span>
                {[
                  { id: 'all', labelAr: 'جميع المستخدمين', labelEn: 'All Users', count: availableUsers.length },
                  { id: 'active', labelAr: 'حسابات نشطة', labelEn: 'Active', count: availableUsers.filter((u) => u.status !== 'suspended').length },
                  { id: 'suspended', labelAr: 'حسابات موقوفة ⚠️', labelEn: 'Suspended ⚠️', count: availableUsers.filter((u) => u.status === 'suspended').length },
                  { id: 'verified', labelAr: 'بائعين موثقين 🛡️', labelEn: 'Verified 🛡️', count: availableUsers.filter((u) => u.verifiedIdentity).length },
                ].map((chip) => (
                  <button
                    key={chip.id}
                    type="button"
                    onClick={() => setUserStatusFilter(chip.id as any)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer whitespace-nowrap flex items-center gap-1.5 ${
                      userStatusFilter === chip.id
                        ? 'bg-rose-500 text-white shadow-md'
                        : 'bg-neutral-900 text-neutral-300 hover:text-white border border-neutral-800 hover:border-neutral-700'
                    }`}
                  >
                    <span>{language === 'ar' ? chip.labelAr : chip.labelEn}</span>
                    <span className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                      userStatusFilter === chip.id ? 'bg-black/30 text-white' : 'bg-neutral-800 text-neutral-400'
                    }`}>
                      {chip.count}
                    </span>
                  </button>
                ))}
              </div>

              {/* Quick Helper Badge */}
              <div className="text-[11px] text-neutral-400 flex items-center gap-1.5 self-end sm:self-auto">
                <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
                <span>{language === 'ar' ? 'قرارات الإيقاف تنفذ فوراً وتقيد الصفقات والنشر' : 'Suspensions apply immediately'}</span>
              </div>
            </div>
          </div>

          {/* Users List Grid */}
          {(() => {
            const filteredUsers = availableUsers.filter((u) => {
              const query = userSearchQuery.trim().toLowerCase();
              const matchesQuery = !query || 
                u.name.toLowerCase().includes(query) || 
                u.phone.includes(query) || 
                u.city.toLowerCase().includes(query);

              if (!matchesQuery) return false;

              if (userStatusFilter === 'active') return u.status !== 'suspended';
              if (userStatusFilter === 'suspended') return u.status === 'suspended';
              if (userStatusFilter === 'verified') return u.verifiedIdentity;
              return true;
            });

            if (filteredUsers.length === 0) {
              return (
                <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-12 text-center text-neutral-400 space-y-2">
                  <UserX className="w-10 h-10 text-neutral-600 mx-auto" />
                  <p className="text-sm font-bold text-white">
                    {language === 'ar' ? 'لم يتم العثور على أي مستخدمين يطابقون البحث' : 'No users match your criteria'}
                  </p>
                  <p className="text-xs">
                    {language === 'ar' ? 'جرب تغيير كلمة البحث أو تبديل خيارات التصفية' : 'Try adjusting search or filters'}
                  </p>
                </div>
              );
            }

            return (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {filteredUsers.map((u) => {
                  const isCurrent = currentUser.id === u.id;
                  const isSuspended = u.status === 'suspended';
                  const userActiveAds = products.filter((p) => p.sellerId === u.id && p.status !== 'sold');
                  const userSoldAds = products.filter((p) => p.sellerId === u.id && p.status === 'sold');

                  return (
                    <div
                      key={u.id}
                      className={`p-5 rounded-3xl border transition-all relative overflow-hidden flex flex-col justify-between gap-4 ${
                        isSuspended
                          ? 'bg-gradient-to-br from-red-950/40 via-neutral-900 to-neutral-950 border-red-500/50 shadow-lg'
                          : 'bg-neutral-900 border-neutral-800 hover:border-neutral-700 shadow-md'
                      }`}
                    >
                      {/* Top Row: User Avatar & Basic Info */}
                      <div>
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-center gap-3">
                            <div className="relative">
                              <img
                                src={u.avatar}
                                alt={u.name}
                                className={`w-14 h-14 rounded-2xl object-cover ring-2 ${
                                  isSuspended ? 'ring-red-500' : 'ring-neutral-700'
                                }`}
                              />
                              {isSuspended ? (
                                <div className="absolute -bottom-1 -end-1 w-5 h-5 rounded-full bg-red-600 text-white flex items-center justify-center border-2 border-neutral-900 shadow">
                                  <AlertOctagon className="w-3 h-3 stroke-[3]" />
                                </div>
                              ) : (
                                <div className="absolute -bottom-1 -end-1 w-4 h-4 rounded-full bg-emerald-500 border-2 border-neutral-900" />
                              )}
                            </div>

                            <div>
                              <div className="flex items-center gap-2 flex-wrap">
                                <h4 className="font-black text-white text-base">
                                  {u.name}
                                </h4>
                                {u.role === 'admin' && (
                                  <span className="text-[10px] bg-red-950 text-red-300 border border-red-800 px-1.5 py-0.2 rounded font-bold">
                                    Admin
                                  </span>
                                )}
                                {u.verifiedIdentity && (
                                  <span className="text-[10px] bg-emerald-950 text-emerald-300 border border-emerald-800 px-1.5 py-0.2 rounded font-bold flex items-center gap-1">
                                    <ShieldCheck className="w-3 h-3" />
                                    <span>{language === 'ar' ? 'موثق' : 'Verified'}</span>
                                  </span>
                                )}
                                {isCurrent && (
                                  <span className="text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/40 px-1.5 py-0.2 rounded font-bold">
                                    {language === 'ar' ? 'حسابك الحالي' : 'Current'}
                                  </span>
                                )}
                              </div>

                              <p className="text-xs text-neutral-400 mt-0.5 font-mono">
                                {u.phone} • {u.city}
                              </p>
                            </div>
                          </div>

                          {/* Status Badge */}
                          <div>
                            {isSuspended ? (
                              <span className="inline-flex items-center gap-1 text-[11px] font-black px-2.5 py-1 rounded-xl bg-red-600 text-white shadow-md animate-pulse">
                                <AlertTriangle className="w-3 h-3" />
                                <span>{language === 'ar' ? 'موقوف مؤقتاً' : 'Suspended'}</span>
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2.5 py-1 rounded-xl bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                                <span>{language === 'ar' ? 'نشط' : 'Active'}</span>
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Suspension Notice Details Box */}
                        {isSuspended && (
                          <div className="mt-3 p-3 rounded-2xl bg-red-950/60 border border-red-500/40 text-xs space-y-1.5">
                            <div className="flex items-center justify-between text-red-300 font-bold">
                              <span className="flex items-center gap-1.5">
                                <Clock className="w-3.5 h-3.5" />
                                <span>
                                  {language === 'ar'
                                    ? `مدة الإيقاف: ${u.suspensionDays || 7} أيام`
                                    : `Duration: ${u.suspensionDays || 7} days`}
                                </span>
                              </span>
                              {u.suspendedUntil && (
                                <span className="text-[11px] text-neutral-300">
                                  {language === 'ar' ? 'ينتهي في:' : 'Ends:'}{' '}
                                  <strong className="text-white">
                                    {new Date(u.suspendedUntil).toLocaleDateString(language === 'ar' ? 'ar-SD' : 'en-US')}
                                  </strong>
                                </span>
                              )}
                            </div>
                            <div className="text-neutral-200">
                              <span className="text-red-400 font-bold me-1">
                                {language === 'ar' ? 'سبب الإيقاف الصادر للمستخدم:' : 'Reason:'}
                              </span>
                              <span className="text-white">"{u.suspensionReason || 'مخالفة معايير الأمان'}"</span>
                            </div>
                          </div>
                        )}

                        {/* User Metrics Row */}
                        <div className="mt-3 grid grid-cols-4 gap-2 text-center text-xs bg-neutral-950/60 p-2.5 rounded-2xl border border-neutral-800">
                          <div>
                            <div className="text-[10px] text-neutral-400">{language === 'ar' ? 'الصفقات' : 'Deals'}</div>
                            <div className="font-bold text-white">{u.dealsCompleted || 0}</div>
                          </div>
                          <div>
                            <div className="text-[10px] text-neutral-400">{language === 'ar' ? 'إعلانات نشطة' : 'Active Ads'}</div>
                            <div className="font-bold text-amber-400">{userActiveAds.length}</div>
                          </div>
                          <div>
                            <div className="text-[10px] text-neutral-400">{language === 'ar' ? 'مبيعات' : 'Sold'}</div>
                            <div className="font-bold text-emerald-400">{userSoldAds.length}</div>
                          </div>
                          <div>
                            <div className="text-[10px] text-neutral-400">{language === 'ar' ? 'التقييم' : 'Rating'}</div>
                            <div className="font-bold text-amber-300">★ {u.rating?.toFixed(1) || '5.0'}</div>
                          </div>
                        </div>
                      </div>

                      {/* Bottom Action Buttons */}
                      <div className="flex items-center gap-2 pt-2 border-t border-neutral-800/80 flex-wrap">
                        {isSuspended ? (
                          <button
                            type="button"
                            onClick={() => adminUnsuspendUser(u.id)}
                            className="flex-1 py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer shadow-sm"
                          >
                            <UserCheck className="w-3.5 h-3.5" />
                            <span>{language === 'ar' ? 'رفع الإيقاف عن الحساب' : 'Lift Suspension'}</span>
                          </button>
                        ) : (
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedUserForSuspend(u);
                              setSuspensionDays(7);
                              setIsCustomDays(false);
                              setSuspensionReason('طلب تحويل عربون بنكك قبل المعاينة والاستلام يداً بيد (شبهة احتيال)');
                              setIsSuspendModalOpen(true);
                            }}
                            className="flex-1 py-2 px-3 rounded-xl bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 hover:text-white border border-amber-500/40 text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                          >
                            <UserX className="w-3.5 h-3.5 text-amber-400" />
                            <span>{language === 'ar' ? 'إيقاف الحساب لعدة أيام' : 'Suspend Account'}</span>
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => {
                            setSelectedUserForDelete(u);
                            setIsDeleteConfirmModalOpen(true);
                          }}
                          className="py-2 px-3 rounded-xl bg-red-950/80 hover:bg-red-800 text-red-300 hover:text-white border border-red-800 text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
                          title={language === 'ar' ? 'حذف حساب المستخدم نهائياً' : 'Permanently Delete User'}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          <span>{language === 'ar' ? 'حذف الحساب' : 'Delete'}</span>
                        </button>

                        {!isCurrent && (
                          <button
                            type="button"
                            onClick={() => switchUser(u.id)}
                            className="py-2 px-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white border border-neutral-700 text-xs font-semibold flex items-center gap-1 cursor-pointer transition-colors"
                            title={language === 'ar' ? 'التبديل إلى هذا الحساب للمعاينة' : 'Switch to this account'}
                          >
                            <Eye className="w-3.5 h-3.5" />
                            <span>{language === 'ar' ? 'تجربة' : 'Test'}</span>
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })()}
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 1: SUSPEND USER FOR SEVERAL DAYS WITH MANDATORY REASON & NOTIFICATION */}
      {/* ========================================================================= */}
      {isSuspendModalOpen && selectedUserForSuspend && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md overflow-y-auto">
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl w-full max-w-xl p-6 shadow-2xl relative space-y-5 animate-in fade-in zoom-in-95 duration-150 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
              <div className="flex items-center gap-2.5">
                <div className="w-10 h-10 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex items-center justify-center">
                  <UserX className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-black text-white">
                    {language === 'ar' ? 'إيقاف حساب المستخدم وإرسال إشعار رسمي' : 'Suspend User Account'}
                  </h3>
                  <p className="text-xs text-neutral-400">
                    {language === 'ar' ? 'سيتم تعطيل صلاحيات النشر والشراء وإشعار المستخدم بالسبب المحدد' : 'Restrictions apply & user notified'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsSuspendModalOpen(false);
                  setSelectedUserForSuspend(null);
                }}
                className="p-1.5 rounded-xl text-neutral-400 hover:text-white bg-neutral-800 hover:bg-neutral-700 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Target User Info Header */}
            <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 flex items-center gap-3">
              <img
                src={selectedUserForSuspend.avatar}
                alt={selectedUserForSuspend.name}
                className="w-12 h-12 rounded-xl object-cover ring-2 ring-amber-400/50"
              />
              <div className="flex-1 min-w-0">
                <h4 className="font-bold text-white text-sm truncate">
                  {selectedUserForSuspend.name}
                </h4>
                <p className="text-xs text-neutral-400 font-mono">
                  {selectedUserForSuspend.phone} • {selectedUserForSuspend.city}
                </p>
              </div>
            </div>

            {/* Duration Selector */}
            <div className="space-y-2">
              <label className="block text-xs font-bold text-neutral-300 flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5 text-amber-400" />
                <span>{language === 'ar' ? 'تحديد مدة الإيقاف (عدد الأيام):' : 'Suspension Duration (Days):'}</span>
              </label>

              <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                {[
                  { days: 1, labelAr: 'يوم (24 س)' },
                  { days: 3, labelAr: '3 أيام' },
                  { days: 7, labelAr: '7 أيام ⭐' },
                  { days: 14, labelAr: '14 يوماً' },
                  { days: 30, labelAr: '30 يوماً' },
                ].map((item) => (
                  <button
                    key={item.days}
                    type="button"
                    onClick={() => {
                      setSuspensionDays(item.days);
                      setIsCustomDays(false);
                    }}
                    className={`py-2 px-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                      !isCustomDays && suspensionDays === item.days
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md font-black'
                        : 'bg-neutral-950/80 text-neutral-300 border-neutral-800 hover:border-neutral-700'
                    }`}
                  >
                    {item.labelAr}
                  </button>
                ))}

                <button
                  type="button"
                  onClick={() => setIsCustomDays(true)}
                  className={`py-2 px-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                    isCustomDays
                      ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md font-black'
                      : 'bg-neutral-950/80 text-neutral-300 border-neutral-800 hover:border-neutral-700'
                  }`}
                >
                  {language === 'ar' ? 'مخصص...' : 'Custom...'}
                </button>
              </div>

              {isCustomDays && (
                <div className="pt-2 flex items-center gap-2">
                  <input
                    type="number"
                    min="1"
                    max="365"
                    value={customDaysInput}
                    onChange={(e) => {
                      setCustomDaysInput(e.target.value);
                      const parsed = parseInt(e.target.value, 10);
                      if (parsed > 0) setSuspensionDays(parsed);
                    }}
                    placeholder={language === 'ar' ? 'أدخل عدد الأيام (مثال: 10)' : 'Days count...'}
                    className="w-full px-3 py-2 rounded-xl bg-neutral-950 border border-amber-500/50 text-xs text-white focus:outline-none"
                  />
                  <span className="text-xs text-neutral-400 shrink-0">
                    {language === 'ar' ? 'يوماً' : 'days'}
                  </span>
                </div>
              )}
            </div>

            {/* Reason Presets & Custom Input */}
            <div className="space-y-2">
              <label className="block text-xs font-bold text-neutral-300 flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5 text-rose-400" />
                <span>{language === 'ar' ? 'سبب الإيقاف (سيظهر للمستخدم في إشعاره الرسمي):' : 'Reason (sent in user notification):'}</span>
              </label>

              <div className="space-y-1.5 max-h-36 overflow-y-auto no-scrollbar">
                {[
                  'طلب تحويل عربون بنكك قبل المعاينة والاستلام يداً بيد (شبهة احتيال)',
                  'نشر إعلانات وهمية أو صور غير مطابقة للمنتج الفعلي المعروض',
                  'سلوك غير لائق أو ألفاظ مسيئة في المحادثات والتفاوض مع المشترين',
                  'تكرار التخلف عن مواعيد المعاينة وإلغاء الصفقات المتفق عليها',
                  'انتهاك معايير الأمان وشروط الاستخدام الخاصة بمنصة هتش للتجارة الموثوقة',
                ].map((preset, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setSuspensionReason(preset)}
                    className={`w-full text-start p-2 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                      suspensionReason === preset
                        ? 'bg-amber-500/20 text-amber-200 border-amber-500/50'
                        : 'bg-neutral-950/60 text-neutral-300 border-neutral-800/80 hover:border-neutral-700'
                    }`}
                  >
                    • {preset}
                  </button>
                ))}
              </div>

              <textarea
                rows={2}
                value={suspensionReason}
                onChange={(e) => setSuspensionReason(e.target.value)}
                placeholder={language === 'ar' ? 'اكتب تفاصيل أو تعديل لسبب الإيقاف هنا...' : 'Custom suspension reason...'}
                className="w-full px-3 py-2 rounded-xl bg-neutral-950 border border-neutral-700 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500"
              />
            </div>

            {/* Official Notification Live Preview Box */}
            <div className="p-3.5 rounded-2xl bg-neutral-950 border border-amber-500/30 space-y-1.5">
              <div className="text-[11px] font-bold text-amber-400 flex items-center gap-1.5">
                <Bell className="w-3.5 h-3.5" />
                <span>{language === 'ar' ? 'معاينة الإشعار الصادر لصندوق تنبيهات المستخدم:' : 'User Notification Preview:'}</span>
              </div>
              <p className="text-xs font-bold text-white">
                ⚠️ تنبيه من إدارة هتش: تم إيقاف حسابك لمدة {suspensionDays} {suspensionDays === 1 ? 'يوم' : suspensionDays === 2 ? 'يومين' : suspensionDays <= 10 ? 'أيام' : 'يوماً'}
              </p>
              <p className="text-[11px] text-neutral-300 leading-relaxed">
                السبب: {suspensionReason.trim() || 'مخالفة معايير الأمان والشروط'}
              </p>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-2 pt-2 border-t border-neutral-800">
              <button
                type="button"
                onClick={() => {
                  setIsSuspendModalOpen(false);
                  setSelectedUserForSuspend(null);
                }}
                className="px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-bold cursor-pointer"
              >
                {language === 'ar' ? 'إلغاء' : 'Cancel'}
              </button>

              <button
                type="button"
                disabled={!suspensionReason.trim()}
                onClick={() => {
                  const finalDays = Math.max(1, suspensionDays);
                  const finalReason = suspensionReason.trim();
                  adminSuspendUser(selectedUserForSuspend.id, finalDays, finalReason);
                  setIsSuspendModalOpen(false);
                  setSelectedUserForSuspend(null);
                }}
                className="px-5 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-neutral-950 text-xs font-black shadow-lg disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer flex items-center gap-1.5"
              >
                <UserX className="w-4 h-4 stroke-[2.5]" />
                <span>{language === 'ar' ? 'تأكيد إيقاف الحساب وإرسال الإشعار' : 'Confirm Suspension & Notify'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: PERMANENTLY DELETE USER ACCOUNT CONFIRMATION */}
      {/* ========================================================================= */}
      {isDeleteConfirmModalOpen && selectedUserForDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
          <div className="bg-neutral-900 border border-red-500/40 rounded-3xl w-full max-w-md p-6 shadow-2xl relative space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-2xl bg-red-500/20 text-red-400 border border-red-500/40 flex items-center justify-center mx-auto shadow-lg">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center space-y-1.5">
              <h3 className="text-base font-black text-white">
                {language === 'ar' ? 'تأكيد حذف حساب المستخدم نهائياً؟' : 'Permanently Delete User Account?'}
              </h3>
              <p className="text-xs text-neutral-300">
                {language === 'ar' 
                  ? `أنت على وشك حذف حساب (${selectedUserForDelete.name}) نهائياً من قاعدة بيانات هتش.`
                  : `You are about to permanently delete account (${selectedUserForDelete.name}).`}
              </p>
            </div>

            <div className="p-3.5 rounded-2xl bg-red-950/40 border border-red-500/30 text-xs text-red-200 space-y-1">
              <p className="font-bold flex items-center gap-1.5 text-red-300">
                <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                <span>{language === 'ar' ? 'الآثار المترتبة على الحذف:' : 'Permanent consequences:'}</span>
              </p>
              <ul className="list-disc list-inside space-y-0.5 text-[11px] text-neutral-300">
                <li>{language === 'ar' ? 'حذف كافة إعلانات البيع الخاصة بهذا المستخدم' : 'Deletes all listings by this user'}</li>
                <li>{language === 'ar' ? 'حذف كافة سجلات المحادثات والرسائل' : 'Deletes conversation logs'}</li>
                <li>{language === 'ar' ? 'إلغاء الحساب ولا يمكن التراجع عن هذا الإجراء' : 'This action is irreversible'}</li>
              </ul>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setIsDeleteConfirmModalOpen(false);
                  setSelectedUserForDelete(null);
                }}
                className="flex-1 py-2 px-3 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-bold cursor-pointer transition-colors"
              >
                {language === 'ar' ? 'تراجع' : 'Cancel'}
              </button>

              <button
                type="button"
                onClick={() => {
                  adminDeleteUser(selectedUserForDelete.id);
                  setIsDeleteConfirmModalOpen(false);
                  setSelectedUserForDelete(null);
                }}
                className="flex-1 py-2 px-3 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-black shadow-lg shadow-red-600/30 cursor-pointer flex items-center justify-center gap-1.5 transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>{language === 'ar' ? 'نعم، حذف الحساب نهائياً' : 'Yes, Delete Permanently'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
      <AdminConversationModal
        isOpen={isChatAuditModalOpen}
        onClose={() => {
          setIsChatAuditModalOpen(false);
          setSelectedConversation(null);
          setSelectedReport(null);
        }}
        conversation={selectedConversation}
        report={selectedReport}
      />
    </div>
  );
};
