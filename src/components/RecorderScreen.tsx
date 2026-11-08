import React, { useRef, useState, useEffect, useCallback } from 'react';
import {
  Play,
  Pause,
  Square,
  Camera,
  Mic,
  MicOff,
  Sparkles,
  Zap,
  Gauge,
  Layers,
  Settings2,
  CheckCircle2,
  Info,
  Maximize2,
  Timer
} from 'lucide-react';
import { RecorderSettings, RecordedVideoItem, NativeEngineType } from '../types/recorder';
import { formatDuration, calculateEngineMetrics, formatBytes, RESOLUTION_PRESETS, COUNTDOWN_PRESETS } from '../utils/recorderCore';

interface RecorderScreenProps {
  settings: RecorderSettings;
  onUpdateSettings: (newSettings: Partial<RecorderSettings>) => void;
  isRecording: boolean;
  isPaused: boolean;
  durationSec: number;
  onStartRecording: (stream?: MediaStream) => Promise<void>;
  onPauseRecording: () => void;
  onResumeRecording: () => void;
  onStopRecording: () => Promise<RecordedVideoItem | null>;
  onGoToSettings: () => void;
}

export const RecorderScreen: React.FC<RecorderScreenProps> = ({
  settings,
  onUpdateSettings,
  isRecording,
  isPaused,
  durationSec,
  onStartRecording,
  onPauseRecording,
  onResumeRecording,
  onStopRecording,
  onGoToSettings
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const animationFrameId = useRef<number | null>(null);

  // Estado del FaceCam (Cámara frontal flotante)
  const [faceCamActive, setFaceCamActive] = useState<boolean>(settings.enableFaceCam);
  const [faceCamStream, setFaceCamStream] = useState<MediaStream | null>(null);
  const faceCamVideoRef = useRef<HTMLVideoElement | null>(null);

  // Simulación de toques visuales (Touch Pointers)
  const [touchPointers, setTouchPointers] = useState<{ x: number; y: number; id: number }[]>([]);

  // Notificación de captura guardada
  const [savedNotification, setSavedNotification] = useState<string | null>(null);

  // Audio VU Meter animado
  const [audioLevel, setAudioLevel] = useState<number>(35);

  // Estado de cuenta regresiva previa a la captura
  const [isCountingDown, setIsCountingDown] = useState<boolean>(false);
  const [countdownRemaining, setCountdownRemaining] = useState<number>(settings.countdownSec ?? 3);
  const countdownTimerRef = useRef<number | null>(null);

  // Métricas en vivo del motor nativo
  const liveMetrics = calculateEngineMetrics(
    settings.engine,
    settings.fps,
    settings.bitrateMbps,
    durationSec
  );

  // Manejo de la cámara frontal (FaceCam)
  useEffect(() => {
    if (faceCamActive) {
      navigator.mediaDevices?.getUserMedia({ video: { facingMode: 'user', width: 320, height: 240 } })
        .then((stream) => {
          setFaceCamStream(stream);
          if (faceCamVideoRef.current) {
            faceCamVideoRef.current.srcObject = stream;
          }
        })
        .catch(() => {
          // Si el navegador bloquea la cámara real, se mantiene el avatar animado de fallback
        });
    } else {
      if (faceCamStream) {
        faceCamStream.getTracks().forEach((track) => track.stop());
        setFaceCamStream(null);
      }
    }
    return () => {
      if (faceCamStream) {
        faceCamStream.getTracks().forEach((track) => track.stop());
      }
    };
  }, [faceCamActive]);

  // Canvas interactivo que simula la pantalla del móvil a los FPS configurados
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let tick = 0;
    const render = () => {
      tick++;
      const w = canvas.width;
      const h = canvas.height;

      // Fondo dinámico con gradiente de alta tecnología
      const bgGradient = ctx.createLinearGradient(0, 0, w, h);
      bgGradient.addColorStop(0, '#0a0f1d');
      bgGradient.addColorStop(0.5, '#0f172a');
      bgGradient.addColorStop(1, '#020617');
      ctx.fillStyle = bgGradient;
      ctx.fillRect(0, 0, w, h);

      // Cuadrícula futurista sutil
      ctx.strokeStyle = 'rgba(99, 102, 241, 0.07)';
      ctx.lineWidth = 1;
      const gridSize = 32;
      for (let x = 0; x < w; x += gridSize) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, h);
        ctx.stroke();
      }
      for (let y = 0; y < h; y += gridSize) {
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(w, y);
        ctx.stroke();
      }

      // Indicador de estado del monitor de hardware nativo
      ctx.fillStyle = 'rgba(15, 23, 42, 0.4)';
      ctx.fillRect(0, 0, w, 24);
      ctx.fillStyle = settings.engine === 'rust2024' ? '#f59e0b' : '#06b6d4';
      ctx.font = 'bold 9px "JetBrains Mono", monospace';
      ctx.fillText('● SUPERFICIE NATIVA ACTIVA (GPU)', 14, 16);
      ctx.fillStyle = '#94a3b8';
      ctx.font = '9px "JetBrains Mono", monospace';
      ctx.fillText(`${settings.fps} FPS`, w - 50, 16);

      // Círculo central pulsante que representa actividad en pantalla
      const cx = w / 2;
      const cy = h / 2 - 20;
      const pulse = Math.sin(tick * 0.05) * 15;

      const radialGrad = ctx.createRadialGradient(cx, cy, 10, cx, cy, 120 + pulse);
      if (settings.engine === 'rust2024') {
        radialGrad.addColorStop(0, 'rgba(245, 158, 11, 0.35)');
        radialGrad.addColorStop(1, 'rgba(245, 158, 11, 0)');
      } else {
        radialGrad.addColorStop(0, 'rgba(6, 182, 212, 0.35)');
        radialGrad.addColorStop(1, 'rgba(6, 182, 212, 0)');
      }
      ctx.fillStyle = radialGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, 120 + pulse, 0, Math.PI * 2);
      ctx.fill();

      // Gráfico de ondas simuladas en el centro (Game/App Simulation)
      ctx.strokeStyle = settings.engine === 'rust2024' ? '#f59e0b' : '#06b6d4';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      for (let x = 30; x < w - 30; x += 5) {
        const wave = Math.sin((x + tick * 4) * 0.04) * (20 + (isRecording ? 15 : 5));
        const y = cy + wave;
        if (x === 30) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      // Texto de motor nativo activo en el canvas
      ctx.fillStyle = '#f8fafc';
      ctx.font = 'bold 14px "JetBrains Mono", monospace';
      ctx.textAlign = 'center';
      const engineText = settings.engine === 'rust2024' ? 'MOTOR: RUST 2024 (ZERO-COPY)' : 'MOTOR: C++23 (AMEDIACODEC)';
      ctx.fillText(engineText, cx, cy + 60);

      ctx.fillStyle = '#64748b';
      ctx.font = '11px sans-serif';
      ctx.fillText(`${settings.resolution.name} • ${settings.fps} FPS • ${settings.bitrateMbps} Mbps`, cx, cy + 80);

      // Si está grabando, mostrar cuadro rojo de captura
      if (isRecording) {
        ctx.strokeStyle = 'rgba(239, 68, 68, 0.85)';
        ctx.lineWidth = 3;
        ctx.strokeRect(4, 4, w - 8, h - 8);

        ctx.fillStyle = '#ef4444';
        ctx.beginPath();
        ctx.arc(20, 48, 5, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#fca5a5';
        ctx.font = 'bold 11px monospace';
        ctx.textAlign = 'left';
        ctx.fillText(formatDuration(durationSec), 32, 52);
      }

      if (document.hidden) {
        // En segundo plano o fuera de foco, requestAnimationFrame se detiene.
        // Usamos setInterval para continuar alimentando el stream de MediaRecorder.
      } else {
        animationFrameId.current = requestAnimationFrame(render);
      }
    };

    let bgInterval: number | null = null;
    const handleVisibilityChange = () => {
      if (document.hidden) {
        if (animationFrameId.current) {
          cancelAnimationFrame(animationFrameId.current);
          animationFrameId.current = null;
        }
        if (!bgInterval) {
          bgInterval = window.setInterval(() => {
            render();
          }, 1000 / Math.min(settings.fps, 30));
        }
      } else {
        if (bgInterval) {
          clearInterval(bgInterval);
          bgInterval = null;
        }
        if (!animationFrameId.current) {
          animationFrameId.current = requestAnimationFrame(render);
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    render();

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      if (bgInterval) {
        clearInterval(bgInterval);
      }
      if (animationFrameId.current) {
        cancelAnimationFrame(animationFrameId.current);
      }
    };
  }, [settings, isRecording, durationSec]);

  // Simulación de fluctuación de audio para el medidor VU
  useEffect(() => {
    if (!isRecording || isPaused || settings.audioSource === 'mute') {
      setAudioLevel(0);
      return;
    }
    const interval = setInterval(() => {
      setAudioLevel(Math.floor(20 + Math.random() * 65));
    }, 150);
    return () => clearInterval(interval);
  }, [isRecording, isPaused, settings.audioSource]);

  // Manejo de toques en el canvas para simular punteros de toque
  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!settings.showTouchPointers) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const id = Date.now();
    setTouchPointers((prev) => [...prev, { x, y, id }]);
    setTimeout(() => {
      setTouchPointers((prev) => prev.filter((p) => p.id !== id));
    }, 600);
  };

  // Iniciar con selector de pantalla nativo si el usuario lo solicita
  const handleStartCaptureWithRealDisplay = useCallback(async () => {
    try {
      if (navigator.mediaDevices && 'getDisplayMedia' in navigator.mediaDevices) {
        const stream = await navigator.mediaDevices.getDisplayMedia({
          video: {
            width: { ideal: settings.resolution.width },
            height: { ideal: settings.resolution.height },
            frameRate: { ideal: settings.fps }
          },
          audio: settings.audioSource !== 'mute'
        });
        await onStartRecording(stream);
      } else {
        await onStartRecording();
      }
    } catch {
      // Si el usuario canceló el selector de pantalla o está en móvil restringido, grabamos el canvas en vivo
      await onStartRecording();
    }
  }, [settings, onStartRecording]);

  // Manejador del botón que evalúa si hay cuenta regresiva
  const handleInitiateRecording = async () => {
    const delaySec = settings.countdownSec ?? 3;
    if (delaySec > 0) {
      setCountdownRemaining(delaySec);
      setIsCountingDown(true);
    } else {
      await handleStartCaptureWithRealDisplay();
    }
  };

  // Temporizador de cuenta regresiva
  useEffect(() => {
    if (isCountingDown) {
      if (countdownRemaining > 0) {
        countdownTimerRef.current = window.setTimeout(() => {
          setCountdownRemaining((prev) => prev - 1);
        }, 1000);
      } else {
        setIsCountingDown(false);
        handleStartCaptureWithRealDisplay();
      }
    }
    return () => {
      if (countdownTimerRef.current) {
        clearTimeout(countdownTimerRef.current);
      }
    };
  }, [isCountingDown, countdownRemaining, handleStartCaptureWithRealDisplay]);

  const handleStopAndSave = async () => {
    const item = await onStopRecording();
    if (item) {
      setSavedNotification(`¡Grabación guardada! (${formatBytes(item.sizeBytes)})`);
      setTimeout(() => setSavedNotification(null), 4000);
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-60px)] pb-24 px-3 pt-3 max-w-lg mx-auto w-full relative">
      {/* Modal / Overlay de Cuenta Regresiva */}
      {isCountingDown && (
        <div className="fixed inset-0 z-50 bg-slate-950/85 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-cyan-500/60 rounded-3xl p-6 max-w-xs w-full text-center shadow-2xl flex flex-col items-center">
            <div className="w-20 h-20 rounded-full border-4 border-cyan-400 bg-cyan-950/40 flex items-center justify-center mb-4 shadow-lg shadow-cyan-500/20">
              <span className="text-4xl font-mono font-black text-cyan-300">
                {countdownRemaining > 0 ? countdownRemaining : '¡YA!'}
              </span>
            </div>
            <h3 className="text-base font-bold text-white mb-1">Iniciando Grabación...</h3>
            <p className="text-xs text-slate-400 mb-5">
              Prepárate o ve a la aplicación que deseas capturar.
            </p>
            <button
              type="button"
              onClick={() => {
                setIsCountingDown(false);
                if (countdownTimerRef.current) clearTimeout(countdownTimerRef.current);
              }}
              className="w-full py-2.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-rose-400 font-bold text-xs border border-slate-700 active:scale-95 transition-all"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      {/* Notificación flotante de guardado */}
      {savedNotification && (
        <div className="mb-3 bg-emerald-950/90 border border-emerald-500/60 text-emerald-200 px-4 py-2.5 rounded-xl text-xs flex items-center space-x-2 shadow-lg animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="font-medium">{savedNotification}</span>
        </div>
      )}

      {/* Tarjeta Nativa Material 3: Configuración Rápida de Grabación */}
      <div className="mb-3 w-full bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800 shadow-md">
        <div className="flex items-center justify-between mb-2.5">
          <div className="flex items-center space-x-1.5">
            <Settings2 className="w-4 h-4 text-cyan-400" />
            <span className="text-xs font-bold text-white tracking-wide">Ajustes Rápidos de Calidad</span>
          </div>
          <span className="text-[11px] font-mono font-bold px-2 py-0.5 rounded-md bg-slate-800 text-cyan-300 border border-indigo-500/30">
            {settings.resolution.id.toUpperCase()} • {settings.bitrateMbps}M{settings.countdownSec ? ` • ${settings.countdownSec}s` : ' • 0s'}
          </span>
        </div>

        {/* Fila 1: Resolución con Chips táctiles */}
        <div className="mb-2.5">
          <div className="text-[11px] font-semibold text-slate-400 mb-1.5 flex items-center space-x-1">
            <span>Resolución de Pantalla</span>
          </div>
          <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
            {RESOLUTION_PRESETS.map((preset) => {
              const isSelected = settings.resolution.id === preset.id;
              return (
                <button
                  key={preset.id}
                  type="button"
                  disabled={isRecording}
                  onClick={() => onUpdateSettings({ resolution: preset, bitrateMbps: preset.recommendedBitrateMbps })}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold shrink-0 transition-all ${
                    isSelected
                      ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/40 border border-indigo-400'
                      : 'bg-slate-800/80 text-slate-300 border border-slate-700/80 hover:bg-slate-750'
                  }`}
                >
                  {preset.name.split(' ')[0]} ({preset.id.toUpperCase()})
                </button>
              );
            })}
          </div>
        </div>

        {/* Fila 2: Tasa de Bits (Bitrate) */}
        <div className="mb-2.5">
          <div className="text-[11px] font-semibold text-slate-400 mb-1.5 flex items-center space-x-1">
            <span>Tasa de Bits (Bitrate)</span>
          </div>
          <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
            {[4, 8, 12, 16, 24, 35, 50].map((mbps) => {
              const isSelected = settings.bitrateMbps === mbps;
              const isIdeal = mbps === 12;
              return (
                <button
                  key={mbps}
                  type="button"
                  disabled={isRecording}
                  onClick={() => onUpdateSettings({ bitrateMbps: mbps })}
                  className={`px-2.5 py-1.5 rounded-xl text-xs font-semibold shrink-0 font-mono transition-all ${
                    isSelected
                      ? 'bg-cyan-500 text-slate-950 font-bold shadow-md shadow-cyan-500/40 border border-cyan-300'
                      : 'bg-slate-800/80 text-slate-300 border border-slate-700/80 hover:bg-slate-750'
                  }`}
                >
                  {isIdeal ? `${mbps} Mbps ★` : `${mbps} Mbps`}
                </button>
              );
            })}
          </div>
        </div>

        {/* Fila 3: Cuenta Regresiva de Inicio */}
        <div>
          <div className="text-[11px] font-semibold text-slate-400 mb-1.5 flex items-center space-x-1">
            <Timer className="w-3.5 h-3.5 text-slate-400" />
            <span>Cuenta Regresiva de Inicio</span>
          </div>
          <div className="flex items-center space-x-2 overflow-x-auto pb-1 scrollbar-none">
            {COUNTDOWN_PRESETS.map((sec) => {
              const currentSec = settings.countdownSec ?? 3;
              const isSelected = currentSec === sec;
              const label = sec === 0 ? '0s (Inmediata)' : sec === 3 ? '3s (Ideal ★)' : `${sec}s`;
              return (
                <button
                  key={sec}
                  type="button"
                  disabled={isRecording}
                  onClick={() => onUpdateSettings({ countdownSec: sec })}
                  className={`px-2.5 py-1.5 rounded-xl text-xs font-semibold shrink-0 transition-all ${
                    isSelected
                      ? 'bg-indigo-600 text-white font-bold shadow-md shadow-indigo-600/40 border border-indigo-400'
                      : 'bg-slate-800/80 text-slate-300 border border-slate-700/80 hover:bg-slate-750'
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Visor de Grabación Móvil (Viewport) */}
      <div className="relative w-full aspect-[9/13] max-h-[460px] bg-slate-950 rounded-2xl overflow-hidden border border-slate-800 shadow-2xl flex flex-col justify-between">
        {/* Canvas de captura */}
        <canvas
          ref={canvasRef}
          width={360}
          height={520}
          onClick={handleCanvasClick}
          className="absolute inset-0 w-full h-full object-cover cursor-crosshair select-none"
        />

        {/* Efectos de toque interactivo (Touch Pointers) */}
        {touchPointers.map((p) => (
          <div
            key={p.id}
            style={{ left: p.x - 18, top: p.y - 18 }}
            className="absolute w-9 h-9 rounded-full bg-indigo-500/40 border-2 border-indigo-300 pointer-events-none animate-ping"
          />
        ))}

        {/* Cámara frontal flotante (FaceCam Bubble) */}
        {faceCamActive && (
          <div className="absolute top-4 right-4 z-20 w-24 h-24 rounded-full overflow-hidden border-2 border-indigo-400 shadow-xl bg-slate-800 flex items-center justify-center">
            {faceCamStream ? (
              <video
                ref={faceCamVideoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover transform -scale-x-100"
              />
            ) : (
              <div className="flex flex-col items-center justify-center text-slate-300">
                <Camera className="w-5 h-5 text-indigo-400 mb-1" />
                <span className="text-[9px] font-semibold">FaceCam</span>
              </div>
            )}
          </div>
        )}

        {/* Barra de HUD superior con accesos rápidos */}
        <div className="relative z-10 p-3 flex items-center justify-between pointer-events-auto bg-gradient-to-b from-slate-950/80 to-transparent">
          {/* Selector rápido de Motor */}
          <button
            type="button"
            onClick={() => {
              const nextEngine: NativeEngineType = settings.engine === 'cpp23' ? 'rust2024' : 'cpp23';
              onUpdateSettings({ engine: nextEngine });
            }}
            className={`flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-mono font-bold backdrop-blur-md border shadow transition-all ${
              settings.engine === 'rust2024'
                ? 'bg-amber-950/80 text-amber-300 border-amber-600/70 hover:bg-amber-900/80'
                : 'bg-cyan-950/80 text-cyan-300 border-cyan-500/70 hover:bg-cyan-900/80'
            }`}
            title="Toca para alternar entre C++23 y Rust 2024"
            aria-label="Cambiar motor nativo"
          >
            <span>{settings.engine === 'rust2024' ? '🦀 Rust 2024' : '⚡ C++23'}</span>
            <span className="text-[9px] text-slate-400 font-sans">⇄</span>
          </button>

          {/* Selector rápido de Resolución y FPS */}
          <button
            type="button"
            onClick={onGoToSettings}
            className="flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-900/80 text-slate-200 border border-slate-700/80 backdrop-blur-md hover:bg-slate-800"
            aria-label="Abrir configuración de resolución"
          >
            <span>{settings.resolution.id}</span>
            <span className="text-slate-400">•</span>
            <span>{settings.fps} FPS</span>
            <Settings2 className="w-3.5 h-3.5 ml-0.5 text-indigo-400" />
          </button>
        </div>

        {/* Telemetría inferior dentro del frame */}
        <div className="relative z-10 p-3 bg-gradient-to-t from-slate-950/90 via-slate-950/60 to-transparent flex flex-col space-y-2 pointer-events-auto">
          {/* Medidor VU de Audio */}
          <div className="flex items-center space-x-2 bg-slate-900/70 backdrop-blur-md px-2.5 py-1.5 rounded-lg border border-slate-800/80">
            {settings.audioSource === 'mute' ? (
              <MicOff className="w-3.5 h-3.5 text-slate-500" />
            ) : (
              <Mic className="w-3.5 h-3.5 text-emerald-400 animate-pulse" />
            )}
            <div className="flex-1 h-1.5 bg-slate-800 rounded-full overflow-hidden flex">
              <div
                style={{ width: `${audioLevel}%` }}
                className={`h-full transition-all duration-150 rounded-full ${
                  audioLevel > 75 ? 'bg-rose-500' : audioLevel > 50 ? 'bg-amber-400' : 'bg-emerald-400'
                }`}
              />
            </div>
            <span className="text-[10px] font-mono text-slate-400 min-w-8 text-right">
              {settings.audioSource === 'mute' ? 'MUTE' : `${audioLevel}dB`}
            </span>
          </div>

          {/* Métricas del Hardware en vivo */}
          <div className="grid grid-cols-4 gap-1.5 text-center">
            <div className="bg-slate-900/80 backdrop-blur-md px-1.5 py-1 rounded-md border border-slate-800">
              <span className="block text-[9px] text-slate-400 uppercase font-semibold">CPU</span>
              <span className="text-xs font-mono font-bold text-slate-200">{liveMetrics.cpuUsagePercent}%</span>
            </div>
            <div className="bg-slate-900/80 backdrop-blur-md px-1.5 py-1 rounded-md border border-slate-800">
              <span className="block text-[9px] text-slate-400 uppercase font-semibold">Latencia</span>
              <span className="text-xs font-mono font-bold text-slate-200">{liveMetrics.frameEncodingLatencyMs}ms</span>
            </div>
            <div className="bg-slate-900/80 backdrop-blur-md px-1.5 py-1 rounded-md border border-slate-800">
              <span className="block text-[9px] text-slate-400 uppercase font-semibold">Memoria</span>
              <span className="text-xs font-mono font-bold text-slate-200">{liveMetrics.memoryUsageMb}MB</span>
            </div>
            <div className="bg-slate-900/80 backdrop-blur-md px-1.5 py-1 rounded-md border border-slate-800">
              <span className="block text-[9px] text-slate-400 uppercase font-semibold">Zero-Copy</span>
              <span className="text-xs font-mono font-bold text-emerald-400">{liveMetrics.zeroCopyEfficiency}%</span>
            </div>
          </div>
        </div>
      </div>

      {/* Controles de Acción Principal (Start / Pause / Stop) */}
      <div className="mt-4 flex flex-col space-y-3">
        {/* Botones de acción rápida para overlays (FaceCam, Touch, Mic) */}
        <div className="flex items-center justify-between px-1">
          <button
            type="button"
            onClick={() => {
              const nextState = !faceCamActive;
              setFaceCamActive(nextState);
              onUpdateSettings({ enableFaceCam: nextState });
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-colors ${
              faceCamActive
                ? 'bg-indigo-950/80 text-indigo-300 border-indigo-500/50'
                : 'bg-slate-900/80 text-slate-400 border-slate-800 hover:text-slate-200'
            }`}
            aria-label="Alternar cámara flotante FaceCam"
          >
            <Camera className="w-3.5 h-3.5" />
            <span>FaceCam {faceCamActive ? 'ON' : 'OFF'}</span>
          </button>

          <button
            type="button"
            onClick={() => onUpdateSettings({ showTouchPointers: !settings.showTouchPointers })}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-colors ${
              settings.showTouchPointers
                ? 'bg-indigo-950/80 text-indigo-300 border-indigo-500/50'
                : 'bg-slate-900/80 text-slate-400 border-slate-800 hover:text-slate-200'
            }`}
            aria-label="Alternar visualizador de toques en pantalla"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Toques {settings.showTouchPointers ? 'ON' : 'OFF'}</span>
          </button>

          <button
            type="button"
            onClick={() => {
              const nextSource = settings.audioSource === 'mute' ? 'mic' : 'mute';
              onUpdateSettings({ audioSource: nextSource });
            }}
            className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-medium border transition-colors ${
              settings.audioSource !== 'mute'
                ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/50'
                : 'bg-slate-900/80 text-slate-400 border-slate-800'
            }`}
            aria-label="Alternar audio o silencio"
          >
            {settings.audioSource === 'mute' ? <MicOff className="w-3.5 h-3.5" /> : <Mic className="w-3.5 h-3.5" />}
            <span>{settings.audioSource === 'mute' ? 'Silencio' : 'Audio ON'}</span>
          </button>
        </div>

        {/* Panel de Botones Principales de Grabación */}
        <div className="bg-slate-900/90 border border-slate-800 p-3 rounded-2xl flex items-center justify-around shadow-xl">
          {!isRecording ? (
            /* Botón de Iniciar Grabación */
            <div className="flex items-center space-x-3 w-full">
              <button
                type="button"
                onClick={handleInitiateRecording}
                className="flex-1 bg-gradient-to-r from-rose-600 via-rose-500 to-red-600 hover:from-rose-500 hover:to-red-500 text-white font-bold py-3.5 px-6 rounded-xl flex items-center justify-center space-x-2.5 shadow-lg shadow-rose-600/30 active:scale-95 transition-transform"
                aria-label="Iniciar grabación de pantalla"
              >
                <div className="w-3.5 h-3.5 rounded-full bg-white animate-ping" />
                <span className="text-sm tracking-wide">INICIAR GRABACIÓN</span>
              </button>
            </div>
          ) : (
            /* Botones durante grabación activa (Pausar / Reanudar y Detener) */
            <div className="flex items-center justify-between w-full space-x-3">
              {isPaused ? (
                <button
                  type="button"
                  onClick={onResumeRecording}
                  className="flex-1 bg-amber-600 hover:bg-amber-500 text-white font-bold py-3.5 px-4 rounded-xl flex items-center justify-center space-x-2 active:scale-95 transition-transform"
                  aria-label="Reanudar grabación"
                >
                  <Play className="w-5 h-5 fill-current" />
                  <span className="text-sm">REANUDAR</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={onPauseRecording}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-100 font-bold py-3.5 px-4 rounded-xl border border-slate-700 flex items-center justify-center space-x-2 active:scale-95 transition-transform"
                  aria-label="Pausar grabación"
                >
                  <Pause className="w-5 h-5 fill-current" />
                  <span className="text-sm">PAUSAR</span>
                </button>
              )}

              <button
                type="button"
                onClick={handleStopAndSave}
                className="flex-1 bg-rose-600 hover:bg-rose-500 text-white font-bold py-3.5 px-4 rounded-xl flex items-center justify-center space-x-2 shadow-lg shadow-rose-600/30 active:scale-95 transition-transform"
                aria-label="Detener y guardar video"
              >
                <Square className="w-5 h-5 fill-current" />
                <span className="text-sm">DETENER</span>
              </button>
            </div>
          )}
        </div>

        {/* Tarjeta Informativa sobre el Motor Nativo Seleccionado */}
        <div className="bg-slate-900/60 border border-slate-800/80 rounded-xl p-3 flex items-start space-x-2.5">
          <Info className="w-4 h-4 text-indigo-400 mt-0.5 shrink-0" />
          <div className="text-xs text-slate-400 leading-relaxed">
            <span className="font-semibold text-slate-200">
              {settings.engine === 'rust2024' ? 'Motor Rust (Edition 2024)' : 'Motor C++23 (AMediaCodec NDK)'}:
            </span>{' '}
            {settings.engine === 'rust2024'
              ? 'Canales atómicos lock-free con crossbeam-channel y validación estricta de memoria sin GC.'
              : 'Superficie de hardware directa ANativeWindow con std::span, std::expected y co_await.'}
          </div>
        </div>
      </div>
    </div>
  );
};
