"""Parse Hexon_1000_Gunluk_Teknik_Soru_Cevap.txt → JSON + HexonDailyChatCorpus.kt"""
from __future__ import annotations

import json
import shutil
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC_CANDIDATES = [
    ROOT / "docs" / "Hexon_1000_Gunluk_Teknik_Soru_Cevap.txt",
]
DOCS = ROOT / "docs"
KOTLIN_OUT = ROOT / "composeApp/src/commonMain/kotlin/com/hextech/vango/HexonDailyChatCorpus.kt"
JSON_OUT = DOCS / "hexon_daily_chat_corpus.json"
DOCS_COPY = DOCS / "Hexon_1000_Gunluk_Teknik_Soru_Cevap.txt"


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def parse_tsv(text: str) -> list[dict]:
    lines = text.splitlines()
    start = next(i for i, line in enumerate(lines) if line.startswith("NO\t"))
    grouped: OrderedDict[str, dict] = OrderedDict()
    for line in lines[start + 1 :]:
        if not line.strip() or not line[0].isdigit():
            continue
        parts = line.split("\t")
        if len(parts) < 6:
            continue
        _no, category, niyet_id, konu, soru, yanit = parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]
        if niyet_id not in grouped:
            grouped[niyet_id] = {
                "niyet_id": niyet_id,
                "category": category,
                "title": konu.strip(),
                "questions": [],
                "answers": [],
            }
        intent = grouped[niyet_id]
        q = soru.strip()
        a = yanit.strip()
        if q and q not in intent["questions"]:
            intent["questions"].append(q)
        if a and a not in intent["answers"]:
            intent["answers"].append(a)

    intents: list[dict] = []
    for idx, intent in enumerate(grouped.values(), start=1):
        if not intent["questions"] or not intent["answers"]:
            continue
        intents.append(
            {
                "id": idx,
                "niyet_id": intent["niyet_id"],
                "category": intent["category"],
                "title": intent["title"],
                "questions": intent["questions"],
                "answers": intent["answers"],
            }
        )
    return intents


def write_kotlin(intents: list[dict]) -> None:
    lines = [
        "package com.hextech.vango",
        "",
        "/**",
        " * Hexon offline chat corpus (günlük + teknik + arıza).",
        " * Source: docs/Hexon_1000_Gunluk_Teknik_Soru_Cevap.txt",
        f" * Intents: {len(intents)}; regenerate via tools/gen_hexon_1000_corpus.py",
        " */",
        "data class HexonDailyIntent(",
        "    val id: Int,",
        "    val title: String,",
        "    val questions: List<String>,",
        "    val answers: List<String>,",
        "    val category: String = \"GUNLUK\",",
        "    val niyetId: String = \"\",",
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
            f'        category = "{esc(it["category"])}",',
            f'        niyetId = "{esc(it["niyet_id"])}",',
            f"    ){comma}",
        ]
    lines += [")", ""]
    KOTLIN_OUT.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    src = next((p for p in SRC_CANDIDATES if p.exists()), None)
    if src is None:
        raise SystemExit("Source 1000 Q&A txt not found")
    text = src.read_text(encoding="utf-8")
    DOCS.mkdir(exist_ok=True)
    if src.resolve() != DOCS_COPY.resolve():
        shutil.copyfile(src, DOCS_COPY)
    intents = parse_tsv(text)
    JSON_OUT.write_text(json.dumps(intents, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_kotlin(intents)
    cats: dict[str, int] = {}
    for it in intents:
        cats[it["category"]] = cats.get(it["category"], 0) + 1
    print(
        f"intents={len(intents)} q={sum(len(i['questions']) for i in intents)} "
        f"a={sum(len(i['answers']) for i in intents)} cats={cats}"
    )
    print(f"wrote {KOTLIN_OUT}")
    print(f"wrote {JSON_OUT}")


if __name__ == "__main__":
    main()
