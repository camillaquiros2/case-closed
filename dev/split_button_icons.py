"""One-time cut of the button icon sheet.

Removes the white background, splits the 4x3 grid, and writes each icon
into composeResources/files/images/ui/buttons/.

Usage:
    python dev/split_button_icons.py path/to/sheet.jpg [first|second|third]
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "shared" / "src" / "commonMain" / "composeResources" / "files" / "images" / "ui" / "buttons"

# Left to right, top to bottom, matching each drawn sheet.
SHEETS = {
    "first": [
        ["casos.png", "escena.png", "volver.png", "cerrar.png"],
        ["menu.png", "origen.png", "empezar.png", "reiniciar.png"],
        ["enviar.png", "acusar.png", "seguir.png", "notas.png"],
    ],
    "second": [
        ["anadir_tablero.png", "hipotesis.png", "tab_objetos.png", "tab_sospechosos.png"],
        ["guardar.png", "tab_citas.png", "ver_origen.png", "al_tablero.png"],
        ["enlazar.png", "tab_todos.png", "abrir.png", "confirmar.png"],
    ],
    "third": [
        ["fijar.png", "cancelar.png", "confirmar.png", "reanudar.png"],
        ["subir.png", "bajar.png", "anterior.png", "siguiente.png"],
        ["quitar.png", "cortar.png", None, None],
    ],
}


def is_background(r: int, g: int, b: int) -> bool:
    # Icon fill is beige (~240, 216, 182). Only near-white paper goes away.
    return r >= 248 and g >= 248 and b >= 248


def knock_out(src: Image.Image) -> Image.Image:
    image = src.convert("RGBA")
    pixels = image.load()
    width, height = image.size
    for y in range(height):
        for x in range(width):
            r, g, b, _ = pixels[x, y]
            if is_background(r, g, b):
                pixels[x, y] = (r, g, b, 0)
    return image


def gutters(counts: list[int], min_span: int = 8) -> list[tuple[int, int]]:
    spans: list[tuple[int, int]] = []
    start = None
    for index, count in enumerate(counts):
        empty = count <= 2
        if empty and start is None:
            start = index
        if not empty and start is not None:
            if index - start >= min_span:
                spans.append((start, index))
            start = None
    if start is not None and len(counts) - start >= min_span:
        spans.append((start, len(counts)))
    return spans


def content_bands(image: Image.Image) -> tuple[list[tuple[int, int]], list[tuple[int, int]]]:
    width, height = image.size
    pixels = image.load()
    columns = [0] * width
    rows = [0] * height
    for y in range(height):
        for x in range(width):
            if pixels[x, y][3] > 0:
                columns[x] += 1
                rows[y] += 1

    def bands(gaps: list[tuple[int, int]], limit: int) -> list[tuple[int, int]]:
        edges = [0]
        for begin, end in gaps:
            edges.extend((begin, end))
        edges.append(limit)
        found = []
        for left, right in zip(edges[0::2], edges[1::2]):
            if right - left > 8:
                found.append((left, right))
        return found

    return bands(gutters(columns), width), bands(gutters(rows), height)


def trim(cell: Image.Image, pad: int = 8) -> Image.Image:
    mask = cell.split()[-1].point(lambda value: 255 if value > 0 else 0)
    box = mask.getbbox()
    if box is None:
        return cell
    left, top, right, bottom = box
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(cell.width, right + pad)
    bottom = min(cell.height, bottom + pad)
    return cell.crop((left, top, right, bottom))


def main() -> None:
    if len(sys.argv) not in (2, 3):
        raise SystemExit("usage: python dev/split_button_icons.py path/to/sheet.jpg [first|second]")
    which = sys.argv[2] if len(sys.argv) == 3 else "first"
    names = SHEETS.get(which)
    if names is None:
        raise SystemExit(f"unknown sheet {which!r}; use first, second, or third")
    sheet = knock_out(Image.open(sys.argv[1]))
    col_bands, row_bands = content_bands(sheet)
    expected = (len(names[0]), len(names))
    if (len(col_bands), len(row_bands)) != expected:
        raise SystemExit(f"expected a {expected[0]}x{expected[1]} grid, found {len(col_bands)}x{len(row_bands)}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    written = []
    for row, (top, bottom) in enumerate(row_bands):
        for col, (left, right) in enumerate(col_bands):
            filename = names[row][col]
            if filename is None:
                continue
            icon = trim(sheet.crop((left, top, right, bottom)))
            path = OUT_DIR / filename
            icon.save(path)
            written.append(path.name)
    print(f"Wrote {len(written)} icons to {OUT_DIR}")
    print(", ".join(written))


if __name__ == "__main__":
    main()
