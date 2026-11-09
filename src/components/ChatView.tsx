import React, { useState, useRef, useEffect, useCallback } from 'react';
import { 
  Send, 
  ArrowUp,
  MessageSquare, 
  ArrowLeft, 
  ArrowRight, 
  ShieldCheck, 
  Check, 
  CheckCheck,
  X, 
  CreditCard, 
  Zap, 
  AlertCircle, 
  Info, 
  CheckCircle2,
  Mic,
  Trash2,
  Camera,
  Maximize2,
  RotateCcw,
  RefreshCw,
  Download,
  Tag,
  Sparkles,
  Upload,
  Hand,
  ShieldAlert
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { Conversation, Product } from '../types';
import { AudioMessagePlayer } from './AudioMessagePlayer';
import { createSampleAudioDataUrl, formatAudioDuration } from '../utils/audioUtils';
import { SoldConfirmModal } from './SoldConfirmModal';

export const ChatView: React.FC = () => {
  const { 
    conversations, 
    activeConversationId, 
    setActiveConversationId, 
    sendMessage, 
    respondToOffer, 
    setSelectedProductForDetail, 
    products, 
    currentUser, 
    deleteProduct,
    markProductAsSold,
    openReportModal,
    t, 
    language 
  } = useApp();

  const [inputMessage, setInputMessage] = useState('');
  const [showCounterInput, setShowCounterInput] = useState(false);
  const [counterPrice, setCounterPrice] = useState<number | ''>('');
  const [soldConfirmProduct, setSoldConfirmProduct] = useState<Product | null>(null);
  
  // Image attachment state
  const [stagedImage, setStagedImage] = useState<string | null>(null);
  const [stagedImageTag, setStagedImageTag] = useState<string | null>(null);
  const [showCameraActionMenu, setShowCameraActionMenu] = useState(false);
  const [activeLightboxImage, setActiveLightboxImage] = useState<{ url: string; caption?: string; tag?: string } | null>(null);
  
  // Android-style pinch-to-zoom & pan state
  const [lightboxScale, setLightboxScale] = useState(1);
  const [lightboxPan, setLightboxPan] = useState({ x: 0, y: 0 });
  const [isTouchInteracting, setIsTouchInteracting] = useState(false);

  const touchStateRef = useRef<{
    initialDist: number;
    initialScale: number;
    startX: number;
    startY: number;
    initialPanX: number;
    initialPanY: number;
    isPinching: boolean;
    isPanning: boolean;
    lastTapTime: number;
  }>({
    initialDist: 0,
    initialScale: 1,
    startX: 0,
    startY: 0,
    initialPanX: 0,
    initialPanY: 0,
    isPinching: false,
    isPanning: false,
    lastTapTime: 0,
  });

  const resetLightboxZoom = useCallback(() => {
    setLightboxScale(1);
    setLightboxPan({ x: 0, y: 0 });
    touchStateRef.current.isPinching = false;
    touchStateRef.current.isPanning = false;
  }, []);

  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Live Camera state
  const [isCameraModalOpen, setIsCameraModalOpen] = useState(false);
  const [cameraFacingMode, setCameraFacingMode] = useState<'environment' | 'user'>('environment');
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [capturedFrame, setCapturedFrame] = useState<string | null>(null);
  const [isFlashActive, setIsFlashActive] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const cameraStreamRef = useRef<MediaStream | null>(null);

  // Voice recording state
  const [isRecording, setIsRecording] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const [showAudioFallbackPrompt, setShowAudioFallbackPrompt] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const audioStreamRef = useRef<MediaStream | null>(null);

  const activeConv = activeConversationId 
    ? (conversations.find((c) => c.id === activeConversationId) || null) 
    : null;

  const convProduct = activeConv 
    ? (products.find((p) => p.id === activeConv.productId) || null) 
    : null;

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConv?.messages]);

  // Clean up media recorder, camera stream, and timers on unmount
  useEffect(() => {
    return () => {
      if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
      if (audioStreamRef.current) {
        audioStreamRef.current.getTracks().forEach((track) => track.stop());
      }
      if (cameraStreamRef.current) {
        cameraStreamRef.current.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  // Keyboard shortcut (ESC) to close modals
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (activeLightboxImage) setActiveLightboxImage(null);
        if (isCameraModalOpen) stopLiveCamera();
        if (showCameraActionMenu) setShowCameraActionMenu(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeLightboxImage, isCameraModalOpen, showCameraActionMenu]);

  // Stage original image directly without any downscaling or lossy compression
  const stageOriginalImage = useCallback((dataUrl: string, defaultTag?: string) => {
    setStagedImage(dataUrl);
    if (defaultTag) setStagedImageTag(defaultTag);
    setShowCameraActionMenu(false);
  }, []);

  // Handle native file picker (Original Quality - No Compression)
  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onloadend = () => {
      if (reader.result) {
        stageOriginalImage(reader.result as string);
      }
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  // Clipboard paste support (Ctrl+V / Cmd+V) (Original Quality - No Compression)
  const handlePaste = (e: React.ClipboardEvent) => {
    const items = e.clipboardData.items;
    for (let i = 0; i < items.length; i++) {
      if (items[i].type.indexOf('image') !== -1) {
        const file = items[i].getAsFile();
        if (file) {
          const reader = new FileReader();
          reader.onloadend = () => {
            if (reader.result) {
              stageOriginalImage(reader.result as string);
            }
          };
          reader.readAsDataURL(file);
          e.preventDefault();
        }
        break;
      }
    }
  };

  // Drag and drop support (Original Quality - No Compression)
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (reader.result) {
          stageOriginalImage(reader.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  // Live Camera Controls
  const stopLiveCamera = () => {
    if (cameraStreamRef.current) {
      cameraStreamRef.current.getTracks().forEach((track) => track.stop());
      cameraStreamRef.current = null;
    }
    setIsCameraModalOpen(false);
    setCapturedFrame(null);
    setCameraError(null);
  };

  const startLiveCamera = async (facing: 'environment' | 'user' = cameraFacingMode) => {
    try {
      setCameraError(null);
      setCapturedFrame(null);
      if (cameraStreamRef.current) {
        cameraStreamRef.current.getTracks().forEach((track) => track.stop());
        cameraStreamRef.current = null;
      }

      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('Camera not supported');
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: facing,
          width: { ideal: 1920 },
          height: { ideal: 1080 },
        },
        audio: false,
      });

      cameraStreamRef.current = stream;
      setIsCameraModalOpen(true);
      setShowCameraActionMenu(false);

      setTimeout(() => {
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.play().catch(console.error);
        }
      }, 150);
    } catch (err) {
      console.warn('Camera access failed, fallback to file input', err);
      setCameraError(t.cameraAccessError);
      setIsCameraModalOpen(true);
    }
  };

  const snapLivePhoto = () => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1920;
    canvas.height = video.videoHeight || 1080;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Flash animation
    setIsFlashActive(true);
    setTimeout(() => setIsFlashActive(false), 200);

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    // Full uncompressed quality
    const dataUrl = canvas.toDataURL('image/jpeg', 1.0);
    setCapturedFrame(dataUrl);
  };

  const acceptCapturedPhoto = () => {
    if (capturedFrame) {
      stageOriginalImage(capturedFrame);
      stopLiveCamera();
    }
  };

  const flipCameraFacing = () => {
    const nextFacing = cameraFacingMode === 'environment' ? 'user' : 'environment';
    setCameraFacingMode(nextFacing);
    startLiveCamera(nextFacing);
  };

  // Android-style Touch Pinch-To-Zoom & Pan Handlers
  const handleTouchStart = (e: React.TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2) {
      // 2 fingers: pinch gesture start
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const dist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      touchStateRef.current.initialDist = dist;
      touchStateRef.current.initialScale = lightboxScale;
      touchStateRef.current.isPinching = true;
      touchStateRef.current.isPanning = false;
      setIsTouchInteracting(true);
    } else if (e.touches.length === 1) {
      // 1 finger: check double-tap (Android gallery toggle) or start pan if zoomed
      const now = Date.now();
      const diff = now - touchStateRef.current.lastTapTime;
      if (diff < 320) {
        // Double tap toggle
        if (lightboxScale > 1.1) {
          resetLightboxZoom();
        } else {
          setLightboxScale(2.5);
          setLightboxPan({ x: 0, y: 0 });
        }
        touchStateRef.current.lastTapTime = 0;
        return;
      }
      touchStateRef.current.lastTapTime = now;

      if (lightboxScale > 1) {
        touchStateRef.current.startX = e.touches[0].clientX;
        touchStateRef.current.startY = e.touches[0].clientY;
        touchStateRef.current.initialPanX = lightboxPan.x;
        touchStateRef.current.initialPanY = lightboxPan.y;
        touchStateRef.current.isPanning = true;
        setIsTouchInteracting(true);
      }
    }
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2 && touchStateRef.current.isPinching) {
      // 2 fingers: Pinching outwards to zoom in, inwards to zoom out
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const dist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      if (touchStateRef.current.initialDist > 0) {
        const factor = dist / touchStateRef.current.initialDist;
        const nextScale = Math.min(Math.max(touchStateRef.current.initialScale * factor, 1), 5);
        setLightboxScale(nextScale);
        if (nextScale <= 1.05) {
          setLightboxPan({ x: 0, y: 0 });
        }
      }
    } else if (e.touches.length === 1 && touchStateRef.current.isPanning && lightboxScale > 1) {
      // 1 finger: Pan across image while zoomed
      const deltaX = e.touches[0].clientX - touchStateRef.current.startX;
      const deltaY = e.touches[0].clientY - touchStateRef.current.startY;
      const bound = 180 * (lightboxScale - 1);
      const newX = Math.min(Math.max(touchStateRef.current.initialPanX + deltaX, -bound), bound);
      const newY = Math.min(Math.max(touchStateRef.current.initialPanY + deltaY, -bound), bound);
      setLightboxPan({ x: newX, y: newY });
    }
  };

  const handleTouchEnd = (e: React.TouchEvent<HTMLDivElement>) => {
    if (e.touches.length < 2) {
      touchStateRef.current.isPinching = false;
    }
    if (e.touches.length === 0) {
      touchStateRef.current.isPanning = false;
      setIsTouchInteracting(false);
      if (lightboxScale < 1.05) {
        resetLightboxZoom();
      }
    }
  };

  const handleWheelZoom = (e: React.WheelEvent<HTMLDivElement>) => {
    e.preventDefault();
    const delta = e.deltaY * -0.005;
    setLightboxScale((prev) => {
      const next = Math.min(Math.max(prev + delta, 1), 5);
      if (next <= 1.05) {
        setLightboxPan({ x: 0, y: 0 });
      }
      return next;
    });
  };

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if ((!inputMessage.trim() && !stagedImage) || !activeConv) return;
    sendMessage(
      activeConv.id, 
      inputMessage.trim(), 
      undefined, 
      stagedImage ? { imageUrl: stagedImage, imageTag: stagedImageTag || undefined } : undefined
    );
    setInputMessage('');
    setStagedImage(null);
    setStagedImageTag(null);
  };

  const startRecording = async () => {
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('getUserMedia not supported in this environment');
      }

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      audioStreamRef.current = stream;

      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : MediaRecorder.isTypeSupported('audio/webm')
        ? 'audio/webm'
        : '';

      const mediaRecorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);

      mediaRecorderRef.current = mediaRecorder;
      audioChunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.start(100);
      setIsRecording(true);
      setRecordingDuration(0);
      setShowAudioFallbackPrompt(false);

      recordingTimerRef.current = setInterval(() => {
        setRecordingDuration((prev) => prev + 1);
      }, 1000);
    } catch (error) {
      console.warn('Microphone permission or hardware access issue:', error);
      setShowAudioFallbackPrompt(true);
    }
  };

  const stopAndSendRecording = () => {
    if (!mediaRecorderRef.current) return;
    const finalDuration = Math.max(1, recordingDuration);

    mediaRecorderRef.current.onstop = () => {
      const audioBlob = new Blob(audioChunksRef.current, {
        type: mediaRecorderRef.current?.mimeType || 'audio/webm',
      });

      const reader = new FileReader();
      reader.readAsDataURL(audioBlob);
      reader.onloadend = () => {
        const base64Audio = reader.result as string;
        if (activeConv) {
          sendMessage(activeConv.id, '', {
            audioUrl: base64Audio,
            audioDuration: finalDuration,
          });
        }
      };

      if (audioStreamRef.current) {
        audioStreamRef.current.getTracks().forEach((track) => track.stop());
        audioStreamRef.current = null;
      }
    };

    if (mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }

    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }

    setIsRecording(false);
    setRecordingDuration(0);
  };

  const cancelRecording = () => {
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    if (audioStreamRef.current) {
      audioStreamRef.current.getTracks().forEach((track) => track.stop());
      audioStreamRef.current = null;
    }
    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
    audioChunksRef.current = [];
    setIsRecording(false);
    setRecordingDuration(0);
  };

  const sendSampleVoiceNote = () => {
    if (!activeConv) return;
    const sampleUrl = createSampleAudioDataUrl(4);
    sendMessage(activeConv.id, '', {
      audioUrl: sampleUrl,
      audioDuration: 4,
    });
    setShowAudioFallbackPrompt(false);
  };

  const handleQuickReply = (text: string) => {
    if (!activeConv) return;
    sendMessage(activeConv.id, text);
  };

  const handleViewProduct = () => {
    if (!activeConv) return;
    const prod = products.find((p) => p.id === activeConv.productId);
    if (prod) setSelectedProductForDetail(prod);
  };

  const handleAcceptOffer = () => {
    if (!activeConv?.activeOffer) return;
    respondToOffer(activeConv.id, 'accept');
  };

  const handleDeclineOffer = () => {
    if (!activeConv?.activeOffer) return;
    respondToOffer(activeConv.id, 'decline');
  };

  const handleCounterOffer = () => {
    if (!activeConv?.activeOffer || !counterPrice) return;
    respondToOffer(activeConv.id, 'counter', Number(counterPrice));
    setShowCounterInput(false);
    setCounterPrice('');
  };

  return (
    <div className="w-full max-w-6xl mx-auto px-2 sm:px-4 py-3 sm:py-6 h-[calc(100vh-8.5rem)] flex flex-col">
      <div className="bg-neutral-900 border border-neutral-800 rounded-3xl overflow-hidden flex-1 flex flex-col md:flex-row shadow-2xl">
        {/* Left Side: Conversations List */}
        <div 
          className={`w-full md:w-80 lg:w-96 border-b md:border-b-0 md:border-e border-neutral-800 flex flex-col bg-neutral-950/50 ${
            activeConversationId ? 'hidden md:flex' : 'flex'
          }`}
        >
          {/* Header */}
          <div className="p-4 border-b border-neutral-800 flex items-center justify-between">
            <h3 className="font-bold text-base text-white flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-amber-400" />
              {t.tabMessages}
            </h3>
            <span className="text-xs bg-neutral-800 text-neutral-300 px-2 py-0.5 rounded-full font-mono">
              {conversations.length}
            </span>
          </div>

          {/* List items */}
          <div className="overflow-y-auto flex-1 divide-y divide-neutral-800/60 no-scrollbar">
            {conversations.map((conv) => {
              const isSelected = activeConv?.id === conv.id;
              return (
                <div
                  key={conv.id}
                  onClick={() => setActiveConversationId(conv.id)}
                  className={`p-3.5 flex items-start gap-3 cursor-pointer transition-colors ${
                    isSelected ? 'bg-amber-500/10' : 'hover:bg-neutral-800/50'
                  }`}
                >
                  {/* Participant Avatar */}
                  <div className="relative">
                    <img
                      src={conv.participantAvatar}
                      alt={conv.participantName}
                      className="w-11 h-11 rounded-full object-cover ring-1 ring-amber-400/40"
                    />
                    {/* Small product thumbnail badge */}
                    <img
                      src={conv.productImage}
                      alt={conv.productTitle}
                      className="absolute -bottom-1 -end-1 w-5 h-5 rounded-md object-cover border border-neutral-900"
                    />
                  </div>

                  {/* Details */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-0.5">
                      <h4 className="text-xs font-bold text-white truncate max-w-[130px]">
                        {conv.participantName}
                      </h4>
                      <span className="text-[10px] text-neutral-400">
                        {conv.lastMessageTime}
                      </span>
                    </div>

                    <div className="text-[11px] text-amber-400 font-semibold truncate mb-1">
                      {conv.productTitle}
                    </div>

                    <div className="flex items-center gap-1.5 text-xs text-neutral-400">
                      {(() => {
                        const lastMsg = conv.messages[conv.messages.length - 1];
                        if (lastMsg && lastMsg.senderId === currentUser.id) {
                          return (
                            <span 
                              className="shrink-0 inline-flex items-center" 
                              title={
                                lastMsg.status === 'read'
                                  ? t.msgStatusRead
                                  : lastMsg.status === 'delivered'
                                  ? t.msgStatusDelivered
                                  : t.msgStatusSent
                              }
                            >
                              {lastMsg.status === 'read' || !lastMsg.status ? (
                                <CheckCheck className="w-3.5 h-3.5 text-sky-400 stroke-[2.2]" />
                              ) : lastMsg.status === 'delivered' ? (
                                <CheckCheck className="w-3.5 h-3.5 text-neutral-400 stroke-[2]" />
                              ) : (
                                <Check className="w-3.5 h-3.5 text-neutral-400 stroke-[2]" />
                              )}
                            </span>
                          );
                        }
                        return null;
                      })()}
                      <p className="truncate">
                        {conv.lastMessage}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Side: Active Chat Window */}
        {activeConv ? (
          <div className="flex-1 flex flex-col bg-neutral-900 overflow-hidden">
            {/* Chat Top Header */}
            <div className="p-3 sm:p-4 bg-neutral-950/80 border-b border-neutral-800 flex items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                {/* Mobile Back Button to list */}
                <button
                  onClick={() => setActiveConversationId(null)}
                  className="md:hidden p-1.5 rounded-lg bg-neutral-800 text-neutral-300 hover:text-white"
                >
                  {language === 'ar' ? <ArrowRight className="w-4 h-4" /> : <ArrowLeft className="w-4 h-4" />}
                </button>

                <img
                  src={activeConv.participantAvatar}
                  alt={activeConv.participantName}
                  className="w-9 h-9 rounded-full object-cover ring-1 ring-amber-400"
                />

                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-bold text-white truncate">
                      {activeConv.participantName}
                    </span>
                    <span title={t.verifiedSeller}>
                      <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    </span>
                  </div>
                  <div className="text-[11px] text-neutral-400 truncate">
                    {activeConv.productTitle}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-1.5 shrink-0">
                {/* View product button */}
                <button
                  onClick={handleViewProduct}
                  className="flex items-center gap-1 text-xs text-amber-400 hover:text-amber-300 bg-amber-500/10 border border-amber-500/20 px-2.5 py-1.5 rounded-xl transition-colors cursor-pointer"
                >
                  <Info className="w-3.5 h-3.5" />
                  <span className="hidden sm:inline font-semibold">
                    {language === 'ar' ? 'عرض السلعة' : 'View Item'}
                  </span>
                </button>

                {/* Report violation button to HATCH administration */}
                <button
                  type="button"
                  onClick={() => {
                    openReportModal({
                      productId: activeConv.productId,
                      productTitle: activeConv.productTitle,
                      userId: activeConv.participantId,
                      userName: activeConv.participantName,
                    });
                  }}
                  className="flex items-center gap-1 text-xs text-red-400 hover:text-red-300 bg-red-500/10 border border-red-500/20 px-2.5 py-1.5 rounded-xl transition-colors cursor-pointer"
                  title={t.reportListing}
                >
                  <ShieldAlert className="w-3.5 h-3.5" />
                  <span className="hidden md:inline font-semibold text-[11px]">
                    {language === 'ar' ? 'إبلاغ للإدارة' : 'Report'}
                  </span>
                </button>
              </div>
            </div>

            {/* Active Negotiation Banner if an offer exists */}
            {activeConv.activeOffer && (
              <div className="p-3 bg-gradient-to-r from-amber-950/40 via-neutral-950 to-amber-950/40 border-b border-amber-500/20">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-amber-500/20 text-amber-400 flex items-center justify-center shrink-0">
                      <Zap className="w-4 h-4 fill-amber-400" />
                    </div>
                    <div>
                      <span className="text-neutral-400">
                        {language === 'ar' ? 'عرض تفاوض سارٍ:' : 'Active Negotiation Offer:'}
                      </span>{' '}
                      <span className="font-extrabold text-amber-400 font-mono text-sm">
                        {activeConv.activeOffer.offeredPrice.toLocaleString()} {t.currency}
                      </span>
                      <span className="text-[10px] text-neutral-500 ms-1.5 line-through">
                        ({activeConv.productPrice.toLocaleString()} {t.currency})
                      </span>
                    </div>
                  </div>

                  {/* Status & Response Actions */}
                  <div className="flex items-center gap-1.5">
                    {activeConv.activeOffer.status === 'accepted' ? (
                      <div className="flex items-center gap-2">
                        <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-400 bg-emerald-950/80 border border-emerald-700 px-2 py-0.5 rounded-full">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          {language === 'ar' ? 'تم قبول العرض' : 'Offer Accepted'}
                        </span>
                        {convProduct && (
                          <button
                            type="button"
                            onClick={() => setSoldConfirmProduct(convProduct)}
                            className="text-[11px] font-bold text-white bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 hover:to-emerald-400 px-2.5 py-1 rounded-lg shadow-sm border border-emerald-400/30 transition-all flex items-center gap-1 cursor-pointer active:scale-95"
                          >
                            <CheckCircle2 className="w-3 h-3" />
                            <span>{t.markAsSold}</span>
                          </button>
                        )}
                      </div>
                    ) : activeConv.activeOffer.status === 'declined' ? (
                      <span className="text-[11px] font-bold text-rose-400 bg-rose-950/80 border border-rose-800 px-2 py-0.5 rounded-full">
                        {language === 'ar' ? 'تم الرفض' : 'Offer Declined'}
                      </span>
                    ) : (
                      <div className="flex items-center gap-1.5">
                        <button
                          onClick={handleAcceptOffer}
                          className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-lg transition-colors cursor-pointer text-[11px] flex items-center gap-1"
                        >
                          <Check className="w-3 h-3 stroke-[3]" />
                          {t.acceptOffer}
                        </button>
                        <button
                          onClick={handleDeclineOffer}
                          className="px-2 py-1 bg-neutral-800 hover:bg-rose-950 hover:text-rose-400 text-neutral-300 font-semibold rounded-lg border border-neutral-700 transition-colors cursor-pointer text-[11px]"
                        >
                          {t.declineOffer}
                        </button>
                        <button
                          onClick={() => setShowCounterInput(!showCounterInput)}
                          className="px-2 py-1 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-semibold rounded-lg border border-amber-500/30 transition-colors cursor-pointer text-[11px]"
                        >
                          {t.counterOffer}
                        </button>
                      </div>
                    )}
                  </div>
                </div>

                {/* Counter offer input drop */}
                {showCounterInput && (
                  <div className="mt-2.5 pt-2 border-t border-neutral-800 flex items-center gap-2">
                    <input
                      type="number"
                      placeholder="أدخل السعر المضاد (ج.س)"
                      value={counterPrice}
                      onChange={(e) => setCounterPrice(e.target.value ? Number(e.target.value) : '')}
                      className="bg-neutral-900 border border-neutral-700 rounded-lg px-2.5 py-1 text-xs text-white focus:outline-none focus:border-amber-500 w-48"
                    />
                    <button
                      onClick={handleCounterOffer}
                      className="px-3 py-1 bg-amber-500 text-neutral-950 font-bold rounded-lg text-xs cursor-pointer"
                    >
                      {language === 'ar' ? 'إرسال المضاد' : 'Send Counter'}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Safety Mini Tip */}
            <div className="bg-neutral-950/90 px-3 py-1.5 border-b border-neutral-800/80 flex items-center gap-1.5 text-[11px] text-amber-400/90">
              <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
              <span>
                {language === 'ar'
                  ? 'أمان هتش: راجع إشعار تطبيق بنكك بنفسك قبل تسليم المنتج.'
                  : 'HATCH Safe: Always verify Bankak deposit inside your official app.'}
              </span>
            </div>

            {/* Messages Scroll Area */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 no-scrollbar">
              {activeConv.messages.map((msg) => {
                const isMe = msg.senderId === currentUser.id;
                return (
                  <div
                    key={msg.id}
                    className={`flex flex-col ${isMe ? 'items-end' : 'items-start'}`}
                  >
                    <div
                      className={`max-w-[85%] sm:max-w-md p-3 rounded-2xl text-xs sm:text-sm leading-relaxed shadow-sm ${
                        isMe
                          ? 'bg-amber-500 text-neutral-950 font-medium rounded-br-none'
                          : 'bg-neutral-800 text-white font-normal rounded-bl-none border border-neutral-700/60'
                      }`}
                    >
                      {msg.imageUrl && (
                        <div className="mb-2">
                          {msg.imageTag && (
                            <div className="mb-1.5 flex items-center">
                              <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold flex items-center gap-1 shadow-xs ${
                                isMe 
                                  ? 'bg-neutral-950/25 text-neutral-950 border border-neutral-950/20' 
                                  : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                              }`}>
                                <Tag className="w-2.5 h-2.5" />
                                {msg.imageTag}
                              </span>
                            </div>
                          )}
                          <div 
                            onClick={() => {
                              resetLightboxZoom();
                              setActiveLightboxImage({ 
                                url: msg.imageUrl!, 
                                caption: msg.text,
                                tag: msg.imageTag
                              });
                            }}
                            className="relative rounded-xl overflow-hidden cursor-pointer group bg-black/40 border border-black/10 hover:border-amber-400/60 transition-all max-w-[260px] sm:max-w-[300px]"
                            title={t.viewFullPhoto}
                          >
                            <img
                              src={msg.imageUrl}
                              alt="Product inspection"
                              className="w-full h-auto max-h-60 object-cover rounded-xl transition-transform duration-300 group-hover:scale-[1.03]"
                              loading="lazy"
                            />
                            <div className="absolute inset-0 bg-neutral-950/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 text-white text-xs font-semibold backdrop-blur-[1px]">
                              <Maximize2 className="w-4 h-4 text-amber-400" />
                              <span>{t.viewFullPhoto}</span>
                            </div>
                          </div>
                        </div>
                      )}
                      {msg.audioUrl ? (
                        <div className="flex flex-col gap-1.5">
                          <AudioMessagePlayer
                            audioUrl={msg.audioUrl}
                            duration={msg.audioDuration}
                            isMe={isMe}
                            label={t.voiceNote}
                          />
                          {msg.text && 
                           msg.text !== '🎤 تسجيل صوتي' && 
                           msg.text !== '🎤 Voice note' && (
                            <p className="text-xs pt-1 border-t border-current/15 opacity-90">
                              {msg.text}
                            </p>
                          )}
                        </div>
                      ) : (
                        (!msg.imageUrl || (
                          msg.text && 
                          msg.text !== '📷 صورة للمعاينة' && 
                          msg.text !== '📷 Photo for inspection' &&
                          msg.text !== '📷 صورة للمعاينة والتفاوض' &&
                          msg.text !== '📷 Inspection photo'
                        )) && (
                          <div className={msg.imageUrl ? 'pt-1 font-medium' : ''}>
                            {msg.text}
                          </div>
                        )
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 mt-1 px-1">
                      <span className="text-[10px] text-neutral-500">
                        {msg.timestamp}
                      </span>
                      {isMe && (
                        <span
                          className="inline-flex items-center transition-all duration-300"
                          title={
                            msg.status === 'read'
                              ? t.msgStatusRead
                              : msg.status === 'delivered'
                              ? t.msgStatusDelivered
                              : t.msgStatusSent
                          }
                          aria-label={
                            msg.status === 'read'
                              ? t.msgStatusRead
                              : msg.status === 'delivered'
                              ? t.msgStatusDelivered
                              : t.msgStatusSent
                          }
                        >
                          {msg.status === 'read' || !msg.status ? (
                            <CheckCheck className="w-3.5 h-3.5 text-sky-400 stroke-[2.2]" />
                          ) : msg.status === 'delivered' ? (
                            <CheckCheck className="w-3.5 h-3.5 text-neutral-400 stroke-[2]" />
                          ) : (
                            <Check className="w-3.5 h-3.5 text-neutral-400 stroke-[2]" />
                          )}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Quick Sudanese Marketplace Replies */}
            <div className="px-3 py-1.5 bg-neutral-950/60 border-t border-neutral-800 flex items-center gap-1.5 overflow-x-auto no-scrollbar">
              {t.quickReplies.map((reply, idx) => (
                <button
                  key={idx}
                  onClick={() => handleQuickReply(reply)}
                  className="px-2.5 py-1 rounded-full bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-[11px] whitespace-nowrap border border-neutral-700 transition-colors cursor-pointer"
                >
                  {reply}
                </button>
              ))}
            </div>

            {/* Microphone permission or fallback notice if triggered */}
            {showAudioFallbackPrompt && (
              <div className="bg-amber-950/60 border-t border-amber-500/30 px-3 py-2 flex items-center justify-between gap-2 text-xs">
                <div className="flex items-center gap-1.5 text-amber-200 min-w-0">
                  <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
                  <span className="truncate">{t.micBlockedOrUnavailable}</span>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={sendSampleVoiceNote}
                    className="px-2.5 py-1 bg-amber-500 text-neutral-950 font-bold rounded-lg hover:bg-amber-400 transition-colors cursor-pointer text-xs flex items-center gap-1"
                  >
                    <Mic className="w-3.5 h-3.5" />
                    <span>{t.sendSampleAudio}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowAudioFallbackPrompt(false)}
                    className="p-1 text-neutral-400 hover:text-white cursor-pointer"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}

            {/* Drag & Drop Visual Indicator if user drags image over chat */}
            {isDragOver && (
              <div className="absolute inset-0 z-40 bg-amber-500/15 border-2 border-dashed border-amber-400 backdrop-blur-xs flex flex-col items-center justify-center p-6 text-center pointer-events-none animate-in fade-in duration-150">
                <div className="w-14 h-14 rounded-2xl bg-neutral-900 border border-amber-400 flex items-center justify-center text-amber-400 mb-2 shadow-xl">
                  <Upload className="w-7 h-7 animate-bounce" />
                </div>
                <p className="text-sm font-bold text-white mb-1">{t.dragDropHint}</p>
                <p className="text-xs text-neutral-300">JPEG, PNG, WebP</p>
              </div>
            )}

            {/* Staged Image Preview Bar (before sending) */}
            {stagedImage && (
              <div className="px-3 pt-2.5 pb-2 bg-neutral-950 border-t border-neutral-800 flex flex-col gap-2 animate-in fade-in duration-200">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div 
                      onClick={() => {
                        resetLightboxZoom();
                        setActiveLightboxImage({ url: stagedImage, caption: t.photoReadyToSend, tag: stagedImageTag || undefined });
                      }}
                      className="relative group w-12 h-12 rounded-lg overflow-hidden border border-amber-500/60 shrink-0 cursor-pointer shadow-md"
                      title={t.viewFullPhoto}
                    >
                      <img
                        src={stagedImage}
                        alt="Attached preview"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      />
                      <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
                        <Maximize2 className="w-3.5 h-3.5 text-white" />
                      </div>
                    </div>
                    <div className="flex flex-col min-w-0">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="text-xs font-bold text-amber-400 flex items-center gap-1 truncate">
                          <Camera className="w-3.5 h-3.5 shrink-0" />
                          {t.photoReadyToSend}
                        </span>
                        <span className="px-1.5 py-0.2 rounded text-[9px] font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                          {t.originalQualityLabel}
                        </span>
                      </div>
                      <span className="text-[10px] text-neutral-400 truncate">
                        {language === 'ar' ? 'تم إلغاء الضغط — سيتم إرسال الصورة بدقتها الأصلية الكاملة' : 'No compression — image will be sent in full original resolution'}
                      </span>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setStagedImage(null);
                      setStagedImageTag(null);
                    }}
                    title={t.removePhoto}
                    className="p-1.5 rounded-lg bg-neutral-800 hover:bg-rose-950 hover:text-rose-400 text-neutral-400 border border-neutral-700 transition-colors cursor-pointer shrink-0"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                {/* Quick inspection category tag selector */}
                <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-0.5">
                  <span className="text-[10px] text-neutral-400 shrink-0 flex items-center gap-1">
                    <Tag className="w-3 h-3 text-amber-400" />
                    {t.selectTag}
                  </span>
                  {[
                    { id: 'body', label: language === 'ar' ? 'فحص الهيكل' : 'Body' },
                    { id: 'receipt', label: language === 'ar' ? 'فاتورة وضمان' : 'Receipt' },
                    { id: 'battery', label: language === 'ar' ? 'البطارية والسيريال' : 'Battery' },
                    { id: 'accessories', label: language === 'ar' ? 'الملحقات' : 'Accessories' },
                  ].map((cat) => {
                    const isSelected = stagedImageTag === cat.label;
                    return (
                      <button
                        key={cat.id}
                        type="button"
                        onClick={() => setStagedImageTag(isSelected ? null : cat.label)}
                        className={`px-2 py-0.5 rounded-md text-[10px] font-semibold border transition-all cursor-pointer whitespace-nowrap ${
                          isSelected
                            ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-xs'
                            : 'bg-neutral-900 hover:bg-neutral-800 text-neutral-300 border-neutral-700'
                        }`}
                      >
                        {cat.label}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}


            {/* Hidden native file input for gallery / file picker */}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/*"
              onChange={handleImageSelect}
              className="hidden"
            />

            {/* Centered "تم البيع" (Sold) Button above the message input bar in the middle when offer is accepted */}
            {activeConv && activeConv.activeOffer?.status === 'accepted' && (
              <div className="py-2.5 px-4 bg-gradient-to-r from-emerald-950/30 via-neutral-950 to-emerald-950/30 border-t border-emerald-500/20 flex flex-col items-center justify-center gap-1.5 animate-in fade-in slide-in-from-bottom-1 duration-200">
                {convProduct ? (
                  <button
                    type="button"
                    onClick={() => setSoldConfirmProduct(convProduct)}
                    className="py-2 px-6 rounded-2xl bg-gradient-to-r from-emerald-600 via-emerald-500 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white font-black text-xs shadow-lg shadow-emerald-950/60 hover:scale-105 active:scale-95 transition-all flex items-center justify-center gap-2 cursor-pointer border border-emerald-400/40"
                  >
                    <CheckCircle2 className="w-4 h-4 text-white" />
                    <span>{t.markAsSold}</span>
                    <span className="text-[10px] bg-emerald-700/60 text-emerald-100 px-2 py-0.5 rounded-full font-medium">
                      {language === 'ar' ? 'حذف من الإعلانات' : 'Remove Listing'}
                    </span>
                  </button>
                ) : (
                  <div className="flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-emerald-950/80 border border-emerald-700/70 text-emerald-400 text-xs font-bold shadow-sm">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    <span>
                      {language === 'ar' 
                        ? 'تم تأكيد بيع السلعة وحذفها من الإعلانات النشطة بنجاح' 
                        : 'Item sold and removed from active listings'}
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Message Input & Voice Recording Bar */}
            {isRecording ? (
              <div className="p-3 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between gap-2.5">
                {/* Active recording timer and animated waveform */}
                <div className="flex items-center gap-2.5 min-w-0">
                  <span className="relative flex h-3 w-3 shrink-0">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
                  </span>
                  <span className="text-xs sm:text-sm font-bold text-red-400 font-mono shrink-0">
                    {formatAudioDuration(recordingDuration)}
                  </span>
                  <span className="text-xs text-neutral-300 hidden sm:inline truncate">
                    {t.recordingAudio}
                  </span>
                  
                  {/* Soundwave bounce bars */}
                  <div className="flex items-center gap-1 h-4 ms-1 shrink-0">
                    <div className="w-1 bg-red-500 rounded-full h-3 animate-pulse"></div>
                    <div className="w-1 bg-red-400 rounded-full h-4 animate-bounce"></div>
                    <div className="w-1 bg-red-500 rounded-full h-2 animate-pulse"></div>
                    <div className="w-1 bg-red-400 rounded-full h-3.5 animate-bounce"></div>
                  </div>
                </div>

                {/* Recording Actions (Cancel & Send) */}
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={cancelRecording}
                    className="px-3 py-2 rounded-xl bg-neutral-800 hover:bg-rose-950/50 hover:text-rose-400 text-neutral-300 text-xs font-semibold flex items-center gap-1.5 border border-neutral-700 transition-colors cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4 text-rose-400" />
                    <span>{t.cancelRecording}</span>
                  </button>

                  <button
                    type="button"
                    onClick={stopAndSendRecording}
                    className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-neutral-950 text-xs font-bold flex items-center gap-1.5 transition-all shadow-md cursor-pointer hover:scale-105 active:scale-95"
                  >
                    <ArrowUp className="w-4 h-4 stroke-[2.5]" />
                    <span>{t.sendRecording}</span>
                  </button>
                </div>
              </div>
            ) : (
              <form 
                onSubmit={handleSend} 
                className="relative p-3 bg-neutral-950 border-t border-neutral-800 flex items-center gap-2"
                onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={handleDrop}
              >
                {/* Voice record button to the RIGHT of the message input bar */}
                <button
                  type="button"
                  onClick={startRecording}
                  title={t.recordVoiceNote}
                  aria-label={t.recordVoiceNote}
                  className="w-10 h-10 rounded-xl bg-amber-500/15 hover:bg-amber-500/25 text-amber-400 border border-amber-500/30 flex items-center justify-center transition-all cursor-pointer shrink-0 hover:scale-105 active:scale-95 shadow-sm group"
                >
                  <Mic className="w-5 h-5 group-hover:text-amber-300 transition-colors" />
                </button>

                {/* Attach Image button with options popover */}
                <div className="relative shrink-0">
                  <button
                    type="button"
                    onClick={() => setShowCameraActionMenu(!showCameraActionMenu)}
                    title={t.attachPhotoTooltip}
                    aria-label={t.attachPhoto}
                    className={`w-10 h-10 rounded-xl border flex items-center justify-center transition-all cursor-pointer shrink-0 hover:scale-105 active:scale-95 shadow-sm group ${
                      showCameraActionMenu
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md'
                        : 'bg-neutral-800/80 hover:bg-neutral-700 text-amber-400 border-neutral-700 hover:border-amber-500/50'
                    }`}
                  >
                    <Camera className="w-5 h-5 group-hover:scale-110 transition-transform" />
                  </button>

                  {/* Camera Action Popover Menu */}
                  {showCameraActionMenu && (
                    <div 
                      className="absolute bottom-12 start-0 z-30 w-56 bg-neutral-900 border border-neutral-700 rounded-2xl p-1.5 shadow-2xl flex flex-col gap-1 animate-in zoom-in-95 duration-150"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {/* Live Camera Option */}
                      <button
                        type="button"
                        onClick={() => startLiveCamera()}
                        className="w-full flex items-center gap-2.5 p-2 rounded-xl hover:bg-amber-500/15 text-start text-xs font-semibold text-white hover:text-amber-300 transition-colors cursor-pointer group"
                      >
                        <div className="w-7 h-7 rounded-lg bg-neutral-800 group-hover:bg-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
                          <Camera className="w-4 h-4" />
                        </div>
                        <div className="flex flex-col">
                          <span>{t.openLiveCamera}</span>
                          <span className="text-[10px] text-neutral-400 font-normal">{language === 'ar' ? 'التقاط فوري بعدسة الكاميرا' : 'Instant snapshot'}</span>
                        </div>
                      </button>

                      {/* File / Gallery Option */}
                      <button
                        type="button"
                        onClick={() => {
                          setShowCameraActionMenu(false);
                          fileInputRef.current?.click();
                        }}
                        className="w-full flex items-center gap-2.5 p-2 rounded-xl hover:bg-amber-500/15 text-start text-xs font-semibold text-white hover:text-amber-300 transition-colors cursor-pointer group"
                      >
                        <div className="w-7 h-7 rounded-lg bg-neutral-800 group-hover:bg-amber-500/20 flex items-center justify-center text-amber-400 shrink-0">
                          <Upload className="w-4 h-4" />
                        </div>
                        <div className="flex flex-col">
                          <span>{t.chooseFromGallery}</span>
                          <span className="text-[10px] text-neutral-400 font-normal">{language === 'ar' ? 'من ملفات الجهاز أو الاستوديو' : 'From phone or computer'}</span>
                        </div>
                      </button>

                    </div>
                  )}
                </div>

                {/* Message text input with paste support */}
                <input
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  onPaste={handlePaste}
                  placeholder={
                    stagedImage 
                      ? (language === 'ar' ? 'اكتب رسالة توضيحية مع الصورة (اختياري)...' : 'Add an optional note with the photo...') 
                      : t.typeMessagePlaceholder
                  }
                  className="flex-1 bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-2.5 text-xs sm:text-sm text-white placeholder-neutral-500 focus:outline-none focus:border-amber-500"
                />

                {/* Send button (enabled if text OR stagedImage) */}
                <button
                  type="submit"
                  disabled={!inputMessage.trim() && !stagedImage}
                  className="w-10 h-10 rounded-xl bg-amber-500 hover:bg-amber-400 disabled:opacity-40 disabled:pointer-events-none text-neutral-950 flex items-center justify-center transition-all cursor-pointer shrink-0 shadow-md"
                  title={t.sendBtn}
                >
                  <ArrowUp className="w-5 h-5 stroke-[2.5]" />
                </button>
              </form>
            )}
          </div>
        ) : (
          <div className="hidden md:flex flex-1 flex-col items-center justify-center p-8 text-center text-neutral-400 bg-neutral-900/40">
            <div className="w-16 h-16 rounded-2xl bg-neutral-800/80 border border-neutral-700/60 flex items-center justify-center text-amber-400 mb-4 shadow-lg">
              <MessageSquare className="w-8 h-8" />
            </div>
            <h4 className="text-base font-bold text-white mb-1.5">
              {language === 'ar' ? 'محادثات وعروض هتش' : 'HATCH Messages'}
            </h4>
            <p className="text-xs text-neutral-400 max-w-xs leading-relaxed">
              {language === 'ar' ? 'اختر محادثة من القائمة للتفاوض والمراسلة مع البائع' : 'Select a conversation from the list to start chatting'}
            </p>
          </div>
        )}
      </div>

      {/* Live Camera Viewfinder Modal */}
      {isCameraModalOpen && (
        <div 
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex flex-col items-center justify-center p-2 sm:p-4 animate-in fade-in duration-200"
          onClick={stopLiveCamera}
        >
          <div 
            className="relative max-w-md w-full bg-neutral-950 border border-neutral-700 rounded-3xl overflow-hidden shadow-2xl flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div className="flex items-center justify-between p-3.5 bg-neutral-900/90 border-b border-neutral-800">
              <div className="flex items-center gap-2 text-white font-semibold text-xs sm:text-sm">
                <Camera className="w-4 h-4 text-amber-400" />
                <span>{t.cameraModalTitle}</span>
              </div>
              <button
                type="button"
                onClick={stopLiveCamera}
                className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors cursor-pointer"
                title={t.closePhoto}
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Viewfinder Video Area / Captured Frame */}
            <div className="relative aspect-[4/3] bg-black flex items-center justify-center overflow-hidden">
              {capturedFrame ? (
                <img
                  src={capturedFrame}
                  alt="Captured snapshot"
                  className="w-full h-full object-cover"
                />
              ) : cameraError ? (
                <div className="p-6 text-center flex flex-col items-center gap-3 text-neutral-300">
                  <AlertCircle className="w-10 h-10 text-amber-400" />
                  <p className="text-xs max-w-xs leading-relaxed text-amber-200">{cameraError}</p>
                  <button
                    type="button"
                    onClick={() => {
                      stopLiveCamera();
                      fileInputRef.current?.click();
                    }}
                    className="mt-2 px-4 py-2 bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold rounded-xl text-xs flex items-center gap-1.5 cursor-pointer"
                  >
                    <Upload className="w-4 h-4" />
                    <span>{t.chooseFromGallery}</span>
                  </button>
                </div>
              ) : (
                <>
                  <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    muted
                    className="w-full h-full object-cover"
                  />

                  {/* Camera HUD Corner Guides */}
                  <div className="absolute inset-4 pointer-events-none border border-white/20 rounded-2xl">
                    <div className="absolute top-0 left-0 w-4 h-4 border-t-2 border-l-2 border-amber-400 rounded-tl-lg"></div>
                    <div className="absolute top-0 right-0 w-4 h-4 border-t-2 border-r-2 border-amber-400 rounded-tr-lg"></div>
                    <div className="absolute bottom-0 left-0 w-4 h-4 border-b-2 border-l-2 border-amber-400 rounded-bl-lg"></div>
                    <div className="absolute bottom-0 right-0 w-4 h-4 border-b-2 border-r-2 border-amber-400 rounded-br-lg"></div>
                  </div>

                  {/* Flash Animation */}
                  {isFlashActive && (
                    <div className="absolute inset-0 bg-white opacity-80 pointer-events-none transition-opacity duration-150"></div>
                  )}
                </>
              )}
            </div>

            {/* Camera Controls Bar */}
            <div className="p-4 bg-neutral-950 border-t border-neutral-800 flex items-center justify-between">
              {capturedFrame ? (
                <div className="w-full flex items-center justify-between gap-3">
                  <button
                    type="button"
                    onClick={() => setCapturedFrame(null)}
                    className="flex-1 py-2.5 px-3 bg-neutral-800 hover:bg-neutral-700 text-neutral-200 font-semibold rounded-xl text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <RotateCcw className="w-4 h-4" />
                    <span>{t.retakePhoto}</span>
                  </button>
                  <button
                    type="button"
                    onClick={acceptCapturedPhoto}
                    className="flex-1 py-2.5 px-3 bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold rounded-xl text-xs flex items-center justify-center gap-1.5 shadow-lg transition-transform hover:scale-102 active:scale-98 cursor-pointer"
                  >
                    <Check className="w-4 h-4 stroke-[2.5]" />
                    <span>{t.usePhoto}</span>
                  </button>
                </div>
              ) : (
                <div className="w-full flex items-center justify-between">
                  {/* Flip camera */}
                  <button
                    type="button"
                    onClick={flipCameraFacing}
                    title={t.flipCamera}
                    className="p-3 rounded-2xl bg-neutral-800/80 hover:bg-neutral-700 text-neutral-200 border border-neutral-700 transition-colors cursor-pointer"
                  >
                    <RefreshCw className="w-5 h-5" />
                  </button>

                  {/* Big Circular Shutter Button */}
                  <button
                    type="button"
                    onClick={snapLivePhoto}
                    title={t.snapPhoto}
                    className="w-16 h-16 rounded-full border-4 border-amber-400/80 p-1 flex items-center justify-center transition-transform hover:scale-105 active:scale-90 cursor-pointer shadow-xl group"
                  >
                    <div className="w-full h-full bg-white group-hover:bg-amber-400 rounded-full transition-colors shadow-inner"></div>
                  </button>

                  {/* Fallback to gallery upload */}
                  <button
                    type="button"
                    onClick={() => {
                      stopLiveCamera();
                      fileInputRef.current?.click();
                    }}
                    title={t.chooseFromGallery}
                    className="p-3 rounded-2xl bg-neutral-800/80 hover:bg-neutral-700 text-neutral-200 border border-neutral-700 transition-colors cursor-pointer"
                  >
                    <Upload className="w-5 h-5" />
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Enhanced Lightbox / Fullscreen Image Viewer Modal */}
      {activeLightboxImage && (
        <div 
          className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex flex-col items-center justify-center p-2 sm:p-4 animate-in fade-in duration-200"
          onClick={() => setActiveLightboxImage(null)}
        >
          <div 
            className="relative max-w-4xl w-full max-h-[94vh] bg-neutral-900 border border-neutral-700/80 rounded-3xl overflow-hidden shadow-2xl flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Lightbox Header with Zoom and Download Controls */}
            <div className="flex items-center justify-between p-3.5 bg-neutral-950 border-b border-neutral-800">
              <div className="flex items-center gap-2 text-white font-semibold text-xs sm:text-sm truncate">
                <Camera className="w-4 h-4 text-amber-400 shrink-0" />
                <span className="truncate">{activeLightboxImage.caption || t.photoInspectionLabel}</span>
                {activeLightboxImage.tag && (
                  <span className="px-2 py-0.5 rounded-md bg-amber-500/20 text-amber-300 border border-amber-500/30 text-[10px] font-bold shrink-0">
                    {activeLightboxImage.tag}
                  </span>
                )}
              </div>

              {/* Controls (No +/- buttons, native Android gesture zoom) */}
              <div className="flex items-center gap-1.5 shrink-0">
                {lightboxScale > 1.05 && (
                  <>
                    <span className="px-2 py-0.5 rounded-lg bg-amber-500/20 text-amber-300 font-mono text-xs font-bold border border-amber-500/30">
                      {lightboxScale.toFixed(1)}x
                    </span>
                    <button
                      type="button"
                      onClick={resetLightboxZoom}
                      className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-amber-400 hover:text-amber-300 transition-colors cursor-pointer"
                      title={t.resetZoom}
                    >
                      <RotateCcw className="w-4 h-4" />
                    </button>
                  </>
                )}
                <a
                  href={activeLightboxImage.url}
                  download={`hatch-product-photo-${Date.now()}.jpg`}
                  target="_blank"
                  rel="noreferrer"
                  className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors cursor-pointer"
                  title={t.downloadPhoto}
                >
                  <Download className="w-4 h-4" />
                </a>
                <button
                  type="button"
                  onClick={() => setActiveLightboxImage(null)}
                  className="p-1.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors cursor-pointer ms-1"
                  title={t.closePhoto}
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Photo Center with Android-style pinch-to-zoom & pan */}
            <div 
              className="relative overflow-hidden flex-1 min-h-[320px] max-h-[75vh] flex items-center justify-center p-3 bg-neutral-950/80 no-scrollbar touch-none cursor-grab active:cursor-grabbing select-none"
              onTouchStart={handleTouchStart}
              onTouchMove={handleTouchMove}
              onTouchEnd={handleTouchEnd}
              onTouchCancel={handleTouchEnd}
              onWheel={handleWheelZoom}
              style={{ touchAction: 'none' }}
            >
              <img
                src={activeLightboxImage.url}
                alt="Product inspection full view"
                draggable={false}
                style={{ 
                  transform: `translate3d(${lightboxPan.x}px, ${lightboxPan.y}px, 0) scale(${lightboxScale})`, 
                  transformOrigin: 'center center',
                  transition: isTouchInteracting ? 'none' : 'transform 0.2s cubic-bezier(0.2, 0, 0, 1)'
                }}
                className="max-w-full max-h-[70vh] object-contain rounded-2xl shadow-2xl select-none pointer-events-none"
              />
            </div>

            {/* Lightbox Footer with Android Gesture Guide */}
            <div className="p-3 bg-neutral-950 border-t border-neutral-800 text-[11px] text-neutral-400 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2 text-neutral-300">
                <span className="p-1 rounded-md bg-amber-500/10 text-amber-400 border border-amber-500/20">
                  <Hand className="w-3.5 h-3.5" />
                </span>
                <span className="font-medium text-amber-300/90">
                  {t.pinchZoomGuide}
                </span>
              </div>
              <div className="flex items-center gap-2 ms-auto">
                {lightboxScale > 1.05 && (
                  <button
                    type="button"
                    onClick={resetLightboxZoom}
                    className="px-2.5 py-1 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 hover:text-white rounded-lg text-xs font-medium cursor-pointer transition-colors flex items-center gap-1.5"
                  >
                    <RotateCcw className="w-3.5 h-3.5 text-amber-400" />
                    <span>{t.resetZoom}</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setActiveLightboxImage(null)}
                  className="px-3 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-white rounded-lg text-xs font-semibold cursor-pointer shrink-0"
                >
                  {t.closePhoto}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Sold Confirmation Modal */}
      <SoldConfirmModal
        product={soldConfirmProduct}
        isOpen={Boolean(soldConfirmProduct)}
        onClose={() => setSoldConfirmProduct(null)}
        onConfirm={() => {
          if (soldConfirmProduct && activeConv) {
            markProductAsSold(soldConfirmProduct.id);
            sendMessage(
              activeConv.id, 
              language === 'ar' 
                ? '🤝 تم تأكيد بيع السلعة ونقلها إلى قسم المبيعات الناجحة. شكراً جزيلاً لتعاملكم الراقي!' 
                : '🤝 Sale confirmed and moved to completed deals. Thank you!'
            );
            setSoldConfirmProduct(null);
          }
        }}
      />
    </div>
  );
};
