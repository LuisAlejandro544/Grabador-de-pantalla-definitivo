import React from 'react';
import { Video, Sliders, Film, Activity, Code2, ShieldAlert } from 'lucide-react';
import { NativeEngineType } from '../types/recorder';

export type ActiveTab = 'recorder' | 'settings' | 'gallery' | 'benchmark' | 'code';

interface NavigationProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
  isRecording: boolean;
  recordingDurationSec: number;
  engine: NativeEngineType;
  galleryCount: number;
}

export const Navigation: React.FC<NavigationProps> = ({
  activeTab,
  onTabChange,
  isRecording,
  recordingDurationSec,
  engine,
  galleryCount
}) => {
  const formatTime = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const s = secs % 60;
    return `${mins.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <>
      {/* Barra superior de estado móvil */}
      <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-4 py-3 flex items-center justify-between">
        <div className="flex items-center space-x-2.5">
          <div className="relative">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-indigo-500/25">
              <Video className="w-4 h-4 text-white" />
            </div>
            {isRecording && (
              <span className="absolute -top-1 -right-1 flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-rose-500"></span>
              </span>
            )}
          </div>
          <div>
            <div className="flex items-center space-x-1.5">
              <h1 className="font-bold text-sm tracking-tight text-white">VeloRec Studio</h1>
              <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                v1.0 Pro
              </span>
            </div>
            <p className="text-[11px] text-slate-400">RN + Kotlin + NDK</p>
          </div>
        </div>

        {/* Indicador de Motor Activo y Estado de Grabación */}
        <div className="flex items-center space-x-2">
          {isRecording ? (
            <div className="flex items-center space-x-2 bg-rose-950/80 border border-rose-500/50 px-2.5 py-1 rounded-full animate-pulse">
              <span className="w-2 h-2 rounded-full bg-rose-500" />
              <span className="text-xs font-mono font-bold text-rose-200">
                REC {formatTime(recordingDurationSec)}
              </span>
            </div>
          ) : (
            <div className="flex items-center space-x-1.5 bg-slate-800/90 border border-slate-700/80 px-2.5 py-1 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
              <span className="text-xs font-medium text-slate-300">Listo</span>
            </div>
          )}

          {/* Badge del Motor C++23 o Rust 2024 */}
          <div
            className={`px-2.5 py-1 rounded-full text-xs font-mono font-semibold flex items-center space-x-1 border transition-colors ${
              engine === 'rust2024'
                ? 'bg-amber-950/60 text-amber-300 border-amber-600/50'
                : 'bg-cyan-950/60 text-cyan-300 border-cyan-500/50'
            }`}
          >
            <span>{engine === 'rust2024' ? '🦀 Rust 2024' : '⚡ C++23'}</span>
          </div>
        </div>
      </header>

      {/* Barra de navegación inferior fija para móviles (Touch-First) */}
      <nav
        aria-label="Navegación principal de la aplicación"
        className="fixed bottom-0 left-0 right-0 z-40 bg-slate-900/95 backdrop-blur-lg border-t border-slate-800 px-2 py-2 flex items-center justify-around pb-safe"
      >
        <button
          type="button"
          onClick={() => onTabChange('recorder')}
          className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all relative ${
            activeTab === 'recorder'
              ? 'text-indigo-400 font-semibold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-label="Pantalla de Grabador"
        >
          <div className="relative">
            <Video className="w-5 h-5 mb-0.5" />
            {isRecording && (
              <span className="absolute -top-1 -right-1 w-2 h-2 bg-rose-500 rounded-full animate-ping" />
            )}
          </div>
          <span className="text-[11px]">Grabar</span>
          {activeTab === 'recorder' && (
            <span className="absolute -bottom-1 w-6 h-0.5 bg-indigo-500 rounded-full" />
          )}
        </button>

        <button
          type="button"
          onClick={() => onTabChange('settings')}
          className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all relative ${
            activeTab === 'settings'
              ? 'text-indigo-400 font-semibold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-label="Pantalla de Ajustes"
        >
          <Sliders className="w-5 h-5 mb-0.5" />
          <span className="text-[11px]">Ajustes</span>
          {activeTab === 'settings' && (
            <span className="absolute -bottom-1 w-6 h-0.5 bg-indigo-500 rounded-full" />
          )}
        </button>

        <button
          type="button"
          onClick={() => onTabChange('gallery')}
          className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all relative ${
            activeTab === 'gallery'
              ? 'text-indigo-400 font-semibold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-label="Pantalla de Galería de grabaciones"
        >
          <div className="relative">
            <Film className="w-5 h-5 mb-0.5" />
            {galleryCount > 0 && (
              <span className="absolute -top-1.5 -right-2 px-1 text-[9px] font-bold bg-indigo-600 text-white rounded-full min-w-4 text-center">
                {galleryCount}
              </span>
            )}
          </div>
          <span className="text-[11px]">Galería</span>
          {activeTab === 'gallery' && (
            <span className="absolute -bottom-1 w-6 h-0.5 bg-indigo-500 rounded-full" />
          )}
        </button>

        <button
          type="button"
          onClick={() => onTabChange('benchmark')}
          className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all relative ${
            activeTab === 'benchmark'
              ? 'text-indigo-400 font-semibold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-label="Pantalla de Comparativa y Rendimiento"
        >
          <Activity className="w-5 h-5 mb-0.5" />
          <span className="text-[11px]">Motor</span>
          {activeTab === 'benchmark' && (
            <span className="absolute -bottom-1 w-6 h-0.5 bg-indigo-500 rounded-full" />
          )}
        </button>

        <button
          type="button"
          onClick={() => onTabChange('code')}
          className={`flex flex-col items-center justify-center py-1 px-3 rounded-xl transition-all relative ${
            activeTab === 'code'
              ? 'text-indigo-400 font-semibold scale-105'
              : 'text-slate-400 hover:text-slate-200'
          }`}
          aria-label="Pantalla de Código Nativo y Descarga"
        >
          <Code2 className="w-5 h-5 mb-0.5" />
          <span className="text-[11px]">Código</span>
          {activeTab === 'code' && (
            <span className="absolute -bottom-1 w-6 h-0.5 bg-indigo-500 rounded-full" />
          )}
        </button>
      </nav>
    </>
  );
};
