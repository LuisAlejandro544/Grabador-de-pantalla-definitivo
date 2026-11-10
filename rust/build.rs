// build.rs - Enlace estricto de librerías nativas del Android NDK y C++ STL (libc++_shared)
fn main() {
    println!("cargo:rustc-link-lib=mediandk");
    println!("cargo:rustc-link-lib=android");
    println!("cargo:rustc-link-lib=log");
    // Enlace explícito a libc++_shared.so para resolver la rutina de excepciones __gxx_personality_v0
    // requerida por la biblioteca Google Oboe (C++) en Android
    println!("cargo:rustc-link-lib=c++_shared");
}
