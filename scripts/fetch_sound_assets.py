#!/usr/bin/env python3
"""
fetch_sound_assets.py

¿QUÉ HACE ESTE SCRIPT?
Descarga efectos de sonido de interfaz de dominio público (CC0 1.0 Universal,
sin requisitos de atribución ni copyleft) desde GitHub hacia
'android/app/src/main/res/raw/' durante la compilación en GitHub Actions.

SONIDOS INCLUIDOS:
- rec_start.wav      (confirmation_001.wav - sonido limpio de inicio de grabación)
- rec_stop.wav       (close_002.wav        - sonido limpio de finalización)
- countdown_tick.wav (tick_001.wav         - pulso suave de cuenta regresiva)

RESILIENCIA:
Si alguna descarga falla por red, genera un tono WAV limpio de respaldo con
la librería estándar de Python para no interrumpir la compilación del APK.
"""

import math
import os
import struct
import urllib.request
import wave

RAW_DIR = os.path.join("android", "app", "src", "main", "res", "raw")

SOUND_ASSETS = {
    "rec_start.wav": {
        "url": "https://raw.githubusercontent.com/Calinou/kenney-interface-sounds/master/addons/kenney_interface_sounds/confirmation_001.wav",
        "fallback_freq": 880.0,
        "fallback_ms": 140,
    },
    "rec_stop.wav": {
        "url": "https://raw.githubusercontent.com/Calinou/kenney-interface-sounds/master/addons/kenney_interface_sounds/close_002.wav",
        "fallback_freq": 523.25,
        "fallback_ms": 160,
    },
    "countdown_tick.wav": {
        "url": "https://raw.githubusercontent.com/Calinou/kenney-interface-sounds/master/addons/kenney_interface_sounds/tick_001.wav",
        "fallback_freq": 660.0,
        "fallback_ms": 70,
    },
}


def generate_fallback_wav(dest_path: str, freq_hz: float, duration_ms: int) -> None:
    sample_rate = 22050
    num_samples = int(sample_rate * duration_ms / 1000)
    with wave.open(dest_path, "w") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        frames = bytearray()
        for i in range(num_samples):
            env = 1.0 - (i / num_samples)
            val = int(12000 * env * math.sin(2.0 * math.pi * freq_hz * i / sample_rate))
            frames.extend(struct.pack("<h", val))
        wav_file.writeframes(bytes(frames))


def download_sound(filename: str, meta: dict) -> None:
    dest_path = os.path.join(RAW_DIR, filename)
    url = meta["url"]
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "VeloRecStudio-Builder"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            data = resp.read()
        if len(data) < 44 or not data.startswith(b"RIFF"):
            raise ValueError("Archivo descargado no es un WAV válido")
        with open(dest_path, "wb") as f:
            f.write(data)
        print(f"✅ Descargado {dest_path} ({len(data)} bytes) desde {url}")
    except Exception as exc:
        print(f"⚠️ Falló descarga de {filename} ({exc}); generando WAV de respaldo...")
        generate_fallback_wav(dest_path, meta["fallback_freq"], meta["fallback_ms"])
        print(f"✅ Generado respaldo {dest_path}")


def main() -> None:
    os.makedirs(RAW_DIR, exist_ok=True)
    for filename, meta in SOUND_ASSETS.items():
        download_sound(filename, meta)


if __name__ == "__main__":
    main()
