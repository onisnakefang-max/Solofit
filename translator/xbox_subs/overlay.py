import queue
import threading
from typing import Callable


class Overlay:
    """Прозрачное окно поверх всех окон с русским субтитром (Windows; tkinter входит в Python).

    Запускайте игру/плеер захвата в режиме «окно без рамки», тогда оверлей виден поверх видео.
    """

    def __init__(self, worker: Callable[[Callable[[str], None]], None]):
        self._worker = worker
        self._texts: "queue.Queue[str]" = queue.Queue()

    def run(self) -> None:
        import tkinter as tk

        root = tk.Tk()
        root.overrideredirect(True)
        root.attributes("-topmost", True)
        transparent = "#010101"
        root.configure(bg=transparent)
        try:
            root.attributes("-transparentcolor", transparent)
        except tk.TclError:
            root.attributes("-alpha", 0.85)  # не Windows: просто полупрозрачное окно
        width, height = root.winfo_screenwidth(), 180
        root.geometry(f"{width}x{height}+0+{root.winfo_screenheight() - height - 60}")
        label = tk.Label(root, text="", fg="white", bg=transparent, font=("Segoe UI", 28, "bold"),
                         wraplength=int(width * 0.8), justify="center")
        label.pack(expand=True)
        root.bind("<Escape>", lambda _e: root.destroy())

        def poll() -> None:
            try:
                while True:
                    label.config(text=self._texts.get_nowait())
            except queue.Empty:
                pass
            root.after(50, poll)

        threading.Thread(target=self._worker, args=(self._texts.put,), daemon=True).start()
        poll()
        root.mainloop()
