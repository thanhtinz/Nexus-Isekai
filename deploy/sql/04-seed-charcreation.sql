-- ============================================================
-- SEED: Character Creation từ asset Seliel + Mana Seed
-- Chạy SAU khi đã import asset vào bảng `assets`.
-- Script này tạo các lựa chọn mẫu; asset_id để NULL nếu chưa
-- map — admin gắn asset sau qua giao diện, hoặc map bằng tên file.
-- ============================================================

-- ── RACES / GENDER ─────────────────────────────────────────────
-- Con Người (humn) — nam & nữ
INSERT INTO char_races (code, race, race_name_vn, gender, faction_id, sort_order) VALUES
  ('humn_male',   'humn', 'Con Người', 'male',   0, 1),
  ('humn_female', 'humn', 'Con Người', 'female', 0, 2),
  ('demn_male',   'demn', 'Ma Tộc',    'male',   4, 3),
  ('demn_female', 'demn', 'Ma Tộc',    'female', 4, 4),
  ('gbln_male',   'gbln', 'Yêu Tinh',  'male',   3, 5),
  ('gbln_female', 'gbln', 'Yêu Tinh',  'female', 3, 6)
ON CONFLICT (code) DO NOTHING;

-- ── SKIN / BODY (0bas) — tông da Con Người v00→v10 ──────────────
INSERT INTO char_options (slot, code, name_vn, race_filter, is_default, sort_order) VALUES
  ('skin', 'humn_v00', 'Da rất sáng',   'humn', TRUE,  1),
  ('skin', 'humn_v01', 'Da sáng',       'humn', FALSE, 2),
  ('skin', 'humn_v02', 'Da sáng hồng',  'humn', FALSE, 3),
  ('skin', 'humn_v03', 'Da tự nhiên',   'humn', FALSE, 4),
  ('skin', 'humn_v04', 'Da rám nhẹ',    'humn', FALSE, 5),
  ('skin', 'humn_v05', 'Da rám',        'humn', FALSE, 6),
  ('skin', 'humn_v06', 'Da ngăm',       'humn', FALSE, 7),
  ('skin', 'humn_v07', 'Da nâu',        'humn', FALSE, 8),
  ('skin', 'humn_v08', 'Da nâu đậm',    'humn', FALSE, 9),
  ('skin', 'humn_v09', 'Da đen',        'humn', FALSE, 10),
  ('skin', 'humn_v10', 'Da đen đậm',    'humn', FALSE, 11),
  ('skin', 'demn_v01', 'Da quỷ đỏ',     'demn', TRUE,  12),
  ('skin', 'demn_v02', 'Da quỷ tím',    'demn', FALSE, 13),
  ('skin', 'gbln_v01', 'Da yêu tinh',   'gbln', TRUE,  14)
ON CONFLICT (slot, code) DO NOTHING;

-- ── EYES (3fac) — 11 màu Mana Seed ─────────────────────────────
INSERT INTO char_options (slot, code, name_vn, color_index, hex_preview, is_default, sort_order) VALUES
  ('eyes', 'eye_00', 'Bạc',   0,  '#c0c0c8', FALSE, 1),
  ('eyes', 'eye_01', 'Mako',  1,  '#2a8a8a', FALSE, 2),
  ('eyes', 'eye_02', 'Xanh dương', 2, '#3a78d0', TRUE, 3),
  ('eyes', 'eye_03', 'Xanh lá', 3, '#3a9a4a', FALSE, 4),
  ('eyes', 'eye_04', 'Đỏ',    4,  '#c83a3a', FALSE, 5),
  ('eyes', 'eye_05', 'Nâu',   5,  '#8a5a2a', FALSE, 6),
  ('eyes', 'eye_06', 'Hồng',  6,  '#e07aa8', FALSE, 7),
  ('eyes', 'eye_07', 'Tím',   7,  '#8a4ac8', FALSE, 8),
  ('eyes', 'eye_08', 'Thép',  8,  '#5a7a8a', FALSE, 9),
  ('eyes', 'eye_09', 'Hạt dẻ', 9, '#9a7a4a', FALSE, 10),
  ('eyes', 'eye_10', 'Hổ phách', 10, '#d09a3a', FALSE, 11)
ON CONFLICT (slot, code) DO NOTHING;

-- ── HAIR (4har) — các kiểu tóc cơ bản ──────────────────────────
INSERT INTO char_options (slot, code, name_vn, is_default, sort_order) VALUES
  ('hair', 'bob1', 'Tóc bob',        TRUE,  1),
  ('hair', 'bob2', 'Tóc bob 2',      FALSE, 2),
  ('hair', 'flat', 'Tóc thẳng',      FALSE, 3),
  ('hair', 'fro1', 'Tóc xù',         FALSE, 4),
  ('hair', 'pon1', 'Tóc đuôi ngựa',  FALSE, 5),
  ('hair', 'spk2', 'Tóc dựng',       FALSE, 6),
  ('hair', 'dap1', 'Tóc lệch',       FALSE, 7)
ON CONFLICT (slot, code) DO NOTHING;

-- ── OUTFIT (1out) — áo quần khởi đầu ───────────────────────────
INSERT INTO char_options (slot, code, name_vn, is_default, sort_order) VALUES
  ('outfit', 'undi', 'Đồ lót cơ bản',   TRUE,  1),
  ('outfit', 'pfpn', 'Đồ nông dân',     FALSE, 2)
ON CONFLICT (slot, code) DO NOTHING;

-- ── Tự động map asset_id theo tên file (nếu asset đã import) ────
-- Map skin/body
UPDATE char_options o SET asset_id = a.id
FROM assets a
WHERE o.slot='skin' AND o.asset_id IS NULL
  AND a.file_path LIKE '%0bas_' || o.code || '%';

-- Map eyes (theo eye_vXX trong tên)
UPDATE char_options o SET asset_id = a.id
FROM assets a
WHERE o.slot='eyes' AND o.asset_id IS NULL
  AND a.file_path LIKE '%3fac_eye_v' || lpad(o.color_index::text,2,'0') || '%';

-- Map hair (theo 4har_<code>)
UPDATE char_options o SET asset_id = a.id
FROM assets a
WHERE o.slot='hair' AND o.asset_id IS NULL
  AND a.file_path LIKE '%4har_' || o.code || '_v01%';

-- Map outfit (theo 1out_<code>)
UPDATE char_options o SET asset_id = a.id
FROM assets a
WHERE o.slot='outfit' AND o.asset_id IS NULL
  AND a.file_path LIKE '%1out_' || o.code || '_v01%';

-- Map race base sprite (0bas)
UPDATE char_races r SET base_asset_id = a.id
FROM assets a
WHERE r.base_asset_id IS NULL
  AND a.file_path LIKE '%0bas_' || r.race || '%';

\echo '✅ Seed character creation xong. Asset_id tự map nếu file đã import vào bảng assets.'
\echo '   Nếu chưa map, vào Admin → Tạo nhân vật để gắn sprite thủ công.'
