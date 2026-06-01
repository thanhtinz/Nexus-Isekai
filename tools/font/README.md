# Bitmap Font Generator

`gen_bitmap_font.py` tao bitmap font (PNG nhieu mau + .dat metrics) tu font `.ttf/.otf` HOP PHAP,
cho client J2ME (`com.nexusisekai.ui.MFont`) va cac client khong co font engine.

## Quan trong ve ban quyen
- CHI dung font OFL/CC0 hoac da mua giay phep. KHONG dung font trich tu game thuong mai
  (TeaMobi/Ninja School/NRO...) hay font doc quyen (Tahoma cua Microsoft...).
- Bo mau dang co trong `client-j2me/res/font/` duoc tao tu **Liberation Sans (OFL)** — hop phap.
- Khi co font rieng (vd Be Vietnam Pro), chay lai de thay:

```
pip install Pillow --break-system-packages
python3 tools/font/gen_bitmap_font.py --font /path/BeVietnamPro-Regular.ttf --size 12 --name main  --out client-j2me/res/font
python3 tools/font/gen_bitmap_font.py --font /path/BeVietnamPro-Regular.ttf --size 10 --name small --out client-j2me/res/font
```

## Client nao can bitmap font?
- **J2ME** (`client-j2me`): CAN — MIDP khong co font engine tot. Dung `MFont`.
- **Android / iOS / PC / Unity**: dung font `.ttf` NATIVE (Android Typeface, Unity TextMeshPro...).
  Khong can bitmap, chi can tha file `.ttf` OFL vao project (xem client/Assets/Fonts/README.md cho Unity).

## Charset
ASCII in duoc + day du nguyen am tieng Viet (thuong + HOA). Sua bien CHARSET trong script neu can them.
