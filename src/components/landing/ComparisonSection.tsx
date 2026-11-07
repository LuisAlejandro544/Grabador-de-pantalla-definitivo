import React, { useState } from 'react';
import { Check, X, HardDrive, Sparkles, Smartphone, ShieldCheck } from 'lucide-react';
import { formatBytes, calculateEstimatedFileSize } from '../../utils/recorderCore';

interface ComparisonRow {
  feature: string;
  velorec: string;
  others: string;
  highlight?: boolean;
}

const COMPARISON_DATA: ComparisonRow[] = [
  {
    feature: 'Publicidad y Anuncios',
    velorec: '100% Libre de anuncios, siempre',
    others: 'Anuncios en pantalla completa y videos de 30s',
    highlight: true,
  },
  {
    feature: 'Marcas de Agua en el Video',
    velorec: 'Cero marcas de agua. Video 100% limpio',
    others: 'Logo gigante en la esquina a menos que pagues',
    highlight: true,
  },
  {
    feature: 'Grabación en Segundo Plano',
    velorec: 'No se corta nunca al salir de la app o jugar',
    others: 'Se congela o se cierra solo a mitad de partida',
    highlight: true,
  },
  {
    feature: 'Audio Interno del Juego',
    velorec: 'Sonido directo estéreo digital y micrófono',
    others: 'Solo micrófono con eco o bloqueado por el sistema',
  },
  {
    feature: 'Impacto en el Teléfono',
    velorec: 'Optimizado: no calienta el móvil ni baja FPS',
    others: 'Sobrecalienta el celular y produce lag en juegos',
  },
  {
    feature: 'Precio y Suscripciones',
    velorec: 'Completamente Gratis para siempre',
    others: 'Suscripción semanal o mensual costosa',
    highlight: true,
  },
];

export const ComparisonSection: React.FC = () => {
  const [minutes, setMinutes] = useState<number>(15);
  const [qualityPreset, setQualityPreset] = useState<'720p' | '1080p' | '1080p60'>('1080p');

  // Cálculo en MB para el usuario
  const bitrate = qualityPreset === '720p' ? 6 : qualityPreset === '1080p' ? 12 : 18;
  const estimatedBytes = calculateEstimatedFileSize(minutes * 60, bitrate);
  const estimatedFormatted = formatBytes(estimatedBytes);

  return (
    <section id="comparativa" className="py-20 md:py-28 bg-slate-950 relative border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Encabezado */}
        <div className="max-w-3xl mb-14 text-left">
          <p className="text-xs font-semibold text-rose-400 uppercase tracking-wider mb-2">
            La diferencia real
          </p>
          <h2
            className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight"
            style={{ textWrap: 'balance' }}
          >
            VeloRec Studio frente a los grabadores tradicionales.
          </h2>
          <p className="text-slate-400 text-base sm:text-lg mt-3 leading-relaxed">
            Mira por qué miles de jugadores prefieren una app limpia y optimizada en lugar de grabadores saturados de publicidad.
          </p>
        </div>

        {/* Tabla Comparativa Responsiva */}
        <div className="rounded-3xl border border-slate-800 bg-slate-900/40 overflow-hidden shadow-xl mb-16">
          <div className="grid grid-cols-1 md:grid-cols-12 divide-y md:divide-y-0 md:divide-x divide-slate-800">
            {/* Columna VeloRec */}
            <div className="md:col-span-7 p-6 sm:p-8 bg-slate-900/60 flex flex-col justify-between">
              <div className="flex items-center justify-between pb-4 mb-6 border-b border-slate-800">
                <div className="flex items-center space-x-2.5">
                  <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center font-bold text-sm">
                    VR
                  </div>
                  <span className="font-extrabold text-lg text-white">VeloRec Studio</span>
                </div>
                <span className="px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold border border-emerald-500/30 flex items-center space-x-1">
                  <Sparkles className="w-3 h-3" />
                  <span>Recomendado</span>
                </span>
              </div>

              <div className="space-y-4">
                {COMPARISON_DATA.map((item) => (
                  <div
                    key={item.feature}
                    className="p-3.5 rounded-2xl bg-slate-950/70 border border-slate-800/80 flex items-start space-x-3 text-left"
                  >
                    <div className="w-5 h-5 rounded-full bg-emerald-950 border border-emerald-700/80 text-emerald-400 flex items-center justify-center shrink-0 mt-0.5">
                      <Check className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <span className="block text-xs font-semibold text-slate-400">{item.feature}</span>
                      <span className="text-sm font-bold text-white mt-0.5 block">{item.velorec}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Columna Otras Aplicaciones */}
            <div className="md:col-span-5 p-6 sm:p-8 bg-slate-950/50 flex flex-col justify-between text-left">
              <div className="pb-4 mb-6 border-b border-slate-800">
                <span className="font-bold text-base text-slate-400">Otros Grabadores Comunes</span>
                <span className="block text-xs text-slate-500 mt-0.5">Apps con anuncios de la tienda</span>
              </div>

              <div className="space-y-4">
                {COMPARISON_DATA.map((item) => (
                  <div
                    key={item.feature}
                    className="p-3.5 rounded-2xl bg-slate-900/30 border border-slate-800/40 flex items-start space-x-3 opacity-75"
                  >
                    <div className="w-5 h-5 rounded-full bg-rose-950 border border-rose-800 text-rose-400 flex items-center justify-center shrink-0 mt-0.5">
                      <X className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <span className="block text-xs text-slate-500">{item.feature}</span>
                      <span className="text-sm font-medium text-slate-300 mt-0.5 block">{item.others}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Calculadora Rápida de Almacenamiento (Fácil y Práctica para Usuarios) */}
        <div className="rounded-3xl border border-slate-800 bg-slate-900/40 p-6 sm:p-8 text-left">
          <div className="max-w-2xl mb-6">
            <span className="text-xs font-semibold text-cyan-400 uppercase tracking-wider block mb-1">
              Calculadora Práctica
            </span>
            <h3 className="text-xl sm:text-2xl font-bold text-white mb-2">
              ¿Cuánto espacio ocupará tu video en el celular?
            </h3>
            <p className="text-sm text-slate-300 leading-relaxed">
              Elige cuánto tiempo piensas grabar y la calidad deseada para ver el peso aproximado antes de empezar.
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
            <div className="lg:col-span-7 space-y-5">
              {/* Selector de Calidad */}
              <div>
                <label className="text-xs font-semibold text-slate-300 block mb-2">
                  Calidad de Grabación
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setQualityPreset('720p')}
                    className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                      qualityPreset === '720p'
                        ? 'bg-indigo-600/30 border-indigo-500 text-white'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="block text-xs font-bold">HD 720p</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">Ahorro de espacio</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setQualityPreset('1080p')}
                    className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                      qualityPreset === '1080p'
                        ? 'bg-indigo-600/30 border-indigo-500 text-white'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="block text-xs font-bold">Full HD 1080p</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">Recomendado</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setQualityPreset('1080p60')}
                    className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                      qualityPreset === '1080p60'
                        ? 'bg-indigo-600/30 border-indigo-500 text-white'
                        : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <span className="block text-xs font-bold">60 FPS Gaming</span>
                    <span className="block text-[10px] text-slate-400 mt-0.5">Máxima fluidez</span>
                  </button>
                </div>
              </div>

              {/* Slider de Duración */}
              <div>
                <div className="flex items-center justify-between text-xs font-semibold text-slate-300 mb-2">
                  <span>Duración de la partida o video:</span>
                  <span className="font-bold text-indigo-400">{minutes} minutos</span>
                </div>
                <input
                  type="range"
                  min={1}
                  max={60}
                  step={1}
                  value={minutes}
                  onChange={(e) => setMinutes(Number(e.target.value))}
                  className="w-full accent-indigo-500 cursor-pointer h-2 bg-slate-950 rounded-lg"
                />
                <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                  <span>1 min (Clip rápido)</span>
                  <span>15 min (Partida estándar)</span>
                  <span>60 min (Directo largo)</span>
                </div>
              </div>
            </div>

            {/* Resultado Visual de Espacio */}
            <div className="lg:col-span-5 p-6 rounded-2xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center text-center space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-indigo-950/80 border border-indigo-700/60 flex items-center justify-center text-indigo-400">
                <HardDrive className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs text-slate-400 block">Tamaño estimado del archivo:</span>
                <span className="text-3xl font-extrabold text-white font-mono mt-1 block">
                  {estimatedFormatted}
                </span>
              </div>
              <p className="text-[11px] text-emerald-400 flex items-center space-x-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Formato MP4 estándar listo para compartir por WhatsApp</span>
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
