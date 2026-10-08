"""コマ撮り(shots/screenshots/frames/f000.png ...)をつないで、動きの動画(WebP と GIF)を作る。

使い方: python3 scripts/make-motion.py <コマのフォルダ> <出力先(拡張子なし)>
"""
import glob
import os
import sys

from PIL import Image

FRAME_MS = 33


def main(frames_dir: str, out_base: str) -> None:
    paths = sorted(glob.glob(os.path.join(frames_dir, "f*.png")))
    if not paths:
        print("no frames")
        return
    frames = [Image.open(p).convert("RGB") for p in paths]
    # 最後のコマで少し止める
    durations = [FRAME_MS] * (len(frames) - 1) + [1200]

    # WebP: 全コマ・高画質(GitHub の README で動く)
    frames[0].save(
        out_base + ".webp", save_all=True, append_images=frames[1:],
        duration=durations, loop=0, quality=82, method=4,
    )

    # GIF: 2コマに1コマ・小さめ(どこでも動く予備)
    small = [f.resize((f.width * 2 // 3, f.height * 2 // 3), Image.LANCZOS) for f in frames[::2]]
    gif_durations = [FRAME_MS * 2] * (len(small) - 1) + [1200]
    palette_frames = [f.quantize(colors=192, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG) for f in small]
    palette_frames[0].save(
        out_base + ".gif", save_all=True, append_images=palette_frames[1:],
        duration=gif_durations, loop=0, optimize=True,
    )
    for ext in (".webp", ".gif"):
        print(out_base + ext, os.path.getsize(out_base + ext) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
