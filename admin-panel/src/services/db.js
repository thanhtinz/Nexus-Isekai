const { Pool } = require('pg');

const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT||'5432'),
  database: process.env.DB_NAME || 'fantasyrealm',
  user: process.env.DB_USER || 'fro',
  password: process.env.DB_PASS || 'fro123',
  max: 20, idleTimeoutMillis: 30000, connectionTimeoutMillis: 2000,
});
pool.on('error', err => console.error('[DB]', err.message));
const query  = (t,p) => pool.query(t,p);
const getOne = async(t,p)=>{ const r=await pool.query(t,p); return r.rows[0]||null; };
const getAll = async(t,p)=>{ const r=await pool.query(t,p); return r.rows; };

async function initAdminTables() {
  const client = await pool.connect();
  try {
    await client.query(`
      CREATE TABLE IF NOT EXISTS admin_users (
        id SERIAL PRIMARY KEY, username VARCHAR(32) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL, role VARCHAR(16) DEFAULT 'admin',
        created_at TIMESTAMPTZ DEFAULT NOW(), last_login TIMESTAMPTZ
      );
      CREATE TABLE IF NOT EXISTS game_servers (
        id SERIAL PRIMARY KEY, name VARCHAR(64) NOT NULL, slug VARCHAR(32) UNIQUE NOT NULL,
        type VARCHAR(16) DEFAULT 'live', status VARCHAR(16) DEFAULT 'stopped',
        game_port INT NOT NULL DEFAULT 7777, admin_port INT NOT NULL DEFAULT 8080,
        host VARCHAR(128) DEFAULT 'localhost', db_name VARCHAR(64),
        max_players INT DEFAULT 500, container_id VARCHAR(128),
        api_user VARCHAR(32) DEFAULT 'gm', api_pass VARCHAR(128) DEFAULT 'gm_secret_2024',
        description TEXT, opens_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS server_logs (
        id BIGSERIAL PRIMARY KEY, server_id INT REFERENCES game_servers(id) ON DELETE CASCADE,
        level VARCHAR(8) DEFAULT 'INFO', message TEXT, created_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS giftcodes (
        id SERIAL PRIMARY KEY, code VARCHAR(32) UNIQUE NOT NULL, description VARCHAR(256),
        rewards JSONB NOT NULL DEFAULT '[]', max_uses INT DEFAULT 1, uses INT DEFAULT 0,
        server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
        expires_at TIMESTAMPTZ, created_by INT REFERENCES admin_users(id),
        created_at TIMESTAMPTZ DEFAULT NOW(), is_active BOOLEAN DEFAULT TRUE
      );
      CREATE TABLE IF NOT EXISTS giftcode_uses (
        id BIGSERIAL PRIMARY KEY, code_id INT REFERENCES giftcodes(id),
        character_name VARCHAR(32) NOT NULL, server_id INT,
        used_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS news_articles (
        id SERIAL PRIMARY KEY, title VARCHAR(256) NOT NULL, slug VARCHAR(256) UNIQUE,
        category VARCHAR(32) DEFAULT 'general', thumbnail VARCHAR(512), content TEXT,
        excerpt VARCHAR(512), author_id INT REFERENCES admin_users(id),
        server_id INT REFERENCES game_servers(id) ON DELETE SET NULL,
        views INT DEFAULT 0, is_published BOOLEAN DEFAULT FALSE, is_pinned BOOLEAN DEFAULT FALSE,
        published_at TIMESTAMPTZ, created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS game_events (
        id SERIAL PRIMARY KEY, name VARCHAR(128) NOT NULL, type VARCHAR(32) NOT NULL,
        description TEXT, rewards JSONB DEFAULT '[]',
        server_id INT REFERENCES game_servers(id) ON DELETE CASCADE,
        starts_at TIMESTAMPTZ, ends_at TIMESTAMPTZ, is_active BOOLEAN DEFAULT FALSE,
        created_by INT REFERENCES admin_users(id), created_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS admin_action_logs (
        id BIGSERIAL PRIMARY KEY, admin_id INT REFERENCES admin_users(id),
        action VARCHAR(64), target_type VARCHAR(32), target_id VARCHAR(64),
        details JSONB, ip VARCHAR(45), created_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE TABLE IF NOT EXISTS cross_server_bans (
        id SERIAL PRIMARY KEY, character_name VARCHAR(32), reason TEXT,
        banned_by INT REFERENCES admin_users(id),
        expires_at TIMESTAMPTZ, created_at TIMESTAMPTZ DEFAULT NOW()
      );
      CREATE INDEX IF NOT EXISTS idx_news_pub   ON news_articles(is_published, published_at DESC);
      CREATE INDEX IF NOT EXISTS idx_news_cat   ON news_articles(category);
      CREATE INDEX IF NOT EXISTS idx_gc_code    ON giftcodes(code);
      CREATE INDEX IF NOT EXISTS idx_svlog_sv   ON server_logs(server_id, created_at DESC);
    `);
    const bcrypt = require('bcryptjs');
    const ex = await client.query("SELECT id FROM admin_users WHERE username='admin'");
    if(!ex.rows.length){
      const hash = await bcrypt.hash('Admin@2024!', 12);
      await client.query("INSERT INTO admin_users(username,password_hash,role)VALUES('admin',$1,'superadmin')",[hash]);
      console.log('[DB] Default admin: admin / Admin@2024!');
    }
    const sv = await client.query("SELECT id FROM game_servers WHERE slug='sv1'");
    if(!sv.rows.length){
      await client.query("INSERT INTO game_servers(name,slug,type,game_port,admin_port,db_name,description)VALUES('Server 1','sv1','live',7777,8080,'fantasyrealm','Server chính thức SV1')");
      await client.query("INSERT INTO game_servers(name,slug,type,game_port,admin_port,db_name,description)VALUES('Server Beta','beta','beta',7778,8081,'fantasyrealm_beta','Server thử nghiệm — tính năng mới')");
      await client.query("INSERT INTO game_servers(name,slug,type,game_port,admin_port,db_name,description)VALUES('Server Test','test','test',7779,8082,'fantasyrealm_test','Nội bộ dev — không ổn định')");
    }
    console.log('[DB] Admin tables ready');
  } finally { client.release(); }
}
module.exports = { pool, query, getOne, getAll, initAdminTables };
