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
| voice | npc/intro | Audio/Voice/ | lồng tiếng |

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

> Liên quan: CONTENT_REGISTRY.md (phân loại content) · ITEM_ID_CONVENTION.md · FARM_ASSETS.md
