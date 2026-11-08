import React, { useState } from 'react';
import {
  Cpu,
  Tv,
  Film,
  Mic,
  Sliders,
  Check,
  AlertCircle,
  HardDrive,
  ShieldCheck,
  Zap,
  HelpCircle,
  Timer
} from 'lucide-react';
import {
  RecorderSettings,
  ResolutionConfig,
  NativeEngineType,
  VideoCodec,
  AudioSourceType,
  SupportedAbi
} from '../types/recorder';
import {
  RESOLUTION_PRESETS,
  COUNTDOWN_PRESETS,
  calculateEstimatedFileSize,
  formatBytes,
  validateResolution
} from '../utils/recorderCore';

interface SettingsScreenProps {
  settings: RecorderSettings;
  onUpdateSettings: (newSettings: Partial<RecorderSettings>) => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  onUpdateSettings
}) => {
  const [customMode, setCustomMode] = useState<boolean>(false);
  const [customWidth, setCustomWidth] = useState<number>(settings.customWidth || 1080);
  const [customHeight, setCustomHeight] = useState<number>(settings.customHeight || 1920);
  const [customError, setCustomError] = useState<string | null>(null);

  // Estimación de peso para 10 minutos
  const estimatedSize10Min = calculateEstimatedFileSize(600, settings.bitrateMbps, settings.audioBitrateKbps);

  const handleSelectPreset = (preset: ResolutionConfig) => {
    setCustomMode(false);
    onUpdateSettings({
      resolution: preset,
      bitrateMbps: preset.recommendedBitrateMbps
    });
  };

  const handleApplyCustomResolution = () => {
    const check = validateResolution(customWidth, customHeight);
    if (!check.valid) {
      setCustomError(check.reason || 'Resolución no válida.');
      return;
    }
    setCustomError(null);
    const customConfig: ResolutionConfig = {
      id: 'custom',
      name: `Personalizada (${customWidth}x${customHeight})`,
      width: customWidth,
      height: customHeight,
      aspectRatio: `${customWidth}:${customHeight}`,
      recommendedBitrateMbps: Math.round((customWidth * customHeight * settings.fps * 0.1) / 1_000_000),
      category: customWidth >= 2160 ? '4K' : customWidth >= 1440 ? '2K' : 'FHD'
    };
    onUpdateSettings({
      resolution: customConfig,
      customWidth,
      customHeight
    });
  };

  const toggleAbi = (abi: SupportedAbi) => {
    const current = [...settings.targetAbis];
    if (current.includes(abi)) {
      if (current.length === 1) return; // Mantener al menos una arquitectura
      onUpdateSettings({ targetAbis: current.filter((a) => a !== abi) });
    } else {
      onUpdateSettings({ targetAbis: [...current, abi] });
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-60px)] pb-28 px-4 pt-4 max-w-lg mx-auto w-full space-y-6">
      {/* Título de Sección */}
      <div className="border-b border-slate-800 pb-3">
        <h2 className="text-lg font-bold text-slate-100 flex items-center space-x-2">
          <Sliders className="w-5 h-5 text-indigo-400" />
          <span>Configuración de Grabación</span>
        </h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Ajustes del motor nativo, resolución, bitrate y códecs acelerados por hardware.
        </p>
      </div>

      {/* SECCIÓN 1: SELECCIÓN DEL MOTOR NATIVO (C++23 vs Rust 2024) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl">
        <div className="flex items-center justify-between mb-3">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <Cpu className="w-4 h-4 text-indigo-400" />
            <span>Motor de Codificación Nativo</span>
          </label>
          <span className="text-[11px] text-slate-400">JNI / NDK</span>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {/* Opción C++23 */}
          <button
            type="button"
            onClick={() => onUpdateSettings({ engine: 'cpp23' })}
            className={`p-3.5 rounded-xl border text-left transition-all relative ${
              settings.engine === 'cpp23'
                ? 'bg-cyan-950/40 border-cyan-500 shadow-md shadow-cyan-500/20 ring-1 ring-cyan-500'
                : 'bg-slate-800/60 border-slate-700/80 hover:border-slate-600'
            }`}
          >
            {settings.engine === 'cpp23' && (
              <span className="absolute top-2 right-2 w-4 h-4 rounded-full bg-cyan-500 text-slate-950 flex items-center justify-center">
                <Check className="w-2.5 h-2.5 stroke-[3]" />
              </span>
            )}
            <div className="text-sm font-bold text-cyan-300 font-mono">⚡ C++23</div>
            <p className="text-[11px] text-slate-300 mt-1 font-medium">AMediaCodec NDK</p>
            <p className="text-[10px] text-slate-400 mt-1 leading-snug">
              std::span, std::expected, co_await y ANativeWindow directa.
            </p>
          </button>

          {/* Opción Rust 2024 */}
          <button
            type="button"
            onClick={() => onUpdateSettings({ engine: 'rust2024' })}
            className={`p-3.5 rounded-xl border text-left transition-all relative ${
              settings.engine === 'rust2024'
                ? 'bg-amber-950/40 border-amber-500 shadow-md shadow-amber-500/20 ring-1 ring-amber-500'
                : 'bg-slate-800/60 border-slate-700/80 hover:border-slate-600'
            }`}
          >
            {settings.engine === 'rust2024' && (
              <span className="absolute top-2 right-2 w-4 h-4 rounded-full bg-amber-500 text-slate-950 flex items-center justify-center">
                <Check className="w-2.5 h-2.5 stroke-[3]" />
              </span>
            )}
            <div className="text-sm font-bold text-amber-300 font-mono">🦀 Rust 2024</div>
            <p className="text-[11px] text-slate-300 mt-1 font-medium">Edición 2024 (1.85+)</p>
            <p className="text-[10px] text-slate-400 mt-1 leading-snug">
              Zero-cost safety, crossbeam-channel y ausencia total de data races.
            </p>
          </button>
        </div>
      </div>

      {/* SECCIÓN 2: RESOLUCIÓN DE VIDEO */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <Tv className="w-4 h-4 text-indigo-400" />
            <span>Resolución de Video</span>
          </label>
          <span className="text-[11px] font-mono text-indigo-300 font-semibold">
            {settings.resolution.width}x{settings.resolution.height}
          </span>
        </div>

        {/* Presets en Grid */}
        <div className="grid grid-cols-1 gap-2">
          {RESOLUTION_PRESETS.map((preset) => {
            const isSelected = !customMode && settings.resolution.id === preset.id;
            return (
              <button
                key={preset.id}
                type="button"
                onClick={() => handleSelectPreset(preset)}
                className={`w-full p-2.5 rounded-xl border flex items-center justify-between text-left transition-all ${
                  isSelected
                    ? 'bg-indigo-950/50 border-indigo-500 ring-1 ring-indigo-500 shadow-sm'
                    : 'bg-slate-800/50 border-slate-700/70 hover:bg-slate-800'
                }`}
              >
                <div>
                  <div className="text-xs font-bold text-slate-100">{preset.name}</div>
                  <div className="text-[10px] text-slate-400">
                    {preset.width}x{preset.height} px • Bitrate recomendado: {preset.recommendedBitrateMbps} Mbps
                  </div>
                </div>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                    isSelected ? 'bg-indigo-600 text-white' : 'bg-slate-700 text-slate-300'
                  }`}
                >
                  {preset.category}
                </span>
              </button>
            );
          })}
        </div>

        {/* Opción Personalizada */}
        <div className="pt-2 border-t border-slate-800">
          <button
            type="button"
            onClick={() => setCustomMode(!customMode)}
            className="text-xs text-indigo-400 hover:text-indigo-300 font-medium flex items-center space-x-1"
          >
            <span>{customMode ? '▲ Ocultar resolución personalizada' : '▼ Definir resolución personalizada'}</span>
          </button>

          {customMode && (
            <div className="mt-3 bg-slate-950 p-3 rounded-xl border border-slate-800 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[11px] text-slate-400 block mb-1">Ancho (px)</label>
                  <input
                    type="number"
                    value={customWidth}
                    onChange={(e) => setCustomWidth(parseInt(e.target.value) || 0)}
                    step={2}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 font-mono focus:outline-none focus:border-indigo-500"
                  />
                </div>
                <div>
                  <label className="text-[11px] text-slate-400 block mb-1">Alto (px)</label>
                  <input
                    type="number"
                    value={customHeight}
                    onChange={(e) => setCustomHeight(parseInt(e.target.value) || 0)}
                    step={2}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 font-mono focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              {customError && (
                <div className="text-[11px] text-rose-400 flex items-center space-x-1">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{customError}</span>
                </div>
              )}

              <button
                type="button"
                onClick={handleApplyCustomResolution}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-1.5 px-3 rounded-lg text-xs"
              >
                Aplicar Resolución Personalizada
              </button>
            </div>
          )}
        </div>
      </div>

      {/* SECCIÓN 3: CUADROS POR SEGUNDO (FPS) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <Film className="w-4 h-4 text-indigo-400" />
            <span>Tasa de Cuadros (FPS)</span>
          </label>
          <span className="text-xs font-mono font-bold text-indigo-300">{settings.fps} FPS</span>
        </div>

        <div className="grid grid-cols-5 gap-2">
          {[24, 30, 60, 90, 120].map((fpsVal) => (
            <button
              key={fpsVal}
              type="button"
              onClick={() => onUpdateSettings({ fps: fpsVal })}
              className={`py-2 px-1 rounded-xl text-center font-mono text-xs font-bold border transition-all ${
                settings.fps === fpsVal
                  ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                  : 'bg-slate-800/70 text-slate-300 border-slate-700/80 hover:bg-slate-800'
              }`}
            >
              {fpsVal}
            </button>
          ))}
        </div>
      </div>

      {/* SECCIÓN 4: BITRATE DE VIDEO Y ESTIMACIÓN DE ALMACENAMIENTO */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <HardDrive className="w-4 h-4 text-indigo-400" />
            <span>Tasa de Bits (Bitrate)</span>
          </label>
          <span className="text-xs font-mono font-bold text-indigo-300">{settings.bitrateMbps} Mbps</span>
        </div>

        <input
          type="range"
          min={2}
          max={50}
          step={1}
          value={settings.bitrateMbps}
          onChange={(e) => onUpdateSettings({ bitrateMbps: parseInt(e.target.value) })}
          className="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
        />

        <div className="flex items-center justify-between text-[11px] text-slate-400">
          <span>2 Mbps (Ligero)</span>
          <span>12 Mbps (Equilibrado)</span>
          <span>50 Mbps (Extremo)</span>
        </div>

        {/* Tarjeta de estimación de peso */}
        <div className="bg-slate-950/70 p-2.5 rounded-xl border border-slate-800/80 flex items-center justify-between text-xs">
          <span className="text-slate-400">Peso estimado en 10 min:</span>
          <span className="font-mono font-bold text-emerald-400">{formatBytes(estimatedSize10Min)}</span>
        </div>
      </div>

      {/* SECCIÓN 5: CÓDEC DE COMPRESIÓN DE HARDWARE */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <Zap className="w-4 h-4 text-indigo-400" />
            <span>Códec de Video (MediaCodec)</span>
          </label>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {(['H264', 'HEVC', 'AV1'] as VideoCodec[]).map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => onUpdateSettings({ codec: c })}
              className={`p-2.5 rounded-xl border text-center transition-all ${
                settings.codec === c
                  ? 'bg-indigo-950/60 border-indigo-500 text-indigo-200 ring-1 ring-indigo-500'
                  : 'bg-slate-800/60 border-slate-700/80 text-slate-400 hover:text-slate-200'
              }`}
            >
              <div className="text-xs font-bold font-mono">{c}</div>
              <div className="text-[9px] mt-0.5">
                {c === 'H264' ? 'Compatibilidad' : c === 'HEVC' ? 'Compresión Alta' : 'Ultra Eficiente'}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* SECCIÓN 6: FUENTE DE AUDIO */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
          <Mic className="w-4 h-4 text-indigo-400" />
          <span>Fuente de Audio</span>
        </label>

        <div className="grid grid-cols-2 gap-2">
          {[
            { id: 'both', label: 'Mezcla Estéreo (Juego + Mic)' },
            { id: 'internal', label: 'Audio Interno Únicamente' },
            { id: 'mic', label: 'Micrófono Únicamente' },
            { id: 'mute', label: 'Sin Audio (Mudo)' }
          ].map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => onUpdateSettings({ audioSource: item.id as AudioSourceType })}
              className={`p-2.5 rounded-xl border text-left text-xs font-medium transition-all ${
                settings.audioSource === item.id
                  ? 'bg-indigo-950/60 border-indigo-500 text-indigo-200 ring-1 ring-indigo-500'
                  : 'bg-slate-800/60 border-slate-700/80 text-slate-400 hover:text-slate-200'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* SECCIÓN 7: CUENTA REGRESIVA DE INICIO */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <Timer className="w-4 h-4 text-cyan-400" />
            <span>Cuenta Regresiva de Inicio</span>
          </label>
          <span className="text-xs font-mono font-bold text-cyan-400">
            {(settings.countdownSec ?? 3) === 0 ? 'Inmediata' : `${settings.countdownSec ?? 3}s`}
          </span>
        </div>
        <p className="text-xs text-slate-400">
          Segundos de espera antes de comenzar a grabar para darte tiempo de cambiar de app.
        </p>

        <div className="grid grid-cols-4 gap-2">
          {COUNTDOWN_PRESETS.map((sec) => {
            const isSelected = (settings.countdownSec ?? 3) === sec;
            return (
              <button
                key={sec}
                type="button"
                onClick={() => onUpdateSettings({ countdownSec: sec })}
                className={`p-2.5 rounded-xl border text-center transition-all ${
                  isSelected
                    ? 'bg-cyan-950/60 border-cyan-500 text-cyan-200 ring-1 ring-cyan-500'
                    : 'bg-slate-800/60 border-slate-700/80 text-slate-400 hover:text-slate-200'
                }`}
              >
                <div className="text-sm font-bold font-mono">{sec}s</div>
                <div className="text-[9px] mt-0.5">
                  {sec === 0 ? 'Inmediata' : sec === 3 ? 'Ideal ★' : sec === 5 ? 'Estándar' : 'Larga'}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* SECCIÓN 8: ARQUITECTURAS NDK SOPORTADAS (32-bit y 64-bit) */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-slate-200 flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>Arquitecturas de CPU (ABI Filters)</span>
          </label>
          <span className="text-[10px] text-slate-400">32 & 64 bits</span>
        </div>

        <p className="text-xs text-slate-400">
          El build de Gradle y Cargo compilará las bibliotecas .so para las siguientes arquitecturas:
        </p>

        <div className="space-y-2">
          {[
            { id: 'arm64-v8a', name: 'arm64-v8a (64 bits)', desc: 'Estándar para todos los teléfonos modernos.' },
            { id: 'armeabi-v7a', name: 'armeabi-v7a (32 bits)', desc: 'Compatibilidad para dispositivos antiguos.' },
            { id: 'x86_64', name: 'x86_64 (64 bits)', desc: 'Emuladores Android Studio y ChromeOS.' }
          ].map((abi) => {
            const isChecked = settings.targetAbis.includes(abi.id as SupportedAbi);
            return (
              <label
                key={abi.id}
                className="flex items-center space-x-3 p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 cursor-pointer"
              >
                <input
                  type="checkbox"
                  checked={isChecked}
                  onChange={() => toggleAbi(abi.id as SupportedAbi)}
                  className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 bg-slate-800 border-slate-700"
                />
                <div className="flex-1 text-xs">
                  <span className="font-mono font-bold text-slate-200">{abi.name}</span>
                  <p className="text-[10px] text-slate-400">{abi.desc}</p>
                </div>
              </label>
            );
          })}
        </div>
      </div>

      {/* NOTA TÉCNICA: VERSIONES DE ANDROID SDK */}
      <div className="bg-indigo-950/30 border border-indigo-500/30 rounded-2xl p-4 text-xs text-indigo-200/90 leading-relaxed space-y-1.5">
        <div className="font-bold text-indigo-300 flex items-center space-x-1.5">
          <HelpCircle className="w-4 h-4" />
          <span>Políticas de Android 14+ (API 34/35) y minSdkVersion 26</span>
        </div>
        <p>
          Android exige que los servicios de captura de pantalla declaren el tipo{' '}
          <code className="bg-indigo-950 px-1 py-0.5 rounded font-mono text-indigo-200">
            FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
          </code>
          . Al fijar <code className="bg-indigo-950 px-1 py-0.5 rounded font-mono">minSdkVersion = 26</code>{' '}
          (Android 8.0 Oreo), la app aprovecha aceleración por hardware NDK completa sin comprometer estabilidad.
        </p>
      </div>
    </div>
  );
};
