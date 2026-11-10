"""Lámina del caso Blackwood: del escritorio a la acusación.

Compone los retratos y objetos del juego en un diagrama para explicar
por qué saber el nombre no basta. La lógica sale del expediente de Case001.

    python dev/diagrama_caso.py
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "shared" / "src" / "commonMain" / "composeResources" / "files" / "images" / "cases" / "case_001"
OUT = Path(__file__).resolve().parent / "caso_001_hacia_la_resolucion.png"

W, H = 2200, 4200
MARGIN = 64

PAPER = (244, 236, 223)
INK = (46, 34, 30)
BURG = (112, 36, 40)
WAX = (142, 48, 48)
GOLD = (168, 128, 62)
CREAM = (252, 247, 238)
CARD = (250, 244, 232)
RULE = (196, 176, 150)
MUTED = (102, 84, 74)
GREEN = (42, 96, 76)
GREEN_BG = (226, 234, 224)
GOLD_BG = (244, 232, 206)
RED_BG = (246, 228, 222)
WHITE = (255, 252, 247)


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


SERIF = "C:/Windows/Fonts/georgia.ttf"
SERIF_B = "C:/Windows/Fonts/georgiab.ttf"
SANS = "C:/Windows/Fonts/segoeui.ttf"
SANS_B = "C:/Windows/Fonts/segoeuib.ttf"

F_KICKER = font(SANS_B, 22)
F_TITLE = font(SERIF_B, 58)
F_SUB = font(SERIF, 28)
F_H = font(SERIF_B, 34)
F_BODY = font(SANS, 24)
F_BODY_B = font(SANS_B, 24)
F_SMALL = font(SANS, 20)
F_SMALL_B = font(SANS_B, 20)
F_TINY = font(SANS, 17)
F_BADGE = font(SANS_B, 16)
F_TIME = font(SERIF_B, 26)


def wrap(text: str, face: ImageFont.FreeTypeFont, width: int) -> list[str]:
    lines: list[str] = []
    for paragraph in text.split("\n"):
        words = paragraph.split()
        if not words:
            lines.append("")
            continue
        current = words[0]
        for word in words[1:]:
            trial = f"{current} {word}"
            if face.getlength(trial) <= width:
                current = trial
            else:
                lines.append(current)
                current = word
        lines.append(current)
    return lines


def text_height(lines: list[str], face: ImageFont.FreeTypeFont, gap: int) -> int:
    if not lines:
        return 0
    step = face.size + gap
    return step * len(lines) - gap


def draw_lines(
    draw: ImageDraw.ImageDraw,
    lines: list[str],
    face: ImageFont.FreeTypeFont,
    x: int,
    y: int,
    fill: tuple[int, int, int],
    gap: int = 7,
) -> int:
    step = face.size + gap
    for line in lines:
        draw.text((x, y), line, font=face, fill=fill)
        y += step
    return y


def rounded(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], radius: int, fill) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill)


def cover(path: Path, w: int, h: int, focus: float = 0.32) -> Image.Image:
    image = Image.open(path).convert("RGB")
    scale = max(w / image.width, h / image.height)
    resized = image.resize((max(1, round(image.width * scale)), max(1, round(image.height * scale))), Image.Resampling.LANCZOS)
    left = max(0, (resized.width - w) // 2)
    top = max(0, round((resized.height - h) * focus))
    top = min(top, max(0, resized.height - h))
    return resized.crop((left, top, left + w, top + h))


def paste_round(canvas: Image.Image, image: Image.Image, xy: tuple[int, int], radius: int) -> None:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width, image.height), radius=radius, fill=255)
    canvas.paste(image, xy, mask)


def arrow(draw: ImageDraw.ImageDraw, x1: int, y1: int, x2: int, y2: int, fill=GOLD, width: int = 5) -> None:
    draw.line((x1, y1, x2, y2), fill=fill, width=width)
    angle = math.atan2(y2 - y1, x2 - x1)
    length = 16
    spread = 0.45
    points = [
        (x2, y2),
        (x2 - length * math.cos(angle - spread), y2 - length * math.sin(angle - spread)),
        (x2 - length * math.cos(angle + spread), y2 - length * math.sin(angle + spread)),
    ]
    draw.polygon(points, fill=fill)


def badge(draw: ImageDraw.ImageDraw, text: str, x: int, y: int, fill, ink=WHITE) -> int:
    pad_x, pad_y = 12, 6
    tw = int(F_BADGE.getlength(text))
    box = (x, y, x + tw + pad_x * 2, y + F_BADGE.size + pad_y * 2)
    rounded(draw, box, 8, fill)
    draw.text((x + pad_x, y + pad_y - 1), text, font=F_BADGE, fill=ink)
    return box[2]


def section_title(draw: ImageDraw.ImageDraw, number: str, title: str, y: int) -> int:
    draw.ellipse((MARGIN, y + 4, MARGIN + 42, y + 46), fill=BURG)
    num_w = F_BODY_B.getlength(number)
    draw.text((MARGIN + 21 - num_w / 2, y + 8), number, font=F_BODY_B, fill=WHITE)
    draw.text((MARGIN + 58, y + 4), title, font=F_H, fill=INK)
    return y + 64


def main() -> None:
    canvas = Image.new("RGB", (W, H), PAPER)
    draw = ImageDraw.Draw(canvas)
    content_w = W - MARGIN * 2

    rounded(draw, (0, 0, W, 214), 0, BURG)
    draw.rectangle((0, 206, W, 214), fill=GOLD)
    draw.text((MARGIN, 28), "DETECTIVE DE MESA   ·   LÁMINA PARA LEER EL CASO", font=F_KICKER, fill=GOLD)
    draw.text((MARGIN, 62), "Del escritorio a la acusación", font=F_TITLE, fill=WHITE)
    draw.text((MARGIN, 142), "El envenenamiento de la Mansión Blackwood", font=F_SUB, fill=(244, 220, 196))
    draw.text((MARGIN, 178), "CC-1924-014    ·    14 de noviembre de 1924    ·    Mansión Blackwood, Devon", font=F_SMALL, fill=(230, 196, 176))

    y = 242
    intro = (
        "Lord Arthur aparece muerto sobre el escritorio poco después de las once. "
        "La copa tiene belladona, hay cuatro personas en la casa y un testamento debía firmarse a medianoche. "
        "Se puede nombrar al culpable y aun así perder: el jefe devuelve el expediente si en el corcho no están las tres fichas que sostienen el cargo."
    )
    lines = wrap(intro, F_BODY, content_w - 48)
    box_h = 28 + text_height(lines, F_BODY, 8) + 24
    rounded(draw, (MARGIN, y, W - MARGIN, y + box_h), 16, CREAM)
    draw_lines(draw, lines, F_BODY, MARGIN + 24, y + 20, INK, 8)
    y += box_h + 28

    y = section_title(draw, "1", "La noche, en orden", y)
    beats = [
        ("10:40", "Graves sirve el brandy y sale a las 10:42. Arthur sigue vivo."),
        ("10:45", "Reid entra al estudio con el maletín. Eleanor lo ve en el pasillo."),
        ("Luego", "Alguien vierte en la copa ya servida la tintura que falta en el frasco."),
        ("11:17", "Graves vuelve, encuentra el cuerpo y da la alarma."),
        ("00:00", "Iba a firmarse el codicilo que cancela la renta de Reid."),
    ]
    gap = 14
    beat_w = (content_w - gap * 4) // 5
    beat_h = 168
    for index, (time, sentence) in enumerate(beats):
        x = MARGIN + index * (beat_w + gap)
        rounded(draw, (x, y, x + beat_w, y + beat_h), 14, CREAM)
        draw.rectangle((x, y, x + 8, y + beat_h), fill=GOLD if index != 2 else WAX)
        draw.text((x + 20, y + 14), time, font=F_TIME, fill=BURG)
        body = wrap(sentence, F_SMALL, beat_w - 36)
        draw_lines(draw, body, F_SMALL, x + 20, y + 54, INK, 5)
    y += beat_h + 34

    y = section_title(draw, "2", "Cuatro personas. Solo una tocó la copa.", y)
    people = [
        (
            "suspect_eleanor.png",
            "Lady Eleanor",
            "La viuda",
            "DESCARTADA",
            GREEN,
            "Parece fría y es la esposa. Su fortuna ya estaba protegida y la tierra de los zapatos es del invernadero. No hay huellas suyas en la copa.",
        ),
        (
            "suspect_julian.png",
            "Julian Vance",
            "El sobrino",
            "DESCARTADO",
            GREEN,
            "Debe dinero y el testamento le quita la herencia. Estaba encerrado en el billar: Graves tuvo que abrirle. El telegrama no toca la copa.",
        ),
        (
            "suspect_morris.png",
            "Morris Graves",
            "El mayordomo",
            "DESCARTADO",
            GREEN,
            "Él sirvió la bandeja, así que parece el primero. Sus huellas están en el asa, no en el borde. Encontró el cuerpo; no preparó el veneno.",
        ),
        (
            "suspect_reid.png",
            "Dr. Alistair Reid",
            "El médico",
            "LA ACUSACIÓN",
            WAX,
            "Entra después del brandy, el borde de la copa es suyo y faltan dos dracmas de su frasco. A medianoche perdía la renta de por vida.",
        ),
    ]
    card_gap = 16
    card_w = (content_w - card_gap * 3) // 4
    photo_h = 210
    card_h = 430
    for index, (file_name, name, role, stamp, stamp_color, blurb) in enumerate(people):
        x = MARGIN + index * (card_w + card_gap)
        rounded(draw, (x, y, x + card_w, y + card_h), 16, CREAM if stamp != WAX else (255, 246, 240))
        photo = cover(ART / "suspects" / file_name, card_w - 20, photo_h, 0.18)
        paste_round(canvas, photo, (x + 10, y + 10), 12)
        draw = ImageDraw.Draw(canvas)
        badge(draw, stamp, x + 16, y + photo_h - 8, stamp_color)
        draw.text((x + 16, y + photo_h + 28), name, font=F_BODY_B, fill=INK)
        draw.text((x + 16, y + photo_h + 58), role, font=F_SMALL, fill=MUTED)
        blurb_lines = wrap(blurb, F_SMALL, card_w - 32)
        draw_lines(draw, blurb_lines, F_SMALL, x + 16, y + photo_h + 96, INK, 5)
    y += card_h + 36

    y = section_title(draw, "3", "El camino que el juego obliga a recorrer", y)
    lead = wrap(
        "Se lee de arriba hacia abajo. Una flecha es una puerta: lo de abajo no aparece si falta lo de arriba.",
        F_BODY,
        content_w,
    )
    y = draw_lines(draw, lead, F_BODY, MARGIN, y, MUTED, 6)
    y += 16

    starts = [
        ("item_brandy.png", "La copa ya está", "Llega con el expediente. Sin ella no se pueden marcar el residuo ni las huellas del borde."),
        ("item_pillbox.png", "El pastillero ya está", "También llega con el expediente. Sirve para descartarlo. No es la dosis que mata."),
    ]
    start_gap = 18
    start_w = (content_w - start_gap) // 2
    start_h = 132
    for index, (image_name, title, body) in enumerate(starts):
        x = MARGIN + index * (start_w + start_gap)
        rounded(draw, (x, y, x + start_w, y + start_h), 14, CREAM)
        icon = cover(ART / "items" / image_name, 100, 100, 0.4)
        paste_round(canvas, icon, (x + 16, y + 16), 10)
        draw = ImageDraw.Draw(canvas)
        draw.text((x + 132, y + 18), title, font=F_BODY_B, fill=INK)
        draw_lines(draw, wrap(body, F_SMALL, start_w - 156), F_SMALL, x + 132, y + 54, MUTED, 4)
    y += start_h + 8
    arrow(draw, W // 2, y, W // 2, y + 28)
    y += 36
    draw.text((MARGIN, y), "En el archivo se marcan estas frases", font=F_SMALL_B, fill=INK)
    y += 36

    quotes = [
        (GREEN, "Descarta el pastillero", "La forense", "Los signos son de belladona, no de la digitalina del pastillero. Las tabletas están intactas."),
        (GOLD, "Pide la copa", "El laboratorio", "El residuo de la copa es belladona concentrada. Junto con la frase de Graves, abre el frasco."),
        (BURG, "No pide nada previo", "Graves", "Reid entró al salir él, a tomarle el pulso. Eleanor lo confirma: lo vio en el pasillo a las 10:45."),
    ]
    quote_gap = 16
    quote_w = (content_w - quote_gap * 2) // 3
    quote_h = 188
    for index, (tone, stamp, title, body) in enumerate(quotes):
        x = MARGIN + index * (quote_w + quote_gap)
        rounded(draw, (x, y, x + quote_w, y + quote_h), 14, CREAM)
        draw.rectangle((x, y, x + quote_w, y + 8), fill=tone)
        badge(draw, stamp, x + 16, y + 22, tone)
        draw.text((x + 16, y + 64), title, font=F_BODY_B, fill=INK)
        draw_lines(draw, wrap(body, F_SMALL, quote_w - 32), F_SMALL, x + 16, y + 102, INK, 5)
    y += quote_h + 12
    note = "La del laboratorio y la de Graves, juntas, abren el frasco. La de la forense solo descarta."
    note_w = int(F_SMALL.getlength(note))
    draw.text(((W - note_w) / 2, y), note, font=F_SMALL, fill=MUTED)
    y += 32
    arrow(draw, W // 2, y, W // 2, y + 22)
    y += 30

    gate_h = 176
    rounded(draw, (MARGIN, y, W - MARGIN, y + gate_h), 16, BURG)
    thumb = cover(ART / "items" / "item_vial.png", 148, 144, 0.45)
    paste_round(canvas, thumb, (MARGIN + 16, y + 16), 12)
    draw = ImageDraw.Draw(canvas)
    draw.text((MARGIN + 186, y + 16), "La puerta del frasco", font=F_H, fill=WHITE)
    gate_lines = wrap(
        "Reid solo responde por el frasco ámbar cuando ya están marcadas las dos frases de arriba: el residuo en la copa, y que él entró al estudio. El frasco sale entonces del maletín. Faltan dos dracmas, la misma cantidad vertida en el brandy.",
        F_SMALL,
        content_w - 230,
    )
    draw_lines(draw, gate_lines, F_SMALL, MARGIN + 186, y + 66, (255, 236, 220), 5)
    y += gate_h + 10
    arrow(draw, W // 2, y, W // 2, y + 28)
    y += 36
    draw.text((MARGIN, y), "Para que el jefe archive la acusación hacen falta las cuatro", font=F_SMALL_B, fill=INK)
    y += 36

    pins = [
        ("item_brandy.png", "1  ·  La copa", "Prueba que el veneno iba en el brandy ya servido."),
        ("item_vial.png", "2  ·  El frasco", "Prueba que la dosis salió del maletín de Reid."),
        (None, "3  ·  Graves", "Prueba que Reid entró después de servida la copa."),
        (None, "4  ·  El nombre", "La acusación tiene que decir Dr. Alistair Reid."),
    ]
    pin_gap = 16
    pin_w = (content_w - pin_gap * 3) // 4
    pin_h = 248
    for index, (image_name, title, body) in enumerate(pins):
        x = MARGIN + index * (pin_w + pin_gap)
        fill = RED_BG if index == 3 else CREAM
        rounded(draw, (x, y, x + pin_w, y + pin_h), 14, fill)
        if image_name:
            icon = cover(ART / "items" / image_name, pin_w - 32, 110, 0.42)
            paste_round(canvas, icon, (x + 16, y + 14), 10)
            draw = ImageDraw.Draw(canvas)
        elif index == 2:
            rounded(draw, (x + 16, y + 14, x + pin_w - 16, y + 124), 10, GOLD_BG)
            quote = wrap("«Reid entró mientras yo salía.»", F_SMALL_B, pin_w - 64)
            draw_lines(draw, quote, F_SMALL_B, x + 32, y + 48, BURG, 4)
        else:
            rounded(draw, (x + 16, y + 14, x + pin_w - 16, y + 124), 10, (236, 214, 208))
            draw.text((x + 28, y + 48), "Dr. Reid", font=F_H, fill=WAX)
        draw.text((x + 16, y + 146), title, font=F_SMALL_B, fill=BURG)
        draw_lines(draw, wrap(body, F_SMALL, pin_w - 32), F_SMALL, x + 16, y + 178, INK, 4)
    y += pin_h + 14
    warn = wrap(
        "Con el nombre correcto y una sola de las tres fichas sin fijar, el jefe devuelve el expediente. No detiene a nadie por una corazonada.",
        F_SMALL,
        content_w,
    )
    y = draw_lines(draw, warn, F_SMALL, MARGIN, y, WAX, 5)
    y += 28

    y = section_title(draw, "4", "Lo que parece prueba y no cierra el caso", y)
    decoys = [
        ("item_pillbox.png", "El pastillero", "Está abierto, lleva las iniciales del doctor y guarda digitalina. Las tabletas están intactas. Es la distracción."),
        ("item_telegram.png", "El telegrama", "Prueba la deuda de Julian. No tiene veneno ni relación con la copa."),
        ("item_lamp.png", "La lámpara", "Sigue caliente. Aceite de casa, sin belladona. Se puede recoger y no acusa a nadie."),
    ]
    decoy_w = (content_w - 32) // 3
    decoy_h = 150
    for index, (image_name, title, body) in enumerate(decoys):
        x = MARGIN + index * (decoy_w + 16)
        rounded(draw, (x, y, x + decoy_w, y + decoy_h), 14, CREAM)
        icon = cover(ART / "items" / image_name, 92, 118, 0.35)
        paste_round(canvas, icon, (x + 14, y + 16), 10)
        draw = ImageDraw.Draw(canvas)
        draw.text((x + 120, y + 16), title, font=F_SMALL_B, fill=INK)
        draw_lines(draw, wrap(body, F_TINY, decoy_w - 140), F_TINY, x + 120, y + 48, MUTED, 4)
    y += decoy_h + 16
    extra = wrap(
        "También se pueden marcar las huellas del borde y el codicilo de medianoche. Sirven para entender el caso. El jefe no las pide para archivar la acusación.",
        F_SMALL,
        content_w,
    )
    y = draw_lines(draw, extra, F_SMALL, MARGIN, y, MUTED, 5)
    y += 28

    y = section_title(draw, "5", "Cómo queda cerrado", y)
    verdict = (
        "Reid envenenó el brandy con la belladona de su frasco después de que Graves saliera del estudio. "
        "Sus huellas están en el borde de la copa. El pastillero fue una distracción. "
        "Arthur estaba a pocas horas de cancelar la asignación, y Reid no podía dejar que aquel papel se firmara."
    )
    v_lines = wrap(verdict, F_BODY, content_w - 48)
    v_h = 24 + text_height(v_lines, F_BODY, 8) + 20
    rounded(draw, (MARGIN, y, W - MARGIN, y + v_h), 16, BURG)
    draw_lines(draw, v_lines, F_BODY, MARGIN + 24, y + 18, WHITE, 8)
    y += v_h + 22

    footer = "Si el nombre es otro, el expediente vuelve: Eleanor no tenía nada que perder, Julian no pudo salir del billar, y Graves solo tocó la bandeja."
    y = draw_lines(draw, wrap(footer, F_SMALL, content_w), F_SMALL, MARGIN, y, MUTED, 5)

    canvas.crop((0, 0, W, y + 48)).save(OUT, "PNG")
    print(OUT)


if __name__ == "__main__":
    main()
