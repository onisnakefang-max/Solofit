from collections import OrderedDict
from typing import Protocol


class Translator(Protocol):
    def translate(self, text: str) -> str: ...


class ArgosTranslator:
    """Офлайн-перевод en->ru. Языковой пакет (~100 МБ) скачивается при первом запуске."""

    def __init__(self):
        import argostranslate.package
        import argostranslate.translate

        self._translate = argostranslate.translate
        installed = {lang.code for lang in self._translate.get_installed_languages()}
        if not {"en", "ru"} <= installed:
            argostranslate.package.update_package_index()
            packages = argostranslate.package.get_available_packages()
            package = next(p for p in packages if p.from_code == "en" and p.to_code == "ru")
            argostranslate.package.install_from_path(package.download())

    def translate(self, text: str) -> str:
        return self._translate.translate(text, "en", "ru")


class GoogleTranslator:
    """Онлайн-перевод: качество выше, нужен интернет на ПК."""

    def __init__(self):
        from deep_translator import GoogleTranslator as Google

        self._google = Google(source="en", target="ru")

    def translate(self, text: str) -> str:
        return self._google.translate(text)


class CachedTranslator:
    """Не переводит одну и ту же реплику дважды, переживает сбои движка."""

    def __init__(self, inner: Translator, size: int = 512):
        self._inner = inner
        self._size = size
        self._cache: OrderedDict[str, str] = OrderedDict()

    def translate(self, text: str) -> str:
        if text in self._cache:
            self._cache.move_to_end(text)
            return self._cache[text]
        result = self._inner.translate(text)
        self._cache[text] = result
        if len(self._cache) > self._size:
            self._cache.popitem(last=False)
        return result


def make_translator(engine: str) -> Translator:
    engines = {"argos": ArgosTranslator, "google": GoogleTranslator}
    if engine not in engines:
        raise ValueError(f"Неизвестный движок перевода: {engine} (доступны: {', '.join(engines)})")
    return CachedTranslator(engines[engine]())
