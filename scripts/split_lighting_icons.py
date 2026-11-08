"""
Split lighting icon sheet (4x5) into individual icon-only PNGs.
Crops out Turkish labels; exports transparent square assets.
"""
from __future__ import annotations

import os
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(os.environ.get("VANGO_LIGHTING_ICON_SHEET", ROOT / "assets" / "lighting_icons.png"))
OUT = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "drawable"
DIAG = ROOT / "scripts" / "_light_icon_diag"

COLS = 5
ROWS = 4
EXPORT_SIZE = 512
# Fraction of each cell height used for icon (exclude label band)
ICON_HEIGHT_FRAC = 0.68
PAD_FRAC = 0.08

# Sheet order → catalog keys
KEYS: list[list[str]] = [
    [
        "panel_icon_tavan_aydinlatma",
        "panel_icon_spot_aydinlatma",
        "panel_icon_led_serit",
        "panel_icon_sarkit_aydinlatma",
        "panel_icon_duvar_aydinlatma",
    ],
    [
        "panel_icon_okuma_lambasi",
        "panel_icon_dolap_alti_aydinlatma",
        "panel_icon_dolap_ici_aydinlatma",
        "panel_icon_ayna_aydinlatma",
        "panel_icon_dis_aydinlatma",
    ],
    [
        "panel_icon_giris_aydinlatma",
        "panel_icon_merdiven_aydinlatma",
        "panel_icon_zemin_aydinlatma",
        "panel_icon_ambiyans_aydinlatma",
        "panel_icon_yatak_basi",
    ],
    [
        "panel_icon_mutfak_aydinlatma",
        "panel_icon_tavan_havalandirma_aydinlatma",
        "panel_icon_banyo_aydinlatma",
        "panel_icon_tente_alti_aydinlatma",
        "panel_icon_rgb_aydinlatma",
    ],
]


def ink_mask(rgb: np.ndarray) -> np.ndarray:
    # Dark line art on light/checkerboard → ink where dark & low sat
    r, g, b = rgb[:, :, 0].astype(np.float32), rgb[:, :, 1].astype(np.float32), rgb[:, :, 2].astype(np.float32)
    mx = np.maximum(np.maximum(r, g), b)
    mn = np.minimum(np.minimum(r, g), b)
    sat = np.where(mx > 1, (mx - mn) / mx, 0.0)
    # checkerboard has mid-gray tiles; ink is darker charcoal
    return (mx < 90) & (sat < 0.35)


def tight_crop(rgba: Image.Image, pad_frac: float = PAD_FRAC) -> Image.Image:
    arr = np.array(rgba)
    if arr.shape[2] == 4:
        alpha = arr[:, :, 3] > 16
        rgb = arr[:, :, :3]
        ink = ink_mask(rgb) | alpha
    else:
        ink = ink_mask(arr)
    ys, xs = np.where(ink)
    if len(xs) == 0:
        return rgba
    y0, y1 = int(ys.min()), int(ys.max()) + 1
    x0, x1 = int(xs.min()), int(xs.max()) + 1
    h, w = y1 - y0, x1 - x0
    pad = max(4, int(max(h, w) * pad_frac))
    y0 = max(0, y0 - pad)
    x0 = max(0, x0 - pad)
    y1 = min(arr.shape[0], y1 + pad)
    x1 = min(arr.shape[1], x1 + pad)
    return rgba.crop((x0, y0, x1, y1))


def to_transparent_line_art(cell: Image.Image) -> Image.Image:
    rgb = np.array(cell.convert("RGB"))
    ink = ink_mask(rgb)
    # Soft edge: also keep slightly darker pixels
    gray = rgb.mean(axis=2)
    soft = (gray < 140) & (np.abs(rgb[:, :, 0] - rgb[:, :, 1]) < 25) & (np.abs(rgb[:, :, 1] - rgb[:, :, 2]) < 25)
    mask = ink | soft
    out = np.zeros((rgb.shape[0], rgb.shape[1], 4), dtype=np.uint8)
    # White glyph (tintable via ColorFilter) with alpha from darkness
    darkness = np.clip(255 - gray, 0, 255).astype(np.uint8)
    out[:, :, 0] = 255
    out[:, :, 1] = 255
    out[:, :, 2] = 255
    out[:, :, 3] = np.where(mask, darkness, 0).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def square_pad(im: Image.Image, size: int = EXPORT_SIZE) -> Image.Image:
    w, h = im.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(im, ((side - w) // 2, (side - h) // 2), im)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    DIAG.mkdir(parents=True, exist_ok=True)
    src = Image.open(SRC).convert("RGB")
    W, H = src.size
    cw, ch = W / COLS, H / ROWS
    print(f"sheet={W}x{H} cell={cw:.1f}x{ch:.1f}")

    for r, row in enumerate(KEYS):
        for c, key in enumerate(row):
            x0 = int(round(c * cw))
            y0 = int(round(r * ch))
            x1 = int(round((c + 1) * cw))
            y1 = int(round(r * ch + ch * ICON_HEIGHT_FRAC))
            cell = src.crop((x0, y0, x1, y1))
            transparent = to_transparent_line_art(cell)
            cropped = tight_crop(transparent)
            final = square_pad(cropped)
            path = OUT / f"{key}.png"
            final.save(path)
            # diag with checker for QA
            diag = Image.new("RGBA", final.size, (220, 220, 220, 255))
            diag.paste(final, (0, 0), final)
            diag.save(DIAG / f"{key}.png")
            print(f"wrote {path.name} ink_bbox={cropped.size}")

    print("done")


if __name__ == "__main__":
    main()
