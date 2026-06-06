-- Ngành nghề mẫu (admin thêm tiếp trong panel). "Bác sĩ/công an" bản fantasy:
INSERT INTO business_types (type_code,name,name_vn,category,job_action,base_pay,purchase_price,daily_income,max_employees,sort_order) VALUES
('tavern','Tavern','Quán Trọ & Ăn Uống','food','cook',60,120000,6000,6,1),
('bakery','Bakery','Tiệm Bánh','food','bake',45,80000,4000,4,2),
('teahouse','Tea House','Quán Trà','food','brew',40,70000,3500,4,3),
('clinic','Apothecary Clinic','Y Quán (Thầy Thuốc)','medical','heal',80,200000,9000,5,4),
('temple_healer','Temple Healer','Thánh Đường Trị Liệu','medical','bless',90,250000,11000,4,5),
('guard_post','Guard Post','Trạm Vệ Binh (Công An)','security','patrol',70,180000,8000,8,6),
('bounty_office','Bounty Office','Sở Truy Nã','security','hunt',85,220000,9500,6,7),
('blacksmith','Blacksmith','Lò Rèn','craft','forge',65,150000,7000,5,8),
('tailor','Tailor','Tiệm May','craft','sew',50,90000,4500,4,9),
('alchemist','Alchemist','Tiệm Luyện Đan','craft','brew_potion',75,170000,7500,4,10),
('general_store','General Store','Cửa Hàng Tạp Hóa','trade','sell',55,100000,5000,5,11),
('market_stall','Market Stall','Sạp Chợ','trade','vend',35,50000,2500,3,12),
('bank','Bank','Ngân Hàng','finance','manage',100,500000,15000,6,13),
('auction_house','Auction House','Nhà Đấu Giá','finance','auction',95,400000,13000,5,14),
('tavern_stage','Bard Stage','Sân Khấu Hát Rong','entertainment','perform',50,110000,5500,5,15),
('arena','Arena','Đấu Trường','entertainment','host',80,300000,10000,8,16),
('stable','Stable','Chuồng Thú Cưỡi','service','tend',45,90000,4500,4,17),
('courier','Courier Guild','Hội Đưa Thư','service','deliver',50,95000,4800,6,18)
ON CONFLICT (type_code) DO NOTHING;

\echo '✅ Seed 18 ngành nghề. Thêm cơ sở cụ thể trong Admin → Ngành nghề → Quản lý cơ sở.'
