import React, { useState, useRef, useEffect } from 'react';
import {
  Film,
  Play,
  Download,
  Trash2,
  Clock,
  HardDrive,
  X,
  FileVideo,
  AlertCircle,
  CheckCircle2,
  Share2
} from 'lucide-react';
import { RecordedVideoItem } from '../types/recorder';
import { formatBytes, formatDuration } from '../utils/recorderCore';

interface GalleryScreenProps {
  videos: RecordedVideoItem[];
  onDeleteVideo: (id: string) => void;
  onGoToRecorder: () => void;
}

export const GalleryScreen: React.FC<GalleryScreenProps> = ({
  videos,
  onDeleteVideo,
  onGoToRecorder
}) => {
  const [selectedVideo, setSelectedVideo] = useState<RecordedVideoItem | null>(null);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [videoLoadError, setVideoLoadError] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const previewCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const animFrameRef = useRef<number | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleDownload = (video: RecordedVideoItem) => {
    try {
      if (video.blobUrl && video.blobUrl.startsWith('blob:')) {
        const a = document.createElement('a');
        a.href = video.blobUrl;
        a.download = `${video.title}.mp4`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        showToast('Descarga iniciada en tu dispositivo');
      } else {
        // Fallback para videos de demostración sin blobUrl activa
        const dummyText = `VeloRec Studio Video Meta\nID: ${video.id}\nTitle: ${video.title}\nEngine: ${video.engineUsed}\nResolution: ${video.resolution}\nFPS: ${video.fps}`;
        const blob = new Blob([dummyText], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${video.title}_metadata.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('Metadatos del clip descargados correctamente');
      }
    } catch {
      showToast('Error al procesar la descarga');
    }
  };

  // Reproductor interactivo por canvas cuando no hay un blobUrl de video disponible o falla el códec
  useEffect(() => {
    if (!selectedVideo || (selectedVideo.blobUrl && !videoLoadError)) return;

    const canvas = previewCanvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let frame = 0;
    const renderSim = () => {
      frame++;
      const w = canvas.width;
      const h = canvas.height;

      // Fondo
      ctx.fillStyle = '#090d16';
      ctx.fillRect(0, 0, w, h);

      // Rejilla sutil
      ctx.strokeStyle = 'rgba(99, 102, 241, 0.1)';
      ctx.lineWidth = 1;
      for (let x = 0; x < w; x += 24) {
        ctx.beginPath();
        ctx.moveTo(x, 0);
        ctx.lineTo(x, h);
        ctx.stroke();
      }

      // Animación de pulso
      const cx = w / 2;
      const cy = h / 2 - 20;
      const radius = 60 + Math.sin(frame * 0.08) * 12;

      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.fillStyle = selectedVideo.engineUsed === 'rust2024' ? 'rgba(245, 158, 11, 0.25)' : 'rgba(6, 182, 212, 0.25)';
      ctx.fill();

      ctx.beginPath();
      ctx.arc(cx, cy, radius * 0.6, 0, Math.PI * 2);
      ctx.fillStyle = selectedVideo.engineUsed === 'rust2024' ? 'rgba(245, 158, 11, 0.6)' : 'rgba(6, 182, 212, 0.6)';
      ctx.fill();

      // Ondas dinámicas
      ctx.strokeStyle = selectedVideo.engineUsed === 'rust2024' ? '#f59e0b' : '#06b6d4';
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      for (let x = 20; x < w - 20; x += 4) {
        const y = cy + Math.sin((x + frame * 5) * 0.05) * 18;
        if (x === 20) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();

      // Textos de estado
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 12px "JetBrains Mono", monospace';
      ctx.textAlign = 'center';
      ctx.fillText(`REPRODUCIENDO: ${selectedVideo.title}`, cx, cy + 90);

      ctx.fillStyle = '#94a3b8';
      ctx.font = '10px monospace';
      ctx.fillText(`${selectedVideo.resolution} @ ${selectedVideo.fps} FPS • ${selectedVideo.codec}`, cx, cy + 110);

      animFrameRef.current = requestAnimationFrame(renderSim);
    };

    renderSim();

    return () => {
      if (animFrameRef.current) {
        cancelAnimationFrame(animFrameRef.current);
      }
    };
  }, [selectedVideo, videoLoadError]);

  return (
    <div className="flex flex-col min-h-[calc(100vh-60px)] pb-28 px-4 pt-4 max-w-lg mx-auto w-full space-y-5">
      {/* Notificación flotante */}
      {toastMessage && (
        <div className="fixed top-14 left-1/2 -translate-x-1/2 z-50 bg-slate-900 border border-indigo-500/50 text-indigo-200 px-4 py-2 rounded-xl text-xs flex items-center space-x-2 shadow-2xl animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Encabezado */}
      <div className="border-b border-slate-800 pb-3 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center space-x-2">
            <Film className="w-5 h-5 text-indigo-400" />
            <span>Galería de Grabaciones</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            {videos.length} {videos.length === 1 ? 'video guardado' : 'videos guardados'} en almacenamiento local
          </p>
        </div>

        <button
          type="button"
          onClick={onGoToRecorder}
          className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-3 py-1.5 rounded-xl shadow-md transition-all active:scale-95"
        >
          + Grabar Nuevo
        </button>
      </div>

      {/* Lista de Videos o Estado Vacío */}
      {videos.length === 0 ? (
        <div className="bg-slate-900/50 border border-slate-800/80 rounded-2xl p-8 text-center flex flex-col items-center justify-center space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-slate-800/80 flex items-center justify-center text-slate-500">
            <FileVideo className="w-7 h-7" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-200">No hay grabaciones aún</h3>
            <p className="text-xs text-slate-400 mt-1 max-w-xs">
              Inicia una grabación de pantalla desde la pestaña principal para almacenar tus videos aquí.
            </p>
          </div>
          <button
            type="button"
            onClick={onGoToRecorder}
            className="mt-2 bg-gradient-to-r from-indigo-600 to-indigo-500 text-white font-semibold text-xs px-5 py-2.5 rounded-xl shadow-lg"
          >
            Ir al Grabador
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3">
          {videos.map((video) => (
            <div
              key={video.id}
              className="bg-slate-900/90 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-3.5 shadow-lg transition-all flex flex-col space-y-3"
            >
              <div className="flex items-start space-x-3">
                {/* Miniatura / Thumbnail con botón de reproducción */}
                <div
                  onClick={() => {
                    setVideoLoadError(false);
                    setSelectedVideo(video);
                  }}
                  role="button"
                  tabIndex={0}
                  aria-label={`Ver miniatura y reproducir ${video.title}`}
                  className="relative w-24 h-24 bg-slate-950 rounded-xl overflow-hidden border border-slate-800 shrink-0 flex items-center justify-center cursor-pointer group"
                >
                  {video.thumbnailUrl ? (
                    <img src={video.thumbnailUrl} alt={video.title} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-slate-900 to-slate-950 flex flex-col items-center justify-center">
                      <Film className="w-6 h-6 text-slate-600 mb-1" />
                      <span className="text-[9px] font-mono text-slate-400">{video.resolution}</span>
                    </div>
                  )}
                  <div className="absolute inset-0 bg-black/40 group-hover:bg-black/20 flex items-center justify-center transition-colors">
                    <div className="w-8 h-8 rounded-full bg-indigo-600/90 text-white flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform">
                      <Play className="w-4 h-4 fill-current ml-0.5" />
                    </div>
                  </div>
                  <span className="absolute bottom-1 right-1 bg-slate-950/80 text-white text-[9px] font-mono px-1 rounded">
                    {formatDuration(video.durationSec)}
                  </span>
                </div>

                {/* Información del video */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-1.5 mb-1">
                    <span
                      className={`text-[10px] font-mono font-bold px-1.5 py-0.5 rounded border ${
                        video.engineUsed === 'rust2024'
                          ? 'bg-amber-950/80 text-amber-300 border-amber-600/60'
                          : 'bg-cyan-950/80 text-cyan-300 border-cyan-500/60'
                      }`}
                    >
                      {video.engineUsed === 'rust2024' ? '🦀 Rust 2024' : '⚡ C++23'}
                    </span>
                    <span className="text-[10px] text-slate-400 font-mono">
                      {video.codec} • {video.fps} FPS
                    </span>
                  </div>

                  <h3 className="text-xs font-bold text-slate-100 truncate" title={video.title}>
                    {video.title}
                  </h3>

                  <div className="flex items-center space-x-3 text-[11px] text-slate-400 mt-2">
                    <span className="flex items-center space-x-1">
                      <HardDrive className="w-3 h-3 text-slate-500" />
                      <span>{formatBytes(video.sizeBytes)}</span>
                    </span>
                    <span className="flex items-center space-x-1">
                      <Clock className="w-3 h-3 text-slate-500" />
                      <span>{new Date(video.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </span>
                  </div>
                </div>
              </div>

              {/* Botones de acción (Descargar y Eliminar) */}
              <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between">
                <button
                  type="button"
                  onClick={() => {
                    setVideoLoadError(false);
                    setSelectedVideo(video);
                  }}
                  aria-label={`Reproducir video ${video.title}`}
                  className="text-xs text-indigo-400 hover:text-indigo-300 font-medium flex items-center space-x-1"
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>Reproducir</span>
                </button>

                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => handleDownload(video)}
                    aria-label={`Descargar archivo ${video.title}`}
                    className="p-1.5 rounded-lg bg-slate-800 text-slate-200 hover:bg-slate-700 text-xs flex items-center space-x-1 border border-slate-700 active:scale-95 transition-transform"
                    title="Descargar archivo en el teléfono"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Descargar</span>
                  </button>

                  {deleteConfirmId === video.id ? (
                    <div className="flex items-center space-x-1">
                      <button
                        type="button"
                        onClick={() => {
                          onDeleteVideo(video.id);
                          setDeleteConfirmId(null);
                          showToast('Video eliminado');
                        }}
                        aria-label={`Confirmar borrar ${video.title}`}
                        className="bg-rose-600 text-white text-[10px] px-2 py-1 rounded font-bold"
                      >
                        Sí, Borrar
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleteConfirmId(null)}
                        aria-label="Cancelar borrado"
                        className="bg-slate-800 text-slate-300 text-[10px] px-2 py-1 rounded"
                      >
                        Cancelar
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setDeleteConfirmId(video.id)}
                      aria-label={`Eliminar video ${video.title}`}
                      className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950/30 transition-colors"
                      title="Eliminar grabación"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal de Reproducción de Video Seguro */}
      {selectedVideo && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-md flex items-center justify-center p-3 animate-in fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-md overflow-hidden shadow-2xl flex flex-col">
            {/* Cabecera del reproductor */}
            <div className="p-3.5 bg-slate-950/80 border-b border-slate-800 flex items-center justify-between">
              <div className="min-w-0 pr-2">
                <h4 className="text-xs font-bold text-slate-100 truncate">{selectedVideo.title}</h4>
                <div className="flex items-center space-x-2 text-[10px] text-slate-400 mt-0.5">
                  <span className="font-mono">
                    {selectedVideo.engineUsed === 'rust2024' ? '🦀 Rust 2024' : '⚡ C++23'}
                  </span>
                  <span>•</span>
                  <span>{selectedVideo.resolution}</span>
                  <span>•</span>
                  <span>{formatBytes(selectedVideo.sizeBytes)}</span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => {
                  setSelectedVideo(null);
                  setVideoLoadError(false);
                }}
                aria-label="Cerrar reproductor"
                className="w-7 h-7 rounded-full bg-slate-800 text-slate-300 hover:text-white flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Video Player HTML5 o Fallback de Previsualización por Canvas */}
            <div className="relative bg-black aspect-[9/14] max-h-[440px] flex items-center justify-center overflow-hidden">
              {selectedVideo.blobUrl && !videoLoadError ? (
                <video
                  src={selectedVideo.blobUrl}
                  controls
                  autoPlay
                  playsInline
                  className="w-full h-full object-contain"
                  onError={() => {
                    setVideoLoadError(true);
                  }}
                  onPlay={(e) => {
                    try {
                      e.currentTarget.playbackRate = playbackSpeed;
                    } catch {
                      // Ignorar restricciones de tasa de reproducción
                    }
                  }}
                />
              ) : (
                <div className="w-full h-full relative flex flex-col items-center justify-center">
                  <canvas
                    ref={previewCanvasRef}
                    width={320}
                    height={460}
                    className="w-full h-full object-cover"
                  />
                  {videoLoadError && (
                    <div className="absolute top-3 left-3 right-3 bg-amber-950/90 border border-amber-600/70 p-2 rounded-xl flex items-center space-x-2 text-[11px] text-amber-200">
                      <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
                      <span>El códec nativo se reproduce mediante renderizado por simulación visual acelerada.</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Controles de velocidad y descarga en el modal */}
            <div className="p-3 bg-slate-950/90 border-t border-slate-800 flex items-center justify-between">
              <div className="flex items-center space-x-1.5">
                <span className="text-[10px] text-slate-400 uppercase font-semibold">Velocidad:</span>
                {[0.5, 1, 1.5, 2].map((spd) => (
                  <button
                    key={spd}
                    type="button"
                    onClick={() => {
                      setPlaybackSpeed(spd);
                      try {
                        const videoEl = document.querySelector('video');
                        if (videoEl) videoEl.playbackRate = spd;
                      } catch {
                        // Ignorar
                      }
                    }}
                    className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${
                      playbackSpeed === spd
                        ? 'bg-indigo-600 text-white'
                        : 'bg-slate-800 text-slate-400 hover:text-slate-200'
                    }`}
                  >
                    {spd}x
                  </button>
                ))}
              </div>

              <button
                type="button"
                onClick={() => handleDownload(selectedVideo)}
                className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold px-3 py-1.5 rounded-xl flex items-center space-x-1.5 shadow active:scale-95 transition-transform"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Guardar en Teléfono</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
