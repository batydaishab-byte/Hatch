import React, { useState, useEffect } from 'react';
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
  Image as ImageIcon,
  Save,
  Pencil
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Product, CategoryId, SudaneseCity, Condition, PaymentMethod, SUDANESE_CITIES } from '../types';
import { readyImagePresets } from '../data/mockData';

interface EditListingModalProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
}

export const EditListingModal: React.FC<EditListingModalProps> = ({
  product,
  isOpen,
  onClose
}) => {
  const { updateProduct, t, language } = useApp();

  const [titleAr, setTitleAr] = useState('');
  const [titleEn, setTitleEn] = useState('');
  const [category, setCategory] = useState<CategoryId>('phones');
  const [condition, setCondition] = useState<Condition>('like_new');
  const [city, setCity] = useState<SudaneseCity>(SUDANESE_CITIES[0]);
  const [areaAr, setAreaAr] = useState('');
  const [price, setPrice] = useState<number | ''>('');
  const [originalPrice, setOriginalPrice] = useState<number | ''>('');
  const [descriptionAr, setDescriptionAr] = useState('');
  const [negotiable, setNegotiable] = useState(true);
  const [urgentSale, setUrgentSale] = useState(false);
  const [deliveryAvailable, setDeliveryAvailable] = useState(false);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>(['bankak', 'cash']);
  const [images, setImages] = useState<string[]>([]);

  useEffect(() => {
    if (product) {
      setTitleAr(product.titleAr);
      setTitleEn(product.titleEn || product.titleAr);
      setCategory(product.category);
      setCondition(product.condition);
      setCity(product.city);
      setAreaAr(product.areaAr);
      setPrice(product.price);
      setOriginalPrice(product.originalPrice || '');
      setDescriptionAr(product.descriptionAr);
      setNegotiable(product.negotiable);
      setUrgentSale(Boolean(product.urgentSale));
      setDeliveryAvailable(Boolean(product.deliveryAvailable));
      setPaymentMethods(product.paymentMethods || ['bankak', 'cash']);
      setImages(product.images || []);
    }
  }, [product]);

  if (!isOpen || !product) return null;

  const handleAddPresetImage = (url: string) => {
    if (images.length >= 6) return;
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

    updateProduct(product.id, {
      titleAr: titleAr.trim(),
      titleEn: titleEn.trim() || titleAr.trim(),
      descriptionAr: descriptionAr.trim(),
      descriptionEn: descriptionAr.trim(),
      price: Number(price),
      originalPrice: originalPrice ? Number(originalPrice) : undefined,
      category,
      condition,
      city,
      areaAr: areaAr.trim() || product.areaAr,
      images,
      negotiable,
      urgentSale,
      deliveryAvailable,
      paymentMethods,
    });

    onClose();
  };

  const categories: { id: CategoryId; label: string }[] = [
    { id: 'phones', label: t.categories.phones },
    { id: 'electronics', label: t.categories.electronics },
    { id: 'vehicles', label: t.categories.vehicles },
    { id: 'solar_energy', label: t.categories.solar_energy },
    { id: 'furniture', label: t.categories.furniture },
    { id: 'appliances', label: t.categories.appliances },
    { id: 'fashion', label: t.categories.fashion },
    { id: 'real_estate', label: t.categories.real_estate },
    { id: 'books_hobbies', label: t.categories.books_hobbies },
  ];

  const conditions: { id: Condition; label: string }[] = [
    { id: 'new', label: t.conditions.new },
    { id: 'like_new', label: t.conditions.like_new },
    { id: 'good', label: t.conditions.good },
    { id: 'fair', label: t.conditions.fair },
  ];

  const paymentOptions: { id: PaymentMethod; label: string }[] = [
    { id: 'bankak', label: t.payments.bankak },
    { id: 'fawry', label: t.payments.fawry },
    { id: 'cash', label: t.payments.cash },
    { id: 'ocash', label: t.payments.ocash },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-0 sm:p-4 bg-neutral-950/85 backdrop-blur-md overflow-y-auto">
      <div className="relative w-full max-w-2xl bg-neutral-900 border border-neutral-800 rounded-none sm:rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[100vh] sm:max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="p-4 sm:p-5 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/90 backdrop-blur-md sticky top-0 z-10">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-amber-400">
              <Pencil className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                {t.editAdTitle}
              </h3>
              <p className="text-[11px] text-neutral-400">
                {language === 'ar' ? 'تعديل السعر أو المواصفات أو الصور' : 'Update pricing, specs or photos'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-neutral-800 hover:bg-neutral-700 flex items-center justify-center text-neutral-300 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-4 sm:p-6 overflow-y-auto space-y-5 text-neutral-200">
          {/* Images Section */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5 flex items-center justify-between">
              <span>{t.adImagesTitle} * ({images.length}/6)</span>
              <span className="text-[10px] text-neutral-400 font-normal">
                {language === 'ar' ? 'اسحب أو اضغط للإضافة' : 'Tap to add photos'}
              </span>
            </label>

            <div className="grid grid-cols-3 sm:grid-cols-4 gap-2.5 mb-3">
              {images.map((img, idx) => (
                <div 
                  key={idx} 
                  className="relative aspect-square rounded-2xl overflow-hidden border border-neutral-700 group bg-neutral-950 shadow-md"
                >
                  <img src={img} alt="preview" className="w-full h-full object-cover" />
                  <button
                    type="button"
                    onClick={() => handleRemoveImage(idx)}
                    className="absolute top-1.5 end-1.5 w-6 h-6 rounded-full bg-rose-600 text-white flex items-center justify-center opacity-90 group-hover:opacity-100 hover:scale-110 transition-all cursor-pointer shadow-lg"
                    title="Remove"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                  {idx === 0 && (
                    <span className="absolute bottom-1.5 start-1.5 text-[9px] bg-neutral-900/90 text-amber-400 font-bold px-1.5 py-0.5 rounded-md border border-amber-500/30">
                      {language === 'ar' ? 'الرئيسية' : 'Cover'}
                    </span>
                  )}
                </div>
              ))}

              {images.length < 6 && (
                <label className="aspect-square rounded-2xl border-2 border-dashed border-neutral-700 hover:border-amber-500/60 bg-neutral-800/40 hover:bg-neutral-800/80 flex flex-col items-center justify-center gap-1.5 cursor-pointer transition-all group">
                  <Upload className="w-5 h-5 text-neutral-400 group-hover:text-amber-400 transition-colors" />
                  <span className="text-[10px] font-semibold text-neutral-400 group-hover:text-neutral-200">
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

            {/* Quick preset images library */}
            <div className="bg-neutral-950/60 border border-neutral-800/80 p-2.5 rounded-2xl">
              <span className="text-[10px] text-neutral-400 font-semibold block mb-1.5 flex items-center gap-1">
                <ImageIcon className="w-3 h-3 text-amber-400" />
                <span>{language === 'ar' ? 'إضافة صور جاهزة سريعة من المعرض:' : 'Quick preset images:'}</span>
              </span>
              <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
                {readyImagePresets.map((preset, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleAddPresetImage(preset.url)}
                    className="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-[11px] whitespace-nowrap transition-colors border border-neutral-700 shrink-0 cursor-pointer"
                  >
                    <Plus className="w-3 h-3 text-amber-400" />
                    <span>{language === 'ar' ? preset.nameAr : preset.nameEn}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Title Inputs */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.adTitleArLabel} *
            </label>
            <input
              type="text"
              required
              value={titleAr}
              onChange={(e) => setTitleAr(e.target.value)}
              placeholder={t.adTitlePlaceholder}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500"
            />
          </div>

          {/* Category Selector */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.categoryLabel} *
            </label>
            <div className="grid grid-cols-3 sm:grid-cols-3 gap-2">
              {categories.map((cat) => {
                const isSelected = category === cat.id;
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setCategory(cat.id)}
                    className={`p-2 rounded-xl text-xs font-semibold border text-center transition-all cursor-pointer truncate ${
                      isSelected
                        ? 'bg-amber-500/20 text-amber-400 border-amber-500 shadow-sm'
                        : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-neutral-700'
                    }`}
                  >
                    {cat.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Condition Selector */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.conditionLabel} *
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {conditions.map((cond) => {
                const isSelected = condition === cond.id;
                return (
                  <button
                    key={cond.id}
                    type="button"
                    onClick={() => setCondition(cond.id)}
                    className={`p-2 rounded-xl text-xs font-semibold border text-center transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 font-bold'
                        : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-neutral-700'
                    }`}
                  >
                    {cond.label}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Price & Original Price */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.priceLabel} ({t.currency}) *
              </label>
              <input
                type="number"
                required
                min="1000"
                value={price}
                onChange={(e) => setPrice(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="مثال: 950000"
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-neutral-400 mb-1.5">
                {language === 'ar' ? 'السعر الأصلي (اختياري)' : 'Original Price (Optional)'} ({t.currency})
              </label>
              <input
                type="number"
                min="1000"
                value={originalPrice}
                onChange={(e) => setOriginalPrice(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder={language === 'ar' ? 'قبل الخصم' : 'Before discount'}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500 font-mono"
              />
            </div>
          </div>

          {/* Location: City and Area */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-neutral-300 mb-1.5">
                {t.cityLabel} *
              </label>
              <select
                value={city}
                onChange={(e) => setCity(e.target.value as SudaneseCity)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-amber-500"
              >
                {SUDANESE_CITIES.map((c) => (
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

          {/* Toggle Switches: Negotiable, Urgent, Delivery */}
          <div className="space-y-2 pt-1">
            <label className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/60 border border-neutral-800 hover:border-neutral-700 cursor-pointer">
              <span className="text-xs font-semibold text-neutral-300 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-emerald-400" />
                <span>{t.negotiableOption}</span>
              </span>
              <input
                type="checkbox"
                checked={negotiable}
                onChange={(e) => setNegotiable(e.target.checked)}
                className="w-4 h-4 accent-amber-500 rounded cursor-pointer"
              />
            </label>

            <label className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/60 border border-neutral-800 hover:border-neutral-700 cursor-pointer">
              <span className="text-xs font-semibold text-neutral-300 flex items-center gap-2">
                <Flame className="w-4 h-4 text-rose-500" />
                <span>{t.urgentOption}</span>
              </span>
              <input
                type="checkbox"
                checked={urgentSale}
                onChange={(e) => setUrgentSale(e.target.checked)}
                className="w-4 h-4 accent-amber-500 rounded cursor-pointer"
              />
            </label>

            <label className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/60 border border-neutral-800 hover:border-neutral-700 cursor-pointer">
              <span className="text-xs font-semibold text-neutral-300 flex items-center gap-2">
                <Truck className="w-4 h-4 text-amber-400" />
                <span>{t.deliveryOption}</span>
              </span>
              <input
                type="checkbox"
                checked={deliveryAvailable}
                onChange={(e) => setDeliveryAvailable(e.target.checked)}
                className="w-4 h-4 accent-amber-500 rounded cursor-pointer"
              />
            </label>
          </div>

          {/* Payment Methods */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-2">
              {t.acceptedPaymentsLabel}
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {paymentOptions.map((opt) => {
                const isSelected = paymentMethods.includes(opt.id);
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => togglePaymentMethod(opt.id)}
                    className={`flex items-center justify-center gap-1.5 p-2 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                      isSelected
                        ? 'bg-amber-500/20 text-amber-300 border-amber-500'
                        : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-neutral-700'
                    }`}
                  >
                    <CreditCard className="w-3.5 h-3.5" />
                    <span>{opt.label}</span>
                    {isSelected && <Check className="w-3.5 h-3.5 text-amber-400" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-bold text-neutral-300 mb-1.5">
              {t.descriptionLabel}
            </label>
            <textarea
              rows={3}
              value={descriptionAr}
              onChange={(e) => setDescriptionAr(e.target.value)}
              placeholder={t.descriptionPlaceholder}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500 resize-none"
            />
          </div>

          {/* Actions Footer */}
          <div className="pt-2 flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="w-1/3 py-3 rounded-2xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 font-bold text-xs transition-colors cursor-pointer"
            >
              {t.cancelBtn}
            </button>
            <button
              type="submit"
              className="w-2/3 py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-yellow-400 text-neutral-950 font-black text-xs shadow-lg shadow-amber-500/20 hover:scale-[1.01] active:scale-[0.99] transition-all cursor-pointer flex items-center justify-center gap-1.5"
            >
              <Save className="w-4 h-4" />
              <span>{t.saveChangesBtn}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
