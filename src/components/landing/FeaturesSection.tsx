import React from 'react';
import { Gamepad2, ShieldAlert, Mic, Ban, SlidersHorizontal, PlayCircle, CheckCircle2 } from 'lucide-react';

export const FeaturesSection: React.FC = () => {
  return (
    <section id="ventajas" className="py-20 md:py-28 bg-slate-950/80 border-t border-slate-900 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Encabezado directo */}
        <div className="max-w-3xl mb-14 text-left">
          <p className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-2">
            Hecho para jugadores y creadores
          </p>
          <h2
            className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight"
            style={{ textWrap: 'balance' }}
          >
            Todo lo que necesitas para grabar como un profesional en tu celular.
          </h2>
          <p className="text-slate-400 text-base sm:text-lg mt-3 leading-relaxed">
            Eliminamos todo lo malo de los grabadores convencionales: sin publicidad pesada, sin logos forzados en tus videos y sin cierres repentinos a mitad de partida.
          </p>
        </div>

        {/* Grid de 6 Tarjetas de Beneficios Reales */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* 1. Cero Lag en Juegos */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-indigo-950 border border-indigo-800/80 flex items-center justify-center text-indigo-400 mb-5 group-hover:scale-105 transition-transform">
                <Gamepad2 className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                Cero Lag en Juegos
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                Graba tus partidas a 60, 90 o 120 FPS fluidos. Está optimizado para consumir el mínimo de batería y procesador, evitando que tu teléfono se caliente o baje los cuadros por segundo.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-emerald-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Ideal para Free Fire, COD, Roblox y Minecraft</span>
            </div>
          </div>

          {/* 2. No se corta al salir de la app */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-rose-950 border border-rose-800/80 flex items-center justify-center text-rose-400 mb-5 group-hover:scale-105 transition-transform">
                <ShieldAlert className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                No se corta al salir
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                ¿Cansado de que el grabador se cierre cuando cambias de aplicación? VeloRec funciona con protección en segundo plano y controles directos en tu barra de notificaciones para pausar o detener.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-emerald-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Controles rápidos en notificaciones</span>
            </div>
          </div>

          {/* 3. Audio Interno + Micrófono */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-cyan-950 border border-cyan-800/80 flex items-center justify-center text-cyan-400 mb-5 group-hover:scale-105 transition-transform">
                <Mic className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                Audio Interno y Micrófono
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                Captura el sonido limpio de tu juego directamente del sistema y tu voz a través del micrófono para comentar jugadas o crear tutoriales con audio estéreo nítido.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-cyan-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Funciona con altavoces y auriculares</span>
            </div>
          </div>

          {/* 4. Cero Marcas de Agua ni Anuncios */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-amber-950 border border-amber-800/80 flex items-center justify-center text-amber-400 mb-5 group-hover:scale-105 transition-transform">
                <Ban className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                100% Limpio: Sin Marcas de Agua
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                Tus videos son totalmente tuyos. No te ponemos logos molestos encima de tus partidas ni te obligamos a ver anuncios de 30 segundos cada vez que quieres guardar un clip.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-emerald-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Sin compras dentro de la app</span>
            </div>
          </div>

          {/* 5. Calidad, Bitrate y Resolución a tu gusto */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-violet-950 border border-violet-800/80 flex items-center justify-center text-violet-400 mb-5 group-hover:scale-105 transition-transform">
                <SlidersHorizontal className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                Bitrate de 1 a 60 Mbps y Resoluciones Libres
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                Tú tienes el control total: ajusta el bitrate desde 1 Mbps (ultra ahorro) hasta 60 Mbps con deslizador táctil y protección antiahogamiento térmico. Cambiar de resolución jamás alterará ni reseteará tu tasa de bits elegida.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-violet-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Bitrate independiente y protección contra throttling</span>
            </div>
          </div>

          {/* 6. Galería con Reproductor Integrado */}
          <div className="p-7 sm:p-8 rounded-3xl border border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all text-left flex flex-col justify-between group">
            <div>
              <div className="w-12 h-12 rounded-2xl bg-emerald-950 border border-emerald-800/80 flex items-center justify-center text-emerald-400 mb-5 group-hover:scale-105 transition-transform">
                <PlayCircle className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">
                Galería y Compartir Rápido
              </h3>
              <p className="text-slate-300 text-sm leading-relaxed">
                Mira tus grabaciones inmediatamente dentro de la app sin que se quede congelada. Comparte tus jugadas directo a WhatsApp, Instagram o Discord con un solo botón.
              </p>
            </div>
            <div className="pt-6 mt-6 border-t border-slate-800/80 flex items-center space-x-2 text-xs font-medium text-emerald-400">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>Cero errores de formato al enviar</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
