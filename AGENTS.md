# 🤖 Guía Operativa para Agentes de IA — VeloRec Studio

Instrucciones de operación, comandos de prueba y protocolos de modificación para agentes y desarrolladores automatizados que trabajen en este repositorio.

---

## 🎯 Protocolo de Actuación

Antes de realizar cualquier cambio en el código:
1. **Razonar antes de ejecutar**: Analizar el impacto en la arquitectura de cuatro capas (React Native -> Kotlin -> JNI -> C++23/Rust 2024).
2. **Respetar la restricción móvil**: Recordar que el usuario final opera **desde un teléfono**. La interfaz debe ser ergonómica para interacción táctil y no se debe exigir el uso de una terminal local en el dispositivo.
3. **No alterar el diseño gráfico sin solicitud explícita**: Mantener paleta de colores, iconos, tipografía y navegación existentes.
4. **No modificar `commit_message.txt`**: Si existe dicho archivo, no actualizarlo a menos que el usuario lo solicite expresamente.

---

## 🛠️ Roles de Desarrollo Aplicables

Este proyecto implementa el ciclo estructurado de 7 fases:

| Rol | Función en el Proyecto |
|---|---|
| **01. El Arquitecto** | Planifica la estructura entre Kotlin Coroutines, NDK y React Native Specs. |
| **02. El Constructor** | Escribe código modular, tipado y listo para producción sin placeholders. |
| **03. El Detective** | Diagnostica bugs en el ciclo de vida de `MediaProjection`, JNI o Web APIs. |
| **04. El Crítico** | Revisa la seguridad de memoria (zero-copy), permisos y eficiencia de hilos. |
| **05. El Optimizador** | Refactoriza cálculos matemáticos y reduce la latencia por cuadro. |
| **06. El Escudo** | Mantiene la suite de tests con `node:test` y `node:assert/strict`. |
| **07. El Narrador** | Documenta APIs, manifiestos y flujos paso a paso para el usuario. |

---

## 🧪 Comandos de Verificación Obligatorios

Cualquier cambio de código debe validarse ejecutando en orden:

```bash
# 1. Pruebas Unitarias de Lógica Pura (desacoplada del DOM)
npx --yes tsx --test src/utils/recorderCore.test.ts

# 2. Verificación de Tipos TypeScript (sin compilar native_source móvil)
npm run lint

# 3. Compilación de Producción del Applet Web
npm run build
```

---

## ⚠️ Puntos Críticos y Trampas Comunes

- **`actions/setup-node@v4` en GitHub Actions**:
  - **NUNCA** añadir `cache: 'npm'` ni `cache: 'gradle'` a menos que un lockfile (`package-lock.json`) esté garantizado en la raíz del repositorio. De lo contrario, la acción fallará inmediatamente.
- **Disparo del GitHub Action**:
  - Debe mantenerse configurado **únicamente** con `on: workflow_dispatch`. No añadir `push` ni `pull_request` para evitar ejecuciones innecesarias que consuman cuotas de GitHub.
- **Paridad en Resoluciones**:
  - Los codificadores de hardware NDK (`AMediaCodec`) en Android fallarán con error si el ancho o alto son números impares (ej: 1081x1920). Cualquier función que modifique resoluciones debe aplicar `width % 2 === 0 && height % 2 === 0`.
- **Corrutinas en Kotlin**:
  - Toda llamada al NDK (`nativeInitCppRecorder`, `nativeInitRustRecorder`, `nativeStartCppEncoding`) debe ejecutarse en `Dispatchers.IO`, nunca en el hilo principal de la UI.
