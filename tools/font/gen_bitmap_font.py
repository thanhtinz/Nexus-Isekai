#!/usr/bin/env python3
"""
Tao bitmap font sheet (PNG nhieu mau) + metrics (.dat) tu mot font .ttf/.otf HOP PHAP
(OFL/CC0 hoac da mua ban quyen). Dung cho client J2ME (MFont) va cac client khong co
font engine.

KHONG dung font trich tu game thuong mai (TeaMobi/NRO...) hay font doc quyen (Tahoma...).
Mac dinh dung Liberation Sans (OFL) co san trong moi truong de tao bo mau hop phap.

Cach dung:
  python3 gen_bitmap_font.py --font /duong/dan/BeVietnamPro.ttf --size 12 --name main --out ../../client-j2me/res/font

Dinh dang .dat (big-endian, doc bang java.io.DataInputStream):
  [4] magic 'BMF1'
  [1] height
  [1] spacing (px giua cac glyph khi ve)
  [2] count (so glyph)
  [count] widths (1 byte/glyph)
  [2] charsetByteLen
  [charsetByteLen] charset (UTF-8)
Sheet: 1 dai ngang, glyph i nam o [sum(width[0..i-1]) .. +width[i]), cao = height.
"""
import argparse, os, struct
from PIL import Image, ImageFont, ImageDraw

# Bo ky tu: ASCII in duoc + tieng Viet (thuong + HOA, du dau)
ASCII = "".join(chr(c) for c in range(32, 127))
VIET = (
    "àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ"
    "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ"
)
CHARSET = ASCII + VIET

# Mau xuat (giong bo cua game cu nhung TU SINH tu font hop phap)
COLORS = {
    "white":  (255, 255, 255, 255),
    "yellow": (255, 220, 80, 255),
    "red":    (235, 80, 80, 255),
    "green":  (110, 210, 110, 255),
    "blue":   (110, 170, 245, 255),
    "grey":   (160, 160, 160, 255),
    "orange": (245, 160, 70, 255),
}

def gen(font_path, size, name, out_dir, spacing=1):
    os.makedirs(out_dir, exist_ok=True)
    font = ImageFont.truetype(font_path, size)
    ascent, descent = font.getmetrics()
    height = ascent + descent
    # do rong tung glyph (advance, lam tron, toi thieu 1)
    widths = []
    for ch in CHARSET:
        w = int(round(font.getlength(ch)))
        if ch == " " and w <= 0:
            w = max(2, size // 3)
        widths.append(max(1, min(255, w)))
    total_w = sum(widths)

    # ve sheet cho tung mau
    for cname, rgba in COLORS.items():
        img = Image.new("RGBA", (total_w, height), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        x = 0
        for ch, w in zip(CHARSET, widths):
            d.text((x, 0), ch, font=font, fill=rgba)
            x += w
        img.save(os.path.join(out_dir, f"{name}_{cname}.png"))

    # ghi metrics .dat (big-endian)
    cs_bytes = CHARSET.encode("utf-8")
    with open(os.path.join(out_dir, f"{name}.dat"), "wb") as f:
        f.write(b"BMF1")
        f.write(struct.pack(">B", min(255, height)))
        f.write(struct.pack(">B", spacing))
        f.write(struct.pack(">H", len(widths)))
        f.write(bytes(widths))
        f.write(struct.pack(">H", len(cs_bytes)))
        f.write(cs_bytes)

    print(f"OK: {name}  height={height}  glyphs={len(CHARSET)}  colors={len(COLORS)}  sheetW={total_w}")
    print(f"     -> {out_dir}/{name}_<color>.png + {name}.dat")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--font", default="/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                    help="Font .ttf/.otf HOP PHAP (OFL/CC0/da mua). Mac dinh Liberation Sans (OFL).")
    ap.add_argument("--size", type=int, default=12)
    ap.add_argument("--name", default="main")
    ap.add_argument("--out", default="client-j2me/res/font")
    ap.add_argument("--spacing", type=int, default=1)
    a = ap.parse_args()
    gen(a.font, a.size, a.name, a.out, a.spacing)
