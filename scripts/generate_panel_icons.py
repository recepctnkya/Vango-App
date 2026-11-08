"""
Generate clean line-art panel icons (512px) matching the VanGO icon sheet
categories/colors. Used because the source sheet is only 1024x682 and
automatic crops are soft/broken.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "drawable"
SIZE = 512
STROKE = 22
PAD = 40

COLORS = {
    "lighting": (255, 176, 32, 255),
    "electric": (139, 207, 74, 255),
    "water": (78, 168, 255, 255),
    "living": (176, 124, 255, 255),
    "climate": (64, 208, 224, 255),
    "security": (255, 90, 90, 255),
    "other": (232, 232, 232, 255),
}


def canvas():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def draw_base(color):
    im = canvas()
    d = ImageDraw.Draw(im)
    return im, d, color


def box():
    return PAD, PAD, SIZE - PAD, SIZE - PAD


def cxcy():
    return SIZE // 2, SIZE // 2


def line(d, color, pts, w=STROKE):
    d.line(pts, fill=color, width=w, joint="curve")


def arc(d, color, bbox, start, end, w=STROKE):
    d.arc(bbox, start=start, end=end, fill=color, width=w)


def ellipse(d, color, bbox, w=STROKE):
    d.ellipse(bbox, outline=color, width=w)


def rect(d, color, bbox, w=STROKE, rad=24):
    d.rounded_rectangle(bbox, radius=rad, outline=color, width=w)


# --- icon drawers ---

def icon_tavan(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    ellipse(d, c, (x - 70, y - 40, x + 70, y + 50))
    line(d, c, [(x, y - 90), (x, y - 40)])
    for dx in (-40, 0, 40):
        line(d, c, [(x + dx * 0.3, y + 55), (x + dx, y + 110)], w=14)
    return im


def icon_spot(color):
    im, d, c = draw_base(color)
    # angled cylinder
    line(d, c, [(200, 180), (340, 300)], w=28)
    ellipse(d, c, (160, 280, 250, 360))
    line(d, c, [(180, 350), (150, 400)], w=12)
    line(d, c, [(200, 360), (190, 410)], w=12)
    line(d, c, [(220, 350), (240, 400)], w=12)
    return im


def icon_led(color):
    im, d, c = draw_base(color)
    rect(d, c, (110, 220, 400, 280), rad=20)
    for x in range(150, 380, 50):
        ellipse(d, c, (x - 12, 300, x + 12, 324))
    return im


def icon_pendant(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    line(d, c, [(x, 90), (x, 160)])
    line(d, c, [(x - 90, 200), (x + 90, 200), (x + 55, 280), (x - 55, 280), (x - 90, 200)])
    ellipse(d, c, (x - 28, 275, x + 28, 320))
    for dx in (-45, 0, 45):
        line(d, c, [(x + dx * 0.2, 325), (x + dx, 390)], w=12)
    return im


def icon_globe(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    ellipse(d, c, (x - 80, y - 70, x + 80, y + 90))
    line(d, c, [(x - 50, y + 100), (x + 50, y + 100)])
    return im


def icon_sun(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    ellipse(d, c, (x - 55, y - 55, x + 55, y + 55))
    import math
    for a in range(0, 360, 45):
        rad = math.radians(a)
        x1 = x + math.cos(rad) * 75
        y1 = y + math.sin(rad) * 75
        x2 = x + math.cos(rad) * 120
        y2 = y + math.sin(rad) * 120
        line(d, c, [(x1, y1), (x2, y2)], w=14)
    return im


def icon_desk_lamp(color):
    im, d, c = draw_base(color)
    line(d, c, [(160, 380), (280, 380)], w=20)
    line(d, c, [(220, 380), (220, 300), (300, 200)], w=16)
    line(d, c, [(270, 170), (350, 210), (320, 250), (250, 210), (270, 170)], w=16)
    return im


def icon_table_lamp(color):
    im, d, c = draw_base(color)
    x = 256
    line(d, c, [(x - 60, 380), (x + 60, 380)], w=18)
    line(d, c, [(x, 380), (x, 280)], w=16)
    line(d, c, [(x - 90, 200), (x + 90, 200), (x + 55, 280), (x - 55, 280), (x - 90, 200)], w=16)
    return im


def icon_closet_light(color):
    im, d, c = draw_base(color)
    rect(d, c, (150, 110, 360, 400), rad=16)
    # hanger
    arc(d, c, (200, 180, 310, 260), 200, 340, w=14)
    line(d, c, [(255, 180), (255, 150)], w=14)
    line(d, c, [(210, 250), (300, 250)], w=14)
    ellipse(d, c, (230, 130, 280, 165))
    return im


def icon_floor_light(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 240, 370, 300), rad=16)
    for x in range(180, 350, 45):
        ellipse(d, c, (x - 10, 315, x + 10, 335))
    for dx in (-50, 0, 50):
        line(d, c, [(256 + dx * 0.3, 340), (256 + dx, 400)], w=12)
    return im


def icon_outlet(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (140, 140, 370, 370))
    ellipse(d, c, (200, 200, 245, 280))
    ellipse(d, c, (265, 200, 310, 280))
    ellipse(d, c, (230, 300, 280, 340))
    return im


def icon_usb(color):
    im, d, c = draw_base(color)
    rect(d, c, (150, 200, 360, 310), rad=12)
    line(d, c, [(200, 230), (200, 280)], w=14)
    line(d, c, [(255, 230), (255, 280)], w=14)
    line(d, c, [(310, 230), (310, 280)], w=14)
    return im


def icon_inverter(color):
    im, d, c = draw_base(color)
    rect(d, c, (130, 160, 380, 350), rad=18)
    # waveform
    pts = [(160, 255), (200, 200), (240, 310), (280, 200), (320, 310), (360, 255)]
    line(d, c, pts, w=14)
    return im


def icon_generator(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 180, 370, 360), rad=20)
    ellipse(d, c, (200, 220, 310, 320))
    line(d, c, [(160, 160), (160, 190)], w=14)
    line(d, c, [(350, 160), (350, 190)], w=14)
    return im


def icon_tank(color):
    im, d, c = draw_base(color)
    rect(d, c, (170, 140, 340, 380), rad=30)
    arc(d, c, (190, 200, 320, 280), 200, 340, w=14)
    return im


def icon_heater(color):
    im, d, c = draw_base(color)
    rect(d, c, (150, 250, 360, 380), rad=16)
    for x in (200, 256, 312):
        line(d, c, [(x, 230), (x - 10, 160), (x + 10, 120)], w=14)
    return im


def icon_pump(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (160, 180, 320, 340))
    rect(d, c, (300, 230, 380, 290), rad=10)
    line(d, c, [(200, 200), (280, 320)], w=14)
    line(d, c, [(280, 200), (200, 320)], w=14)
    return im


def icon_fan(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    ellipse(d, c, (x - 30, y - 30, x + 30, y + 30))
    import math
    for a in (90, 210, 330):
        rad = math.radians(a)
        x2 = x + math.cos(rad) * 120
        y2 = y + math.sin(rad) * 120
        line(d, c, [(x, y), (x2, y2)], w=22)
        ellipse(d, c, (x2 - 35, y2 - 25, x2 + 35, y2 + 25))
    return im


def icon_tv(color):
    im, d, c = draw_base(color)
    rect(d, c, (110, 140, 400, 320), rad=18)
    line(d, c, [(200, 340), (256, 380), (312, 340)], w=16)
    line(d, c, [(180, 400), (330, 400)], w=16)
    return im


def icon_microwave(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 150, 390, 360), rad=16)
    rect(d, c, (150, 180, 300, 330), rad=10)
    ellipse(d, c, (330, 200, 360, 230))
    ellipse(d, c, (330, 250, 360, 280))
    return im


def icon_hairdryer(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (140, 160, 300, 300))
    line(d, c, [(280, 250), (380, 360)], w=28)
    line(d, c, [(150, 200), (100, 180)], w=14)
    line(d, c, [(150, 230), (90, 230)], w=14)
    line(d, c, [(150, 260), (100, 280)], w=14)
    return im


def icon_water_tank(color, level=0.5):
    im, d, c = draw_base(color)
    rect(d, c, (160, 120, 350, 400), rad=24)
    y = int(400 - (400 - 120) * level)
    line(d, c, [(180, y), (220, y - 15), (260, y + 10), (300, y - 10), (330, y)], w=14)
    return im


def icon_faucet(color):
    im, d, c = draw_base(color)
    line(d, c, [(140, 220), (280, 220), (280, 300)], w=22)
    line(d, c, [(230, 180), (330, 180)], w=18)
    line(d, c, [(280, 180), (280, 220)], w=18)
    ellipse(d, c, (255, 300, 305, 340))
    return im


def icon_faucet_in(color):
    im = icon_faucet(color)
    d = ImageDraw.Draw(im)
    line(d, color, [(340, 250), (400, 250)], w=14)
    line(d, color, [(340, 250), (360, 230)], w=12)
    line(d, color, [(340, 250), (360, 270)], w=12)
    return im


def icon_faucet_out(color):
    im = icon_faucet(color)
    d = ImageDraw.Draw(im)
    line(d, color, [(340, 250), (400, 250)], w=14)
    line(d, color, [(400, 250), (380, 230)], w=12)
    line(d, color, [(400, 250), (380, 270)], w=12)
    return im


def icon_filter(color):
    im, d, c = draw_base(color)
    rect(d, c, (200, 120, 310, 400), rad=40)
    line(d, c, [(220, 200), (290, 200)], w=12)
    line(d, c, [(220, 260), (290, 260)], w=12)
    line(d, c, [(220, 320), (290, 320)], w=12)
    return im


def icon_shower(color):
    im, d, c = draw_base(color)
    line(d, c, [(256, 100), (256, 180)], w=16)
    arc(d, c, (170, 160, 340, 300), 200, 340, w=18)
    for x in (200, 230, 256, 282, 312):
        line(d, c, [(x, 280), (x, 360)], w=10)
    return im


def icon_hot_water(color):
    im, d, c = draw_base(color)
    rect(d, c, (170, 200, 340, 400), rad=20)
    for x in (210, 256, 300):
        line(d, c, [(x, 180), (x - 8, 120)], w=12)
    return im


def icon_drop(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    # teardrop approximation
    line(d, c, [(x, 120), (x + 90, 280), (x, 380), (x - 90, 280), (x, 120)], w=18)
    return im


def icon_bed(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 220, 390, 320), rad=12)
    line(d, c, [(140, 220), (140, 180), (220, 180), (220, 220)], w=16)
    line(d, c, [(140, 320), (140, 370)], w=16)
    line(d, c, [(370, 320), (370, 370)], w=16)
    return im


def icon_sofa(color):
    im, d, c = draw_base(color)
    rect(d, c, (130, 220, 380, 320), rad=20)
    rect(d, c, (120, 180, 200, 260), rad=16)
    rect(d, c, (310, 180, 390, 260), rad=16)
    line(d, c, [(150, 320), (150, 370)], w=14)
    line(d, c, [(360, 320), (360, 370)], w=14)
    return im


def icon_kitchen(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 200, 390, 380), rad=12)
    rect(d, c, (160, 240, 250, 320), rad=8)
    line(d, c, [(300, 220), (300, 280)], w=14)
    line(d, c, [(280, 220), (320, 220)], w=14)
    return im


def icon_toilet(color):
    im, d, c = draw_base(color)
    rect(d, c, (180, 120, 330, 220), rad=12)
    ellipse(d, c, (170, 220, 340, 380))
    line(d, c, [(255, 220), (255, 260)], w=14)
    return im


def icon_table(color):
    im, d, c = draw_base(color)
    line(d, c, [(120, 220), (390, 220)], w=20)
    line(d, c, [(160, 220), (140, 380)], w=16)
    line(d, c, [(350, 220), (370, 380)], w=16)
    return im


def icon_chair(color):
    im, d, c = draw_base(color)
    rect(d, c, (160, 200, 340, 300), rad=16)
    line(d, c, [(160, 200), (160, 140), (340, 140), (340, 200)], w=16)
    line(d, c, [(180, 300), (170, 380)], w=14)
    line(d, c, [(320, 300), (330, 380)], w=14)
    return im


def icon_wardrobe(color):
    im, d, c = draw_base(color)
    rect(d, c, (150, 100, 360, 400), rad=12)
    line(d, c, [(255, 100), (255, 400)], w=14)
    ellipse(d, c, (220, 240, 245, 265))
    ellipse(d, c, (265, 240, 290, 265))
    return im


def icon_door(color):
    im, d, c = draw_base(color)
    rect(d, c, (170, 100, 350, 400), rad=8)
    ellipse(d, c, (300, 240, 330, 270))
    return im


def icon_window(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 140, 370, 370), rad=8)
    line(d, c, [(255, 140), (255, 370)], w=14)
    line(d, c, [(140, 255), (370, 255)], w=14)
    return im


def icon_curtain(color):
    im, d, c = draw_base(color)
    line(d, c, [(130, 120), (380, 120)], w=16)
    for x0 in (150, 280):
        line(d, c, [(x0, 120), (x0 - 20, 380)], w=14)
        line(d, c, [(x0 + 40, 120), (x0 + 60, 380)], w=14)
        line(d, c, [(x0 + 80, 120), (x0 + 40, 380)], w=14)
    return im


def icon_ac(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 160, 390, 280), rad=18)
    line(d, c, [(150, 220), (360, 220)], w=12)
    for x in (180, 256, 330):
        line(d, c, [(x, 300), (x - 15, 370)], w=12)
        line(d, c, [(x, 300), (x + 15, 370)], w=12)
    return im


def icon_heat_waves(color):
    im, d, c = draw_base(color)
    for x in (180, 256, 330):
        line(d, c, [(x, 380), (x - 15, 280), (x + 15, 200), (x, 120)], w=16)
    return im


def icon_vent(color):
    im, d, c = draw_base(color)
    rect(d, c, (130, 180, 380, 330), rad=12)
    for y in range(210, 310, 28):
        line(d, c, [(160, y), (350, y)], w=12)
    return im


def icon_thermo(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (210, 300, 300, 390))
    rect(d, c, (235, 120, 275, 320), rad=16)
    line(d, c, [(255, 180), (255, 320)], w=14)
    return im


def icon_snow(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    import math
    for a in range(0, 360, 60):
        rad = math.radians(a)
        line(d, c, [(x, y), (x + math.cos(rad) * 120, y + math.sin(rad) * 120)], w=14)
        bx = x + math.cos(rad) * 70
        by = y + math.sin(rad) * 70
        line(d, c, [(bx + math.cos(rad + 0.8) * 30, by + math.sin(rad + 0.8) * 30),
                    (bx, by),
                    (bx + math.cos(rad - 0.8) * 30, by + math.sin(rad - 0.8) * 30)], w=12)
    return im


def icon_humidity(color):
    im = icon_drop(color)
    d = ImageDraw.Draw(im)
    # percent
    d.text((230, 240), "%", fill=color)
    return im


def icon_sensor_text(color, text):
    im, d, c = draw_base(color)
    ellipse(d, c, (130, 130, 380, 380))
    # simple bars for text substitute
    if text == "CO2":
        line(d, c, [(180, 240), (240, 240)], w=14)
        line(d, c, [(200, 220), (200, 280)], w=14)
        ellipse(d, c, (260, 220, 300, 260))
        ellipse(d, c, (310, 220, 350, 260))
    elif text == "CO":
        ellipse(d, c, (180, 220, 250, 290))
        ellipse(d, c, (270, 220, 340, 290))
    else:
        ellipse(d, c, (200, 200, 310, 310))
    return im


def icon_smoke(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (160, 200, 350, 360))
    arc(d, c, (190, 140, 320, 240), 200, 340, w=14)
    arc(d, c, (200, 110, 310, 200), 200, 340, w=12)
    return im


def icon_alarm_bell(color):
    im, d, c = draw_base(color)
    arc(d, c, (160, 140, 350, 340), 200, 340, w=20)
    line(d, c, [(170, 300), (340, 300)], w=18)
    line(d, c, [(255, 120), (255, 150)], w=14)
    ellipse(d, c, (240, 320, 270, 360))
    return im


def icon_power(color):
    im, d, c = draw_base(color)
    arc(d, c, (140, 140, 370, 370), 40, 320, w=22)
    line(d, c, [(256, 130), (256, 260)], w=22)
    return im


def icon_lock(color):
    im, d, c = draw_base(color)
    rect(d, c, (160, 230, 350, 400), rad=16)
    arc(d, c, (190, 120, 320, 280), 180, 360, w=18)
    return im


def icon_siren(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (180, 180, 330, 330))
    rect(d, c, (200, 320, 310, 380), rad=8)
    for a in (-40, 0, 40):
        import math
        rad = math.radians(a - 90)
        line(d, c, [(256 + math.cos(rad) * 90, 255 + math.sin(rad) * 90),
                    (256 + math.cos(rad) * 140, 255 + math.sin(rad) * 140)], w=12)
    return im


def icon_camera(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 180, 320, 330), rad=16)
    line(d, c, [(320, 220), (390, 180), (390, 330), (320, 290)], w=16)
    ellipse(d, c, (170, 210, 260, 300))
    return im


def icon_motion(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (220, 120, 280, 180))
    line(d, c, [(250, 180), (250, 280), (200, 370)], w=16)
    line(d, c, [(250, 280), (310, 370)], w=16)
    line(d, c, [(200, 230), (300, 230)], w=16)
    arc(d, c, (300, 140, 400, 240), 220, 320, w=12)
    return im


def icon_door_sensor(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 140, 230, 370), rad=8)
    rect(d, c, (260, 200, 360, 300), rad=8)
    return im


def icon_remote(color):
    im, d, c = draw_base(color)
    rect(d, c, (190, 100, 320, 400), rad=24)
    ellipse(d, c, (225, 140, 285, 200))
    for y in (240, 290, 340):
        rect(d, c, (220, y, 290, y + 30), rad=6)
    return im


def icon_bluetooth(color):
    im, d, c = draw_base(color)
    x, y = cxcy()
    line(d, c, [(x, 120), (x, 390)], w=16)
    line(d, c, [(x, 120), (x + 80, 200), (x, 255), (x + 80, 310), (x, 390)], w=16)
    line(d, c, [(x - 70, 200), (x, 255), (x - 70, 310)], w=16)
    return im


def icon_wifi(color):
    im, d, c = draw_base(color)
    x, y = 256, 340
    ellipse(d, c, (x - 16, y - 16, x + 16, y + 16))
    arc(d, c, (x - 60, y - 70, x + 60, y + 20), 200, 340, w=16)
    arc(d, c, (x - 100, y - 110, x + 100, y + 30), 200, 340, w=16)
    arc(d, c, (x - 140, y - 150, x + 140, y + 40), 200, 340, w=16)
    return im


def icon_gps(color):
    im, d, c = draw_base(color)
    x, y = 256, 220
    line(d, c, [(x, 120), (x + 80, 280), (x, 240), (x - 80, 280), (x, 120)], w=16)
    ellipse(d, c, (x - 25, y - 10, x + 25, y + 40))
    return im


def icon_sos(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (120, 120, 390, 390))
    # S O S as blocks
    for x0 in (160, 230, 300):
        rect(d, c, (x0, 220, x0 + 50, 290), rad=6)
    return im


def icon_fridge(color):
    im, d, c = draw_base(color)
    rect(d, c, (160, 100, 350, 400), rad=20)
    line(d, c, [(160, 220), (350, 220)], w=14)
    line(d, c, [(320, 140), (320, 200)], w=12)
    line(d, c, [(320, 250), (320, 330)], w=12)
    return im


def icon_freezer(color):
    im = icon_fridge(color)
    d = ImageDraw.Draw(im)
    # small snow
    line(d, color, [(220, 280), (280, 340)], w=10)
    line(d, color, [(280, 280), (220, 340)], w=10)
    line(d, color, [(250, 270), (250, 350)], w=10)
    return im


def icon_appliance(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 120, 370, 400), rad=18)
    ellipse(d, c, (190, 180, 320, 340))
    return im


def icon_washer(color):
    im = icon_appliance(color)
    d = ImageDraw.Draw(im)
    ellipse(d, color, (220, 210, 290, 280))
    return im


def icon_dryer(color):
    im = icon_washer(color)
    d = ImageDraw.Draw(im)
    arc(d, color, (200, 200, 310, 310), 40, 300, w=12)
    return im


def icon_stove(color):
    im, d, c = draw_base(color)
    rect(d, c, (120, 140, 390, 370), rad=12)
    for cx, cy in ((200, 220), (310, 220), (200, 310), (310, 310)):
        ellipse(d, c, (cx - 35, cy - 35, cx + 35, cy + 35))
    return im


def icon_oven(color):
    im, d, c = draw_base(color)
    rect(d, c, (130, 120, 380, 400), rad=14)
    rect(d, c, (160, 180, 350, 360), rad=10)
    line(d, c, [(180, 150), (220, 150)], w=12)
    line(d, c, [(290, 150), (330, 150)], w=12)
    return im


def icon_hood(color):
    im, d, c = draw_base(color)
    line(d, c, [(160, 200), (350, 200), (390, 280), (120, 280), (160, 200)], w=16)
    rect(d, c, (220, 120, 290, 200), rad=8)
    return im


def icon_charger(color):
    im, d, c = draw_base(color)
    rect(d, c, (170, 160, 340, 360), rad=20)
    line(d, c, [(255, 200), (230, 255), (270, 255), (245, 320)], w=16)
    return im


def icon_solar(color):
    im, d, c = draw_base(color)
    line(d, c, [(120, 340), (390, 200), (390, 340), (120, 340)], w=16)
    line(d, c, [(200, 340), (300, 250)], w=12)
    line(d, c, [(280, 340), (360, 280)], w=12)
    ellipse(d, c, (300, 110, 370, 180))
    return im


def icon_battery(color):
    im, d, c = draw_base(color)
    rect(d, c, (140, 180, 370, 330), rad=16)
    rect(d, c, (370, 220, 400, 290), rad=6)
    line(d, c, [(180, 210), (180, 300)], w=12)
    line(d, c, [(160, 255), (200, 255)], w=12)
    line(d, c, [(300, 255), (340, 255)], w=12)
    return im


def icon_compressor(color):
    im, d, c = draw_base(color)
    ellipse(d, c, (160, 160, 320, 360))
    rect(d, c, (280, 220, 380, 300), rad=10)
    line(d, c, [(200, 140), (200, 170)], w=14)
    return im


ICONS: list[tuple[str, str, callable]] = [
    # lighting
    ("panel_icon_tavan_aydinlatma", "lighting", icon_tavan),
    ("panel_icon_spot_aydinlatma", "lighting", icon_spot),
    ("panel_icon_led_serit", "lighting", icon_led),
    ("panel_icon_mutfak_aydinlatma", "lighting", icon_pendant),
    ("panel_icon_banyo_aydinlatma", "lighting", icon_globe),
    ("panel_icon_dis_aydinlatma", "lighting", icon_sun),
    ("panel_icon_okuma_lambasi", "lighting", icon_desk_lamp),
    ("panel_icon_yatak_basi", "lighting", icon_table_lamp),
    ("panel_icon_dolap_ici_aydinlatma", "lighting", icon_closet_light),
    ("panel_icon_zemin_aydinlatma", "lighting", icon_floor_light),
    # electric
    ("panel_icon_priz_220v", "electric", icon_outlet),
    ("panel_icon_usb_priz", "electric", icon_usb),
    ("panel_icon_inverter", "electric", icon_inverter),
    ("panel_icon_jenerator", "electric", icon_generator),
    ("panel_icon_kompresor", "electric", icon_compressor),
    ("panel_icon_elektrikli_isitici", "electric", icon_heater),
    ("panel_icon_su_pompasi_elektrik", "electric", icon_pump),
    ("panel_icon_fan", "electric", icon_fan),
    ("panel_icon_tv", "electric", icon_tv),
    ("panel_icon_mikrodalga", "electric", icon_microwave),
    ("panel_icon_sac_kurutma", "electric", icon_hairdryer),
    # water
    ("panel_icon_temiz_su_tanki", "water", lambda c: icon_water_tank(c, 0.7)),
    ("panel_icon_gri_su_tanki", "water", lambda c: icon_water_tank(c, 0.45)),
    ("panel_icon_pis_su_tanki", "water", lambda c: icon_water_tank(c, 0.2)),
    ("panel_icon_su_pompasi", "water", icon_pump),
    ("panel_icon_su_girisi", "water", icon_faucet_in),
    ("panel_icon_su_cikisi", "water", icon_faucet_out),
    ("panel_icon_su_filtresi", "water", icon_filter),
    ("panel_icon_musluk", "water", icon_faucet),
    ("panel_icon_dus", "water", icon_shower),
    ("panel_icon_sicak_su", "water", icon_hot_water),
    ("panel_icon_su_seviye", "water", icon_drop),
    # living
    ("panel_icon_yatak_odasi", "living", icon_bed),
    ("panel_icon_oturma_alani", "living", icon_sofa),
    ("panel_icon_mutfak", "living", icon_kitchen),
    ("panel_icon_banyo", "living", icon_shower),
    ("panel_icon_klozet", "living", icon_toilet),
    ("panel_icon_masa", "living", icon_table),
    ("panel_icon_koltuk", "living", icon_chair),
    ("panel_icon_dolap", "living", icon_wardrobe),
    ("panel_icon_kapi", "living", icon_door),
    ("panel_icon_pencere", "living", icon_window),
    ("panel_icon_perde", "living", icon_curtain),
    # climate
    ("panel_icon_klima", "climate", icon_ac),
    ("panel_icon_isitma", "climate", icon_heat_waves),
    ("panel_icon_havalandirma", "climate", icon_fan),
    ("panel_icon_hava_cikisi", "climate", icon_vent),
    ("panel_icon_sicaklik", "climate", icon_thermo),
    ("panel_icon_sogutma", "climate", icon_snow),
    ("panel_icon_nem", "climate", icon_humidity),
    ("panel_icon_gaz_dedektoru", "climate", lambda c: icon_sensor_text(c, "CO2")),
    ("panel_icon_duman_dedektoru", "climate", icon_smoke),
    ("panel_icon_co_dedektoru", "climate", lambda c: icon_sensor_text(c, "CO")),
    ("panel_icon_yangin_alarmi", "security", icon_alarm_bell),
    # security
    ("panel_icon_ac_kapat", "security", icon_power),
    ("panel_icon_kilit", "security", icon_lock),
    ("panel_icon_alarm", "security", icon_siren),
    ("panel_icon_kamera", "security", icon_camera),
    ("panel_icon_hareket_sensoru", "security", icon_motion),
    ("panel_icon_kapi_sensoru", "security", icon_door_sensor),
    ("panel_icon_uzaktan_kumanda", "security", icon_remote),
    ("panel_icon_bluetooth", "security", icon_bluetooth),
    ("panel_icon_wifi", "security", icon_wifi),
    ("panel_icon_gps", "security", icon_gps),
    ("panel_icon_acil_durum", "security", icon_sos),
    # other
    ("panel_icon_buzdolabi", "other", icon_fridge),
    ("panel_icon_dondurucu", "other", icon_freezer),
    ("panel_icon_bulasik_makinesi", "other", icon_appliance),
    ("panel_icon_camasir_makinesi", "other", icon_washer),
    ("panel_icon_kurutma_makinesi", "other", icon_dryer),
    ("panel_icon_ocak", "other", icon_stove),
    ("panel_icon_firin", "other", icon_oven),
    ("panel_icon_davlumbaaz", "other", icon_hood),
    ("panel_icon_sarj_cihazi", "other", icon_charger),
    ("panel_icon_gunes_paneli", "other", icon_solar),
    ("panel_icon_aku", "other", icon_battery),
]


def main() -> None:
    import numpy as np

    OUT.mkdir(parents=True, exist_ok=True)
    for name, cat, fn in ICONS:
        color = COLORS[cat]
        img = fn(color)
        bg = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
        bg.alpha_composite(img)
        arr = np.asarray(bg).copy()
        bri = arr[:, :, :3].max(axis=2)
        arr[:, :, 3] = np.where(bri > 30, 255, 0).astype(np.uint8)
        # clear pure black rgb where transparent
        arr[arr[:, :, 3] == 0] = 0
        Image.fromarray(arr, "RGBA").save(OUT / f"{name}.png")
    print(f"DONE {len(ICONS)} icons -> {OUT}")


if __name__ == "__main__":
    main()
