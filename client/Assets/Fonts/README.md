# Fonts

Thư mục này chứa font cho game (UI + TextMeshPro). Trước đây có 4 file `.ttf` rỗng 0 byte
(barmeneb, chelthm, normalFont1, staccato) — đã gỡ vì file rỗng làm Unity báo lỗi import và
không file nào được tham chiếu.

## Không thể commit sẵn font ở đây
Font là tài sản nhị phân và đa phần có bản quyền — không nhúng sẵn vào repo để tránh vi phạm.
Đội ngũ tự tải font hợp lệ (giấy phép OFL/CC0/thương mại đã mua) rồi đặt vào thư mục này.

## Font miễn phí (OFL) hỗ trợ tiếng Việt đầy đủ — gợi ý
Tải tại Google Fonts (fonts.google.com), giấy phép SIL Open Font License, dùng thương mại OK:

- Body / UI chính: "Be Vietnam Pro" (thiết kế riêng cho tiếng Việt) hoặc "Inter", "Roboto".
- Tiêu đề / số: "Rajdhani", "Oswald" (lưu ý kiểm dấu tiếng Việt trước khi dùng).
- Bao phủ ký tự rộng (dự phòng): "Noto Sans".

Không dùng các font thương mại có bản quyền (vd các font giống barmeneb/staccato) trừ khi đã mua giấy phép.

## Cách thêm vào game
1. Chép file `.ttf`/`.otf` vào thư mục `client/Assets/Fonts/`.
2. Mở Unity → Window > TextMeshPro > Font Asset Creator → chọn font nguồn →
   Character Set = "Custom Characters" và dán bộ ký tự tiếng Việt, hoặc "Unicode Range (Hex)"
   bao gồm Latin Extended (0x1EA0-0x1EF9 cho dấu tiếng Việt) → Generate → Save thành `*_SDF.asset`.
3. Gán Font Asset SDF vào component TextMeshProUGUI, hoặc đặt làm Default Font Asset trong
   Project Settings > TextMesh Pro > Settings.

## Bộ ký tự tiếng Việt (dán vào Custom Characters khi tạo SDF)
Bao gồm: chữ Latin cơ bản, số, dấu câu, và toàn bộ nguyên âm có dấu:
àáảãạ ăằắẳẵặ âầấẩẫậ èéẻẽẹ êềếểễệ ìíỉĩị òóỏõọ ôồốổỗộ ơờớởỡợ ùúủũụ ưừứửữự ỳýỷỹỵ đ
(và các dạng IN HOA tương ứng).
