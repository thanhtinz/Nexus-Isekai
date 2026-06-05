# 👑 Hệ thống GM/Admin trong game

Tài khoản admin (players.is_admin = TRUE) khi login vào game có **character riêng**
bình thường, nhưng được mở khóa toàn bộ quyền GM.

## Cách kích hoạt
```sql
UPDATE players SET is_admin = TRUE WHERE username = 'tên_account';
```
Login bằng account đó → server tự set cờ GM (`PlayerSession.isGm`).

## Lệnh GM (2 cách nhập)

**1. Chat command** — gõ trong khung chat, bắt đầu bằng `/`:
**2. Panel GM** — bấm phím **F8** mở bảng điều khiển (ô nhập + nút nhanh).

| Lệnh | Tác dụng |
|------|----------|
| `/help` | Danh sách lệnh |
| `/tp x y [zone]` | Dịch chuyển tới tọa độ |
| `/tpto <tên>` | Tới chỗ người chơi |
| `/give gold <n>` | Nhận vàng |
| `/setlevel <n>` | Đặt cấp |
| `/heal` | Hồi đầy máu/mana |
| `/kill <mobId>` | Giết quái |
| `/spawn <tplId>` | Spawn quái từ template |
| `/invis <0\|1\|2>` | Tàng hình (xem dưới) |
| `/possess npc\|mob <id>` | Nhập vào NPC/mob |
| `/release` | Thoát điều khiển |
| `/broadcast <msg>` | Thông báo toàn server |
| `/kick <tên>` | Kick người chơi |
| `/where` | Vị trí hiện tại |

## Tàng hình (2 mức)

- **Mức 1** (`/invis 1`): ẩn khỏi danh sách người chơi trong zone, quái không aggro. Người khác vẫn không thấy trong list.
- **Mức 2** (`/invis 2`): tàng hình hoàn toàn — bị xóa khỏi tầm nhìn mọi người (gửi S_PLAYER_LEFT).
- `/invis 0`: hiện hình lại.

Client làm mờ nhân vật GM theo mức (mức 1 = 50%, mức 2 = 25% alpha) để GM tự biết.

## Nhập (possess) NPC/mob

`/possess mob 5` hoặc `/possess npc 12` → bắt đầu điều khiển.

Khi đang điều khiển (PossessController.Active):
- **WASD**: di chuyển NPC/mob đó (gửi C_GM_POSSESS_MOVE → server cập nhật + broadcast)
- **Phím 1**: NPC/mob "nói" (chat trong zone với tên NPC/mob)
- **Phím 2**: hành động — mob thì tấn công người chơi, NPC thì đổi activity/emote
- `/release` hoặc nút Release: thoát, trả input về character GM

Hai chế độ gộp làm một: vừa **điều khiển thay** (di chuyển), vừa **ra lệnh hành động** (nói/đánh/emote).

## Packet (0xE0-0xE8)

| Packet | ID | Nội dung |
|--------|----|----|
| C_GM_COMMAND | 0xE0 | text lệnh |
| S_GM_RESULT | 0xE1 | kết quả lệnh |
| C_GM_POSSESS | 0xE2 | type, id |
| S_GM_POSSESS_OK | 0xE3 | type, id (hoặc npc_move + x,y) |
| C_GM_POSSESS_MOVE | 0xE4 | x, y |
| C_GM_POSSESS_ACTION | 0xE5 | action, arg |
| C_GM_RELEASE | 0xE6 | — |
| C_GM_INVISIBLE | 0xE7 | level |
| S_GM_INVISIBLE | 0xE8 | level |

## Bảo mật

Mọi lệnh GM **kiểm tra `s.isGm()` ở server** trước khi thực thi — client gửi lệnh GM mà không phải admin sẽ bị từ chối. Không tin client.

## Client (Unity)

- `GMPanel.cs` — bảng F8, ô lệnh, nút nhanh, log kết quả. Gọi `EnableGm()` sau login nếu là admin.
- `PossessController.cs` — WASD điều khiển đối tượng possess, phím ra lệnh, khóa input GM.

Setup Editor: thêm GMPanel + PossessController vào scene game, gán reference. Xem UNITY-SCENE-SETUP.md.
