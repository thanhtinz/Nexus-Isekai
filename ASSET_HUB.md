# ASSET HUB — quản lý toàn bộ asset theo DANH MỤC

> Mọi asset (sprite, nhạc, âm thanh, background, effect, UI, font, video, config)
> đều phân loại 2 trục: asset_type (loại media) x category (nội dung).
> Trong mỗi danh mục, file đánh số lại từ 1. Admin lọc theo type/category.

## 1. SPRITE / ẢNH — bảng client_assets (asset_type='sprite'/'icon'/'atlas')
| Folder | category | Tên file | Nguồn |
|---|---|---|---|
| Sprites/Weapon/ | weapon | weapon_<n>.png | items 1xxx |
| Sprites/Armor/ | armor | armor_<n>.png | items 2xxx |
| Sprites/Accessory/ | accessory | accessory_<n>.png | items 3xxx |
| Sprites/Consumable/ | consumable | consumable_<n>.png | items 4xxx |
| Sprites/Material/ | material | material_<n>.png | items 5xxx |
| Sprites/Farm/Seed/ | farm_seed | seed_<n>.png | items 6000-99 |
| Sprites/Farm/Crop/ | farm_crop | crop_<n>.png (+_s1..) | items 6100-99 |
| Sprites/Farm/Feed/ | farm_feed | feed_<n>.png | items 6200-99 |
| Sprites/Farm/Produce/ | farm_produce | produce_<n>.png | items 6300-99 |
| Sprites/Farm/Tool/ | farm_tool | tool_<n>.png | items 6400-99 |
| Sprites/Farm/Animal/ | farm_animal | animal_<n>.png | farm_animals |
| Sprites/Pet/ | pet | pet_<n>.png | pet_templates |
| Sprites/Mount/ | mount | mount_<n>.png | mount_templates |
| Sprites/Monster/ | monster | monster_<n>.png | monsters |
| Sprites/Npc/ | npc | npc_<n>.png | npcs |
| Sprites/Skill/ | skill | skill_<n>.png | skill_templates |
| Sprites/Cosmetic/ | cosmetic | cosmetic_<n>.png | cosmetic_templates |
| Sprites/Title/ | title | title_<n>.png | titles |
| Sprites/Gem/ | gem | gem_<n>.png | items 7xxx |
| Sprites/Furniture/ | furniture | furniture_<n>.png | furniture_catalog |

## 2. BACKGROUND / MAP — asset_type='bg'/'tileset'
| Folder | category | Tên file |
|---|---|---|
| Maps/ | map_bg | map_<id>.png (file_name trong bảng maps) |
| Maps/Tiles/ | map_tile | tile_<n>.png |
| Maps/Sky/ | sky | sky_<n>.png |
| Maps/Parallax/ | parallax | parallax_<n>.png |

## 3. NHẠC / ÂM THANH — bảng audio_assets (asset_type x category)
| asset_type | category | Folder | Mô tả |
|---|---|---|---|
| bgm | scene_login | Audio/BGM/ | nhạc đăng nhập |
| bgm | scene_town | Audio/BGM/ | nhạc làng/thành |
| bgm | scene_field | Audio/BGM/ | nhạc đồng/bản đồ |
| bgm | scene_dungeon | Audio/BGM/ | nhạc hầm ngục |
| bgm | scene_boss | Audio/BGM/ | nhạc boss/PvP |
| bgm | event | Audio/BGM/ | nhạc sự kiện |
| sfx | combat | Audio/SFX/ | đánh, chí mạng, chết |
| sfx | skill | Audio/SFX/ | hiệu ứng skill |
| sfx | item | Audio/SFX/ | nhặt đồ, cường hoá, gacha |
| sfx | farm | Audio/SFX/ | trồng, thu hoạch |
| sfx | levelup | Audio/SFX/ | lên cấp |
| ui | ui_click | Audio/UI/ | click, mở/đóng panel |
| ambient | nature/cave/rain | Audio/Ambient/ | tiếng môi trường |
| voice | intro | Audio/Voice/Class/ | lời thoại giới thiệu class lúc tạo nhân vật (class_<id>.ogg) |
| voice | npc | Audio/Voice/Npc/ | câu bark ngắn của NPC khi tương tác (không phải full dialog) |

## 4. HỆ THỐNG / UI — asset_type='effect'/'particle'/'font'/'video'/'config'
| Folder | category | Mô tả |
|---|---|---|
| Sprites/Ui/ | ui | khung, nền panel |
| Sprites/Hud/ | hud | thanh máu/mp, joystick |
| Sprites/Icons/ | icon | vàng, kim cương, icon hệ thống |
| Sprites/Button/ | button | nút bấm |
| Sprites/Frame/ | frame | khung avatar/độ hiếm |
| Effects/ | effect | hiệu ứng chiêu, buff |
| Effects/Particle/ | particle | hạt (lửa, băng, hào quang) |
| Fonts/ | font | font chữ |
| Videos/ | video | intro MP4, cutscene |
| Loading/ | loading | màn hình chờ |
| Config/ | config | json cấu hình |
| Localization/ | localization | file ngôn ngữ |

## 4b. UI PANEL TÍNH NĂNG — mỗi tính năng 1 folder, nền panel + icon + nút
| Folder | category | Mô tả |
|---|---|---|
| Sprites/Ui/Afk/ | ui_afk | nền panel AFK, icon thẻ treo theo giờ |
| Sprites/Ui/Market/ | ui_market | chợ người chơi: nền, tab vàng/kim cương, nút bán/mua |
| Sprites/Ui/GuildWar/ | ui_guildwar | guild chiến: nền, cờ 2 bang, thanh điểm |
| Sprites/Ui/WorldBoss/ | ui_worldboss | world boss: nền, thanh HP lớn, khung BXH sát thương |
| Sprites/Ui/OuterRealm/ | ui_outer | ngoại vực: nền tháp, nút tầng theo level |
| Sprites/Ui/Vip/ | ui_vip | VIP: nền, huy hiệu 9 mốc, khung đặc quyền |
| Sprites/Ui/Activity/ | ui_activity | Hoạt Động: nền hub, cột trái danh sách, khung phải chi tiết, thanh tiến độ |
| Sprites/Ui/Pk/ | ui_pk | chọn chế độ PK (hoà bình/guild/phe/server/cuồng chiến), khung truy nã/nhà tù |

## 4c. ICON LOẠI HOẠT ĐỘNG — Sprites/Icons/Activity/, 1 icon mỗi type
| Folder | category | Tên file | Nguồn |
|---|---|---|---|
| Sprites/Icons/Activity/ | icon_activity | activity_<type_key>.png | bảng activity_types (login,online,kill_monster,fishing,spend_diamond,ranking,x2_exp,gold_boost,exclusive_drop,...) |

> icon_id trong bảng activities trỏ tới icon theo type (hoặc icon riêng nếu admin đặt).
> 43 type trong catalog → tối thiểu 43 icon, đặt tên theo type_key cho khớp.

## 4d. HUD MÀN CHƠI — Sprites/Hud/, category 'hud' (phần tử luôn hiển thị khi chơi)
| File | Mô tả |
|---|---|
| hud_hp_bar.png / hud_mp_bar.png | thanh máu / mana người chơi |
| hud_exp_bar.png | thanh kinh nghiệm |
| hud_avatar_frame.png | khung avatar + level góc trên trái |
| hud_joystick_base.png / hud_joystick_knob.png | cần điều khiển di chuyển |
| hud_btn_attack.png | nút tấn công chính |
| hud_skill_slot.png (x7) | ô kỹ năng (7 active) + lớp phủ hồi chiêu |
| hud_quickslot.png (x4) | ô vật phẩm nhanh (bình máu/mp) |
| hud_buff_icon_frame.png | khung icon buff/debuff đang hiệu lực |
| hud_minimap_frame.png | khung bản đồ nhỏ + chấm quái/NPC/cổng |
| hud_target_bar.png | thanh HP mục tiêu (quái/boss/người chơi) |
| hud_currency_gold.png / hud_currency_diamond.png | hiển thị vàng / kim cương góc trên phải |
| hud_menu_btn.png | nút mở menu chính (túi, kỹ năng, nhiệm vụ...) |
| hud_chat_box.png | khung chat + tab kênh |
| hud_quest_tracker.png | bảng theo dõi nhiệm vụ góc phải |
| hud_event_entry.png | nút nổi vào Hoạt Động/sự kiện đang mở |
| hud_pk_mode_badge.png | huy hiệu chế độ PK hiện tại + cờ truy nã |
| hud_vip_badge.png | huy hiệu VIP cạnh avatar |

## 5. Bảng quản lý
- client_assets: sprite/bg/effect/ui/font/video/config — cột asset_type x category + version + hash (OTA).
- audio_assets: nhạc/âm thanh — asset_type x category + volume + loop.
- asset_bundles: gói asset (bundle_category: sprite/audio/ui/map/effect/font/mixed) để phát hành theo đợt.
- asset_packs: gói upload thô (admin giải nén).

## 6. Nguyên tắc
1. Mỗi asset BẮT BUỘC có asset_type + category đúng → không lẫn loại (nhạc boss không lẫn nhạc làng, sprite farm không lẫn vũ khí).
2. Trong 1 danh mục, file đánh số từ 1.
3. Admin hub lọc 2 tầng: chọn asset_type rồi category.
4. OTA: client so version/hash, tải bổ sung theo category cần dùng.
5. UI tính năng mới: mỗi tính năng 1 category ui_<feature> (xem 4b) để tải/đổi skin riêng theo sự kiện; icon Hoạt Động đặt tên theo type_key (xem 4c) để map tự động với bảng activity_types.
6. Spine + VFX + âm thanh gắn qua admin: monster/npc có `spine_key` (skeleton Spine) + `sfx_*`; skill có `vfx_key`/`vfx_hit_key` (Effects/) + `sfx_key`. Đặt Spine vào Sprites/Spine/, hiệu ứng skill vào Effects/Skill/, khai key trong admin.

> Liên quan: CONTENT_REGISTRY.md (phân loại content) · ITEM_ID_CONVENTION.md · FARM_ASSETS.md

## 6. ASSET CẦN LÀM CHO TÍNH NĂNG MỚI (cập nhật)

### 6a. Phúc Lợi (Welfare)
- Icon panel: `ui_welfare` (Sprites/UI/welfare/). Icon từng loại theo welfare_type: `welfare_<type>` (checkin, online, first_topup, daily_topup, cumulative_topup, monthly_card, growth_fund, battle_pass, giftcode, level_gift, power_gift, achievement_gift, newbie_gift, refund).
- Badge trạng thái: chưa đạt / có thể nhận (chấm đỏ) / đã nhận / hết hạn.

### 6b. Kho Báu (Treasure)
- Icon rương theo treasure_chests.icon_id (Sprites/Icons/Treasure/). Hiệu ứng mở rương: Effects/Treasure/open_<rarity>.
- Âm thanh: sound_events `treasure_open` (Audio/SFX/).

### 6c. Vòng Quay May Mắn (Lucky Wheel)
- Sprite vòng quay nền + kim: Sprites/UI/wheel/wheel_base, wheel_pointer. Icon từng ô theo segments_json.label.
- Âm thanh quay/trúng: sound_events `wheel_spin`, `wheel_win`.

### 6d. Minigame (CHỈ CƯỢC BẰNG VÀNG — có cảnh báo cờ bạc)
- Bầu Cua: Sprites/Minigame/baucua/ (6 linh vật: bau,cua,ca,tom,ga,nai) + xúc xắc.
- Tiến Lên: bộ bài 52 lá Sprites/Minigame/cards/ (rank*4+chat, 0=3 bích ... 51=2 cơ).
- Đua Thú: 6 lane thú Sprites/Minigame/duathu/.
- Ô Ăn Quan: bàn 12 ô + quân Sprites/Minigame/oanquan/.
- Đá Gà: 2 gà + animation Sprites/Minigame/daga/.
- Đố Vui: chỉ UI text.
- Banner cảnh báo cờ bạc (responsible gaming) hiển thị khi mở sảnh minigame.

### 6e. Spine/VFX/SFX (gắn admin)
- monster/npc `spine_key` → Sprites/Spine/<key> (.skel/.atlas/.png) + SpineMarker render.
- skill `vfx_key`/`vfx_hit_key` → Effects/Skill/<key> (prefab). `sfx_key` → audio_assets.
- Nhạc map: maps.bg_music → audio_assets (bgm). Lời thoại: Audio/Voice/Class|Npc/.

> Nguyên tắc: KEY trong admin = tên file/asset (không khoảng trắng). Team bỏ asset đúng folder + đặt tên khớp key là client OTA tự nạp.
