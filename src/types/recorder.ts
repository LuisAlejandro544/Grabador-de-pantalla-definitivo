/**
 * Definiciones de tipos para el Grabador de Pantalla Móvil VeloRec
 * Arquitectura modular que integra React Native, Kotlin, C++23 y Rust (Edition 2024).
 */

export type NativeEngineType = 'cpp23' | 'rust2024';

export type VideoCodec = 'H264' | 'HEVC' | 'AV1';

export type AudioSourceType = 'internal' | 'mic' | 'both' | 'mute';

export type SupportedAbi = 'arm64-v8a' | 'armeabi-v7a' | 'x86_64';

export interface ResolutionConfig {
  id: string;
  name: string;
  width: number;
  height: number;
  aspectRatio: string;
  recommendedBitrateMbps: number;
  category: 'SD' | 'HD' | 'FHD' | '2K' | '4K';
}

export interface RecorderSettings {
  engine: NativeEngineType;
  resolution: ResolutionConfig;
  customWidth?: number;
  customHeight?: number;
  fps: number; // 24, 30, 60, 90, 120
  bitrateMbps: number; // 2 to 50 Mbps
  codec: VideoCodec;
  audioSource: AudioSourceType;
  audioSampleRate: number; // 44100 or 48000
  audioBitrateKbps: number; // 128, 192, 320
  showTouchPointers: boolean;
  enableFaceCam: boolean;
  targetAbis: SupportedAbi[];
  saveToGallery: boolean;
}

export interface RecordedVideoItem {
  id: string;
  title: string;
  blobUrl: string;
  mimeType: string;
  sizeBytes: number;
  durationSec: number;
  createdAt: number;
  engineUsed: NativeEngineType;
  resolution: string;
  fps: number;
  codec: VideoCodec;
  thumbnailUrl?: string;
}

export interface EngineLiveMetrics {
  cpuUsagePercent: number;
  frameEncodingLatencyMs: number;
  memoryUsageMb: number;
  droppedFrames: number;
  totalFrames: number;
  currentFps: number;
  thermalState: 'Normal' | 'Tibio' | 'Caliente';
  zeroCopyEfficiency: number; // %
}
