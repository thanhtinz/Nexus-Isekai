# ASSET HUB — Quản lý toàn bộ asset theo danh mục

> Mọi asset (sprite, nhạc, âm thanh, background, effect, UI, font, video, config, Spine)
> phân loại theo 2 trục: **asset_type** (loại media) × **category** (nội dung).
> Trong mỗi danh mục, file đánh số lại từ 1. Admin lọc theo type/category.
>
> Tài liệu chia 3 phần:
> - **Phần I — Quy ước chung**: cách đặt tên, OTA, bảng quản lý.
> - **Phần II — Danh mục tham chiếu**: folder/category cho từng loại media.
> - **Phần III — Asset cần làm**: checklist chi tiết theo TỪNG tính năng (kèm cột map tới bảng/admin).

---

# PHẦN I — QUY ƯỚC CHUNG

## I.1. Nguyên tắc đặt asset
1. Mỗi asset BẮT BUỘC có `asset_type` + `category` đúng → không lẫn loại (nhạc boss khác nhạc làng, sprite farm khác vũ khí).
2. Trong 1 danh mục, file đánh số từ 1 (vd `weapon_1.png`, `weapon_2.png`).
3. **KEY khai trong admin = tên file/asset, KHÔNG khoảng trắng.** Team bỏ asset đúng folder + đặt tên khớp key → client OTA tự nạp, không cần sửa code.
4. Spine/VFX/SFX gắn qua admin bằng `*_key` (xem Phần III mục O) — chỉ cần đặt tên file khớp key.

## I.2. OTA (cập nhật asset không cần build lại app)
- Client so `version`/`hash` từng category → chỉ tải bổ sung phần thay đổi.
- Đổi skin theo sự kiện: tạo category `ui_<feature>` riêng, admin trỏ version mới.

## I.3. Bảng quản lý (DB)
| Bảng | Vai trò |
|---|---|
| `client_assets` | sprite/bg/effect/ui/font/video/config — cột asset_type × category + version + hash (OTA) |
| `audio_assets` | nhạc/âm thanh/voice — asset_type × category + volume + loop |
| `asset_bundles` | gói phát hành theo đợt (bundle_category: sprite/audio/ui/map/effect/font/mixed) |
| `asset_packs` | gói upload thô (admin giải nén) |

> Liên quan: CONTENT_REGISTRY.md · ITEM_ID_CONVENTION.md · FARM_ASSETS.md · CLASSES.md · NPC_VOICE_SCRIPT.md

---

# PHẦN II — DANH MỤC THAM CHIẾU (folder × category)

## II.1. Sprite / Ảnh — client_assets (asset_type sprite/icon/atlas)
| Folder | category | Tên file | Nguồn (bảng) |
|---|---|---|---|
| Sprites/Weapon/ | weapon | weapon_<n>.png | items 1xxx |
| Sprites/Armor/ | armor | armor_<n>.png | items 2xxx |
| Sprites/Accessory/ | accessory | accessory_<n>.png | items 3xxx |
| Sprites/Consumable/ | consumable | consumable_<n>.png | items 4xxx |
| Sprites/Material/ | material | material_<n>.png | items 5xxx |
| Sprites/Gem/ | gem | gem_<n>.png | items 7xxx |
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
| Sprites/Furniture/ | furniture | furniture_<n>.png | furniture_catalog |

## II.2. Background / Map — asset_type bg/tileset
| Folder | category | Tên file |
|---|---|---|
| Maps/ | map_bg | map_<id>.png (= maps.file_name) |
| Maps/Tiles/ | map_tile | tile_<n>.png |
| Maps/Sky/ | sky | sky_<n>.png |
| Maps/Parallax/ | parallax | parallax_<n>.png |

## II.3. Nhạc / Âm thanh — audio_assets (asset_type × category)
| asset_type | category | Folder | Mô tả |
|---|---|---|---|
| bgm | scene_login/town/field/dungeon/boss/event | Audio/BGM/ | nhạc nền theo cảnh |
| sfx | combat | Audio/SFX/ | đánh, chí mạng, chết |
| sfx | skill | Audio/SFX/ | hiệu ứng skill |
| sfx | item | Audio/SFX/ | nhặt đồ, cường hoá, gacha |
| sfx | farm | Audio/SFX/ | trồng, thu hoạch |
| sfx | levelup | Audio/SFX/ | lên cấp |
| ui | ui_click | Audio/UI/ | click, mở/đóng panel, nhận thưởng |
| ambient | nature/cave/rain | Audio/Ambient/ | tiếng môi trường |
| voice | intro | Audio/Voice/Class/ | thoại giới thiệu class (class_<id>.ogg) |
| voice | npc | Audio/Voice/Npc/ | câu bark ngắn NPC |

## II.4. UI / HUD / Hệ thống — asset_type effect/particle/font/video/config
| Folder | category | Mô tả |
|---|---|---|
| Sprites/Ui/ | ui | khung, nền panel |
| Sprites/Hud/ | hud | thanh máu/mp, joystick (xem Phần III.J) |
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

---

# PHẦN III — ASSET CẦN LÀM (checklist theo tính năng)

> Mỗi mục là 1 đầu việc cho team art/audio. Cột **Map tới** cho biết key/bảng admin
> để đặt tên cho khớp.

## A. Nhân vật & Class (8 class × 2 giới tính)
| Asset | Folder | Tên/Key | Mô tả | Map tới |
|---|---|---|---|---|
| Spine nhân vật | Sprites/Spine/Char/ | char_<class>_<gender> | skeleton di chuyển/đánh/chết, 8 class × 2 giới | class_templates |
| Avatar class | Sprites/Icons/Class/ | class_<id>.png | ảnh đại diện chọn class (1-8) | class_templates.id |
| Thoại giới thiệu | Audio/Voice/Class/ | class_<id>.ogg | lời thoại lúc tạo nhân vật | voice_lines (class_intro) |

## B. Trang bị / Vật phẩm / Gem
| Asset | Folder | Tên/Key | Map tới |
|---|---|---|---|
| Icon vũ khí | Sprites/Weapon/ | weapon_<n>.png | items 1xxx |
| Icon giáp | Sprites/Armor/ | armor_<n>.png | items 2xxx |
| Icon phụ kiện | Sprites/Accessory/ | accessory_<n>.png | items 3xxx |
| Icon tiêu hao | Sprites/Consumable/ | consumable_<n>.png | items 4xxx |
| Icon nguyên liệu | Sprites/Material/ | material_<n>.png | items 5xxx |
| Icon ngọc | Sprites/Gem/ | gem_<n>.png | items 7xxx |
> Ưu tiên làm icon cho item đang seed (kiểm bảng item_template / items.icon_id).

## C. Kỹ năng — 160 skill (8 class × 20)
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Icon skill | Sprites/Skill/ | skill_<id>.png | icon ô kỹ năng (id = class*100+1..20) | skill_templates.icon_id |
| VFX lúc tung | Effects/Skill/ | <vfx_key> (prefab) | hiệu ứng khi cast | skill_templates.vfx_key |
| VFX lúc trúng | Effects/Skill/ | <vfx_hit_key> (prefab) | hiệu ứng khi trúng mục tiêu | skill_templates.vfx_hit_key |
| SFX skill | Audio/SFX/ | <sfx_key> | âm thanh tung chiêu | skill_templates.sfx_key + audio_assets |

## D. Quái / Boss
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Spine quái | Sprites/Spine/Monster/ | <spine_key> | skeleton đi/đánh/chết | monsters.spine_key |
| SFX đánh | Audio/SFX/ | <sfx_attack> | tiếng quái tấn công | monsters.sfx_attack |
| SFX bị đánh | Audio/SFX/ | <sfx_hurt> | tiếng quái trúng đòn | monsters.sfx_hurt |
| SFX chết | Audio/SFX/ | <sfx_death> | tiếng quái chết | monsters.sfx_death |
| SFX gầm (boss) | Audio/SFX/ | boss_<id>_roar | boss gầm lúc xuất hiện | sound_events |

## E. NPC
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Spine NPC | Sprites/Spine/Npc/ | <spine_key> | skeleton NPC đứng/idle | npcs.spine_key |
| SFX tương tác | Audio/SFX/ | <sfx_key> | tiếng khi nói chuyện | npcs.sfx_key |
| Câu bark | Audio/Voice/Npc/ | npc_<loại>_<n>.ogg | câu ngắn theo loại NPC (xem NPC_VOICE_SCRIPT.md) | voice_lines (npc_bark) |

## F. Bản đồ
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Nền map | Maps/ | map_<id>.png | ảnh nền màn chơi | maps.file_name |
| Nhạc riêng map | Audio/BGM/ | <bg_music> | nhạc nền theo map | maps.bg_music |
| Tile/Sky/Parallax | Maps/Tiles,Sky,Parallax/ | (xem II.2) | lớp nền phụ | — |

## G. Âm thanh sự kiện (action / ui) — bảng sound_events
| Nhóm | event_key mẫu | Folder | Map tới |
|---|---|---|---|
| Hành động | player_attack, skill_cast, levelup, pickup, die | Audio/SFX/ | sound_events.audio_key |
| Giao diện | btn_click, panel_open, reward, purchase | Audio/UI/ | sound_events.audio_key |
> Client tải bảng sound_events 1 lần khi vào game, phát theo event_key. Admin sửa key/âm lượng/bật-tắt.

## H. Lời thoại (Voice) — bảng voice_lines
| Loại | Folder | Tên file | Map tới |
|---|---|---|---|
| Giới thiệu class | Audio/Voice/Class/ | class_<id>.ogg | voice_lines (context=class_intro, ref_id=class) |
| Bark NPC | Audio/Voice/Npc/ | npc_<loại>_<n>.ogg | voice_lines (context=npc_bark, ref_id=npc_id) |
> Kịch bản + ghi chú giọng điệu: **NPC_VOICE_SCRIPT.md**. Định dạng .ogg mono 44.1kHz, 1-3 giây/câu.

## I. UI tính năng (mỗi tính năng 1 folder ui_<feature>)
| Folder | category | Cần làm |
|---|---|---|
| Sprites/Ui/Activity/ | ui_activity | Hub Hoạt Động: nền, cột trái danh sách, khung phải chi tiết, thanh tiến độ |
| Sprites/Ui/Welfare/ | ui_welfare | Hub Phúc Lợi: nền, cột trái, khung phải, badge trạng thái (xem K) |
| Sprites/Ui/Vip/ | ui_vip | VIP: nền, huy hiệu 9 mốc, khung đặc quyền |
| Sprites/Ui/Market/ | ui_market | Chợ: nền, tab vàng/kim cương, nút bán/mua |
| Sprites/Ui/GuildWar/ | ui_guildwar | Guild chiến: nền, cờ 2 bang, thanh điểm |
| Sprites/Ui/WorldBoss/ | ui_worldboss | World boss: nền, thanh HP lớn, BXH sát thương |
| Sprites/Ui/OuterRealm/ | ui_outer | Ngoại vực: nền tháp, nút tầng theo level |
| Sprites/Ui/Afk/ | ui_afk | AFK: nền, icon thẻ treo theo giờ |
| Sprites/Ui/Pk/ | ui_pk | Chọn chế độ PK + khung truy nã/nhà tù |

## J. HUD màn chơi — Sprites/Hud/ (luôn hiển thị khi chơi)
| File | Mô tả |
|---|---|
| hud_hp_bar.png / hud_mp_bar.png | thanh máu / mana người chơi |
| hud_exp_bar.png | thanh kinh nghiệm |
| hud_avatar_frame.png | khung avatar + level góc trên trái |
| hud_joystick_base.png / hud_joystick_knob.png | cần điều khiển di chuyển |
| hud_btn_attack.png | nút tấn công chính |
| hud_skill_slot.png (×5) | ô kỹ năng (5 active, trang bị) + lớp phủ hồi chiêu |
| hud_quickslot.png (×4) | ô vật phẩm nhanh (bình máu/mp) |
| hud_buff_icon_frame.png | khung icon buff/debuff |
| hud_minimap_frame.png | khung bản đồ nhỏ + chấm quái/NPC/cổng |
| hud_target_bar.png | thanh HP mục tiêu |
| hud_currency_gold.png / hud_currency_diamond.png | hiển thị vàng / kim cương góc trên phải |
| hud_menu_btn.png | nút mở menu chính |
| hud_chat_box.png | khung chat + tab kênh |
| hud_quest_tracker.png | bảng theo dõi nhiệm vụ |
| hud_event_entry.png | nút nổi vào Hoạt Động/sự kiện đang mở |
| hud_pk_mode_badge.png | huy hiệu chế độ PK + cờ truy nã |
| hud_vip_badge.png | huy hiệu VIP cạnh avatar |
> Lưu ý: ô kỹ năng còn **5** (trang bị 5 skill), không phải 7.

## K. Phúc Lợi (Welfare)
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Icon từng loại | Sprites/Icons/Welfare/ | welfare_<type>.png | 14 loại: checkin, online, first_topup, daily_topup, cumulative_topup, monthly_card, growth_fund, battle_pass, giftcode, level_gift, power_gift, achievement_gift, newbie_gift, refund | welfare.welfare_type |
| Nền hub | Sprites/Ui/Welfare/ | ui_welfare | cột trái danh sách + khung phải chi tiết + tiến độ + thời gian | — |
| Badge trạng thái | Sprites/Ui/Welfare/ | welfare_state_<n> | chưa đạt / có thể nhận (chấm đỏ) / đã nhận / hết hạn | — |

## L. Kho Báu (Treasure)
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Icon rương | Sprites/Icons/Treasure/ | treasure_<n>.png | hình rương theo từng loại | treasure_chests.icon_id |
| Hiệu ứng mở | Effects/Treasure/ | open_<rarity> (prefab) | hiệu ứng mở rương theo độ hiếm | — |
| SFX mở rương | Audio/SFX/ | treasure_open | tiếng mở/đào | sound_events |
| Icon chìa/vé | Sprites/Material/ | material_<id>.png | vật phẩm chi phí (vd Chìa Vàng) | treasure_chests.cost_item_id |

## M. Vòng Quay May Mắn (Lucky Wheel)
| Asset | Folder | Key | Mô tả | Map tới |
|---|---|---|---|---|
| Nền vòng quay | Sprites/Ui/Wheel/ | wheel_base | bánh xe chia ô | — |
| Kim chỉ | Sprites/Ui/Wheel/ | wheel_pointer | kim quay dừng tại ô trúng | — |
| Icon từng ô | Sprites/Icons/Wheel/ | (theo segments_json.label) | phần thưởng mỗi ô | lucky_wheels.segments_json |
| SFX quay/trúng | Audio/SFX/ | wheel_spin, wheel_win | tiếng quay + trúng thưởng | sound_events |

## N. Minigame (CHỈ CƯỢC BẰNG VÀNG — bắt buộc có cảnh báo cờ bạc)
| Game | Folder | Cần làm |
|---|---|---|
| Bầu Cua | Sprites/Minigame/baucua/ | 6 linh vật (bau, cua, ca, tom, ga, nai) + xúc xắc + nền |
| Tiến Lên | Sprites/Minigame/cards/ | bộ 52 lá (id = rank×4+chất, 0 = 3 bích … 51 = 2 cơ) + lưng bài |
| Đua Thú | Sprites/Minigame/duathu/ | 6 lane + sprite thú chạy |
| Ô Ăn Quan | Sprites/Minigame/oanquan/ | bàn 12 ô + quân + quan |
| Đá Gà | Sprites/Minigame/daga/ | 2 gà + animation chọi |
| Đố Vui | — | chỉ UI text (không cần sprite) |
| Banner cảnh báo | Sprites/Ui/Minigame/ | banner "Chơi có trách nhiệm, chỉ dùng vàng trong game" hiện khi mở sảnh |
> Toàn bộ minigame cá cược **chỉ dùng VÀNG** (không kim cương). Server đã ép; UI tạo phòng không hiển thị lựa chọn kim cương.

## O. Cơ chế Spine / VFX / SFX gắn qua admin (tham chiếu nhanh)
| Đối tượng | Cột admin | Đặt asset vào | Client xử lý |
|---|---|---|---|
| Quái/NPC | spine_key | Sprites/Spine/ | gắn SpineMarker, lớp render Spine của team |
| Quái | sfx_attack/hurt/death | audio_assets | phát khi đánh/trúng/chết |
| NPC | sfx_key | audio_assets | phát khi tương tác |
| Skill | vfx_key / vfx_hit_key | Effects/Skill/ (prefab) | instantiate lúc cast/trúng |
| Skill | sfx_key | audio_assets | phát lúc tung |
| Map | bg_music | audio_assets (bgm) | phát khi đổi map |

> Quy trình chung: team đặt file vào đúng folder, đặt tên khớp KEY, khai KEY trong admin (panel tương ứng), client OTA tự nạp. Không cần sửa code.
