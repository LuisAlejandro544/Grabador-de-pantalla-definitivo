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

# Definir biblioteca compartida .so
add_library(velorec_cpp23 SHARED
    NativeRecorderEngine.cpp
)

# Inclusiones de encabezados
target_include_directories(velorec_cpp23 PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Enlace con bibliotecas esenciales del NDK de Android
# mediandk: AMediaCodec, AMediaMuxer, AMediaFormat
# android: ANativeWindow, ANativeWindow_toSurface
# log: __android_log_print
target_link_libraries(velorec_cpp23 PRIVATE
    android
    mediandk
    log
)

# Optimización de tamaño eliminando símbolos no utilizados
set_target_properties(velorec_cpp23 PROPERTIES
    LINK_FLAGS "-Wl,--gc-sections -Wl,--as-needed"
)
