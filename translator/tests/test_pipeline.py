import numpy as np

from xbox_subs.config import Region
from xbox_subs.ocr import clean_text
from xbox_subs.pipeline import Pipeline
from xbox_subs.render import draw_subtitle, wrap, _font
from xbox_subs.stabilizer import Stabilizer
from xbox_subs.translate import CachedTranslator


class FakeOcr:
    def __init__(self, texts):
        self.texts, self.calls = list(texts), 0

    def read(self, image):
        self.calls += 1
        return self.texts.pop(0) if self.texts else ""


class CountingTranslator:
    def __init__(self):
        self.calls = 0

    def translate(self, text):
        self.calls += 1
        return f"RU:{text}"


def frame(value=0):
    return np.full((100, 200, 3), value, dtype=np.uint8)


def make_pipeline(texts, translator=None, **kw):
    ticks = iter(range(1000))
    return Pipeline(FakeOcr(texts), translator or CountingTranslator(), Region(0, 0.5, 1, 0.5),
                    ocr_fps=1, stabilizer=Stabilizer(confirm=2, clear_after=2), clock=lambda: float(next(ticks)), **kw)


def test_translates_stable_text_and_clears():
    p = make_pipeline(["Hello there", "Hello there", "", ""])
    changes = [p.process(frame(i * 50)) for i in range(4)]
    assert changes == [False, True, False, True]
    assert p.russian == ""


def test_translated_when_stable():
    p = make_pipeline(["Hello there", "Hello there"])
    p.process(frame(0))
    p.process(frame(100))
    assert p.english == "Hello there"
    assert p.russian == "RU:Hello there"


def test_static_picture_does_not_rerun_ocr():
    p = make_pipeline(["Hello there"])
    for _ in range(3):
        p.process(frame(10))
    assert p.ocr.calls == 1
    assert p.russian == "RU:Hello there"


def test_ocr_throttled():
    ticks = iter([0.0, 0.1, 0.2, 0.3])
    p = Pipeline(FakeOcr(["a b c"] * 4), CountingTranslator(), Region(), ocr_fps=1, clock=lambda: next(ticks))
    for i in range(4):
        p.process(frame(i * 60))
    assert p.ocr.calls == 1


def test_translator_failure_falls_back_to_english():
    class Broken:
        def translate(self, text):
            raise RuntimeError("offline")

    p = make_pipeline(["Hello there", "Hello there"], translator=Broken())
    p.process(frame(0))
    p.process(frame(100))
    assert p.russian == "Hello there"


def test_cache_translates_once():
    inner = CountingTranslator()
    cached = CachedTranslator(inner)
    assert cached.translate("Hi") == cached.translate("Hi")
    assert inner.calls == 1


def test_region_crop():
    assert Region(0.5, 0.5, 0.5, 0.5).crop(frame()).shape[:2] == (50, 100)


def test_region_parse_validates():
    assert Region.parse("0.1,0.7,0.8,0.25").w == 0.8
    for bad in ["1,2,3", "0.5,0.5,0.8,0.8", "-1,0,1,1"]:
        try:
            Region.parse(bad)
        except ValueError:
            continue
        raise AssertionError(bad)


def test_clean_text():
    assert clean_text("  Hello \n  world ") == "Hello world"
    assert clean_text("12 ::") == ""


def test_render_russian_subtitle_draws_pixels():
    img = np.zeros((720, 1280, 3), dtype=np.uint8)
    out = draw_subtitle(img, "Привет, это длинный русский субтитр, который должен переноситься на несколько строк")
    assert out.shape == img.shape and out.max() == 255 and img.max() == 0
    assert out[:300].max() == 0  # субтитр только внизу


def test_wrap_respects_width():
    font = _font(30)
    lines = wrap("раз два три четыре пять шесть семь восемь девять десять", font, 200)
    assert len(lines) > 1 and all(font.getlength(l) <= 200 for l in lines)


def test_clean_text_drops_symbol_noise():
    assert clean_text("We leave [ before | they find us") == "We leave before they find us"
