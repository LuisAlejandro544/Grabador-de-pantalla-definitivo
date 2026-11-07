import React, { useState } from 'react';
import { ChevronDown, HelpCircle } from 'lucide-react';

interface FaqItem {
  question: string;
  answer: string;
}

const FAQ_ITEMS: FaqItem[] = [
  {
    question: '¿Es realmente gratis o me cobrarán después?',
    answer:
      'Es 100% gratuito. No hay suscripciones ocultas, no hay versiones "Premium" con funciones bloqueadas y nunca te cobraremos por quitar marcas de agua o grabar a máxima calidad.',
  },
  {
    question: '¿Por qué la grabación no se corta cuando salgo al menú o cambio de juego?',
    answer:
      'VeloRec está programado específicamente para funcionar en segundo plano continuo mediante el servicio de grabación oficial de Android. Incluso si cambias de app, te llega un mensaje de WhatsApp o abres un juego pesado, la grabación sigue activa y segura con controles en tu barra de notificaciones.',
  },
  {
    question: '¿Puedo grabar el audio interno del juego y mi voz a la vez?',
    answer:
      'Sí. Puedes elegir grabar solo el sonido digital interno de tu juego (disparos, pasos, música), solo tu micrófono para tutoriales, o ambos combinados para comentar tus partidas con tus amigos.',
  },
  {
    question: '¿Necesito tener mi celular rooteado (Root)?',
    answer:
      'No, no necesitas Root. VeloRec funciona en cualquier teléfono con Android 8.0 o superior utilizando las funciones de captura seguras del sistema.',
  },
  {
    question: '¿Por qué se descarga como un archivo APK y no en Google Play?',
    answer:
      'Distribuir el APK directamente y en plataformas abiertas como Uptodown nos permite ofrecer la app completamente libre de publicidad forzada, sin limitaciones de la tienda y con actualizaciones más rápidas sin intermediarios.',
  },
  {
    question: '¿Dónde se guardan mis videos grabados?',
    answer:
      'Tus videos se guardan directamente en el almacenamiento de tu teléfono, dentro de la carpeta Movies/VeloRec. Puedes verlos en la galería de tu celular, editarlos o enviarlos por WhatsApp, TikTok o YouTube de inmediato.',
  },
  {
    question: '¿Funciona bien en teléfonos de gama baja o con poca memoria?',
    answer:
      'Sí. VeloRec fue optimizado para no sobrecargar el procesador ni la memoria RAM. Además, puedes seleccionar la calidad en 720p a 30 FPS para ahorrar espacio y mantener el celular fresco en partidas largas.',
  },
];

export const FAQSection: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const toggleFaq = (idx: number) => {
    setOpenIndex((prev) => (prev === idx ? null : idx));
  };

  return (
    <section id="faq" className="py-20 md:py-28 bg-slate-950/80 border-t border-slate-900 relative">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-14 text-left">
          <p className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-2 flex items-center space-x-1.5">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Dudas Habituales</span>
          </p>
          <h2
            className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight"
            style={{ textWrap: 'balance' }}
          >
            Preguntas Frecuentes
          </h2>
          <p className="text-slate-400 text-base sm:text-lg mt-3 leading-relaxed">
            Todo lo que necesitas saber antes de instalar VeloRec Studio en tu dispositivo móvil.
          </p>
        </div>

        <div className="space-y-3.5">
          {FAQ_ITEMS.map((item, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div
                key={item.question}
                className="rounded-2xl border border-slate-800/90 bg-slate-900/40 overflow-hidden transition-colors text-left"
              >
                <button
                  type="button"
                  onClick={() => toggleFaq(idx)}
                  className="w-full p-5 sm:p-6 text-left flex items-center justify-between gap-4 cursor-pointer"
                  aria-expanded={isOpen}
                >
                  <span className="text-sm sm:text-base font-bold text-white leading-snug">
                    {item.question}
                  </span>
                  <ChevronDown
                    className={`w-5 h-5 text-slate-400 shrink-0 transition-transform duration-300 ${
                      isOpen ? 'rotate-180 text-indigo-400' : ''
                    }`}
                  />
                </button>

                {isOpen && (
                  <div className="px-5 pb-5 sm:px-6 sm:pb-6 text-xs sm:text-sm text-slate-300 leading-relaxed border-t border-slate-800/60 pt-4">
                    {item.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
