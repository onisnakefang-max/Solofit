import os
import sys
import time
from typing import Iterator

import numpy as np


def frames(source: str) -> Iterator[np.ndarray]:
    """Отдаёт BGR-кадры. source: номер карты захвата ('0'), 'screen', файл видео или картинки."""
    import cv2

    if source == "screen":
        yield from _screen_frames()
        return

    if source.isdigit():
        backend = cv2.CAP_DSHOW if sys.platform == "win32" else cv2.CAP_ANY
        cap = cv2.VideoCapture(int(source), backend)
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1920)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 1080)
    elif os.path.splitext(source)[1].lower() in (".png", ".jpg", ".jpeg", ".bmp"):
        image = cv2.imread(source)
        if image is None:
            raise RuntimeError(f"Не удалось прочитать картинку: {source}")
        while True:
            yield image
            time.sleep(0.25)
    else:
        cap = cv2.VideoCapture(source)

    if not cap.isOpened():
        raise RuntimeError(f"Не удалось открыть источник видео: {source}")
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                return
            yield frame
    finally:
        cap.release()


def _screen_frames() -> Iterator[np.ndarray]:
    import cv2
    import mss

    with mss.mss() as grabber:
        monitor = grabber.monitors[1]
        while True:
            shot = np.array(grabber.grab(monitor))
            yield cv2.cvtColor(shot, cv2.COLOR_BGRA2BGR)
