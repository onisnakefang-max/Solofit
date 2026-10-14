from dataclasses import dataclass


@dataclass(frozen=True)
class Region:
    """Область кадра в долях 0..1: где игра рисует субтитры."""

    x: float = 0.10
    y: float = 0.72
    w: float = 0.80
    h: float = 0.25

    @classmethod
    def parse(cls, text: str) -> "Region":
        parts = [float(p) for p in text.split(",")]
        if len(parts) != 4:
            raise ValueError("region: нужно 4 числа x,y,w,h")
        region = cls(*parts)
        if not (0 <= region.x < 1 and 0 <= region.y < 1 and region.w > 0 and region.h > 0
                and region.x + region.w <= 1.0001 and region.y + region.h <= 1.0001):
            raise ValueError("region: значения должны быть в долях 0..1 и не выходить за кадр")
        return region

    def crop(self, frame):
        height, width = frame.shape[:2]
        x0, y0 = int(self.x * width), int(self.y * height)
        x1, y1 = int((self.x + self.w) * width), int((self.y + self.h) * height)
        return frame[y0:y1, x0:x1]
