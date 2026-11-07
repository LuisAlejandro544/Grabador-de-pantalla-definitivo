/**
 * Lógica pura del grabador de pantalla y utilidades de codificación.
 * Este módulo contiene funciones matemáticas y de validación desacopladas de la interfaz de usuario,
 * permitiendo ejecutar tests unitarios deterministas con node:test.
 */

import { ResolutionConfig, NativeEngineType, EngineLiveMetrics } from '../types/recorder';

/**
 * Presets estándar de resoluciones optimizadas para pantallas móviles
 * (Modo vertical típico 9:16 o modo horizontal 16:9).
 * Se aseguran dimensiones pares requeridas por codificadores de hardware YUV420p.
 */
export const RESOLUTION_PRESETS: ResolutionConfig[] = [
  {
    id: '480p',
    name: '480p SD (Ahorro de batería)',
    width: 480,
    height: 854,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 3,
    category: 'SD'
  },
  {
    id: '720p',
    name: '720p HD (Equilibrado)',
    width: 720,
    height: 1280,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 6,
    category: 'HD'
  },
  {
    id: '1080p',
    name: '1080p Full HD (Recomendado)',
    width: 1080,
    height: 1920,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 12,
    category: 'FHD'
  },
  {
    id: '1440p',
    name: '2K QHD (Máxima Nitidez)',
    width: 1440,
    height: 2560,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 20,
    category: '2K'
  },
  {
    id: '2160p',
    name: '4K Ultra HD (Profesional)',
    width: 2160,
    height: 3840,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 36,
    category: '4K'
  }
];

/**
 * Calcula el tamaño estimado de archivo en bytes.
 * Fórmula: ((Bitrate Video kbps + Bitrate Audio kbps) * 1000 / 8) * segundos
 */
export function calculateEstimatedFileSize(
  durationSec: number,
  videoBitrateMbps: number,
  audioBitrateKbps: number = 192
): number {
  if (durationSec <= 0 || videoBitrateMbps <= 0) return 0;
  const totalBitrateBps = (videoBitrateMbps * 1_000_000) + (audioBitrateKbps * 1_000);
  const bytesPerSec = totalBitrateBps / 8;
  return Math.round(bytesPerSec * durationSec);
}

/**
 * Formatea bytes en cadena legible (B, KB, MB, GB).
 */
export function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const safeI = Math.min(i, units.length - 1);
  return `${(bytes / Math.pow(1024, safeI)).toFixed(1)} ${units[safeI]}`;
}

/**
 * Formatea duración en segundos a formato mm:ss o hh:mm:ss.
 */
export function formatDuration(seconds: number): string {
  if (seconds < 0 || isNaN(seconds)) return '00:00';
  const totalSecs = Math.floor(seconds);
  const hrs = Math.floor(totalSecs / 3600);
  const mins = Math.floor((totalSecs % 3600) / 60);
  const secs = totalSecs % 60;

  const pad = (n: number) => n.toString().padStart(2, '0');

  if (hrs > 0) {
    return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
  }
  return `${pad(mins)}:${pad(secs)}`;
}

/**
 * Valida que una resolución sea compatible con codificadores de hardware móvil.
 * Los encoders (MediaCodec en Android) exigen que ancho y alto sean múltiplos de 2 o 16.
 */
export function validateResolution(width: number, height: number): { valid: boolean; reason?: string } {
  if (width < 240 || height < 240) {
    return { valid: false, reason: 'La resolución mínima admitida es 240x240 px.' };
  }
  if (width > 4096 || height > 4096) {
    return { valid: false, reason: 'La resolución máxima admitida es 4096x4096 px.' };
  }
  if (width % 2 !== 0 || height % 2 !== 0) {
    return { valid: false, reason: 'Tanto el ancho como el alto deben ser números pares para el codec YUV420p.' };
  }
  return { valid: true };
}

/**
 * Calcula métricas simuladas de rendimiento según el motor seleccionado:
 * - C++23: Compilado con GCC 14/Clang 18 con -O3, std::span y co_await. Máxima velocidad de codificación cruda, consumo constante de memoria.
 * - Rust (Edición 2024): Compilado con Rust 1.85+, LLVM optimizado, memory safety sin GC, excelente pipeline con crossbeam-channel y zero allocations.
 */
export function calculateEngineMetrics(
  engine: NativeEngineType,
  fps: number,
  bitrateMbps: number,
  elapsedSec: number
): EngineLiveMetrics {
  const isRust = engine === 'rust2024';
  
  // Variación leve determinista basada en el tiempo
  const cycle = Math.sin(elapsedSec * 0.5);

  if (isRust) {
    // Rust Edition 2024: Zero-cost abstractions, canal seguro sin contención
    return {
      cpuUsagePercent: Math.max(8, Math.min(22, Math.round(11 + (bitrateMbps * 0.3) + cycle * 2))),
      frameEncodingLatencyMs: Number((3.8 + (fps > 60 ? 1.8 : 0.8) + cycle * 0.4).toFixed(2)),
      memoryUsageMb: Math.round(36 + (bitrateMbps * 0.8)),
      droppedFrames: Math.max(0, Math.floor(elapsedSec * 0.015)),
      totalFrames: Math.max(0, Math.floor(elapsedSec * fps)),
      currentFps: Math.round(fps - (cycle > 0.8 ? 1 : 0)),
      thermalState: bitrateMbps > 25 ? 'Tibio' : 'Normal',
      zeroCopyEfficiency: 99.4
    };
  } else {
    // C++23: std::jthread, AMediaCodec directo por puntero crudo NDK
    return {
      cpuUsagePercent: Math.max(7, Math.min(20, Math.round(10 + (bitrateMbps * 0.28) + cycle * 1.8))),
      frameEncodingLatencyMs: Number((3.4 + (fps > 60 ? 1.5 : 0.6) + cycle * 0.3).toFixed(2)),
      memoryUsageMb: Math.round(32 + (bitrateMbps * 0.7)),
      droppedFrames: Math.max(0, Math.floor(elapsedSec * 0.012)),
      totalFrames: Math.max(0, Math.floor(elapsedSec * fps)),
      currentFps: Math.round(fps - (cycle > 0.9 ? 1 : 0)),
      thermalState: bitrateMbps > 28 ? 'Tibio' : 'Normal',
      zeroCopyEfficiency: 99.8
    };
  }
}
