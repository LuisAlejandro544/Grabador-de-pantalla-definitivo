#!/usr/bin/env python3
"""
generate_launcher_pngs.py

¿QUÉ HACE ESTE SCRIPT?
Genera los archivos PNG del icono de la aplicación ('ic_launcher.png' e
'ic_launcher_round.png') en todas las densidades estándar de Android
(mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) dentro de 'android/app/src/main/res/'.

MODO AUTÓNOMO SIN DEPENDENCIAS EXTERNAS:
1. Lee los colores desde 'colors.xml' y los paths desde 'ic_launcher_background.xml'
   e 'ic_launcher_foreground.xml'.
2. Si 'rsvg-convert' está disponible en el sistema, lo utiliza.
3. Si no está disponible (evitando depender de 'apt-get' en GitHub Actions),
   renderiza los PNGs RGBA con antialiasing por supersampling (4x) usando
   únicamente la librería estándar de Python ('zlib', 'struct', 'math').
"""

import math
import os
import shutil
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zlib

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def load_colors(colors_path: str) -> dict[str, str]:
    colors: dict[str, str] = {}
    if not os.path.exists(colors_path):
        return colors
    tree = ET.parse(colors_path)
    for elem in tree.getroot().findall("color"):
        name = elem.attrib.get("name")
        if name and elem.text:
            colors[f"@color/{name}"] = elem.text.strip()
    return colors


def hex_to_rgba(hex_color: str) -> tuple[int, int, int, int]:
    h = hex_color.lstrip("#")
    if len(h) == 6:
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)
    if len(h) == 8:
        return (int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16), int(h[0:2], 16))
    return (0, 0, 0, 255)


def extract_paths(vector_xml_path: str, colors: dict[str, str]) -> list[tuple[str, str]]:
    tree = ET.parse(vector_xml_path)
    paths: list[tuple[str, str]] = []
    for elem in tree.getroot().findall(".//path"):
        fill = elem.attrib.get(f"{ANDROID_NS}fillColor", "#000000")
        fill = colors.get(fill, fill)
        path_data = elem.attrib.get(f"{ANDROID_NS}pathData", "")
        if path_data:
            paths.append((fill, path_data))
    return paths


def build_svg(
    bg_paths: list[tuple[str, str]],
    fg_paths: list[tuple[str, str]],
    round_mask: bool,
) -> str:
    clip_shape = (
        '<circle cx="54" cy="54" r="36" />'
        if round_mask
        else '<rect x="18" y="18" width="72" height="72" rx="16" ry="16" />'
    )

    path_elements = []
    for fill, d in bg_paths + fg_paths:
        path_elements.append(f'    <path fill="{fill}" d="{d}" />')
    paths_str = "\n".join(path_elements)

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="512" height="512">
  <defs>
    <clipPath id="launcherClip">
      {clip_shape}
    </clipPath>
  </defs>
  <g clip-path="url(#launcherClip)">
{paths_str}
  </g>
</svg>
'''


def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    chunk = chunk_type + data
    return struct.pack("!I", len(data)) + chunk + struct.pack("!I", zlib.crc32(chunk) & 0xFFFFFFFF)


def _write_png_rgba(path: str, width: int, height: int, pixels: bytearray) -> None:
    raw_rows = bytearray()
    stride = width * 4
    for y in range(height):
        raw_rows.append(0)  # Filter type 0 (None)
        raw_rows.extend(pixels[y * stride : (y + 1) * stride])

    ihdr = struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_rows), level=9)
    png_bytes = (
        b"\x89PNG\r\n\x1a\n"
        + _png_chunk(b"IHDR", ihdr)
        + _png_chunk(b"IDAT", idat)
        + _png_chunk(b"IEND", b"")
    )
    with open(path, "wb") as f:
        f.write(png_bytes)


def _point_in_triangle(px: float, py: float, ax: float, ay: float, bx: float, by: float, cx: float, cy: float) -> bool:
    d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
    d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
    d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
    has_neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    has_pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (has_neg and has_pos)


def _inside_rounded_rect(vx: float, vy: float, x0: float, y0: float, w: float, h: float, r: float) -> bool:
    if vx < x0 or vx > x0 + w or vy < y0 or vy > y0 + h:
        return False
    cx = min(max(vx, x0 + r), x0 + w - r)
    cy = min(max(vy, y0 + r), y0 + h - r)
    dx = vx - cx
    dy = vy - cy
    return (dx * dx + dy * dy) <= (r * r)


def render_png_python(
    out_path: str,
    size: int,
    colors: dict[str, str],
    round_mask: bool,
) -> None:
    bg_dark = hex_to_rgba(colors.get("@color/surface_dark", "#131a29"))
    indigo = (99, 102, 241, 255)
    red = (239, 68, 68, 255)
    white = (255, 255, 255, 255)

    ss = 4  # 4x4 supersampling para antialiasing nítido
    inv_samples = 1.0 / (ss * ss)
    pixels = bytearray(size * size * 4)

    for py in range(size):
        for px in range(size):
            acc_r = acc_g = acc_b = acc_a = 0
            for sy in range(ss):
                vy = 18.0 + ((py + (sy + 0.5) / ss) / size) * 72.0
                for sx in range(ss):
                    vx = 18.0 + ((px + (sx + 0.5) / ss) / size) * 72.0

                    if round_mask:
                        dx = vx - 54.0
                        dy = vy - 54.0
                        inside = (dx * dx + dy * dy) <= (36.0 * 36.0)
                    else:
                        inside = _inside_rounded_rect(vx, vy, 18.0, 18.0, 72.0, 72.0, 16.0)

                    if not inside:
                        continue

                    dx = vx - 54.0
                    dy = vy - 54.0
                    dist2 = dx * dx + dy * dy

                    if _point_in_triangle(vx, vy, 49.0, 46.0, 63.0, 54.0, 49.0, 62.0):
                        r, g, b, a = white
                    elif dist2 <= 18.0 * 18.0:
                        r, g, b, a = red
                    elif dist2 <= 26.0 * 26.0:
                        r, g, b, a = indigo
                    else:
                        r, g, b, a = bg_dark

                    acc_r += r
                    acc_g += g
                    acc_b += b
                    acc_a += a

            idx = (py * size + px) * 4
            pixels[idx] = int(acc_r * inv_samples)
            pixels[idx + 1] = int(acc_g * inv_samples)
            pixels[idx + 2] = int(acc_b * inv_samples)
            pixels[idx + 3] = int(acc_a * inv_samples)

    _write_png_rgba(out_path, size, size, pixels)


def main() -> None:
    res_dir = os.path.join("android", "app", "src", "main", "res")
    colors = load_colors(os.path.join(res_dir, "values", "colors.xml"))
    bg_paths = extract_paths(os.path.join(res_dir, "drawable", "ic_launcher_background.xml"), colors)
    fg_paths = extract_paths(os.path.join(res_dir, "drawable", "ic_launcher_foreground.xml"), colors)

    rsvg_bin = shutil.which("rsvg-convert")

    if rsvg_bin:
        square_svg = build_svg(bg_paths, fg_paths, round_mask=False)
        round_svg = build_svg(bg_paths, fg_paths, round_mask=True)

        with tempfile.TemporaryDirectory() as tmpdir:
            square_svg_path = os.path.join(tmpdir, "ic_launcher.svg")
            round_svg_path = os.path.join(tmpdir, "ic_launcher_round.svg")

            with open(square_svg_path, "w", encoding="utf-8") as f:
                f.write(square_svg)
            with open(round_svg_path, "w", encoding="utf-8") as f:
                f.write(round_svg)

            for folder, size in DENSITIES.items():
                out_dir = os.path.join(res_dir, folder)
                os.makedirs(out_dir, exist_ok=True)
                out_square = os.path.join(out_dir, "ic_launcher.png")
                out_round = os.path.join(out_dir, "ic_launcher_round.png")
                subprocess.run(
                    [rsvg_bin, "-w", str(size), "-h", str(size), square_svg_path, "-o", out_square],
                    check=True,
                )
                subprocess.run(
                    [rsvg_bin, "-w", str(size), "-h", str(size), round_svg_path, "-o", out_round],
                    check=True,
                )
                print(f"✅ Generados (rsvg-convert) {out_square} y {out_round} ({size}x{size})")
    else:
        for folder, size in DENSITIES.items():
            out_dir = os.path.join(res_dir, folder)
            os.makedirs(out_dir, exist_ok=True)
            out_square = os.path.join(out_dir, "ic_launcher.png")
            out_round = os.path.join(out_dir, "ic_launcher_round.png")
            render_png_python(out_square, size, colors, round_mask=False)
            render_png_python(out_round, size, colors, round_mask=True)
            print(f"✅ Generados (Python nativo) {out_square} y {out_round} ({size}x{size})")


if __name__ == "__main__":
    main()
