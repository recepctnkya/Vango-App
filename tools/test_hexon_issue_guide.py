"""Smoke-test motor issue step advancement (logic mirror)."""
from __future__ import annotations


def norm(s: str) -> str:
    s = s.lower()
    for a, b in [("ı", "i"), ("İ", "i"), ("ğ", "g"), ("ü", "u"), ("ş", "s"), ("ö", "o"), ("ç", "c")]:
        s = s.replace(a, b)
    return s.strip()


TRIGGERS = [
    "motor calismiyor",
    "tente hareket etmiyor",
    "motor hareket etmiyor",
]
CONT = ["kontrol ettim", "hala calismiyor", "yaptim", "kablo gevsek", "servis"]


def is_start(q: str) -> bool:
    q = norm(q)
    return any(t in q for t in TRIGGERS)


def is_cont(q: str) -> bool:
    q = norm(q)
    return any(norm(c) in q for c in CONT) or "motor" in q


steps = [
    "Motorun calismadigini anladim. Uygulamada bir hata veya kilit uyarisi gorunuyor mu?",
    "Motorun enerji beslemesini kontrol edebilir misin?",
    "Motor baglantisinda gevseklik ... enerji kapaliyken kontrol ettir.",
    "Bunlar normalse gorunen hata mesajini veya baglanti durumunu bana yaz...",
    "SERVICE: yetkili servis; motor sohbetten calistirilmaz",
]

# Simulate dialogue
idx = -1
script = [
    ("Motor calismiyor", "start"),
    ("Kontrol ettim, uyari yok", "advance"),
    ("Kontrol ettim, hala calismiyor", "advance"),
    ("Kabolar gevsek gibi", "service"),
]

ok = True
for text, expect in script:
    q = norm(text)
    if idx < 0:
        assert is_start(q), text
        idx = 0
        got = "start"
    elif "gevsek" in q or "kablo" in q:
        idx = 4
        got = "service"
    elif is_cont(q):
        # do not reset to 0
        prev = idx
        idx = min(idx + 1, len(steps) - 1)
        assert idx > prev or prev == len(steps) - 1
        got = "advance" if idx < 4 else "service"
    else:
        got = "other"
    status = "PASS" if got == expect or (expect == "service" and idx == 4) else "FAIL"
    if status == "FAIL":
        ok = False
    print(f"[{status}] {text!r} -> step={idx} {steps[idx][:50]}... ({got})")

# Ensure we never went back to 0 after start
print("OK" if ok else "FAIL")
raise SystemExit(0 if ok else 1)
