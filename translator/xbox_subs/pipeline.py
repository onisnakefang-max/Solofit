import time
from typing import Callable, Iterable, Iterator, Optional

import numpy as np

from .config import Region
from .ocr import Ocr
from .stabilizer import Stabilizer
from .translate import Translator


class Pipeline:
    """Кадры -> (OCR не чаще ocr_fps) -> стабилизация -> перевод."""

    def __init__(self, ocr: Ocr, translator: Translator, region: Region, ocr_fps: float = 4.0,
                 stabilizer: Optional[Stabilizer] = None, clock: Callable[[], float] = time.monotonic,
                 change_threshold: float = 2.0):
        self.ocr = ocr
        self.translator = translator
        self.region = region
        self.stabilizer = stabilizer or Stabilizer()
        self._interval = 1.0 / ocr_fps
        self._clock = clock
        self._change_threshold = change_threshold
        self._last_ocr = float("-inf")
        self._last_crop: Optional[np.ndarray] = None
        self._last_text = ""
        self.russian = ""  # текущий русский субтитр ('' = ничего не показывать)
        self.english = ""

    def process(self, frame: np.ndarray) -> bool:
        """Обрабатывает кадр. True, если русский субтитр изменился."""
        now = self._clock()
        if now - self._last_ocr < self._interval:
            return False
        self._last_ocr = now

        crop = self.region.crop(frame)
        if self._last_crop is not None and self._last_crop.shape == crop.shape:
            diff = np.abs(crop.astype(np.int16) - self._last_crop.astype(np.int16)).mean()
            if diff < self._change_threshold:
                text = self._last_text  # картинка не менялась — OCR не нужен
            else:
                text = self.ocr.read(crop)
        else:
            text = self.ocr.read(crop)
        self._last_crop, self._last_text = crop, text

        stable = self.stabilizer.feed(text)
        if stable is None:
            return False
        self.english = stable
        try:
            self.russian = self.translator.translate(stable) if stable else ""
        except Exception as error:  # сеть/движок упали — не роняем поток видео
            print(f"[перевод] ошибка: {error}")
            self.russian = stable
        return True

    def run(self, source: Iterable[np.ndarray]) -> Iterator[np.ndarray]:
        for frame in source:
            self.process(frame)
            yield frame
