#!/usr/bin/env python3
"""One-shot splitter: PhoneHomeHeroScene.kt → modular draw files. Run from repo root."""
from __future__ import annotations

from pathlib import Path

ROOT = Path("composeApp/src/commonMain/kotlin/com/hextech/vango")
SRC = ROOT / "PhoneHomeHeroScene.kt"

COMMON_IMPORTS = """package com.hextech.vango

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
"""

# 1-indexed inclusive line ranges from PhoneHomeHeroScene.kt
SECTIONS: dict[str, list[tuple[int, int]]] = {
    "HeroScenePalette.kt": [(456, 759)],
    "HeroSceneLayout.kt": [(761, 801), (830, 856)],  # layout + compute; skip HeroVehicleColors
    "HeroSceneLight.kt": [(858, 959)],
    "HeroSceneWater.kt": [(1143, 1801)],  # lake + wet shore + body light
    "HeroSceneGround.kt": [(1803, 2164)],  # park
    "HeroSceneMountains.kt": [(2530, 2832)],
    "HeroSceneTrees.kt": [(2834, 3502)],  # clusters + pine
    "HeroSceneSky.kt": [(3052, 3232)],  # WAIT overlap with trees - fix below
    "HeroScenePhoneLink.kt": [(3506, 3664)],
    "HeroVehicleDraw.kt": [
        (803, 828),  # HeroVehicleColors + metrics
        (2166, 2528),  # wheel/window/door helpers
        (3666, 4109),  # towable + motorhome
        (4145, 4182),  # closeup preview
    ],
    "HeroSceneMath.kt": [(3504, 3504), (4111, 4119)],
}


def lines_1indexed(text: str) -> list[str]:
    # Keep newline endings
    parts = text.splitlines(keepends=True)
    return parts


def extract(lines: list[str], ranges: list[tuple[int, int]]) -> str:
    chunks: list[str] = []
    for a, b in ranges:
        chunks.append("".join(lines[a - 1 : b]))
    return "".join(chunks)


def privatize_to_internal(body: str) -> str:
    out: list[str] = []
    for line in body.splitlines(keepends=True):
        stripped = line.lstrip()
        # Only top-level private → internal (indent == 0 after strip of leading spaces means starts at column 0)
        if line.startswith("private "):
            out.append("internal " + line[len("private ") :])
        elif line.startswith("private fun ") or line.startswith("private data ") or line.startswith(
            "private const "
        ) or line.startswith("private object ") or line.startswith("private val "):
            out.append("internal " + line[len("private ") :])
        else:
            out.append(line)
    return "".join(out)


def main() -> None:
    text = SRC.read_text(encoding="utf-8")
    lines = lines_1indexed(text)

    # Correct section map without overlap
    sections: dict[str, list[tuple[int, int]]] = {
        "HeroScenePalette.kt": [(456, 759)],
        "HeroSceneLayout.kt": [(761, 801), (830, 856)],
        "HeroSceneLight.kt": [(858, 959)],
        "HeroSceneWater.kt": [(1143, 1801)],
        "HeroSceneGround.kt": [(1803, 2164)],
        "HeroSceneMountains.kt": [(2530, 2832)],
        "HeroSceneTrees.kt": [(2834, 3050), (3234, 3502)],
        "HeroSceneSky.kt": [(3052, 3232)],
        "HeroScenePhoneLink.kt": [(3506, 3664)],
        "HeroVehicleDraw.kt": [
            (803, 828),
            (2166, 2528),
            (3666, 4109),
            (4145, 4182),
        ],
        "HeroSceneMath.kt": [(3504, 3504), (4111, 4119)],
    }

    for name, ranges in sections.items():
        body = privatize_to_internal(extract(lines, ranges))
        path = ROOT / name
        path.write_text(COMMON_IMPORTS + "\n" + body, encoding="utf-8")
        print(f"Wrote {path} ({sum(b - a + 1 for a, b in ranges)} lines body)")

    # Keep in main: 1-455 (through end of card), drawHeroScene 961-1141,
    # capitalize + badge 4121-4138
    keep_ranges = [(1, 455), (961, 1141), (4121, 4138)]
    kept = extract(lines, keep_ranges)
    # Remove extracted private decls that are no longer needed from head? Head is only card.
    # drawHeroScene still private — OK in same file as card.
    SRC.write_text(kept + "\n", encoding="utf-8")
    print(f"Rewrote {SRC} kept parts")


if __name__ == "__main__":
    main()
