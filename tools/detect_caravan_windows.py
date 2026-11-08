"""Detect glass regions on phone_caravan_motor.png and write a soft night glow overlay."""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
from pathlib import Path
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "composeApp/src/commonMain/composeResources/drawable"
SRC = DRAWABLE / "phone_caravan_motor.png"
OUT = DRAWABLE / "phone_caravan_motor_night_glow.png"


def main() -> None:
    im = Image.open(SRC).convert("RGBA")
    arr = np.array(im)
    h, w = arr.shape[:2]
    r, g, b, a = [arr[:, :, i].astype(np.float32) for i in range(4)]
    lum = 0.2126 * r + 0.7152 * g + 0.0722 * b

    glass = (
        (a > 220)
        & (lum < 58)
        & (lum > 8)
        & (b + 8 >= r)
        & (b + 8 >= g)
        & (np.maximum(np.maximum(r, g), b) - np.minimum(np.minimum(r, g), b) < 55)
    )
    glass[: int(h * 0.17), :] = False
    glass[int(h * 0.55) :, :] = False

    labeled, n = ndimage.label(glass)
    windows = []
    for i in range(1, n + 1):
        ys, xs = np.where(labeled == i)
        if len(xs) < 180:
            continue
        x0, x1 = int(xs.min()), int(xs.max())
        y0, y1 = int(ys.min()), int(ys.max())
        ww, hh = x1 - x0 + 1, y1 - y0 + 1
        area = len(xs)
        fill = area / (ww * hh + 1e-6)
        asp = ww / max(hh, 1)
        if fill < 0.35 or asp > 6 or asp < 0.15:
            continue
        if ww < 14 or hh < 16 or area < 350 or ww / w > 0.45:
            continue
        windows.append({"box": (x0, y0, x1, y1), "mask": labeled == i})

    windows.sort(key=lambda d: d["box"][0])
    print(f"detected {len(windows)} windows")

    core = np.zeros((h, w, 4), dtype=np.float32)
    union = np.zeros((h, w), dtype=bool)
    for d in windows:
        m = d["mask"]
        union |= m
        ys, xs = np.where(m)
        cx, cy = xs.mean(), ys.mean()
        x0, y0, x1, y1 = d["box"]
        rx = max((x1 - x0) * 0.7, 1.0)
        ry = max((y1 - y0) * 0.7, 1.0)
        yy, xx = np.ogrid[0:h, 0:w]
        fall = np.clip(1.0 - (((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2), 0, 1) ** 1.8
        fall = fall * m
        core[..., 0] += 255 * fall
        core[..., 1] += 200 * fall
        core[..., 2] += 110 * fall
        core[..., 3] += 42 * fall

    core = np.clip(core, 0, 255)
    core[..., 3] = np.clip(core[..., 3], 0, 55)
    core_img = Image.fromarray(core.astype(np.uint8), "RGBA").filter(
        ImageFilter.GaussianBlur(radius=max(3.5, w / 280))
    )

    bloom = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bloom)
    for d in windows:
        x0, y0, x1, y1 = d["box"]
        cx = (x0 + x1) / 2
        cy = (y0 + y1) / 2
        rx = (x1 - x0) * 1.4
        ry = (y1 - y0) * 1.5
        bd.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(255, 175, 90, 22))
    bloom = bloom.filter(ImageFilter.GaussianBlur(radius=max(18, w // 55)))
    union_d = np.array(
        Image.fromarray((union * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(11))
    ) > 0
    ba = np.array(bloom).astype(np.float32)
    ba[..., 3] *= np.where(union_d, 1.0, 0.18)
    bloom = Image.fromarray(np.clip(ba, 0, 255).astype(np.uint8), "RGBA")

    overlay = Image.alpha_composite(bloom, core_img)
    head = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    hd = ImageDraw.Draw(head)
    hd.ellipse(
        [int(w * 0.085), int(h * 0.515), int(w * 0.15), int(h * 0.575)],
        fill=(255, 240, 200, 32),
    )
    overlay = Image.alpha_composite(
        overlay, head.filter(ImageFilter.GaussianBlur(radius=max(10, w // 85)))
    )
    overlay.save(OUT)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
