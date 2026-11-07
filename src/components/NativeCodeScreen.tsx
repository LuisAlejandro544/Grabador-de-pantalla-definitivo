import React, { useState } from 'react';
import {
  Code2,
  Download,
  Copy,
  Check,
  Folder,
  FileCode,
  FileText,
  Layers,
  ArrowRight,
  ExternalLink,
  Cpu
} from 'lucide-react';
import { NATIVE_FILES_CATALOG, NativeFileItem } from '../utils/nativeCodeCatalog';
import { exportNativeProjectZip, downloadBlob } from '../utils/zipExport';

export const NativeCodeScreen: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<NativeFileItem>(NATIVE_FILES_CATALOG[0]);
  const [copied, setCopied] = useState<boolean>(false);
  const [isExporting, setIsExporting] = useState<boolean>(false);
  const [exportSuccess, setExportSuccess] = useState<boolean>(false);

  // Categorías únicas
  const categories = Array.from(new Set(NATIVE_FILES_CATALOG.map((f) => f.category)));
  const [selectedCategory, setSelectedCategory] = useState<string>(categories[0]);

  const filesInSelectedCategory = NATIVE_FILES_CATALOG.filter((f) => f.category === selectedCategory);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(selectedFile.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback
    }
  };

  const handleDownloadZip = async () => {
    setIsExporting(true);
    setExportSuccess(false);
    try {
      const blob = await exportNativeProjectZip();
      downloadBlob(blob, 'VeloRec_Studio_Native_RN_Kotlin_CPP_Rust.zip');
      setExportSuccess(true);
      setTimeout(() => setExportSuccess(false), 4000);
    } catch (e) {
      console.error(e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-60px)] pb-28 px-4 pt-4 max-w-lg mx-auto w-full space-y-6">
      {/* Encabezado */}
      <div className="border-b border-slate-800 pb-3 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center space-x-2">
            <Code2 className="w-5 h-5 text-indigo-400" />
            <span>Código Nativo & Exportación</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Explora el código fuente real de React Native, Kotlin, C++23 y Rust (Edition 2024).
          </p>
        </div>
      </div>

      {/* Botón de Exportar Todo en ZIP para Móvil */}
      <div className="bg-gradient-to-r from-indigo-950 via-slate-900 to-indigo-950 border border-indigo-500/40 rounded-2xl p-4 shadow-xl flex flex-col space-y-3">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-xl bg-indigo-600/30 border border-indigo-500/50 flex items-center justify-center text-indigo-300">
            <Download className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold text-slate-100">Exportar Proyecto Completo</h3>
            <p className="text-[11px] text-slate-400">
              Descarga en tu teléfono el archivo .ZIP con Gradle, CMake, Cargo y código fuente.
            </p>
          </div>
        </div>

        <button
          type="button"
          disabled={isExporting}
          onClick={handleDownloadZip}
          className={`w-full py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center space-x-2 transition-all shadow-lg active:scale-95 ${
            exportSuccess
              ? 'bg-emerald-600 text-white'
              : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-600/30'
          }`}
        >
          {isExporting ? (
            <>
              <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>COMPRIMIENDO PROYECTO NATIVO...</span>
            </>
          ) : exportSuccess ? (
            <>
              <Check className="w-4 h-4 stroke-[3]" />
              <span>¡ARCHIVO ZIP DESCARGADO CON ÉXITO!</span>
            </>
          ) : (
            <>
              <Download className="w-4 h-4" />
              <span>DESCARGAR PROYECTO NATIVO (.ZIP)</span>
            </>
          )}
        </button>
      </div>

      {/* Tarjeta: Compilador en la Nube con GitHub Actions (APK Debug) */}
      <div className="bg-slate-900/90 border border-emerald-500/30 rounded-2xl p-4 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
            <h3 className="text-xs font-bold text-slate-100 font-mono">GitHub Action: APK Debug (Manual)</h3>
          </div>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-950/80 text-emerald-300 border border-emerald-500/40">
            Manual 100%
          </span>
        </div>

        <p className="text-xs text-slate-300 leading-relaxed">
          Flujo automatizado listo en <code className="bg-slate-950 px-1 py-0.5 rounded font-mono text-emerald-300">.github/workflows/build-apk.yml</code>.
          Está configurado para ejecutarse <strong>únicamente bajo demanda</strong> con el botón <em>"Run workflow"</em> desde tu celular.
        </p>

        <div className="bg-slate-950/80 rounded-xl p-3 border border-slate-800 text-[11px] text-slate-400 space-y-1.5">
          <div className="flex items-center space-x-1.5 text-slate-200 font-semibold">
            <span>📱 Pasos para compilar desde tu teléfono:</span>
          </div>
          <div>1. Sube este proyecto o el ZIP a tu repositorio de GitHub.</div>
          <div>2. En GitHub desde tu móvil, entra a la pestaña <strong>Actions</strong>.</div>
          <div>3. Selecciona <strong>Compilar APK Debug</strong> y pulsa <strong>Run workflow</strong>.</div>
          <div>4. GitHub compila React Native, Kotlin, C++23 y Rust 2024, firma con <code>debug.keystore</code> y te entrega el APK listo para descargar e instalar.</div>
        </div>
      </div>

      {/* Diagrama de Flujo Arquitectónico */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-xl space-y-3">
        <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider flex items-center space-x-1.5">
          <Layers className="w-4 h-4 text-indigo-400" />
          <span>Flujo de Datos Zero-Copy (Sin Copia)</span>
        </h3>

        <div className="flex flex-col space-y-2 text-xs">
          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div>
              <span className="font-bold text-indigo-300">1. React Native (TypeScript)</span>
              <p className="text-[10px] text-slate-400">Interfaz de usuario, botones y ajustes de resolución</p>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-600" />
          </div>

          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div>
              <span className="font-bold text-indigo-300">2. Kotlin Module & Coroutines</span>
              <p className="text-[10px] text-slate-400">Gestión de permisos MediaProjection en segundo plano (Dispatchers.IO)</p>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-600" />
          </div>

          <div className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div>
              <span className="font-bold text-indigo-300">3. Android Foreground Service</span>
              <p className="text-[10px] text-slate-400">Tipo mediaProjection obligatorio en Android 14+ (API 34/35)</p>
            </div>
            <ArrowRight className="w-4 h-4 text-slate-600" />
          </div>

          <div className="p-2.5 rounded-xl bg-gradient-to-r from-cyan-950/60 to-amber-950/60 border border-slate-700 flex items-center justify-between">
            <div>
              <span className="font-bold text-slate-100">4. Motor Nativo (C++23 o Rust 2024)</span>
              <p className="text-[10px] text-slate-300">Entrega Surface directa al GPU: cero saturación de memoria en Java</p>
            </div>
            <Cpu className="w-4 h-4 text-cyan-400" />
          </div>
        </div>
      </div>

      {/* Selector de Categorías (Pills) */}
      <div className="flex space-x-1.5 overflow-x-auto pb-1 scrollbar-none">
        {categories.map((cat) => (
          <button
            key={cat}
            type="button"
            onClick={() => {
              setSelectedCategory(cat);
              const firstInCat = NATIVE_FILES_CATALOG.find((f) => f.category === cat);
              if (firstInCat) setSelectedFile(firstInCat);
            }}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all border ${
              selectedCategory === cat
                ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                : 'bg-slate-900/80 text-slate-400 border-slate-800 hover:text-slate-200'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Selector de Archivos dentro de la Categoría */}
      <div className="flex space-x-2 overflow-x-auto pb-1">
        {filesInSelectedCategory.map((file) => (
          <button
            key={file.id}
            type="button"
            onClick={() => setSelectedFile(file)}
            className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl text-xs font-mono font-medium border transition-all ${
              selectedFile.id === file.id
                ? 'bg-slate-800 text-indigo-300 border-indigo-500/80 ring-1 ring-indigo-500/50'
                : 'bg-slate-900/70 text-slate-400 border-slate-800 hover:bg-slate-800/50'
            }`}
          >
            <FileCode className="w-3.5 h-3.5 shrink-0" />
            <span className="truncate max-w-[160px]">{file.name}</span>
          </button>
        ))}
      </div>

      {/* Visor de Código Fuente */}
      <div className="bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col">
        {/* Barra de cabecera del visor */}
        <div className="bg-slate-900/90 border-b border-slate-800 p-3 flex items-center justify-between">
          <div className="min-w-0 pr-2">
            <div className="flex items-center space-x-2">
              <span className="text-xs font-mono font-bold text-slate-200 truncate">{selectedFile.name}</span>
              <span className="text-[10px] font-mono uppercase px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
                {selectedFile.language}
              </span>
            </div>
            <p className="text-[10px] text-slate-400 font-mono mt-0.5 truncate">{selectedFile.path}</p>
          </div>

          <button
            type="button"
            onClick={handleCopyCode}
            className="flex items-center space-x-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs px-2.5 py-1.5 rounded-lg border border-slate-700 transition-colors"
            title="Copiar código al portapapeles"
          >
            {copied ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" />
                <span className="text-emerald-300 text-[11px] font-medium">¡Copiado!</span>
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" />
                <span className="text-[11px]">Copiar</span>
              </>
            )}
          </button>
        </div>

        {/* Descripción didáctica del archivo */}
        <div className="bg-indigo-950/20 border-b border-indigo-500/20 px-3.5 py-2 text-xs text-indigo-300/90 leading-relaxed">
          {selectedFile.description}
        </div>

        {/* Bloque de código con scroll táctil */}
        <div className="p-3.5 overflow-x-auto max-h-[380px] font-mono text-xs text-slate-300 leading-relaxed">
          <pre className="whitespace-pre">
            <code>{selectedFile.code}</code>
          </pre>
        </div>
      </div>
    </div>
  );
};
