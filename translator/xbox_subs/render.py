import os
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageFont

_FONT_CANDIDATES = [
    r"C:\Windows\Fonts\segoeui.ttf",
    r"C:\Windows\Fonts\arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]


@lru_cache(maxsize=8)
def _font(size: int) -> ImageFont.FreeTypeFont:
    for path in _FONT_CANDIDATES:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    raise RuntimeError("Не найден шрифт с кириллицей. Укажите путь в _FONT_CANDIDATES.")


def wrap(text: str, font, max_width: int) -> list[str]:
    """Перенос по словам под заданную ширину в пикселях."""
    lines: list[str] = []
    line = ""
    for word in text.split():
        trial = f"{line} {word}".strip()
        if line and font.getlength(trial) > max_width:
            lines.append(line)
            line = word
        else:
            line = trial
    if line:
        lines.append(line)
    return lines


def draw_subtitle(frame_bgr: np.ndarray, text: str) -> np.ndarray:
    """Рисует русский субтитр внизу кадра (белый текст, чёрная обводка)."""
    if not text:
        return frame_bgr
    height, width = frame_bgr.shape[:2]
    size = max(18, height // 22)
    font = _font(size)
    lines = wrap(text, font, int(width * 0.8))

    image = Image.fromarray(frame_bgr[:, :, ::-1])
    draw = ImageDraw.Draw(image)
    line_height = int(size * 1.25)
    y = height - int(height * 0.06) - line_height * len(lines)
    for line in lines:
        x = (width - font.getlength(line)) / 2
        draw.text((x, y), line, font=font, fill="white", stroke_width=max(2, size // 12), stroke_fill="black")
        y += line_height
    return np.asarray(image)[:, :, ::-1].copy()
