import React from 'react';
import { Download, ShieldCheck, PlayCircle, CheckCircle2, Smartphone } from 'lucide-react';

interface InstallationGuideProps {
  onOpenDownloadModal: () => void;
}

export const InstallationGuideSection: React.FC<InstallationGuideProps> = ({ onOpenDownloadModal }) => {
  return (
    <section id="instalacion" className="py-20 md:py-28 bg-slate-950 relative border-t border-slate-900">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-14 text-left">
          <p className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2">
            Instalación Rápida
          </p>
          <h2
            className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight"
            style={{ textWrap: 'balance' }}
          >
            Cómo instalar VeloRec en tu teléfono en 3 simples pasos.
          </h2>
          <p className="text-slate-400 text-base sm:text-lg mt-3 leading-relaxed">
            Sin suscripciones, sin registros de cuentas y sin configuraciones complicadas. Listo para funcionar en menos de un minuto.
          </p>
        </div>

        {/* 3 Pasos Visuales */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          {/* Paso 1 */}
          <div className="p-7 rounded-3xl border border-slate-800 bg-slate-900/40 relative flex flex-col justify-between text-left">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-indigo-950/80 border border-indigo-700/60 flex items-center justify-center text-indigo-400 mb-5 font-black text-lg">
                1
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Descarga el APK</h3>
              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Toca el botón de descarga para guardar el archivo instalador oficial en tu teléfono. Pesa menos de 10 MB.
              </p>
            </div>
            <div className="pt-4 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs text-slate-400">
              <Download className="w-3.5 h-3.5 text-indigo-400" />
              <span>Descarga directa sin esperas</span>
            </div>
          </div>

          {/* Paso 2 */}
          <div className="p-7 rounded-3xl border border-slate-800 bg-slate-900/40 relative flex flex-col justify-between text-left">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-cyan-950/80 border border-cyan-700/60 flex items-center justify-center text-cyan-400 mb-5 font-black text-lg">
                2
              </div>
              <h3 className="text-lg font-bold text-white mb-2">Permite la Instalación</h3>
              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Abre el archivo descargado. Si tu celular te pregunta por seguridad, activa la casilla <em>"Permitir desde esta fuente"</em>. Es el paso estándar en Android.
              </p>
            </div>
            <div className="pt-4 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs text-slate-400">
              <ShieldCheck className="w-3.5 h-3.5 text-cyan-400" />
              <span>100% Seguro y verificado</span>
            </div>
          </div>

          {/* Paso 3 */}
          <div className="p-7 rounded-3xl border border-slate-800 bg-slate-900/40 relative flex flex-col justify-between text-left">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-emerald-950/80 border border-emerald-700/60 flex items-center justify-center text-emerald-400 mb-5 font-black text-lg">
                3
              </div>
              <h3 className="text-lg font-bold text-white mb-2">¡Empieza a Grabar!</h3>
              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                Abre VeloRec, concede los permisos de pantalla y micrófono, y presiona el botón rojo para capturar tus jugadas sin lag.
              </p>
            </div>
            <div className="pt-4 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs text-slate-400">
              <PlayCircle className="w-3.5 h-3.5 text-emerald-400" />
              <span>Listo para jugar de inmediato</span>
            </div>
          </div>
        </div>

        {/* Tarjeta de Seguridad y Botón CTA */}
        <div className="p-6 sm:p-8 rounded-3xl bg-gradient-to-r from-indigo-950/60 via-slate-900 to-slate-950 border border-indigo-900/50 flex flex-col sm:flex-row items-center justify-between gap-6 text-left">
          <div className="space-y-1.5">
            <h4 className="text-lg font-bold text-white flex items-center space-x-2">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <span>Garantía de Privacidad y Seguridad</span>
            </h4>
            <p className="text-xs sm:text-sm text-slate-300 max-w-xl leading-relaxed">
              VeloRec no recopila datos personales, no incluye rastreadores de publicidad y tus videos se guardan exclusivamente en la memoria interna de tu teléfono.
            </p>
          </div>

          <button
            type="button"
            onClick={onOpenDownloadModal}
            className="w-full sm:w-auto px-6 py-3.5 rounded-xl font-bold text-sm text-white bg-indigo-600 hover:bg-indigo-500 active:scale-95 transition-all shadow-lg shadow-indigo-600/30 flex items-center justify-center space-x-2 whitespace-nowrap cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Descargar Gratis Ahora</span>
          </button>
        </div>
      </div>
    </section>
  );
};
