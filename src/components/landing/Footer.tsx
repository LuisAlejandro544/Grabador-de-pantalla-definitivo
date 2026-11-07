import React from 'react';
import { Video, ShieldCheck } from 'lucide-react';

interface FooterProps {
  onOpenDownloadModal: () => void;
}

export const Footer: React.FC<FooterProps> = ({ onOpenDownloadModal }) => {
  return (
    <footer className="py-12 bg-slate-950 border-t border-slate-900 text-slate-400 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6 pb-8 border-b border-slate-900">
          {/* Marca */}
          <div className="flex items-center space-x-2.5">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-indigo-600 to-rose-500 flex items-center justify-center text-white">
              <Video className="w-4 h-4" />
            </div>
            <span className="text-sm font-bold text-white tracking-tight">VeloRec Studio</span>
            <span className="text-slate-600">·</span>
            <span>Grabador de pantalla sin lag ni anuncios para Android</span>
          </div>

          {/* Enlaces Rápidos */}
          <div className="flex flex-wrap items-center justify-center gap-6">
            <a href="#ventajas" className="hover:text-slate-200 transition-colors">
              Ventajas
            </a>
            <a href="#comparativa" className="hover:text-slate-200 transition-colors">
              Comparativa
            </a>
            <a href="#modos" className="hover:text-slate-200 transition-colors">
              Modos
            </a>
            <a href="#instalacion" className="hover:text-slate-200 transition-colors">
              Cómo instalar
            </a>
            <a href="#faq" className="hover:text-slate-200 transition-colors">
              Preguntas
            </a>
            <button
              type="button"
              onClick={onOpenDownloadModal}
              className="text-indigo-400 hover:text-indigo-300 font-semibold cursor-pointer"
            >
              Descargar APK
            </button>
          </div>
        </div>

        {/* Copyright y Notas */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-slate-500">
          <p>© {new Date().getFullYear()} VeloRec Studio. Software libre y gratuito.</p>
          <div className="flex items-center space-x-2 text-emerald-400">
            <ShieldCheck className="w-4 h-4" />
            <span>Sin publicidad, sin marcas de agua y sin compras integradas.</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
