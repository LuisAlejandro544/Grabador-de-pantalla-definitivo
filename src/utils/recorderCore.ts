/**
 * Lógica pura del grabador de pantalla y utilidades de codificación.
 * Este módulo contiene funciones matemáticas y de validación desacopladas de la interfaz de usuario,
 * permitiendo ejecutar tests unitarios deterministas con node:test.
 */

import { ResolutionConfig, NativeEngineType, EngineLiveMetrics, VideoCodec, AudioSourceType } from '../types/recorder';

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
 * Calcula la etiqueta legible de relación de aspecto para una resolución dada.
 */
export function calculateAspectRatioLabel(width: number, height: number): string {
  const cleanW = Math.max(1, width % 2 === 0 ? width : width - 1);
  const cleanH = Math.max(1, height % 2 === 0 ? height : height - 1);
  const aspectDecimal = cleanH / cleanW;

  if (Math.abs(aspectDecimal - (20.0 / 9.0)) < 0.025) return '20:9';
  if (Math.abs(aspectDecimal - (19.5 / 9.0)) < 0.025) return '19.5:9';
  if (Math.abs(aspectDecimal - (19.0 / 9.0)) < 0.025) return '19:9';
  if (Math.abs(aspectDecimal - (21.0 / 9.0)) < 0.025) return '21:9';
  if (Math.abs(aspectDecimal - (16.0 / 9.0)) < 0.025) return '9:16';
  return `${aspectDecimal.toFixed(2)}:1`;
}

/**
 * Crea una configuración de resolución personalizada asegurando dimensiones pares.
 */
export function createCustomResolutionPreset(
  width: number,
  height: number,
  customName?: string
): ResolutionConfig {
  const cleanW = Math.max(240, width % 2 === 0 ? width : width - 1);
  const cleanH = Math.max(240, height % 2 === 0 ? height : height - 1);
  const ratio = calculateAspectRatioLabel(cleanW, cleanH);

  let category: 'SD' | 'HD' | 'FHD' | '2K' | '4K' | 'CUSTOM' = 'CUSTOM';
  let recommendedBitrate = 4;

  if (cleanW >= 2160) {
    category = '4K';
    recommendedBitrate = 36;
  } else if (cleanW >= 1440) {
    category = '2K';
    recommendedBitrate = 20;
  } else if (cleanW >= 1080) {
    category = 'FHD';
    recommendedBitrate = 12;
  } else if (cleanW >= 720) {
    category = 'HD';
    recommendedBitrate = 6;
  } else {
    category = 'SD';
    recommendedBitrate = 3;
  }

  return {
    id: `custom-${cleanW}x${cleanH}`,
    name: customName || `Personalizada (${cleanW}x${cleanH})`,
    width: cleanW,
    height: cleanH,
    aspectRatio: ratio,
    recommendedBitrateMbps: recommendedBitrate,
    category,
    isCustom: true,
    scalePercent: 100
  };
}

/**
 * Lista exhaustiva de resoluciones estándar más comunes en Android y redes sociales.
 */
export const STANDARD_POPULAR_PRESETS: ResolutionConfig[] = [
  // 20:9
  {
    id: 'std_2400x1080',
    name: '2400 x 1080 (FHD+ 20:9)',
    width: 1080,
    height: 2400,
    aspectRatio: '20:9',
    recommendedBitrateMbps: 12,
    category: 'FHD',
    formatGroup: '20:9'
  },
  {
    id: 'std_1600x720',
    name: '1600 x 720 (HD+ 20:9)',
    width: 720,
    height: 1600,
    aspectRatio: '20:9',
    recommendedBitrateMbps: 8,
    category: 'HD',
    formatGroup: '20:9'
  },
  {
    id: 'std_1200x540',
    name: '1200 x 540 (Ahorro 20:9)',
    width: 540,
    height: 1200,
    aspectRatio: '20:9',
    recommendedBitrateMbps: 4,
    category: 'SD',
    formatGroup: '20:9'
  },
  // 19.5:9 / 13:6
  {
    id: 'std_2340x1080',
    name: '2340 x 1080 (FHD+ 19.5:9)',
    width: 1080,
    height: 2340,
    aspectRatio: '19.5:9',
    recommendedBitrateMbps: 12,
    category: 'FHD',
    formatGroup: '19.5:9'
  },
  {
    id: 'std_1560x720',
    name: '1560 x 720 (HD+ 19.5:9)',
    width: 720,
    height: 1560,
    aspectRatio: '19.5:9',
    recommendedBitrateMbps: 8,
    category: 'HD',
    formatGroup: '19.5:9'
  },
  {
    id: 'std_1040x480',
    name: '1040 x 480 (SD+ 19.5:9)',
    width: 480,
    height: 1040,
    aspectRatio: '19.5:9',
    recommendedBitrateMbps: 4,
    category: 'SD',
    formatGroup: '19.5:9'
  },
  {
    id: 'std_780x360',
    name: '780 x 360 (Ultra Ahorro 19.5:9)',
    width: 360,
    height: 780,
    aspectRatio: '19.5:9',
    recommendedBitrateMbps: 3,
    category: 'SD',
    formatGroup: '19.5:9'
  },
  // 19:9
  {
    id: 'std_3040x1440',
    name: '3040 x 1440 (QHD+ 19:9)',
    width: 1440,
    height: 3040,
    aspectRatio: '19:9',
    recommendedBitrateMbps: 24,
    category: '2K',
    formatGroup: '19:9'
  },
  // 21:9
  {
    id: 'std_2520x1080',
    name: '2520 x 1080 (Cinemático 21:9)',
    width: 1080,
    height: 2520,
    aspectRatio: '21:9',
    recommendedBitrateMbps: 14,
    category: 'FHD',
    formatGroup: '21:9'
  },
  // 9:16
  {
    id: '2160p',
    name: '2160 x 3840 (4K UHD 9:16)',
    width: 2160,
    height: 3840,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 36,
    category: '4K',
    formatGroup: '9:16'
  },
  {
    id: '1440p',
    name: '1440 x 2560 (2K QHD 9:16)',
    width: 1440,
    height: 2560,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 20,
    category: '2K',
    formatGroup: '9:16'
  },
  {
    id: '1080p',
    name: '1080 x 1920 (FHD 9:16)',
    width: 1080,
    height: 1920,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 12,
    category: 'FHD',
    formatGroup: '9:16'
  },
  {
    id: '720p',
    name: '720 x 1280 (HD 9:16)',
    width: 720,
    height: 1280,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 6,
    category: 'HD',
    formatGroup: '9:16'
  },
  {
    id: '480p',
    name: '480 x 854 (SD 9:16)',
    width: 480,
    height: 854,
    aspectRatio: '9:16',
    recommendedBitrateMbps: 3,
    category: 'SD',
    formatGroup: '9:16'
  }
];

/**
 * Presets estándar de resoluciones optimizadas para pantallas móviles.
 * Se aseguran dimensiones pares requeridas por codificadores de hardware YUV420p.
 */
export const RESOLUTION_PRESETS: ResolutionConfig[] = [
  NATIVE_DEFAULT_PRESETS[0], // Nativa 100% (Pantalla Completa)
  ...STANDARD_POPULAR_PRESETS.filter(p => ['1080p', '720p', '480p', '1440p', '2160p'].includes(p.id))
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

/**
 * Clave de persistencia para almacenamiento local de ajustes del usuario.
 */
export const PREFERENCES_STORAGE_KEY = 'velorec_user_preferences_v1';

export interface PersistedUserPreferences {
  engine: NativeEngineType;
  resolutionId: string;
  customWidth?: number;
  customHeight?: number;
  bitrateMbps: number;
  fps: number;
  codec: VideoCodec;
  audioSource: AudioSourceType;
  countdownSec: number;
}

export const DEFAULT_USER_PREFERENCES: PersistedUserPreferences = {
  engine: 'rust2024',
  resolutionId: 'native-100',
  bitrateMbps: 12,
  fps: 60,
  codec: 'H264',
  audioSource: 'both',
  countdownSec: 3
};

/**
 * Serializa las preferencias del usuario a formato JSON para persistencia local.
 */
export function serializeUserPreferences(prefs: PersistedUserPreferences): string {
  return JSON.stringify(prefs);
}

/**
 * Deserializa las preferencias guardadas con validación defensiva y valores por defecto.
 */
export function parseUserPreferences(jsonString: string | null | undefined): PersistedUserPreferences {
  if (!jsonString) return { ...DEFAULT_USER_PREFERENCES };
  try {
    const parsed = JSON.parse(jsonString);
    return {
      engine: parsed.engine === 'cpp23' ? 'cpp23' : 'rust2024',
      resolutionId: typeof parsed.resolutionId === 'string' ? parsed.resolutionId : 'native-100',
      customWidth: typeof parsed.customWidth === 'number' && parsed.customWidth > 0 ? parsed.customWidth : undefined,
      customHeight: typeof parsed.customHeight === 'number' && parsed.customHeight > 0 ? parsed.customHeight : undefined,
      bitrateMbps: typeof parsed.bitrateMbps === 'number' && parsed.bitrateMbps > 0 ? parsed.bitrateMbps : 12,
      fps: typeof parsed.fps === 'number' && parsed.fps > 0 ? parsed.fps : 60,
      codec: ['H264', 'HEVC', 'AV1'].includes(parsed.codec) ? parsed.codec : 'H264',
      audioSource: ['internal', 'mic', 'both', 'mute'].includes(parsed.audioSource) ? parsed.audioSource : 'both',
      countdownSec: typeof parsed.countdownSec === 'number' && parsed.countdownSec >= 0 ? parsed.countdownSec : 3
    };
  } catch {
    return { ...DEFAULT_USER_PREFERENCES };
  }
}

/**
 * Valida si una cadena cumple con el formato estándar de un nombre de paquete Android
 * (ejemplo: 'com.dts.freefireth', 'com.tencent.ig', 'com.supercell.brawlstars').
 */
export function validateAppPackageName(packageName: string | null | undefined): boolean {
  if (!packageName || typeof packageName !== 'string') return false;
  const trimmed = packageName.trim();
  if (trimmed.length < 3 || trimmed.length > 256) return false;
  const regex = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/;
  return regex.test(trimmed);
}

/**
 * Describe las capacidades de la tubería de sombreadores en GPU (GLSL ES 3.20 y SPIR-V Vulkan).
 */
export function getShaderPipelineCapabilities() {
  return {
    formats: ['GLSL_ES32', 'SPIR_V'] as const,
    supportedEffects: [
      { id: 'passthrough', name: 'Passthrough Puro', acceleration: 'GPU Direct' },
      { id: 'facecam_circular', name: 'FaceCam Circular con Borde y Chroma Key', acceleration: 'GPU Hardware' },
      { id: 'gaming_color_boost', name: 'Gaming Color Boost (Saturación y Contraste)', acceleration: 'GPU Adreno/Mali' }
    ],
    vulkanSpirvSupported: true,
    glslEs32Supported: true,
    zeroCpuOverhead: true
  };
}

/**
 * Describe las especificaciones del motor de streaming nativo en Rust (Edition 2024) para protocolos de red.
 * Sustituye completamente el runtime de Go, eliminando pausas de Garbage Collector y reduciendo el uso de RAM.
 */
export function getRustStreamingEngineSpecs() {
  return {
    engine: 'Rust (Edition 2024)',
    protocols: ['RTMP', 'RTMPS', 'SRT', 'WebRTC'] as const,
    queueCapacity: 1024,
    ringBufferBytes: 4 * 1024 * 1024,
    zeroCopyNalDispatch: true,
    memoryFootprintMb: '< 10 MB',
    zeroGarbageCollection: true,
    crossPlatformAbis: ['arm64-v8a', 'armeabi-v7a', 'x86_64'] as const,
    nativeModule: 'streamer.rs'
  };
}

/**
 * Alias retrocompatible para el motor de streaming consolidado en Rust.
 */
export const getGoStreamingEngineSpecs = getRustStreamingEngineSpecs;


export interface StorageTechnologyReport {
  type: 'eMMC' | 'UFS' | 'NVMe';
  isEmmc: boolean;
  bufferPacingRecommended: boolean;
  pacingBatchLimit: number;
  description: string;
}

/**
 * Detecta si el dispositivo cuenta con almacenamiento eMMC o de alta velocidad (UFS/NVMe)
 * mediante el análisis de nodos de bloques Linux (/sys/block/mmcblk0 o /proc/partitions).
 * Si se detecta eMMC, recomienda activar el buffer pacing asíncrono para prevenir micro-congelamientos (stutters).
 */
export function detectStorageTechnology(blockDevices: string[] = ['mmcblk0']): StorageTechnologyReport {
  const hasEmmc = blockDevices.some(dev => dev.includes('mmcblk'));
  const hasNvme = blockDevices.some(dev => dev.includes('nvme'));

  if (hasEmmc) {
    return {
      type: 'eMMC',
      isEmmc: true,
      bufferPacingRecommended: true,
      pacingBatchLimit: 16,
      description: 'Almacenamiento eMMC 5.1 detectado. Buffer Pacing Asíncrono activo para amortiguar picos de I/O.'
    };
  }

  if (hasNvme) {
    return {
      type: 'NVMe',
      isEmmc: false,
      bufferPacingRecommended: false,
      pacingBatchLimit: 0,
      description: 'Almacenamiento NVMe PCIe de ultra-alta velocidad. Escritura directa habilitada.'
    };
  }

  return {
    type: 'UFS',
    isEmmc: false,
    bufferPacingRecommended: false,
    pacingBatchLimit: 0,
    description: 'Almacenamiento UFS (Universal Flash Storage) detectado. Escritura directa sin cuello de botella.'
  };
}

export interface ThreadAffinityReport {
  totalCores: number;
  efficiencyCoreIndices: number[];
  cpuAffinityMask: number;
  efficiencyCoresCount: number;
  bigCoresReservedForGaming: number;
  description: string;
}

/**
 * Calcula la afinidad de hilos óptima para procesadores móviles big.LITTLE (ej: Helio G81/G85/G88, Snapdragon).
 * Asigna los hilos de codificación y I/O exclusivamente a los núcleos de eficiencia (LITTLE, ej: Cortex-A55),
 * dejando los núcleos de alto rendimiento (Big, ej: Cortex-A75) 100% libres para el juego (Subway Surfers, Free Fire, etc.).
 */
export function calculateOptimalThreadAffinity(
  totalCores: number = 8,
  coreFrequenciesKhz?: number[]
): ThreadAffinityReport {
  const safeTotal = Math.max(1, totalCores);

  let efficiencyIndices: number[] = [];

  if (coreFrequenciesKhz && coreFrequenciesKhz.length === safeTotal) {
    const maxFreq = Math.max(...coreFrequenciesKhz);
    efficiencyIndices = coreFrequenciesKhz
      .map((freq, idx) => ({ freq, idx }))
      .filter(item => item.freq < maxFreq)
      .map(item => item.idx);
  }

  // Si no se proporcionaron frecuencias o todos tienen la misma frecuencia:
  // En SoCs de 8 núcleos estándar, reservamos la mitad inferior (cores 0..3) para tareas de fondo
  if (efficiencyIndices.length === 0) {
    const count = safeTotal >= 8 ? 4 : Math.max(1, Math.floor(safeTotal / 2));
    efficiencyIndices = Array.from({ length: count }, (_, i) => i);
  }

  let mask = 0;
  for (const idx of efficiencyIndices) {
    mask |= (1 << idx);
  }

  const bigCoresCount = safeTotal - efficiencyIndices.length;

  return {
    totalCores: safeTotal,
    efficiencyCoreIndices: efficiencyIndices,
    cpuAffinityMask: mask,
    efficiencyCoresCount: efficiencyIndices.length,
    bigCoresReservedForGaming: Math.max(0, bigCoresCount),
    description: `Afinidad fijada a ${efficiencyIndices.length} núcleos LITTLE. ${bigCoresCount} núcleos Big reservados para el videojuego.`
  };
}

export interface SavingProgressState {
  stage: 'stopping' | 'finalizing' | 'completed';
  percent: number;
  label: string;
  isComplete: boolean;
}

/**
 * Modela las etapas de guardado asíncrono con notificación de progreso en segundo plano.
 * Permite que la UI cambie de inmediato al estado inactivo sin congelar la experiencia del usuario.
 */
export function calculateSavingProgress(
  stage: 'stopping' | 'finalizing' | 'completed'
): SavingProgressState {
  switch (stage) {
    case 'stopping':
      return {
        stage: 'stopping',
        percent: 25,
        label: 'Deteniendo codificación...',
        isComplete: false
      };
    case 'finalizing':
      return {
        stage: 'finalizing',
        percent: 75,
        label: 'Publicando en Galería...',
        isComplete: false
      };
    case 'completed':
      return {
        stage: 'completed',
        percent: 100,
        label: '¡Grabación guardada con éxito!',
        isComplete: true
      };
  }
}

export interface DirectGallerySpeedupReport {
  fileSizeMb: number;
  legacyCopyTimeMs: number;
  directFdTimeMs: number;
  timeSavedMs: number;
  speedupFactor: number;
  description: string;
}

/**
 * Estima la ganancia de velocidad al escribir directamente sobre el Descriptor de Archivo (FD)
 * de la Galería (MediaStore) frente a la copia byte a byte tradicional en almacenamientos móviles.
 */
export function estimateDirectGallerySpeedup(
  fileSizeBytes: number,
  isEmmc: boolean = false
): DirectGallerySpeedupReport {
  const sizeMb = Math.max(0, fileSizeBytes / (1024 * 1024));
  // Velocidad típica de copia eMMC 5.1 (~40 MB/s con I/O contención) vs UFS (~180 MB/s)
  const copySpeedMbPerSec = isEmmc ? 40 : 180;
  const legacyCopyTimeMs = Math.round((sizeMb / copySpeedMbPerSec) * 1000);
  // Con targetFd directo, AMediaMuxer escribe directamente; finalizar es solo IS_PENDING = 0 (< 15 ms)
  const directFdTimeMs = 12;
  const timeSavedMs = Math.max(0, legacyCopyTimeMs - directFdTimeMs);
  const speedupFactor = legacyCopyTimeMs > 0 ? Number((legacyCopyTimeMs / directFdTimeMs).toFixed(1)) : 1;

  return {
    fileSizeMb: Number(sizeMb.toFixed(2)),
    legacyCopyTimeMs,
    directFdTimeMs,
    timeSavedMs,
    speedupFactor,
    description: `Ahorro de ${(timeSavedMs / 1000).toFixed(2)}s al eliminar la copia redundante de ${sizeMb.toFixed(1)} MB (${speedupFactor}x más rápido).`
  };
}

/**
 * Valida un objetivo de grabación directa sobre la Galería (MediaStore).
 */
export function validateDirectGalleryTarget(
  target: { rawFd: number; uri: string; filePath: string } | null | undefined
): { isValid: boolean; reason?: string } {
  if (!target) {
    return { isValid: false, reason: 'El descriptor de la galería no fue inicializado.' };
  }
  if (typeof target.rawFd !== 'number' || target.rawFd < 0) {
    return { isValid: false, reason: 'El descriptor de archivo nativo (FD) es inválido (< 0).' };
  }
  if (!target.uri || !target.uri.startsWith('content://')) {
    return { isValid: false, reason: 'La URI de MediaStore debe iniciar con content://.' };
  }
  if (!target.filePath || target.filePath.trim().length === 0) {
    return { isValid: false, reason: 'La ruta absoluta del archivo en Movies/VeloRec está vacía.' };
  }
  return { isValid: true };
}

// =========================================================================
// GESTIÓN DE BITRATE PERSONALIZADO Y PROTECCIÓN TÉRMICA ("NO AHOGAR EL TELÉFONO")
// =========================================================================

export const MIN_CUSTOM_BITRATE_MBPS = 1;
export const MAX_SAFE_PHONE_BITRATE_MBPS = 60;
export const MAX_EXTREME_HARDWARE_BITRATE_MBPS = 80;
export const RECOMMENDED_GAMING_BITRATE_MBPS = 12;

export const EXTENDED_BITRATE_PRESETS: number[] = [1, 2, 4, 8, 12, 16, 20, 24, 35, 50, 60];

export type BitrateTier =
  | 'ultra_saving'      // 1-3 Mbps: Ultra Ahorro de espacio y batería
  | 'safe_gaming'       // 4-16 Mbps: Zona Óptima para juegos (100% segura para cualquier teléfono)
  | 'high_quality'      // 17-30 Mbps: Alta Definición para celulares de gama media y alta
  | 'pro_quality'       // 31-50 Mbps: Calidad Profesional para edición y pantallas 2K
  | 'extreme_hardware'; // 51-80 Mbps: Límite extremo del codificador por hardware

export interface BitrateProfile {
  bitrateMbps: number;
  isValid: boolean;
  tier: BitrateTier;
  tierLabel: string;
  thermalSafety: 'ultra_cool' | 'optimal' | 'moderate' | 'heavy' | 'extreme_throttle_risk';
  advice: string;
  storageUsagePerMinuteMb: number;
  storageUsagePerHourMb: number;
  recommendedForPhone: boolean;
  warningMessage?: string;
}

/**
 * Evalúa un bitrate personalizado (desde 1 Mbps hasta 80 Mbps) y calcula el perfil
 * de seguridad térmica para garantizar que la codificación no sature ("ahogue")
 * los procesadores ni las memorias de smartphones modestos.
 */
export function getBitrateProfile(
  bitrateMbps: number,
  isEmmc: boolean = false
): BitrateProfile {
  const cleanMbps = Math.min(
    MAX_EXTREME_HARDWARE_BITRATE_MBPS,
    Math.max(MIN_CUSTOM_BITRATE_MBPS, Math.round(bitrateMbps))
  );

  const storageUsagePerMinuteMb = Number(((cleanMbps * 60) / 8).toFixed(1));
  const storageUsagePerHourMb = Number((storageUsagePerMinuteMb * 60).toFixed(0));

  if (cleanMbps <= 3) {
    return {
      bitrateMbps: cleanMbps,
      isValid: true,
      tier: 'ultra_saving',
      tierLabel: 'Ultra Ahorro',
      thermalSafety: 'ultra_cool',
      advice: 'Archivos diminutos (~7-22 MB/min). Cero calentamiento. Ideal para clases, tutoriales y horas de grabación.',
      storageUsagePerMinuteMb,
      storageUsagePerHourMb,
      recommendedForPhone: true
    };
  }

  if (cleanMbps <= 16) {
    return {
      bitrateMbps: cleanMbps,
      isValid: true,
      tier: 'safe_gaming',
      tierLabel: cleanMbps === 12 ? 'Zona Gamer Óptima ★' : 'Zona Segura Gamer',
      thermalSafety: 'optimal',
      advice: 'Excelente nitidez en videojuegos (Free Fire, Subway Surfers, COD). Fluidez total sin caídas de FPS ni sobrecalentamiento.',
      storageUsagePerMinuteMb,
      storageUsagePerHourMb,
      recommendedForPhone: true
    };
  }

  if (cleanMbps <= 30) {
    return {
      bitrateMbps: cleanMbps,
      isValid: true,
      tier: 'high_quality',
      tierLabel: 'Alta Calidad',
      thermalSafety: 'moderate',
      advice: 'Definición cristalina en movimientos rápidos. Recomendado para celulares de gama media con procesadores equilibrados.',
      storageUsagePerMinuteMb,
      storageUsagePerHourMb,
      recommendedForPhone: true
    };
  }

  if (cleanMbps <= 50) {
    const warning = isEmmc
      ? 'Aviso: En memorias eMMC puede generar contención de disco durante escenas intensas. Se recomienda Buffer Pacing.'
      : undefined;

    return {
      bitrateMbps: cleanMbps,
      isValid: true,
      tier: 'pro_quality',
      tierLabel: 'Calidad Profesional',
      thermalSafety: 'heavy',
      advice: 'Tasa de datos exigente. Destinado a pantallas 2K y exportaciones de alta gama.',
      storageUsagePerMinuteMb,
      storageUsagePerHourMb,
      recommendedForPhone: !isEmmc,
      warningMessage: warning
    };
  }

  // 51 a 80 Mbps: Límite extremo del codificador
  return {
    bitrateMbps: cleanMbps,
    isValid: true,
    tier: 'extreme_hardware',
    tierLabel: 'Límite Extremo de Hardware',
    thermalSafety: 'extreme_throttle_risk',
    advice: 'Tasa masiva (~375-600 MB/min). Puede sobrecalentar el SoC o provocar caídas de cuadros en teléfonos de gama baja.',
    storageUsagePerMinuteMb,
    storageUsagePerHourMb,
    recommendedForPhone: false,
    warningMessage: 'Precaución: Tasa muy alta. Puede sobrecalentar el dispositivo en grabaciones prolongadas.'
  };
}

/**
 * Garantiza que la selección de una nueva resolución NO altere ni sobrescriba
 * el bitrate personalizado que el usuario ha configurado voluntariamente.
 */
export function preserveBitrateOnResolutionChange(
  currentBitrateMbps: number,
  _newResolutionRecommendedBitrate: number
): number {
  // El bitrate seleccionado por el usuario siempre tiene prioridad absoluta
  return Math.min(
    MAX_EXTREME_HARDWARE_BITRATE_MBPS,
    Math.max(MIN_CUSTOM_BITRATE_MBPS, currentBitrateMbps)
  );
}

export interface RustIntegratedLibrarySpec {
  name: string;
  crate: string;
  version: string;
  category: 'audio' | 'color' | 'gpu';
  license: string;
  description: string;
  nativeModule: string;
  simdNeomSupported: boolean;
  zeroCopy: boolean;
}

/**
 * Retorna las especificaciones de las librerías de alto rendimiento integradas en el motor Rust 2024.
 */
export function getRustIntegratedLibraries(): RustIntegratedLibrarySpec[] {
  return [
    {
      name: 'Google Oboe (Rust)',
      crate: 'oboe',
      version: '0.6.1',
      category: 'audio',
      license: 'Apache-2.0',
      description: 'Audio de ultra baja latencia (<10 ms) con AAudio nativo y fallback OpenSL ES.',
      nativeModule: 'audio_oboe.rs',
      simdNeomSupported: false,
      zeroCopy: true
    },
    {
      name: 'YUV Utils SIMD',
      crate: 'yuvutils-rs',
      version: '0.7.0',
      category: 'color',
      license: 'MIT / Apache-2.0',
      description: 'Conversión ultrarrápida YUV420/NV12 a RGBA a más de 200 FPS con aceleración SIMD ARM NEON.',
      nativeModule: 'color_yuv.rs',
      simdNeomSupported: true,
      zeroCopy: true
    },
    {
      name: 'Glow (GL on Whatever)',
      crate: 'glow',
      version: '0.16.0',
      category: 'gpu',
      license: 'MIT / Apache-2.0',
      description: 'Tubería de sombreadores GPU OpenGL ES 3.20 multiplataforma para FaceCam y Gaming Boost.',
      nativeModule: 'gpu_glow.rs',
      simdNeomSupported: false,
      zeroCopy: true
    }
  ];
}

export interface StreamingComparisonItem {
  language: 'Rust' | 'Go';
  role: string;
  memoryFootprintMb: string;
  garbageCollectorPausas: string;
  thermalEfficiency: string;
  onDeviceClientVerdict: string;
  serverRelayVerdict: string;
}

/**
 * Retorna el análisis técnico comparativo entre Rust y Go para streaming móvil en el futuro.
 */
export function getStreamingEngineComparison(): {
  recommendationOnDevice: 'Rust';
  recommendationServer: 'Go';
  summary: string;
  items: StreamingComparisonItem[];
} {
  return {
    recommendationOnDevice: 'Rust',
    recommendationServer: 'Go',
    summary: 'Para el smartphone cliente donde corre el videojuego a 60/120 FPS, Rust es superior porque no tiene Garbage Collector y comparte memoria con el NDK. Para el servidor o relay en la nube, Go es excelente por su concurrencia ágil.',
    items: [
      {
        language: 'Rust',
        role: 'Cliente Móvil en el Smartphone (VeloRec)',
        memoryFootprintMb: '< 10 MB RSS',
        garbageCollectorPausas: 'CERO pausas (Memory safety estático sin GC)',
        thermalEfficiency: 'Máxima (Cero CPU extra en hilos de fondo)',
        onDeviceClientVerdict: 'RECOMENDADO (Inmune a LMK, comparte binario .so)',
        serverRelayVerdict: 'Muy bueno (alto rendimiento con Tokio/Axum)'
      },
      {
        language: 'Go',
        role: 'Servidor / Relay RTMP y SRT en la Nube',
        memoryFootprintMb: '25 - 40 MB RSS (Runtime Go)',
        garbageCollectorPausas: 'Micro-pausas de GC periódicas (posibles caídas de cuadros en juegos)',
        thermalEfficiency: 'Media (El runtime de Go mantiene hilos de monitoreo)',
        onDeviceClientVerdict: 'No óptimo en móviles (requiere empaquetar libgo.so adicional)',
        serverRelayVerdict: 'RECOMENDADO (Goroutines y ecosistema RTMP/WebRTC maduro)'
      }
    ]
  };
}

// =========================================================================
// MULTIPLEXACIÓN DE AUDIO Y VIDEO EN TIEMPO REAL 100% EN RUST (CERO ESPERA AL DETENER)
// =========================================================================

export interface RustMuxingReport {
  isRealTimeRustMuxing: boolean;
  fileSizeMb: number;
  stopLatencyMs: number;
  timeSavedMs: number;
  postProcessingRequired: boolean;
  tempFilesCreated: number;
  speedupDescription: string;
}

/**
 * Retorna las especificaciones técnicas del motor de multiplexación simultánea en Rust 2024.
 */
export function getRustRealTimeMuxingCapabilities(): {
  name: string;
  engine: 'Rust 2024';
  zeroDelayStop: boolean;
  videoTrackFormat: string;
  audioTrackFormat: string;
  threadSafeMuxerState: boolean;
  queueType: string;
  summary: string;
} {
  return {
    name: 'Multiplexación Nativa de Audio y Video en Tiempo Real',
    engine: 'Rust 2024',
    zeroDelayStop: true,
    videoTrackFormat: 'H.264 / HEVC / AV1 (AMediaCodec Surface Zero-Copy)',
    audioTrackFormat: 'AAC-LC (audio/mp4a-latm, 48000Hz, Estéreo)',
    threadSafeMuxerState: true,
    queueType: 'SharedMuxerState protegido con cerrojo atómico Rust y buffer de vaciado inmediato',
    summary: 'Registra las pistas de video y audio en AMediaMuxer antes del inicio de la captura y entrelaza paquetes en tiempo real. Al pulsar Detener, el MP4 ya está 100% completo, reduciendo la espera a menos de 15 ms.'
  };
}

/**
 * Estima la latencia al detener la captura comparando la multiplexación tradicional
 * en dos pasos (merge posterior en disco) frente a la multiplexación 100% en vivo de Rust 2024.
 */
export function estimateMuxingLatency(
  isRealTimeRustMuxing: boolean,
  fileSizeBytes: number
): RustMuxingReport {
  const sizeMb = Math.max(0, fileSizeBytes / (1024 * 1024));

  if (isRealTimeRustMuxing) {
    return {
      isRealTimeRustMuxing: true,
      fileSizeMb: Number(sizeMb.toFixed(2)),
      stopLatencyMs: 12, // Cierre directo de AMediaMuxer (< 15 ms)
      timeSavedMs: Math.max(0, Math.round(sizeMb * 18)), // Ahorro frente a extracción y reempaquetado
      postProcessingRequired: false,
      tempFilesCreated: 0,
      speedupDescription: `Cero espera al detener (< 15 ms). El video de ${sizeMb.toFixed(1)} MB ya tiene audio y video multiplexados al instante en Rust.`
    };
  }

  // Método tradicional de 2 pasos (MediaExtractor + MediaMuxer en disco)
  const legacyMergeMs = Math.round(Math.max(400, sizeMb * 18));
  return {
    isRealTimeRustMuxing: false,
    fileSizeMb: Number(sizeMb.toFixed(2)),
    stopLatencyMs: legacyMergeMs,
    timeSavedMs: 0,
    postProcessingRequired: true,
    tempFilesCreated: 2, // temp_video.mp4 y temp_audio.m4a
    speedupDescription: `Fusión secundaria en disco requerida (~${(legacyMergeMs / 1000).toFixed(2)}s de espera tras presionar Detener).`
  };
}
