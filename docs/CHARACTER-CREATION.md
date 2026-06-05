# 🧙 Hệ thống Tạo Nhân Vật — Hướng dẫn đầy đủ

Tài liệu nối toàn bộ pipeline tạo nhân vật: từ asset → admin cấu hình → server validate → client render.

## Luồng tổng thể

```
game-assets/ (sprite paperdoll)
   │ ① import-assets.js → bảng `assets`
   ▼
Admin Panel /charcreation
   │ ② cấu hình races + options (da/mắt/tóc/áo), gắn asset
   │    → preview ghép layer ngay trong admin (sharp)
   ▼
char_races + char_options (DB)
   │ ③ server Java đọc qua JdbcTemplate
   ▼
Game Server (CharCreationService + Handler)
   │ ④ client xin options → validate → tạo character
   ▼
Client Unity (CharacterCreationUI)
   │ ⑤ dựng UI, ghép sprite paperdoll, gửi lựa chọn
   ▼
CharacterEntity.outfit_json = {race,skin,eyes,hair,outfit}
```

## ① Import asset vào DB

```bash
cd admin-panel
node src/scripts/import-assets.js
# → copy game-assets/*.png vào public/uploads/game-assets/
#   + insert vào bảng `assets` (tự phân loại type/category)
```

## ② Cấu hình trong Admin

```bash
# Seed sẵn lựa chọn mẫu (tự map asset_id theo tên file)
psql -U fro -d fantasyrealm -f deploy/sql/04-seed-charcreation.sql
```

Vào Admin → **Tạo nhân vật** (`/charcreation`):
- Thêm/sửa chủng tộc + giới tính (gắn body base sprite, khóa theo phe)
- 4 slot: da / mắt / tóc / áo quần — gắn asset, khóa theo race/gender, đặt mặc định
- Panel **Xem trước** ghép layer real-time (cần `npm i sharp`)
- **Export JSON** (`/charcreation/export/json`) cho client tham khảo

## ③④ Server xử lý

`CharCreationService` đọc `char_races`/`char_options` qua JdbcTemplate.
`CharCreationHandler` xử lý 2 packet:

| Packet | ID | Chiều | Nội dung |
|--------|----|----|----------|
| C_CHAR_CREATE_OPTIONS | 0x36 | client→server | Xin danh sách lựa chọn |
| S_CHAR_CREATE_OPTIONS | 0x37 | server→client | JSON {races,skin,eyes,hair,outfit} |
| C_CHAR_CREATE | 0x38 | client→server | name,race,skin,eyes,hair,outfit |
| S_CHAR_CREATE_OK | 0x39 | server→client | id,name,faction,outfitJson |
| S_CHAR_CREATE_FAIL | 0x3A | server→client | Thông báo lỗi |

Server validate: tên 2-16 ký tự, chưa trùng, race tồn tại, mỗi lựa chọn hợp lệ với race/gender (theo race_filter/gender_filter trong admin).

## ⑤ Client Unity

### Chuẩn bị sprite

```bash
# Tách frame idle-south từ mỗi sheet → preview-frames/
cd game-assets && python3 extract-preview-frames.py

# Copy vào Unity
cp -r game-assets/preview-frames/* <UnityProject>/Assets/Resources/CharParts/
# → Resources/CharParts/skin/humn_v03.png, eyes/eye_02.png, hair/bob2.png...
```

> Import vào Unity với Texture Type = Sprite (2D and UI), Filter = Point (no filter), Compression = None để giữ pixel-art sắc nét.

### Gắn script

1. Tạo Canvas, thêm `CharacterCreationUI` component
2. Kéo thả: nameInput, các container (race/skin/eyes/hair/outfit), optionButtonPrefab, createButton, errorText
3. Preview: 4 Image xếp chồng đúng thứ tự (dưới→trên): Skin → Outfit → Eyes → Hair
4. Gán `GameNetworkManager` vào field `net`

Script tự: xin options khi enable → dựng nút → ghép preview → gửi tạo.

### Paperdoll layer order

```
0bas (skin)  →  1out (outfit)  →  3fac (eyes)  →  4har (hair)  →  5hat
   dưới cùng                                                        trên cùng
```

Trong game thật (không chỉ preview), dùng `CharPartLoader.LoadFrame(slot, code, frameIndex)`
để lấy frame theo animation (walk/run/idle 8 hướng). Slice sheet 512x512 thành grid 64x64.

## Lưu trữ

Lựa chọn ngoại hình lưu trong `characters.outfit_json`:
```json
{"race":"humn_male","skin":"humn_v03","eyes":"eye_02","hair":"bob2","outfit":"undi"}
```
Khi load nhân vật vào game, đọc JSON này → ghép lại paperdoll y hệt.

## J2ME

Client J2ME nhẹ hơn — có thể chỉ cho chọn race + vài preset thay vì full paperdoll
(màn hình nhỏ). Đọc cùng packet 0x36/0x38, render sprite đơn giản hóa.

---

## Render ngoại hình trong game (CharacterAppearance)

`CharacterAppearance.cs` áp paperdoll lên player GameObject từ outfit JSON.

**Setup prefab người chơi:**
1. Player prefab có 4 SpriteRenderer con, sorting order tăng dần:
   `skin (0) → outfit (1) → eyes (2) → hair (3)`
2. Gắn `CharacterAppearance`, kéo 4 renderer vào các field tương ứng
3. Khi spawn / đổi outfit, hệ thống tự gọi `ApplyJson(outfitJson)` để ghép sprite

**Dùng cho:**
- Người chơi khác: `OtherPlayerManager` tự gọi khi spawn + khi nhận `S_CHANGE_OUTFIT`
- Nhân vật của mình: gọi `appearance.ApplyJson(PlayerPrefs.GetString("char_outfit"))` khi vào game

**Sau khi tạo nhân vật:** `CharacterCreationUI` lưu name/faction/outfit vào PlayerPrefs
rồi `SceneManager.LoadScene(gameSceneName)`. Đặt `gameSceneName` đúng tên scene game
(và thêm scene đó vào Build Settings).
