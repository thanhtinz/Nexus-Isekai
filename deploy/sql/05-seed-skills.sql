-- ============================================================
-- SEED SKILLS — kỹ năng mẫu (admin có thể thêm/sửa/xóa thoải mái)
-- Chạy: psql -U fro -d fantasyrealm -f deploy/sql/05-seed-skills.sql
-- ============================================================

INSERT INTO skills (skill_code,name,name_vn,description,category,class_code,faction_id,
  effect_type,power,level_req,mana_cost,cooldown_ms,range_px,buff_duration_ms,sort_order) VALUES
-- Chính (mọi class/phe)
('power_strike','Power Strike','Đòn Mạnh','Đòn đánh mạnh đơn mục tiêu','main',NULL,0,'damage',1.5,1,5,3000,48,0,1),
('whirlwind','Whirlwind','Lốc Xoáy','Sát thương vùng quanh người','main',NULL,0,'aoe_damage',1.2,5,15,8000,96,0,2),
-- Phụ
('heal','Heal','Hồi Phục','Hồi máu bản thân','sub',NULL,0,'heal',0.4,3,12,6000,0,0,1),
-- Đặc biệt theo phe
('holy_smite','Holy Smite','Thánh Quang','Sát thương thánh','special',NULL,1,'damage',2.0,4,18,7000,64,0,1),
('divine_shield','Divine Shield','Khiên Thần','Tăng phòng thủ','special',NULL,1,'buff_def',0.5,8,20,15000,0,10000,2),
('nature_arrow','Nature Arrow','Mũi Tên Tự Nhiên','Mũi tên phép','special',NULL,2,'damage',1.8,4,14,5000,120,0,1),
('savage_bite','Savage Bite','Cắn Hoang Dã','Cắn mạnh','special',NULL,3,'damage',2.2,4,12,4500,48,0,1),
('shadow_bolt','Shadow Bolt','Ám Tiễn','Đạn bóng tối','special',NULL,4,'damage',2.1,4,16,5500,100,0,1),
-- Riêng (ví dụ skill ultimate phe Ma Tộc)
('life_drain','Life Drain','Hút Sinh Mệnh','Hút máu địch','unique',NULL,4,'drain',1.3,8,22,10000,80,0,1)
ON CONFLICT (skill_code) DO NOTHING;

\echo '✅ Seed skills xong. Vào Admin → Kỹ năng để thêm/sửa.'
