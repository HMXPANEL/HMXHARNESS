#!/usr/bin/env python3
"""Generate original HMXHARNESS decorative 3D-style artwork (no stock images).

All artwork is drawn programmatically with PIL: gradients, glow, isometric
geometry and glass panels. Outputs are 16:9 PNGs that blend into #080910 slides.
"""
from pathlib import Path
import math, random
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "generated"
OUT.mkdir(parents=True, exist_ok=True)
FONT = ROOT / "assets" / "fonts" / "Inter-Regular.ttf"

W, H = 1920, 1080
BG = (8, 9, 16)
NAVY = (17, 22, 41)
PURPLE = (139, 92, 246)
VIOLET = (167, 139, 250)
CYAN = (56, 189, 248)
WHITE = (248, 250, 252)
GREY = (161, 161, 170)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def base(cx=0.62, cy=0.42):
    """Dark radial gradient canvas with subtle purple/cyan atmosphere."""
    img = Image.new("RGB", (W, H), BG)
    px = img.load()
    for y in range(H):
        for x in range(W):
            d = math.hypot((x / W - cx) * 1.3, (y / H - cy) * 1.7)
            v = max(0.0, 1.0 - d)
            c = mix(BG, NAVY, min(0.75, v * 0.95))
            px[x, y] = c
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    g.ellipse([int(W*0.45), int(H*0.08), int(W*1.05), int(H*0.85)], fill=(*PURPLE, 46))
    g.ellipse([int(W*0.02), int(H*0.55), int(W*0.55), int(H*1.05)], fill=(*CYAN, 24))
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    return Image.alpha_composite(img.convert("RGBA"), glow)


def glow_layer(img, fn, color, blur=24, alpha=200):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    fn(ImageDraw.Draw(layer), (*color, alpha))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    return Image.alpha_composite(img, layer)


def rr(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def gradient_rect(img, box, top, bottom, radius=24, outline=None):
    x0, y0, x1, y1 = box
    strip = Image.new("RGB", (1, max(1, y1 - y0)))
    sp = strip.load()
    h = y1 - y0
    for i in range(h):
        sp[0, i] = mix(top, bottom, i / max(1, h - 1))
    mask = Image.new("L", (x1 - x0, h), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, x1 - x0 - 1, h - 1], radius=radius, fill=255)
    img.paste(strip.resize((x1 - x0, h)), (x0, y0), mask)
    if outline:
        ImageDraw.Draw(img).rounded_rectangle(box, radius=radius, outline=outline, width=2)


def phone(img, cx, cy, scale=1.0, tilt=0.0, screen_ui=True):
    """Draw a glossy 3D phone with holographic UI."""
    pw, ph = int(430 * scale), int(860 * scale)
    x0, y0 = int(cx - pw / 2), int(cy - ph / 2)
    # shadow
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sh)
    sd.rounded_rectangle([x0 + 26, y0 + 34, x0 + pw + 26, y0 + ph + 34], radius=int(58*scale), fill=(0, 0, 0, 170))
    img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(38)))
    # outer body
    d = ImageDraw.Draw(img)
    rr(d, [x0, y0, x0+pw, y0+ph], int(58*scale), fill=(24, 27, 45, 255), outline=(*VIOLET, 190), width=3)
    rr(d, [x0+10, y0+10, x0+pw-10, y0+ph-10], int(50*scale), fill=(10, 12, 24, 255))
    # screen
    sx0, sy0, sx1, sy1 = x0+24, y0+58, x0+pw-24, y0+ph-34
    gradient_rect(img, [sx0, sy0, sx1, sy1], (18, 22, 44), (8, 9, 18), radius=int(38*scale))
    d = ImageDraw.Draw(img)
    # status bar
    d.rounded_rectangle([sx0+30, sy0+22, sx0+120, sy0+30], radius=4, fill=(*GREY, 160))
    d.rounded_rectangle([sx1-110, sy0+22, sx1-30, sy0+30], radius=4, fill=(*GREY, 120))
    # header chips
    d.rounded_rectangle([sx0+22, sy0+58, sx0+int(pw*0.46), sy0+108], radius=18, fill=(*PURPLE, 70), outline=(*VIOLET, 150), width=2)
    d.rounded_rectangle([sx1-int(pw*0.30), sy0+58, sx1-22, sy0+108], radius=18, fill=(*CYAN, 45), outline=(*CYAN, 130), width=2)
    if screen_ui:
        # chat bubbles
        for i, (ww, col) in enumerate([(0.58, PURPLE), (0.74, NAVY), (0.62, PURPLE)]):
            by = sy0 + 150 + i * 150
            bx1 = sx1 - 20 if i % 2 == 0 else sx0 + int(pw*0.22) + int((sx1-sx0)*ww)
            bx0 = bx1 - int((sx1-sx0)*ww)
            d.rounded_rectangle([bx0, by, bx1, by+104], radius=18, fill=(*col, 130 if i % 2 == 0 else 190), outline=(*VIOLET, 80), width=1)
            for k in range(3):
                d.rounded_rectangle([bx0+18, by+20+k*24, bx0+int((bx1-bx0)*[0.7,0.9,0.5][k]), by+29+k*24], radius=5, fill=(*WHITE, 90))
        # tool card
        d.rounded_rectangle([sx0+20, sy1-210, sx1-20, sy1-88], radius=18, fill=(*CYAN, 36), outline=(*CYAN, 120), width=2)
        d.ellipse([sx0+36, sy1-186, sx0+72, sy1-150], fill=(*CYAN, 180))
        for k in range(2):
            d.rounded_rectangle([sx0+86, sy1-178+k*34, sx1-60-k*40, sy1-168+k*34], radius=5, fill=(*WHITE, 95))
        # composer
        d.rounded_rectangle([sx0+18, sy1-64, sx1-18, sy1-20], radius=22, fill=(30, 34, 58, 220), outline=(*VIOLET, 90), width=1)
        d.ellipse([sx1-58, sy1-58, sx1-22, sy1-22], fill=(*PURPLE, 210))
    # notch
    d.rounded_rectangle([cx-int(58*scale), y0+20, cx+int(58*scale), y0+38], radius=9, fill=(5, 6, 12, 255))
    # edge highlight (subtle vertical specular strip, not an arc)
    d.rounded_rectangle([x0+6, y0+int(ph*0.22), x0+11, y0+int(ph*0.62)], radius=3, fill=(*WHITE, 55))


def glass_card(img, x, y, w, h, accent, lines=3, label_bar=True):
    d = ImageDraw.Draw(img)
    rr(d, [x, y, x+w, y+h], 20, fill=(*NAVY, 205), outline=(*accent, 150), width=2)
    d.ellipse([x+18, y+18, x+50, y+50], fill=(*accent, 190))
    if label_bar:
        for i in range(lines):
            d.rounded_rectangle([x+18, y+66+i*26, x+w-24-((i%2)*30), y+76+i*26], radius=5, fill=(*WHITE, 80))


def holographic_arcs(img, cx, cy, r0):
    def fn(d, col):
        for i in range(4):
            r = r0 + i * 46
            d.arc([cx-r, cy-r, cx+r, cy+r], 200 + i*8, 350 - i*6, fill=col, width=3)
    glow_layer(img, fn, PURPLE, 18, 150)


def make_hero():
    img = base(0.68, 0.46)
    holographic_arcs(img, 1290, 470, 300)
    def fn(d, col):
        d.ellipse([250, 180, 900, 830], fill=col)
    img = glow_layer(img, fn, PURPLE, 90, 60)
    phone(img, 1300, 540, 1.02)
    glass_card(img, 190, 250, 330, 190, CYAN, 3)
    glass_card(img, 120, 520, 300, 170, VIOLET, 3)
    glass_card(img, 420, 760, 300, 160, PURPLE, 2)
    return img


def make_problem():
    img = base(0.72, 0.5)
    d = ImageDraw.Draw(img)
    d.line([960, 120, 960, 960], fill=(*GREY, 70), width=2)
    # left: desktop monitor + tower + cables
    rr(d, [180, 250, 780, 650], 18, fill=(26, 28, 38, 255), outline=(*GREY, 90), width=2)
    rr(d, [210, 280, 750, 620], 10, fill=(12, 14, 22, 255))
    for i in range(6):
        d.rounded_rectangle([240, 320+i*42, 560-((i%3)*70), 336+i*42], radius=5, fill=(*GREY, 55))
    rr(d, [330, 650, 430, 820], 10, fill=(26, 28, 38, 255), outline=(*GREY, 80), width=2)
    for off in range(3):
        d.arc([430+off*22, 700, 620+off*22, 900], 200, 340, fill=(*GREY, 55), width=5)
    # right: phone glowing
    def fn(dr, col):
        dr.ellipse([1080, 170, 1800, 890], fill=col)
    img = glow_layer(img, fn, PURPLE, 110, 70)
    phone(img, 1440, 540, 0.86)
    glass_card(img, 1010, 700, 300, 170, CYAN, 3)
    return img


def make_solution():
    img = base(0.5, 0.5)
    def fn(d, col):
        d.ellipse([560, 140, 1360, 940], fill=col)
    img = glow_layer(img, fn, VIOLET, 120, 65)
    phone(img, 960, 540, 0.92)
    cards = [
        (300, 180, 300, 165, CYAN), (1320, 175, 300, 165, PURPLE),
        (180, 480, 290, 165, VIOLET), (1450, 500, 290, 165, CYAN),
        (360, 800, 300, 160, PURPLE), (1280, 810, 300, 160, VIOLET),
    ]
    for c in cards:
        glass_card(img, *c, 3)
    d = ImageDraw.Draw(img)
    for x1, y1 in [(480, 300), (1400, 300), (350, 560), (1450, 580), (500, 850), (1300, 860)]:
        d.line([x1, y1, 960, 540], fill=(*VIOLET, 60), width=2)
    return img


def make_agent():
    img = base(0.5, 0.48)
    random.seed(7)
    cx, cy = 960, 500
    def fn(d, col):
        d.ellipse([cx-330, cy-330, cx+330, cy+330], fill=col)
    img = glow_layer(img, fn, PURPLE, 100, 70)
    d = ImageDraw.Draw(img)
    nodes = [(cx + int(170*math.cos(a)), cy + int(150*math.sin(a))) for a in [i*math.pi/6 for i in range(12)]]
    for n in nodes:
        for m in nodes:
            if math.hypot(n[0]-m[0], n[1]-m[1]) < 260:
                d.line([n, m], fill=(*CYAN, 45), width=2)
    for n in nodes:
        d.ellipse([n[0]-11, n[1]-11, n[0]+11, n[1]+11], fill=(*CYAN, 200))
    d.ellipse([cx-120, cy-120, cx+120, cy+120], fill=(*PURPLE, 150), outline=(*WHITE, 120), width=3)
    tools = [
        (170, 200, "F"), (1450, 200, "T"), (140, 470, "G"), (1550, 470, "B"),
        (250, 790, "M"), (1420, 790, "C"),
    ]
    for (x, y, _) in tools:
        glass_card(img, x, y, 260, 150, CYAN if x < 960 else PURPLE, 2)
        d.line([x+130, y+75, cx, cy], fill=(*VIOLET, 55), width=2)
    return img


def make_cloud():
    img = base(0.52, 0.5)
    d = ImageDraw.Draw(img)
    # cloud
    for box in [(760, 150, 1180, 420), (850, 100, 1120, 460), (700, 230, 1240, 430)]:
        d.ellipse(box, fill=(*NAVY, 235), outline=(*CYAN, 110), width=2)
    d.ellipse([930, 230, 1020, 320], fill=(*CYAN, 200))
    # stream
    for i in range(7):
        y = 430 + i*58
        d.line([960, y, 960, y+30], fill=(*PURPLE, 180-i*18), width=5)
    phone(img, 960, 830, 0.62)
    return img


def make_roadmap():
    img = base(0.5, 0.6)
    d = ImageDraw.Draw(img)
    platforms = [(220, 780, 560, 900), (560, 650, 900, 770), (900, 520, 1240, 640), (1240, 390, 1580, 510)]
    for i, b in enumerate(platforms):
        x0, y0, x1, y1 = b
        poly = [(x0, y0), (x1-70, y0), (x1, y0+60), (x0+70, y0+60)]
        d.polygon(poly, fill=(*NAVY, 230), outline=(*VIOLET, 150))
        d.line([(x0+70, y0+60), (x0+70, y1), (x1, y1), (x1, y0+60)], fill=(*PURPLE, 110), width=2)
        d.ellipse([x0+80, y0-70, x0+150, y0], fill=(*CYAN, 200) if i % 2 == 0 else (*PURPLE, 210))
        d.line([x0+115, y0, x0+115, y0+18], fill=(*WHITE, 120), width=3)
    return img


def make_closing():
    img = base(0.5, 0.5)
    random.seed(11)
    def fn(d, col):
        d.ellipse([500, 100, 1420, 1000], fill=col)
    img = glow_layer(img, fn, PURPLE, 130, 55)
    phone(img, 960, 560, 0.8)
    d = ImageDraw.Draw(img)
    for i in range(120):
        x = random.randint(180, 1740)
        y = random.randint(120, 960)
        r = random.choice([2, 2, 3, 4])
        col = random.choice([PURPLE, VIOLET, CYAN])
        d.ellipse([x-r, y-r, x+r, y+r], fill=(*col, random.randint(50, 180)))
    return img


if __name__ == "__main__":
    jobs = {
        "hero_phone": make_hero,
        "problem_split": make_problem,
        "solution_exploded": make_solution,
        "agent_brain": make_agent,
        "cloud_phone": make_cloud,
        "roadmap": make_roadmap,
        "closing": make_closing,
    }
    for name, fn in jobs.items():
        im = fn().resize((1600, 900), Image.LANCZOS)
        path = OUT / f"{name}.png"
        im.save(path, optimize=True)
        print(f"wrote {path.name} {path.stat().st_size//1024} KB")
