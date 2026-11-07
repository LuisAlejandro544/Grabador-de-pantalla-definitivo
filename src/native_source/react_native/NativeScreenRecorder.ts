/**
 * @file NativeScreenRecorder.ts
 * @description Especificación de TurboModule / NativeModule para React Native.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Define la interfaz tipada mediante la cual el código JavaScript/TypeScript de React Native
 * se comunica de forma bidireccional con el módulo nativo en Kotlin (Android).
 * 
 * Permite:
 * 1. Configurar parámetros de grabación (resolución, fps, bitrate, audio).
 * 2. Seleccionar el motor nativo de codificación ('cpp23' o 'rust2024').
 * 3. Iniciar, pausar, reanudar y detener la captura de pantalla.
 * 4. Suscribirse a eventos nativos en tiempo real (tiempo transcurrido, fps reales, métricas de hardware).
 */

import { TurboModule, TurboModuleRegistry } from 'react-native';

export type NativeEngine = 'cpp23' | 'rust2024';

export interface NativeRecordConfig {
  engine: NativeEngine;           // 'cpp23' (C++23) o 'rust2024' (Rust Edition 2024)
  width: number;                  // Ancho en píxeles (ej: 1080)
  height: number;                 // Alto en píxeles (ej: 1920)
  fps: number;                    // Cuadros por segundo (ej: 60)
  bitrateBps: number;             // Bitrate en bits/segundo (ej: 12_000_000 para 12 Mbps)
  codec: 'H264' | 'HEVC' | 'AV1'; // Códec de video acelerado por hardware
  audioSource: 'internal' | 'mic' | 'both' | 'mute';
  sampleRate: number;             // 44100 o 48000 Hz
  outputPath?: string;            // Ruta opcional en almacenamiento interno/MediaStore
}

export interface NativeRecorderMetrics {
  durationMs: number;
  currentFps: number;
  cpuUsagePercent: number;
  memoryUsageMb: number;
  droppedFrames: number;
  totalFrames: number;
  zeroCopyEfficiency: number;
}

export interface Spec extends TurboModule {
  // Inicializa el servicio nativo y solicita permiso de MediaProjection al usuario
  prepareRecorder(config: NativeRecordConfig): Promise<boolean>;

  // Inicia la captura de pantalla y el pipeline nativo (C++23 o Rust)
  startRecording(): Promise<string>; // Retorna el filePath de destino

  // Pausa la codificación sin cerrar el archivo muxer
  pauseRecording(): Promise<boolean>;

  // Reanuda la codificación
  resumeRecording(): Promise<boolean>;

  // Detiene la grabación, finaliza el contenedor MP4/MKV y libera recursos de hardware
  stopRecording(): Promise<{ filePath: string; durationMs: number; fileSizeBytes: number }>;

  // Obtiene el estado actual del motor nativo
  getEngineStatus(): Promise<{ engine: NativeEngine; isRecording: boolean; isPaused: boolean }>;

  // Consulta las métricas de rendimiento en tiempo real
  getLiveMetrics(): Promise<NativeRecorderMetrics>;
}

export default TurboModuleRegistry.get<Spec>('ScreenRecorderModule');
