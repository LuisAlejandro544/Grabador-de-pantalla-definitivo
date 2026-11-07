import React from 'react';
import { Video, Download, Shield } from 'lucide-react';

interface NavbarProps {
  onOpenDownloadModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenDownloadModal }) => {
  return (
    <header className="fixed top-0 left-0 right-0 z-40 bg-slate-950/85 backdrop-blur-md border-b border-slate-800/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 sm:h-18 flex items-center justify-between">
        {/* Zona 1: Identidad y Marca */}
        <a href="#" className="flex items-center space-x-3 group">
          <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-rose-500 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
            <Video className="w-5 h-5" />
          </div>
          <div className="flex flex-col text-left">
            <span className="font-extrabold text-base sm:text-lg text-white tracking-tight leading-none group-hover:text-indigo-400 transition-colors">
              VeloRec Studio
            </span>
            <span className="text-[10px] text-slate-400 font-medium mt-0.5">
              Grabador de Pantalla para Android
            </span>
          </div>
        </a>

        {/* Zona 2: Enlaces de Navegación para usuarios comunes */}
        <nav className="hidden md:flex items-center space-x-6 text-sm font-medium text-slate-300">
          <a href="#ventajas" className="hover:text-white transition-colors">
            Ventajas
          </a>
          <a href="#comparativa" className="hover:text-white transition-colors">
            Comparativa
          </a>
          <a href="#modos" className="hover:text-white transition-colors">
            Modos de Grabación
          </a>
          <a href="#instalacion" className="hover:text-white transition-colors">
            ¿Cómo instalar?
          </a>
          <a href="#faq" className="hover:text-white transition-colors">
            Preguntas
          </a>
        </nav>

        {/* Zona 3: Botón de Descarga Destacado */}
        <div className="flex items-center space-x-3">
          <div className="hidden sm:flex items-center space-x-1.5 text-xs text-emerald-400 font-medium mr-1">
            <Shield className="w-3.5 h-3.5" />
            <span>Sin anuncios</span>
          </div>
          <button
            type="button"
            onClick={onOpenDownloadModal}
            className="px-4 py-2 sm:px-5 sm:py-2.5 rounded-xl text-xs sm:text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-500 active:scale-95 transition-all shadow-md shadow-indigo-600/30 flex items-center space-x-2 cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Descargar APK</span>
          </button>
        </div>
      </div>
    </header>
  );
};
