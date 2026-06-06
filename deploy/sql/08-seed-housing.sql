-- Loại nhà mẫu
INSERT INTO house_types (type_code,name,name_vn,tier,purchase_price,storage_slots,max_furniture,sort_order) VALUES
('cottage','Cottage','Nhà Tranh','basic',50000,20,20,1),
('townhouse','Townhouse','Nhà Phố','comfort',150000,40,40,2),
('manor','Manor','Trang Viên','luxury',500000,80,80,3),
('estate','Grand Estate','Dinh Thự','estate',2000000,150,150,4),
('guild_hall','Guild Hall','Hội Quán','estate',5000000,300,200,5)
ON CONFLICT (type_code) DO NOTHING;

-- Nội thất mẫu
INSERT INTO furniture_catalog (furniture_code,name,name_vn,category,price) VALUES
('wooden_bed','Wooden Bed','Giường Gỗ','bed',2000),
('royal_bed','Royal Bed','Giường Hoàng Gia','bed',15000),
('wooden_chair','Wooden Chair','Ghế Gỗ','seating',500),
('sofa','Sofa','Ghế Sofa','seating',5000),
('dining_table','Dining Table','Bàn Ăn','table',3000),
('storage_chest','Storage Chest','Rương Đồ','storage',2500),
('bookshelf','Bookshelf','Kệ Sách','storage',2000),
('candle_stand','Candle Stand','Giá Nến','lighting',800),
('chandelier','Chandelier','Đèn Chùm','lighting',8000),
('rug','Rug','Thảm','decor',1500),
('painting','Painting','Tranh Treo','decor',3000),
('potted_plant','Potted Plant','Chậu Cây','decor',1000)
ON CONFLICT (furniture_code) DO NOTHING;

\echo '✅ Seed 5 loại nhà + 12 nội thất. Tạo lô đất trong Admin → Nhà ở → Lô đất.'
