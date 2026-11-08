/**
 * Lógica pura del grabador de pantalla y utilidades de codificación.
 * Este módulo contiene funciones matemáticas y de validación desacopladas de la interfaz de usuario,
 * permitiendo ejecutar tests unitarios deterministas con node:test.
 */

import { ResolutionConfig, NativeEngineType, EngineLiveMetrics } from '../types/recorder';

/**
/**
 * Genera preajustes de resolución escalados proporcionalmente a la pantalla nativa del dispositivo.
 * Garantiza que todas las dimensiones sean números pares (% 2 === 0) requeridos por codificadores
 * YUV420p de hardware y conserven exactamente la relación de aspecto del teléfono (evitando barras negras).
 */
export function generateNativeResolutionPresets(
  screenWidth: number = 1080,
  screenHeight: number = 2400
): ResolutionConfig[] {
  const cleanW = screenWidth % 2 === 0 ? screenWidth : screenWidth - 1;
  const cleanH = screenHeight % 2 === 0 ? screenHeight : screenHeight - 1;
  const aspect = cleanH / Math.max(1, cleanW);
  const ratioLabel = `Nativa (${aspect.toFixed(2)}:1)`;

  const makePreset = (
    id: string,
    name: string,
    targetW: number,
    scalePercent: number,
    recommendedBitrateMbps: number,
    category: 'SD' | 'HD' | 'FHD' | '2K' | '4K' | 'NATIVE'
  ): ResolutionConfig => {
    const rawH = Math.round(targetW * aspect);
    const evenW = targetW % 2 === 0 ? targetW : targetW - 1;
    const evenH = rawH % 2 === 0 ? rawH : rawH + 1;

    return {
      id,
      name,
      width: evenW,
      height: evenH,
      aspectRatio: ratioLabel,
      recommendedBitrateMbps,
      category,
      isNative: true,
      scalePercent,
      scaleLabel: `${scalePercent}%`
    };
  };

  return [
    makePreset('native-100', `Nativa 100% (${cleanW}x${cleanH})`, cleanW, 100, cleanW >= 1440 ? 24 : cleanW >= 1080 ? 14 : 8, 'NATIVE'),
    makePreset('native-fhd', 'Nativa FHD Adaptada (1080p)', 1080, Math.min(100, Math.round((1080 / cleanW) * 100)), 12, 'FHD'),
    makePreset('native-hd', 'Nativa HD Adaptada (720p)', 720, Math.min(100, Math.round((720 / cleanW) * 100)), 8, 'HD'),
    makePreset('native-sd', 'Nativa Ahorro / Gaming (540p)', 540, Math.min(100, Math.round((540 / cleanW) * 100)), 4, 'SD'),
    makePreset('native-2k', 'Nativa 2K Ultra (1440p)', 1440, Math.round((1440 / cleanW) * 100), 24, '2K')
  ];
}

export const NATIVE_DEFAULT_PRESETS = generateNativeResolutionPresets(1080, 2400);

/**
 * Presets estándar de resoluciones optimizadas para pantallas móviles.
 * Se aseguran dimensiones pares requeridas por codificadores de hardware YUV420p.
 */
export const RESOLUTION_PRESETS: ResolutionConfig[] = [
  NATIVE_DEFAULT_PRESETS[0], // Nativa 100% (Pantalla Completa)
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
    id: '720p',
    name: '720p HD (Equilibrado)',
    width: 720,
    height: 1280,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 6,
    category: 'HD'
  },
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

/**
 * Valida y describe la fuente de captura de audio seleccionada.
 */
export function validateAudioSource(source: string): {
  valid: boolean;
  mode: 'mic' | 'internal' | 'both' | 'mute';
  description: string;
} {
  switch (source) {
    case 'mic':
      return {
        valid: true,
        mode: 'mic',
        description: 'Micrófono del dispositivo (Voz del jugador)'
      };
    case 'internal':
      return {
        valid: true,
        mode: 'internal',
        description: 'Audio interno del sistema y aplicaciones (YouTube/Juegos en Android 10+)'
      };
    case 'both':
      return {
        valid: true,
        mode: 'both',
        description: 'Mezcla estéreo dual en tiempo real (Micrófono + Audio Interno)'
      };
    case 'mute':
      return {
        valid: true,
        mode: 'mute',
        description: 'Sin captura de audio (Silencio)'
      };
    default:
      return {
        valid: false,
        mode: 'both',
        description: 'Fuente de audio desconocida, se aplica fallback a mezcla dual'
      };
  }
}

/**
 * Realiza la suma de muestras PCM de 16 bits con saturación matemática (Clamping).
 * Previene el desbordamiento digital (distorsión/clipping) entre -32768 y 32767.
 */
export function mixPcm16Samples(sampleA: number, sampleB: number): number {
  const sum = sampleA + sampleB;
  if (sum > 32767) return 32767;
  if (sum < -32768) return -32768;
  return sum;
}

/**
 * Genera el comando de reparación de FFmpeg para rescatar archivos de video truncados
 * o dañados por apagones del smartphone o terminación de proceso por Android OOM Killer.
 */
export function getFfmpegRepairCommand(inputPath: string, outputPath: string): string {
  return `-y -err_detect ignore_err -i "${inputPath}" -c copy -movflags +faststart "${outputPath}"`;
}

/**
 * Genera el comando de multiplexación sin pérdidas con FFmpeg (+faststart para reproducción instantánea).
 */
export function getFfmpegLosslessMergeCommand(
  videoPath: string,
  audioPath: string,
  outputPath: string,
  isMkv: boolean = false
): string {
  const movFlags = isMkv ? '' : '-movflags +faststart';
  return `-y -i "${videoPath}" -i "${audioPath}" -c:v copy -c:a aac -b:a 192k -af apad -shortest -map 0:v:0 -map 1:a:0? ${movFlags} "${outputPath}"`.trim();
}

/**
 * Genera la ruta relativa estándar para el álbum público de la galería del teléfono (Movies/VeloRec).
 */
export function getGalleryPublicAlbumPath(fileName: string): string {
  const cleanName = fileName.replace(/[^a-zA-Z0-9._-]/g, '_');
  return `Movies/VeloRec/${cleanName}`;
}

/**
 * Detecta el tipo de contenedor multimedia según la extensión del archivo.
 */
export function detectVideoContainer(fileName: string): 'mp4' | 'mkv' | 'unknown' {
  const ext = fileName.split('.').pop()?.toLowerCase();
  if (ext === 'mp4') return 'mp4';
  if (ext === 'mkv') return 'mkv';
  return 'unknown';
}

/**
 * Presets de cuenta regresiva para preparar la captura antes de iniciar.
 * 0 = inicio inmediato sin espera, 3s = estándar recomendado, 5s y 10s para demos o juegos.
 */
export const COUNTDOWN_PRESETS = [0, 3, 5, 10] as const;
export type CountdownSeconds = typeof COUNTDOWN_PRESETS[number];

/**
 * Valida y describe la configuración de cuenta regresiva previa a la captura.
 */
export function validateCountdownSeconds(seconds: number): { valid: boolean; seconds: number; label: string } {
  if (seconds <= 0) return { valid: true, seconds: 0, label: 'Inmediato (Sin espera)' };
  if (seconds === 3) return { valid: true, seconds: 3, label: '3 segundos (Recomendado)' };
  if (seconds === 5) return { valid: true, seconds: 5, label: '5 segundos' };
  if (seconds === 10) return { valid: true, seconds: 10, label: '10 segundos' };
  // Fallback seguro a 3 segundos
  return { valid: false, seconds: 3, label: '3 segundos (Recomendado)' };
}


