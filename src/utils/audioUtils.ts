/**
 * Generates an audible PCM 16-bit WAV base64 data URL for testing voice notes
 * or serving as a fallback when microphone hardware permission is restricted.
 */
export function createSampleAudioDataUrl(durationSeconds = 3): string {
  const sampleRate = 8000;
  const numSamples = Math.floor(sampleRate * durationSeconds);
  const buffer = new ArrayBuffer(44 + numSamples * 2);
  const view = new DataView(buffer);

  function writeString(v: DataView, offset: number, string: string) {
    for (let i = 0; i < string.length; i++) {
      v.setUint8(offset + i, string.charCodeAt(i));
    }
  }

  // RIFF header
  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + numSamples * 2, true);
  writeString(view, 8, 'WAVE');

  // fmt sub-chunk
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true); // SubChunk1Size (16 for PCM)
  view.setUint16(20, 1, true); // AudioFormat (1 for PCM)
  view.setUint16(22, 1, true); // NumChannels (1 mono)
  view.setUint32(24, sampleRate, true); // SampleRate
  view.setUint32(28, sampleRate * 2, true); // ByteRate
  view.setUint16(32, 2, true); // BlockAlign
  view.setUint16(34, 16, true); // BitsPerSample

  // data sub-chunk
  writeString(view, 36, 'data');
  view.setUint32(40, numSamples * 2, true);

  // Write harmonic voice-like bell chime
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate;
    // Multi-tone voice chime (520Hz + 780Hz harmonic with soft decay)
    const decay = Math.exp(-t * 1.5);
    const sample = (
      Math.sin(2 * Math.PI * 523.25 * t) * 0.45 +
      Math.sin(2 * Math.PI * 659.25 * t) * 0.25 +
      Math.sin(2 * Math.PI * 783.99 * t) * 0.15
    ) * decay * 0.6;

    const clamped = Math.max(-1, Math.min(1, sample));
    const intSample = clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff;
    view.setInt16(44 + i * 2, intSample, true);
  }

  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return 'data:audio/wav;base64,' + btoa(binary);
}

export function formatAudioDuration(seconds: number): string {
  if (isNaN(seconds) || seconds < 0) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}
