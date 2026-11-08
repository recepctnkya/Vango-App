"""Offline matcher smoke test mirroring HexonDailyChatMatcher scoring."""
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
    s = re.sub(r"\s+", " ", s).strip()
    return s


def tokens(s: str) -> set[str]:
    return {t for t in s.split(" ") if len(t) >= 2}


def phrase_score(query: str, phrase: str) -> float:
    if query == phrase:
        return 1.0
    if len(phrase) <= 12:
        if query == phrase:
            return 1.0
        if query.startswith(phrase) and len(query) <= len(phrase) + 4:
            return 0.92
        if phrase.startswith(query) and len(query) >= 3 and len(query) + 2 >= len(phrase):
            return 0.88
    if phrase in query and len(phrase) >= 4:
        ratio = len(phrase) / len(query)
        return min(0.95, 0.72 + 0.25 * ratio)
    if query in phrase and len(query) >= 4:
        ratio = len(query) / len(phrase)
        return min(0.93, 0.70 + 0.22 * ratio)
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


def score_intent(q: str, intent: dict) -> float:
    best = 0.0
    best = max(best, phrase_score(q, normalize(intent["title"])))
    for question in intent["questions"]:
        best = max(best, phrase_score(q, normalize(question)))
    return best


def match(raw: str):
    q = normalize(raw)
    best_id, best_score = -1, 0.0
    for intent in corpus:
        s = score_intent(q, intent)
        if s > best_score:
            best_score, best_id = s, intent["id"]
    if best_id < 0 or best_score < CONFIDENT:
        return None
    intent = next(i for i in corpus if i["id"] == best_id)
    return best_id, best_score, intent["answers"][0]


cases = [
    ("Nasılsın?", True),
    ("Nasıl gidiyor?", True),
    ("Sen kimsin?", True),
    ("asdf qwerty zxcvbn bilinmeyen soru 999", False),
]

ok = True
for text, expect_hit in cases:
    hit = match(text)
    passed = (hit is not None) == expect_hit
    ok = ok and passed
    status = "PASS" if passed else "FAIL"
    detail = f"id={hit[0]} score={hit[1]:.2f} => {hit[2][:60]}" if hit else "no-match (rephrase/remote)"
    print(f"[{status}] {text!r} -> {detail}")

raise SystemExit(0 if ok else 1)
