/**
 * VeloRec Studio - Página Oficial de Bienvenida
 * Presentación orientada a usuarios comunes para descargar el grabador de pantalla
 * nativo en Android (sin publicidad, sin marcas de agua y sin lag en juegos).
 */

import React, { useState } from 'react';
import { Navbar } from './components/landing/Navbar';
import { HeroSection } from './components/landing/HeroSection';
import { FeaturesSection } from './components/landing/FeaturesSection';
import { ComparisonSection } from './components/landing/ComparisonSection';
import { RecordingModesSection } from './components/landing/RecordingModesSection';
import { InstallationGuideSection } from './components/landing/InstallationGuideSection';
import { FAQSection } from './components/landing/FAQSection';
import { Footer } from './components/landing/Footer';
import { DownloadModal } from './components/landing/DownloadModal';

export default function App() {
  const [isDownloadModalOpen, setIsDownloadModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white antialiased">
      {/* Barra de Navegación */}
      <Navbar onOpenDownloadModal={() => setIsDownloadModalOpen(true)} />

      {/* Contenido Principal para Usuarios Comunes */}
      <main className="flex-1 w-full flex flex-col">
        {/* Sección Hero con Titular y Mockup de Celular en CSS/SVG Puro */}
        <HeroSection onOpenDownloadModal={() => setIsDownloadModalOpen(true)} />

        {/* Ventajas Clave para Jugadores y Creadores (Sin lag, segundo plano, audio interno) */}
        <FeaturesSection />

        {/* Comparativa Directa con otras apps de la tienda y calculadora de espacio */}
        <ComparisonSection />

        {/* Modos de Grabación Rápidos (Gaming, Tutoriales, Redes) */}
        <RecordingModesSection />

        {/* Guía Rápida de Instalación en 3 Pasos */}
        <InstallationGuideSection onOpenDownloadModal={() => setIsDownloadModalOpen(true)} />

        {/* Preguntas Frecuentes para Usuarios Comunes */}
        <FAQSection />
      </main>

      {/* Pie de Página */}
      <Footer onOpenDownloadModal={() => setIsDownloadModalOpen(true)} />

      {/* Modal de Descarga del APK */}
      <DownloadModal
        isOpen={isDownloadModalOpen}
        onClose={() => setIsDownloadModalOpen(false)}
      />
    </div>
  );
}
