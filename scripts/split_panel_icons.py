"""
Split panel icon sheet into individual square PNG assets.

Find icon column centers via smoothed projection peaks, fill missing
centers in largest gaps, then crop a square window around each center
from a tall icon band (labels excluded).
"""
from __future__ import annotations

import os
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(os.environ.get("VANGO_PANEL_ICON_SHEET", ROOT / "assets" / "panel_icons.png"))
OUT = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "drawable"
DIAG = ROOT / "scripts" / "_icon_diag"

UPSCALE = 5
EXPORT_SIZE = 512
TITLE_MAX_X = 118
CONTENT_RIGHT = 1010
PAD_FRAC = 0.12
MIN_PAD_PX = 12

ROWS: list[list[str]] = [
    [
        "panel_icon_tavan_aydinlatma", "panel_icon_spot_aydinlatma", "panel_icon_led_serit",
        "panel_icon_mutfak_aydinlatma", "panel_icon_banyo_aydinlatma", "panel_icon_dis_aydinlatma",
        "panel_icon_okuma_lambasi", "panel_icon_yatak_basi", "panel_icon_dolap_ici_aydinlatma",
        "panel_icon_zemin_aydinlatma",
    ],
    [
        "panel_icon_priz_220v", "panel_icon_usb_priz", "panel_icon_inverter", "panel_icon_jenerator",
        "panel_icon_kompresor", "panel_icon_elektrikli_isitici", "panel_icon_su_pompasi_elektrik",
        "panel_icon_fan", "panel_icon_tv", "panel_icon_mikrodalga", "panel_icon_sac_kurutma",
    ],
    [
        "panel_icon_temiz_su_tanki", "panel_icon_gri_su_tanki", "panel_icon_pis_su_tanki",
        "panel_icon_su_pompasi", "panel_icon_su_girisi", "panel_icon_su_cikisi",
        "panel_icon_su_filtresi", "panel_icon_musluk", "panel_icon_dus", "panel_icon_sicak_su",
        "panel_icon_su_seviye",
    ],
    [
        "panel_icon_yatak_odasi", "panel_icon_oturma_alani", "panel_icon_mutfak", "panel_icon_banyo",
        "panel_icon_klozet", "panel_icon_masa", "panel_icon_koltuk", "panel_icon_dolap",
        "panel_icon_kapi", "panel_icon_pencere", "panel_icon_perde",
    ],
    [
        "panel_icon_klima", "panel_icon_isitma", "panel_icon_havalandirma", "panel_icon_hava_cikisi",
        "panel_icon_sicaklik", "panel_icon_sogutma", "panel_icon_nem", "panel_icon_gaz_dedektoru",
        "panel_icon_duman_dedektoru", "panel_icon_co_dedektoru", "panel_icon_yangin_alarmi",
    ],
    [
        "panel_icon_ac_kapat", "panel_icon_kilit", "panel_icon_alarm", "panel_icon_kamera",
        "panel_icon_hareket_sensoru", "panel_icon_kapi_sensoru", "panel_icon_uzaktan_kumanda",
        "panel_icon_bluetooth", "panel_icon_wifi", "panel_icon_gps", "panel_icon_acil_durum",
    ],
    [
        "panel_icon_buzdolabi", "panel_icon_dondurucu", "panel_icon_bulasik_makinesi",
        "panel_icon_camasir_makinesi", "panel_icon_kurutma_makinesi", "panel_icon_ocak",
        "panel_icon_firin", "panel_icon_davlumbaaz", "panel_icon_sarj_cihazi",
        "panel_icon_gunes_paneli", "panel_icon_aku",
    ],
]


def ink_mask(rgba: np.ndarray, white_icons: bool) -> np.ndarray:
    rgb = rgba[:, :, :3].astype(np.float32)
    mx = rgb.max(axis=2)
    mn = rgb.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1.0), 0.0)
    if white_icons:
        return mx > 50
    return (mx > 28) & (sat > 0.17)


def to_transparent(rgba: np.ndarray, white_icons: bool) -> np.ndarray:
    rgb = rgba[:, :, :3].astype(np.float32)
    mx = rgb.max(axis=2)
    mn = rgb.min(axis=2)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1.0), 0.0)
    out = rgba.copy()
    if white_icons:
        out[:, :, 3] = np.clip((mx - 16.0) * 5.5, 0, 255).astype(np.uint8)
    else:
        keep = (sat > 0.15) & (mx > 24)
        alpha = np.zeros(mx.shape, np.uint8)
        alpha[keep] = np.clip((mx[keep] - 14.0) * 5.5, 50, 255).astype(np.uint8)
        soft = (sat > 0.06) & (mx > 18) & ~keep
        alpha[soft] = np.clip((mx[soft] - 14.0) * 2.2, 0, 110).astype(np.uint8)
        out[:, :, 3] = alpha
    return out


def trim_square(rgba: np.ndarray, size: int) -> Image.Image:
    a = rgba[:, :, 3]
    rows = np.where(a.max(axis=1) > 20)[0]
    cols = np.where(a.max(axis=0) > 20)[0]
    if len(rows) == 0 or len(cols) == 0:
        return Image.fromarray(np.zeros((size, size, 4), np.uint8), "RGBA")
    icon = rgba[int(rows[0]) : int(rows[-1]) + 1, int(cols[0]) : int(cols[-1]) + 1]
    ih, iw = icon.shape[:2]
    side = max(ih, iw)
    pad = max(MIN_PAD_PX, int(side * PAD_FRAC))
    side += pad * 2
    canvas = np.zeros((side, side, 4), np.uint8)
    oy = (side - ih) // 2
    ox = (side - iw) // 2
    canvas[oy : oy + ih, ox : ox + iw] = icon
    img = Image.fromarray(canvas, "RGBA").resize((size, size), Image.Resampling.LANCZOS)
    return img.filter(ImageFilter.UnsharpMask(radius=1.2, percent=120, threshold=2))


def peak_centers(mask: np.ndarray, n: int, min_sep: int) -> list[int]:
    """Return n X centers (mask-local) for icons."""
    col = mask.sum(axis=0).astype(float)
    if col.max() < 1:
        return [int((i + 0.5) * mask.shape[1] / n) for i in range(n)]
    k = max(5, min_sep // 3)
    if k % 2 == 0:
        k += 1
    kernel = np.ones(k) / k
    sm = np.convolve(col, kernel, mode="same")
    thr = max(3.0, sm.max() * 0.12)
    peaks: list[tuple[float, int]] = []
    for x in range(2, len(sm) - 2):
        if sm[x] >= thr and sm[x] >= sm[x - 1] and sm[x] >= sm[x + 1]:
            peaks.append((sm[x], x))
    peaks.sort(reverse=True)
    kept: list[tuple[float, int]] = []
    for val, x in peaks:
        if all(abs(x - kx) >= min_sep for _, kx in kept):
            kept.append((val, x))
    centers = sorted(x for _, x in kept)

    # Fill missing centers in largest gaps (wide icons like LED strip may be one peak)
    while len(centers) < n:
        if len(centers) == 0:
            centers = [int((i + 0.5) * mask.shape[1] / n) for i in range(n)]
            break
        # gaps including edges
        points = [-min_sep] + centers + [mask.shape[1] + min_sep]
        best_i = 1
        best_gap = -1
        for i in range(1, len(points)):
            gap = points[i] - points[i - 1]
            if gap > best_gap:
                best_gap = gap
                best_i = i
        if best_gap < min_sep * 1.2 and len(centers) > 0:
            # force uniform
            lo = centers[0]
            hi = centers[-1]
            centers = [int(lo + i * (hi - lo) / max(n - 1, 1)) for i in range(n)]
            break
        new_x = (points[best_i - 1] + points[best_i]) // 2
        centers.append(int(new_x))
        centers = sorted(centers)

    if len(centers) > n:
        # keep n strongest among candidates near current centers
        scored = [(sm[x] if 0 <= x < len(sm) else 0.0, x) for x in centers]
        scored.sort(reverse=True)
        centers = sorted(x for _, x in scored[:n])
    return centers


def crop_around(
    arr: np.ndarray,
    ink: np.ndarray,
    cx: int,
    y0: int,
    y1: int,
    half_w: int,
    x_min: int,
    x_max: int,
) -> tuple[np.ndarray, tuple[int, int, int, int]]:
    """Crop icon window; tighten to ink bbox inside the window."""
    ax0 = max(x_min, cx - half_w)
    ax1 = min(x_max, cx + half_w)
    window_ink = ink[y0 : y1 + 1, ax0 : ax1 + 1]
    if window_ink.any():
        ys, xs = np.where(window_ink)
        pad = max(4, int(0.12 * max(xs.max() - xs.min() + 1, ys.max() - ys.min() + 1)))
        bx0 = max(0, int(xs.min()) - pad)
        bx1 = min(window_ink.shape[1] - 1, int(xs.max()) + pad)
        by0 = max(0, int(ys.min()) - pad)
        by1 = min(window_ink.shape[0] - 1, int(ys.max()) + pad)
        x0 = ax0 + bx0
        x1 = ax0 + bx1
        yy0 = y0 + by0
        yy1 = y0 + by1
    else:
        x0, x1, yy0, yy1 = ax0, ax1, y0, y1
    return arr[yy0 : yy1 + 1, x0 : x1 + 1].copy(), (x0, yy0, x1, yy1)


def main() -> None:
    DIAG.mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    for p in OUT.glob("panel_icon_*.png"):
        p.unlink()

    raw = Image.open(SRC).convert("RGBA")
    w0, h0 = raw.size
    big = raw.resize((w0 * UPSCALE, h0 * UPSCALE), Image.Resampling.LANCZOS)
    arr = np.asarray(big)

    top = 19 * UPSCALE
    bottom = 666 * UPSCALE
    title_x = TITLE_MAX_X * UPSCALE
    right = CONTENT_RIGHT * UPSCALE
    row_h = (bottom - top + 1) / 7.0

    overlay = big.copy()
    draw = ImageDraw.Draw(overlay)
    contact: list[Image.Image] = []
    exported = 0

    for ri, names in enumerate(ROWS):
        white_icons = ri == 6
        y0 = int(top + ri * row_h)
        y1 = int(top + (ri + 1) * row_h) - 1
        rh = y1 - y0 + 1
        # White labels contaminate white icons — shorter band for last row
        band_bottom_frac = 0.56 if white_icons else 0.70
        band_y0 = y0 + int(rh * 0.03)
        band_y1 = y0 + int(rh * band_bottom_frac)

        ink = ink_mask(arr, white_icons=white_icons)
        band = ink[band_y0 : band_y1 + 1, title_x : right + 1]
        n = len(names)
        # Expected pitch ~85px on original sheet
        min_sep = int(36 * UPSCALE)
        centers_local = peak_centers(band, n, min_sep=min_sep)
        centers = [title_x + c for c in centers_local]
        # half window ~ 45% of median pitch
        if len(centers) >= 2:
            pitches = [centers[i + 1] - centers[i] for i in range(len(centers) - 1)]
            median_pitch = float(np.median(pitches))
        else:
            median_pitch = (right - title_x) / float(n)
        half_w = int(median_pitch * 0.48)

        print(f"row{ri}: centers={len(centers)} half_w={half_w} band={band_y0}-{band_y1}")
        draw.rectangle([title_x, band_y0, right, band_y1], outline=(40, 140, 255, 255), width=3)

        for ci, name in enumerate(names):
            cx = centers[ci] if ci < len(centers) else title_x + int((ci + 0.5) * (right - title_x) / n)
            cell, box = crop_around(arr, ink, cx, band_y0, band_y1, half_w, title_x, right)
            rgba = to_transparent(cell, white_icons=white_icons)
            icon = trim_square(rgba, EXPORT_SIZE)
            icon.save(OUT / f"{name}.png")
            Image.fromarray(cell, "RGBA").save(DIAG / f"{name}_cell.png")
            draw.rectangle(box, outline=(0, 255, 90, 255), width=2)
            draw.ellipse([cx - 4, band_y0 + 4, cx + 4, band_y0 + 12], fill=(255, 80, 80, 255))
            contact.append(icon.resize((96, 96), Image.Resampling.LANCZOS))
            exported += 1

    cols = 11
    rows = (len(contact) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 100, rows * 100), (8, 8, 8, 255))
    for i, im in enumerate(contact):
        r, c = divmod(i, cols)
        sheet.paste(im, (c * 100 + 2, r * 100 + 2))
    sheet.save(DIAG / "contact_sheet.png")
    overlay.save(DIAG / "grid_overlay.png")
    print(f"DONE {exported}")


if __name__ == "__main__":
    main()
