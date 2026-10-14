import argparse

from .config import Region
from .pipeline import Pipeline


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="xbox_subs", description="Русские субтитры для английских игр (Xbox через HDMI-захват)")
    p.add_argument("--source", default="0", help="номер карты захвата (0,1,..), 'screen', видео- или фото-файл")
    p.add_argument("--region", default="0.10,0.72,0.80,0.25",
                   help="область субтитров в долях кадра x,y,w,h (по умолчанию нижняя четверть)")
    p.add_argument("--engine", default="argos", choices=["argos", "google"], help="движок перевода")
    p.add_argument("--display", default="preview", choices=["preview", "overlay", "console"],
                   help="preview: окно с видео и субтитрами; overlay: прозрачный оверлей; console: только текст")
    p.add_argument("--ocr-fps", type=float, default=4.0, help="сколько раз в секунду читать текст")
    return p


def main() -> None:
    args = build_parser().parse_args()
    from .capture import frames
    from .ocr import RapidOcr
    from .translate import make_translator

    pipeline = Pipeline(RapidOcr(), make_translator(args.engine), Region.parse(args.region), args.ocr_fps)

    if args.display == "console":
        shown = None
        for _ in pipeline.run(frames(args.source)):
            if pipeline.russian != shown:
                shown = pipeline.russian
                if shown:
                    print(f"EN: {pipeline.english}\nRU: {shown}\n", flush=True)
        return

    if args.display == "overlay":
        from .overlay import Overlay

        def worker(show):
            for _ in pipeline.run(frames(args.source)):
                show(pipeline.russian)

        Overlay(worker).run()
        return

    import cv2
    from .render import draw_subtitle

    for frame in pipeline.run(frames(args.source)):
        cv2.imshow("xbox_subs (Esc - выход)", draw_subtitle(frame, pipeline.russian))
        if cv2.waitKey(1) == 27:
            break
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
