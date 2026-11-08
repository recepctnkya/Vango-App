# -*- coding: utf-8 -*-
"""Smoke-test Hexon 1000 corpus matching (mirrors Kotlin scorer)."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
corpus = json.loads((ROOT / "docs/hexon_daily_chat_corpus.json").read_text(encoding="utf-8"))
CONFIDENT = 0.78


def normalize(s: str) -> str:
    s = s.lower()
    for a, b in [("ı", "i"), ("İ", "i"), ("ğ", "g"), ("ü", "u"), ("ş", "s"), ("ö", "o"), ("ç", "c")]:
        s = s.replace(a, b)
    s = re.sub(r"[?!.,;:\"'`()\[\]{}…]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def tokens(s: str) -> set[str]:
    return {t for t in s.split(" ") if len(t) >= 2}


def phrase_score(query: str, phrase: str) -> float:
    if query == phrase:
        return 1.0
    if len(phrase) <= 12:
        if query.startswith(phrase) and len(query) <= len(phrase) + 4:
            return 0.92
        if phrase.startswith(query) and len(query) >= 3 and len(query) + 2 >= len(phrase):
            return 0.88
    if phrase in query and len(phrase) >= 4:
        return min(0.95, 0.72 + 0.25 * (len(phrase) / max(len(query), 1)))
    if query in phrase and len(query) >= 4:
        return min(0.93, 0.70 + 0.22 * (len(query) / max(len(phrase), 1)))
    qt, pt = tokens(query), tokens(phrase)
    if not qt or not pt:
        return 0.0
    inter = len(qt & pt)
    if inter == 0:
        return 0.0
    precision = inter / len(qt)
    recall = inter / len(pt)
    f1 = 2 * precision * recall / (precision + recall)
    return f1 * 0.9 if f1 >= 0.72 and inter >= min(2, len(pt)) else 0.0


def match(raw: str):
    q = normalize(raw)
    best = None
    best_score = 0.0
    for intent in corpus:
        scores = [phrase_score(q, normalize(intent["title"]))]
        scores.extend(phrase_score(q, normalize(x)) for x in intent["questions"])
        score = max(scores)
        if score > best_score:
            best_score = score
            best = intent
    if best and best_score >= CONFIDENT:
        return best, best_score
    return None, best_score


tests = [
    "Nasılsın?",
    "nasılsın",
    "Teşekkürler",
    "Bir sorunum var",
    "Motor çalışmıyor",
    "Su seviyelerini göster",
    "Akü durumu",
    "BLE bağlanmıyor",
    "Sen kimsin?",
]
for t in tests:
    intent, score = match(t)
    if intent:
        print(f"OK  {t!r} -> id={intent['id']} {intent['niyet_id']} {intent['title']!r} score={score:.2f}")
        print(f"    ans={intent['answers'][0][:90]}")
    else:
        print(f"MISS {t!r} best={score:.2f}")
