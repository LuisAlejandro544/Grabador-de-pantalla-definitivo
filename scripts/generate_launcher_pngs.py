#!/usr/bin/env python3
"""
generate_launcher_pngs.py

¿QUÉ HACE ESTE SCRIPT?
Lee los recursos vectoriales nativos de Android ('colors.xml', 'ic_launcher_background.xml'
e 'ic_launcher_foreground.xml'), los combina en un SVG de alta precisión y genera
los archivos PNG ('ic_launcher.png' e 'ic_launcher_round.png') en todas las densidades
estándar de Android (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) usando 'rsvg-convert'.

Se ejecuta en GitHub Actions durante la compilación del APK Debug y Release para
empaquetar los PNGs en todos sus tamaños sin subir binarios al repositorio.
"""

import os
import subprocess
import tempfile
import xml.etree.ElementTree as ET

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
    # Adaptive icons tienen safe-zone de 72x72 dentro de 108x108; recortamos al área visible (18..90)
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


def main() -> None:
    res_dir = os.path.join("android", "app", "src", "main", "res")
    colors = load_colors(os.path.join(res_dir, "values", "colors.xml"))
    bg_paths = extract_paths(os.path.join(res_dir, "drawable", "ic_launcher_background.xml"), colors)
    fg_paths = extract_paths(os.path.join(res_dir, "drawable", "ic_launcher_foreground.xml"), colors)

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
                ["rsvg-convert", "-w", str(size), "-h", str(size), square_svg_path, "-o", out_square],
                check=True,
            )
            subprocess.run(
                ["rsvg-convert", "-w", str(size), "-h", str(size), round_svg_path, "-o", out_round],
                check=True,
            )
            print(f"✅ Generados {out_square} y {out_round} ({size}x{size})")


if __name__ == "__main__":
    main()
