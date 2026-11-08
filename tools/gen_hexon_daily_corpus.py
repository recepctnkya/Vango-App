"""Parse Hexon_Gunluk_Konusma_Diyaloglari.txt -> JSON + HexonDailyChatCorpus.kt"""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_CANDIDATES = [
    ROOT / "docs" / "Hexon_Gunluk_Konusma_Diyaloglari.txt",
]
DOCS = ROOT / "docs"
KOTLIN_OUT = ROOT / "composeApp/src/commonMain/kotlin/com/hextech/vango/HexonDailyChatCorpus.kt"


def parse_txt(text: str) -> list[dict]:
    blocks = re.split(r"\n(?=\[\d+\])", text)
    intents: list[dict] = []
    for b in blocks:
        m = re.match(r"\[(\d+)\]\s*(.+)", b.strip())
        if not m:
            continue
        iid, title = int(m.group(1)), m.group(2).strip()
        qs = re.search(r"SORULAR:\s*(.*?)(?:YANITLAR:|$)", b, re.S)
        ys = re.search(r"YANITLAR:\s*(.*)$", b, re.S)

        def bullets(s: str | None) -> list[str]:
            if not s:
                return []
            out = []
            for line in s.splitlines():
                line = line.strip()
                if line.startswith("- "):
                    out.append(line[2:].strip())
            return out

        questions = bullets(qs.group(1) if qs else None)
        answers = bullets(ys.group(1) if ys else None)
        if not questions and title:
            questions = [title]
        if questions and answers:
            intents.append(
                {"id": iid, "title": title, "questions": questions, "answers": answers}
            )
    return intents


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def write_kotlin(intents: list[dict]) -> None:
    lines = [
        "package com.hextech.vango",
        "",
        "/**",
        " * Hexon offline daily-chat corpus.",
        " * Source: docs/Hexon_Gunluk_Konusma_Diyaloglari.txt (converted).",
        f" * Intents: {len(intents)}; regenerate via tools/gen_hexon_daily_corpus.py",
        " */",
        "data class HexonDailyIntent(",
        "    val id: Int,",
        "    val title: String,",
        "    val questions: List<String>,",
        "    val answers: List<String>,",
        ")",
        "",
        "val HexonDailyChatCorpus: List<HexonDailyIntent> = listOf(",
    ]
    for i, it in enumerate(intents):
        comma = "," if i < len(intents) - 1 else ""
        qs = ", ".join(f'"{esc(q)}"' for q in it["questions"])
        ans = ", ".join(f'"{esc(a)}"' for a in it["answers"])
        lines += [
            "    HexonDailyIntent(",
            f"        id = {it['id']},",
            f'        title = "{esc(it["title"])}",',
            f"        questions = listOf({qs}),",
            f"        answers = listOf({ans}),",
            f"    ){comma}",
        ]
    lines += [")", ""]
    KOTLIN_OUT.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    src = next((p for p in SRC_CANDIDATES if p.exists()), None)
    if src is None:
        raise SystemExit("Source dialogue txt not found")
    text = src.read_text(encoding="utf-8")
    DOCS.mkdir(exist_ok=True)
    (DOCS / "Hexon_Gunluk_Konusma_Diyaloglari.txt").write_text(text, encoding="utf-8")
    intents = parse_txt(text)
    (DOCS / "hexon_daily_chat_corpus.json").write_text(
        json.dumps(intents, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    write_kotlin(intents)
    print(
        f"intents={len(intents)} q={sum(len(i['questions']) for i in intents)} "
        f"a={sum(len(i['answers']) for i in intents)}"
    )
    print(f"wrote {KOTLIN_OUT}")


if __name__ == "__main__":
    main()
