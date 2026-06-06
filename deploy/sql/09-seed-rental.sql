-- Phòng trọ cho thuê (người mới chưa có nhà) — nội thất cố định, có rương mặc định
INSERT INTO rental_rooms (room_code,name,name_vn,zone_id,rent_per_day,default_storage_slots,fixed_layout_json) VALUES
('inn_basic','Basic Inn Room','Phòng Trọ Bình Dân',1,200,10,'{"bed":{"x":50,"y":50},"chest":{"x":80,"y":50},"chair":{"x":60,"y":80}}'),
('inn_cozy','Cozy Inn Room','Phòng Trọ Ấm Cúng',1,500,15,'{"bed":{"x":50,"y":50},"chest":{"x":80,"y":50},"table":{"x":60,"y":80},"candle":{"x":40,"y":40}}'),
('inn_deluxe','Deluxe Inn Room','Phòng Trọ Cao Cấp',1,1200,25,'{"royal_bed":{"x":50,"y":50},"chest":{"x":80,"y":50},"table":{"x":60,"y":80},"chandelier":{"x":50,"y":30},"rug":{"x":55,"y":60}}')
ON CONFLICT (room_code) DO NOTHING;

\echo '✅ Seed 3 phòng trọ. Người mới thuê phòng để có chỗ ở + rương mặc định.'
