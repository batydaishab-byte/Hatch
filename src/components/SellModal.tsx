import React, { useState } from 'react';
import { 
  X, 
  Upload, 
  Plus, 
  Trash2, 
  Check, 
  CreditCard, 
  Flame, 
  Truck, 
  Sparkles, 
  Image as ImageIcon 
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { CategoryId, SudaneseCity, Condition, PaymentMethod, SUDANESE_CITIES } from '../types';
import { readyImagePresets } from '../data/mockData';

export const SellModal: React.FC = () => {
  const { 
    isSellModalOpen, 
    setIsSellModalOpen, 
    addProduct, 
    currentUser, 
    t, 
    language 
  } = useApp();

  const [titleAr, setTitleAr] = useState('');
  const [titleEn, setTitleEn] = useState('');
  const [category, setCategory] = useState<CategoryId>('phones');
  const [condition, setCondition] = useState<Condition>('like_new');
  const [city, setCity] = useState<SudaneseCity>(SUDANESE_CITIES[0]);
  const [areaAr, setAreaAr] = useState('حي الشاطئ');
  const [price, setPrice] = useState<number | ''>('');
  const [originalPrice, setOriginalPrice] = useState<number | ''>('');
  const [descriptionAr, setDescriptionAr] = useState('');
  const [negotiable, setNegotiable] = useState(true);
  const [urgentSale, setUrgentSale] = useState(false);
  const [deliveryAvailable, setDeliveryAvailable] = useState(false);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>(['bankak', 'cash']);
  const [images, setImages] = useState<string[]>([
    readyImagePresets[0].url
  ]);

  if (!isSellModalOpen) return null;

  const handleAddPresetImage = (url: string) => {
    if (images.length >= 5) return;
    setImages((prev) => [...prev, url]);
  };

  const handleRemoveImage = (index: number) => {
    setImages((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setImages((prev) => [...prev, reader.result as string]);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const togglePaymentMethod = (method: PaymentMethod) => {
    setPaymentMethods((prev) =>
      prev.includes(method) ? prev.filter((m) => m !== method) : [...prev, method]
    );
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!titleAr.trim() || !price || images.length === 0) return;

    addProduct({
      titleAr: titleAr.trim(),
      titleEn: titleEn.trim() || titleAr.trim(),
      descriptionAr: descriptionAr.trim() || 'لا يوجد وصف إضافي',
      descriptionEn: descriptionAr.trim() || 'No additional description provided',
      price: Number(price),
      originalPrice: originalPrice ? Number(originalPrice) : undefined,
      category,
      condition,
      city,
      areaAr: areaAr.trim() || 'وسط المدينة',
      areaEn: areaAr.trim() || 'City Center',
      images: images.length > 0 ? images : [readyImagePresets[0].url],
      sellerId: currentUser.id,
      sellerName: currentUser.name,
      sellerAvatar: currentUser.avatar,
      sellerPhone: currentUser.phone,
      sellerRating: currentUser.rating,
      sellerDealsCount: currentUser.dealsCompleted,
      sellerVerified: currentUser.verifiedIdentity,
      paymentMethods: paymentMethods.length > 0 ? paymentMethods : ['bankak', 'cash'],
      negotiable,
      deliveryAvailable,
      urgentSale,
      featured: true,
    });
  };

  const cities = SUDANESE_CITIES;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[100vh] sm:max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="p-4 sm:p-6 bg-neutral-950/90 border-b border-neutral-800 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-black text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-amber-400" />
              {t.postAdTitle}
            </h3>
            <p className="text-xs text-neutral-400 mt-0.5">
              {t.postAdSubtitle}
            </p>
          </div>

          <button
            onClick={() => setIsSellModalOpen(false)}
            className="w-8 h-8 rounded-full bg-neutral-850 hover:bg-neutral-800 text-neutral-400 hover:text-white flex items-center justify-center cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Form Body */}
        <form onSubmit={handleSubmit} className="overflow-y-auto flex-1 p-4 sm:p-6 space-y-5 no-scrollbar">
          {/* Images Section */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold text-neutral-300">
                {t.adImagesTitle} ({images.length}/5)
              </label>
              <span className="text-[11px] text-neutral-500">
                {t.adImagesHint}
              </span>
            </div>

            {/* Active selected images */}
            <div className="grid grid-cols-3 sm:grid-cols-5 gap-2 mb-3">
              {images.map((img, idx) => (
                <div key={idx} className="relative aspect-square rounded-xl overflow-hidden border border-neutral-700 bg-neutral-950 group">
                  <img src={img} alt="item" className="w-full h-full object-cover" />
                  <button
                    type="button"
                    onClick={() => handleRemoveImage(idx)}
                    className="absolute top-1 end-1 w-6 h-6 rounded-full bg-red-600/90 text-white flex items-center justify-center text-xs opacity-90 group-hover:opacity-100 transition-opacity cursor-pointer"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                </div>
              ))}

              {/* Upload Custom Image Button */}
              {images.length < 5 && (
                <label className="aspect-square rounded-xl border-2 border-dashed border-neutral-700 hover:border-amber-500/80 bg-neutral-950/40 flex flex-col items-center justify-center text-neutral-400 hover:text-amber-400 cursor-pointer transition-colors p-2 text-center">
                  <Upload className="w-5 h-5 mb-1" />
                  <span className="text-[10px] leading-tight font-medium">
                    {language === 'ar' ? 'رفع صورة' : 'Upload'}
                  </span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>

            {/* Fast Presets */}
            <div>
              <span className="text-[11px] text-neutral-400 block mb-1.5 font-medium">
                {t.orChoosePreset}
              </span>
              <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1">
                {readyImagePresets.map((preset, idx) => (
                  <button
                    type="button"
                    key={idx}
                    onClick={() => handleAddPresetImage(preset.url)}
                    className="px-2.5 py-1 rounded-lg bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 text-[11px] text-neutral-300 whitespace-nowrap flex items-center gap-1 cursor-pointer"
                  >
                    <Plus className="w-3 h-3 text-amber-400" />
                    <span>{language === 'ar' ? preset.nameAr : preset.nameEn}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Title and Category */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.adTitleArLabel} *
              </label>
              <input
                type="text"
                value={titleAr}
                onChange={(e) => setTitleAr(e.target.value)}
                placeholder={t.adTitlePlaceholder}
                required
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.categoryLabel} *
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value as CategoryId)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                <option value="phones">{t.categories.phones}</option>
                <option value="solar_energy">{t.categories.solar_energy}</option>
                <option value="electronics">{t.categories.electronics}</option>
                <option value="vehicles">{t.categories.vehicles}</option>
                <option value="fashion">{t.categories.fashion}</option>
                <option value="furniture">{t.categories.furniture}</option>
                <option value="appliances">{t.categories.appliances}</option>
                <option value="books_hobbies">{t.categories.books_hobbies}</option>
                <option value="real_estate">{t.categories.real_estate}</option>
              </select>
            </div>
          </div>

          {/* Condition & Pricing */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.conditionLabel}
              </label>
              <select
                value={condition}
                onChange={(e) => setCondition(e.target.value as Condition)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                <option value="new">{t.conditions.new}</option>
                <option value="like_new">{t.conditions.like_new}</option>
                <option value="good">{t.conditions.good}</option>
                <option value="fair">{t.conditions.fair}</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.priceLabel} *
              </label>
              <div className="relative">
                <input
                  type="number"
                  value={price}
                  onChange={(e) => setPrice(e.target.value ? Number(e.target.value) : '')}
                  placeholder="مثال: 450000"
                  required
                  min="1"
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-xs font-bold text-amber-400 focus:outline-none focus:border-amber-500"
                />
                <span className="absolute end-3 top-1/2 -translate-y-1/2 text-xs text-neutral-400">
                  {t.currency}
                </span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-400 mb-1.5">
                {language === 'ar' ? 'السعر الأصلي (اختياري)' : 'Original Price (Optional)'}
              </label>
              <div className="relative">
                <input
                  type="number"
                  value={originalPrice}
                  onChange={(e) => setOriginalPrice(e.target.value ? Number(e.target.value) : '')}
                  placeholder="قبل الخصم"
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-xs text-neutral-300 focus:outline-none focus:border-amber-500"
                />
                <span className="absolute end-3 top-1/2 -translate-y-1/2 text-xs text-neutral-400">
                  {t.currency}
                </span>
              </div>
            </div>
          </div>

          {/* City & Area */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.cityLabel} *
              </label>
              <select
                value={city}
                onChange={(e) => setCity(e.target.value as SudaneseCity)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                {cities.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.areaLabel}
              </label>
              <input
                type="text"
                value={areaAr}
                onChange={(e) => setAreaAr(e.target.value)}
                placeholder={t.areaPlaceholder}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              />
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.descriptionLabel}
            </label>
            <textarea
              value={descriptionAr}
              onChange={(e) => setDescriptionAr(e.target.value)}
              placeholder={t.descriptionPlaceholder}
              rows={3}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl p-3 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          {/* Checkbox Options */}
          <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-2.5">
            <label className="flex items-center gap-2 text-xs text-neutral-200 cursor-pointer">
              <input
                type="checkbox"
                checked={negotiable}
                onChange={(e) => setNegotiable(e.target.checked)}
                className="rounded accent-amber-500"
              />
              <span className="font-medium">{t.negotiableOption}</span>
            </label>

            <label className="flex items-center gap-2 text-xs text-neutral-200 cursor-pointer">
              <input
                type="checkbox"
                checked={urgentSale}
                onChange={(e) => setUrgentSale(e.target.checked)}
                className="rounded accent-rose-500"
              />
              <span className="font-medium text-rose-400 flex items-center gap-1">
                <Flame className="w-3.5 h-3.5" />
                {t.urgentOption}
              </span>
            </label>

            <label className="flex items-center gap-2 text-xs text-neutral-200 cursor-pointer">
              <input
                type="checkbox"
                checked={deliveryAvailable}
                onChange={(e) => setDeliveryAvailable(e.target.checked)}
                className="rounded accent-amber-500"
              />
              <span className="font-medium flex items-center gap-1">
                <Truck className="w-3.5 h-3.5 text-amber-400" />
                {t.deliveryOption}
              </span>
            </label>
          </div>

          {/* Payment Methods */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-2">
              {t.acceptedPaymentsLabel}
            </label>
            <div className="grid grid-cols-2 gap-2 text-xs">
              {(['bankak', 'cash', 'fawry', 'ocash'] as PaymentMethod[]).map((method) => {
                const isSelected = paymentMethods.includes(method);
                return (
                  <button
                    type="button"
                    key={method}
                    onClick={() => togglePaymentMethod(method)}
                    className={`p-2.5 rounded-xl border flex items-center gap-2 transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-amber-500/10 border-amber-500 text-amber-300 font-bold'
                        : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:bg-neutral-800'
                    }`}
                  >
                    <div className={`w-4 h-4 rounded flex items-center justify-center border ${
                      isSelected ? 'bg-amber-500 border-amber-500 text-neutral-950' : 'border-neutral-700'
                    }`}>
                      {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                    </div>
                    <span>{t.payments[method]}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Submit Button */}
          <div className="pt-2">
            <button
              type="submit"
              className="w-full py-3.5 rounded-xl bg-gradient-to-r from-amber-500 via-yellow-400 to-amber-500 hover:from-amber-400 hover:to-yellow-300 text-neutral-950 font-black text-sm shadow-xl shadow-amber-500/25 flex items-center justify-center gap-2 transition-all hover:scale-[1.01] active:scale-[0.99] cursor-pointer"
            >
              <Sparkles className="w-4 h-4" />
              <span>{t.publishAdBtn}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
