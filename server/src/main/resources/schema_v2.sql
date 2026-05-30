-- ============================================================
-- NEXUS ISEKAI - Schema V2 Migration
-- Chạy sau schema.sql để thêm các hệ thống mới
-- ============================================================

-- ─────────────────────────────────────────
-- 1. MULTI-SERVER & MAINTENANCE
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS game_servers (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    server_type  TINYINT NOT NULL DEFAULT 0,    -- 0=main, 1=test, 2=event
    host         VARCHAR(128) NOT NULL,
    port         INT NOT NULL DEFAULT 7777,
    admin_port   INT NOT NULL DEFAULT 8080,
    status       TINYINT NOT NULL DEFAULT 0,    -- 0=offline,1=online,2=maintenance
    open_time    DATETIME NULL,                 -- NULL = luôn mở
    close_time   DATETIME NULL,
    max_players  INT NOT NULL DEFAULT 1000,
    description  TEXT,
    version      VARCHAR(32),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maintenance_schedule (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    server_id    INT NOT NULL DEFAULT 0,        -- 0 = all servers
    title        VARCHAR(128) NOT NULL,
    message      TEXT NOT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    patch_notes  TEXT,
    status       TINYINT NOT NULL DEFAULT 0,    -- 0=scheduled,1=active,2=done
    created_by   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (server_id) REFERENCES game_servers(id) ON DELETE CASCADE
);

-- Seed default servers
INSERT IGNORE INTO game_servers (id, name, server_type, host, port, admin_port, status, description, version)
VALUES
(1, 'Server Chính', 0, '127.0.0.1', 7777, 8080, 1, 'Server chính thức', '1.0.0'),
(2, 'Server Test',  1, '127.0.0.1', 7778, 8081, 0, 'Server thử nghiệm tính năng mới', '1.0.0-dev');

-- ─────────────────────────────────────────
-- 2. CURRENCY: DIAMOND
-- ─────────────────────────────────────────
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS diamond    INT NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS web_token  VARCHAR(256) DEFAULT NULL; -- dùng cho webshop
ALTER TABLE characters ADD COLUMN IF NOT EXISTS gender   TINYINT NOT NULL DEFAULT 0; -- 0=male,1=female

CREATE TABLE IF NOT EXISTS diamond_transactions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT NOT NULL,
    amount       INT NOT NULL,      -- + nạp / - tiêu
    type         VARCHAR(32) NOT NULL, -- 'topup','spend','giftcode','refund','bonus'
    ref_id       VARCHAR(128),      -- sepay transaction_id / giftcode / item_id
    description  VARCHAR(256),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 3. PAYMENT - SEPAY
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sepay_config (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    api_key          VARCHAR(256) NOT NULL DEFAULT '',
    webhook_secret   VARCHAR(256) NOT NULL DEFAULT '',
    bank_account     VARCHAR(64) NOT NULL DEFAULT '',
    bank_name        VARCHAR(64) NOT NULL DEFAULT '',
    account_name     VARCHAR(128) NOT NULL DEFAULT '',
    callback_url     VARCHAR(256) NOT NULL DEFAULT '',
    is_active        TINYINT NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO sepay_config (id) VALUES (1);

CREATE TABLE IF NOT EXISTS topup_packages (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    diamond      INT NOT NULL,
    bonus_diamond INT NOT NULL DEFAULT 0,
    price_vnd    INT NOT NULL,
    is_featured  TINYINT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1,
    icon_url     VARCHAR(256),
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO topup_packages (id,name,diamond,bonus_diamond,price_vnd,is_featured,sort_order) VALUES
(1,'Gói Nhỏ',        100,  0,    10000, 0, 1),
(2,'Gói Vừa',        500,  50,   50000, 0, 2),
(3,'Gói Lớn',       1200, 200,  100000, 1, 3),
(4,'Gói VIP',       2500, 500,  200000, 0, 4),
(5,'Gói Siêu VIP',  6500,1500,  500000, 1, 5);

CREATE TABLE IF NOT EXISTS topup_orders (
    id              VARCHAR(64) PRIMARY KEY,          -- format: NI_{timestamp}_{random}
    account_id      BIGINT NOT NULL,
    package_id      INT NOT NULL,
    amount_vnd      INT NOT NULL,
    diamond         INT NOT NULL,
    bonus_diamond   INT NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,paid,failed,refunded
    sepay_txn_id    VARCHAR(128),
    sepay_content   VARCHAR(256),
    is_first_topup  TINYINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at         TIMESTAMP NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (package_id) REFERENCES topup_packages(id)
);

-- Nạp đầu nhận thưởng config
CREATE TABLE IF NOT EXISTS first_topup_rewards (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    item_id     INT NOT NULL,
    qty         INT NOT NULL DEFAULT 1,
    description VARCHAR(128)
);

INSERT IGNORE INTO first_topup_rewards (item_id, qty, description) VALUES
(1, 1, 'Gói Trang Bị Khởi Đầu'),
(2, 3, 'Bình Hồi Phục x3');

-- ─────────────────────────────────────────
-- 4. WEBSHOP ITEMS (không có trong shop ingame)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS webshop_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    item_type    VARCHAR(32) NOT NULL, -- 'skin','cosmetic','pack','mount','pet'
    class_id     INT NOT NULL DEFAULT 0, -- 0 = all classes
    diamond_price INT NOT NULL DEFAULT 0,
    original_price INT NOT NULL DEFAULT 0, -- để hiển thị giá gốc nếu sale
    is_limited   TINYINT NOT NULL DEFAULT 0,
    stock        INT NOT NULL DEFAULT -1, -- -1 = unlimited
    is_active    TINYINT NOT NULL DEFAULT 1,
    is_featured  TINYINT NOT NULL DEFAULT 0,
    icon_url     VARCHAR(256),
    preview_url  VARCHAR(256),
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webshop_item_contents (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    webshop_item_id INT NOT NULL,
    item_id      INT NOT NULL,
    qty          INT NOT NULL DEFAULT 1,
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS webshop_orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id   BIGINT NOT NULL,
    char_id      BIGINT NOT NULL,
    webshop_item_id INT NOT NULL,
    diamond_spent INT NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,delivered,failed
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id)
);

-- ─────────────────────────────────────────
-- 5. MISSION PASS (Battle Pass)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mission_pass_seasons (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    description  TEXT,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    free_diamond INT NOT NULL DEFAULT 0,   -- giá mua pass miễn phí (0 = free tier)
    premium_diamond INT NOT NULL DEFAULT 0,
    max_level    INT NOT NULL DEFAULT 100,
    is_active    TINYINT NOT NULL DEFAULT 0,
    banner_url   VARCHAR(256),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mission_pass_rewards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    level        INT NOT NULL,
    tier         TINYINT NOT NULL DEFAULT 0, -- 0=free,1=premium
    item_id      INT NOT NULL,
    item_qty     INT NOT NULL DEFAULT 1,
    diamond      INT NOT NULL DEFAULT 0,
    gold         INT NOT NULL DEFAULT 0,
    description  VARCHAR(128),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mission_pass_tasks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    task_type    VARCHAR(32) NOT NULL, -- 'daily','weekly','seasonal'
    title        VARCHAR(128) NOT NULL,
    description  TEXT,
    task_condition VARCHAR(64) NOT NULL, -- 'kill_monster:50', 'login:1', 'complete_quest:3'
    target       INT NOT NULL DEFAULT 1,
    exp_reward   INT NOT NULL DEFAULT 10, -- pass EXP (không phải char EXP)
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_mission_pass (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    season_id    INT NOT NULL,
    pass_level   INT NOT NULL DEFAULT 1,
    pass_exp     INT NOT NULL DEFAULT 0,
    has_premium  TINYINT NOT NULL DEFAULT 0,
    claimed_rewards TEXT, -- JSON array: [{"level":1,"tier":0}]
    UNIQUE KEY uk_char_season (char_id, season_id),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id)
);

CREATE TABLE IF NOT EXISTS player_pass_task_progress (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    task_id      INT NOT NULL,
    season_id    INT NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    is_completed TINYINT NOT NULL DEFAULT 0,
    reset_date   DATE,
    UNIQUE KEY uk_char_task (char_id, task_id),
    FOREIGN KEY (task_id) REFERENCES mission_pass_tasks(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 6. GIFT CODES
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS giftcodes (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(32) NOT NULL UNIQUE,
    name         VARCHAR(128),
    description  TEXT,
    max_uses     INT NOT NULL DEFAULT 1,      -- -1 = unlimited
    used_count   INT NOT NULL DEFAULT 0,
    min_level    INT NOT NULL DEFAULT 1,
    expires_at   DATETIME NULL,
    is_active    TINYINT NOT NULL DEFAULT 1,
    server_id    INT NOT NULL DEFAULT 0,      -- 0 = all servers
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS giftcode_rewards (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    giftcode_id  INT NOT NULL,
    reward_type  VARCHAR(16) NOT NULL, -- 'item','diamond','gold','title','pet','mount'
    reward_id    INT NOT NULL DEFAULT 0,
    qty          INT NOT NULL DEFAULT 1,
    FOREIGN KEY (giftcode_id) REFERENCES giftcodes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS giftcode_usage (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    giftcode_id  INT NOT NULL,
    char_id      BIGINT NOT NULL,
    account_id   BIGINT NOT NULL,
    used_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code_char (giftcode_id, char_id),
    FOREIGN KEY (giftcode_id) REFERENCES giftcodes(id)
);

-- ─────────────────────────────────────────
-- 7. TITLES (Danh hiệu)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS titles (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    description  TEXT,
    title_type   VARCHAR(32) NOT NULL DEFAULT 'normal', -- 'quest','achievement','giftcode','event','purchase'
    stat_bonus   VARCHAR(256), -- JSON: {"str":5,"agi":3}
    color_hex    VARCHAR(8) NOT NULL DEFAULT 'FFFFFF',
    icon_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_titles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    title_id     INT NOT NULL,
    is_equipped  TINYINT NOT NULL DEFAULT 0,
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source       VARCHAR(64), -- 'quest_1','giftcode_ABCD','admin'
    UNIQUE KEY uk_char_title (char_id, title_id),
    FOREIGN KEY (title_id) REFERENCES titles(id)
);

INSERT IGNORE INTO titles (id,name,description,title_type,color_hex,sort_order) VALUES
(1,'Tân Binh',  'Nhân vật mới tạo',         'achievement','AAAAAA',1),
(2,'Chiến Binh','Hoàn thành 100 trận đấu',  'achievement','FF6600',2),
(3,'Thám Hiểm', 'Khám phá 10 bản đồ',       'achievement','00CC88',3),
(4,'Nạp Thủ',   'Nạp game lần đầu',         'purchase',   'FFD700',4),
(5,'Nhà Vô Địch','Đứng đầu bảng xếp hạng',  'achievement','FF0000',5);

-- ─────────────────────────────────────────
-- 8. SKILL SYSTEM EXTENDED (30-40 skills/class, 7 active)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS skill_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    class_id     INT NOT NULL,              -- 0 = all classes
    skill_type   VARCHAR(16) NOT NULL DEFAULT 'active', -- active/passive/ultimate
    element      VARCHAR(16) NOT NULL DEFAULT 'none', -- fire,ice,lightning,none
    base_damage  INT NOT NULL DEFAULT 0,
    mp_cost      INT NOT NULL DEFAULT 0,
    cooldown_ms  INT NOT NULL DEFAULT 1000,
    range        FLOAT NOT NULL DEFAULT 1.0,
    max_level    INT NOT NULL DEFAULT 10,
    description  TEXT,
    icon_id      INT NOT NULL DEFAULT 0,
    unlock_level INT NOT NULL DEFAULT 1,
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS character_skill_slots (
    char_id      BIGINT NOT NULL,
    slot_index   TINYINT NOT NULL,  -- 0-6 (7 skill slots)
    skill_id     INT NOT NULL,
    PRIMARY KEY (char_id, slot_index)
);

-- Mở rộng character_skills: thêm level và exp cho từng skill
ALTER TABLE character_skills ADD COLUMN IF NOT EXISTS skill_level INT NOT NULL DEFAULT 1;
ALTER TABLE character_skills ADD COLUMN IF NOT EXISTS skill_exp   INT NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────
-- 9. ENHANCEMENT (Cường hoá)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS enhancement_config (
    level        INT PRIMARY KEY,
    success_rate FLOAT NOT NULL,       -- % thành công
    cost_gold    INT NOT NULL DEFAULT 0,
    cost_diamond INT NOT NULL DEFAULT 0,
    cost_material_id INT NOT NULL DEFAULT 0,
    cost_material_qty INT NOT NULL DEFAULT 1,
    stat_bonus_pct FLOAT NOT NULL DEFAULT 0.05 -- % tăng stat mỗi level
);

INSERT IGNORE INTO enhancement_config VALUES
(1,100.0,500,  0,0,0,0.05),(2,90.0, 1000, 0,0,0,0.05),
(3,80.0, 2000, 0,0,0,0.05),(4,70.0, 5000, 0,0,0,0.07),
(5,60.0, 8000, 0,0,0,0.07),(6,50.0,12000, 0,5,1,1,0.10),
(7,40.0,20000, 0,5,1,2,0.10),(8,30.0,30000,10,5,2,3,0.12),
(9,20.0,50000,20,5,3,4,0.15),(10,10.0,100000,50,5,4,5,0.20);

ALTER TABLE character_inventory ADD COLUMN IF NOT EXISTS enhance_level INT NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────
-- 10. PETS
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pet_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    element      VARCHAR(16) NOT NULL DEFAULT 'none',
    rarity       TINYINT NOT NULL DEFAULT 1, -- 1=common,...5=legendary
    base_hp      INT NOT NULL DEFAULT 100,
    base_atk     INT NOT NULL DEFAULT 10,
    base_def     INT NOT NULL DEFAULT 5,
    skill_id     INT NOT NULL DEFAULT 0,
    icon_id      INT NOT NULL DEFAULT 0,
    obtain_source VARCHAR(64),
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS player_pets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    template_id  INT NOT NULL,
    nickname     VARCHAR(32),
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    hunger       INT NOT NULL DEFAULT 100, -- 0-100
    loyalty      INT NOT NULL DEFAULT 50,
    is_active    TINYINT NOT NULL DEFAULT 0, -- đang mang theo
    equipment    VARCHAR(256),              -- JSON pet gear
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES pet_templates(id)
);

INSERT IGNORE INTO pet_templates (id,name,element,rarity,base_hp,base_atk,base_def,icon_id,obtain_source) VALUES
(1,'Cáo Con',    'fire',1,120,15,8, 2001,'quest'),
(2,'Thỏ Băng',   'ice', 1,100,12,10,2002,'shop'),
(3,'Rồng Mini',  'fire',5,500,80,40,2010,'webshop'),
(4,'Phượng Hoàng','lightning',4,350,60,30,2011,'event'),
(5,'Mèo Đêm',   'none',2,150,20,15,2003,'giftcode');

-- ─────────────────────────────────────────
-- 11. MOUNTS (Thú cưỡi)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mount_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    speed_bonus  FLOAT NOT NULL DEFAULT 0.2,  -- +20% tốc độ
    rarity       TINYINT NOT NULL DEFAULT 1,
    icon_id      INT NOT NULL DEFAULT 0,
    obtain_source VARCHAR(64),
    is_active    TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS player_mounts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    template_id  INT NOT NULL,
    is_active    TINYINT NOT NULL DEFAULT 0,
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    obtained_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES mount_templates(id)
);

INSERT IGNORE INTO mount_templates (id,name,speed_bonus,rarity,icon_id,obtain_source) VALUES
(1,'Ngựa Trắng',   0.20,1,3001,'quest'),
(2,'Hổ Vàng',      0.35,3,3002,'webshop'),
(3,'Rồng Thiêng',  0.50,5,3010,'event'),
(4,'Sói Đêm',      0.30,2,3003,'giftcode');

-- ─────────────────────────────────────────
-- 12. RELATIONSHIP & MARRIAGE
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS relationships (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a    BIGINT NOT NULL,
    char_id_b    BIGINT NOT NULL,
    rel_type     VARCHAR(16) NOT NULL DEFAULT 'friend', -- friend,dating,married
    affection    INT NOT NULL DEFAULT 0,
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (char_id_a, char_id_b)
);

CREATE TABLE IF NOT EXISTS marriages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a    BIGINT NOT NULL,
    char_id_b    BIGINT NOT NULL,
    wedding_date TIMESTAMP,
    wedding_map  INT NOT NULL DEFAULT 0,
    ring_item_id INT NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'engaged', -- engaged,married,divorced
    anniversary  DATE,
    UNIQUE KEY uk_marriage (char_id_a, char_id_b)
);

-- ─────────────────────────────────────────
-- 13. CHILDREN (Con cái)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS children (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    marriage_id  BIGINT NOT NULL,
    name         VARCHAR(32) NOT NULL,
    gender       TINYINT NOT NULL DEFAULT 0,
    age          INT NOT NULL DEFAULT 0,
    level        INT NOT NULL DEFAULT 1,
    exp          INT NOT NULL DEFAULT 0,
    hp           INT NOT NULL DEFAULT 50,
    max_hp       INT NOT NULL DEFAULT 50,
    atk          INT NOT NULL DEFAULT 5,
    def          INT NOT NULL DEFAULT 3,
    skin_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 0,     -- đang mang theo trong chiến đấu
    happiness    INT NOT NULL DEFAULT 100,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (marriage_id) REFERENCES marriages(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 14. HOUSING (Nhà)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS houses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL UNIQUE,         -- chủ nhà
    spouse_id    BIGINT,                          -- NULL nếu chưa kết hôn
    house_level  INT NOT NULL DEFAULT 1,
    house_style  INT NOT NULL DEFAULT 0,
    map_id       INT NOT NULL DEFAULT 100,        -- map nhà riêng
    happiness    INT NOT NULL DEFAULT 100,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS house_furniture (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    house_id     BIGINT NOT NULL,
    furniture_id INT NOT NULL,
    pos_x        FLOAT NOT NULL DEFAULT 0,
    pos_y        FLOAT NOT NULL DEFAULT 0,
    rotation     INT NOT NULL DEFAULT 0,
    FOREIGN KEY (house_id) REFERENCES houses(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 15. FARMING (Trồng cây)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS farm_plots (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    plot_index   INT NOT NULL,
    seed_id      INT NOT NULL DEFAULT 0,         -- 0 = trống
    planted_at   TIMESTAMP NULL,
    stage        TINYINT NOT NULL DEFAULT 0,     -- 0=empty,1=seed,2=sprout,3=mature
    water_count  INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_char_plot (char_id, plot_index)
);

CREATE TABLE IF NOT EXISTS animal_pens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    pen_index    INT NOT NULL,
    animal_id    INT NOT NULL DEFAULT 0,
    animal_type  VARCHAR(16) NOT NULL DEFAULT 'fish', -- fish,bird,rabbit,cow
    name         VARCHAR(32),
    hunger       INT NOT NULL DEFAULT 100,
    happiness    INT NOT NULL DEFAULT 100,
    UNIQUE KEY uk_char_pen (char_id, pen_index)
);

-- ─────────────────────────────────────────
-- 16. MINIGAMES
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS minigame_rooms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_type    VARCHAR(32) NOT NULL, -- 'bau_cua','tien_len','dua_thu','o_an_quan','da_ga','do_vui'
    room_name    VARCHAR(64),
    host_char_id BIGINT NOT NULL,
    max_players  INT NOT NULL DEFAULT 4,
    min_bet      INT NOT NULL DEFAULT 0,
    max_bet      INT NOT NULL DEFAULT 100000,
    currency     TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    status       VARCHAR(16) NOT NULL DEFAULT 'waiting', -- waiting,playing,finished
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS minigame_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT NOT NULL,
    char_id      BIGINT NOT NULL,
    game_type    VARCHAR(32) NOT NULL,
    bet_amount   INT NOT NULL DEFAULT 0,
    win_amount   INT NOT NULL DEFAULT 0,
    result       VARCHAR(16) NOT NULL, -- win,lose,draw
    played_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ─────────────────────────────────────────
-- 17. MENTOR SYSTEM (Sư đồ)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mentor_relationships (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    mentor_id    BIGINT NOT NULL,               -- char_id của thầy (hoặc NPC id < 0)
    student_id   BIGINT NOT NULL,
    is_npc_mentor TINYINT NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'active', -- active,graduated,broken
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    graduated_at TIMESTAMP NULL,
    UNIQUE KEY uk_mentor_student (mentor_id, student_id)
);

CREATE TABLE IF NOT EXISTS mentor_missions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(128) NOT NULL,
    description  TEXT,
    mission_type VARCHAR(32) NOT NULL,           -- 'reach_level','kill_boss','complete_quest'
    target_value INT NOT NULL DEFAULT 1,
    reward_exp   INT NOT NULL DEFAULT 0,
    reward_gold  INT NOT NULL DEFAULT 0,
    reward_item_id INT NOT NULL DEFAULT 0,
    sort_order   INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mentor_mission_progress (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    relationship_id BIGINT NOT NULL,
    mission_id   INT NOT NULL,
    progress     INT NOT NULL DEFAULT 0,
    is_completed TINYINT NOT NULL DEFAULT 0,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uk_rel_mission (relationship_id, mission_id),
    FOREIGN KEY (relationship_id) REFERENCES mentor_relationships(id) ON DELETE CASCADE,
    FOREIGN KEY (mission_id) REFERENCES mentor_missions(id)
);

INSERT IGNORE INTO mentor_missions (id,title,description,mission_type,target_value,reward_exp,reward_gold,sort_order) VALUES
(1,'Đặt Nền Móng',  'Đệ tử đạt level 10',  'reach_level',  10, 500,  1000, 1),
(2,'Bước Đường Dài','Đệ tử đạt level 30',  'reach_level',  30, 2000, 5000, 2),
(3,'Thực Chiến',    'Đệ tử hạ 50 monster', 'kill_monster', 50, 1000, 2000, 3),
(4,'Xuất Sư',       'Đệ tử đạt level 50',  'reach_level',  50, 5000,10000, 4);

-- ─────────────────────────────────────────
-- 18. LEADERBOARD / RANKINGS
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leaderboard_snapshots (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id      BIGINT NOT NULL,
    char_name    VARCHAR(32) NOT NULL,
    class_id     INT NOT NULL,
    level        INT NOT NULL,
    rank_type    VARCHAR(32) NOT NULL, -- 'level','pvp','wealth','guild'
    rank_value   BIGINT NOT NULL DEFAULT 0,
    rank_pos     INT NOT NULL DEFAULT 0,
    snapshot_date DATE NOT NULL,
    UNIQUE KEY uk_char_rank_date (char_id, rank_type, snapshot_date)
);

-- ─────────────────────────────────────────
-- 19. CHARACTER CLASSES EXTENDED (admin có thể thêm/sửa/xoá)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS class_templates (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    name_en      VARCHAR(64),
    description  TEXT,
    base_hp      INT NOT NULL DEFAULT 200,
    base_mp      INT NOT NULL DEFAULT 100,
    base_str     INT NOT NULL DEFAULT 10,
    base_agi     INT NOT NULL DEFAULT 10,
    base_intel   INT NOT NULL DEFAULT 10,
    base_vit     INT NOT NULL DEFAULT 10,
    hp_per_level INT NOT NULL DEFAULT 20,
    mp_per_level INT NOT NULL DEFAULT 10,
    starter_weapon_id INT NOT NULL DEFAULT 1,
    first_quest_id INT NOT NULL DEFAULT 1,
    icon_id      INT NOT NULL DEFAULT 0,
    male_sprite  VARCHAR(256),
    female_sprite VARCHAR(256),
    is_active    TINYINT NOT NULL DEFAULT 1,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO class_templates (id,name,name_en,base_hp,base_mp,base_str,base_agi,base_intel,base_vit,starter_weapon_id,first_quest_id,sort_order) VALUES
(1,'Kiếm Sĩ',  'Warrior',  250,100,15,10, 5,12,1,1,1),
(2,'Sát Thủ',  'Rogue',    180,120,12,18, 8, 8,2,2,2),
(3,'Pháp Sư',  'Mage',     150,200, 5,10,18, 8,3,3,3),
(4,'Pháp Thủ', 'Isekai',   200,180,10,12,15,10,4,4,4),
(5,'Cung Thủ', 'Archer',   200,130, 8,16,10,10,5,5,5);

-- ─────────────────────────────────────────
-- 20. WEBSHOP: giới hạn mua & kho hàng
-- ─────────────────────────────────────────

-- Thêm cột giới hạn vào webshop_items (nếu chưa có)
ALTER TABLE webshop_items
    ADD COLUMN IF NOT EXISTS per_user_limit   INT NOT NULL DEFAULT -1,  -- -1 = không giới hạn
    ADD COLUMN IF NOT EXISTS per_user_period  VARCHAR(16) NOT NULL DEFAULT 'all', -- all, daily, weekly, monthly
    ADD COLUMN IF NOT EXISTS total_sold       INT NOT NULL DEFAULT 0;

-- Lịch sử mua theo user (để kiểm tra giới hạn)
CREATE TABLE IF NOT EXISTS webshop_purchase_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    webshop_item_id INT NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    diamond_spent   INT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_item (account_id, webshop_item_id),
    FOREIGN KEY (webshop_item_id) REFERENCES webshop_items(id)
);

-- ─────────────────────────────────────────
-- 21. ADMIN ITEM WAREHOUSE (kho item admin)
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS admin_item_warehouse (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id      INT NOT NULL,
    item_name    VARCHAR(128) NOT NULL DEFAULT '',
    item_type    VARCHAR(32) NOT NULL DEFAULT 'item', -- item,diamond,gold,pet,mount,title
    qty          INT NOT NULL DEFAULT 0,
    qty_total    INT NOT NULL DEFAULT 0,   -- tổng đã thêm vào
    qty_used     INT NOT NULL DEFAULT 0,   -- tổng đã dùng
    description  TEXT,
    icon_url     VARCHAR(256),
    is_active    TINYINT NOT NULL DEFAULT 1,
    created_by   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_warehouse_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    action       VARCHAR(16) NOT NULL, -- 'add','use','adjust'
    qty_change   INT NOT NULL,
    reason       VARCHAR(256),
    ref_type     VARCHAR(32),   -- 'giftcode','event','manual','webshop'
    ref_id       VARCHAR(128),
    admin_user   VARCHAR(64),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warehouse_id) REFERENCES admin_item_warehouse(id)
);

-- ─────────────────────────────────────────
-- 22. MISSION PASS: nhiệm vụ cấu hình đầy đủ
-- ─────────────────────────────────────────

-- Thêm pass_type vào webshop_items để phân biệt đây là premium pass
-- pass_season_id liên kết đến season tương ứng
ALTER TABLE webshop_items
    ADD COLUMN IF NOT EXISTS pass_season_id INT NOT NULL DEFAULT 0;  -- >0 = đây là item mua premium pass

-- Cập nhật premium_diamond = 0 trong season (giá lấy từ webshop_items)
-- Bảng tasks đã có, thêm reset_type cho rõ
ALTER TABLE mission_pass_tasks
    ADD COLUMN IF NOT EXISTS reset_type VARCHAR(16) NOT NULL DEFAULT 'none'; -- none, daily, weekly

-- Mốc nhận thưởng bonus (ngoài reward theo level)
CREATE TABLE IF NOT EXISTS mission_pass_milestones (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    season_id    INT NOT NULL,
    milestone_exp INT NOT NULL,  -- đạt X pass exp thì nhận
    tier         TINYINT NOT NULL DEFAULT 0, -- 0=free, 1=premium
    item_id      INT NOT NULL DEFAULT 0,
    item_qty     INT NOT NULL DEFAULT 1,
    diamond      INT NOT NULL DEFAULT 0,
    gold         INT NOT NULL DEFAULT 0,
    description  VARCHAR(128),
    FOREIGN KEY (season_id) REFERENCES mission_pass_seasons(id) ON DELETE CASCADE
);


-- ─────────────────────────────────────────
-- 23. CHAT EXTENDED — sticker, lì xì, voice, cross-server
-- ─────────────────────────────────────────

-- Sticker packs
CREATE TABLE IF NOT EXISTS sticker_packs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    price_diamond INT NOT NULL DEFAULT 0,  -- 0 = free/default
    is_default  TINYINT NOT NULL DEFAULT 0,
    is_active   TINYINT NOT NULL DEFAULT 1,
    icon_id     INT NOT NULL DEFAULT 0,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stickers (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    pack_id     INT NOT NULL,
    name        VARCHAR(64) NOT NULL,
    asset_key   VARCHAR(128) NOT NULL,  -- Resources path: "Stickers/pack1/sticker_01"
    sort_order  INT NOT NULL DEFAULT 0,
    FOREIGN KEY (pack_id) REFERENCES sticker_packs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_sticker_packs (
    char_id     BIGINT NOT NULL,
    pack_id     INT NOT NULL,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, pack_id)
);

-- Red envelopes (lì xì)
CREATE TABLE IF NOT EXISTS red_envelopes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_char_id  BIGINT NOT NULL,
    sender_name     VARCHAR(32) NOT NULL,
    channel         TINYINT NOT NULL,           -- 0=world,1=map,2=guild
    channel_ref     BIGINT NOT NULL DEFAULT 0,  -- map_id or guild_id
    currency        TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    total_amount    INT NOT NULL,
    amount_per_grab INT NOT NULL,
    max_grabbers    INT NOT NULL,
    grabbed_count   INT NOT NULL DEFAULT 0,
    message         VARCHAR(128) NOT NULL DEFAULT '',
    expires_at      TIMESTAMP NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,exhausted,expired
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS red_envelope_grabs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    envelope_id BIGINT NOT NULL,
    char_id     BIGINT NOT NULL,
    char_name   VARCHAR(32) NOT NULL,
    amount      INT NOT NULL,
    grabbed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_env_char (envelope_id, char_id),
    FOREIGN KEY (envelope_id) REFERENCES red_envelopes(id)
);

-- Voice messages (lưu URL/path, không lưu raw audio)
CREATE TABLE IF NOT EXISTS voice_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id     BIGINT NOT NULL,
    char_name   VARCHAR(32) NOT NULL,
    channel     TINYINT NOT NULL,
    channel_ref BIGINT NOT NULL DEFAULT 0,
    file_path   VARCHAR(256) NOT NULL,   -- đường dẫn file trên server
    duration_ms INT NOT NULL DEFAULT 0,  -- thời lượng ms
    file_size   INT NOT NULL DEFAULT 0,  -- bytes
    expires_at  TIMESTAMP NOT NULL,      -- auto-delete sau 7 ngày
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat history (lưu lại để reload khi re-login)
CREATE TABLE IF NOT EXISTS chat_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    server_id    INT NOT NULL DEFAULT 1,
    channel      TINYINT NOT NULL,
    channel_ref  BIGINT NOT NULL DEFAULT 0,
    sender_id    BIGINT NOT NULL,
    sender_name  VARCHAR(32) NOT NULL,
    content_type TINYINT NOT NULL DEFAULT 0,
    content      TEXT NOT NULL,         -- JSON: {"text":"..."} / {"stickerId":1} / v.v.
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_channel_time (channel, channel_ref, created_at)
);

-- Cross-server relay queue (server khác poll để nhận)
CREATE TABLE IF NOT EXISTS cross_server_relay (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_server INT NOT NULL,
    sender_id   BIGINT NOT NULL,
    sender_name VARCHAR(32) NOT NULL,
    content_type TINYINT NOT NULL DEFAULT 0,
    content     TEXT NOT NULL,
    sent_to     TEXT,   -- JSON array server ids đã nhận
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
);

-- Seed default sticker packs
INSERT IGNORE INTO sticker_packs (id,name,is_default,is_active,sort_order) VALUES
(1,'Mặc Định',1,1,0),
(2,'Cute Pack',0,1,1),
(3,'Chiến Đấu',0,1,2);

INSERT IGNORE INTO stickers (id,pack_id,name,asset_key,sort_order) VALUES
(1,1,'Vui','Stickers/default/happy',0),(2,1,'Buồn','Stickers/default/sad',1),
(3,1,'Tức','Stickers/default/angry',2),(4,1,'OK','Stickers/default/ok',3),
(5,1,'GG','Stickers/default/gg',4),(6,1,'?!','Stickers/default/what',5),
(7,2,'Cute1','Stickers/cute/cat1',0),(8,2,'Cute2','Stickers/cute/cat2',1),
(9,3,'Attack','Stickers/battle/atk',0),(10,3,'Win','Stickers/battle/win',1);

-- ─────────────────────────────────────────
-- 24. GUILD extended
-- ─────────────────────────────────────────
ALTER TABLE guilds
    ADD COLUMN IF NOT EXISTS icon_id       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notice        TEXT,
    ADD COLUMN IF NOT EXISTS max_members   INT NOT NULL DEFAULT 50,
    ADD COLUMN IF NOT EXISTS gold          BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_active     TINYINT NOT NULL DEFAULT 1;

ALTER TABLE guild_members
    ADD COLUMN IF NOT EXISTS contribution  INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS title         VARCHAR(32) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS guild_invites (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    guild_id    BIGINT NOT NULL,
    inviter_id  BIGINT NOT NULL,
    target_id   BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_guild_target (guild_id, target_id),
    FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
);

-- ─────────────────────────────────────────
-- 25. SKILL SYSTEM EXTENDED
-- ─────────────────────────────────────────
-- Seed 8 skills cho mỗi class (40 total)
INSERT IGNORE INTO skill_templates
(id,name,class_id,skill_type,element,base_damage,mp_cost,cooldown_ms,max_level,description,icon_id,unlock_level) VALUES
-- Kiếm Sĩ (class 1)
(101,'Chém Mạnh',1,'active','none',120,10,1500,10,'Đòn chém gây 120% sát thương',1001,1),
(102,'Phòng Thủ',1,'active','none',0,15,8000,10,'Giảm 50% sát thương nhận trong 3s',1002,5),
(103,'Xoáy Kiếm',1,'active','none',150,25,4000,10,'Tấn công tất cả quái xung quanh',1003,10),
(104,'Kiếm Khí',1,'active','none',200,35,6000,10,'Phóng khí kiếm xuyên qua kẻ thù',1004,15),
(105,'Hồi Máu',1,'passive','none',0,0,0,5,'Hồi 2% HP mỗi 5 giây',1005,1),
(106,'Tăng STR',1,'passive','none',0,0,0,5,'Tăng STR +10 mỗi level skill',1006,1),
(107,'Chiến Thần',1,'ultimate','fire',500,80,30000,5,'Bùng cháy 10 giây, ATK +100%',1007,30),
(108,'Đột Kích',1,'active','none',180,20,3000,10,'Lao tới mục tiêu gây 180% sát thương',1008,8),
-- Sát Thủ (class 2)
(201,'Đâm Nhanh',2,'active','none',110,8,1000,10,'Đâm nhanh 110% sát thương',2001,1),
(202,'Tàng Hình',2,'active','none',0,30,15000,5,'Ẩn thân 5 giây, tấn công gây x2',2002,10),
(203,'Ném Dao',2,'active','none',130,15,2000,10,'Ném dao từ xa',2003,5),
(204,'Chất Độc',2,'active','none',80,20,4000,10,'Gây độc liên tục 5 giây',2004,8),
(205,'Tốc Độ',2,'passive','none',0,0,0,5,'Tăng tốc di chuyển 5% mỗi level',2005,1),
(206,'Né Tránh',2,'passive','none',0,0,0,5,'Tăng 3% tỉ lệ né đòn mỗi level',2006,1),
(207,'Sát Thủ Ngầm',2,'ultimate','none',600,100,30000,5,'Ngay lập tức ẩn và tấn công chí mạng',2007,30),
(208,'Xiềng Xích',2,'active','none',0,25,10000,5,'Làm chậm kẻ thù 50% trong 3 giây',2008,12),
-- Pháp Sư (class 3)
(301,'Quả Cầu Lửa',3,'active','fire',150,20,2000,10,'Bắn quả cầu lửa',3001,1),
(302,'Băng Kết',3,'active','ice',130,25,4000,10,'Đóng băng kẻ thù trong 2 giây',3002,5),
(303,'Sét Đánh',3,'active','lightning',170,30,3000,10,'Sét đánh gây 170% sát thương',3003,10),
(304,'Bão Lửa',3,'active','fire',120,40,5000,10,'Bão lửa liên tục trên khu vực',3004,15),
(305,'Trí Tuệ',3,'passive','none',0,0,0,5,'Tăng INT +8 mỗi level skill',3005,1),
(306,'Tiết Kiệm MP',3,'passive','none',0,0,0,5,'Giảm 5% MP cost mỗi level',3006,1),
(307,'Thảm Hoạ Lửa',3,'ultimate','fire',800,120,30000,5,'Thiên thạch rơi gây sát thương khủng',3007,30),
(308,'Hồi MP',3,'active','none',0,0,8000,5,'Hồi 30% MP tối đa',3008,8),
-- Pháp Thủ (class 4)
(401,'Tên Lửa Thần',4,'active','fire',140,18,2500,10,'Bắn tên lửa phép thuật',4001,1),
(402,'Khiên Phép',4,'active','none',0,30,12000,5,'Tạo khiên hấp thụ 500 sát thương',4002,5),
(403,'Phép Hồi',4,'active','none',-200,35,5000,10,'Hồi phục HP nhân vật (âm = heal)',4003,10),
(404,'Phép Buff',4,'active','none',0,40,20000,5,'Tăng ATK+DEF toàn nhóm 20% trong 30s',4004,15),
(405,'Thánh Linh',4,'passive','none',0,0,0,5,'Tăng kháng phép +5% mỗi level',4005,1),
(406,'Tăng VIT',4,'passive','none',0,0,0,5,'Tăng VIT +8 mỗi level skill',4006,1),
(407,'Ngọn Lửa Thánh',4,'ultimate','fire',700,110,30000,5,'Gọi ngọn lửa thánh huỷ diệt',4007,30),
(408,'Dịch Chuyển',4,'active','none',0,20,8000,5,'Dịch chuyển tức thì đến vị trí chỉ định',4008,12),
-- Cung Thủ (class 5)
(501,'Mưa Tên',5,'active','none',100,15,2000,10,'Bắn loạt tên vào một khu vực',5001,1),
(502,'Tên Xuyên Thấu',5,'active','none',160,20,3000,10,'Tên xuyên qua nhiều kẻ thù',5002,5),
(503,'Mắt Thần',5,'active','none',0,10,6000,5,'Phát hiện kẻ thù ẩn trong 10 giây',5003,8),
(504,'Tên Lửa',5,'active','fire',180,25,4000,10,'Bắn tên lửa gây thêm sát thương lửa',5004,10),
(505,'Tăng AGI',5,'passive','none',0,0,0,5,'Tăng AGI +10 mỗi level skill',5005,1),
(506,'Mắt Sắc',5,'passive','none',0,0,0,5,'Tăng crit rate +3% mỗi level',5006,1),
(507,'Bão Tên',5,'ultimate','none',600,100,30000,5,'Bắn hàng trăm mũi tên trong 5 giây',5007,30),
(508,'Bẫy Chông',5,'active','none',120,20,8000,5,'Đặt bẫy gây sát thương kẻ đi qua',5008,12);

-- ─────────────────────────────────────────
-- 26. PVP SYSTEM
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pvp_duels (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenger_id BIGINT NOT NULL,
    defender_id   BIGINT NOT NULL,
    map_id        INT NOT NULL DEFAULT 0,
    status        VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,active,finished,declined
    winner_id     BIGINT DEFAULT NULL,
    started_at    TIMESTAMP NULL,
    ended_at      TIMESTAMP NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pvp_stats (
    char_id  BIGINT PRIMARY KEY,
    wins     INT NOT NULL DEFAULT 0,
    losses   INT NOT NULL DEFAULT 0,
    rating   INT NOT NULL DEFAULT 1000,  -- ELO
    FOREIGN KEY (char_id) REFERENCES characters(id)
);

-- ─────────────────────────────────────────
-- 27. MINIGAME extended config
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS minigame_config (
    game_type    VARCHAR(32) PRIMARY KEY,
    min_bet      INT NOT NULL DEFAULT 100,
    max_bet      INT NOT NULL DEFAULT 1000000,
    house_edge   FLOAT NOT NULL DEFAULT 0.05,  -- 5% house advantage
    is_active    TINYINT NOT NULL DEFAULT 1,
    config_json  TEXT   -- game-specific config
);

INSERT IGNORE INTO minigame_config (game_type,min_bet,max_bet,house_edge,config_json) VALUES
('bau_cua',  100,500000,0.04,'{"dice_count":3,"symbols":["bau","cua","ca","tom","ga","nai"]}'),
('tien_len', 100,200000,0.05,'{"num_cards":13,"jokers":0}'),
('dua_thu',  100,300000,0.06,'{"num_lanes":6,"race_distance":100}'),
('o_an_quan',50, 100000,0.0, '{"holes":10,"stones_per_hole":5}'),
('da_ga',   500,1000000,0.05,'{"rounds":5}'),
('do_vui',  0,  0,       0.0,'{"questions_per_game":10}');

-- ─────────────────────────────────────────
-- 28. FARMING config
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS farm_seeds (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    growth_time_min INT NOT NULL DEFAULT 60, -- phút để trưởng thành
    stages       INT NOT NULL DEFAULT 4,     -- số giai đoạn
    harvest_item_id INT NOT NULL,
    harvest_qty_min INT NOT NULL DEFAULT 1,
    harvest_qty_max INT NOT NULL DEFAULT 5,
    seed_item_id INT NOT NULL,               -- item để trồng
    water_needed INT NOT NULL DEFAULT 3,     -- lần tưới cần thiết
    is_active    TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO farm_seeds (id,name,growth_time_min,stages,harvest_item_id,harvest_qty_min,harvest_qty_max,seed_item_id,water_needed) VALUES
(1,'Lúa Mì',   30,4,101,3,8,201,2),
(2,'Cà Rốt',   60,4,102,2,6,202,3),
(3,'Bí Đỏ',   120,4,103,1,4,203,4),
(4,'Hoa Sen',  240,5,104,1,3,204,5),
(5,'Linh Thảo',480,6,105,1,2,205,6);

CREATE TABLE IF NOT EXISTS farm_animals (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    animal_type  VARCHAR(16) NOT NULL,  -- fish,bird,pig,cow
    feed_item_id INT NOT NULL,
    produce_item_id INT NOT NULL,
    produce_time_min INT NOT NULL DEFAULT 120,
    produce_qty  INT NOT NULL DEFAULT 1,
    is_active    TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO farm_animals (id,name,animal_type,feed_item_id,produce_item_id,produce_time_min,produce_qty) VALUES
(1,'Gà Vàng','bird',301,401,60,3),
(2,'Lợn Hồng','pig',302,402,120,1),
(3,'Cá Chép','fish',303,403,30,5),
(4,'Bò Sữa','cow',304,404,180,2);

-- ─────────────────────────────────────────
-- 29. HOUSING catalog
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS furniture_catalog (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(64) NOT NULL,
    furniture_type VARCHAR(32) NOT NULL, -- bed,table,chair,decoration,storage
    width        INT NOT NULL DEFAULT 1,
    height       INT NOT NULL DEFAULT 1,
    gold_price   INT NOT NULL DEFAULT 0,
    diamond_price INT NOT NULL DEFAULT 0,
    stat_bonus   VARCHAR(256),  -- JSON bonus khi đặt trong nhà
    icon_id      INT NOT NULL DEFAULT 0,
    is_active    TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,width,height,gold_price,diamond_price) VALUES
(1,'Giường Đơn','bed',2,1,5000,0),(2,'Giường Đôi','bed',3,2,15000,0),
(3,'Bàn Gỗ','table',1,1,2000,0),(4,'Ghế Gỗ','chair',1,1,1000,0),
(5,'Tủ Sách','storage',1,2,8000,0),(6,'Chậu Hoa','decoration',1,1,3000,0),
(7,'Tranh Phong Cảnh','decoration',2,1,10000,50),(8,'Đèn Lồng','decoration',1,1,5000,0),
(9,'Bếp Lửa','decoration',1,1,12000,0),(10,'Bể Cá','decoration',2,1,0,100);

-- ─────────────────────────────────────────
-- 30. LEADERBOARD
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leaderboard_cache (
    rank_type    VARCHAR(32) NOT NULL,   -- level,pvp_rating,wealth,guild_level
    rank_pos     INT NOT NULL,
    char_id      BIGINT NOT NULL,
    char_name    VARCHAR(32) NOT NULL,
    class_id     INT NOT NULL DEFAULT 1,
    gender       TINYINT NOT NULL DEFAULT 0,
    rank_value   BIGINT NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (rank_type, rank_pos)
);
ALTER TABLE characters ADD COLUMN IF NOT EXISTS server_id INT NOT NULL DEFAULT 1;

-- ─────────────────────────────────────────
-- 24. EVENT CURRENCY — tien te su kien phu
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS event_currencies (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    event_id        INT NOT NULL DEFAULT 0,      -- 0 = thuong truc, >0 = gan su kien cu the
    currency_code   VARCHAR(32) NOT NULL UNIQUE,  -- VD: "hoa_sen", "sao_bang", "la_phong"
    display_name    VARCHAR(64) NOT NULL,          -- VD: "Hoa Sen", "Sao Bang"
    icon_asset      VARCHAR(128) NOT NULL DEFAULT 'Icons/Currency/default',
    description     VARCHAR(256) DEFAULT '',
    exchange_rate_gold    INT NOT NULL DEFAULT 0,  -- 1 token = ? gold (0 = khong doi duoc)
    exchange_rate_diamond INT NOT NULL DEFAULT 0,  -- 1 token = ? diamond
    is_active       TINYINT NOT NULL DEFAULT 1,
    expires_at      DATETIME DEFAULT NULL,         -- NULL = khong het han
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active (is_active, expires_at)
);

CREATE TABLE IF NOT EXISTS player_event_currencies (
    char_id         BIGINT NOT NULL,
    currency_id     INT NOT NULL,
    amount          INT NOT NULL DEFAULT 0,
    last_earned_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (char_id, currency_id),
    FOREIGN KEY (currency_id) REFERENCES event_currencies(id) ON DELETE CASCADE
);

-- Event currency shop (item mua bang event token)
CREATE TABLE IF NOT EXISTS event_currency_shop (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    currency_id     INT NOT NULL,
    item_id         INT NOT NULL,
    item_name       VARCHAR(64) NOT NULL,
    price           INT NOT NULL,
    stock           INT NOT NULL DEFAULT -1,   -- -1 = vo han
    per_user_limit  INT NOT NULL DEFAULT 0,    -- 0 = khong gioi han
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    FOREIGN KEY (currency_id) REFERENCES event_currencies(id)
);

-- Log giao dich event currency
CREATE TABLE IF NOT EXISTS event_currency_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id         BIGINT NOT NULL,
    currency_id     INT NOT NULL,
    amount          INT NOT NULL,       -- + = nhan, - = tieu
    reason          VARCHAR(128) NOT NULL, -- 'quest_reward','shop_buy','exchange','admin_grant','event_drop'
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_char_cur (char_id, currency_id)
);

-- Seed VD
INSERT IGNORE INTO event_currencies (id,currency_code,display_name,icon_asset,description,exchange_rate_gold,is_active)
VALUES
(1,'hoa_sen','Hoa Sen','Icons/Currency/hoa_sen','Tien te su kien Tet Nguyen Dan',100,0),
(2,'sao_bang','Sao Bang','Icons/Currency/sao_bang','Tien te su kien He',50,0),
(3,'la_phong','La Phong','Icons/Currency/la_phong','Tien te su kien Thu',80,0);

-- ═════════════════════════════════════════════════════════════
-- 25. MASTER ITEM REGISTRY — Kho tong, moi thu trong game
-- ═════════════════════════════════════════════════════════════

-- Phan loai chinh cua moi "thing" trong game
-- Dung cho admin: khi can chon item (event, pass, giftcode) chi can nhap ID
CREATE TABLE IF NOT EXISTS master_registry (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    registry_type   VARCHAR(32) NOT NULL,   -- 'item','skin','pet','mount','title','map','event_currency','sticker_pack','furniture','seed','animal'
    ref_id          INT NOT NULL,           -- ID trong bang goc (items.id, pet_templates.id,...)
    display_name    VARCHAR(128) NOT NULL,
    category        VARCHAR(64) NOT NULL DEFAULT 'general', -- 'weapon','armor','consumable','material','cosmetic','mount','pet','title','map','currency','sticker','furniture','seed','animal'
    sub_category    VARCHAR(64) DEFAULT '',  -- 'sword','staff','hp_potion','cape_skin',...
    rarity          TINYINT NOT NULL DEFAULT 0,  -- 0=common,1=uncommon,2=rare,3=epic,4=legendary,5=mythic
    icon_asset      VARCHAR(256) DEFAULT 'Icons/default',
    description     TEXT,
    tags            VARCHAR(256) DEFAULT '', -- 'event_tet,limited,tradeable,stackable'
    is_tradeable    TINYINT NOT NULL DEFAULT 1,
    is_stackable    TINYINT NOT NULL DEFAULT 0,
    max_stack       INT NOT NULL DEFAULT 1,
    is_active       TINYINT NOT NULL DEFAULT 1,
    added_by        VARCHAR(64) DEFAULT 'system',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (registry_type),
    INDEX idx_category (category, sub_category),
    INDEX idx_rarity (rarity),
    INDEX idx_tags (tags),
    UNIQUE KEY uk_type_ref (registry_type, ref_id)
);

-- Populate tu cac bang hien co (chay 1 lan khi setup)
-- INSERT IGNORE INTO master_registry (registry_type,ref_id,display_name,category,rarity)
--   SELECT 'item', id, name, CASE type WHEN 0 THEN 'weapon' ... END, rarity FROM items;

-- ═════════════════════════════════════════════════════════════
-- 26. TRADING — Giao dich giua player
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS trade_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_a_id     BIGINT NOT NULL,
    player_b_id     BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending,confirmed_a,confirmed_b,completed,cancelled
    gold_a          BIGINT NOT NULL DEFAULT 0,  -- vang player A dua
    gold_b          BIGINT NOT NULL DEFAULT 0,  -- vang player B dua
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS trade_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id        BIGINT NOT NULL,
    from_char_id    BIGINT NOT NULL,  -- nguoi dua item
    inventory_id    BIGINT NOT NULL,  -- character_inventory.id
    item_id         INT NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    FOREIGN KEY (trade_id) REFERENCES trade_sessions(id) ON DELETE CASCADE
);

-- ═════════════════════════════════════════════════════════════
-- 27. AUCTION HOUSE — Nha dau gia
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS auction_listings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_char_id  BIGINT NOT NULL,
    seller_name     VARCHAR(32) NOT NULL,
    inventory_id    BIGINT NOT NULL,       -- item dang ban
    item_id         INT NOT NULL,
    item_name       VARCHAR(64) NOT NULL,
    qty             INT NOT NULL DEFAULT 1,
    enhance_level   INT NOT NULL DEFAULT 0,
    rarity          INT NOT NULL DEFAULT 0,
    start_price     BIGINT NOT NULL,       -- gia khoi diem
    buyout_price    BIGINT DEFAULT NULL,   -- gia mua ngay (NULL = ko co)
    current_bid     BIGINT NOT NULL DEFAULT 0,
    bidder_char_id  BIGINT DEFAULT NULL,   -- nguoi dau gia cao nhat hien tai
    bidder_name     VARCHAR(32) DEFAULT NULL,
    currency        TINYINT NOT NULL DEFAULT 0, -- 0=gold,1=diamond
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,sold,expired,cancelled
    expires_at      DATETIME NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_expires (status, expires_at),
    INDEX idx_item (item_id, rarity),
    INDEX idx_seller (seller_char_id)
);

CREATE TABLE IF NOT EXISTS auction_bids (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id      BIGINT NOT NULL,
    bidder_char_id  BIGINT NOT NULL,
    amount          BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES auction_listings(id)
);

-- Admin config cho auction
CREATE TABLE IF NOT EXISTS auction_config (
    config_key      VARCHAR(64) PRIMARY KEY,
    config_value    VARCHAR(256) NOT NULL,
    description     VARCHAR(256) DEFAULT ''
);
INSERT IGNORE INTO auction_config VALUES
('tax_rate','5','Thue ban hang (%)'),
('max_duration_hours','48','Thoi gian dang toi da (gio)'),
('min_price','100','Gia toi thieu'),
('max_listings_per_player','20','So luong dang ban toi da/player'),
('is_enabled','1','Bat/tat auction house');

-- ═════════════════════════════════════════════════════════════
-- 28. PARTY — Nhom chien dau
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS parties (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    leader_char_id  BIGINT NOT NULL,
    name            VARCHAR(32) DEFAULT '',
    max_members     INT NOT NULL DEFAULT 4,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,disbanded,in_dungeon
    dungeon_id      INT DEFAULT NULL,         -- NULL = khong trong dungeon
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS party_members (
    party_id        BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    role            TINYINT NOT NULL DEFAULT 0, -- 0=member,1=leader
    joined_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (party_id, char_id),
    FOREIGN KEY (party_id) REFERENCES parties(id) ON DELETE CASCADE
);

-- ═════════════════════════════════════════════════════════════
-- 29. DUNGEON — Instance dungeon
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS dungeon_templates (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     TEXT,
    min_level       INT NOT NULL DEFAULT 1,
    max_players     INT NOT NULL DEFAULT 4,
    map_id          INT NOT NULL,              -- map rieng cho dungeon
    boss_monster_id INT DEFAULT NULL,          -- boss cuoi
    reward_exp      INT NOT NULL DEFAULT 0,
    reward_gold     INT NOT NULL DEFAULT 0,
    reward_items    TEXT,                       -- JSON: [{"item_id":1,"qty":1,"drop_rate":0.5}]
    cooldown_minutes INT NOT NULL DEFAULT 60,  -- thoi gian cho giua cac lan vao
    time_limit_minutes INT NOT NULL DEFAULT 30,
    difficulty      TINYINT NOT NULL DEFAULT 1, -- 1=normal,2=hard,3=hell
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dungeon_instances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id     INT NOT NULL,
    party_id        BIGINT NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'active', -- active,completed,failed,expired
    started_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at        DATETIME DEFAULT NULL,
    boss_killed     TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (template_id) REFERENCES dungeon_templates(id)
);

CREATE TABLE IF NOT EXISTS dungeon_cooldowns (
    char_id         BIGINT NOT NULL,
    template_id     INT NOT NULL,
    next_entry_at   DATETIME NOT NULL,
    PRIMARY KEY (char_id, template_id)
);

-- Seed dungeon templates
INSERT IGNORE INTO dungeon_templates (id,name,min_level,max_players,map_id,reward_exp,reward_gold,difficulty) VALUES
(1,'Hang Doi Am U',10,4,101,500,2000,1),
(2,'Lau Dai Bi Mat',25,4,102,2000,8000,1),
(3,'Than Dien Co Dai',40,4,103,5000,20000,2),
(4,'Dia Nguc Tang 1',60,4,104,15000,50000,3);

-- ═════════════════════════════════════════════════════════════
-- 30. NPC DIALOG — Hoi thoai NPC (cay hoi thoai)
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS npc_dialogs (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    npc_id          INT NOT NULL,
    dialog_key      VARCHAR(64) NOT NULL,      -- 'greeting','quest_1_start','shop_intro'
    text            TEXT NOT NULL,              -- Noi dung hoi thoai
    speaker         VARCHAR(32) DEFAULT '',     -- Ten nguoi noi (NPC name, hoac 'Player')
    next_dialog_id  INT DEFAULT NULL,           -- Dialog tiep theo (NULL = ket thuc)
    options         TEXT DEFAULT NULL,           -- JSON: [{"text":"Chap nhan","goto":5},{"text":"Tu choi","goto":null}]
    condition       TEXT DEFAULT NULL,           -- JSON: {"min_level":10,"has_item":5,"quest_completed":2}
    action          TEXT DEFAULT NULL,           -- JSON: {"give_item":1,"start_quest":3,"open_shop":2}
    sort_order      INT NOT NULL DEFAULT 0,
    FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE,
    INDEX idx_npc_key (npc_id, dialog_key)
);

-- ═════════════════════════════════════════════════════════════
-- 31. SYSTEM ANNOUNCEMENTS — Thong bao he thong (sticky)
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS system_announcements (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    content         TEXT NOT NULL,
    announce_type   VARCHAR(32) NOT NULL DEFAULT 'info', -- info,warning,event,maintenance,achievement
    priority        INT NOT NULL DEFAULT 0,    -- 0=normal,1=important,2=critical (sticky)
    is_sticky       TINYINT NOT NULL DEFAULT 0, -- 1 = luon hien tren dau
    target          VARCHAR(32) DEFAULT 'all', -- 'all','server_1','guild_123','char_456'
    start_at        DATETIME DEFAULT NULL,     -- NULL = ngay lap tuc
    expires_at      DATETIME DEFAULT NULL,     -- NULL = khong het han
    created_by      VARCHAR(64) DEFAULT 'admin',
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_active_priority (is_active, priority DESC, created_at DESC)
);

-- Log su kien quan trong (tu dong insert boi server)
CREATE TABLE IF NOT EXISTS system_event_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(32) NOT NULL,   -- 'leaderboard_top','boss_kill','wedding','level_cap','first_clear','guild_war'
    char_id         BIGINT DEFAULT NULL,
    char_name       VARCHAR(32) DEFAULT '',
    message         TEXT NOT NULL,
    data_json       TEXT DEFAULT NULL,       -- extra data
    server_id       INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_time (event_type, created_at DESC)
);

-- Seed announcements
INSERT IGNORE INTO system_announcements (id,title,content,announce_type,priority,is_sticky) VALUES
(1,'Chao mung den Nexus Isekai!','Chuc cac Luu Dan co nhung trai nghiem tuyet voi tai Vong Linh Gioi.','info',0,1),
(2,'Luu y bao mat','Khong chia se mat khau voi bat ky ai. Admin khong bao gio hoi mat khau.','warning',1,1);

-- ═════════════════════════════════════════════════════════════
-- 32. RATE LIMITING — Gioi han tan suat goi
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS rate_limit_config (
    config_key      VARCHAR(64) PRIMARY KEY,
    max_per_second  INT NOT NULL DEFAULT 10,
    max_per_minute  INT NOT NULL DEFAULT 100,
    description     VARCHAR(256) DEFAULT ''
);
INSERT IGNORE INTO rate_limit_config VALUES
('chat',5,60,'Gioi han chat'),
('move',20,600,'Gioi han di chuyen'),
('attack',10,200,'Gioi han tan cong'),
('trade',3,30,'Gioi han giao dich'),
('shop',5,60,'Gioi han mua ban');

-- Guild invites table (da tham chieu trong GuildHandler)
CREATE TABLE IF NOT EXISTS guild_invites (
    guild_id        BIGINT NOT NULL,
    char_id         BIGINT NOT NULL,
    invited_by      BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (guild_id, char_id)
);

-- PvP duels table
CREATE TABLE IF NOT EXISTS pvp_duels (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    char_id_a       BIGINT NOT NULL,
    char_id_b       BIGINT NOT NULL,
    winner_char_id  BIGINT DEFAULT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    map_id          INT DEFAULT NULL,
    elo_change      INT DEFAULT 0,
    started_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at        DATETIME DEFAULT NULL
);

-- Farm seeds/animals config
CREATE TABLE IF NOT EXISTS farm_seeds (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    seed_name       VARCHAR(64) NOT NULL,
    grow_time_min   INT NOT NULL DEFAULT 60,
    harvest_item_id INT NOT NULL,
    harvest_qty     INT NOT NULL DEFAULT 1,
    buy_price       INT NOT NULL DEFAULT 100,
    exp_reward      INT NOT NULL DEFAULT 10,
    is_active       TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS farm_animals (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    animal_name     VARCHAR(64) NOT NULL,
    feed_item_id    INT NOT NULL DEFAULT 0,
    product_item_id INT NOT NULL,
    produce_time_min INT NOT NULL DEFAULT 120,
    buy_price       INT NOT NULL DEFAULT 500,
    is_active       TINYINT NOT NULL DEFAULT 1
);

-- Furniture catalog
CREATE TABLE IF NOT EXISTS furniture_catalog (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    furniture_type  VARCHAR(32) NOT NULL DEFAULT 'decoration',
    price_gold      INT NOT NULL DEFAULT 1000,
    size            INT NOT NULL DEFAULT 1,
    icon_id         INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO farm_seeds (id,seed_name,grow_time_min,harvest_item_id,harvest_qty,buy_price) VALUES
(1,'Hat Lua',30,1001,5,50),(2,'Hat Ngo',45,1002,3,80),(3,'Hat Ca Chua',60,1003,4,100);

INSERT IGNORE INTO farm_animals (id,animal_name,product_item_id,produce_time_min,buy_price) VALUES
(1,'Ga',1010,60,200),(2,'Bo',1011,180,500),(3,'Ca',1012,90,300);

INSERT IGNORE INTO furniture_catalog (id,name,furniture_type,price_gold,size) VALUES
(1,'Ban Go','table',500,2),(2,'Giuong','bed',1000,4),(3,'Tu Quan Ao','wardrobe',800,2),
(4,'Den Lung','lamp',200,1),(5,'Cay Canh','plant',150,1);

-- ═════════════════════════════════════════════════════════════
-- 33. OTA ASSET UPDATE — Hệ thống cập nhật client qua mạng
-- ═════════════════════════════════════════════════════════════

-- Mỗi asset (ảnh, config, data) được track version + hash
CREATE TABLE IF NOT EXISTS client_assets (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    asset_key       VARCHAR(256) NOT NULL UNIQUE,  -- VD: "Sprites/Items/item_001.png", "Config/skills.json"
    asset_type      VARCHAR(32) NOT NULL,           -- image,config,audio,data,sprite_atlas,hud,icon,map_tile
    category        VARCHAR(64) NOT NULL DEFAULT 'general', -- items,monsters,npcs,hud,icons,maps,effects,ui,audio,config
    file_path       VARCHAR(512) NOT NULL,          -- đường dẫn thực trên server
    file_size       INT NOT NULL DEFAULT 0,         -- bytes
    hash_md5        VARCHAR(32) NOT NULL DEFAULT '', -- MD5 hash để client so sánh
    version         INT NOT NULL DEFAULT 1,         -- tăng mỗi lần upload mới
    display_name    VARCHAR(128) DEFAULT '',
    description     VARCHAR(256) DEFAULT '',
    mime_type       VARCHAR(64) DEFAULT 'image/png',
    width           INT DEFAULT 0,                  -- cho ảnh
    height          INT DEFAULT 0,
    is_required     TINYINT NOT NULL DEFAULT 1,     -- 1=phải tải, 0=optional
    is_active       TINYINT NOT NULL DEFAULT 1,
    uploaded_by     VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type_cat (asset_type, category),
    INDEX idx_version (version)
);

-- Bundle: nhóm asset để cập nhật đồng thời
CREATE TABLE IF NOT EXISTS asset_bundles (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    bundle_name     VARCHAR(128) NOT NULL,          -- "HUD Pack v2", "Monster Sprites v3"
    description     TEXT,
    version         INT NOT NULL DEFAULT 1,
    total_size      BIGINT NOT NULL DEFAULT 0,      -- tổng kích thước
    asset_count     INT NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft,published,archived
    published_at    DATETIME DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS asset_bundle_items (
    bundle_id       INT NOT NULL,
    asset_id        INT NOT NULL,
    PRIMARY KEY (bundle_id, asset_id),
    FOREIGN KEY (bundle_id) REFERENCES asset_bundles(id) ON DELETE CASCADE,
    FOREIGN KEY (asset_id) REFERENCES client_assets(id) ON DELETE CASCADE
);

-- Client version tracking
CREATE TABLE IF NOT EXISTS client_versions (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    platform        VARCHAR(16) NOT NULL,           -- android,ios,pc,webgl,j2me
    version_code    INT NOT NULL,                   -- 1,2,3... (tăng dần)
    version_name    VARCHAR(32) NOT NULL,            -- "1.0.0", "1.1.0"
    min_asset_version INT NOT NULL DEFAULT 1,       -- asset version tối thiểu cần có
    download_url    VARCHAR(512) DEFAULT '',         -- link tải APK/IPA/JAR mới
    release_notes   TEXT,
    is_force_update TINYINT NOT NULL DEFAULT 0,     -- 1=bắt buộc cập nhật
    is_latest       TINYINT NOT NULL DEFAULT 1,
    published_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_platform (platform, is_latest)
);

INSERT IGNORE INTO client_versions (id,platform,version_code,version_name,download_url) VALUES
(1,'android',1,'1.0.0','/download/NexusIsekai.apk'),
(2,'ios',1,'1.0.0',''),
(3,'pc',1,'1.0.0','/download/NexusIsekai-PC.jar'),
(4,'webgl',1,'1.0.0','/play');

-- Config hot-reload (key-value, client poll định kỳ)
CREATE TABLE IF NOT EXISTS hot_config (
    config_key      VARCHAR(128) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    config_type     VARCHAR(16) NOT NULL DEFAULT 'string', -- string,int,float,json,bool
    category        VARCHAR(64) NOT NULL DEFAULT 'game',
    description     VARCHAR(256) DEFAULT '',
    version         INT NOT NULL DEFAULT 1,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO hot_config VALUES
('server_notice','','string','system','Thông báo trên màn hình login',1,NOW()),
('maintenance_mode','false','bool','system','Chế độ bảo trì',1,NOW()),
('double_exp_active','false','bool','event','Double EXP đang bật',1,NOW()),
('max_enhance_level','10','int','game','Cấp cường hoá tối đa',1,NOW()),
('pvp_enabled','true','bool','game','PvP có bật không',1,NOW()),
('auction_enabled','true','bool','game','Nhà đấu giá có bật không',1,NOW()),
('trade_enabled','true','bool','game','Giao dịch có bật không',1,NOW());

-- ═════════════════════════════════════════════════════════════
-- 34. STORY EDITOR — Quản lý cốt truyện + AI
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS story_chapters (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    chapter_order   INT NOT NULL DEFAULT 0,
    title           VARCHAR(128) NOT NULL,
    synopsis        TEXT,                           -- tóm tắt
    full_text       LONGTEXT,                       -- nội dung đầy đủ
    region_id       INT DEFAULT NULL,               -- vùng đất liên quan
    min_level       INT NOT NULL DEFAULT 1,
    max_level       INT NOT NULL DEFAULT 99,
    prev_chapter_id INT DEFAULT NULL,
    next_chapter_id INT DEFAULT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft', -- draft,published,archived
    cutscene_data   TEXT,                           -- JSON: cấu hình cutscene
    ai_generated    TINYINT NOT NULL DEFAULT 0,     -- 1=được AI tạo
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS story_quest_links (
    chapter_id      INT NOT NULL,
    quest_id        INT NOT NULL,
    quest_order     INT NOT NULL DEFAULT 0,         -- thứ tự quest trong chapter
    PRIMARY KEY (chapter_id, quest_id),
    FOREIGN KEY (chapter_id) REFERENCES story_chapters(id) ON DELETE CASCADE
);

-- AI generation log
CREATE TABLE IF NOT EXISTS ai_generation_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    gen_type        VARCHAR(32) NOT NULL,            -- quest,dialog,story,item_desc,event_desc,announcement
    prompt          TEXT NOT NULL,
    result          LONGTEXT,
    model           VARCHAR(64) DEFAULT 'claude-sonnet-4-20250514',
    tokens_used     INT DEFAULT 0,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed story chapters
INSERT IGNORE INTO story_chapters (id,chapter_order,title,synopsis,min_level,status) VALUES
(1,1,'Thức Tỉnh','Lưu Dân tỉnh dậy tại Làng Khải Nguyên, không nhớ gì về quá khứ. Trưởng làng hướng dẫn những bước đầu tiên.',1,'published'),
(2,2,'Sương Mù Bí Ẩn','Tin đồn về quái vật xuất hiện ở Đồng Bằng Sương Mù. Lưu Dân nhận nhiệm vụ điều tra.',10,'published'),
(3,3,'Rừng Ám Ảnh','Ma khí nhiễm vào khu rừng phía Bắc. Cần tìm nguồn gốc sức mạnh tà ác.',25,'published'),
(4,4,'Liên Minh Thiên Quang','Đến thủ phủ, gia nhập liên minh. Tiết lộ về Giáo Phái Vọng Linh.',35,'draft'),
(5,5,'Phong Ấn Rung Chuyển','Phong ấn Azaroth bắt đầu yếu đi. Cuộc đua với thời gian.',50,'draft');
