import React from 'react';
import { Download, ArrowRight, ShieldCheck, Flame, Mic, Volume2, Sparkles, CheckCircle2 } from 'lucide-react';

interface HeroSectionProps {
  onOpenDownloadModal: () => void;
}

export const HeroSection: React.FC<HeroSectionProps> = ({ onOpenDownloadModal }) => {
  return (
    <section className="relative pt-28 pb-16 md:pt-36 md:pb-24 overflow-hidden">
      {/* Resplandor ambiental de fondo */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-gradient-to-tr from-indigo-600/20 via-rose-500/10 to-transparent blur-[130px] rounded-full pointer-events-none -z-10" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-center">
          {/* Columna de Texto y Propuesta de Valor para el usuario común */}
          <div className="lg:col-span-7 flex flex-col items-start space-y-6 text-left">
            {/* Badge de confianza */}
            <div className="inline-flex items-center space-x-2 text-xs sm:text-sm font-semibold text-indigo-400 bg-indigo-950/60 border border-indigo-800/60 px-3.5 py-1.5 rounded-full">
              <Sparkles className="w-3.5 h-3.5 text-rose-400" />
              <span>Grabador de pantalla 100% gratuito para Android</span>
            </div>

            {/* Titular Principal directo y comprensible */}
            <h1
              className="text-3xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-white leading-[1.15]"
              style={{ textWrap: 'balance' }}
            >
              Graba tus juegos y pantalla <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-rose-400 to-amber-300">sin lag, sin cortes</span> y sin marcas de agua.
            </h1>

            {/* Subtítulo descriptivo sin tecnicismos difíciles */}
            <p className="text-base sm:text-lg text-slate-300 leading-relaxed max-w-xl">
              Olvídate de grabadores que llenan tu pantalla de publicidad, te cobran suscripciones o se cierran solos a mitad de partida. VeloRec graba en Full HD a 60 y 120 FPS con audio interno del juego y tu voz al mismo tiempo.
            </p>

            {/* Bloque de Acciones Principales */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 w-full sm:w-auto pt-2">
              <button
                type="button"
                onClick={onOpenDownloadModal}
                className="px-6 py-4 rounded-2xl font-bold text-sm sm:text-base text-white bg-indigo-600 hover:bg-indigo-500 active:scale-95 transition-all shadow-xl shadow-indigo-600/30 flex items-center justify-center space-x-2.5 whitespace-nowrap cursor-pointer group"
              >
                <Download className="w-5 h-5 group-hover:-translate-y-0.5 transition-transform" />
                <span>Descargar APK Gratis</span>
              </button>

              <a
                href="#comparativa"
                className="px-6 py-4 rounded-2xl font-semibold text-sm text-slate-200 bg-slate-900/90 hover:bg-slate-800 border border-slate-800 transition-colors flex items-center justify-center space-x-2 whitespace-nowrap"
              >
                <span>Ver comparativa</span>
                <ArrowRight className="w-4 h-4 text-slate-400" />
              </a>
            </div>

            <p className="text-xs text-slate-400 flex items-center space-x-1.5">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>Compatible con Android 8.0 hasta Android 15 • No requiere Root • ~8 MB</span>
            </p>

            {/* Indicadores clave para el usuario común */}
            <div className="pt-6 border-t border-slate-800/80 grid grid-cols-3 gap-3 sm:gap-6 w-full max-w-lg">
              <div className="p-3 rounded-xl bg-slate-900/50 border border-slate-800/60">
                <span className="block text-xl sm:text-2xl font-black text-emerald-400">0%</span>
                <span className="text-[11px] sm:text-xs text-slate-300 mt-0.5 block font-medium">Sin anuncios</span>
              </div>
              <div className="p-3 rounded-xl bg-slate-900/50 border border-slate-800/60">
                <span className="block text-xl sm:text-2xl font-black text-indigo-400">120 FPS</span>
                <span className="text-[11px] sm:text-xs text-slate-300 mt-0.5 block font-medium">Fluidez total</span>
              </div>
              <div className="p-3 rounded-xl bg-slate-900/50 border border-slate-800/60">
                <span className="block text-xl sm:text-2xl font-black text-rose-400">100%</span>
                <span className="text-[11px] sm:text-xs text-slate-300 mt-0.5 block font-medium">Audio interno</span>
              </div>
            </div>
          </div>

          {/* Columna Visual: Mockup de Smartphone Android en HTML/CSS/SVG Puro (CERO imágenes de IA) */}
          <div className="lg:col-span-5 relative flex justify-center">
            {/* Chasis exterior del smartphone */}
            <div className="relative w-full max-w-[320px] sm:max-w-[340px] rounded-[42px] p-3.5 bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-2xl shadow-indigo-950/60 border border-slate-700/60">
              {/* Botón lateral de volumen y encendido simulados */}
              <div className="absolute -left-1.5 top-24 w-1 h-12 bg-slate-600 rounded-l-md" />
              <div className="absolute -left-1.5 top-40 w-1 h-16 bg-slate-600 rounded-l-md" />
              <div className="absolute -right-1.5 top-32 w-1 h-16 bg-slate-600 rounded-r-md" />

              {/* Pantalla interior */}
              <div className="relative rounded-[32px] overflow-hidden bg-slate-950 border border-slate-800 flex flex-col aspect-[9/19] text-left select-none">
                {/* Cámara frontal (Punch-hole) y altavoz */}
                <div className="pt-3 px-6 pb-2 flex items-center justify-between text-[11px] font-semibold text-slate-400 bg-slate-950 z-20">
                  <span>12:45</span>
                  <div className="w-3.5 h-3.5 rounded-full bg-slate-900 border border-slate-700 flex items-center justify-center">
                    <div className="w-1.5 h-1.5 rounded-full bg-indigo-950" />
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <span>5G</span>
                    <span>100%</span>
                  </div>
                </div>

                {/* Contenido de la Pantalla: Interfaz de VeloRec grabando en tiempo real */}
                <div className="flex-1 flex flex-col p-4 relative justify-between overflow-hidden">
                  {/* Fondo visual estilo partida de juego estilizada en SVG */}
                  <div className="absolute inset-0 bg-gradient-to-br from-indigo-950/50 via-slate-950 to-rose-950/30 -z-10" />
                  
                  {/* Rejilla de juego estilizada */}
                  <div className="absolute inset-0 opacity-15 pointer-events-none -z-10">
                    <svg className="w-full h-full" xmlns="http://www.w3.org/2000/svg">
                      <defs>
                        <pattern id="grid" width="24" height="24" patternUnits="userSpaceOnUse">
                          <path d="M 24 0 L 0 0 0 24" fill="none" stroke="#818cf8" strokeWidth="0.5" />
                        </pattern>
                      </defs>
                      <rect width="100%" height="100%" fill="url(#grid)" />
                    </svg>
                  </div>

                  {/* Header de la App VeloRec */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <div className="w-6 h-6 rounded-lg bg-indigo-600 flex items-center justify-center text-white">
                          <Flame className="w-3.5 h-3.5" />
                        </div>
                        <span className="font-extrabold text-xs text-white">VeloRec Studio</span>
                      </div>
                      <span className="px-2 py-0.5 rounded-md bg-emerald-500/20 text-emerald-400 text-[10px] font-bold border border-emerald-500/30">
                        HD 1080p
                      </span>
                    </div>

                    {/* Banner flotante de grabación activa */}
                    <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 shadow-lg backdrop-blur-sm">
                      <div className="flex items-center justify-between mb-1.5">
                        <div className="flex items-center space-x-2">
                          <span className="relative flex h-2.5 w-2.5">
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75" />
                            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-rose-500" />
                          </span>
                          <span className="text-xs font-bold text-white">Grabando Partida</span>
                        </div>
                        <span className="font-mono text-xs font-bold text-rose-400">04:38</span>
                      </div>

                      {/* Mini ecualizador animado de audio */}
                      <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1 border-t border-slate-800/80">
                        <div className="flex items-center space-x-1 text-cyan-400">
                          <Volume2 className="w-3 h-3" />
                          <span>Juego: 100%</span>
                        </div>
                        <div className="flex items-center space-x-1 text-emerald-400">
                          <Mic className="w-3 h-3" />
                          <span>Micrófono: Activo</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Widget central: Botón flotante y estado */}
                  <div className="my-auto text-center space-y-4 py-2">
                    {/* Botón circular táctil */}
                    <div className="relative inline-block">
                      <div className="w-24 h-24 rounded-full bg-rose-600/20 border-2 border-rose-500/40 flex items-center justify-center animate-pulse">
                        <button
                          type="button"
                          className="w-18 h-18 rounded-full bg-gradient-to-tr from-rose-600 to-rose-500 text-white font-extrabold text-xs shadow-lg shadow-rose-600/50 flex flex-col items-center justify-center space-y-1"
                        >
                          <div className="w-4 h-4 rounded-sm bg-white" />
                          <span>DETENER</span>
                        </button>
                      </div>
                    </div>

                    <div className="space-y-1">
                      <p className="text-xs font-bold text-white">Segundo plano protegido</p>
                      <p className="text-[10px] text-slate-400">No se detiene al cambiar de juego</p>
                    </div>

                    {/* Selector rápido táctil */}
                    <div className="grid grid-cols-2 gap-2 text-[10px] font-semibold">
                      <div className="p-2 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">
                        <span className="block text-slate-400 text-[9px]">Cuadros</span>
                        <span className="text-indigo-400 font-bold">60 FPS Fluido</span>
                      </div>
                      <div className="p-2 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300">
                        <span className="block text-slate-400 text-[9px]">Marca de agua</span>
                        <span className="text-emerald-400 font-bold">Desactivada</span>
                      </div>
                    </div>
                  </div>

                  {/* Tarjeta de video guardado recientemente en galería */}
                  <div className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center space-x-2.5">
                    <div className="w-9 h-9 rounded-lg bg-indigo-950 border border-indigo-800 flex items-center justify-center text-indigo-400 shrink-0 font-bold text-xs">
                      ▶
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[11px] font-bold text-white truncate">Victoria_Epica_FreeFire.mp4</p>
                      <p className="text-[9px] text-slate-400">1080p • 120 MB • Listo para compartir</p>
                    </div>
                  </div>
                </div>

                {/* Barra de navegación inferior simulada de Android */}
                <div className="pb-2 pt-1 flex justify-center items-center bg-slate-950">
                  <div className="w-28 h-1 rounded-full bg-slate-700" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
