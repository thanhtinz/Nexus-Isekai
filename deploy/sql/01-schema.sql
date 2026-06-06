-- Fantasy Realm Online - PostgreSQL Schema
-- Run once on fresh DB: psql -U fro -d fantasyrealm -f schema.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ACCOUNTS
-- ============================================================
CREATE TABLE IF NOT EXISTS players (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(32)  UNIQUE NOT NULL,
    email           VARCHAR(128) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    salt            VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    last_login      TIMESTAMPTZ,
    is_banned       BOOLEAN      DEFAULT FALSE,
    ban_reason      VARCHAR(512),
    premium_expires TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_player_username ON players(username);
CREATE INDEX IF NOT EXISTS idx_player_email    ON players(email);

-- ============================================================
-- CHARACTERS
-- ============================================================
CREATE TABLE IF NOT EXISTS characters (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    name            VARCHAR(32)  UNIQUE NOT NULL,
    faction_id      INT          NOT NULL DEFAULT 1 CHECK (faction_id BETWEEN 1 AND 4),
    religion_id     INT          NOT NULL DEFAULT 0,
    level           INT          NOT NULL DEFAULT 1,
    exp             BIGINT       NOT NULL DEFAULT 0,
    gold            BIGINT       NOT NULL DEFAULT 1000,
    premium_coins   BIGINT       NOT NULL DEFAULT 0,
    zone_id         INT          NOT NULL DEFAULT 1,
    pos_x           FLOAT        NOT NULL DEFAULT 100,
    pos_y           FLOAT        NOT NULL DEFAULT 100,
    outfit_json     TEXT         DEFAULT '{}',
    fame_fashion    INT          DEFAULT 0,
    fame_fishing    INT          DEFAULT 0,
    fame_cooking    INT          DEFAULT 0,
    fame_wealth     INT          DEFAULT 0,
    followers       INT          DEFAULT 0,
    play_time_secs  BIGINT       DEFAULT 0,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    last_seen       TIMESTAMPTZ  DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_char_player   ON characters(player_id);
CREATE INDEX IF NOT EXISTS idx_char_name     ON characters(name);
CREATE INDEX IF NOT EXISTS idx_char_zone     ON characters(zone_id);
CREATE INDEX IF NOT EXISTS idx_char_fashion  ON characters(fame_fashion DESC);
CREATE INDEX IF NOT EXISTS idx_char_fishing  ON characters(fame_fishing DESC);
CREATE INDEX IF NOT EXISTS idx_char_gold     ON characters(gold DESC);

-- ============================================================
-- ITEMS & INVENTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS item_definitions (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(64)  UNIQUE NOT NULL,
    name        VARCHAR(128) NOT NULL,
    category    VARCHAR(32)  NOT NULL,
    rarity      SMALLINT     NOT NULL DEFAULT 0,
    base_price  BIGINT       NOT NULL DEFAULT 100,
    stackable   BOOLEAN      DEFAULT TRUE,
    tradeable   BOOLEAN      DEFAULT TRUE,
    description TEXT,
    meta_json   TEXT         DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS inventories (
    id          BIGSERIAL PRIMARY KEY,
    character_id BIGINT  NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    item_id     BIGINT   NOT NULL,
    quantity    INT      NOT NULL DEFAULT 1 CHECK (quantity > 0),
    meta_json   TEXT     DEFAULT '{}',
    obtained_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(character_id, item_id)
);
CREATE INDEX IF NOT EXISTS idx_inv_char ON inventories(character_id);

-- ============================================================
-- MARKET
-- ============================================================
CREATE TABLE IF NOT EXISTS market_listings (
    id          BIGSERIAL PRIMARY KEY,
    seller_id   BIGINT      NOT NULL REFERENCES characters(id),
    item_id     BIGINT      NOT NULL,
    quantity    INT         NOT NULL CHECK (quantity > 0),
    price       BIGINT      NOT NULL CHECK (price > 0),
    listed_at   TIMESTAMPTZ DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    is_active   BOOLEAN     DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_listing_active ON market_listings(is_active, item_id);
CREATE INDEX IF NOT EXISTS idx_listing_seller ON market_listings(seller_id);

-- ============================================================
-- MAIL
-- ============================================================
CREATE TABLE IF NOT EXISTS mailbox (
    id          BIGSERIAL PRIMARY KEY,
    from_id     BIGINT,
    to_id       BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    subject     VARCHAR(128),
    body        TEXT,
    item_id     BIGINT,
    gold_amount BIGINT      DEFAULT 0,
    is_read     BOOLEAN     DEFAULT FALSE,
    sent_at     TIMESTAMPTZ DEFAULT NOW(),
    expires_at  TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_mail_to ON mailbox(to_id, is_read);

-- ============================================================
-- FRIENDS & RELATIONSHIPS
-- ============================================================
CREATE TABLE IF NOT EXISTS friendships (
    id          BIGSERIAL PRIMARY KEY,
    char_a      BIGINT NOT NULL REFERENCES characters(id),
    char_b      BIGINT NOT NULL REFERENCES characters(id),
    rel_type    VARCHAR(16) DEFAULT 'FRIEND',
    intimacy    INT DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(char_a, char_b),
    CHECK (char_a < char_b)
);

CREATE TABLE IF NOT EXISTS marriages (
    id          BIGSERIAL PRIMARY KEY,
    char_a      BIGINT NOT NULL REFERENCES characters(id),
    char_b      BIGINT NOT NULL REFERENCES characters(id),
    married_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(char_a, char_b)
);

-- ============================================================
-- ACHIEVEMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS achievements (
    id                  BIGSERIAL PRIMARY KEY,
    character_id        BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    achievement_code    VARCHAR(64) NOT NULL,
    unlocked_at         TIMESTAMPTZ DEFAULT NOW(),
    reward_given        BOOLEAN     DEFAULT FALSE,
    UNIQUE(character_id, achievement_code)
);
CREATE INDEX IF NOT EXISTS idx_ach_char ON achievements(character_id);

-- ============================================================
-- PROFESSIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS profession_levels (
    id              BIGSERIAL PRIMARY KEY,
    character_id    BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    profession_type VARCHAR(32) NOT NULL,
    level           INT         NOT NULL DEFAULT 0,
    exp             INT         NOT NULL DEFAULT 0,
    UNIQUE(character_id, profession_type)
);

-- ============================================================
-- PETS
-- ============================================================
CREATE TABLE IF NOT EXISTS pets (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT      NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    template_id BIGINT      NOT NULL,
    nickname    VARCHAR(32) NOT NULL,
    level       INT         NOT NULL DEFAULT 1,
    exp         BIGINT      NOT NULL DEFAULT 0,
    happiness   INT         NOT NULL DEFAULT 100,
    is_active   BOOLEAN     DEFAULT FALSE,
    obtained_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_pet_owner ON pets(owner_id);

-- ============================================================
-- FARMING PLOTS
-- ============================================================
CREATE TABLE IF NOT EXISTS farm_plots (
    id          BIGSERIAL PRIMARY KEY,
    character_id BIGINT     NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    plot_key    VARCHAR(64) NOT NULL,
    seed_id     BIGINT,
    planted_at  TIMESTAMPTZ,
    ready_at    TIMESTAMPTZ,
    state       VARCHAR(16) DEFAULT 'EMPTY',
    watered     BOOLEAN     DEFAULT FALSE,
    fertilized  BOOLEAN     DEFAULT FALSE,
    UNIQUE(character_id, plot_key)
);

-- ============================================================
-- MUSEUM
-- ============================================================
CREATE TABLE IF NOT EXISTS museum_exhibits (
    id          BIGSERIAL PRIMARY KEY,
    donor_id    BIGINT      NOT NULL REFERENCES characters(id),
    item_id     BIGINT      NOT NULL,
    category    VARCHAR(32) NOT NULL,
    donated_at  TIMESTAMPTZ DEFAULT NOW(),
    value       BIGINT      DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_museum_cat ON museum_exhibits(category);

-- ============================================================
-- POLITICS / ELECTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS election_votes (
    id              BIGSERIAL PRIMARY KEY,
    voter_id        BIGINT  NOT NULL REFERENCES characters(id),
    candidate_id    BIGINT  NOT NULL REFERENCES characters(id),
    zone_id         INT     NOT NULL,
    voted_at        TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(voter_id, zone_id)
);

CREATE TABLE IF NOT EXISTS mayors (
    zone_id         INT PRIMARY KEY,
    character_id    BIGINT  NOT NULL REFERENCES characters(id),
    elected_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- NEWSPAPER ARTICLES
-- ============================================================
CREATE TABLE IF NOT EXISTS articles (
    id          BIGSERIAL PRIMARY KEY,
    author_id   BIGINT      NOT NULL REFERENCES characters(id),
    title       VARCHAR(128) NOT NULL,
    body        TEXT        NOT NULL,
    category    VARCHAR(32),
    views       INT         DEFAULT 0,
    likes       INT         DEFAULT 0,
    is_feature  BOOLEAN     DEFAULT FALSE,
    published_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- AUDIT / GM LOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS gm_logs (
    id          BIGSERIAL PRIMARY KEY,
    gm_username VARCHAR(64),
    action      VARCHAR(64),
    target_id   BIGINT,
    details     TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- SEED: default item definitions
-- ============================================================
INSERT INTO item_definitions (code, name, category, rarity, base_price) VALUES
    ('flour',          'Bột Mì',            'ingredient', 0, 20),
    ('egg',            'Trứng Gà',           'ingredient', 0, 15),
    ('wheat_seed',     'Hạt Lúa Mì',         'seed',       0, 30),
    ('carrot_seed',    'Hạt Cà Rốt',         'seed',       0, 50),
    ('corn_seed',      'Hạt Bắp Ngô',        'seed',       0, 40),
    ('iron_ore',       'Quặng Sắt',          'material',   0,100),
    ('leather',        'Da Thuộc',           'material',   0, 80),
    ('herb_basic',     'Thảo Dược Cơ Bản',   'material',   0, 50),
    ('fishing_rod',    'Cần Câu Cơ Bản',     'tool',       0,500),
    ('bait',           'Mồi Câu',            'consumable', 0, 10),
    ('fish_common1',   'Cá Chép Thường',     'fish',       0, 50),
    ('fish_uncommon1', 'Cá Hồi Xuân',        'fish',       1,300),
    ('fish_rare1',     'Cá Rồng Đỏ',         'fish',       2,2000),
    ('fish_epic1',     'Cá Thần Long',       'fish',       3,10000),
    ('fish_legendary1','Cá Trăng Huyền Thoại','fish',      4,50000)
ON CONFLICT (code) DO NOTHING;

-- ============================================================
-- ADMIN PANEL TABLES
-- ============================================================

-- Admin users
CREATE TABLE IF NOT EXISTS admin_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32) DEFAULT 'moderator',
    email           VARCHAR(128),
    is_active       BOOLEAN DEFAULT TRUE,
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
-- Default superadmin (password: SuperAdmin@2024)
INSERT INTO admin_users(username,password_hash,role,email) VALUES
  ('superadmin','$2b$12$placeholder_hash_change_me_on_first_run','superadmin','admin@fantasyrealm.vn')
ON CONFLICT DO NOTHING;

-- Admin action logs
CREATE TABLE IF NOT EXISTS admin_logs (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT REFERENCES admin_users(id),
    action      VARCHAR(64) NOT NULL,
    detail      TEXT,
    ip          VARCHAR(64),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_admin_logs_time ON admin_logs(created_at DESC);

-- Multi-server registry
CREATE TABLE IF NOT EXISTS game_servers (
    server_id   BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    server_type VARCHAR(16) DEFAULT 'MAIN',
    api_host    VARCHAR(128) NOT NULL,
    api_port    INT DEFAULT 8080,
    game_port   INT DEFAULT 7777,
    admin_user  VARCHAR(64) DEFAULT 'gm',
    admin_pass  VARCHAR(128) DEFAULT 'gm_secret_2024',
    description TEXT,
    max_players INT DEFAULT 500,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);
-- Auto-register the main game server
INSERT INTO game_servers(name,server_type,api_host,api_port,game_port,description)
  VALUES('SV1 - Chính Thức','MAIN','game-server',8080,7777,'Server chính thức đầu tiên')
ON CONFLICT DO NOTHING;

-- Giftcodes
CREATE TABLE IF NOT EXISTS giftcodes (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(32) UNIQUE NOT NULL,
    rewards_json    TEXT DEFAULT '{}',
    max_uses        INT DEFAULT 1,
    expires_at      TIMESTAMPTZ,
    apply_servers   VARCHAR(32) DEFAULT 'all',
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS giftcode_uses (
    id              BIGSERIAL PRIMARY KEY,
    code_id         BIGINT NOT NULL REFERENCES giftcodes(id) ON DELETE CASCADE,
    character_id    BIGINT,
    server_id       BIGINT,
    used_at         TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(code_id, character_id)
);
CREATE INDEX IF NOT EXISTS idx_gc_uses_code ON giftcode_uses(code_id);

-- News & Articles
CREATE TABLE IF NOT EXISTS news (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(256) NOT NULL,
    slug        VARCHAR(256) UNIQUE NOT NULL,
    content     TEXT,
    excerpt     TEXT,
    category    VARCHAR(32) DEFAULT 'news',
    thumbnail   TEXT,
    is_published BOOLEAN DEFAULT FALSE,
    is_pinned   BOOLEAN DEFAULT FALSE,
    views       INT DEFAULT 0,
    author_id   BIGINT REFERENCES admin_users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_news_cat ON news(category, is_published);
CREATE INDEX IF NOT EXISTS idx_news_slug ON news(slug);

-- Scheduled events
CREATE TABLE IF NOT EXISTS scheduled_events (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(64) NOT NULL,
    server_id   BIGINT,
    trigger_at  TIMESTAMPTZ NOT NULL,
    note        TEXT,
    executed    BOOLEAN DEFAULT FALSE,
    created_by  BIGINT REFERENCES admin_users(id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- System config
CREATE TABLE IF NOT EXISTS system_config (
    key         VARCHAR(128) PRIMARY KEY,
    value       TEXT,
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);
INSERT INTO system_config(key,value) VALUES
  ('maintenance_mode','false'),
  ('exp_rate','1.0'),
  ('gold_rate','1.0'),
  ('drop_rate','1.0'),
  ('crosssv_enabled','true'),
  ('new_server_open','false')
ON CONFLICT DO NOTHING;

-- Hôn nhân (vợ chồng — dùng cho nhà chung)
CREATE TABLE IF NOT EXISTS marriages (
  id SERIAL PRIMARY KEY,
  char_a BIGINT NOT NULL,
  char_b BIGINT NOT NULL,
  married_at TIMESTAMPTZ DEFAULT NOW(),
  shared_house_id INT,                  -- nhà chung (NULL nếu chưa có)
  UNIQUE(char_a, char_b)
);
CREATE INDEX IF NOT EXISTS idx_marriage_a ON marriages(char_a);
CREATE INDEX IF NOT EXISTS idx_marriage_b ON marriages(char_b);
