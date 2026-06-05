-- ============================================================
-- Fantasy Realm Online — Khởi tạo Databases cho Multi-Server
-- Chạy với quyền superuser: psql -U postgres -f 00-setup-databases.sql
-- ============================================================

-- Tạo user ứng dụng (đổi mật khẩu trước khi chạy production!)
DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fro') THEN
      CREATE ROLE fro LOGIN PASSWORD 'CHANGE_ME_STRONG_PASSWORD';
   END IF;
END
$$;

-- Database chính (SV1 - live)
SELECT 'CREATE DATABASE fantasyrealm OWNER fro'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fantasyrealm')\gexec

-- Database server beta/thử nghiệm
SELECT 'CREATE DATABASE fantasyrealm_beta OWNER fro'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fantasyrealm_beta')\gexec

-- Database server test nội bộ
SELECT 'CREATE DATABASE fantasyrealm_test OWNER fro'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fantasyrealm_test')\gexec

-- Cấp quyền
GRANT ALL PRIVILEGES ON DATABASE fantasyrealm      TO fro;
GRANT ALL PRIVILEGES ON DATABASE fantasyrealm_beta TO fro;
GRANT ALL PRIVILEGES ON DATABASE fantasyrealm_test TO fro;

\echo '✅ Databases đã tạo. Tiếp theo chạy schema.sql cho từng database:'
\echo '   psql -U fro -d fantasyrealm      -f schema.sql'
\echo '   psql -U fro -d fantasyrealm_beta -f schema.sql'
\echo '   psql -U fro -d fantasyrealm_test -f schema.sql'
