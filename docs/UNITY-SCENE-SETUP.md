# 🎬 Hướng dẫn ráp Scene Unity (phần Editor)

Các script C# đã viết đầy đủ logic, nhưng Unity cần bạn **dựng scene + prefab + gán reference** trong Editor — phần này không thể tự động từ code. Tài liệu này hướng dẫn từng bước.

> Đọc trước: `BUILD-UNITY.md` (tạo project, import script) và `CHARACTER-CREATION.md` (sprite paperdoll).

## Tổng quan 2 scene

| Scene | Vai trò | Script chính |
|-------|---------|--------------|
| `Login` | Đăng nhập + tạo nhân vật | GameNetworkManager, CharacterCreationUI |
| `GameWorld` | Thế giới game | GameBootstrap, PlayerCharacterController, các UI |

---

## 1. Sprite: slice sheet thành frame

Sheet Seliel/Mana Seed là 512×512, lưới 8×8 ô 64×64.

1. Import sheet vào `Assets/Resources/Sheets/` (hoặc thư mục riêng)
2. Chọn file → Inspector:
   - Texture Type: **Sprite (2D and UI)**
   - Sprite Mode: **Multiple**
   - Filter Mode: **Point (no filter)** ← giữ pixel sắc nét
   - Compression: **None**
3. Mở **Sprite Editor** → Slice → Grid By Cell Size → 64×64 → Slice → Apply
4. Sprite con tự đặt tên `{sheet}_0`, `{sheet}_1`... (index = cell)

Hoặc dùng frame preview đã tách sẵn: copy `game-assets/preview-frames/*` vào
`Assets/Resources/CharParts/{slot}/` (mỗi file 1 sprite idle — đủ cho preview tạo nhân vật).

> `CharPartLoader.LoadFrame(slot, code, cellIndex)` load `Resources/CharParts/{slot}/{code}_{cellIndex}`.
> `CharPartLoader.Load(slot, code)` load `Resources/CharParts/{slot}/{code}` (frame idle đơn).

---

## 2. Player Prefab (paperdoll)

Tạo prefab nhân vật dùng cho cả người chơi mình và người khác:

```
Player (GameObject gốc)
├── Rigidbody2D (Gravity Scale = 0, Freeze Rotation Z)
├── CircleCollider2D (hoặc BoxCollider2D)
├── PlayerCharacterController   (cho nhân vật mình)
│   hoặc RemotePlayerMover      (cho người khác)
├── SpriteSheetAnimator
├── CharacterAppearance
├── Sprites (child, sorting order tăng dần)
│   ├── SkinLayer   (SpriteRenderer, order 0)
│   ├── OutfitLayer (SpriteRenderer, order 1)
│   ├── EyesLayer   (SpriteRenderer, order 2)
│   └── HairLayer   (SpriteRenderer, order 3)
└── NameLabel (child, Canvas World Space hoặc TextMesh)
```

**Gán reference trong Inspector:**
- `SpriteSheetAnimator.layers` = [SkinLayer, OutfitLayer, EyesLayer, HairLayer]
- `SpriteSheetAnimator.layerSlots` = ["skin","outfit","eyes","hair"]
- `CharacterAppearance`: kéo 4 SpriteRenderer vào skinLayer/outfitLayer/eyesLayer/hairLayer
- `PlayerCharacterController.animator` = SpriteSheetAnimator
- `PlayerCharacterController.rb` = Rigidbody2D

Tạo 2 biến thể prefab: **PlayerSelf** (có PlayerCharacterController) và
**PlayerRemote** (có RemotePlayerMover thay vì controller).

---

## 3. Scene Login

```
Login (scene)
├── GameNetworkManager      (host/port server, DontDestroyOnLoad)
├── PacketRouter            (DontDestroyOnLoad)
├── Canvas
│   ├── LoginPanel (username, password, nút Đăng nhập)
│   └── CharacterCreationUI  ← màn tạo nhân vật
│       ├── nameInput
│       ├── raceContainer / skinContainer / eyesContainer / hairContainer / outfitContainer
│       ├── optionButtonPrefab (prefab nút lựa chọn: Button + Text con)
│       ├── 4 preview Image (Skin/Outfit/Eyes/Hair xếp chồng)
│       ├── createButton, errorText
│       └── gameSceneName = "GameWorld"
```

Gán `GameNetworkManager` vào `CharacterCreationUI.net`.
Thêm cả 2 scene vào **File → Build Settings → Scenes In Build**.

---

## 4. Scene GameWorld

```
GameWorld (scene)
├── GameManager
│   ├── GameBootstrap        (kéo player, camera2D, otherPlayers vào)
│   ├── OtherPlayerManager   (playerPrefab = PlayerRemote prefab)
│   └── ClientZoneManager
├── Main Camera
│   └── CameraFollow         (target tự nối qua GameBootstrap)
├── Grid/Tilemap            (bản đồ — vẽ bằng Tilemap hoặc sprite nền)
├── PlayerSelf (prefab instance, hoặc để GameBootstrap spawn)
└── Canvas (Screen Space - Overlay)
    ├── PlayerHUD            (nameText, levelText, goldText, factionIcon, expBar, các nút)
    ├── ChatUI               (chatPanel, scrollRect, messageContainer, messagePrefab, inputField)
    ├── InventoryUI          (panel, slotContainer, slotPrefab, detailText)
    ├── SocialUI             (panel, friendListContainer, mailContainer, requestContainer + prefab)
    ├── MarketUI
    ├── FactionUI
    ├── AchievementUI        (popup, titleText, descText)
    └── EventNotificationSystem (banner, bannerText)
```

**Prefab phụ cần tạo:**
- `messagePrefab`: 1 dòng chat (Text)
- `slotPrefab` (inventory): GameObject có child "Icon" (Image) + "Count" (Text) + Button
- `friendItemPrefab`, `mailItemPrefab`, `requestItemPrefab`: dòng + Text (+ Button cho request)
- `optionButtonPrefab`: Button + Text (cho màn tạo nhân vật)

---

## 5. Animation 8 hướng

`SpriteSheetAnimator` đã map sẵn cell index theo guide Mana Seed (Walk/Run/Idle × 4 hướng).
Hướng Trái = lật ngang hướng Phải (flipX).

Nếu cell index không khớp sheet của bạn, sửa bảng `MAP` trong `SpriteSheetAnimator.cs`
theo `game-assets/.../farmer base animation guide.png` (số vàng = cell index, số tím = timing ms).

---

## 6. Kiểm tra chạy

1. Mở scene Login → Play
2. Đăng nhập → màn tạo nhân vật hiện options (server phải đang chạy + đã seed charcreation)
3. Chọn da/mắt/tóc/áo → preview ghép layer real-time
4. Tạo → chuyển sang GameWorld, nhân vật xuất hiện đúng ngoại hình
5. WASD di chuyển (animation 8 hướng), Shift chạy, Enter chat, I mở túi đồ

> Nếu nhân vật trắng/không hiện: kiểm tra sprite đã slice + đặt đúng `Resources/CharParts/`.
> Nếu không kết nối: kiểm tra host/port trong GameNetworkManager + server đang chạy + firewall.

---

## Tóm tắt việc Editor (không code thay được)

- [ ] Slice sprite sheet (Sprite Editor)
- [ ] Tạo Player prefab (Self + Remote) với 4 layer SpriteRenderer
- [ ] Gán reference cho mọi script trong Inspector
- [ ] Dựng 2 scene + thêm vào Build Settings
- [ ] Tạo các prefab UI con (message, slot, friend item...)
- [ ] Vẽ bản đồ (Tilemap)
- [ ] Test luồng login → tạo nhân vật → vào game
