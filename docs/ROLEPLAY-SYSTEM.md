# 🎭 Hệ thống Roleplay (RP) — chất tự do nhập vai kiểu GTA RP, bản 2D fantasy

Không phải clone GTA V, mà lấy *tinh thần RP*: tự do nhập vai, làm nghề kiếm sống,
hành động có hậu quả, thế giới phản ứng. Trong khuôn khổ 2D fantasy.

## 1. Luật vùng (an toàn / tự do / PvP) — kết hợp cả 3

Mỗi zone có 2 thuộc tính (trong ZoneType):
- **isSafe**: vùng an toàn — cấm mọi combat, không truy nã (thành phố, học viện, dân cư)
- **isPvp**: vùng PvP — cho phép đánh người chơi (vùng phiêu lưu, ma giới, thảo nguyên)
- Còn lại: vùng thường (đánh quái được, PvP không)

Client nhận S_ZONE_RULE khi vào zone → hiện "🛡 Vùng An Toàn" / "⚔ Vùng PvP".

## 2. Danh tiếng (Karma) & Truy nã (Wanted)

**Karma** — danh hiệu theo hành vi:
Đại Ác Nhân (≤-100) → Kẻ Xấu → Trung Lập → Người Tốt → Anh Hùng (≥100)

**Wanted (0-5 sao)** — làm điều xấu bị truy nã:
- Đánh người chơi (vùng PvP): +1 sao, -10 karma
- Giết người chơi: thêm +2 sao
- Truy nã ≥3 sao: bị cảnh báo toàn zone + **không vào được vùng an toàn**
- Tự giảm 1 sao mỗi phút nếu không phạm tội mới

**Nghề lương thiện** tăng karma → cân bằng với hành vi xấu.

## 3. Nghề RP (làm việc kiếm sống)

`C_RP_JOB_START` với nghề: blacksmith (thợ rèn), farmer (nông dân),
merchant (lái buôn), guard (vệ binh), healer (thầy thuốc), miner (thợ mỏ).
Làm việc → nhận lương (mỗi nghề mức khác nhau) + 2 karma.

## 4. Emote / cử chỉ RP

`C_RP_EMOTE`: sit, wave, dance, bow, laugh, cry, point, sleep...
→ broadcast cả zone, người khác thấy animation/cử chỉ.

`C_RP_STATUS`: đặt câu trạng thái hiện trên đầu nhân vật (vd "đang câu cá").

## Packet (0xD0-0xD9)

| Packet | ID | Nội dung |
|--------|----|----|
| C_ATTACK_PLAYER | 0xD0 | targetId (PvP) |
| S_WANTED_UPDATE | 0xD1 | số sao |
| S_KARMA_UPDATE | 0xD2 | karma, danh hiệu |
| C_RP_EMOTE / S_RP_EMOTE | 0xD3/D4 | emote code |
| C_RP_STATUS / S_RP_STATUS | 0xD5/D6 | text trạng thái |
| C_RP_JOB_START / S_RP_JOB_RESULT | 0xD7/D8 | nghề, lương |
| S_ZONE_RULE | 0xD9 | isSafe, isPvp |

## Server
- ReputationService: karma/wanted, decay, chặn vùng an toàn
- RpHandler: PvP (tôn trọng luật vùng), emote, status, nghề
- ZoneType: isSafe/isPvp mỗi vùng; ZoneManager chặn tội phạm vào vùng an toàn

## Client (Unity)
- RpSystem.cs: HUD sao truy nã + karma, emote, status, nghề, hiển thị luật vùng
- OtherPlayerManager.AttackPlayer(): gửi PvP

Setup Editor: gắn RpSystem vào HUD, tạo emote panel + job panel, gán reference.
