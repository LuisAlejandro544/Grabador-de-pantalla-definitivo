// build.rs - Enlace estricto de librerías nativas del Android NDK
fn main() {
    println!("cargo:rustc-link-lib=mediandk");
    println!("cargo:rustc-link-lib=android");
    println!("cargo:rustc-link-lib=log");
}
