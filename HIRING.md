# Tài Liệu Tuyển Dụng — Nexus Isekai

> Phần code đã hoàn thiện. Tài liệu này mô tả các vị trí cần thuê để hoàn thành phần content và vận hành.

## Tổng quan
Nexus Isekai là MMORPG đa nền tảng (Unity + mobile + PC + web). Phần backend, protocol, database, và logic game đã hoàn chỉnh. Cần bổ sung **content** (art, audio) và **vận hành** (devops, QA, live-ops).

---

## 1. 2D Game Artist (2-3 người) — ƯU TIÊN CAO

**Thời gian:** 3-4 tháng
**Nhiệm vụ:**
- Vẽ sprite nhân vật cho 8 class (Kiếm Sĩ, Pháp Sư, Xạ Thủ, Slinger, Axeman, Quyền Sư, Cung Thủ, Sát Thủ), mỗi class 2 giới tính
- Animation: idle, walk, run, attack, skill, hit, die (Spine 2D hoặc spritesheet)
- ~475 asset UI/HUD (xem `client/Assets/UI_ASSET_LIST.md` — đã có spec chi tiết: kích thước, tĩnh/động, song ngữ)
- Icon item, skill, buff/debuff
- Background map, tileset

**Yêu cầu:** Thành thạo Spine 2D / Photoshop / Aseprite. Portfolio game art. Hiểu pixel-perfect cho mobile.

**Deliverable:** PNG/Spine theo spec trong `UI_ASSET_LIST.md`. Đặt vào `client/Assets/Sprites/`.

---

## 2. Sound Designer / Composer (1 người)

**Thời gian:** 1-2 tháng
**Nhiệm vụ:**
- Nhạc nền: login, làng, dungeon, boss, PvP (5-8 track loop)
- SFX: attack, skill, hit, level up, gacha pull, UI click, coin/diamond
- Ambient: gió, nước, chợ

**Yêu cầu:** Kinh nghiệm game audio. Xuất OGG/WAV. Hiểu loop seamless.

**Deliverable:** File audio đặt vào `client/Assets/Audio/{BGM,SFX,Ambient,UI}/`. DB config sẵn ở bảng `audio_assets`.

---

## 3. Unity Developer (1-2 người)

**Thời gian:** 2-3 tháng
**Nhiệm vụ:**
- Lắp ráp Unity scenes (Loading, Login, ServerSelect, CharCreate, Game) — script C# đã có sẵn trong `client/Assets/Scripts/`
- Tạo prefab từ sprite artist giao
- Wire UI panels (78 script đã viết sẵn) với artwork
- Tích hợp Spine runtime cho nhân vật
- Build pipeline Android/iOS/PC/WebGL

**Yêu cầu:** Unity 2022 LTS, C#, Spine-Unity, mobile build. Hiểu networking (code client đã có).

---

## 4. Game Designer / Balance (1 người)

**Thời gian:** liên tục
**Nhiệm vụ:**
- Cân bằng số liệu: damage formula, exp curve, drop rate, economy (gold/diamond sink)
- Thiết kế nội dung: quest, dungeon, boss, event
- Tinh chỉnh gacha rate, gói nạp, mission pass
- Phân tích metric (DAU, retention, ARPU) → điều chỉnh

**Yêu cầu:** Kinh nghiệm thiết kế MMORPG/gacha. Tư duy số liệu. Biết SQL để query DB.

---

## 5. DevOps Engineer (1 người)

**Thời gian:** 1 tháng setup + part-time vận hành
**Nhiệm vụ:**
- Setup production: MySQL (read-replica), Redis cluster, server Netty
- Load balancer, auto-scaling, CDN cho asset
- Monitoring: Prometheus + Grafana, alerting
- CI/CD pipeline (đã có `.github/workflows/ci.yml` cơ bản)
- Backup + disaster recovery
- Load test 5000+ CCU

**Yêu cầu:** Linux, Docker/K8s, MySQL, Redis, monitoring stack. Xem `DEPLOYMENT.md`.

---

## 6. QA Tester (1-2 người)

**Thời gian:** liên tục từ closed beta
**Nhiệm vụ:**
- Test gameplay, tìm bug, exploit
- Test thanh toán (SePay), gacha, gift code
- Test đa thiết bị (Android/iOS/PC)
- Regression test mỗi update

---

## Lộ trình tuyển
1. **Tháng 1:** Artist + Sound + Unity dev (làm content)
2. **Tháng 2-3:** + Game designer (balance song song)
3. **Tháng 3:** + DevOps (chuẩn bị infra)
4. **Tháng 4:** + QA (closed beta)
5. **Tháng 5-6:** Soft launch 1 khu vực

## Ngân sách ước tính (tham khảo VN)
| Vị trí | Số người | Tháng | Chi phí/tháng |
|---|---|---|---|
| 2D Artist | 2-3 | 4 | 15-25tr/người |
| Sound Designer | 1 | 2 | 15-20tr |
| Unity Dev | 1-2 | 3 | 20-35tr/người |
| Game Designer | 1 | 6 | 20-30tr |
| DevOps | 1 | 6 (part) | 25-40tr |
| QA | 1-2 | 4 | 10-15tr/người |

**Tổng ước tính:** 600tr - 1.2 tỷ VND cho 6 tháng đến soft launch.
