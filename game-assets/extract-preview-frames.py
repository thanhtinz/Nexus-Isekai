#!/usr/bin/env python3
"""
Tách frame idle-south (frame 0 = ô 64x64 góc trên trái) từ mỗi sprite sheet
trong game-assets/ → xuất ra preview-frames/{slot}/{code}.png

Dùng cho:
  - Unity: copy vào Assets/Resources/CharParts/{slot}/  (CharPartLoader.Load đọc)
  - Admin preview / web client

Chạy:  python3 extract-preview-frames.py
Cần:   pip install Pillow
"""
import os, re, glob
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT  = os.path.join(ROOT, "preview-frames")
FRAME = 64

def save_frame(src, slot, code):
    os.makedirs(os.path.join(OUT, slot), exist_ok=True)
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    box = (0, 0, min(FRAME, w), min(FRAME, h))
    frame = img.crop(box)
    # pad về 64x64 nếu nhỏ hơn
    if frame.size != (FRAME, FRAME):
        canvas = Image.new("RGBA", (FRAME, FRAME), (0,0,0,0))
        canvas.alpha_composite(frame)
        frame = canvas
    frame.save(os.path.join(OUT, slot, f"{code}.png"))

count = 0

# SKIN / BODY: char_a_p1_0bas_{race}_v{NN}
for f in glob.glob(f"{ROOT}/characters/base/char_a_p1/*0bas*.png"):
    m = re.search(r'0bas_([a-z]+_v\d+)', os.path.basename(f))
    if m: save_frame(f, "skin", m.group(1)); count += 1

# EYES: eye_v{NN} → eye_{NN}
for f in glob.glob(f"{ROOT}/characters/eyes/**/*eye_v*.png", recursive=True):
    m = re.search(r'eye_v(\d+)', os.path.basename(f))
    if m: save_frame(f, "eyes", f"eye_{m.group(1)}"); count += 1

# HAIR: char_a_p1_4har_{style}_v01 → {style}
for f in glob.glob(f"{ROOT}/characters/hairstyles/**/*4har*_v01.png", recursive=True):
    m = re.search(r'4har_([a-z0-9]+)_v01', os.path.basename(f))
    if m: save_frame(f, "hair", m.group(1)); count += 1

# OUTFIT: char_a_p1_1out_{code}_v01 → {code}
for f in glob.glob(f"{ROOT}/characters/**/*1out*_v01.png", recursive=True):
    m = re.search(r'1out_([a-z0-9]+)_v01', os.path.basename(f))
    if m: save_frame(f, "outfit", m.group(1)); count += 1

print(f"✅ Xuất {count} preview frame → {OUT}/")
print("   Copy preview-frames/* vào Unity Assets/Resources/CharParts/")
