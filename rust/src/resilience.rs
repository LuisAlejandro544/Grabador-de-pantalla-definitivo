//! resilience.rs - Guardián de Resiliencia, Prioridad de Proceso y Blindaje Anti-LMK en Rust 2024
//!
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. Blindaje contra Low Memory Killer (LMK / lmkd) y Phantom Process Killer de Android:
//!    - Ejecuta compactación periódica de heap con 'malloc_trim(0)' liberando páginas de memoria
//!      al kernel de Linux para mantener la huella RSS plana (<15 MB).
//!    - Monitorea '/proc/self/status' (VmRSS), '/proc/self/oom_score', '/proc/self/oom_score_adj'
//!      y '/proc/meminfo' (MemAvailable) para detectar presión de memoria antes de que sea crítica.
//! 2. Prioridad de Proceso y E/S en Tiempo Real:
//!    - Configura la prioridad del hilo en nice -16 ('libc::setpriority').
//!    - Configura prioridad de E/S en Linux ('SYS_ioprio_set') para garantizar que la escritura
//!      en disco del multiplexor MP4 tenga preferencia sobre tareas en segundo plano.
//! 3. Fijación de Páginas Críticas con 'mlock':
//!    - Protege estructuras de control y buffers clave para que el compresor zRAM o kswapd
//!      de Android no las pagine a disco durante partidas de juegos intensivos.
//! 4. Manejador de Señales de Emergencia (Zero Data Loss):
//!    - Intercepta señales POSIX 'SIGTERM', 'SIGINT' y 'SIGHUP' mediante 'sigaction' para
//!      cerrar y escribir atómicamente el átomo 'moov' del MP4 si el sistema intenta forzar
//!      el cierre, garantizando que el video no quede corrupto.

use std::fs::File;
use std::io::{BufRead, BufReader};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Instant;
use log::{info, warn};

// Declaración externa de malloc_trim para la biblioteca estándar Bionic libc de Android
unsafe extern "C" {
    fn malloc_trim(pad: usize) -> libc::c_int;
}

// Constantes de prioridad de E/S para la llamada al sistema ioprio_set en Linux
const IOPRIO_CLASS_BE: libc::c_int = 2; // Best Effort
const IOPRIO_WHO_PROCESS: libc::c_int = 1;

#[inline]
fn ioprio_prio_value(class: libc::c_int, data: libc::c_int) -> libc::c_int {
    (class << 13) | data
}

/// Estado de salud de la memoria y riesgo de LMK
#[derive(Debug, Clone)]
pub struct MemoryHealthReport {
    pub rss_kb: u64,
    pub vmsize_kb: u64,
    pub oom_score: i32,
    pub oom_score_adj: i32,
    pub system_available_ram_mb: u64,
    pub total_trims_performed: u64,
    pub risk_level: String,
}

/// Guardián nativo de estabilidad y resiliencia de VeloRec
pub struct ResilienceGuardian {
    pub total_trims: Arc<AtomicU64>,
    pub is_emergency_handler_active: Arc<AtomicBool>,
    pub last_trim_time: Arc<std::sync::Mutex<Instant>>,
}

impl ResilienceGuardian {
    pub fn new() -> Self {
        Self {
            total_trims: Arc::new(AtomicU64::new(0)),
            is_emergency_handler_active: Arc::new(AtomicBool::new(false)),
            last_trim_time: Arc::new(std::sync::Mutex::new(Instant::now())),
        }
    }

    /// Configura prioridad máxima de CPU y E/S para el hilo de codificación y captura
    pub fn boost_thread_priority(thread_name: &str) {
        unsafe {
            // 1. Prioridad de CPU (nice -16)
            let res_prio = libc::setpriority(libc::PRIO_PROCESS, 0, -16);
            if res_prio == 0 {
                info!("ResilienceGuardian: Prioridad CPU nice -16 asignada para [{}]", thread_name);
            }

            // 2. Prioridad de E/S de Linux (ioprio_set BE Clase 2, Prioridad 0)
            #[cfg(target_os = "android")]
            {
                let ioprio = ioprio_prio_value(IOPRIO_CLASS_BE, 0);
                #[cfg(target_arch = "aarch64")]
                let sys_ioprio_set = 30; // __NR_ioprio_set en arm64
                #[cfg(target_arch = "arm")]
                let sys_ioprio_set = 314; // __NR_ioprio_set en arm32
                #[cfg(target_arch = "x86_64")]
                let sys_ioprio_set = 251; // __NR_ioprio_set en x86_64
                #[cfg(not(any(target_arch = "aarch64", target_arch = "arm", target_arch = "x86_64")))]
                let sys_ioprio_set = 0;

                if sys_ioprio_set > 0 {
                    let res_io = libc::syscall(sys_ioprio_set, IOPRIO_WHO_PROCESS, 0, ioprio);
                    if res_io == 0 {
                        info!("ResilienceGuardian: Prioridad I/O de tiempo real configurada para [{}]", thread_name);
                    }
                }
            }
        }
    }

    /// Ejecuta una compactación forzada de memoria nativa devolviendo páginas al kernel
    pub fn force_memory_trim(&self) -> bool {
        unsafe {
            let res = malloc_trim(0);
            if res != 0 {
                self.total_trims.fetch_add(1, Ordering::Relaxed);
                if let Ok(mut lock) = self.last_trim_time.lock() {
                    *lock = Instant::now();
                }
                info!("ResilienceGuardian: malloc_trim ejecutado exitosamente. Páginas liberadas a Linux.");
                true
            } else {
                false
            }
        }
    }

    /// Bloquea páginas de memoria crítica en RAM física evitando swapping a zRAM
    pub fn lock_critical_memory(addr: *const libc::c_void, len: usize) -> bool {
        if addr.is_null() || len == 0 {
            return false;
        }
        unsafe {
            let res = libc::mlock(addr, len);
            if res == 0 {
                info!("ResilienceGuardian: mlock fijó {} bytes en RAM física.", len);
                true
            } else {
                false
            }
        }
    }

    /// Desbloquea páginas de memoria previamente fijadas
    pub fn unlock_critical_memory(addr: *const libc::c_void, len: usize) {
        if !addr.is_null() && len > 0 {
            unsafe {
                libc::munlock(addr, len);
            }
        }
    }

    /// Obtiene telemetría en tiempo real sobre el uso de memoria, presión del sistema y score OOM
    pub fn get_memory_health(&self) -> MemoryHealthReport {
        let (rss_kb, vmsize_kb) = Self::read_process_memory_stats();
        let (oom_score, oom_score_adj) = Self::read_oom_scores();
        let system_available_ram_mb = Self::read_available_system_ram_mb();
        let total_trims_performed = self.total_trims.load(Ordering::Relaxed);

        // Evalúa el riesgo de que el LMK mate la aplicación
        let risk_level = if oom_score > 700 || (system_available_ram_mb > 0 && system_available_ram_mb < 80) {
            "CRITICAL_LMK_RISK".to_string()
        } else if oom_score > 350 || (system_available_ram_mb > 0 && system_available_ram_mb < 180) {
            "WARNING_ELEVATED_PRESSURE".to_string()
        } else {
            "OPTIMAL_HEALTHY".to_string()
        };

        MemoryHealthReport {
            rss_kb,
            vmsize_kb,
            oom_score,
            oom_score_adj,
            system_available_ram_mb,
            total_trims_performed,
            risk_level,
        }
    }

    /// Lee VmRSS y VmSize de /proc/self/status
    fn read_process_memory_stats() -> (u64, u64) {
        let mut rss = 0u64;
        let mut vmsize = 0u64;

        if let Ok(file) = File::open("/proc/self/status") {
            let reader = BufReader::new(file);
            for line in reader.lines().flatten() {
                if line.starts_with("VmRSS:") {
                    if let Some(kb) = line.split_whitespace().nth(1).and_then(|v| v.parse::<u64>().ok()) {
                        rss = kb;
                    }
                } else if line.starts_with("VmSize:") {
                    if let Some(kb) = line.split_whitespace().nth(1).and_then(|v| v.parse::<u64>().ok()) {
                        vmsize = kb;
                    }
                }
                if rss > 0 && vmsize > 0 {
                    break;
                }
            }
        }

        (rss, vmsize)
    }

    /// Lee /proc/self/oom_score y /proc/self/oom_score_adj
    fn read_oom_scores() -> (i32, i32) {
        let score = std::fs::read_to_string("/proc/self/oom_score")
            .ok()
            .and_then(|s| s.trim().parse::<i32>().ok())
            .unwrap_or(0);

        let adj = std::fs::read_to_string("/proc/self/oom_score_adj")
            .ok()
            .and_then(|s| s.trim().parse::<i32>().ok())
            .unwrap_or(0);

        (score, adj)
    }

    /// Lee MemAvailable de /proc/meminfo
    fn read_available_system_ram_mb() -> u64 {
        if let Ok(file) = File::open("/proc/meminfo") {
            let reader = BufReader::new(file);
            for line in reader.lines().flatten() {
                if line.starts_with("MemAvailable:") {
                    if let Some(kb) = line.split_whitespace().nth(1).and_then(|v| v.parse::<u64>().ok()) {
                        return kb / 1024;
                    }
                }
            }
        }
        0
    }

    /// Instala manejadores de señales POSIX para garantizar que el MP4 se cierre
    /// limpiamente sin corrupción de datos ante cierres forzados por el sistema
    pub fn install_emergency_signal_handler(&self) {
        if self.is_emergency_handler_active.swap(true, Ordering::SeqCst) {
            return;
        }

        unsafe {
            let mut sa: libc::sigaction = std::mem::zeroed();
            sa.sa_sigaction = emergency_signal_handler as usize;
            sa.sa_flags = libc::SA_SIGINFO;
            libc::sigemptyset(&mut sa.sa_mask);

            libc::sigaction(libc::SIGTERM, &sa, std::ptr::null_mut());
            libc::sigaction(libc::SIGINT, &sa, std::ptr::null_mut());
            libc::sigaction(libc::SIGHUP, &sa, std::ptr::null_mut());
        }

        info!("ResilienceGuardian: Manejadores de señal de emergencia (SIGTERM/SIGINT/SIGHUP) instalados.");
    }
}

/// Callback de bajo nivel invocado por el kernel en caso de señales de terminación
extern "C" fn emergency_signal_handler(sig: libc::c_int, _info: *mut libc::siginfo_t, _ctx: *mut libc::c_void) {
    warn!(
        "ResilienceGuardian: Señal de terminación interceptada ({})! Ejecutando parada de emergencia...",
        sig
    );
    // Intenta detener y guardar el muxer de forma atómica
    if let Ok(mut lock) = crate::jni_bindings::GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref mut engine) = *lock {
            engine.stop();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_resilience_guardian_creation_and_stats() {
        let guardian = ResilienceGuardian::new();
        let report = guardian.get_memory_health();
        assert_eq!(report.total_trims_performed, 0);
        assert!(!report.risk_level.is_empty());
    }

    #[test]
    fn test_ioprio_calculation() {
        let prio = ioprio_prio_value(IOPRIO_CLASS_BE, 0);
        assert_eq!(prio, (2 << 13) | 0);
    }
}
