-- create-db.sql — Chạy lần đầu để tạo database và user
-- mysql -u root -p < scripts/create-db.sql

CREATE DATABASE IF NOT EXISTS nexus_isekai
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Đổi 'CHANGE_THIS_PASSWORD' thành mật khẩu thực
CREATE USER IF NOT EXISTS 'nexus'@'localhost' IDENTIFIED BY 'CHANGE_THIS_PASSWORD';
GRANT ALL PRIVILEGES ON nexus_isekai.* TO 'nexus'@'localhost';
FLUSH PRIVILEGES;

SELECT 'Database nexus_isekai created successfully!' as status;
