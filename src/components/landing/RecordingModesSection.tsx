import React, { useState } from 'react';
import { Gamepad2, GraduationCap, Share2, Check, Sparkles, Volume2, Mic, Eye, Zap } from 'lucide-react';

interface RecordingMode {
  id: 'gaming' | 'tutorials' | 'light';
  title: string;
  subtitle: string;
  icon: React.ReactNode;
  recommendedResolution: string;
  recommendedFps: string;
  audioSetup: string;
  touchesSetup: string;
  description: string;
  tips: string[];
}

const MODES: RecordingMode[] = [
  {
    id: 'gaming',
    title: 'Modo Gaming',
    subtitle: 'Para Free Fire, COD, Roblox, Brawl Stars y shooters',
    icon: <Gamepad2 className="w-6 h-6 text-rose-400" />,
    recommendedResolution: '1080p Full HD / 2K',
    recommendedFps: '60 o 120 FPS ultrarrápido',
    audioSetup: 'Audio interno del juego al 100% + Micrófono opcional',
    touchesSetup: 'Toques en pantalla ocultos (video limpio)',
    description:
      'Prioriza la fluidez total y el audio interno directo del juego para que los disparos, pasos y efectos suenen con máxima nitidez sin interferencias.',
    tips: [
      'Activa los 60 o 120 FPS si tu teléfono tiene pantalla rápida.',
      'Graba con auriculares para escuchar tus llamadas de Discord sin eco.',
      'Cero lag: la app no interfiere con la tasa de cuadros del juego.',
    ],
  },
  {
    id: 'tutorials',
    title: 'Modo Tutoriales y Guías',
    subtitle: 'Para explicar apps, configuraciones o trucos',
    icon: <GraduationCap className="w-6 h-6 text-indigo-400" />,
    recommendedResolution: '1080p Full HD nítido',
    recommendedFps: '30 o 60 FPS equilibrado',
    audioSetup: 'Micrófono con reducción de ruido ambiental',
    touchesSetup: 'Toques en pantalla visibles para guiar al espectador',
    description:
      'Pensado para creadores que quieren mostrar paso a paso cómo usar una aplicación, configurar un truco en Android o enseñar un proyecto a amigos.',
    tips: [
      'Muestra círculos donde tocas la pantalla para que sea fácil seguirte.',
      'Tu voz se graba clara y sin distorsión.',
      'Puedes pausar la grabación con la burbuja flotante cuando lo necesites.',
    ],
  },
  {
    id: 'light',
    title: 'Modo Rápido / WhatsApp',
    subtitle: 'Videos que pesan poco para enviar al instante',
    icon: <Share2 className="w-6 h-6 text-emerald-400" />,
    recommendedResolution: '720p HD optimizado',
    recommendedFps: '30 FPS estándar',
    audioSetup: 'Audio estéreo balanceado',
    touchesSetup: 'Opcional según tu preferencia',
    description:
      'Ideal cuando quieres mandar un clip rápido por WhatsApp, Telegram o subir un estado sin gastar megas de tu plan de datos y sin esperar a que cargue.',
    tips: [
      'Un video de 5 minutos ocupa menos de 45 MB.',
      'Compatible con todos los celulares y chats de mensajería.',
      'Ahorra batería al máximo durante grabaciones largas.',
    ],
  },
];

export const RecordingModesSection: React.FC = () => {
  const [activeMode, setActiveMode] = useState<'gaming' | 'tutorials' | 'light'>('gaming');
  const current = MODES.find((m) => m.id === activeMode) || MODES[0];

  return (
    <section id="modos" className="py-20 md:py-28 bg-slate-950/70 border-t border-slate-900 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-12 text-left">
          <p className="text-xs font-semibold text-cyan-400 uppercase tracking-wider mb-2">
            Configuración Inteligente con 1 Toque
          </p>
          <h2
            className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight"
            style={{ textWrap: 'balance' }}
          >
            Elige qué quieres grabar y VeloRec se ajusta solo.
          </h2>
          <p className="text-slate-400 text-base sm:text-lg mt-3 leading-relaxed">
            No necesitas saber de resoluciones ni ajustes raros. Selecciona tu objetivo y la app prepara la mejor configuración al instante.
          </p>
        </div>

        {/* Selector de Modos de Uso */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-8">
          {MODES.map((mode) => {
            const isSelected = activeMode === mode.id;
            return (
              <button
                key={mode.id}
                type="button"
                onClick={() => setActiveMode(mode.id)}
                className={`p-5 rounded-2xl border text-left cursor-pointer transition-all ${
                  isSelected
                    ? 'bg-slate-900 border-indigo-500 shadow-lg shadow-indigo-950/50'
                    : 'bg-slate-950/60 border-slate-800 hover:border-slate-700 text-slate-400'
                }`}
              >
                <div className="flex items-center space-x-3 mb-2">
                  <div className="w-10 h-10 rounded-xl bg-slate-800/80 flex items-center justify-center">
                    {mode.icon}
                  </div>
                  <div>
                    <h3 className={`font-bold text-base ${isSelected ? 'text-white' : 'text-slate-300'}`}>
                      {mode.title}
                    </h3>
                  </div>
                </div>
                <p className="text-xs text-slate-400 line-clamp-2">{mode.subtitle}</p>
              </button>
            );
          })}
        </div>

        {/* Ficha Detallada del Modo Seleccionado */}
        <div className="p-6 sm:p-8 rounded-3xl bg-slate-900/40 border border-slate-800 text-left">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            <div className="lg:col-span-7 space-y-5">
              <div className="flex items-center space-x-2 text-indigo-400 text-xs font-semibold">
                <Sparkles className="w-4 h-4" />
                <span>Configuración recomendada para {current.title}</span>
              </div>
              <p className="text-sm sm:text-base text-slate-200 leading-relaxed">
                {current.description}
              </p>

              {/* Ajustes Clave con Iconos */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
                  <span className="text-[11px] text-slate-400 block">Resolución</span>
                  <span className="text-sm font-bold text-white mt-0.5 block">
                    {current.recommendedResolution}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
                  <span className="text-[11px] text-slate-400 block">Tasa de Cuadros</span>
                  <span className="text-sm font-bold text-cyan-400 mt-0.5 block">
                    {current.recommendedFps}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
                  <span className="text-[11px] text-slate-400 block">Audio</span>
                  <span className="text-sm font-bold text-emerald-400 mt-0.5 block">
                    {current.audioSetup}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800">
                  <span className="text-[11px] text-slate-400 block">Toques en Pantalla</span>
                  <span className="text-sm font-bold text-amber-300 mt-0.5 block">
                    {current.touchesSetup}
                  </span>
                </div>
              </div>
            </div>

            {/* Consejos prácticos */}
            <div className="lg:col-span-5 p-5 rounded-2xl bg-slate-950 border border-slate-800 space-y-3">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center space-x-1.5">
                <Zap className="w-3.5 h-3.5 text-amber-400" />
                <span>Consejos para mejores resultados:</span>
              </h4>

              <div className="space-y-2.5">
                {current.tips.map((tip) => (
                  <div key={tip} className="flex items-start space-x-2 text-xs text-slate-300">
                    <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                    <span>{tip}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
