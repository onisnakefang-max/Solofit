from difflib import SequenceMatcher
from typing import Optional


def similar(a: str, b: str, threshold: float) -> bool:
    return SequenceMatcher(None, a.lower(), b.lower()).ratio() >= threshold


class Stabilizer:
    """Превращает шумные результаты OCR в стабильный текст субтитра.

    Текст принимается, когда его увидели `confirm` раз подряд. Мелкие ошибки OCR
    (похожесть >= similarity) не считаются новой репликой. Субтитр гаснет после
    `clear_after` пустых чтений.
    """

    def __init__(self, confirm: int = 2, clear_after: int = 3, similarity: float = 0.85):
        self.confirm = confirm
        self.clear_after = clear_after
        self.similarity = similarity
        self.current = ""
        self._candidate = ""
        self._seen = 0
        self._empty = 0

    def feed(self, text: str) -> Optional[str]:
        """Возвращает новый стабильный текст ('' = убрать субтитр) или None, если ничего не изменилось."""
        if not text:
            self._candidate, self._seen = "", 0
            self._empty += 1
            if self.current and self._empty >= self.clear_after:
                self.current = ""
                return ""
            return None

        self._empty = 0
        if self.current and similar(text, self.current, self.similarity):
            self._candidate, self._seen = "", 0
            return None

        if self._candidate and similar(text, self._candidate, self.similarity):
            self._seen += 1
        else:
            self._candidate, self._seen = text, 1

        if self._seen >= self.confirm:
            self.current, self._candidate, self._seen = self._candidate, "", 0
            return self.current
        return None
