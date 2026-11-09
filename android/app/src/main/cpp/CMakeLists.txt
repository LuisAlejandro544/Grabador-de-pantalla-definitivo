# CMakeLists.txt para compilar el motor C++23 con Android NDK
# Soporta arquitecturas de 64 bits (arm64-v8a, x86_64) y 32 bits (armeabi-v7a).

cmake_minimum_required(VERSION 3.22.1)
project("velorec_cpp23" LANGUAGES CXX)

# Exigir C++23 explícitamente según especificación del usuario
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Opciones de optimización y avisos del compilador Clang
add_compile_options(
    -Wall
    -Wextra
    -Wpedantic
    -O3
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

# Definir biblioteca compartida .so para aceleración visual y conversiones SIMD NEON
add_library(velorec_cpp23 SHARED
    libyuv_bridge.cpp
    GpuShaderPipeline.cpp
)

# Inclusiones de encabezados
target_include_directories(velorec_cpp23 PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Enlace con bibliotecas esenciales del NDK de Android
# mediandk: AMediaCodec, AMediaMuxer, AMediaFormat
# android: ANativeWindow, ANativeWindow_toSurface
# log: __android_log_print
# GLESv3 & EGL: Tubería de sombreadores GPU (GLSL ES 3.20 y SPIR-V)
target_link_libraries(velorec_cpp23 PRIVATE
    android
    mediandk
    log
    GLESv3
    EGL
)

# Integración con Google Oboe (AAR Prefab) para captura y procesado de audio en C++23
find_package(oboe CONFIG QUIET)
if(oboe_FOUND)
    target_link_libraries(velorec_cpp23 PRIVATE oboe::oboe)
endif()

# Integración con Google libyuv (Licencia BSD) para transformaciones de color aceleradas por hardware
find_package(libyuv CONFIG QUIET)
if(libyuv_FOUND)
    target_link_libraries(velorec_cpp23 PRIVATE libyuv::yuv)
    target_compile_definitions(velorec_cpp23 PRIVATE HAS_LIBYUV=1)
endif()

# Optimización de tamaño eliminando símbolos no utilizados
set_target_properties(velorec_cpp23 PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--as-needed"
)
