-- Đánh dấu tài khoản GM/admin (có character riêng + đầy đủ lệnh GM trong game)
-- Chạy sau khi đã có account: psql -U fro -d fantasyrealm -f deploy/sql/06-seed-gm.sql

-- Cấp quyền admin cho account tên 'gm' (đổi theo account thật của bạn)
UPDATE players SET is_admin = TRUE WHERE username = 'gm';

\echo '✅ Đã cấp quyền GM. Account này login vào game sẽ có đầy đủ lệnh GM.'
