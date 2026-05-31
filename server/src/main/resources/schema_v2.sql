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

-- ═════════════════════════════════════════════════════════════
-- 35. AI REVIEW WORKFLOW — Duyệt nội dung AI trước khi lên production
-- ═════════════════════════════════════════════════════════════

ALTER TABLE ai_generation_log
    ADD COLUMN IF NOT EXISTS status        VARCHAR(16) NOT NULL DEFAULT 'draft',   -- draft,review,testing,approved,published,rejected
    ADD COLUMN IF NOT EXISTS reviewed_by   VARCHAR(64) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS reviewed_at   DATETIME DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS test_server   VARCHAR(32) DEFAULT NULL,               -- 'test_1','staging'
    ADD COLUMN IF NOT EXISTS applied_to    VARCHAR(256) DEFAULT NULL,              -- JSON: {"quest_id":5} hoặc {"dialog_id":12}
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(256) DEFAULT NULL;

-- ═════════════════════════════════════════════════════════════
-- 36. ADMIN AUDIT LOG — Ai làm gì trong admin
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user      VARCHAR(64) NOT NULL,
    action          VARCHAR(64) NOT NULL,       -- 'create_item','ban_player','approve_ai','upload_asset','grant_diamond'
    target_type     VARCHAR(32) DEFAULT '',      -- 'player','item','quest','announcement','config'
    target_id       VARCHAR(64) DEFAULT '',      -- ID của đối tượng bị ảnh hưởng
    details         TEXT,                         -- JSON chi tiết thay đổi
    ip_address      VARCHAR(45) DEFAULT '',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin (admin_user, created_at DESC),
    INDEX idx_action (action, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 37. PLAYER MAIL — Hệ thống thư trong game
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS player_mail (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id    BIGINT NOT NULL,            -- char_id người nhận
    sender_type     VARCHAR(16) NOT NULL DEFAULT 'system', -- system,admin,player,event
    sender_name     VARCHAR(64) NOT NULL DEFAULT 'He Thong',
    title           VARCHAR(128) NOT NULL,
    content         TEXT NOT NULL,
    attachment_json TEXT DEFAULT NULL,           -- JSON: [{"item_id":5,"qty":2},{"gold":1000}]
    is_read         TINYINT NOT NULL DEFAULT 0,
    is_claimed      TINYINT NOT NULL DEFAULT 0, -- đã nhận vật phẩm đính kèm chưa
    expires_at      DATETIME DEFAULT NULL,       -- NULL = không hết hạn
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recipient (recipient_id, is_read, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 38. ADMIN ROLES — Phân quyền admin
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS admin_accounts (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,       -- BCrypt
    display_name    VARCHAR(64) NOT NULL,
    role            VARCHAR(32) NOT NULL DEFAULT 'gm', -- super_admin,admin,gm,support,content_editor,viewer
    permissions     TEXT,                         -- JSON: ["players","items","quests","story","ai","assets","ban","mail"]
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_login      DATETIME DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO admin_accounts (id,username,password_hash,display_name,role,permissions) VALUES
(1,'admin','$2a$10$placeholder','Super Admin','super_admin','["*"]');

-- ═════════════════════════════════════════════════════════════
-- 39. SCHEDULED TASKS — Lịch sự kiện tự động
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    task_name       VARCHAR(64) NOT NULL,
    task_type       VARCHAR(32) NOT NULL,         -- 'double_exp','boss_spawn','maintenance','event_start','event_end','mail_blast','backup'
    cron_expression VARCHAR(64) DEFAULT NULL,     -- '0 20 * * 5' (thứ 6 lúc 20h)
    run_once_at     DATETIME DEFAULT NULL,        -- chạy 1 lần tại thời điểm này
    parameters      TEXT,                          -- JSON: {"exp_multiplier":2,"duration_hours":4}
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_run_at     DATETIME DEFAULT NULL,
    next_run_at     DATETIME DEFAULT NULL,
    created_by      VARCHAR(64) DEFAULT 'admin',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO scheduled_tasks (id,task_name,task_type,cron_expression,parameters) VALUES
(1,'Double EXP Cuoi Tuan','double_exp','0 20 * * 5','{"multiplier":2,"duration_hours":48}'),
(2,'Boss Spawn Hang Ngay','boss_spawn','0 12 * * *','{"boss_id":99,"map_id":5}'),
(3,'Backup DB','backup','0 3 * * *','{"keep_days":7}');

-- ═════════════════════════════════════════════════════════════
-- 40. PLAYER REPORTS — Hệ thống báo cáo / khiếu nại
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS player_reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id     BIGINT NOT NULL,             -- người báo cáo
    reporter_name   VARCHAR(32) NOT NULL,
    reported_id     BIGINT DEFAULT NULL,         -- người bị báo cáo (NULL = báo lỗi chung)
    reported_name   VARCHAR(32) DEFAULT '',
    report_type     VARCHAR(32) NOT NULL,         -- 'cheat','harassment','bug','suggestion','other'
    description     TEXT NOT NULL,
    evidence        TEXT DEFAULT NULL,            -- URL screenshot hoặc chat log
    status          VARCHAR(16) NOT NULL DEFAULT 'open', -- open,investigating,resolved,dismissed
    assigned_to     VARCHAR(64) DEFAULT NULL,     -- admin đang xử lý
    resolution      TEXT DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME DEFAULT NULL,
    INDEX idx_status (status, created_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 41. SERVER STATS — Thống kê server
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS server_stats_hourly (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recorded_at     DATETIME NOT NULL,
    online_count    INT NOT NULL DEFAULT 0,
    new_registrations INT NOT NULL DEFAULT 0,
    new_characters  INT NOT NULL DEFAULT 0,
    total_revenue   BIGINT NOT NULL DEFAULT 0,    -- diamond bán được
    gold_created    BIGINT NOT NULL DEFAULT 0,    -- gold sinh ra từ monster/quest
    gold_destroyed  BIGINT NOT NULL DEFAULT 0,    -- gold tiêu (shop, enhance, tax)
    trades_count    INT NOT NULL DEFAULT 0,
    pvp_matches     INT NOT NULL DEFAULT 0,
    dungeons_cleared INT NOT NULL DEFAULT 0,
    peak_online     INT NOT NULL DEFAULT 0,
    INDEX idx_time (recorded_at DESC)
);

-- ═════════════════════════════════════════════════════════════
-- 42. ACHIEVEMENT SYSTEM — Thành tựu
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS achievements (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     VARCHAR(256) NOT NULL,
    category        VARCHAR(32) NOT NULL DEFAULT 'general', -- combat,social,economy,exploration,collection,event
    icon_asset      VARCHAR(128) DEFAULT 'Icons/Achievement/default',
    condition_type  VARCHAR(32) NOT NULL,  -- 'kill_monster','reach_level','collect_item','complete_quest','win_pvp','gold_earned','login_days','enhance_success','join_guild','get_married','clear_dungeon'
    condition_value INT NOT NULL DEFAULT 1, -- số lượng cần đạt
    reward_type     VARCHAR(16) DEFAULT 'title',  -- title,item,gold,diamond,exp
    reward_id       INT DEFAULT 0,         -- title_id hoặc item_id
    reward_amount   INT DEFAULT 0,
    points          INT NOT NULL DEFAULT 10, -- điểm thành tựu
    is_hidden       TINYINT NOT NULL DEFAULT 0, -- 1=ẩn cho đến khi hoàn thành
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_achievements (
    char_id         BIGINT NOT NULL,
    achievement_id  INT NOT NULL,
    progress        INT NOT NULL DEFAULT 0,
    completed       TINYINT NOT NULL DEFAULT 0,
    claimed         TINYINT NOT NULL DEFAULT 0,
    completed_at    DATETIME DEFAULT NULL,
    PRIMARY KEY (char_id, achievement_id),
    FOREIGN KEY (achievement_id) REFERENCES achievements(id)
);

INSERT IGNORE INTO achievements (id,name,description,category,condition_type,condition_value,reward_type,reward_amount,points) VALUES
(1,'Buoc Dau Tien','Dat cap 10','combat','reach_level',10,'exp',500,10),
(2,'Chien Binh','Dat cap 30','combat','reach_level',30,'gold',5000,20),
(3,'Anh Hung','Dat cap 50','combat','reach_level',50,'diamond',50,50),
(4,'Truyen Thuyet','Dat cap 99','combat','reach_level',99,'diamond',200,100),
(5,'Sat Thu Quai Vat','Tieu diet 100 quai vat','combat','kill_monster',100,'gold',1000,10),
(6,'Diet Long','Tieu diet 1000 quai vat','combat','kill_monster',1000,'diamond',20,30),
(7,'Nhiem Vu Gia','Hoan thanh 10 nhiem vu','exploration','complete_quest',10,'exp',1000,10),
(8,'Nguoi Kham Pha','Hoan thanh 50 nhiem vu','exploration','complete_quest',50,'gold',5000,25),
(9,'Dai Gia','So huu 100.000 vang','economy','gold_earned',100000,'diamond',10,15),
(10,'Trieu Phu','So huu 1.000.000 vang','economy','gold_earned',1000000,'diamond',50,30),
(11,'Cuong Hoa +5','Cuong hoa thanh cong +5','collection','enhance_success',5,'gold',2000,15),
(12,'Cuong Hoa +10','Cuong hoa thanh cong +10','collection','enhance_success',10,'diamond',100,50),
(13,'PvP Tan Thu','Thang 10 tran PvP','combat','win_pvp',10,'gold',2000,10),
(14,'Vo Dich','Thang 100 tran PvP','combat','win_pvp',100,'diamond',50,40),
(15,'Tham Hiem Gia','Xoa dungeon 10 lan','exploration','clear_dungeon',10,'gold',3000,15),
(16,'Ket Hon','Ket hon voi nguoi choi khac','social','get_married',1,'diamond',20,20),
(17,'Hoi Vien','Gia nhap guild','social','join_guild',1,'gold',500,5),
(18,'Trung Thanh','Dang nhap 7 ngay lien tiep','social','login_days',7,'diamond',10,10),
(19,'Cong Dan','Dang nhap 30 ngay lien tiep','social','login_days',30,'diamond',30,25),
(20,'Nha Thu Thap','So huu 50 vat pham khac nhau','collection','collect_item',50,'gold',5000,20);

-- ═════════════════════════════════════════════════════════════
-- 43. DAILY LOGIN REWARDS — Phần thưởng đăng nhập hàng ngày
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS daily_login_rewards (
    day_number      INT NOT NULL PRIMARY KEY,  -- 1-7 (lặp lại)
    reward_type     VARCHAR(16) NOT NULL,       -- gold,diamond,item,exp,event_currency
    reward_id       INT NOT NULL DEFAULT 0,     -- item_id nếu type=item
    reward_amount   INT NOT NULL DEFAULT 0,
    bonus_streak    TINYINT NOT NULL DEFAULT 0,  -- 1=thưởng thêm cho streak đầy đủ
    description     VARCHAR(128) DEFAULT ''
);

CREATE TABLE IF NOT EXISTS player_daily_login (
    char_id         BIGINT NOT NULL PRIMARY KEY,
    current_day     INT NOT NULL DEFAULT 0,     -- ngày hiện tại trong chu kỳ (1-7)
    streak_count    INT NOT NULL DEFAULT 0,     -- số ngày liên tiếp
    last_login_date DATE DEFAULT NULL,          -- ngày đăng nhập gần nhất
    total_logins    INT NOT NULL DEFAULT 0,     -- tổng số ngày đã đăng nhập
    claimed_today   TINYINT NOT NULL DEFAULT 0  -- đã nhận thưởng hôm nay chưa
);

INSERT IGNORE INTO daily_login_rewards VALUES
(1,'gold',0,1000,0,'1.000 Vang'),
(2,'exp',0,500,0,'500 EXP'),
(3,'gold',0,2000,0,'2.000 Vang'),
(4,'item',1001,3,0,'3x Thuoc Hoi Mau'),
(5,'gold',0,3000,0,'3.000 Vang'),
(6,'exp',0,2000,0,'2.000 EXP'),
(7,'diamond',0,10,1,'10 Diamond + Thuong Streak');

-- ═════════════════════════════════════════════════════════════
-- 44. WORLD BOSS — Boss thế giới, spawn theo lịch
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS world_bosses (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    monster_id      INT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    map_id          INT NOT NULL,
    spawn_x         FLOAT NOT NULL DEFAULT 0,
    spawn_y         FLOAT NOT NULL DEFAULT 0,
    hp              INT NOT NULL DEFAULT 1000000,
    atk             INT NOT NULL DEFAULT 5000,
    def             INT NOT NULL DEFAULT 3000,
    reward_exp      INT NOT NULL DEFAULT 50000,
    reward_gold     INT NOT NULL DEFAULT 100000,
    loot_json       TEXT,                          -- JSON: [{"item_id":1,"drop_rate":0.1,"qty":1}]
    spawn_cron      VARCHAR(64) DEFAULT '0 20 * * 0', -- chủ nhật 20h
    duration_min    INT NOT NULL DEFAULT 30,        -- tồn tại tối đa 30 phút
    min_players     INT NOT NULL DEFAULT 5,         -- cần ít nhất 5 người
    announce_before_min INT NOT NULL DEFAULT 10,    -- thông báo trước 10 phút
    is_active       TINYINT NOT NULL DEFAULT 1,
    last_spawn_at   DATETIME DEFAULT NULL,
    last_killed_by  VARCHAR(32) DEFAULT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO world_bosses (id,monster_id,name,map_id,spawn_x,spawn_y,hp,reward_exp,reward_gold,spawn_cron) VALUES
(1,901,'Rong Co Dai',5,50,50,500000,30000,50000,'0 20 * * 0'),
(2,902,'Kiem Thanh Am Anh',3,30,30,800000,50000,80000,'0 20 * * 3'),
(3,903,'Tieu Than Phan Than',6,60,60,2000000,100000,200000,'0 20 * * 6');

-- ═════════════════════════════════════════════════════════════
-- 45. MONSTER DROPS — Bảng drop item chi tiết
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS monster_drops (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    monster_id      INT NOT NULL,
    item_id         INT NOT NULL,
    drop_rate       FLOAT NOT NULL DEFAULT 0.1,    -- 0.0-1.0 (10%)
    min_qty         INT NOT NULL DEFAULT 1,
    max_qty         INT NOT NULL DEFAULT 1,
    min_level       INT NOT NULL DEFAULT 1,         -- level tối thiểu monster
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_monster (monster_id)
);

-- ═════════════════════════════════════════════════════════════
-- 46. NPC SPAWN CONFIG — Quản lý spawn NPC trên map
-- ═════════════════════════════════════════════════════════════

-- Bảng npcs đã có sẵn (schema.sql) với map_id, pos_x, pos_y
-- Thêm fields mở rộng cho NPC management
ALTER TABLE npcs
    ADD COLUMN IF NOT EXISTS npc_type_detail VARCHAR(32) DEFAULT 'generic',  -- generic,quest_giver,shopkeeper,banker,blacksmith,stable_master,teleporter,guard
    ADD COLUMN IF NOT EXISTS interaction     VARCHAR(32) DEFAULT 'dialog',    -- dialog,shop,bank,enhance,teleport,quest,storage
    ADD COLUMN IF NOT EXISTS icon_asset      VARCHAR(128) DEFAULT 'Sprites/NPC/default',
    ADD COLUMN IF NOT EXISTS level_req       INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quest_ids       VARCHAR(256) DEFAULT '',         -- JSON: [1,5,12] — quest mà NPC này cho
    ADD COLUMN IF NOT EXISTS visible_hours   VARCHAR(32) DEFAULT '',          -- "08:00-22:00" hoặc "" = luôn hiển thị
    ADD COLUMN IF NOT EXISTS respawn_sec     INT NOT NULL DEFAULT 0;          -- 0 = luôn tồn tại

-- ═════════════════════════════════════════════════════════════
-- 47. MONSTER SPAWN ZONES — Khu vực spawn quái
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS monster_spawn_zones (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    map_id          INT NOT NULL,
    monster_id      INT NOT NULL,
    zone_x1         FLOAT NOT NULL,     -- góc trên trái
    zone_y1         FLOAT NOT NULL,
    zone_x2         FLOAT NOT NULL,     -- góc dưới phải
    zone_y2         FLOAT NOT NULL,
    max_count       INT NOT NULL DEFAULT 5,      -- số lượng tối đa trong zone
    respawn_sec     INT NOT NULL DEFAULT 30,     -- thời gian respawn
    is_active       TINYINT NOT NULL DEFAULT 1,
    INDEX idx_map (map_id)
);

-- ═════════════════════════════════════════════════════════════
-- 48. EVENT CURRENCY SHOP — CRUD cho shop event token (thiếu)
-- ═════════════════════════════════════════════════════════════
-- Bảng đã có, chỉ cần seed
INSERT IGNORE INTO event_currency_shop (id,currency_id,item_id,item_name,price,stock,per_user_limit,sort_order) VALUES
(1,1,1001,'Thuoc Hoi Mau x10',50,-1,0,1),
(2,1,2001,'Tui Do Mo Rong',200,100,1,2),
(3,1,3001,'Skin Doc Quyen Tet',500,50,1,3);

-- ═════════════════════════════════════════════════════════════
-- 49. CHARACTER APPEARANCE — Ngoại hình nhân vật (Mana Seed layers)
-- ═════════════════════════════════════════════════════════════

-- Tạo nhân vật: chỉ chọn ngoại hình + tên. Class chọn sau.
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS body_type   TINYINT NOT NULL DEFAULT 1,   -- 1=p1, 2=pONE1, 3=pONE2, 4=pONE3
    ADD COLUMN IF NOT EXISTS skin_color  TINYINT NOT NULL DEFAULT 1,   -- 0-10 (v00-v10)
    ADD COLUMN IF NOT EXISTS hair_style  TINYINT NOT NULL DEFAULT 0,   -- 0=bob1, 1=dap1
    ADD COLUMN IF NOT EXISTS hair_color  TINYINT NOT NULL DEFAULT 1,   -- 0-13 (v00-v13)
    ADD COLUMN IF NOT EXISTS shirt_color TINYINT NOT NULL DEFAULT 1,   -- 1-5 (v01-v05)
    ADD COLUMN IF NOT EXISTS pants_color TINYINT NOT NULL DEFAULT 1;   -- 1-5 (v01-v05)

-- class_id = 0 khi mới tạo (chưa chọn class)
-- Player chọn class tại NPC Class Master trong game (sau tutorial)

-- Sprite asset mapping (để server/client biết đường dẫn sprite)
CREATE TABLE IF NOT EXISTS character_sprite_config (
    config_key      VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value    TEXT NOT NULL,
    description     VARCHAR(256) DEFAULT ''
);

INSERT IGNORE INTO character_sprite_config VALUES
('body_types','[{"id":1,"key":"body_1","label":"Than Hinh 1"},{"id":2,"key":"body_2","label":"Than Hinh 2"},{"id":3,"key":"body_3","label":"Than Hinh 3"},{"id":4,"key":"body_4","label":"Than Hinh 4"}]','Cac loai than hinh'),
('skin_colors','11','So luong mau da (v00-v10)'),
('hair_styles','[{"id":0,"key":"bob1","label":"Toc Ngan"},{"id":1,"key":"dap1","label":"Toc Dai"}]','Kieu toc'),
('hair_colors','14','So luong mau toc (v00-v13)'),
('shirt_colors','5','So luong mau ao (v01-v05)'),
('pants_colors','5','So luong mau quan (v01-v05)'),
('sprite_size','64','Kich thuoc sprite (px)'),
('sheet_cols','8','So cot trong spritesheet'),
('sheet_rows','8','So hang trong spritesheet'),
('class_npc_id','0','NPC ID de chon class (0=chua set)');

-- Class change quest (NPC cho chọn class)
CREATE TABLE IF NOT EXISTS class_change_config (
    class_id        INT NOT NULL PRIMARY KEY,
    class_name      VARCHAR(32) NOT NULL,
    required_level  INT NOT NULL DEFAULT 1,      -- level tối thiểu để chọn class
    npc_id          INT NOT NULL DEFAULT 0,      -- NPC cho phép chọn class
    quest_id        INT NOT NULL DEFAULT 0,      -- quest phải hoàn thành trước
    description     TEXT,
    base_hp         INT NOT NULL DEFAULT 100,
    base_mp         INT NOT NULL DEFAULT 50,
    base_atk        INT NOT NULL DEFAULT 10,
    base_def        INT NOT NULL DEFAULT 5,
    hp_per_level    INT NOT NULL DEFAULT 20,
    mp_per_level    INT NOT NULL DEFAULT 10,
    atk_per_level   INT NOT NULL DEFAULT 3,
    def_per_level   INT NOT NULL DEFAULT 2,
    is_active       TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO class_change_config VALUES
(1,'Kiem Si',1,0,0,'Chien binh can chien, phong ngu va tan cong can bang.',120,40,12,8,25,8,4,3,1),
(2,'Sat Thu',1,0,0,'Diet dich nhanh, ne tranh, chi mang.',90,50,15,4,18,10,5,2,1),
(3,'Phap Su',1,0,0,'Phap thuat tam xa, sat thuong dien rong.',80,80,8,4,15,15,3,1,1),
(4,'Phap Thu',1,0,0,'Hoi mau, tang buff, bao ve dong doi.',100,90,6,6,20,18,2,3,1),
(5,'Cung Thu',1,0,0,'Tan cong tam xa, ban tinh, khu vuc.',85,60,13,5,17,12,4,2,1);

-- ═════════════════════════════════════════════════════════════
-- 50. EQUIPMENT SYSTEM — Trang bị toàn diện
-- ═════════════════════════════════════════════════════════════

-- 7 classes mới
DELETE FROM class_change_config;
INSERT INTO class_change_config (class_id,class_name,required_level,description,base_hp,base_mp,base_atk,base_def,hp_per_level,mp_per_level,atk_per_level,def_per_level) VALUES
(1,'Kiem Si',1,'Chien binh can chien su dung kiem, can bang tan cong va phong ngu.',130,40,14,10,28,6,4,3),
(2,'Phap Su',1,'Phap su tam xa, phep thuat manh nhung phong ngu yeu.',80,120,6,4,14,20,2,1),
(3,'Xa Thu',1,'Su dung sung, tan cong nhanh tam trung.',95,50,16,5,18,8,5,2),
(4,'Slinger',1,'Su dung na, linh hoat va ne tranh cao.',90,60,13,6,17,10,4,2),
(5,'Axeman',1,'Chien binh rìu, sat thuong vat ly cao nhat.',140,30,18,8,30,4,5,3),
(6,'Quyen Su',1,'Vo su, chan tay, chi mang va hut mau.',100,50,15,7,22,8,5,2),
(7,'Cung Thu',1,'Cung thu tam xa, ban tinh va khu vuc.',85,60,14,5,16,10,4,2);

-- Equipment slots (25 slots)
CREATE TABLE IF NOT EXISTS equipment_slots (
    slot_id         INT NOT NULL PRIMARY KEY,
    slot_name       VARCHAR(32) NOT NULL,
    slot_type       VARCHAR(16) NOT NULL,   -- main,sub,jewelry,accessory
    slot_key        VARCHAR(32) NOT NULL UNIQUE,
    max_per_char    INT NOT NULL DEFAULT 1,
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO equipment_slots VALUES
-- Trang bi chinh
(1,'Mu','main','helmet',1,1),
(2,'Ao Giap','main','armor',1,2),
(3,'Quan','main','pants',1,3),
(4,'Giay','main','boots',1,4),
(5,'Gang Tay','main','gloves',1,5),
-- Trang bi phu
(6,'Khien','sub','shield',1,6),
(7,'Sach Ma Phap','sub','spellbook',1,7),
(8,'Bao Ten','sub','quiver',1,8),
(9,'Bua Ho Menh','sub','talisman',1,9),
(10,'Ho Phu','sub','charm',1,10),
(11,'Skin','sub','skin',1,11),
-- Trang suc
(12,'Nhan 1','jewelry','ring_1',1,12),
(13,'Nhan 2','jewelry','ring_2',1,13),
(14,'Day Chuyen','jewelry','necklace',1,14),
(15,'Khuyen Tai 1','jewelry','earring_1',1,15),
(16,'Khuyen Tai 2','jewelry','earring_2',1,16),
(17,'Vong Tay 1','jewelry','bracelet_1',1,17),
(18,'Vong Tay 2','jewelry','bracelet_2',1,18),
-- Phu kien
(19,'Canh','accessory','wings',1,19),
(20,'Ao Choang','accessory','cape',1,20),
(21,'Mat Na','accessory','mask',1,21),
(22,'Danh Hieu','accessory','title_slot',1,22),
(23,'Thu Cuoi','accessory','mount_slot',1,23),
(24,'Pet','accessory','pet_slot',1,24),
-- Vu khi (tuy class)
(25,'Vu Khi','main','weapon',1,0);

-- Item templates mo rong — voi 12 chi so
DROP TABLE IF EXISTS item_templates;
CREATE TABLE IF NOT EXISTS item_templates (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     TEXT,
    item_type       VARCHAR(32) NOT NULL,       -- weapon,helmet,armor,pants,boots,gloves,shield,spellbook,quiver,talisman,charm,skin,ring,necklace,earring,bracelet,wings,cape,mask,consumable,material,quest_item,gem,scroll
    equip_slot      VARCHAR(32) DEFAULT NULL,    -- key tu equipment_slots
    class_restrict  VARCHAR(64) DEFAULT '',      -- '1,3,7' = chi Kiem Si, Xa Thu, Cung Thu. '' = tat ca
    level_req       INT NOT NULL DEFAULT 1,
    quality         TINYINT NOT NULL DEFAULT 0,  -- 0=Common,1=Rare,2=Epic,3=Legendary,4=Mythic
    icon_asset      VARCHAR(128) DEFAULT 'Icons/Items/default',
    sprite_asset    VARCHAR(128) DEFAULT '',     -- sprite khi trang bi (layer)
    -- 12 chi so
    stat_hp         INT NOT NULL DEFAULT 0,
    stat_mp         INT NOT NULL DEFAULT 0,
    stat_patk       INT NOT NULL DEFAULT 0,      -- cong vat ly
    stat_matk       INT NOT NULL DEFAULT 0,      -- cong phep
    stat_def        INT NOT NULL DEFAULT 0,
    stat_crit       INT NOT NULL DEFAULT 0,      -- ti le chi mang (x0.01%)
    stat_dodge      INT NOT NULL DEFAULT 0,      -- ne tranh
    stat_accuracy   INT NOT NULL DEFAULT 0,      -- chinh xac
    stat_aspd       INT NOT NULL DEFAULT 0,      -- toc danh
    stat_mspd       INT NOT NULL DEFAULT 0,      -- toc chay
    stat_lifesteal  INT NOT NULL DEFAULT 0,      -- hut mau (x0.01%)
    stat_resist     INT NOT NULL DEFAULT 0,      -- khang hieu ung
    -- Nang cap
    max_enhance     INT NOT NULL DEFAULT 15,     -- +1 den +15
    gem_slots       INT NOT NULL DEFAULT 0,      -- so luong o khảm ngoc
    can_refine      TINYINT NOT NULL DEFAULT 0,  -- co the tinh luyen
    can_awaken      TINYINT NOT NULL DEFAULT 0,  -- co the thuc tinh
    -- Kinh te
    buy_price       INT NOT NULL DEFAULT 0,
    sell_price      INT NOT NULL DEFAULT 0,
    is_tradeable    TINYINT NOT NULL DEFAULT 1,
    is_stackable    TINYINT NOT NULL DEFAULT 0,
    max_stack       INT NOT NULL DEFAULT 1,
    is_active       TINYINT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (item_type),
    INDEX idx_quality (quality),
    INDEX idx_slot (equip_slot)
);

-- Player equipped items
CREATE TABLE IF NOT EXISTS character_equipment (
    char_id         BIGINT NOT NULL,
    slot_key        VARCHAR(32) NOT NULL,        -- tu equipment_slots.slot_key
    inventory_id    BIGINT NOT NULL,             -- ID trong character_inventory
    item_id         INT NOT NULL,
    enhance_level   INT NOT NULL DEFAULT 0,
    gem_1           INT DEFAULT 0,
    gem_2           INT DEFAULT 0,
    gem_3           INT DEFAULT 0,
    refine_level    INT NOT NULL DEFAULT 0,
    awaken_level    INT NOT NULL DEFAULT 0,
    quality_bonus   INT NOT NULL DEFAULT 0,      -- nang cap pham chat: +0 den +5
    PRIMARY KEY (char_id, slot_key)
);

-- ═════════════════════════════════════════════════════════════
-- 51. ENHANCEMENT SYSTEM — Cuong hoa, kham ngoc, tinh luyen, thuc tinh
-- ═════════════════════════════════════════════════════════════

-- Cuong hoa +1 den +15 (ti le giam dan)
CREATE TABLE IF NOT EXISTS enhance_rates (
    level           INT NOT NULL PRIMARY KEY,     -- 1-15
    success_rate    FLOAT NOT NULL,               -- 0.0-1.0
    gold_cost       INT NOT NULL DEFAULT 0,
    material_id     INT DEFAULT 0,                -- item cuong hoa (da cuong hoa)
    material_qty    INT NOT NULL DEFAULT 1,
    on_fail         VARCHAR(16) NOT NULL DEFAULT 'nothing', -- nothing,downgrade,destroy
    stat_bonus_pct  FLOAT NOT NULL DEFAULT 0.05   -- +5% stats per level
);

INSERT IGNORE INTO enhance_rates VALUES
(1, 1.0,   500,  0,1,'nothing',0.03),
(2, 1.0,   1000, 0,1,'nothing',0.03),
(3, 0.95,  2000, 0,1,'nothing',0.04),
(4, 0.90,  4000, 0,1,'nothing',0.04),
(5, 0.80,  8000, 0,1,'nothing',0.05),
(6, 0.70,  15000,0,2,'nothing',0.05),
(7, 0.60,  25000,0,2,'downgrade',0.06),
(8, 0.50,  40000,0,3,'downgrade',0.06),
(9, 0.40,  60000,0,3,'downgrade',0.07),
(10,0.30,  80000,0,4,'downgrade',0.07),
(11,0.20, 100000,0,5,'downgrade',0.08),
(12,0.15, 150000,0,5,'downgrade',0.09),
(13,0.10, 200000,0,6,'destroy', 0.10),
(14,0.05, 300000,0,8,'destroy', 0.12),
(15,0.03, 500000,0,10,'destroy',0.15);

-- Ngoc (gem) — kham vao trang bi
CREATE TABLE IF NOT EXISTS gem_templates (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    gem_type        VARCHAR(16) NOT NULL,        -- atk,def,hp,mp,crit,dodge,aspd,mspd
    stat_value      INT NOT NULL DEFAULT 0,
    quality         TINYINT NOT NULL DEFAULT 0,  -- 0-4
    icon_asset      VARCHAR(128) DEFAULT '',
    is_active       TINYINT NOT NULL DEFAULT 1
);

INSERT IGNORE INTO gem_templates (id,name,gem_type,stat_value,quality) VALUES
(1,'Ngoc Luc','atk',10,0),(2,'Ngoc Do','hp',50,0),(3,'Ngoc Xanh','def',8,0),
(4,'Ngoc Vang','crit',20,0),(5,'Ngoc Tim','dodge',15,0),
(6,'Ngoc Luc Cao','atk',30,1),(7,'Ngoc Do Cao','hp',150,1),(8,'Ngoc Xanh Cao','def',25,1),
(9,'Ngoc Luc Quy','atk',80,2),(10,'Ngoc Do Quy','hp',400,2);

-- Tinh luyen (refine) — tang stats an
CREATE TABLE IF NOT EXISTS refine_config (
    level           INT NOT NULL PRIMARY KEY,
    stat_bonus_pct  FLOAT NOT NULL DEFAULT 0.02,
    gold_cost       INT NOT NULL DEFAULT 0,
    material_id     INT DEFAULT 0,
    success_rate    FLOAT NOT NULL DEFAULT 1.0
);

INSERT IGNORE INTO refine_config VALUES
(1,0.02,5000,0,1.0),(2,0.04,10000,0,0.9),(3,0.06,20000,0,0.8),
(4,0.08,40000,0,0.7),(5,0.10,80000,0,0.5);

-- Thuc tinh (awaken) — mo khoa skill dac biet
CREATE TABLE IF NOT EXISTS awaken_config (
    level           INT NOT NULL PRIMARY KEY,
    required_enhance INT NOT NULL DEFAULT 10,    -- can cuong hoa +X truoc
    required_quality INT NOT NULL DEFAULT 2,     -- can pham chat Epic+
    gold_cost       INT NOT NULL DEFAULT 0,
    material_id     INT DEFAULT 0,
    bonus_skill_id  INT DEFAULT 0,               -- unlock skill dac biet
    stat_multiplier FLOAT NOT NULL DEFAULT 1.0
);

INSERT IGNORE INTO awaken_config VALUES
(1,10,2,100000,0,0,1.10),(2,12,3,300000,0,0,1.25),(3,15,4,1000000,0,0,1.50);

-- ═════════════════════════════════════════════════════════════
-- 52. ADMIN ASSET PACK UPLOAD — Tải ZIP, tự động unzip
-- ═════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS asset_packs (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    pack_name       VARCHAR(128) NOT NULL,
    original_filename VARCHAR(256) NOT NULL,
    extract_path    VARCHAR(256) NOT NULL,        -- đường dẫn sau khi giải nén
    file_count      INT NOT NULL DEFAULT 0,
    total_size      BIGINT NOT NULL DEFAULT 0,
    pack_type       VARCHAR(32) DEFAULT 'sprite', -- sprite,audio,map,ui,effect
    status          VARCHAR(16) NOT NULL DEFAULT 'uploaded', -- uploaded,extracting,ready,error
    uploaded_by     VARCHAR(64) DEFAULT 'admin',
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (pack_type, status)
);

-- Seed pack hiện tại
INSERT IGNORE INTO asset_packs (id,pack_name,original_filename,extract_path,file_count,total_size,pack_type,status) VALUES
(1,'Mana Seed Character Base Demo','FREE_Mana_Seed_Character_Base_Demo_2_0.zip','Character/',334,0,'sprite','ready'),
(2,'Character Base 2.5c','20_01a_-_Character_Base_2_5c.zip','Character/',335,0,'sprite','ready'),
(3,'Hairstyle Pack v0.5.3','21_01a_-_Hairstyle_Pack_v0_5_3.zip','Character/',437,0,'sprite','ready');

-- Seed items — vu khi cho 7 class
INSERT IGNORE INTO item_templates (id,name,item_type,equip_slot,class_restrict,level_req,quality,stat_patk,stat_aspd,buy_price) VALUES
(10001,'Kiem Go','weapon','weapon','1',1,0,15,5,100),
(10002,'Kiem Sat','weapon','weapon','1',10,1,35,8,2000),
(10003,'Gay Phap','weapon','weapon','2',1,0,5,3,100),
(10004,'Gay Phap Xanh','weapon','weapon','2',10,1,15,5,2000),
(10005,'Sung Ngan','weapon','weapon','3',1,0,18,10,100),
(10006,'Sung Truong','weapon','weapon','3',10,1,40,12,2000),
(10007,'Na Dan','weapon','weapon','4',1,0,16,8,100),
(10008,'Rìu Go','weapon','weapon','5',1,0,22,3,100),
(10009,'Rìu Sat','weapon','weapon','5',10,1,50,5,2000),
(10010,'Quyen Sach','weapon','weapon','6',1,0,12,15,100),
(10011,'Cung Ngan','weapon','weapon','7',1,0,14,7,100),
(10012,'Cung Dai','weapon','weapon','7',10,1,32,10,2000);

-- Seed trang bi co ban
INSERT IGNORE INTO item_templates (id,name,item_type,equip_slot,level_req,quality,stat_def,stat_hp,buy_price) VALUES
(20001,'Non La','helmet','helmet',1,0,3,10,50),
(20002,'Mu Sat','helmet','helmet',10,1,8,30,1000),
(20003,'Ao Vai','armor','armor',1,0,5,15,80),
(20004,'Giap Sat','armor','armor',10,1,12,50,1500),
(20005,'Quan Vai','pants','pants',1,0,3,10,60),
(20006,'Quan Giap','pants','pants',10,1,8,30,1000),
(20007,'Giay Co','boots','boots',1,0,2,5,40),
(20008,'Giay Sat','boots','boots',10,1,6,20,800),
(20009,'Gang Vai','gloves','gloves',1,0,2,5,40),
(20010,'Gang Sat','gloves','gloves',10,1,5,15,700);

-- Seed trang suc
INSERT IGNORE INTO item_templates (id,name,item_type,equip_slot,level_req,quality,stat_patk,stat_crit,stat_dodge,buy_price) VALUES
(30001,'Nhan Dong','ring','ring_1',5,0,3,5,0,500),
(30002,'Day Chuyen Dong','necklace','necklace',5,0,2,3,3,600),
(30003,'Khuyen Tai Dong','earring','earring_1',5,0,2,0,5,400),
(30004,'Vong Tay Dong','bracelet','bracelet_1',5,0,1,3,3,350);

-- ═════════════════════════════════════════════════════════════
-- 53. EYES — Thêm mắt vào tạo nhân vật
-- ═════════════════════════════════════════════════════════════

-- Layer mới: 3fac (face) — mắt xếp giữa base body và outfit
-- Sprite path: body_{N}/3fac/eye_v{XX}.png
-- 11 kiểu mắt: v00–v10

ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS eye_style TINYINT NOT NULL DEFAULT 0; -- 0-10 (eye_v00 - eye_v10)

-- Cập nhật sprite config
INSERT INTO character_sprite_config (config_key, config_value, description)
VALUES ('eye_styles', '11', 'Số lượng kiểu mắt (eye_v00 - eye_v10)')
ON DUPLICATE KEY UPDATE config_value='11';

-- Layer order cập nhật:
-- Layer 0: Base body (0bas_humn_vXX)
-- Layer 1: Pants (1out/pfpn_vXX)
-- Layer 2: Shirt (1out/fstr_vXX)
-- Layer 3: Eyes (3fac/eye_vXX)     ← MỚI
-- Layer 4: Hair (4har/{style}_vXX)
-- Layer 5: Hat (5hat/ — equipment)
-- Layer 6: Weapon (6tla/ — equipment)
-- Layer 7: Shield (7tlb/ — equipment)

INSERT INTO character_sprite_config (config_key, config_value, description)
VALUES ('layer_order', '["0bas","1out_pfpn","1out_fstr","3fac_eye","4har","5hat","6tla","7tlb"]',
        'Thứ tự render layer từ dưới lên')
ON DUPLICATE KEY UPDATE config_value='["0bas","1out_pfpn","1out_fstr","3fac_eye","4har","5hat","6tla","7tlb"]';

-- ═════════════════════════════════════════════════════════════
-- 54. ANIMATION SYSTEM — Map Farmer sprites vào character
-- ═════════════════════════════════════════════════════════════

-- Mỗi nhân vật dùng 2 hệ thống sprite:
--   Farmer System  (1024x1024, 32px) → TẤT CẢ animations (walk, idle, combat, farm, fish)
--   Character Base (512x512, 64px) → CHỈ dùng cho preview/avatar UI
-- Farmer System là SHARED — mọi body type dùng chung farmer sheets

CREATE TABLE IF NOT EXISTS animation_states (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    state_key       VARCHAR(32) NOT NULL UNIQUE,  -- 'idle','walk','run','attack_sword','attack_bow','farm_hoe','fish','carry','sit','sleep','die','cast'
    display_name    VARCHAR(64) NOT NULL,
    sprite_system   VARCHAR(16) NOT NULL DEFAULT 'farmer', -- 'charbase' hoặc 'farmer'
    sheet_size      INT NOT NULL DEFAULT 1024,    -- kích thước sheet (px)
    tile_size       INT NOT NULL DEFAULT 32,      -- kích thước 1 tile (px)
    -- Vị trí trong spritesheet (row range cho 4 hướng: down, up, right, left)
    row_down        INT NOT NULL DEFAULT 0,
    row_up          INT NOT NULL DEFAULT 1,
    row_right       INT NOT NULL DEFAULT 2,
    row_left        INT NOT NULL DEFAULT 3,
    frame_count     INT NOT NULL DEFAULT 6,       -- số frame mỗi hướng
    frame_rate      FLOAT NOT NULL DEFAULT 8.0,   -- FPS
    is_looping      TINYINT NOT NULL DEFAULT 1,   -- 1=lặp, 0=chạy 1 lần
    effect_key      VARCHAR(64) DEFAULT '',        -- sprite effect kèm theo
    sort_order      INT NOT NULL DEFAULT 0
);

INSERT IGNORE INTO animation_states (state_key,display_name,sprite_system,sheet_size,tile_size,row_down,row_up,row_right,row_left,frame_count,frame_rate,is_looping,effect_key) VALUES
-- Farmer System (tất cả dùng farmer, kể cả idle/walk)
('idle',          'Đứng yên',        'farmer',1024,32, 0,1,2,3, 6,6,  1,''),
('walk',          'Đi bộ',           'farmer',1024,32, 4,5,6,7, 6,8,  1,''),
-- Farmer System animations (1024x1024, 32px, 32x32)
('run',           'Chạy',            'farmer',1024,32,  4,5,6,7,   6,10, 1,''),
('attack_sword',  'Chém kiếm',       'farmer',1024,32,  16,17,18,19, 6,12, 0,'farmer slash effects 64x64'),
('attack_bow',    'Bắn cung',        'farmer',1024,32,  20,21,22,23, 6,10, 0,'farmer bow 001 32x32 v00'),
('attack_axe',    'Chém rìu',        'farmer',1024,32,  16,17,18,19, 6,10, 0,''),
('attack_fist',   'Đấm',            'farmer',1024,32,  16,17,18,19, 4,12, 0,''),
('attack_gun',    'Bắn súng',        'farmer',1024,32,  20,21,22,23, 4,14, 0,''),
('attack_sling',  'Bắn ná',         'farmer',1024,32,  20,21,22,23, 5,10, 0,''),
('cast_spell',    'Thi phép',       'farmer',1024,32,  24,25,26,27, 6,8,  0,''),
('farm_hoe',      'Cuốc đất',       'farmer',1024,32,  12,13,14,15, 6,8,  0,'farmer tool 001 v00'),
('farm_water',    'Tưới nước',       'farmer',1024,32,  12,13,14,15, 6,8,  0,'farmer tool 002 v00'),
('farm_axe',      'Chặt cây',        'farmer',1024,32,  12,13,14,15, 6,8,  0,'farmer tool 003 v00'),
('fish',          'Câu cá',           'farmer',1024,32,  28,29,30,31, 8,6,  0,'fishing effects (up, down) 64x96 v00'),
('carry',         'Mang vác',         'farmer',1024,32,  8,9,10,11,   6,8,  1,'farmer props 32x32 v00'),
('sit',           'Ngồi',             'farmer',1024,32,  8,9,10,11,   1,1,  1,''),
('die',           'Chết',             'farmer',1024,32,  8,9,10,11,   4,6,  0,''),
('revive',        'Hồi sinh',         'farmer',1024,32,  8,9,10,11,   4,8,  0,'');

-- Map class → attack animation
CREATE TABLE IF NOT EXISTS class_animation_map (
    class_id        INT NOT NULL,
    action_key      VARCHAR(32) NOT NULL,          -- 'attack','skill_1','skill_2',...
    animation_state VARCHAR(32) NOT NULL,           -- FK → animation_states.state_key
    PRIMARY KEY (class_id, action_key)
);

INSERT IGNORE INTO class_animation_map VALUES
(1,'attack','attack_sword'),  (1,'skill','cast_spell'),
(2,'attack','cast_spell'),    (2,'skill','cast_spell'),
(3,'attack','attack_gun'),    (3,'skill','attack_gun'),
(4,'attack','attack_sling'),  (4,'skill','attack_sling'),
(5,'attack','attack_axe'),    (5,'skill','attack_axe'),
(6,'attack','attack_fist'),   (6,'skill','attack_fist'),
(7,'attack','attack_bow'),    (7,'skill','cast_spell');

-- Farmer layer → character appearance mapping
-- Khi nhân vật thực hiện action, dùng farmer layer tương ứng với outfit đang mặc
CREATE TABLE IF NOT EXISTS farmer_layer_mapping (
    char_outfit_key VARCHAR(32) NOT NULL,           -- key từ character appearance (shirt, pants, hair...)
    farmer_layer    VARCHAR(8) NOT NULL,             -- folder trong FarmerSystem/sheets/ (05shrt, 04lwr1, 13hair...)
    farmer_file     VARCHAR(128) NOT NULL,           -- file mặc định
    description     VARCHAR(128) DEFAULT '',
    PRIMARY KEY (char_outfit_key, farmer_layer)
);

INSERT IGNORE INTO farmer_layer_mapping VALUES
('body',    '01body', 'fbas_01body_human_00.png',        'Base body farmer'),
('pants',   '04lwr1', 'fbas_04lwr1_longpants_00a.png',   'Quần dài mặc định'),
('shirt',   '05shrt', 'fbas_05shrt_shortshirt_00a.png',  'Áo ngắn mặc định'),
('hair_bob1','13hair','fbas_13hair_bob1_00.png',          'Tóc bob1'),
('hair_bob2','13hair','fbas_13hair_bob2_00.png',          'Tóc bob2'),
('hair_dap1','13hair','fbas_13hair_dapper_00.png',        'Tóc dapper'),
('hair_flat','13hair','fbas_13hair_flattop_00.png',       'Tóc flat'),
('hair_fro1','13hair','fbas_13hair_afro_00.png',          'Tóc afro'),
('hair_pon1','13hair','fbas_13hair_ponytail1_00.png',     'Tóc ponytail'),
('hair_spk2','13hair','fbas_13hair_spiky2_00.png',        'Tóc spiky'),
('boots',   '03fot1', 'fbas_03fot1_boots_00a.png',       'Giày mặc định'),
('gloves',  '09hand', 'fbas_09hand_gloves_00a.png',      'Găng tay'),
('hat',     '14head', 'fbas_14head_strawhat_01.png',     'Nón mặc định'),
('cape',    '00undr', 'fbas_00undr_cloakplain_00d.png',  'Áo choàng'),
('glasses', '12face', 'fbas_12face_glasses_00a.png',     'Kính');

-- Cập nhật sprite config
INSERT INTO character_sprite_config (config_key, config_value, description)
VALUES 
('farmer_sheet_size', '1024', 'Farmer spritesheet size (px)'),
('farmer_tile_size', '32', 'Farmer tile size (px)'),
('farmer_grid', '32', 'Farmer grid cols/rows'),
('animation_system', 'farmer_only', 'Farmer System cho tất cả gameplay, Character Base cho preview/avatar')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);


-- ═════════════════════════════════════════════════════════════
-- 55. ĐỒNG BỘ KÍCH THƯỚC — Farmer System là primary gameplay
-- ═════════════════════════════════════════════════════════════

-- Character Base (64px) gấp 2x Farmer System (32px)
-- KHÔNG thể trộn trực tiếp → chọn 1 hệ thống làm chính

-- QUYẾT ĐỊNH:
--   Farmer System = GAMEPLAY (mọi animation: walk, idle, combat, farm, fish)
--   Character Base = CHARACTER CREATION UI + AVATAR/PORTRAIT (preview lớn, chi tiết)

-- Cập nhật animation_states: TẤT CẢ dùng farmer system
UPDATE animation_states SET sprite_system='farmer', sheet_size=1024, tile_size=32
WHERE state_key IN ('idle','walk');

-- Cập nhật idle/walk row positions cho Farmer System
UPDATE animation_states SET row_down=0, row_up=1, row_right=2, row_left=3,
    frame_count=6, frame_rate=6 WHERE state_key='idle';
UPDATE animation_states SET row_down=4, row_up=5, row_right=6, row_left=7,
    frame_count=6, frame_rate=8 WHERE state_key='walk';

-- Config rõ ràng
DELETE FROM character_sprite_config WHERE config_key IN ('primary_system','charbase_usage','render_ppu','render_scale');
INSERT INTO character_sprite_config (config_key, config_value, description) VALUES
('primary_system', 'farmer', 'Hệ thống sprite chính cho gameplay'),
('charbase_usage', 'preview', 'Character Base dùng cho: preview khi tạo nhân vật, avatar, portrait'),
('render_ppu', '32', 'Pixels Per Unit trong Unity (Farmer = 32px tiles)'),
('render_scale', '1', 'Scale nhân vật trong game world (1 = 32px, 2 = 64px hiển thị)');

-- ═════════════════════════════════════════════════════════════
-- 56. PLAYER SETTINGS — 11 tabs, ~100 options
-- ═════════════════════════════════════════════════════════════

-- Lưu settings mỗi player dạng JSON blob (đơn giản, flexible)
CREATE TABLE IF NOT EXISTS player_settings (
    char_id         BIGINT NOT NULL PRIMARY KEY,
    settings_json   MEDIUMTEXT NOT NULL DEFAULT '{}',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Default settings (server gửi cho client mới)
CREATE TABLE IF NOT EXISTS default_settings (
    tab_key         VARCHAR(32) NOT NULL,
    setting_key     VARCHAR(64) NOT NULL,
    setting_type    VARCHAR(16) NOT NULL DEFAULT 'bool',  -- bool,int,float,enum,slider
    default_value   VARCHAR(64) NOT NULL DEFAULT 'false',
    display_name    VARCHAR(64) NOT NULL,
    description     VARCHAR(128) DEFAULT '',
    enum_values     VARCHAR(256) DEFAULT '',  -- cho type=enum: 'very_low,low,medium,high,ultra'
    min_val         FLOAT DEFAULT 0,         -- cho slider
    max_val         FLOAT DEFAULT 100,
    sort_order      INT NOT NULL DEFAULT 0,
    PRIMARY KEY (tab_key, setting_key)
);

-- ── Tab 1: GAME ──────────────────────────────────────────
INSERT IGNORE INTO default_settings VALUES
-- Chien dau
('game','auto_target','bool','true','Auto Target','Tu dong khoa muc tieu','',0,0,1),
('game','smart_target','bool','true','Smart Target','Uu tien muc tieu gan nhat','',0,0,2),
('game','priority_player','bool','false','Uu tien Player','Uu tien tan cong nguoi choi','',0,0,3),
('game','priority_boss','bool','true','Uu tien Boss','Uu tien tan cong boss','',0,0,4),
('game','priority_elite','bool','false','Uu tien Elite','Uu tien tan cong elite','',0,0,5),
('game','show_damage','bool','true','Hien sat thuong','Hien so sat thuong','',0,0,6),
('game','show_crit','bool','true','Hien chi mang','Hien hieu ung chi mang','',0,0,7),
('game','show_heal','bool','true','Hien hoi mau','Hien so hoi mau','',0,0,8),
('game','screen_shake','bool','true','Rung khi trung don','Rung man hinh khi nhan sat thuong','',0,0,9),
-- Nhat do
('game','auto_loot','bool','true','Tu nhat do','Tu dong nhat do roi','',0,0,10),
('game','loot_equip_only','bool','false','Chi nhat trang bi','Chi nhat trang bi roi','',0,0,11),
('game','loot_rare_only','bool','false','Chi nhat hiem','Chi nhat vat pham hiem tro len','',0,0,12),
('game','loot_gold_only','bool','false','Chi nhat vang','Chi nhat vang roi','',0,0,13),
('game','loot_filter','bool','false','Bo loc item','Bat bo loc item nang cao','',0,0,14),
-- Quest
('game','auto_accept_quest','bool','false','Auto nhan NV','Tu dong nhan nhiem vu','',0,0,15),
('game','auto_submit_quest','bool','false','Auto tra NV','Tu dong tra nhiem vu','',0,0,16),
('game','track_main_quest','bool','true','Theo doi NV chinh','Hien nhiem vu chinh tren man hinh','',0,0,17),
('game','track_side_quest','bool','true','Theo doi NV phu','Hien nhiem vu phu','',0,0,18),

-- ── Tab 2: GRAPHICS ──────────────────────────────────────
('graphics','quality','enum','medium','Chat luong','Chat luong do hoa','very_low,low,medium,high,ultra',0,0,1),
('graphics','fps_limit','enum','60','FPS','Gioi han FPS','30,60,90,120',0,0,2),
('graphics','show_pet','bool','true','Hien pet','Hien thi pet nguoi khac','',0,0,3),
('graphics','show_mount','bool','true','Hien mount','Hien thi thu cuoi nguoi khac','',0,0,4),
('graphics','show_skill_fx','bool','true','Hien hieu ung ky nang','Hien hieu ung ky nang','',0,0,5),
('graphics','show_others_fx','bool','true','Hien hieu ung nguoi khac','Hien hieu ung ky nang nguoi khac','',0,0,6),
('graphics','show_title','bool','true','Hien danh hieu','Hien danh hieu tren dau','',0,0,7),
('graphics','show_wings','bool','true','Hien canh','Hien canh nguoi khac','',0,0,8),
('graphics','max_visible_players','int','50','Gioi han nguoi choi','So nguoi choi hien thi toi da','',5,200,9),
('graphics','max_visible_pets','int','20','Gioi han pet','So pet hien thi toi da','',0,100,10),
('graphics','crowd_mode','bool','false','Che do dong nguoi','Giam hieu ung khi dong','',0,0,11),
('graphics','battery_saver','bool','false','Tiet kiem pin','Giam FPS va hieu ung de tiet kiem pin','',0,0,12),

-- ── Tab 3: UI ────────────────────────────────────────────
('ui','ui_scale','slider','1.0','Ti le UI','Ti le phong to/thu nho UI','',0.5,2.0,1),
('ui','minimap_scale','slider','1.0','Ti le minimap','','',0.5,2.0,2),
('ui','chat_scale','slider','1.0','Ti le chat','','',0.5,2.0,3),
('ui','show_name','bool','true','Hien ten','Hien ten nguoi choi','',0,0,4),
('ui','show_guild_tag','bool','true','Hien guild','Hien ten guild','',0,0,5),
('ui','show_level','bool','true','Hien level','Hien cap do','',0,0,6),
('ui','show_hp_bar','bool','true','Hien HP','Hien thanh mau','',0,0,7),
('ui','float_damage','bool','true','Float Damage','Hien so sat thuong bay','',0,0,8),
('ui','float_exp','bool','true','Float EXP','Hien EXP nhan duoc','',0,0,9),
('ui','float_gold','bool','true','Float Gold','Hien vang nhan duoc','',0,0,10),
('ui','float_loot','bool','true','Float Loot','Hien vat pham nhat duoc','',0,0,11),

-- ── Tab 4: CHAT ──────────────────────────────────────────
('chat','chat_world','bool','true','Chat World','Hien chat the gioi','',0,0,1),
('chat','chat_guild','bool','true','Chat Guild','Hien chat guild','',0,0,2),
('chat','chat_party','bool','true','Chat Party','Hien chat nhom','',0,0,3),
('chat','chat_pm','bool','true','Chat PM','Hien tin nhan rieng','',0,0,4),
('chat','chat_cross','bool','true','Chat Cross','Hien chat lien server','',0,0,5),
('chat','chat_system','bool','true','Chat System','Hien thong bao he thong','',0,0,6),
('chat','voice_enabled','bool','false','Voice Chat','Bat voice chat','',0,0,7),
('chat','voice_volume','slider','80','Am luong Voice','Am luong voice chat','',0,100,8),
('chat','voice_auto_play','bool','true','Tu dong phat Voice','Tu dong phat voice message','',0,0,9),
('chat','block_guild_invite','bool','false','Chan loi moi guild','Chan loi moi gia nhap guild','',0,0,10),
('chat','block_trade','bool','false','Chan giao dich','Chan yeu cau giao dich','',0,0,11),
('chat','block_friend','bool','false','Chan ket ban','Chan loi moi ket ban','',0,0,12),

-- ── Tab 5: GUILD ─────────────────────────────────────────
('guild','notify_guild','bool','true','TB guild','Thong bao guild chung','',0,0,1),
('guild','notify_guild_war','bool','true','TB chien guild','Thong bao chien tranh guild','',0,0,2),
('guild','notify_guild_boss','bool','true','TB boss guild','Thong bao boss guild','',0,0,3),
('guild','notify_member_online','bool','true','TB thanh vien online','Thong bao khi thanh vien vao game','',0,0,4),

-- ── Tab 6: PARTY ─────────────────────────────────────────
('party','auto_accept_party','bool','false','Tu dong chap nhan','Tu dong vao nhom khi duoc moi','',0,0,1),
('party','auto_decline_party','bool','false','Tu dong tu choi','Tu dong tu choi loi moi nhom','',0,0,2),
('party','auto_follow_leader','bool','false','Tu dong theo doi truong','Di chuyen theo doi truong','',0,0,3),
('party','show_party_pos','bool','true','Hien vi tri dong doi','Hien vi tri thanh vien nhom tren map','',0,0,4),
('party','show_party_cd','bool','true','Hien CD dong doi','Hien cooldown ky nang dong doi','',0,0,5),

-- ── Tab 7: NOTIFICATIONS ─────────────────────────────────
('notify','world_boss','bool','true','World Boss','Thong bao world boss spawn','',0,0,1),
('notify','event_boss','bool','true','Event Boss','Thong bao event boss','',0,0,2),
('notify','dungeon_boss','bool','true','Dungeon Boss','Thong bao dungeon boss','',0,0,3),
('notify','double_exp','bool','true','Double EXP','Thong bao su kien double EXP','',0,0,4),
('notify','mission_pass','bool','true','Mission Pass','Thong bao mission pass moi','',0,0,5),
('notify','new_event','bool','true','Event moi','Thong bao event moi','',0,0,6),
('notify','maintenance','bool','true','Bao tri','Thong bao bao tri','',0,0,7),
('notify','gift_code','bool','true','Gift Code','Thong bao gift code moi','',0,0,8),
('notify','admin_msg','bool','true','TB Admin','Thong bao tu admin','',0,0,9),
('notify','auction_sold','bool','true','Dau gia ban duoc','TB khi vat pham ban duoc','',0,0,10),
('notify','auction_expired','bool','true','Dau gia het han','TB khi vat pham het han','',0,0,11),
('notify','auction_outbid','bool','true','Bi tra gia cao hon','TB khi bi tra gia cao hon','',0,0,12),
('notify','farm_ready','bool','true','Cay truong thanh','TB khi cay da truong thanh','',0,0,13),
('notify','animal_hungry','bool','true','Vat nuoi doi','TB khi vat nuoi doi','',0,0,14),
('notify','house_visitor','bool','true','Khach ghe tham','TB khi co nguoi vao nha','',0,0,15),

-- ── Tab 8: AUDIO ─────────────────────────────────────────
('audio','master_volume','slider','80','Master','Am luong tong','',0,100,1),
('audio','music_volume','slider','60','Music','Nhac nen','',0,100,2),
('audio','effect_volume','slider','80','Effect','Hieu ung am thanh','',0,100,3),
('audio','ui_volume','slider','70','UI','Am thanh giao dien','',0,100,4),
('audio','voice_volume_audio','slider','80','Voice','Am luong giong noi','',0,100,5),
('audio','ambient_volume','slider','50','Ambient','Am thanh moi truong','',0,100,6),

-- ── Tab 9: CONTROLS ──────────────────────────────────────
('controls','joystick_size','slider','1.0','Kich thuoc Joystick','','',0.5,2.0,1),
('controls','skill_btn_size','slider','1.0','Kich thuoc Skill','','',0.5,2.0,2),
('controls','btn_opacity','slider','0.7','Do trong suot nut','','',0.1,1.0,3),
('controls','camera_sensitivity','slider','1.0','Do nhay camera','','',0.1,3.0,4),
('controls','auto_run','bool','false','Auto Run','Tu dong chay','',0,0,5),

-- ── Tab 10: NETWORK ──────────────────────────────────────
('network','show_ping','bool','true','Hien Ping','Hien do tre mang','',0,0,1),
('network','show_fps','bool','true','Hien FPS','Hien so khung hinh','',0,0,2),
('network','data_saver','bool','false','Tiet kiem du lieu','Giam chat luong de tiet kiem 4G','',0,0,3),
('network','wifi_only_download','bool','false','Chi tai qua WiFi','Chi tai tai nguyen khi co WiFi','',0,0,4),
('network','auto_patch','bool','true','Tu tai patch','Tu dong tai ban cap nhat','',0,0,5),

-- ── Tab 11: ACCOUNT ──────────────────────────────────────
('account','font_large','bool','false','Font lon','Tang kich thuoc chu','',0,0,1),
('account','colorblind_mode','bool','false','Che do mu mau','Dieu chinh mau cho nguoi mu mau','',0,0,2),
('account','voice_subtitle','bool','false','Phu de Voice','Hien phu de khi nghe voice','',0,0,3),
('account','one_hand_mode','bool','false','Che do mot tay','Giao dien cho choi 1 tay','',0,0,4);

-- ═════════════════════════════════════════════════════════════
-- 57. TÁCH SETTINGS — Mỗi hệ thống quản lý settings riêng
-- ═════════════════════════════════════════════════════════════

-- NGUYÊN TẮC:
--   Bảng Settings chính (6 tabs): Game, Graphics, Audio, Controls, Network, Account
--   Settings theo tính năng → nằm TRONG UI của tính năng đó:
--     Guild settings    → trong Guild panel
--     Party settings    → trong Party panel  
--     Chat settings     → trong Chat panel
--     Auction settings  → trong Auction UI
--     Farm/Housing      → trong Farm/Housing UI
--     Notifications     → trong Notification center

-- Xoá settings cũ của các tab feature-specific
DELETE FROM default_settings WHERE tab_key IN ('chat','guild','party','notify');

-- Thêm vào player_settings JSON structure mới:
-- {
--   "game": { "auto_target": true, ... },
--   "graphics": { "quality": "medium", ... },
--   "audio": { "master_volume": 80, ... },
--   "controls": { "joystick_size": 1.0, ... },
--   "network": { "show_ping": true, ... },
--   "account": { "font_large": false, ... }
-- }
-- Các settings feature-specific lưu riêng:

-- Chat preferences (hiển thị trong Chat UI, không phải Settings)
CREATE TABLE IF NOT EXISTS player_chat_prefs (
    char_id             BIGINT NOT NULL PRIMARY KEY,
    chat_world          TINYINT NOT NULL DEFAULT 1,
    chat_guild          TINYINT NOT NULL DEFAULT 1,
    chat_party          TINYINT NOT NULL DEFAULT 1,
    chat_pm             TINYINT NOT NULL DEFAULT 1,
    chat_cross          TINYINT NOT NULL DEFAULT 1,
    chat_system         TINYINT NOT NULL DEFAULT 1,
    voice_enabled       TINYINT NOT NULL DEFAULT 0,
    voice_volume        INT NOT NULL DEFAULT 80,
    voice_auto_play     TINYINT NOT NULL DEFAULT 1,
    block_guild_invite  TINYINT NOT NULL DEFAULT 0,
    block_trade         TINYINT NOT NULL DEFAULT 0,
    block_friend        TINYINT NOT NULL DEFAULT 0
);

-- Guild notification prefs (hiển thị trong Guild panel)
CREATE TABLE IF NOT EXISTS player_guild_prefs (
    char_id             BIGINT NOT NULL PRIMARY KEY,
    notify_guild        TINYINT NOT NULL DEFAULT 1,
    notify_guild_war    TINYINT NOT NULL DEFAULT 1,
    notify_guild_boss   TINYINT NOT NULL DEFAULT 1,
    notify_member_online TINYINT NOT NULL DEFAULT 1
);

-- Party prefs (hiển thị trong Party UI)
CREATE TABLE IF NOT EXISTS player_party_prefs (
    char_id             BIGINT NOT NULL PRIMARY KEY,
    auto_accept         TINYINT NOT NULL DEFAULT 0,
    auto_decline        TINYINT NOT NULL DEFAULT 0,
    auto_follow_leader  TINYINT NOT NULL DEFAULT 0,
    show_party_pos      TINYINT NOT NULL DEFAULT 1,
    show_party_cd       TINYINT NOT NULL DEFAULT 1
);

-- Notification prefs (hiển thị trong Notification center / bell icon)
CREATE TABLE IF NOT EXISTS player_notify_prefs (
    char_id             BIGINT NOT NULL PRIMARY KEY,
    world_boss          TINYINT NOT NULL DEFAULT 1,
    event_boss          TINYINT NOT NULL DEFAULT 1,
    dungeon_boss        TINYINT NOT NULL DEFAULT 1,
    double_exp          TINYINT NOT NULL DEFAULT 1,
    mission_pass        TINYINT NOT NULL DEFAULT 1,
    new_event           TINYINT NOT NULL DEFAULT 1,
    maintenance         TINYINT NOT NULL DEFAULT 1,
    gift_code           TINYINT NOT NULL DEFAULT 1,
    admin_msg           TINYINT NOT NULL DEFAULT 1,
    auction_sold        TINYINT NOT NULL DEFAULT 1,
    auction_expired     TINYINT NOT NULL DEFAULT 1,
    auction_outbid      TINYINT NOT NULL DEFAULT 1,
    farm_ready          TINYINT NOT NULL DEFAULT 1,
    animal_hungry       TINYINT NOT NULL DEFAULT 1,
    house_visitor       TINYINT NOT NULL DEFAULT 1
);

-- Giờ bảng Settings chính chỉ còn 6 tabs gọn:
--   Game (18 options)     — chiến đấu, nhặt đồ, quest
--   Graphics (12 options) — chất lượng, FPS, hiển thị
--   Audio (6 options)     — âm lượng
--   Controls (5 options)  — joystick, skill size, camera
--   Network (5 options)   — ping, FPS, data saver
--   Account (4 options)   — accessibility
-- Tổng: 50 options trong Settings, 30 options phân tán ở các UI khác
