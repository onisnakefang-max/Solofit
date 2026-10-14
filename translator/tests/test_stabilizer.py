from xbox_subs.stabilizer import Stabilizer


def test_accepts_text_after_confirmations():
    s = Stabilizer(confirm=2)
    assert s.feed("Hello there") is None
    assert s.feed("Hello there") == "Hello there"


def test_ocr_jitter_is_not_new_line():
    s = Stabilizer(confirm=2)
    s.feed("Hello there, friend")
    s.feed("Hello there, friend")
    assert s.feed("He1lo there, friend") is None
    assert s.current == "Hello there, friend"


def test_new_line_replaces_old():
    s = Stabilizer(confirm=2)
    s.feed("Hello there")
    s.feed("Hello there")
    assert s.feed("Run away now!") is None
    assert s.feed("Run away now!") == "Run away now!"


def test_clears_after_empty_reads():
    s = Stabilizer(confirm=1, clear_after=3)
    assert s.feed("Hello there") == "Hello there"
    assert s.feed("") is None
    assert s.feed("") is None
    assert s.feed("") == ""
    assert s.feed("") is None


def test_one_off_garbage_ignored():
    s = Stabilizer(confirm=2)
    assert s.feed("xqz") is None
    assert s.feed("Real line here") is None
    assert s.current == ""
