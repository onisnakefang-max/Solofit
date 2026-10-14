import re
from typing import Protocol

import numpy as np

_LETTERS = re.compile(r"[A-Za-z]")


class Ocr(Protocol):
    def read(self, image: np.ndarray) -> str: ...


def clean_text(text: str) -> str:
    """Склеивает строки, убирает мусор. Пустая строка, если это не похоже на субтитр."""
    # токены без букв и цифр ("[", "|", "—") — шум детектора
    text = " ".join(t for t in text.split() if re.search(r"\w", t))
    if len(_LETTERS.findall(text)) < 3:
        return ""
    return text


class RapidOcr:
    """Локальный OCR (ONNX). Хорошо читает белый текст с тёмной обводкой, как в играх."""

    def __init__(self, min_confidence: float = 0.6):
        from rapidocr_onnxruntime import RapidOCR

        self._engine = RapidOCR()
        self._min_confidence = min_confidence

    def read(self, image: np.ndarray) -> str:
        result, _ = self._engine(image)
        if not result:
            return ""
        # (box, text, score) -> сверху вниз, слева направо
        lines = [(box[0][1], box[0][0], text) for box, text, score in result
                 if float(score) >= self._min_confidence]
        lines.sort()
        return clean_text(" ".join(text for _, _, text in lines))
