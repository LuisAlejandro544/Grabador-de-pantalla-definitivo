import React, { useState } from 'react';
import { X, Download, ShieldCheck, CheckCircle2, Smartphone, Sparkles, AlertCircle } from 'lucide-react';

interface DownloadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DownloadModal: React.FC<DownloadModalProps> = ({ isOpen, onClose }) => {
  const [deviceType, setDeviceType] = useState<'modern' | 'old' | 'emulator'>('modern');
  const [downloadStarted, setDownloadStarted] = useState(false);
  const [email, setEmail] = useState('');
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleDownloadClick = () => {
    setDownloadStarted(true);
  };

  const handleSubmitWaitlist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !email.includes('@') || !email.includes('.')) {
      setErrorMsg('Por favor ingresa un correo electrónico válido');
      return;
    }
    setErrorMsg(null);
    setIsSubscribed(true);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-lg overflow-hidden shadow-2xl flex flex-col text-left">
        {/* Cabecera */}
        <div className="p-5 sm:p-6 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-indigo-600/30 text-indigo-400 flex items-center justify-center font-bold">
              <Download className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Descargar VeloRec Studio</h3>
              <span className="text-xs text-slate-400">Versión 1.0 Oficial • Para Android</span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar ventana"
            className="w-8 h-8 rounded-full bg-slate-800 text-slate-300 hover:text-white flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Contenido del modal */}
        <div className="p-5 sm:p-6 space-y-6 max-h-[75vh] overflow-y-auto">
          {/* Tarjeta de descarga principal */}
          <div className="p-4 sm:p-5 rounded-2xl bg-gradient-to-br from-indigo-950/60 to-slate-950 border border-indigo-700/50 space-y-3">
            <div className="flex items-center justify-between">
              <span className="px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-400 text-[11px] font-bold border border-emerald-500/30">
                100% Seguro y Gratis
              </span>
              <span className="text-xs text-slate-400 font-mono">Tamaño: ~8.2 MB</span>
            </div>

            <div>
              <h4 className="text-lg font-extrabold text-white">VeloRec_Studio_v1.0.apk</h4>
              <p className="text-xs text-slate-300 mt-1">
                Compatible con teléfonos Samsung, Xiaomi, Motorola, Realme, Huawei y cualquier dispositivo con Android 8.0 hasta Android 15.
              </p>
            </div>

            {!downloadStarted ? (
              <button
                type="button"
                onClick={handleDownloadClick}
                className="w-full py-3.5 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 active:scale-98 transition-all font-bold text-sm text-white shadow-lg shadow-indigo-600/40 flex items-center justify-center space-x-2 cursor-pointer mt-2"
              >
                <Download className="w-4 h-4" />
                <span>Iniciar Descarga del APK</span>
              </button>
            ) : (
              <div className="p-3.5 rounded-xl bg-emerald-950/60 border border-emerald-500/50 space-y-2">
                <div className="flex items-center space-x-2 text-emerald-300 font-bold text-xs">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span>¡Descarga simulada completada!</span>
                </div>
                <p className="text-[11px] text-slate-300 leading-relaxed">
                  Cuando la app esté publicada oficialmente en <strong>Uptodown</strong>, este enlace descargará directamente el archivo APK listo para instalar en tu teléfono.
                </p>
              </div>
            )}
          </div>

          {/* Selector de tipo de dispositivo simplificado */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-2">
              ¿Qué tipo de dispositivo tienes?
            </label>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => setDeviceType('modern')}
                className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                  deviceType === 'modern'
                    ? 'bg-indigo-600/25 border-indigo-500 text-white'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                <span className="block text-xs font-bold">Celular Normal</span>
                <span className="block text-[10px] text-slate-400 mt-0.5">La gran mayoría (64b)</span>
              </button>

              <button
                type="button"
                onClick={() => setDeviceType('old')}
                className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                  deviceType === 'old'
                    ? 'bg-indigo-600/25 border-indigo-500 text-white'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                <span className="block text-xs font-bold">Celular Antiguo</span>
                <span className="block text-[10px] text-slate-400 mt-0.5">32 bits estándar</span>
              </button>

              <button
                type="button"
                onClick={() => setDeviceType('emulator')}
                className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                  deviceType === 'emulator'
                    ? 'bg-indigo-600/25 border-indigo-500 text-white'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-white'
                }`}
              >
                <span className="block text-xs font-bold">Emulador PC</span>
                <span className="block text-[10px] text-slate-400 mt-0.5">BlueStacks / LDPlayer</span>
              </button>
            </div>
          </div>

          {/* Breve recordatorio de cómo instalar */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-1.5 text-xs">
            <span className="font-bold text-slate-300 flex items-center space-x-1.5">
              <Smartphone className="w-3.5 h-3.5 text-indigo-400" />
              <span>Pasos para instalar en tu celular:</span>
            </span>
            <p className="text-slate-400 leading-relaxed text-[11px]">
              1. Toca el APK descargado en la barra de descargas de tu navegador.<br />
              2. Si Android te pide permiso, pulsa <strong>"Permitir desde esta fuente"</strong>.<br />
              3. Toca <strong>Instalar</strong> y ¡listo para grabar tus partidas!
            </p>
          </div>

          {/* Notificación para aviso de Uptodown */}
          <div>
            {!isSubscribed ? (
              <form onSubmit={handleSubmitWaitlist} className="space-y-2.5">
                <label className="text-xs font-semibold text-slate-300 block">
                  ¿Quieres que te avisemos cuando esté en Uptodown?
                </label>
                <div className="flex gap-2">
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="tu-correo@ejemplo.com"
                    className="flex-1 bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 placeholder:text-slate-600"
                  />
                  <button
                    type="submit"
                    className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold rounded-xl whitespace-nowrap cursor-pointer transition-colors"
                  >
                    Avisarme
                  </button>
                </div>
                {errorMsg && <p className="text-[11px] text-rose-400">{errorMsg}</p>}
              </form>
            ) : (
              <div className="p-3 rounded-xl bg-indigo-950/50 border border-indigo-700/50 flex items-center space-x-2 text-xs text-indigo-300">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>¡Anotado! Te avisaremos al correo cuando esté disponible.</span>
              </div>
            )}
          </div>
        </div>

        {/* Pie del modal */}
        <div className="p-4 bg-slate-950/80 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span className="flex items-center space-x-1.5 text-emerald-400">
            <ShieldCheck className="w-4 h-4" />
            <span>Sin virus ni software malicioso</span>
          </span>
          <button
            type="button"
            onClick={onClose}
            className="text-xs font-semibold text-slate-300 hover:text-white px-3.5 py-1.5 rounded-lg bg-slate-800 cursor-pointer"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};
