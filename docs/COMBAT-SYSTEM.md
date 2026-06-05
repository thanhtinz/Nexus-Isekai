# ⚔️ Hệ thống Combat (PvE)

Đánh quái, sát thương, lên cấp, hồi sinh. Nối server ↔ client qua packet 0xC0-0xCB.

## Luồng

```
Client click quái → C_ATTACK_MOB(mobId)
   ↓ server CombatService
   - kiểm tra sống/cooldown/tầm đánh
   - tính sát thương (atk - def, +crit 15%)
   - mob.takeDamage → S_MOB_DAMAGE (broadcast zone)
   - nếu chết: S_MOB_DEATH + thưởng exp/gold/loot + checkLevelUp
   - nếu sống: mob phản đòn → S_PLAYER_DAMAGE
   ↓ nếu người chơi hết máu
   - S_PLAYER_DEATH → client hiện màn hình chết
   - C_PLAYER_RESPAWN → S_PLAYER_RESPAWN (hồi sinh tại làng)
```

## Stats nhân vật (PlayerSession)

| Chỉ số | Công thức |
|--------|-----------|
| Sát thương | 8 + level × 2 |
| Phòng thủ | 2 + level |
| Máu tối đa | 100 + (level-1) × 20 |
| EXP lên cấp | level × 100 |

Lên cấp → tăng máu tối đa + hồi đầy máu.

## Mob (server-side)

- `MobManager` đọc template từ bảng `mobs` (admin DB), spawn 3 con/template/zone
- Respawn sau 15 giây kể từ khi chết
- Loot: thường 30%, elite 50%, boss 90% rơi vật phẩm
- Mob phản đòn người tấn công

## Packet (0xC0-0xCB)

| Packet | ID | Nội dung |
|--------|----|----|
| C_ATTACK_MOB | 0xC0 | mobId |
| S_MOB_DAMAGE | 0xC1 | mobId, dmg, hpRemain, crit, attackerId |
| S_MOB_DEATH | 0xC2 | mobId, killerId |
| S_MOB_SPAWN | 0xC3 | mobId, templateId, name, level, maxHp, hp, x, y |
| S_MOB_LIST | 0xC4 | count + [mob...] (gửi khi vào zone) |
| S_PLAYER_DAMAGE | 0xC5 | playerId, dmg, hpRemain, mobId |
| S_PLAYER_DEATH | 0xC6 | playerId, message |
| S_PLAYER_RESPAWN | 0xC7 | playerId, hp, x, y |
| S_PLAYER_STATS | 0xC8 | level, hp, maxHp, exp, expNext, gold |
| C_PLAYER_RESPAWN | 0xC9 | (không payload) |
| C_USE_SKILL | 0xCA | (dành cho kỹ năng — mở rộng sau) |
| S_LEVEL_UP | 0xCB | level, maxHp |

## Client (Unity)

- `MobManager.cs` — spawn/cập nhật/xóa quái từ packet
- `MobView.cs` — thanh máu, popup sát thương, click để đánh, hiệu ứng trúng đòn/chết
- `CombatSystem.cs` — HUD máu/exp, màn hình chết + nút hồi sinh, hiệu ứng lên cấp

Setup Editor: mob prefab cần SpriteRenderer + Collider2D + MobView (kéo nameLabel, hpBar...).
Xem docs/UNITY-SCENE-SETUP.md.

## Còn mở rộng được

- Kỹ năng (C_USE_SKILL đã có packet, chưa làm logic skill cụ thể)
- Loot table chi tiết theo mob (hiện loot item mẫu)
- PvP (đánh người chơi khác)
