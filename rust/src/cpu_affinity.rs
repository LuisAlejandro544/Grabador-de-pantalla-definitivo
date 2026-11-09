//! cpu_affinity.rs - Afinidad de Hilos hacia Núcleos de Eficiencia (LITTLE) en Linux/Android
//!
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. En arquitecturas ARM big.LITTLE (ej: Helio G81/G85, Snapdragon), ancla mediante
//!    'sched_setaffinity' los hilos de fondo del grabador a los núcleos Cortex-A55 (LITTLE).
//! 2. Reserva los núcleos Cortex-A75 (Big) al 100% para los videojuegos en ejecución,
//!    evitando caídas de cuadros por segundo (FPS) y reduciendo el calentamiento del smartphone.

use std::ffi::CString;
use log::info;

/// Aplica afinidad de hilos hacia núcleos de eficiencia (LITTLE) en procesadores ARM big.LITTLE
pub fn apply_efficiency_core_affinity(thread_name: &str) {
    if let Ok(c_name) = CString::new(thread_name) {
        unsafe {
            libc::pthread_setname_np(libc::pthread_self(), c_name.as_ptr());
        }
    }

    let total_cores = unsafe { libc::sysconf(libc::_SC_NPROCESSORS_CONF) as i32 };
    if total_cores <= 1 {
        return;
    }

    unsafe {
        let mut cpuset: libc::cpu_set_t = std::mem::zeroed();
        libc::CPU_ZERO(&mut cpuset);

        let efficiency_cores = if total_cores >= 8 { 4 } else { (total_cores / 2).max(1) };
        for i in 0..efficiency_cores {
            libc::CPU_SET(i as usize, &mut cpuset);
        }

        let res = libc::sched_setaffinity(0, std::mem::size_of::<libc::cpu_set_t>(), &cpuset);
        if res == 0 {
            info!(
                "Rust 2024: Afinidad aplicada para [{}] hacia {} núcleos LITTLE de {} totales.",
                thread_name, efficiency_cores, total_cores
            );
        }
    }
}
