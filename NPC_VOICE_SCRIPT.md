# KỊCH BẢN LỜI THOẠI NPC (cho thu âm) — Vọng Linh Giới

> Đây là các câu NGẮN (bark) NPC nói khi người chơi tương tác — KHÔNG phải full hội thoại.
> Mỗi dòng: [Câu thoại] + (ghi chú giọng điệu cho diễn viên).
> Khi thu xong, đặt file vào `Audio/Voice/Npc/`, khai báo trong admin:
>   - Bảng Kho Audio (audio_assets): asset_key + file_path
>   - Bảng Lời Thoại (voice_lines): context='npc_bark', ref_id=<npc_id>, audio_key, subtitle
> Nhiều câu cùng một NPC → game tự chọn ngẫu nhiên theo weight.

---

## NPC type 1 — Cửa Hàng (Shopkeeper)
Giọng: niềm nở, lanh lợi, hơi tía lia kiểu người bán hàng. Tông trung-cao, nhịp nhanh.

1. "Ghé xem hàng đi lữ khách, đồ mới về đấy!" — (mời chào, hồ hởi, nhấn 'mới về')
2. "Hàng tốt giá hữu nghị, mua nhanh kẻo hết!" — (giục nhẹ, vui vẻ)
3. "Cần gì cứ nói, ta có đủ cả." — (tự tin, thân thiện)
4. "Cảm ơn đã ghé, hẹn gặp lại!" — (tạm biệt, ấm áp, nhỏ dần)

## NPC type 2 — Nhiệm Vụ (Quest Giver)
Giọng: trầm, nghiêm túc, có chút khẩn trương. Tông trung-trầm, nhịp chậm rõ chữ.

1. "Ta có việc hệ trọng cần ngươi giúp." — (nghiêm, gửi gắm)
2. "Vận mệnh vùng đất này nằm trong tay ngươi đấy." — (nặng lời, nhấn 'vận mệnh')
3. "Hoàn thành đi, phần thưởng sẽ xứng đáng." — (động viên, chắc nịch)
4. "Tốt lắm, ta đã không nhìn lầm người." — (hài lòng, ấm hơn)

## NPC type 3 — Kho (Storage Keeper)
Giọng: điềm đạm, chậm rãi, đáng tin như người giữ của. Tông trầm, bình tĩnh.

1. "Đồ đạc của ngươi ta giữ kỹ, yên tâm." — (trấn an, chậm)
2. "Cần cất hay lấy gì cứ tự nhiên." — (thân thiện, điềm tĩnh)
3. "Kho luôn mở cho ngươi." — (ngắn, ấm)

## NPC type 4 — Chế Tạo (Blacksmith / Crafting)
Giọng: khàn, mạnh, dứt khoát kiểu thợ rèn. Tông trầm-khàn, có lực.

1. "Muốn rèn gì? Ta làm cho ra trò!" — (sang sảng, tự hào nghề)
2. "Đưa nguyên liệu đây, để ta lo." — (chắc nịch, ngắn gọn)
3. "Lửa lò đang nóng, làm liền!" — (hào hứng, có nhiệt)
4. "Đồ này ta rèn, bảo đảm bền." — (tự hào)

## NPC type 5 — Ngân Hàng (Banker)
Giọng: lịch sự, trang trọng, hơi formal. Tông trung, rõ ràng, lễ độ.

1. "Kính chào quý khách, cần gửi hay rút?" — (lễ độ, trang trọng)
2. "Tài sản của ngài được bảo đảm tuyệt đối." — (đảm bảo, chuyên nghiệp)
3. "Hân hạnh phục vụ ngài." — (cúi chào, nhã nhặn)

---

## NPC đặc biệt (gắn theo npc_id cụ thể — admin điền ref_id)
### Trưởng làng (npc gốc cốt truyện)
Giọng: già, ấm, từng trải, hơi run nhẹ tuổi tác. Tông trầm chậm.
1. "Ngươi đã đến rồi... ta chờ ngươi lâu lắm." — (xúc động, chậm)
2. "Hãy mạnh mẽ lên, cả làng trông cậy vào ngươi." — (gửi gắm, run nhẹ)

### Lính gác cổng
Giọng: cứng, ngắn, kỷ luật quân ngũ. Tông trung-cao, dứt khoát.
1. "Đứng lại! ...à, người quen. Qua đi." — (gắt rồi dịu)
2. "Phía trước nguy hiểm, cẩn thận đấy." — (cảnh báo, nghiêm)

### Thương nhân lang thang
Giọng: ranh mãnh, đong đưa, kiểu rao hàng bí ẩn. Tông trung, lên xuống.
1. "Suỵt... ta có món hàng đặc biệt, chỉ cho ngươi thôi." — (thì thầm, bí hiểm)
2. "Hàng hiếm đấy, không mua là tiếc cả đời!" — (mời mọc, phóng đại vui)

---

## Gợi ý kỹ thuật thu âm
- Định dạng: .ogg (hoặc .wav rồi convert), mono, 44.1kHz; mỗi câu 1-3 giây.
- Âm lượng đều, tránh nền ồn; chừa ~0.2s khoảng lặng đầu/cuối.
- Đặt tên file rõ: `npc_<loại>_<số>.ogg` (vd `npc_shop_1.ogg`), khớp asset_key khai trong admin.
- Phụ đề (subtitle) trong voice_lines nên trùng câu thoại để hiện chữ khi phát.

> Liên quan: ASSET_HUB.md (Audio/Voice/Npc/) · bảng admin Lời Thoại (Voice) + Kho Audio.
