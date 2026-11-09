import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause, Mic, Volume2 } from 'lucide-react';
import { formatAudioDuration } from '../utils/audioUtils';

interface AudioMessagePlayerProps {
  audioUrl: string;
  duration?: number;
  isMe: boolean;
  label?: string;
}

export const AudioMessagePlayer: React.FC<AudioMessagePlayerProps> = ({
  audioUrl,
  duration = 0,
  isMe,
  label = 'تسجيل صوتي',
}) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [totalDuration, setTotalDuration] = useState(duration || 0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const handleLoadedMetadata = () => {
      if (audio.duration && !isNaN(audio.duration) && isFinite(audio.duration)) {
        setTotalDuration(Math.round(audio.duration));
      }
    };

    const handleTimeUpdate = () => {
      setCurrentTime(audio.currentTime);
    };

    const handleEnded = () => {
      setIsPlaying(false);
      setCurrentTime(0);
    };

    const handlePause = () => {
      setIsPlaying(false);
    };

    const handlePlay = () => {
      setIsPlaying(true);
    };

    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('pause', handlePause);
    audio.addEventListener('play', handlePlay);

    return () => {
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('pause', handlePause);
      audio.removeEventListener('play', handlePlay);
    };
  }, []);

  const togglePlay = () => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      audio.pause();
    } else {
      audio.play().catch((err) => {
        console.warn('Playback interrupted:', err);
      });
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = audioRef.current;
    if (!audio) return;
    const seekTime = Number(e.target.value);
    audio.currentTime = seekTime;
    setCurrentTime(seekTime);
  };

  const effectiveDuration = totalDuration || duration || 3;
  const progressPercent = Math.min(100, Math.max(0, (currentTime / effectiveDuration) * 100));

  // Static decorative waveform bar heights to create a realistic voice spectrum
  const waveformHeights = [
    30, 45, 80, 60, 95, 40, 70, 100, 85, 50, 75, 90, 40, 65, 85, 55, 35, 60
  ];

  return (
    <div className="flex flex-col gap-1.5 w-full min-w-[210px] max-w-[260px] sm:max-w-[290px]">
      <audio ref={audioRef} src={audioUrl} preload="metadata" />

      {/* Header badge */}
      <div className="flex items-center justify-between text-[10px] opacity-85 px-0.5">
        <span className="flex items-center gap-1 font-semibold">
          <Mic className="w-3 h-3" />
          <span>{label}</span>
        </span>
        <span className="flex items-center gap-1 font-mono">
          <Volume2 className="w-3 h-3 opacity-70" />
          <span>
            {formatAudioDuration(currentTime)} / {formatAudioDuration(effectiveDuration)}
          </span>
        </span>
      </div>

      {/* Main player bar */}
      <div className="flex items-center gap-2.5">
        {/* Play/Pause Button */}
        <button
          type="button"
          onClick={togglePlay}
          aria-label={isPlaying ? 'Pause audio' : 'Play audio'}
          className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 shadow transition-transform active:scale-95 cursor-pointer ${
            isMe
              ? 'bg-neutral-950 text-amber-400 hover:bg-neutral-900'
              : 'bg-amber-500 text-neutral-950 hover:bg-amber-400'
          }`}
        >
          {isPlaying ? (
            <Pause className="w-4 h-4 fill-current" />
          ) : (
            <Play className="w-4 h-4 fill-current translate-x-0.5 rtl:-translate-x-0.5" />
          )}
        </button>

        {/* Waveform and Progress Bar */}
        <div className="flex-1 flex flex-col justify-center gap-1 min-w-0">
          {/* Animated waveform visualizer */}
          <div className="flex items-center justify-between gap-[2px] h-5 px-1">
            {waveformHeights.map((h, i) => {
              const barProgress = (i / waveformHeights.length) * 100;
              const isPast = progressPercent >= barProgress;

              return (
                <div
                  key={i}
                  style={{ height: `${h}%` }}
                  className={`w-[3px] rounded-full transition-all duration-150 ${
                    isPast
                      ? isMe
                        ? 'bg-neutral-950/90'
                        : 'bg-amber-400'
                      : isMe
                      ? 'bg-neutral-950/30'
                      : 'bg-neutral-600/50'
                  } ${isPlaying && isPast ? 'animate-pulse' : ''}`}
                />
              );
            })}
          </div>

          {/* Interactive seek track */}
          <input
            type="range"
            min={0}
            max={effectiveDuration}
            step={0.1}
            value={currentTime}
            onChange={handleSeek}
            className={`w-full h-1 rounded-lg appearance-none cursor-pointer focus:outline-none ${
              isMe ? 'accent-neutral-950 bg-neutral-950/30' : 'accent-amber-400 bg-neutral-700/60'
            }`}
          />
        </div>
      </div>
    </div>
  );
};
