-- ============================================================
-- Fantasy Realm Online — Dữ liệu mẫu để test
-- Chạy SAU schema.sql: psql -U fro -d fantasyrealm -f 03-seed-sample.sql
-- ============================================================

-- Tài khoản test (mật khẩu: test123 — hash bcrypt)
INSERT INTO players (username, email, password_hash, salt)
VALUES
  ('testuser1', 'test1@fro.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RcWQ.PqJK', 'salt1'),
  ('testuser2', 'test2@fro.vn', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RcWQ.PqJK', 'salt2')
ON CONFLICT (username) DO NOTHING;

-- Vật phẩm mẫu
INSERT INTO items (item_id, name, name_vn, type, rarity, base_price, sell_price, description)
VALUES
  (1001, 'Wooden Sword', 'Kiếm Gỗ', 'weapon', 0, 100, 50, 'Vũ khí khởi đầu'),
  (1002, 'Iron Sword', 'Kiếm Sắt', 'weapon', 1, 500, 250, 'Vũ khí cấp 1'),
  (9001, 'Dragon Scale', 'Vảy Rồng', 'material', 4, 50000, 25000, 'Nguyên liệu huyền thoại'),
  (2001, 'Health Potion', 'Bình Máu', 'consumable', 0, 50, 25, 'Hồi 100 HP'),
  (3001, 'Carrot Seed', 'Hạt Cà Rốt', 'seed', 0, 20, 10, 'Trồng được cà rốt'),
  (4001, 'Common Fish', 'Cá Thường', 'fish', 0, 30, 15, 'Cá phổ biến')
ON CONFLICT (item_id) DO NOTHING;

-- Nghề nghiệp mẫu
INSERT INTO professions (code, name, name_vn, category, max_level, description, skills)
VALUES
  ('FISHER', 'Fisher', 'Ngư Dân', 'civilian', 10, 'Nghề câu cá',
   '[{"level":1,"name":"Câu Cơ Bản","effect":"unlock_common_fish"},{"level":10,"name":"Câu Huyền Thoại","effect":"unlock_legendary_fish"}]'),
  ('CHEF', 'Chef', 'Đầu Bếp', 'civilian', 10, 'Nghề nấu ăn',
   '[{"level":1,"name":"Nấu Cơ Bản","effect":"unlock_basic_recipe"}]'),
  ('FARMER', 'Farmer', 'Nông Dân', 'civilian', 10, 'Nghề trồng trọt', '[]'),
  ('BLACKSMITH', 'Blacksmith', 'Thợ Rèn', 'combat', 15, 'Nghề rèn vũ khí', '[]')
ON CONFLICT (code) DO NOTHING;

-- NPC mẫu
INSERT INTO npcs (template_id, name, name_vn, type, zone_id, default_x, default_y, has_shop, has_dialog)
VALUES
  (1001, 'Merchant Bob', 'Thương Nhân Bob', 'merchant', 1, 150, 120, TRUE, TRUE),
  (1002, 'Quest Giver Anna', 'Anna Giao Nhiệm Vụ', 'quest', 1, 200, 180, FALSE, TRUE),
  (1003, 'Banker Tom', 'Chủ Ngân Hàng Tom', 'banker', 1, 100, 100, FALSE, TRUE)
ON CONFLICT (template_id) DO NOTHING;

-- Mob mẫu
INSERT INTO mobs (template_id, name, name_vn, type, zone_ids, level_min, level_max, hp, atk, def, exp_reward)
VALUES
  (5001, 'Slime', 'Slime Xanh', 'normal', '{1,2}', 1, 3, 50, 5, 2, 20),
  (5002, 'Wolf', 'Sói Hoang', 'normal', '{2,3}', 3, 6, 120, 15, 5, 50),
  (9001, 'Ancient Dragon', 'Rồng Cổ Đại', 'boss', '{5}', 50, 50, 100000, 500, 200, 50000)
ON CONFLICT (template_id) DO NOTHING;

-- Bản đồ mẫu
INSERT INTO maps (zone_id, name, name_vn, type, width, height)
VALUES
  (1, 'Starter Town', 'Thị Trấn Khởi Đầu', 'city', 300, 300),
  (2, 'Green Forest', 'Rừng Xanh', 'overworld', 400, 400),
  (5, 'Dragon Lair', 'Hang Rồng', 'dungeon', 200, 200)
ON CONFLICT (zone_id) DO NOTHING;

\echo '✅ Seed data đã nạp xong.'
